package com.example.taskflow_backend.dto;

/**
 * Returned for register / resend / forgot-password. The numbers are fixed configuration values
 * (not per-account state) so the response never reveals whether an account exists.
 */
public record OtpIssuedResponse(String message, String email, int expiresInSeconds, int resendAvailableInSeconds) {}

