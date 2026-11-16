package com.example.taskflow_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Used for verify-email and verify-reset-otp. */
public record OtpVerifyRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        String email,

        @NotBlank(message = "Enter the 6-digit code")
        @Pattern(regexp = "^\\d{6}$", message = "The code must be 6 digits")
        String otp
) {}

