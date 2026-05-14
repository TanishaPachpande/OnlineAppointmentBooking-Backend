package com.medibook.payment.service;

import com.medibook.payment.dto.*;

import java.util.List;

public interface PaymentService {

    // ── Razorpay flow ────────────────────────────────────────────────────────
    /**
     * Step 1: Create a Razorpay order on Razorpay's servers and return the
     * order details the frontend needs to open the Razorpay checkout widget.
     */
    RazorpayOrderResponseDto createRazorpayOrder(RazorpayOrderRequestDto requestDto);

    /**
     * Step 2: Verify the HMAC-SHA256 signature returned by Razorpay after the
     * user completes payment, then persist the payment record in our DB.
     */
    PaymentResponseDto verifyAndRecordPayment(RazorpayVerifyRequestDto requestDto);

    // ── Legacy / query endpoints ─────────────────────────────────────────────
    PaymentResponseDto processPayment(PaymentRequestDto requestDto);

    PaymentResponseDto getPaymentById(Long paymentId);

    PaymentResponseDto getPaymentByAppointmentId(Long appointmentId);

    List<PaymentResponseDto> getPaymentsByPatient(Long patientId);

    PaymentResponseDto refundPayment(Long paymentId, RefundRequestDto requestDto);

    List<PaymentResponseDto> getAllPayments();
}
