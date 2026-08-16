package com.plr.aduaja.controller;

import com.plr.aduaja.model.User;
import com.plr.aduaja.model.OtpVerification;
import com.plr.aduaja.dto.LoginDTO;
import com.plr.aduaja.dto.RegisterDTO;
import com.plr.aduaja.dto.ProfileDTO;
import com.plr.aduaja.dto.ResetPasswordDTO;
import com.plr.aduaja.model.UserProfile;
import com.plr.aduaja.service.StorageService;
import com.plr.aduaja.service.UserService;
import com.plr.aduaja.service.AuthService;
import com.plr.aduaja.service.OtpService;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

// ============================================================
// ABSTRACTION (Abstraksi): Controller hanya tahu Interface Service
// Controller TIDAK mengakses Repository langsung
// Controller TIDAK tahu bagaimana password di-hash, OTP dibuat, dll.
// ============================================================
@Slf4j
@Controller
public class WargaAuthController {

    // ABSTRACTION: hanya inject Interface, bukan implementasi
    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ==========================================
    // GET /warga/login — Halaman login warga
    // ==========================================
    @GetMapping("/warga/login")
    public String loginPage(Model model,
                            @RequestParam(value = "register", required = false) String register,
                            @RequestParam(value = "error", required = false) String error) {
        model.addAttribute("loginDTO", new LoginDTO());
        model.addAttribute("registerDTO", new RegisterDTO());
        if (error != null) {
            String msg = switch (error) {
                case "google_error" -> "Gagal mendapatkan data dari Google. Silakan coba lagi.";
                case "user_not_found" -> "Akun tidak ditemukan. Silakan daftar terlebih dahulu.";
                case "oauth2_failed" -> "Login Google gagal. Pastikan akun Google Anda valid atau coba lagi nanti.";
                default -> "Login gagal. Silakan coba lagi.";
            };
            model.addAttribute("error", msg);
        }
        return "warga/login";
    }

    // ==========================================
    // POST /warga/login — Proses login warga
    // ==========================================
    @PostMapping("/warga/login")
    public String login(@RequestParam("email") String email,
                        @RequestParam("password") String password,
                        HttpSession session,
                        HttpServletRequest request,
                        RedirectAttributes redirectAttributes) {

        // ABSTRACTION: Controller tidak tahu detail proses autentikasi
        LoginDTO dto = new LoginDTO();
        dto.setEmail(email);
        dto.setPassword(password);

        String ipAddress = getClientIp(request);
        Optional<User> userOpt = authService.login(dto, ipAddress);

        if (userOpt.isEmpty()) {
            // Cek apakah akun PENDING (belum verifikasi OTP)
            Optional<User> pendingUser = userService.findByEmail(email.trim());
            if (pendingUser.isPresent() && pendingUser.get().getAccountStatus() == User.AccountStatus.PENDING) {
                redirectAttributes.addFlashAttribute("warning",
                    "Akun Anda belum aktif. Silakan verifikasi OTP terlebih dahulu.");
                redirectAttributes.addAttribute("userId", pendingUser.get().getUserId());
                redirectAttributes.addAttribute("email", email);
                return "redirect:/warga/verify-otp?userId=" + pendingUser.get().getUserId() + "&email=" + email;
            }
            redirectAttributes.addFlashAttribute("error",
                "Email atau password salah.");
            return "redirect:/warga/login";
        }

        User user = userOpt.get();

        // Validasi role
        if (user.getRole() != User.Role.WARGA) {
            redirectAttributes.addFlashAttribute("error", "Akun ini bukan akun warga.");
            return "redirect:/warga/login";
        }

        // Simpan session
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("userName", user.getFullName());
        session.setAttribute("userRole", "WARGA");

        return "redirect:/warga/dashboard";
    }

    // ==========================================
    // GET /warga/register — Halaman registrasi (redirect ke login dengan mode register)
    // ==========================================
    @GetMapping("/warga/register")
    public String registerPage(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        return "warga/register";
    }

