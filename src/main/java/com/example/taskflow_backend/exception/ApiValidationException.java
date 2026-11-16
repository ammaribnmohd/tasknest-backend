package com.example.taskflow_backend.exception;

import java.util.Map;

/** Business-rule validation failure that maps to a field (e.g. password confirmation mismatch). */
public class ApiValidationException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public ApiValidationException(String field, String message) {
        super(message);
        this.fieldErrors = Map.of(field, message);
    }

    public ApiValidationException(String message) {
        super(message);
        this.fieldErrors = Map.of("general", message);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
