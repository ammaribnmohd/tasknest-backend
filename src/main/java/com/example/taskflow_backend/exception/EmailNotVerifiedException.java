package com.example.taskflow_backend.exception;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() {
        super("Please verify your email address before signing in.");
    }

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
