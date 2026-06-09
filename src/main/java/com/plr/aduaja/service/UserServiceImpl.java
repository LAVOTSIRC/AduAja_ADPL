package com.plr.aduaja.service;

import com.plr.aduaja.model.*;
import com.plr.aduaja.repository.*;
import com.plr.aduaja.dto.CreateAdminDTO;
import com.plr.aduaja.dto.CreatePetugasDTO;
import com.plr.aduaja.dto.RegisterDTO;
import com.plr.aduaja.dto.ProfileDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// ============================================================
// POLYMORPHISM (Run-time Polymorphism): UserServiceImpl
// Mengimplementasikan UserService interface → @Override setiap method
//
// ABSTRACTION: Controller tidak perlu tahu implementasi ini,
// hanya tahu interface UserService
// ============================================================
@Slf4j
@Service
@Transactional
public class UserServiceImpl implements UserService {  // ← POLYMORPHISM

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private AgencyRepository agencyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    // CATATAN: @PostConstruct activatePendingUsers() dihapus.
    // Aktivasi akun HANYA dilakukan melalui OtpServiceImpl.verifyOtp()
    // setelah user berhasil verifikasi kode OTP.
    // Mengaktifkan semua PENDING otomatis akan mem-bypass proses verifikasi OTP.

    // ===========================
    // @Override — Run-time Polymorphism
    // Mengimplementasikan semua method dari UserService interface
    // ===========================

    @Override  // ← POLYMORPHISM: Override dari interface
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    @Override  // ← POLYMORPHISM: Override dari interface (Overload)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override  // ← POLYMORPHISM: Override dari interface (Overload)
    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    @Override  // ← POLYMORPHISM: Override dari interface (Overload)
    public List<User> findByRole(User.Role role) {
        return userRepository.findByRole(role);
    }

    @Override  // ← POLYMORPHISM: Override dari interface (Overload: 2 parameter)
    public List<User> findByRoleAndStatus(User.Role role, User.AccountStatus status) {
        return userRepository.findByRoleAndAccountStatus(role, status);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public User createUser(RegisterDTO dto) {
        // ABSTRACTION: Semua logika kompleks disembunyikan dari Controller
        // Email — controller sudah handle PENDING sebelum panggil method ini
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email sudah terdaftar");
        }
        // Phone — hanya ACTIVE/SUSPENDED yang dianggap konflik, PENDING dianggap bebas
        if (dto.getPhoneNumber() != null) {
            Optional<User> existingPhone = userRepository.findByPhoneNumber(dto.getPhoneNumber());
            if (existingPhone.isPresent() && existingPhone.get().getAccountStatus() != User.AccountStatus.PENDING) {
                throw new RuntimeException("Nomor HP sudah terdaftar");
            }
        }

        // Buat User baru
        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        // ENKAPSULASI: password di-hash, tidak pernah disimpan plaintext
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole(User.Role.WARGA);
        // Status PENDING: user harus verifikasi OTP dulu sebelum bisa login
        // OtpServiceImpl.verifyOtp() yang akan mengubah status ke ACTIVE
        user.setAccountStatus(User.AccountStatus.PENDING);

        User savedUser = userRepository.save(user);

        // Buat UserProfile dengan NIK
        UserProfile profile = new UserProfile();
        profile.setUser(savedUser);
        if (dto.getNik() != null && !dto.getNik().isBlank()) {
            // Cek NIK — hanya ACTIVE/SUSPENDED yang dianggap konflik
            Optional<UserProfile> existingProfile = userProfileRepository.findByNik(dto.getNik());
            if (existingProfile.isPresent()) {
                User profileOwner = existingProfile.get().getUser();
                if (profileOwner.getAccountStatus() != User.AccountStatus.PENDING) {
                    throw new RuntimeException("NIK sudah terdaftar");
                }
            }
            profile.setNik(dto.getNik());
        }
        userProfileRepository.save(profile);

        return savedUser;
    }

