package com.example.taskflow_backend.model;

import com.example.taskflow_backend.enums.AccountStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String fullName;

    /** Always stored trimmed + lower-cased. The unique index guards against duplicate/concurrent sign-ups. */
    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private Set<String> roles = new HashSet<>(Set.of("ROLE_USER"));

    private boolean emailVerified;

    private AccountStatus status = AccountStatus.ACTIVE;

    /** Incremented on password reset; sessions created with an older value are rejected. */
    private int authVersion;

    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }

    public int getAuthVersion() { return authVersion; }
    public void setAuthVersion(int authVersion) { this.authVersion = authVersion; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

