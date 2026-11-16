package com.example.taskflow_backend.service;

import com.example.taskflow_backend.dto.*;
import com.example.taskflow_backend.security.AppUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    OtpIssuedResponse register(RegisterRequest request);
    UserResponse verifyEmail(OtpVerifyRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);
    OtpIssuedResponse resendVerification(EmailRequest request);
    UserResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);
    void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse);
    UserResponse getCurrentUser(AppUserDetails principal);
    OtpIssuedResponse forgotPassword(EmailRequest request);
    MessageResponse verifyResetOtp(OtpVerifyRequest request);
    MessageResponse resetPassword(ResetPasswordRequest request);
}

