package com.plr.aduaja.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// ============================================================
// INHERITANCE (Pewarisan): OtpVerification extends BaseEntity
// Menyimpan kode OTP untuk verifikasi registrasi & reset password
// ENKAPSULASI: semua field adalah PRIVATE
// ============================================================
@Entity
@Table(name = "otp_verifications")
public class OtpVerification extends BaseEntity {  // ← INHERITANCE sejati

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "otp_id")
    private String otpId;

    // HAS-A (Composition) dengan User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "otp_type", nullable = false)
    private OtpType otpType = OtpType.REGISTRATION;

    // ============================================================
    // ENUM — Jenis OTP
    // ============================================================
    public enum OtpType {
        REGISTRATION, FORGOT_PASSWORD, LOGIN, ADMIN_ACTIVATION
    }

    // ENKAPSULASI: Getter & Setter untuk semua field PRIVATE
    public String getOtpId() { return otpId; }
    public void setOtpId(String otpId) { this.otpId = otpId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }

    public Boolean getIsUsed() { return isUsed; }
    public void setIsUsed(Boolean isUsed) { this.isUsed = isUsed; }

    public OtpType getOtpType() { return otpType; }
    public void setOtpType(OtpType otpType) { this.otpType = otpType; }
}
