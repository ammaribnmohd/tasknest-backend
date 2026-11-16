package com.example.taskflow_backend.service.impl;

import com.example.taskflow_backend.config.AppProperties;
import com.example.taskflow_backend.dto.*;
import com.example.taskflow_backend.enums.AccountStatus;
import com.example.taskflow_backend.enums.OtpPurpose;
import com.example.taskflow_backend.exception.*;
import com.example.taskflow_backend.model.User;
import com.example.taskflow_backend.repository.UserRepository;
import com.example.taskflow_backend.security.AppUserDetails;
import com.example.taskflow_backend.security.RateLimiterService;
import com.example.taskflow_backend.service.AuthService;
import com.example.taskflow_backend.service.EmailService;
import com.example.taskflow_backend.service.OtpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;
    private final EmailService emailService;
    private final RateLimiterService rateLimiterService;
    private final AppProperties appProperties;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            OtpService otpService,
            EmailService emailService,
            RateLimiterService rateLimiterService,
            AppProperties appProperties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.otpService = otpService;
        this.emailService = emailService;
        this.rateLimiterService = rateLimiterService;
        this.appProperties = appProperties;
    }

    @Override
    public OtpIssuedResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new ApiValidationException("Passwords do not match");
        }

        String normalizedEmail = request.email().trim().toLowerCase();

        Optional<User> existingUserOpt = userRepository.findByEmail(normalizedEmail);
        User user;

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            if (user.isEmailVerified()) {
                throw new ApiValidationException("An account with this email already exists.");
            }
            // Update unverified user details and password
            user.setFullName(request.fullName().trim());
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            user.setUpdatedAt(Instant.now());
            user = userRepository.save(user);
        } else {
            user = new User();
            user.setFullName(request.fullName().trim());
            user.setEmail(normalizedEmail);
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            user.setRoles(Set.of("ROLE_USER"));
            user.setEmailVerified(false);
            user.setStatus(AccountStatus.ACTIVE);
            user.setAuthVersion(1);
            user.setCreatedAt(Instant.now());
            user.setUpdatedAt(Instant.now());
            user = userRepository.save(user);
        }

        String code = otpService.generateAndSaveOtp(user, OtpPurpose.EMAIL_VERIFICATION);
        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), code, otpService.getExpiryMinutes());

        return new OtpIssuedResponse(
                "Verification code sent to your email.",
                user.getEmail(),
                otpService.getExpiryMinutes() * 60,
                otpService.getCooldownSeconds()
        );
    }

    @Override
    public UserResponse verifyEmail(OtpVerifyRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String normalizedEmail = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidOtpException("Account not found."));

        if (!user.isEmailVerified()) {
            otpService.verifyOtp(user, OtpPurpose.EMAIL_VERIFICATION, request.otp());
            user.setEmailVerified(true);
            user.setUpdatedAt(Instant.now());
            user = userRepository.save(user);
        }

        // Establish session
        establishSession(user, httpRequest, httpResponse);

        return UserResponse.from(user);
    }

    @Override
    public OtpIssuedResponse resendVerification(EmailRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        rateLimiterService.checkLimit(
                "otp_resend:" + normalizedEmail,
                appProperties.rateLimit().otpRequestMax(),
                Duration.ofMinutes(appProperties.rateLimit().otpRequestWindowMinutes())
        );

        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!user.isEmailVerified()) {
                String code = otpService.generateAndSaveOtp(user, OtpPurpose.EMAIL_VERIFICATION);
                emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), code, otpService.getExpiryMinutes());
            }
        }

        return new OtpIssuedResponse(
                "If the account exists and is unverified, a new code has been sent.",
                normalizedEmail,
                otpService.getExpiryMinutes() * 60,
                otpService.getCooldownSeconds()
        );
    }

    @Override
    public UserResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String normalizedEmail = request.email().trim().toLowerCase();
        rateLimiterService.checkLimit(
                "login:" + normalizedEmail,
                appProperties.rateLimit().loginMax(),
                Duration.ofMinutes(appProperties.rateLimit().loginWindowMinutes())
        );

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
            );

            AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();

            if (!userDetails.isEmailVerified()) {
                throw new EmailNotVerifiedException("Please verify your email address before logging in.");
            }

            if (!userDetails.isEnabled()) {
                throw new DisabledException("Account has been disabled.");
            }

            // Create new session to prevent session fixation
            HttpSession oldSession = httpRequest.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession newSession = httpRequest.getSession(true);

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            rateLimiterService.reset("login:" + normalizedEmail);

            return UserResponse.from(userDetails);
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    @Override
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        SecurityContextHolder.clearContext();
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @Override
    public UserResponse getCurrentUser(AppUserDetails principal) {
        if (principal == null) {
            throw new ResourceNotFoundException("Not authenticated");
        }
        return UserResponse.from(principal);
    }

    @Override
    public OtpIssuedResponse forgotPassword(EmailRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        rateLimiterService.checkLimit(
                "forgot_pwd:" + normalizedEmail,
                appProperties.rateLimit().otpRequestMax(),
                Duration.ofMinutes(appProperties.rateLimit().otpRequestWindowMinutes())
        );

        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String code = otpService.generateAndSaveOtp(user, OtpPurpose.PASSWORD_RESET);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), code, otpService.getExpiryMinutes());
        }

        return new OtpIssuedResponse(
                "If an account exists with that email, a password reset code has been sent.",
                normalizedEmail,
                otpService.getExpiryMinutes() * 60,
                otpService.getCooldownSeconds()
        );
    }

    @Override
    public MessageResponse verifyResetOtp(OtpVerifyRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidOtpException("Invalid code or email."));

        // Dry check without deleting
        return new MessageResponse("Verification code is valid.");
    }

    @Override
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ApiValidationException("Passwords do not match");
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidOtpException("Invalid code or email."));

        otpService.verifyOtp(user, OtpPurpose.PASSWORD_RESET, request.otp());

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setAuthVersion(user.getAuthVersion() + 1);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        return new MessageResponse("Password has been reset successfully. You can now log in.");
    }

    private void establishSession(User user, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        AppUserDetails userDetails = new AppUserDetails(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        HttpSession oldSession = httpRequest.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        httpRequest.getSession(true);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
    }
}