    @Override
    public User updatePendingRegistration(RegisterDTO dto, String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        if (user.getAccountStatus() != User.AccountStatus.PENDING) {
            throw new RuntimeException("Akun sudah aktif, tidak bisa update data registrasi");
        }

        // Update data user
        user.setFullName(dto.getFullName());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));

        // Phone: cek konflik hanya dengan ACTIVE/SUSPENDED
        if (dto.getPhoneNumber() != null) {
            Optional<User> existingPhone = userRepository.findByPhoneNumber(dto.getPhoneNumber());
            if (existingPhone.isPresent()
                && !existingPhone.get().getUserId().equals(userId)
                && existingPhone.get().getAccountStatus() != User.AccountStatus.PENDING) {
                throw new RuntimeException("Nomor HP sudah terdaftar");
            }
            user.setPhoneNumber(dto.getPhoneNumber());
        } else {
            user.setPhoneNumber(null);
        }

        User savedUser = userRepository.save(user);

        // Update profile & NIK
        UserProfile profile = userProfileRepository.findByUserUserId(userId).orElse(null);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(savedUser);
        }
        if (dto.getNik() != null && !dto.getNik().isBlank()) {
            Optional<UserProfile> existingProfile = userProfileRepository.findByNik(dto.getNik());
            if (existingProfile.isPresent()
                && !existingProfile.get().getUser().getUserId().equals(userId)
                && existingProfile.get().getUser().getAccountStatus() != User.AccountStatus.PENDING) {
                throw new RuntimeException("NIK sudah terdaftar");
            }
            profile.setNik(dto.getNik());
        } else {
            profile.setNik(null);
        }
        userProfileRepository.save(profile);

        return savedUser;
    }

    @Override
    public User createPetugas(CreatePetugasDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email sudah terdaftar");
        }
        // Tidak cek uniqueness nomor HP — satu orang boleh punya akun warga dan petugas
        // dengan nomor HP yang sama (kolom phone_number memang tidak unique di DB)

        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole(User.Role.PETUGAS);
        user.setAccountStatus(User.AccountStatus.PENDING);

        if (dto.getAgencyId() != null && !dto.getAgencyId().isBlank()) {
            Agency agency = agencyRepository.findById(dto.getAgencyId())
                    .orElseThrow(() -> new RuntimeException("Agency tidak ditemukan"));
            user.setAgency(agency);
        }

        User savedUser = userRepository.save(user);

        UserProfile profile = new UserProfile();
        profile.setUser(savedUser);
        if (dto.getNip() != null && !dto.getNip().isBlank()) {
            profile.setNip(dto.getNip());
        }
        if (dto.getWilayahTugasRegionId() != null && !dto.getWilayahTugasRegionId().isBlank()) {
            Region wilayah = regionRepository.findById(dto.getWilayahTugasRegionId())
                    .orElseThrow(() -> new RuntimeException("Wilayah tidak ditemukan"));
            profile.setWilayahTugas(wilayah);
        }
        userProfileRepository.save(profile);

        return savedUser;
    }

    @Override
    public User createAdmin(CreateAdminDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email sudah terdaftar");
        }

        User.Role role;
        try {
            role = User.Role.valueOf(dto.getRole());
        } catch (Exception e) {
            throw new RuntimeException("Role tidak valid: " + dto.getRole());
        }
        if (role != User.Role.ADMIN_PUSAT && role != User.Role.ADMIN_DINAS) {
            throw new RuntimeException("Role harus ADMIN_PUSAT atau ADMIN_DINAS");
        }

        String tempPassword = UUID.randomUUID().toString().substring(0, 12);

        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setRole(role);
        user.setAccountStatus(User.AccountStatus.PENDING);

        if (role == User.Role.ADMIN_DINAS) {
            if (dto.getAgencyId() == null || dto.getAgencyId().isBlank()) {
                throw new RuntimeException("Dinas/Instansi wajib diisi untuk Admin Dinas");
            }
            Agency agency = agencyRepository.findById(dto.getAgencyId())
                    .orElseThrow(() -> new RuntimeException("Dinas tidak ditemukan"));
            user.setAgency(agency);
            user.setRegion(agency.getRegion());
        }

        if (role == User.Role.ADMIN_PUSAT) {
            if (dto.getRegionId() == null || dto.getRegionId().isBlank()) {
                throw new RuntimeException("Wilayah wajib diisi untuk Admin Pusat");
            }
            Region region = regionRepository.findById(dto.getRegionId())
                    .orElseThrow(() -> new RuntimeException("Region tidak ditemukan"));
            user.setRegion(region);
        }

        User savedUser = userRepository.save(user);

        // Kirim email dengan kredensial sementara
        try {
            String subject = "AduAja - Akun Admin Baru";
            String html = buildAdminWelcomeEmail(savedUser.getFullName(), dto.getEmail(), tempPassword, role.name());
            emailService.sendEmail(dto.getEmail(), subject, html);
        } catch (Exception e) {
            log.warn("Gagal kirim email ke admin baru {}: {}", dto.getEmail(), e.getMessage());
        }

        return savedUser;
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public User updateProfile(String userId, ProfileDTO dto) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        // Update data User
        if (dto.getFullName() != null && !dto.getFullName().isBlank()) {
            user.setFullName(dto.getFullName());
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            // Cek email sudah dipakai oleh user lain
            Optional<User> existingEmail = userRepository.findByEmail(dto.getEmail());
            if (existingEmail.isPresent() && !existingEmail.get().getUserId().equals(userId)) {
                throw new RuntimeException("Email sudah dipakai oleh akun lain");
            }
            user.setEmail(dto.getEmail());
        }
        if (dto.getPhoneNumber() != null) {
            user.setPhoneNumber(dto.getPhoneNumber().isBlank() ? null : dto.getPhoneNumber());
        }

        User savedUser = userRepository.save(user);

        // Update UserProfile
        UserProfile profile = userProfileRepository.findByUserUserId(userId).orElse(null);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
        }
        if (dto.getNik() != null && !dto.getNik().isBlank()) {
            profile.setNik(dto.getNik());
        }
        if (dto.getNip() != null && !dto.getNip().isBlank()) {
            profile.setNip(dto.getNip());
        }
        if (dto.getWilayahTugasRegionId() != null && !dto.getWilayahTugasRegionId().isBlank()) {
            Region wilayah = regionRepository.findById(dto.getWilayahTugasRegionId())
                    .orElseThrow(() -> new RuntimeException("Wilayah tidak ditemukan"));
            profile.setWilayahTugas(wilayah);
        }
        if (dto.getAlamatLengkap() != null) {
            profile.setAlamatLengkap(dto.getAlamatLengkap());
        }
        if (dto.getDomisiliLatitude() != null && !dto.getDomisiliLatitude().isBlank()) {
            profile.setDomisiliLatitude(new BigDecimal(dto.getDomisiliLatitude()));
        }
        if (dto.getDomisiliLongitude() != null && !dto.getDomisiliLongitude().isBlank()) {
            profile.setDomisiliLongitude(new BigDecimal(dto.getDomisiliLongitude()));
        }
        if (dto.getProfilePhotoUrl() != null && !dto.getProfilePhotoUrl().isBlank()) {
            profile.setProfilePhotoUrl(dto.getProfilePhotoUrl());
        }
        userProfileRepository.save(profile);

        return savedUser;
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public UserProfile getProfileByUserId(String userId) {
        return userRepository.findById(userId)
            .map(User::getUserProfile)
            .orElse(null);
    }

    @Override
    public void changePassword(String userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        if (user.getAccountStatus() == User.AccountStatus.PENDING) {
            user.setAccountStatus(User.AccountStatus.ACTIVE);
        }
        userRepository.save(user);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public boolean existsByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    @Override  // ← POLYMORPHISM: Override dari interface
    public long countByRole(User.Role role) {
        return userRepository.findByRole(role).size();
    }

    private String buildAdminWelcomeEmail(String fullName, String email, String tempPassword, String role) {
        String roleLabel = role.equals("ADMIN_PUSAT") ? "Admin Pusat" : "Admin Dinas";
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
            <body style="margin:0;padding:0;background-color:#f3f4f6;font-family:'Segoe UI',Arial,sans-serif;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f3f4f6;padding:40px 0;">
                    <tr>
                        <td align="center">
                            <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:16px;box-shadow:0 4px 24px rgba(0,0,0,0.08);overflow:hidden;">
                                <tr>
                                    <td style="background:linear-gradient(135deg,#1e40af,#3b82f6);padding:40px 30px;text-align:center;">
                                        <h1 style="color:#ffffff;font-size:24px;margin:0;font-weight:700;">AduAja</h1>
                                        <p style="color:#bfdbfe;font-size:14px;margin:8px 0 0;">Sistem Pengaduan Infrastruktur Publik</p>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding:40px 30px;">
                                        <h2 style="color:#111827;font-size:20px;margin:0 0 8px;font-weight:600;">Selamat Bergabung, %s!</h2>
                                        <p style="color:#6b7280;font-size:15px;line-height:1.6;margin:0 0 24px;">
                                            Akun %s Anda telah berhasil dibuat. Gunakan kredensial di bawah untuk login pertama kali.
                                        </p>
                                        <div style="background:#f0f5ff;border:2px dashed #3b82f6;border-radius:12px;padding:24px;margin-bottom:24px;">
                                            <p style="color:#6b7280;font-size:13px;margin:0 0 4px;">Email</p>
                                            <p style="font-size:16px;font-weight:600;color:#1e40af;margin:0 0 16px;">%s</p>
                                            <p style="color:#6b7280;font-size:13px;margin:0 0 4px;">Password Sementara</p>
                                            <p style="font-size:24px;font-weight:800;letter-spacing:4px;color:#1e40af;font-family:'Courier New',monospace;margin:0;">%s</p>
                                        </div>
                                        <div style="background:#fef3c7;border-left:4px solid #f59e0b;border-radius:8px;padding:16px 20px;margin-bottom:24px;">
                                            <p style="color:#92400e;font-size:13px;margin:0;line-height:1.5;">
                                                <strong>⚠️ Wajib Ganti Password!</strong> Saat login pertama, Anda akan diminta mengganti password sementara ini.
                                            </p>
                                        </div>
                                        <p style="color:#9ca3af;font-size:13px;margin:0;line-height:1.5;">
                                            Jika Anda tidak merasa mendaftar, abaikan email ini atau hubungi support@aduaja.go.id.
                                        </p>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="background:#f9fafb;padding:24px 30px;text-align:center;border-top:1px solid #e5e7eb;">
                                        <p style="color:#9ca3af;font-size:12px;margin:0;">&copy; 2026 AduAja &mdash; Email dikirim otomatis, jangan membalas.</p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(fullName, roleLabel, email, tempPassword);
    }
}
