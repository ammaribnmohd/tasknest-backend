package com.example.taskflow_backend.exception;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        int status,
        String error,
        String code,
        String message,
        Instant timestamp,
        Map<String, String> validationErrors
) {
    public ApiError(int status, String error, String code, String message) {
        this(status, error, code, message, Instant.now(), null);
    }

    public ApiError(int status, String error, String code, String message, Map<String, String> validationErrors) {
        this(status, error, code, message, Instant.now(), validationErrors);
    }
}
