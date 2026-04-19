package com.medibook.payment.service;

import com.medibook.payment.dto.PaymentRequestDto;
import com.medibook.payment.dto.PaymentResponseDto;
import com.medibook.payment.dto.RefundRequestDto;
import com.medibook.payment.entity.Payment;
import com.medibook.payment.entity.PaymentMode;
import com.medibook.payment.entity.PaymentStatus;
import com.medibook.payment.exception.BusinessException;
import com.medibook.payment.repository.PaymentRepository;
import com.medibook.payment.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void processPayment_ShouldProcessSuccessfully() {
        PaymentRequestDto requestDto = PaymentRequestDto.builder()
                .appointmentId(1L)
                .patientId(101L)
                .amount(500.0)
                .mode(PaymentMode.UPI)
                .notes("Test payment")
                .build();

        Payment savedPayment = Payment.builder()
                .paymentId(1L)
                .appointmentId(1L)
                .patientId(101L)
                .amount(500.0)
                .mode(PaymentMode.UPI)
                .status(PaymentStatus.SUCCESS)
                .transactionId("TXN_123456")
                .currency("INR")
                .notes("Test payment")
                .build();

        when(paymentRepository.findByAppointmentId(1L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        PaymentResponseDto response = paymentService.processPayment(requestDto);

        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals(500.0, response.getAmount());
    }

    @Test
    void processPayment_ShouldThrowException_WhenPaymentAlreadyExists() {
        Payment existingPayment = Payment.builder()
                .paymentId(1L)
                .appointmentId(1L)
                .build();

        PaymentRequestDto requestDto = PaymentRequestDto.builder()
                .appointmentId(1L)
                .patientId(101L)
                .amount(500.0)
                .mode(PaymentMode.UPI)
                .build();

        when(paymentRepository.findByAppointmentId(1L)).thenReturn(Optional.of(existingPayment));

        assertThrows(BusinessException.class, () -> paymentService.processPayment(requestDto));
    }

    @Test
    void refundPayment_ShouldRefundSuccessfully() {
        Payment payment = Payment.builder()
                .paymentId(1L)
                .appointmentId(1L)
                .patientId(101L)
                .amount(500.0)
                .mode(PaymentMode.UPI)
                .status(PaymentStatus.SUCCESS)
                .transactionId("TXN_123456")
                .currency("INR")
                .build();

        RefundRequestDto refundRequestDto = RefundRequestDto.builder()
                .reason("Appointment cancelled")
                .build();

        Payment refundedPayment = Payment.builder()
                .paymentId(1L)
                .appointmentId(1L)
                .patientId(101L)
                .amount(500.0)
                .mode(PaymentMode.UPI)
                .status(PaymentStatus.REFUNDED)
                .transactionId("TXN_123456")
                .currency("INR")
                .notes("Appointment cancelled")
                .build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(refundedPayment);

        PaymentResponseDto response = paymentService.refundPayment(1L, refundRequestDto);

        assertEquals(PaymentStatus.REFUNDED, response.getStatus());
        assertEquals("Appointment cancelled", response.getNotes());
    }
}