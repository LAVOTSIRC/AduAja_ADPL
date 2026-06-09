package com.plr.aduaja.service;

import com.plr.aduaja.model.User;
import com.plr.aduaja.model.OtpVerification;
import com.plr.aduaja.repository.UserRepository;
import com.plr.aduaja.repository.OtpVerificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

// ============================================================
// POLYMORPHISM (Run-time Polymorphism): OtpServiceImpl
// Mengimplementasikan OtpService interface → @Override setiap method
//
// ABSTRACTION: Controller tidak perlu tahu:
// - Bagaimana kode OTP dibuat (6 digit random)
// - Bagaimana validasi expiry OTP
// - Bagaimana OTP diinvalidasi setelah digunakan
// ============================================================
@Slf4j
@Service
@Transactional
public class OtpServiceImpl implements OtpService {  // ← POLYMORPHISM

    private static final int OTP_VALIDITY_MINUTES = 10;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpVerificationRepository otpRepository;

    @Autowired
    private EmailService emailService;

    private final Random random = new Random();

    @Override  // ← POLYMORPHISM: Override dari interface
    public OtpVerification generateOtp(String userId, OtpVerification.OtpType type) {
        // ABSTRACTION: Detail pembuatan OTP disembunyikan
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        String code = generateRandomOtp();
        log.info("Generate OTP untuk userId={}, type={}, code={}", userId, type, code);

        OtpVerification otp = new OtpVerification();
        otp.setUser(user);
        otp.setOtpCode(code);  // ENKAPSULASI: method private
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));
        otp.setOtpType(type);
        otp.setIsVerified(false);
        otp.setIsUsed(false);

        OtpVerification saved = otpRepository.save(otp);

        // Kirim OTP ke email user (async — berjalan di background thread)
        emailService.sendOtpEmail(user.getEmail(), saved.getOtpCode(), type);

        return saved;
    }

    @Override  // ← POLYMORPHISM: Override dari interface (Overload)
    public OtpVerification generateOtpForRegistration(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User dengan email ini tidak ditemukan");
        }
        return generateOtp(userOpt.get().getUserId(), OtpVerification.OtpType.REGISTRATION);
    }

    @Override  // ← POLYMORPHISM: Override dari interface (Overload)
    public OtpVerification generateOtpForPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User dengan email ini tidak ditemukan");
        }
        return generateOtp(userOpt.get().getUserId(), OtpVerification.OtpType.FORGOT_PASSWORD);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public boolean verifyOtp(String userId, String otpCode) {
        log.info("Verify OTP userId={}, code={}", userId, otpCode);
        Optional<OtpVerification> otpOpt = otpRepository
            .findByUserUserIdAndOtpCodeAndIsUsedFalse(userId, otpCode);

        if (otpOpt.isEmpty()) {
            log.warn("OTP tidak ditemukan untuk userId={}, code={}", userId, otpCode);
            return false;
        }

        OtpVerification otp = otpOpt.get();
        log.info("OTP ditemukan: id={}, expiresAt={}, type={}", otp.getOtpId(), otp.getExpiresAt(), otp.getOtpType());

        // Cek apakah OTP sudah kadaluarsa
        if (isOtpExpired(otp.getOtpId())) {
            log.warn("OTP expired: id={}", otp.getOtpId());
            return false;
        }

        // Tandai OTP sudah diverifikasi dan digunakan
        otp.setIsVerified(true);
        otp.setIsUsed(true);
        otpRepository.save(otp);

        // Aktifkan akun user (status PENDING → ACTIVE)
        User user = otp.getUser();
        if (user.getAccountStatus() == User.AccountStatus.PENDING) {
            user.setAccountStatus(User.AccountStatus.ACTIVE);
            userRepository.save(user);
        }

        return true;
    }

    @Override
    public OtpVerification verifyOtpWithoutActivation(String userId, String otpCode) {
        log.info("Verify OTP tanpa aktivasi userId={}, code={}", userId, otpCode);
        Optional<OtpVerification> otpOpt = otpRepository
            .findByUserUserIdAndOtpCodeAndIsUsedFalse(userId, otpCode);

        if (otpOpt.isEmpty()) {
            log.warn("OTP tidak ditemukan untuk userId={}, code={}", userId, otpCode);
            return null;
        }

        OtpVerification otp = otpOpt.get();
        log.info("OTP ditemukan: id={}, expiresAt={}, type={}", otp.getOtpId(), otp.getExpiresAt(), otp.getOtpType());

        if (isOtpExpired(otp.getOtpId())) {
            log.warn("OTP expired: id={}", otp.getOtpId());
            return null;
        }

        otp.setIsVerified(true);
        otp.setIsUsed(true);
        otpRepository.save(otp);

        return otp;
    }

    @Override  // ← POLYMORPHISM: Override dari interface (Overload)
    public boolean verifyOtpByEmail(String email, String otpCode) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }
        return verifyOtp(userOpt.get().getUserId(), otpCode);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public OtpVerification getActiveOtp(String userId) {
        return otpRepository.findByUserUserIdAndIsUsedFalse(userId)
            .filter(otp -> !otp.getIsVerified() && !isOtpExpired(otp.getOtpId()))
            .orElse(null);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public void invalidateOtp(String otpId) {
        otpRepository.findById(otpId).ifPresent(otp -> {
            otp.setIsUsed(true);
            otpRepository.save(otp);
        });
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public boolean isOtpExpired(String otpId) {
        return otpRepository.findById(otpId)
            .map(otp -> LocalDateTime.now().isAfter(otp.getExpiresAt()))
            .orElse(true);
    }

    // ENKAPSULASI: method private — tidak bisa diakses dari luar
    private String generateRandomOtp() {
        return String.format("%06d", random.nextInt(999999));
    }
}
