package com.medibook.auth.dto;

import com.medibook.auth.entity.Role;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Used for POST /auth/register-with-otp.
 * Same fields as RegisterRequestDto, plus the OTP the user received by email.
 *
 * Flow:
 *  1. POST /auth/send-otp  { email }
 *  2. POST /auth/register-with-otp { fullName, email, password, phone, role, otp }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterWithOtpRequestDto {

    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$",
            message = "Password must be at least 8 characters and include uppercase, lowercase, digit and special character"
    )
    private String password;

    @NotBlank(message = "Phone is required")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Phone number must be a valid 10 digit Indian mobile number"
    )
    private String phone;

    @NotNull(message = "Role is required")
    private Role role;

    @NotBlank(message = "OTP is required")
    @Size(min = 6, max = 6, message = "OTP must be 6 digits")
    private String otp;
}
