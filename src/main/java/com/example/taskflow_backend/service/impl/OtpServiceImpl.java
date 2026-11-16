package com.example.taskflow_backend.service.impl;

import com.example.taskflow_backend.config.AppProperties;
import com.example.taskflow_backend.enums.OtpPurpose;
import com.example.taskflow_backend.exception.InvalidOtpException;
import com.example.taskflow_backend.exception.TooManyRequestsException;
import com.example.taskflow_backend.model.OtpChallenge;
import com.example.taskflow_backend.model.User;
import com.example.taskflow_backend.repository.OtpChallengeRepository;
import com.example.taskflow_backend.service.OtpService;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class OtpServiceImpl implements OtpService {

    private final OtpChallengeRepository otpChallengeRepository;
    private final AppProperties appProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpServiceImpl(OtpChallengeRepository otpChallengeRepository, AppProperties appProperties) {
        this.otpChallengeRepository = otpChallengeRepository;
        this.appProperties = appProperties;
    }

    @Override
    public String generateAndSaveOtp(User user, OtpPurpose purpose) {
        Instant now = Instant.now();

        // Check if there is an existing challenge within cooldown
        Optional<OtpChallenge> existing = otpChallengeRepository.findByUserIdAndPurpose(user.getId(), purpose);
        if (existing.isPresent()) {
            OtpChallenge ch = existing.get();
            if (ch.getResendAvailableAt() != null && ch.getResendAvailableAt().isAfter(now)) {
                long waitSeconds = Duration.between(now, ch.getResendAvailableAt()).toSeconds() + 1;
                throw new TooManyRequestsException("Please wait " + waitSeconds + " seconds before requesting a new code.", waitSeconds);
            }
        }

        // Generate 6-digit cryptographically secure code
        int num = 100000 + secureRandom.nextInt(900000);
        String code = String.valueOf(num);

        String otpHash = computeHmac(user.getId(), purpose.name(), code);

        int expiryMinutes = appProperties.otp().expiryMinutes();
        int cooldownSeconds = appProperties.otp().resendCooldownSeconds();

        OtpChallenge challenge = existing.orElseGet(OtpChallenge::new);
        challenge.setUserId(user.getId());
        challenge.setPurpose(purpose);
        challenge.setOtpHash(otpHash);
        challenge.setCreatedAt(now);
        challenge.setExpiresAt(now.plus(Duration.ofMinutes(expiryMinutes)));
        challenge.setResendAvailableAt(now.plus(Duration.ofSeconds(cooldownSeconds)));
        challenge.setFailedAttempts(0);
        challenge.setConsumedAt(null);

        otpChallengeRepository.save(challenge);
        return code;
    }

    @Override
    public void verifyOtp(User user, OtpPurpose purpose, String code) {
        if (code == null || code.trim().length() != 6) {
            throw new InvalidOtpException("Verification code must be 6 digits.");
        }

        Instant now = Instant.now();
        OtpChallenge challenge = otpChallengeRepository.findByUserIdAndPurpose(user.getId(), purpose)
                .orElseThrow(() -> new InvalidOtpException("No verification code requested or it has expired."));

        if (challenge.getExpiresAt().isBefore(now)) {
            otpChallengeRepository.deleteByUserIdAndPurpose(user.getId(), purpose);
            throw new InvalidOtpException("Verification code has expired. Please request a new one.");
        }

        int maxAttempts = appProperties.otp().maxAttempts();
        if (challenge.getFailedAttempts() >= maxAttempts) {
            otpChallengeRepository.deleteByUserIdAndPurpose(user.getId(), purpose);
            throw new TooManyRequestsException("Maximum attempts exceeded. Please request a new code.", 60);
        }

        String expectedHash = challenge.getOtpHash();
        String actualHash = computeHmac(user.getId(), purpose.name(), code.trim());

        boolean matches = MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.UTF_8),
                actualHash.getBytes(StandardCharsets.UTF_8)
        );

        if (!matches) {
            int newAttempts = challenge.getFailedAttempts() + 1;
            challenge.setFailedAttempts(newAttempts);
            otpChallengeRepository.save(challenge);

            int remaining = maxAttempts - newAttempts;
            if (remaining <= 0) {
                otpChallengeRepository.deleteByUserIdAndPurpose(user.getId(), purpose);
                throw new TooManyRequestsException("Maximum attempts exceeded. Please request a new code.", 60);
            }
            throw new InvalidOtpException("Invalid code. " + remaining + " attempts remaining.");
        }

        // Successfully verified
        otpChallengeRepository.deleteByUserIdAndPurpose(user.getId(), purpose);
    }

    @Override
    public int getCooldownSeconds() {
        return appProperties.otp().resendCooldownSeconds();
    }

    @Override
    public int getExpiryMinutes() {
        return appProperties.otp().expiryMinutes();
    }

    private String computeHmac(String userId, String purpose, String code) {
        try {
            String secret = appProperties.otp().hmacSecret();
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);

            String payload = userId + ":" + purpose + ":" + code;
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate OTP HMAC", e);
        }
    }
}
