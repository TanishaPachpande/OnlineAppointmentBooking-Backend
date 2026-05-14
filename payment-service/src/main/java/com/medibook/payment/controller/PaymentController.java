package com.medibook.payment.controller;

import com.medibook.payment.dto.*;
import com.medibook.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/payments")
@Tag(name = "Payment Controller", description = "APIs for Razorpay payment processing")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // ── Razorpay endpoints ───────────────────────────────────────────────────

    /**
     * Step 1: Frontend calls this to get a Razorpay order ID.
     * The returned razorpayOrderId + keyId are used to open the checkout widget.
     */
    @Operation(summary = "Create Razorpay order", description = "Creates an order on Razorpay's servers. Returns order ID and key needed to open checkout.")
    @PostMapping("/razorpay/create-order")
    public ResponseEntity<RazorpayOrderResponseDto> createRazorpayOrder(
            @Valid @RequestBody RazorpayOrderRequestDto requestDto) {
        log.info("API CALL: Create Razorpay order for appointmentId={}", requestDto.getAppointmentId());
        return ResponseEntity.ok(paymentService.createRazorpayOrder(requestDto));
    }

    /**
     * Step 2: Frontend calls this after the user successfully pays in the
     * Razorpay widget. We verify the signature and save the payment.
     */
    @Operation(summary = "Verify and record Razorpay payment", description = "Verifies the HMAC-SHA256 signature from Razorpay and persists the payment record.")
    @PostMapping("/razorpay/verify")
    public ResponseEntity<PaymentResponseDto> verifyAndRecordPayment(
            @Valid @RequestBody RazorpayVerifyRequestDto requestDto) {
        log.info("API CALL: Verify Razorpay payment for appointmentId={}", requestDto.getAppointmentId());
        return ResponseEntity.ok(paymentService.verifyAndRecordPayment(requestDto));
    }

    // ── Legacy / query endpoints ─────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<PaymentResponseDto> processPayment(@Valid @RequestBody PaymentRequestDto requestDto) {
        log.info("API CALL: Process mock payment for appointmentId={}", requestDto.getAppointmentId());
        return ResponseEntity.ok(paymentService.processPayment(requestDto));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDto> getPaymentById(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<PaymentResponseDto> getPaymentByAppointmentId(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(paymentService.getPaymentByAppointmentId(appointmentId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<PaymentResponseDto>> getPaymentsByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(paymentService.getPaymentsByPatient(patientId));
    }

    @PutMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentResponseDto> refundPayment(@PathVariable Long paymentId,
                                                            @Valid @RequestBody RefundRequestDto requestDto) {
        return ResponseEntity.ok(paymentService.refundPayment(paymentId, requestDto));
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponseDto>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }
}
