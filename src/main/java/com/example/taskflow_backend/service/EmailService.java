package com.example.taskflow_backend.service;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String fullName, String otpCode, int expiryMinutes);
    void sendPasswordResetEmail(String toEmail, String fullName, String otpCode, int expiryMinutes);
}

