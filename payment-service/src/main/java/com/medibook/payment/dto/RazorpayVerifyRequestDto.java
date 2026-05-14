package com.medibook.payment.dto;

import com.medibook.payment.entity.PaymentMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RazorpayVerifyRequestDto {

    // All three fields are sent back by Razorpay checkout after a successful payment

    @NotBlank(message = "Razorpay order ID is required")
    private String razorpayOrderId;       // e.g., order_abc123

    @NotBlank(message = "Razorpay payment ID is required")
    private String razorpayPaymentId;     // e.g., pay_xyz789 — this becomes transactionId

    @NotBlank(message = "Razorpay signature is required")
    private String razorpaySignature;     // HMAC-SHA256 signature from Razorpay

    // Context needed to save the payment record
    @NotNull(message = "Appointment ID is required")
    private Long appointmentId;

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Amount is required")
    private Double amount;

    @NotNull(message = "Payment mode is required")
    private PaymentMode mode;

    private String notes;
}
