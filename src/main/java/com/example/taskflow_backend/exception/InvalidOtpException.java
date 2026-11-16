package com.example.taskflow_backend.exception;

/** Deliberately vague: expired, wrong, reused, superseded and locked-out codes all look the same. */
public class InvalidOtpException extends RuntimeException {
    public InvalidOtpException() {
        super("The code is invalid or has expired.");
    }

    public InvalidOtpException(String message) {
        super(message);
    }
}
