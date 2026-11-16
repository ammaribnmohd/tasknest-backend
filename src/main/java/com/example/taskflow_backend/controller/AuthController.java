package com.example.taskflow_backend.controller;

import com.example.taskflow_backend.dto.*;
import com.example.taskflow_backend.security.AppUserDetails;
import com.example.taskflow_backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication and authorization")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user and dispatch email verification OTP")
    public ResponseEntity<OtpIssuedResponse> register(@Valid @RequestBody RegisterRequest request) {
        OtpIssuedResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email using OTP and automatically establish session")
    public ResponseEntity<UserResponse> verifyEmail(
            @Valid @RequestBody OtpVerifyRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        return ResponseEntity.ok(authService.verifyEmail(request, httpRequest, httpResponse));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend email verification OTP with cooldown check")
    public ResponseEntity<OtpIssuedResponse> resendVerification(@Valid @RequestBody EmailRequest request) {
        return ResponseEntity.ok(authService.resendVerification(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with email and password")
    public ResponseEntity<UserResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        return ResponseEntity.ok(authService.login(request, httpRequest, httpResponse));
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out and invalidate current session")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logout(httpRequest, httpResponse);
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal AppUserDetails principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Initiate password reset and dispatch OTP code")
    public ResponseEntity<OtpIssuedResponse> forgotPassword(@Valid @RequestBody EmailRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/verify-reset-otp")
    @Operation(summary = "Verify OTP code for password reset")
    public ResponseEntity<MessageResponse> verifyResetOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyResetOtp(request));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using OTP code")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}