    // ==========================================
    // POST /warga/register — Proses registrasi warga baru
    // ==========================================
    @PostMapping("/warga/register")
    public String register(@ModelAttribute RegisterDTO dto,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        // Validasi password match
        if (!dto.isPasswordMatch()) {
            model.addAttribute("error", "Password tidak cocok");
            model.addAttribute("registerDTO", dto);
            return "warga/register";
        }

        // Validasi NIK 16 digit
        if (!dto.isNikValid()) {
            model.addAttribute("error", "NIK harus 16 digit angka");
            model.addAttribute("registerDTO", dto);
            return "warga/register";
        }

        // Validasi password minimal 8 karakter
        if (!dto.isPasswordStrong()) {
            model.addAttribute("error", "Password minimal 8 karakter");
            model.addAttribute("registerDTO", dto);
            return "warga/register";
        }

        // Cek apakah email sudah terdaftar — handle per status
        Optional<User> existingUser = userService.findByEmail(dto.getEmail().trim());
        if (existingUser.isPresent()) {
            if (existingUser.get().getAccountStatus() == User.AccountStatus.PENDING) {
                // PENDING: update data baru + kirim ulang OTP
                try {
                    userService.updatePendingRegistration(dto, existingUser.get().getUserId());
                    otpService.generateOtp(existingUser.get().getUserId(), OtpVerification.OtpType.REGISTRATION);
                    redirectAttributes.addFlashAttribute("success",
                        "Data registrasi diperbarui! Silakan cek email untuk kode OTP baru.");
                    return "redirect:/warga/verify-otp?userId=" + existingUser.get().getUserId() + "&email=" + dto.getEmail();
                } catch (RuntimeException e) {
                    model.addAttribute("error", e.getMessage());
                    model.addAttribute("registerDTO", dto);
                    return "warga/register";
                }
            }
            // ACTIVE/SUSPENDED: tolak
            model.addAttribute("error", "Email sudah terdaftar");
            model.addAttribute("registerDTO", dto);
            return "warga/register";
        }

        try {
            // ABSTRACTION: createUser menyembunyikan detail hashing, save profile, dll.
            User user = userService.createUser(dto);

            // Generate OTP untuk verifikasi registrasi
            // ABSTRACTION: Controller tidak tahu cara OTP dibuat
            otpService.generateOtp(user.getUserId(), OtpVerification.OtpType.REGISTRATION);

            redirectAttributes.addFlashAttribute("success",
                "Registrasi berhasil! Kode OTP telah dikirim ke email. Masukkan kode OTP untuk mengaktifkan akun.");
            return "redirect:/warga/verify-otp?userId=" + user.getUserId() + "&email=" + dto.getEmail();

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("registerDTO", dto);
            return "warga/register";
        }
    }

    // ==========================================
    // GET /warga/verify-otp — Halaman verifikasi OTP
    // ==========================================
    @GetMapping("/warga/verify-otp")
    public String verifyOtpPage(@RequestParam("userId") String userId,
                                @RequestParam("email") String email,
                                Model model) {
        model.addAttribute("userId", userId);
        model.addAttribute("email", email);
        return "warga/verify-otp";
    }

    // ==========================================
    // POST /warga/verify-otp — Proses verifikasi OTP
    // ==========================================
    @PostMapping("/warga/verify-otp")
    public String verifyOtp(@RequestParam("userId") String userId,
                            @RequestParam("email") String email,
                            @RequestParam("otpCode") String otpCode,
                            RedirectAttributes redirectAttributes) {
        // ABSTRACTION: Controller tidak tahu detail cara OTP diverifikasi
        boolean success = otpService.verifyOtp(userId, otpCode);

        if (success) {
            redirectAttributes.addFlashAttribute("success",
                "Verifikasi berhasil! Silakan login.");
            return "redirect:/warga/login";
        } else {
            redirectAttributes.addFlashAttribute("error",
                "Kode OTP tidak valid atau sudah kadaluarsa.");
            return "redirect:/warga/verify-otp?userId=" + userId + "&email=" + email;
        }
    }

    // ==========================================
    // GET /warga/profile — Lihat profil warga
    // ==========================================
    @GetMapping("/warga/profile")
    public String profilePage(HttpSession session, Model model) {
        String userId = ControllerHelper.requireRole(session, "WARGA");
        if (userId == null) {
            return "redirect:/warga/login";
        }

        // ABSTRACTION: Controller hanya tahu findById dari interface
        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            session.invalidate();
            return "redirect:/warga/login";
        }

        User user = userOpt.get();
        model.addAttribute("user", user);

        // Siapkan ProfileDTO dengan data current
        ProfileDTO profileDTO = new ProfileDTO();
        profileDTO.setFullName(user.getFullName());
        profileDTO.setEmail(user.getEmail());
        profileDTO.setPhoneNumber(user.getPhoneNumber());

        // Load data profil dari UserProfile
        if (user.getUserProfile() != null) {
            profileDTO.setNik(user.getUserProfile().getNik());
            profileDTO.setAlamatLengkap(user.getUserProfile().getAlamatLengkap());
            if (user.getUserProfile().getDomisiliLatitude() != null) {
                profileDTO.setDomisiliLatitude(user.getUserProfile().getDomisiliLatitude().toPlainString());
            }
            if (user.getUserProfile().getDomisiliLongitude() != null) {
                profileDTO.setDomisiliLongitude(user.getUserProfile().getDomisiliLongitude().toPlainString());
            }
        }

