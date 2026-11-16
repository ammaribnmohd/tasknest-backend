package com.example.taskflow_backend.model;

import com.example.taskflow_backend.enums.OtpPurpose;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * One active OTP challenge per (user, purpose). Issuing a new code atomically replaces the old one,
 * which is how older codes become "superseded". The plaintext code is never stored.
 */
@Document(collection = "otp_challenges")
@CompoundIndex(name = "user_purpose_unique", def = "{'userId': 1, 'purpose': 1}", unique = true)
public class OtpChallenge {

    @Id
    private String id;

    private String userId;
    private OtpPurpose purpose;

    /** HMAC-SHA256 of the code (bound to user + purpose). */
    private String otpHash;

    /** TTL index removes the document shortly after expiry; expiry is ALSO enforced in code. */
    @Indexed(expireAfter = "0s")
    private Instant expiresAt;

    private int failedAttempts;
    private Instant createdAt;
    private Instant consumedAt;
    private Instant resendAvailableAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public OtpPurpose getPurpose() { return purpose; }
    public void setPurpose(OtpPurpose purpose) { this.purpose = purpose; }

    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getConsumedAt() { return consumedAt; }
    public void setConsumedAt(Instant consumedAt) { this.consumedAt = consumedAt; }

    public Instant getResendAvailableAt() { return resendAvailableAt; }
    public void setResendAvailableAt(Instant resendAvailableAt) { this.resendAvailableAt = resendAvailableAt; }
}

