package com.medibook.payment.service;

import com.medibook.payment.config.RabbitMQConfig;
import com.medibook.payment.dto.*;
import com.medibook.payment.entity.Payment;
import com.medibook.payment.entity.PaymentMode;
import com.medibook.payment.entity.PaymentStatus;
import com.medibook.payment.exception.BusinessException;
import com.medibook.payment.exception.ResourceNotFoundException;
import com.medibook.payment.repository.PaymentRepository;
import com.medibook.payment.service.impl.PaymentServiceImpl;
import com.razorpay.RazorpayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private RazorpayClient razorpayClient;

    @InjectMocks private PaymentServiceImpl paymentService;

    private Payment successfulPayment;

    @BeforeEach
    void setUp() {
        successfulPayment = Payment.builder()
                .paymentId(1L).appointmentId(10L).patientId(100L)
                .amount(500.0).status(PaymentStatus.SUCCESS)
                .mode(PaymentMode.UPI).transactionId("TXN_12345")
                .currency("INR").paidAt(LocalDateTime.now()).build();
    }

    // ── processPayment (mock/legacy flow) ─────────────────────────────────────

    @Test
    void processPayment_success() {
        PaymentRequestDto req = PaymentRequestDto.builder()
                .appointmentId(10L).patientId(100L).amount(500.0)
                .mode(PaymentMode.UPI).build();

        when(paymentRepository.findByAppointmentId(10L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenReturn(successfulPayment);
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        PaymentResponseDto result = paymentService.processPayment(req);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.getAmount()).isEqualTo(500.0);
        verify(paymentRepository).save(any());
    }

    @Test
    void processPayment_duplicate_throws() {
        PaymentRequestDto req = PaymentRequestDto.builder()
                .appointmentId(10L).patientId(100L).amount(500.0).mode(PaymentMode.UPI).build();

        when(paymentRepository.findByAppointmentId(10L)).thenReturn(Optional.of(successfulPayment));

        assertThatThrownBy(() -> paymentService.processPayment(req))
                .isInstanceOf(BusinessException.class).hasMessageContaining("already exists");
    }

    // ── getPaymentById ─────────────────────────────────────────────────────────

    @Test
    void getPaymentById_found() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(successfulPayment));
        assertThat(paymentService.getPaymentById(1L).getPaymentId()).isEqualTo(1L);
    }

    @Test
    void getPaymentById_notFound_throws() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> paymentService.getPaymentById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getPaymentByAppointmentId ──────────────────────────────────────────────

    @Test
    void getPaymentByAppointmentId_found() {
        when(paymentRepository.findByAppointmentId(10L)).thenReturn(Optional.of(successfulPayment));
        assertThat(paymentService.getPaymentByAppointmentId(10L).getAppointmentId()).isEqualTo(10L);
    }

    @Test
    void getPaymentByAppointmentId_notFound_throws() {
        when(paymentRepository.findByAppointmentId(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> paymentService.getPaymentByAppointmentId(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getPaymentsByPatient ───────────────────────────────────────────────────

    @Test
    void getPaymentsByPatient_returnsList() {
        when(paymentRepository.findByPatientId(100L)).thenReturn(List.of(successfulPayment));
        assertThat(paymentService.getPaymentsByPatient(100L)).hasSize(1);
    }

    // ── refundPayment ──────────────────────────────────────────────────────────

    @Test
    void refundPayment_success() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(successfulPayment));
        when(paymentRepository.save(any())).thenReturn(successfulPayment);
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        RefundRequestDto refundReq = new RefundRequestDto("Patient request");
        PaymentResponseDto result = paymentService.refundPayment(1L, refundReq);
        assertThat(result).isNotNull();
        verify(paymentRepository).save(any());
    }

    @Test
    void refundPayment_alreadyRefunded_throws() {
        successfulPayment.setStatus(PaymentStatus.REFUNDED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(successfulPayment));

        assertThatThrownBy(() -> paymentService.refundPayment(1L, new RefundRequestDto("reason")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("already refunded");
    }

    @Test
    void refundPayment_notSuccessful_throws() {
        successfulPayment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(successfulPayment));

        assertThatThrownBy(() -> paymentService.refundPayment(1L, new RefundRequestDto("reason")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("successful payments");
    }

    @Test
    void refundPayment_notFound_throws() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> paymentService.refundPayment(99L, new RefundRequestDto("reason")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getAllPayments ─────────────────────────────────────────────────────────

    @Test
    void getAllPayments_returnsList() {
        when(paymentRepository.findAll()).thenReturn(List.of(successfulPayment));
        assertThat(paymentService.getAllPayments()).hasSize(1);
    }

    // ── createRazorpayOrder ─── (duplicate check only; Razorpay SDK is final) ──

    @Test
    void createRazorpayOrder_duplicate_throws() {
        RazorpayOrderRequestDto req = RazorpayOrderRequestDto.builder()
                .appointmentId(10L).patientId(100L).amount(500.0).build();

        when(paymentRepository.findByAppointmentId(10L)).thenReturn(Optional.of(successfulPayment));

        assertThatThrownBy(() -> paymentService.createRazorpayOrder(req))
                .isInstanceOf(BusinessException.class).hasMessageContaining("already exists");
    }
}
