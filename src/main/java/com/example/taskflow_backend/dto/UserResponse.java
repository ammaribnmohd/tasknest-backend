package com.example.taskflow_backend.dto;

import com.example.taskflow_backend.model.User;
import com.example.taskflow_backend.security.AppUserDetails;

import java.util.List;

/** Safe subset of user data. Never includes password hash, OTP data or session secrets. */
public record UserResponse(String id, String fullName, String email, List<String> roles, boolean emailVerified) {

    public static UserResponse from(AppUserDetails user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getRoleNames().stream().sorted().toList(),
                user.isEmailVerified()
        );
    }

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRoles() == null ? List.of() : user.getRoles().stream().sorted().toList(),
                user.isEmailVerified()
        );
    }
}
