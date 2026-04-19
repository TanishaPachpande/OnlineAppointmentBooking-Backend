package com.medibook.payment.service;

import com.medibook.payment.dto.*;

import java.util.List;

public interface PaymentService {

    PaymentResponseDto processPayment(PaymentRequestDto requestDto);

    PaymentResponseDto getPaymentById(Long paymentId);

    PaymentResponseDto getPaymentByAppointmentId(Long appointmentId);

    List<PaymentResponseDto> getPaymentsByPatient(Long patientId);

    PaymentResponseDto refundPayment(Long paymentId, RefundRequestDto requestDto);

    List<PaymentResponseDto> getAllPayments();
}