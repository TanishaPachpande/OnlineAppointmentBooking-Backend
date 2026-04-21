package com.medibook.payment.service.impl;

import com.medibook.payment.config.RabbitMQConfig;
import com.medibook.payment.dto.NotificationMessage;
import com.medibook.payment.dto.PaymentRequestDto;
import com.medibook.payment.dto.PaymentResponseDto;
import com.medibook.payment.dto.RefundRequestDto;
import com.medibook.payment.entity.Payment;
import com.medibook.payment.entity.PaymentStatus;
import com.medibook.payment.exception.BusinessException;
import com.medibook.payment.exception.ResourceNotFoundException;
import com.medibook.payment.repository.PaymentRepository;
import com.medibook.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;

    public PaymentServiceImpl(PaymentRepository repo, RabbitTemplate rabbitTemplate) {
        this.paymentRepository = repo;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public PaymentResponseDto processPayment(PaymentRequestDto requestDto) {
        log.info("Processing payment for appointmentId={}, patientId={}, amount={}",
                requestDto.getAppointmentId(), requestDto.getPatientId(), requestDto.getAmount());

        paymentRepository.findByAppointmentId(requestDto.getAppointmentId()).ifPresent(existing -> {
            log.warn("Payment already exists for appointmentId={}", requestDto.getAppointmentId());
            throw new BusinessException("Payment already exists for this appointment");
        });

        String transactionId = "TXN_" + System.currentTimeMillis();

        Payment payment = Payment.builder()
                .appointmentId(requestDto.getAppointmentId())
                .patientId(requestDto.getPatientId())
                .amount(requestDto.getAmount())
                .mode(requestDto.getMode())
                .status(PaymentStatus.SUCCESS)
                .transactionId(transactionId)
                .currency("INR")
                .paidAt(LocalDateTime.now())
                .notes(requestDto.getNotes())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        NotificationMessage notificationMessage = NotificationMessage.builder()
                .userId(savedPayment.getPatientId())
                .email(null)
                .message("Payment successful for appointmentId=" + savedPayment.getAppointmentId()
                        + ", transactionId=" + savedPayment.getTransactionId())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                notificationMessage
        );

        log.info("Payment successful. paymentId={}, transactionId={}",
                savedPayment.getPaymentId(), savedPayment.getTransactionId());

        return mapToResponse(savedPayment);
    }

    @Override
    public PaymentResponseDto getPaymentById(Long paymentId) {
        log.info("Fetching payment by paymentId={}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> {
                    log.error("Payment not found with id={}", paymentId);
                    return new ResourceNotFoundException("Payment not found with id: " + paymentId);
                });

        return mapToResponse(payment);
    }

    @Override
    public PaymentResponseDto getPaymentByAppointmentId(Long appointmentId) {
        log.info("Fetching payment by appointmentId={}", appointmentId);

        Payment payment = paymentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> {
                    log.error("Payment not found for appointmentId={}", appointmentId);
                    return new ResourceNotFoundException("Payment not found for appointmentId: " + appointmentId);
                });

        return mapToResponse(payment);
    }

    @Override
    public List<PaymentResponseDto> getPaymentsByPatient(Long patientId) {
        log.info("Fetching payments for patientId={}", patientId);

        return paymentRepository.findByPatientId(patientId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public PaymentResponseDto refundPayment(Long paymentId, RefundRequestDto requestDto) {
        log.info("Refund requested for paymentId={}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> {
                    log.error("Payment not found with id={}", paymentId);
                    return new ResourceNotFoundException("Payment not found with id: " + paymentId);
                });

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            log.warn("Payment already refunded. paymentId={}", paymentId);
            throw new BusinessException("Payment is already refunded");
        }

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            log.warn("Only successful payments can be refunded. paymentId={}", paymentId);
            throw new BusinessException("Only successful payments can be refunded");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());
        payment.setNotes(requestDto.getReason());

        Payment savedPayment = paymentRepository.save(payment);

        NotificationMessage notificationMessage = NotificationMessage.builder()
                .userId(savedPayment.getPatientId())
                .email(null)
                .message("Payment refunded successfully for paymentId=" + savedPayment.getPaymentId()
                        + ", transactionId=" + savedPayment.getTransactionId())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                notificationMessage
        );

        log.info("Refund successful for paymentId={}, transactionId={}",
                savedPayment.getPaymentId(), savedPayment.getTransactionId());

        return mapToResponse(savedPayment);
    }

    @Override
    public List<PaymentResponseDto> getAllPayments() {
        log.info("Fetching all payments");
        return paymentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private PaymentResponseDto mapToResponse(Payment payment) {
        return PaymentResponseDto.builder()
                .paymentId(payment.getPaymentId())
                .appointmentId(payment.getAppointmentId())
                .patientId(payment.getPatientId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .mode(payment.getMode())
                .transactionId(payment.getTransactionId())
                .currency(payment.getCurrency())
                .paidAt(payment.getPaidAt())
                .refundedAt(payment.getRefundedAt())
                .notes(payment.getNotes())
                .build();
    }
}