package com.plr.aduaja.controller;

import com.plr.aduaja.model.OtpVerification;
import com.plr.aduaja.model.User;
import com.plr.aduaja.dto.LoginDTO;
import com.plr.aduaja.service.AuthService;
import com.plr.aduaja.service.OtpService;
import com.plr.aduaja.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.Optional;

// ============================================================
// ABSTRACTION (Abstraksi): AdminAuthController hanya tahu Interface Service
// Controller TIDAK mengakses Repository atau implementasi langsung
// ============================================================
@Slf4j
@Controller
public class AdminAuthController {

    // ABSTRACTION: hanya inject Interface
    @Autowired
    private AuthService authService;  // ← Abstraction: hanya tahu interface

    @Autowired
    private UserService userService;

    @Autowired
    private OtpService otpService;

    // ==========================================
    // GET /admin/login — Halaman login admin
    // ==========================================
    @GetMapping("/admin/login")
    public String loginPage(Model model,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "error", required = false) String error) {
        model.addAttribute("loginDTO", new LoginDTO());
        if (logout != null) {
            model.addAttribute("info", "Anda berhasil logout.");
        }
        if (error != null) {
            model.addAttribute("error", "Sesi berakhir atau terjadi kesalahan. Silakan login ulang.");
        }
        return "admin/login";
    }

    // ==========================================
    // POST /admin/login — Proses login admin
    // ==========================================
    @PostMapping("/admin/login")
    public String login(@RequestParam("email") String email,
                        @RequestParam("password") String password,
                        HttpSession session,
                        HttpServletRequest request,
                        RedirectAttributes redirectAttributes) {

        // ENKAPSULASI: buat LoginDTO, bukan kirim data mentah
        LoginDTO dto = new LoginDTO();
        dto.setEmail(email);
        dto.setPassword(password);

        String ipAddress = getClientIp(request);

        // ABSTRACTION: Controller tidak tahu detail proses autentikasi
        Optional<User> userOpt = authService.login(dto, ipAddress);

        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Email atau password salah.");
            return "redirect:/admin/login";
        }

        User user = userOpt.get();

        // Validasi role — hanya admin yang bisa login di sini
        if (user.getRole() != User.Role.ADMIN_PUSAT && user.getRole() != User.Role.ADMIN_DINAS) {
            redirectAttributes.addFlashAttribute("error", "Akun ini bukan akun admin.");
            return "redirect:/admin/login";
        }

        // Jika status PENDING (login pertama)
        if (user.getAccountStatus() == User.AccountStatus.PENDING) {
            // FR-AKN-09: Cek masa berlaku 24 jam password sementara
            if (user.getCreatedAt() != null && user.getCreatedAt().plusHours(24).isBefore(LocalDateTime.now())) {
                redirectAttributes.addFlashAttribute("error", "Password sementara sudah expired (lebih dari 24 jam). Hubungi Super Admin untuk reset akun.");
                return "redirect:/admin/login";
            }
            // FR-AKN-10: Kirim OTP untuk verifikasi login pertama
            session.setAttribute("pendingOtpUserId", user.getUserId());
            session.setAttribute("pendingOtpEmail", user.getEmail());
            try {
                otpService.generateOtp(user.getUserId(), OtpVerification.OtpType.ADMIN_ACTIVATION);
            } catch (Exception e) {
                log.error("Gagal kirim OTP admin: {}", e.getMessage(), e);
            }
            return "redirect:/admin/verify-otp";
        }

        // Simpan session
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("userName", user.getFullName());
        session.setAttribute("userRole", user.getRole().toString());

        if (user.getRole() == User.Role.ADMIN_DINAS && user.getAgency() != null) {
            session.setAttribute(ControllerHelper.SESSION_AGENCY_ID, user.getAgency().getAgencyId());
            session.setAttribute(ControllerHelper.SESSION_AGENCY_NAME, user.getAgency().getAgencyName());
            if (user.getAgency().getRegion() != null) {
                session.setAttribute(ControllerHelper.SESSION_REGION_ID, user.getAgency().getRegion().getRegionId());
                session.setAttribute(ControllerHelper.SESSION_REGION_NAME, user.getAgency().getRegion().getRegionName());
            }
        }

        if (user.getRole() == User.Role.ADMIN_PUSAT && user.getRegion() != null) {
            session.setAttribute(ControllerHelper.SESSION_REGION_ID, user.getRegion().getRegionId());
            session.setAttribute(ControllerHelper.SESSION_REGION_NAME, user.getRegion().getRegionName());
        }

        // Redirect ke dashboard yang sesuai role
        if (user.getRole() == User.Role.ADMIN_DINAS) {
            return "redirect:/admin/dinas/dashboard";
        }
        return "redirect:/admin/dashboard";
    }

    // ==========================================
    // POST /admin/logout — Logout admin
    // ==========================================
    @PostMapping("/admin/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/login?logout=true";
    }

    // ==========================================
    // GET /admin/change-password — Paksa ganti password admin
    // ==========================================
    @GetMapping("/admin/change-password")
    public String adminChangePasswordPage(HttpSession session, Model model) {
        String userId = (String) session.getAttribute("forceChangePasswordUserId");
        if (userId == null) {
            return "redirect:/admin/login";
        }
        return "admin/change-password";
    }

    // ==========================================
    // POST /admin/change-password — Proses ganti password admin
    // ==========================================
    @PostMapping("/admin/change-password")
    public String adminChangePasswordPost(
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String userId = (String) session.getAttribute("forceChangePasswordUserId");
        if (userId == null) {
            return "redirect:/admin/login";
        }

        if (newPassword == null || newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Password minimal 6 karakter.");
            return "redirect:/admin/change-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Password tidak cocok.");
            return "redirect:/admin/change-password";
        }

        try {
            userService.changePassword(userId, newPassword);
            session.removeAttribute("forceChangePasswordUserId");
            redirectAttributes.addFlashAttribute("info", "Password berhasil diubah. Silakan login dengan password baru.");
            return "redirect:/admin/login";
        } catch (Exception e) {
            log.error("Gagal ganti password admin: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Gagal mengubah password.");
            return "redirect:/admin/change-password";
        }
    }

    // ==========================================
    // GET /admin/verify-otp — Halaman verifikasi OTP admin
    // ==========================================
    @GetMapping("/admin/verify-otp")
    public String adminVerifyOtpPage(HttpSession session, Model model) {
        String userId = (String) session.getAttribute("pendingOtpUserId");
        if (userId == null) {
            return "redirect:/admin/login";
        }
        model.addAttribute("email", session.getAttribute("pendingOtpEmail"));
        return "admin/verify-otp";
    }

    // ==========================================
    // POST /admin/verify-otp — Proses verifikasi OTP admin
    // ==========================================
    @PostMapping("/admin/verify-otp")
    public String adminVerifyOtpPost(
            @RequestParam("otpCode") String otpCode,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String userId = (String) session.getAttribute("pendingOtpUserId");
        if (userId == null) {
            return "redirect:/admin/login";
        }

        OtpVerification verified = otpService.verifyOtpWithoutActivation(userId, otpCode.trim());
        if (verified == null) {
            redirectAttributes.addFlashAttribute("error", "Kode OTP salah atau sudah expired.");
            return "redirect:/admin/verify-otp";
        }

        // OTP valid — redirect ke change-password
        session.removeAttribute("pendingOtpUserId");
        session.removeAttribute("pendingOtpEmail");
        session.setAttribute("forceChangePasswordUserId", userId);
        return "redirect:/admin/change-password";
    }

    // ==========================================
    // GET /petugas/login — Halaman login petugas
    // ==========================================
    @GetMapping("/petugas/login")
    public String petugasLoginPage(Model model) {
        model.addAttribute("loginDTO", new LoginDTO());
        return "petugas/login";
    }

    // ==========================================
    // POST /petugas/login — Proses login petugas
    // ==========================================
    @PostMapping("/petugas/login")
    public String petugasLogin(@RequestParam("email") String email,
                               @RequestParam("password") String password,
                               HttpSession session,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {

        LoginDTO dto = new LoginDTO();
        dto.setEmail(email);
        dto.setPassword(password);

        String ipAddress = getClientIp(request);
        Optional<User> userOpt = authService.login(dto, ipAddress);

        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Email atau password salah.");
            return "redirect:/petugas/login";
        }

        User user = userOpt.get();

        // Validasi role — hanya petugas yang bisa login di sini
        if (user.getRole() != User.Role.PETUGAS) {
            redirectAttributes.addFlashAttribute("error", "Akun ini bukan akun petugas.");
            return "redirect:/petugas/login";
        }

        // Jika status PENDING (login pertama), paksa ganti password
        if (user.getAccountStatus() == User.AccountStatus.PENDING) {
            session.setAttribute("forceChangePasswordUserId", user.getUserId());
            return "redirect:/petugas/change-password";
        }

        session.setAttribute("userId", user.getUserId());
        session.setAttribute("userName", user.getFullName());
        session.setAttribute("userRole", "PETUGAS");

        return "redirect:/petugas/dashboard";
    }

    // ==========================================
    // POST /petugas/logout — Logout petugas
    // ==========================================
    @PostMapping("/petugas/logout")
    public String petugasLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/petugas/login";
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