        model.addAttribute("profileDTO", profileDTO);
        return "warga/profile";
    }

    // ==========================================
    // POST /warga/profile/edit — Simpan perubahan profil
    // ==========================================
    @PostMapping("/warga/profile/edit")
    public String editProfile(@ModelAttribute ProfileDTO dto,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        String userId = ControllerHelper.requireRole(session, "WARGA");
        if (userId == null) {
            return "redirect:/warga/login";
        }

        try {
            // ABSTRACTION: updateProfile menyembunyikan detail update Entity
            User user = userService.updateProfile(userId, dto);
            session.setAttribute("userName", user.getFullName());
            redirectAttributes.addFlashAttribute("success", "Profil berhasil diperbarui.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/warga/profile";
    }

    // ==========================================
    // POST /warga/profile/photo — Upload foto profil ke Supabase
    // ==========================================
    @PostMapping("/warga/profile/photo")
    public String uploadProfilePhoto(@RequestParam("photo") MultipartFile file,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) return "redirect:/warga/login";

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Pilih file foto terlebih dahulu.");
            return "redirect:/warga/profile";
        }

        try {
            String photoUrl = storageService.upload(file, "profile");

            User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
            UserProfile profile = user.getUserProfile();
            if (profile == null) {
                profile = new UserProfile();
                profile.setUser(user);
            }
            profile.setProfilePhotoUrl(photoUrl);
            user.setUserProfile(profile);
            userService.updateUser(user);

            redirectAttributes.addFlashAttribute("success", "Foto profil berhasil diperbarui.");
        } catch (Exception e) {
            log.error("Gagal upload foto profil: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Gagal mengupload foto: " + e.getMessage());
        }

        return "redirect:/warga/profile";
    }

    // ==========================================
    // POST /warga/logout — Logout warga
    // ==========================================
    @PostMapping("/warga/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/warga/login";
    }

    // ==========================================
    // GET /warga/forgot-password — Halaman lupa password
    // ==========================================
    @GetMapping("/warga/forgot-password")
    public String forgotPasswordPage(Model model) {
        model.addAttribute("step", "email");
        return "warga/forgot-password";
    }

    // ==========================================
    // POST /warga/forgot-password — Kirim OTP ke email
    // ==========================================
    @PostMapping("/warga/forgot-password")
    public String forgotPasswordRequest(
            @RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {
        try {
            Optional<User> userOpt = userService.findByEmail(email.trim());
            if (userOpt.isEmpty()) {
                // Jangan beri tahu email tidak ada (keamanan)
                redirectAttributes.addFlashAttribute("success",
                    "Jika email terdaftar, kode OTP telah dikirim. Masukkan kode OTP di bawah.");
                return "redirect:/warga/forgot-password/verify?email=" + email;
            }
            otpService.generateOtpForPasswordReset(email.trim());
            redirectAttributes.addFlashAttribute("success",
                "Kode OTP telah dikirim. Masukkan kode OTP untuk melanjutkan.");
            return "redirect:/warga/forgot-password/verify?email=" + email;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal mengirim OTP: " + e.getMessage());
            return "redirect:/warga/forgot-password";
        }
    }

    // ==========================================
    // GET /warga/forgot-password/verify — Halaman verifikasi OTP reset
    // ==========================================
    @GetMapping("/warga/forgot-password/verify")
    public String forgotPasswordVerifyPage(
            @RequestParam("email") String email,
            Model model) {
        model.addAttribute("email", email);
        model.addAttribute("resetDTO", new ResetPasswordDTO());
        return "warga/forgot-password-verify";
    }

    // ==========================================
    // POST /warga/forgot-password/verify — Verifikasi OTP & reset password
    // ==========================================
    @PostMapping("/warga/forgot-password/verify")
    public String forgotPasswordVerify(
            @RequestParam("email") String email,
            @RequestParam("otpCode") String otpCode,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmNewPassword") String confirmNewPassword,
            RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmNewPassword)) {
            redirectAttributes.addFlashAttribute("error", "Password baru dan konfirmasi tidak cocok.");
            return "redirect:/warga/forgot-password/verify?email=" + email;
        }
        if (newPassword.length() < 8) {
            redirectAttributes.addFlashAttribute("error", "Password minimal 8 karakter.");
            return "redirect:/warga/forgot-password/verify?email=" + email;
        }
        try {
            Optional<User> userOpt = userService.findByEmail(email.trim());
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Email tidak ditemukan.");
                return "redirect:/warga/forgot-password";
            }
            User user = userOpt.get();
            boolean valid = otpService.verifyOtp(user.getUserId(), otpCode);
            if (!valid) {
                redirectAttributes.addFlashAttribute("error", "Kode OTP tidak valid atau sudah kadaluarsa.");
                return "redirect:/warga/forgot-password/verify?email=" + email;
            }
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("success",
                "Password berhasil direset! Silakan login dengan password baru.");
            return "redirect:/warga/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal reset password: " + e.getMessage());
            return "redirect:/warga/forgot-password/verify?email=" + email;
        }
    }

    // ENKAPSULASI: method private — tidak bisa diakses dari luar
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
