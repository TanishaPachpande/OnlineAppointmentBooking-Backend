package com.medibook.payment.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RazorpayOrderResponseDto {

    private String razorpayOrderId;   // e.g., order_abc123
    private Double amount;             // INR
    private String currency;           // INR
    private String keyId;              // Razorpay public key (safe to send to frontend)
    private Long appointmentId;
    private Long patientId;
    private String notes;
}
