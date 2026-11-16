package com.example.taskflow_backend.service;

import com.example.taskflow_backend.enums.OtpPurpose;
import com.example.taskflow_backend.model.User;

public interface OtpService {
    String generateAndSaveOtp(User user, OtpPurpose purpose);
    void verifyOtp(User user, OtpPurpose purpose, String code);
    int getCooldownSeconds();
    int getExpiryMinutes();
}

