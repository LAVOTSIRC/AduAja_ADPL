package com.plr.aduaja.service;

import com.plr.aduaja.model.OtpVerification;

// ============================================================
// ABSTRACTION (Abstraksi): OtpService Interface sebagai kontrak
// Controller hanya tahu interface ini, tidak tahu implementasinya
//
// POLYMORPHISM (Compile-time / Overloading):
// - generateOtp(String userId, OtpType type)
// - generateOtpForRegistration(String email)  ← Overload
// - generateOtpForPasswordReset(String email) ← Overload
// - verifyOtp(String userId, String otpCode)
// - verifyOtpByEmail(String email, String otpCode)  ← Overload
// ============================================================
public interface OtpService {

    // ===========================
    // OVERLOADING (Compile-time Polymorphism)
    // Generate OTP dengan cara berbeda
    // ===========================
    OtpVerification generateOtp(String userId, OtpVerification.OtpType type);
    OtpVerification generateOtpForRegistration(String email);     // ← Overload
    OtpVerification generateOtpForPasswordReset(String email);    // ← Overload

    // ===========================
    // OVERLOADING (Compile-time Polymorphism)
    // Verify OTP dengan cara berbeda
    // ===========================
    boolean verifyOtp(String userId, String otpCode);
    boolean verifyOtpByEmail(String email, String otpCode);       // ← Overload
    OtpVerification verifyOtpWithoutActivation(String userId, String otpCode); // ← Overload: verifikasi tanpa aktivasi akun

    OtpVerification getActiveOtp(String userId);
    void invalidateOtp(String otpId);
    boolean isOtpExpired(String otpId);
}
