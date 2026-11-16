package com.example.taskflow_backend.service;

import com.example.taskflow_backend.config.AppProperties;
import com.example.taskflow_backend.dto.OtpIssuedResponse;
import com.example.taskflow_backend.dto.OtpVerifyRequest;
import com.example.taskflow_backend.dto.RegisterRequest;
import com.example.taskflow_backend.dto.UserResponse;
import com.example.taskflow_backend.enums.AccountStatus;
import com.example.taskflow_backend.enums.OtpPurpose;
import com.example.taskflow_backend.exception.ApiValidationException;
import com.example.taskflow_backend.exception.InvalidOtpException;
import com.example.taskflow_backend.model.User;
import com.example.taskflow_backend.repository.UserRepository;
import com.example.taskflow_backend.security.RateLimiterService;
import com.example.taskflow_backend.service.impl.AuthServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private OtpService otpService;
    @Mock
    private EmailService emailService;
    @Mock
    private RateLimiterService rateLimiterService;
    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private HttpServletResponse httpResponse;
    @Mock
    private HttpSession session;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties(
                new AppProperties.Mail("test@taskflow.dev", "Taskflow"),
                new AppProperties.Otp(10, 60, 5, "secret-test-key"),
                new AppProperties.Cors(java.util.List.of("http://localhost:4200")),
                new AppProperties.RateLimit(5, 15, 10, 15, 5, 15)
        );

        authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                authenticationManager,
                otpService,
                emailService,
                rateLimiterService,
                appProperties
        );
    }

    @Test
    @DisplayName("Registration throws exception when passwords do not match")
    void registerPasswordMismatch() {
        RegisterRequest req = new RegisterRequest("Test User", "test@example.com", "Password123!", "Mismatch123!");
        assertThrows(ApiValidationException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registration successfully creates unverified user and sends OTP email")
    void registerSuccess() {
        RegisterRequest req = new RegisterRequest("Test User", "test@example.com", "Password123!", "Password123!");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pw");
        when(otpService.getExpiryMinutes()).thenReturn(10);
        when(otpService.getCooldownSeconds()).thenReturn(60);
        when(otpService.generateAndSaveOtp(any(), eq(OtpPurpose.EMAIL_VERIFICATION))).thenReturn("123456");

        User savedUser = new User();
        savedUser.setId("u-123");
        savedUser.setEmail("test@example.com");
        savedUser.setFullName("Test User");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        OtpIssuedResponse response = authService.register(req);

        assertNotNull(response);
        assertEquals("test@example.com", response.email());
        assertEquals(600, response.expiresInSeconds());
        verify(emailService).sendVerificationEmail(eq("test@example.com"), eq("Test User"), eq("123456"), eq(10));
    }

    @Test
    @DisplayName("verifyEmail establishes session and marks user verified")
    void verifyEmailSuccess() {
        OtpVerifyRequest req = new OtpVerifyRequest("test@example.com", "123456");

        User user = new User();
        user.setId("u-123");
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setEmailVerified(false);
        user.setStatus(AccountStatus.ACTIVE);
        user.setRoles(Set.of("ROLE_USER"));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(httpRequest.getSession(false)).thenReturn(null);
        when(httpRequest.getSession(true)).thenReturn(session);

        UserResponse res = authService.verifyEmail(req, httpRequest, httpResponse);

        assertNotNull(res);
        assertTrue(user.isEmailVerified());
        verify(otpService).verifyOtp(user, OtpPurpose.EMAIL_VERIFICATION, "123456");
        verify(userRepository).save(user);
    }
}
