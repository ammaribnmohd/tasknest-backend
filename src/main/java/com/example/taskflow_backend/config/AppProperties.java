package com.example.taskflow_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Typed binding for every "app.*" property in application.properties. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Mail mail, Otp otp, Cors cors, RateLimit rateLimit) {

    public record Mail(String from, String fromName, String brevoApiKey) {
        public Mail(String from, String fromName) {
            this(from, fromName, "");
        }
    }

    public record Otp(int expiryMinutes, int resendCooldownSeconds, int maxAttempts, String hmacSecret) {}

    public record Cors(List<String> allowedOrigins) {}

    public record RateLimit(
            int loginMax, int loginWindowMinutes,
            int otpVerifyMax, int otpVerifyWindowMinutes,
            int otpRequestMax, int otpRequestWindowMinutes) {}
}

