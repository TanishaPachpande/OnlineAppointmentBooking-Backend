package com.medibook.payment.service.impl;

import com.medibook.payment.config.RabbitMQConfig;
import com.medibook.payment.dto.*;
import com.medibook.payment.entity.Payment;
import com.medibook.payment.entity.PaymentStatus;
import com.medibook.payment.exception.BusinessException;
import com.medibook.payment.exception.ResourceNotFoundException;
import com.medibook.payment.repository.PaymentRepository;
import com.medibook.payment.service.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    // Demo emails — same pattern as appointment-service
    private static final String DEMO_PATIENT_EMAIL  = "tanishapachpande0072@gmail.com";
    private static final String DEMO_PROVIDER_EMAIL = "tanishapachpande86@gmail.com";

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate    rabbitTemplate;
    private final RazorpayClient    razorpayClient;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.currency:INR}")
    private String currency;

    public PaymentServiceImpl(PaymentRepository repo,
                              RabbitTemplate rabbitTemplate,
                              RazorpayClient razorpayClient) {
        this.paymentRepository = repo;
        this.rabbitTemplate    = rabbitTemplate;
        this.razorpayClient    = razorpayClient;
    }

    // ── Razorpay flow ────────────────────────────────────────────────────────

    @Override
    public RazorpayOrderResponseDto createRazorpayOrder(RazorpayOrderRequestDto requestDto) {
        log.info("Creating Razorpay order for appointmentId={}, amount={}",
                requestDto.getAppointmentId(), requestDto.getAmount());

        paymentRepository.findByAppointmentId(requestDto.getAppointmentId()).ifPresent(existing -> {
            log.warn("Payment already exists for appointmentId={}", requestDto.getAppointmentId());
            throw new BusinessException("Payment already exists for this appointment");
        });

        try {
            long amountInPaise = Math.round(requestDto.getAmount() * 100);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount",          amountInPaise);
            orderRequest.put("currency",        currency);
            orderRequest.put("receipt",         "appt_" + requestDto.getAppointmentId());
            orderRequest.put("payment_capture", 1);

            Order  order          = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = order.get("id");

            log.info("Razorpay order created. razorpayOrderId={}", razorpayOrderId);

            return RazorpayOrderResponseDto.builder()
                    .razorpayOrderId(razorpayOrderId)
                    .amount(requestDto.getAmount())
                    .currency(currency)
                    .keyId(razorpayKeyId)
                    .appointmentId(requestDto.getAppointmentId())
                    .patientId(requestDto.getPatientId())
                    .notes(requestDto.getNotes())
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage(), e);
            throw new BusinessException("Failed to create Razorpay order: " + e.getMessage());
        }
    }

    @Override
    public PaymentResponseDto verifyAndRecordPayment(RazorpayVerifyRequestDto requestDto) {
        log.info("Verifying Razorpay payment. orderId={}, paymentId={}",
                requestDto.getRazorpayOrderId(), requestDto.getRazorpayPaymentId());

        boolean signatureValid = verifyRazorpaySignature(
                requestDto.getRazorpayOrderId(),
                requestDto.getRazorpayPaymentId(),
                requestDto.getRazorpaySignature()
        );

        if (!signatureValid) {
            log.error("Razorpay signature FAILED for orderId={}", requestDto.getRazorpayOrderId());
            throw new BusinessException("Payment verification failed: invalid signature");
        }

        log.info("Razorpay signature OK for orderId={}", requestDto.getRazorpayOrderId());

        paymentRepository.findByAppointmentId(requestDto.getAppointmentId()).ifPresent(existing -> {
            throw new BusinessException("Payment already exists for this appointment");
        });

        Payment payment = Payment.builder()
                .appointmentId(requestDto.getAppointmentId())
                .patientId(requestDto.getPatientId())
                .amount(requestDto.getAmount())
                .mode(requestDto.getMode())
                .status(PaymentStatus.SUCCESS)
                .transactionId(requestDto.getRazorpayPaymentId())
                .currency(currency)
                .paidAt(LocalDateTime.now())
                .notes(requestDto.getNotes())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // Notify patient
        publishNotification(
                savedPayment.getPatientId(),
                DEMO_PATIENT_EMAIL,
                "Payment Successful – MediBook",
                "Your payment of ₹" + savedPayment.getAmount() +
                        " for appointment #" + savedPayment.getAppointmentId() +
                        " was successful.\nTransaction ID: " + savedPayment.getTransactionId()
        );

        // Notify doctor
        publishNotification(
                savedPayment.getPatientId(), // providerId not stored on payment entity; use appointmentId context
                DEMO_PROVIDER_EMAIL,
                "Payment Received for Appointment #" + savedPayment.getAppointmentId(),
                "A payment of ₹" + savedPayment.getAmount() +
                        " has been received for appointment #" + savedPayment.getAppointmentId() +
                        ".\nTransaction ID: " + savedPayment.getTransactionId()
        );

        log.info("Payment recorded. paymentId={}, transactionId={}",
                savedPayment.getPaymentId(), savedPayment.getTransactionId());

        return mapToResponse(savedPayment);
    }

    // ── Signature helper ─────────────────────────────────────────────────────

    private boolean verifyRazorpaySignature(String orderId, String paymentId, String receivedSignature) {
        try {
            String data = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes("UTF-8"), "HmacSHA256"));
            byte[] hash              = mac.doFinal(data.getBytes("UTF-8"));
            String computedSignature = HexFormat.of().formatHex(hash);
            return constantTimeEquals(computedSignature, receivedSignature);
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) result |= a.charAt(i) ^ b.charAt(i);
        return result == 0;
    }

    // ── Legacy / query ───────────────────────────────────────────────────────

    @Override
    public PaymentResponseDto processPayment(PaymentRequestDto requestDto) {
        log.info("Processing mock payment for appointmentId={}", requestDto.getAppointmentId());

        paymentRepository.findByAppointmentId(requestDto.getAppointmentId()).ifPresent(existing -> {
            throw new BusinessException("Payment already exists for this appointment");
        });

        Payment payment = Payment.builder()
                .appointmentId(requestDto.getAppointmentId())
                .patientId(requestDto.getPatientId())
                .amount(requestDto.getAmount())
                .mode(requestDto.getMode())
                .status(PaymentStatus.SUCCESS)
                .transactionId("TXN_" + System.currentTimeMillis())
                .currency("INR")
                .paidAt(LocalDateTime.now())
                .notes(requestDto.getNotes())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // Notify patient
        publishNotification(
                savedPayment.getPatientId(),
                DEMO_PATIENT_EMAIL,
                "Payment Successful – MediBook",
                "Your payment of ₹" + savedPayment.getAmount() +
                        " for appointment #" + savedPayment.getAppointmentId() + " was successful."
        );

        // Notify doctor
        publishNotification(
                savedPayment.getPatientId(),
                DEMO_PROVIDER_EMAIL,
                "Payment Received for Appointment #" + savedPayment.getAppointmentId(),
                "A payment of ₹" + savedPayment.getAmount() +
                        " has been received for appointment #" + savedPayment.getAppointmentId() + "."
        );

        return mapToResponse(savedPayment);
    }

    @Override
    public PaymentResponseDto getPaymentById(Long paymentId) {
        return mapToResponse(paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId)));
    }

    @Override
    public PaymentResponseDto getPaymentByAppointmentId(Long appointmentId) {
        return mapToResponse(paymentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for appointmentId: " + appointmentId)));
    }

    @Override
    public List<PaymentResponseDto> getPaymentsByPatient(Long patientId) {
        return paymentRepository.findByPatientId(patientId).stream().map(this::mapToResponse).toList();
    }

    @Override
    public PaymentResponseDto refundPayment(Long paymentId, RefundRequestDto requestDto) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        if (payment.getStatus() == PaymentStatus.REFUNDED)
            throw new BusinessException("Payment is already refunded");
        if (payment.getStatus() != PaymentStatus.SUCCESS)
            throw new BusinessException("Only successful payments can be refunded");

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());
        payment.setNotes(requestDto.getReason());
        Payment savedPayment = paymentRepository.save(payment);

        // Notify patient of refund
        publishNotification(
                savedPayment.getPatientId(),
                DEMO_PATIENT_EMAIL,
                "Refund Processed – MediBook",
                "Your payment for appointment #" + savedPayment.getAppointmentId() +
                        " has been refunded.\nReason: " + requestDto.getReason()
        );

        // Notify doctor of refund
        publishNotification(
                savedPayment.getPatientId(),
                DEMO_PROVIDER_EMAIL,
                "Payment Refunded for Appointment #" + savedPayment.getAppointmentId(),
                "Payment #" + savedPayment.getPaymentId() +
                        " for appointment #" + savedPayment.getAppointmentId() +
                        " has been refunded.\nReason: " + requestDto.getReason()
        );

        return mapToResponse(savedPayment);
    }

    @Override
    public List<PaymentResponseDto> getAllPayments() {
        return paymentRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Builds a NotificationMessage that exactly matches NotificationEventDto
     * in notification-service (userId, recipient, type, subject, message)
     * and sends it to the correct exchange + routing-key.
     */
    private void publishNotification(Long userId, String recipient, String subject, String message) {
        NotificationMessage msg = NotificationMessage.builder()
                .userId(userId)
                .recipient(recipient)
                .type("EMAIL")
                .subject(subject)
                .message(message)
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, msg);
        log.info("Notification published to exchange={} routingKey={} for recipient={}",
                RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, recipient);
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
