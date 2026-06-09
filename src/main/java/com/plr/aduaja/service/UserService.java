package com.plr.aduaja.service;

import com.plr.aduaja.model.User;
import com.plr.aduaja.model.UserProfile;
import com.plr.aduaja.dto.CreateAdminDTO;
import com.plr.aduaja.dto.CreatePetugasDTO;
import com.plr.aduaja.dto.RegisterDTO;
import com.plr.aduaja.dto.ProfileDTO;

import java.util.List;
import java.util.Optional;

// ============================================================
// ABSTRACTION (Abstraksi): UserService Interface sebagai kontrak
// Controller hanya tahu interface ini, tidak tahu implementasinya
//
// POLYMORPHISM (Compile-time / Overloading):
// Beberapa method dengan nama mirip tapi parameter beda:
// - findById(String id)
// - findByEmail(String email)
// - findByPhoneNumber(String phoneNumber)
// - findByRole(Role role)
// - findByRoleAndStatus(Role role, AccountStatus status)   ← Overload
// ============================================================
public interface UserService {

    // ===========================
    // OVERLOADING (Compile-time Polymorphism)
    // Method berbeda — mengembalikan User yang sama tapi cara cari berbeda
    // ===========================
    Optional<User> findById(String id);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);

    // OVERLOADING: findBy dengan parameter berbeda
    List<User> findByRole(User.Role role);
    List<User> findByRoleAndStatus(User.Role role, User.AccountStatus status);  // ← Overload

    // ===========================
    // METHOD UTAMA
    // ===========================
    User createUser(RegisterDTO dto);
    User updatePendingRegistration(RegisterDTO dto, String userId);
    User createPetugas(CreatePetugasDTO dto);
    User createAdmin(CreateAdminDTO dto);
    User updateUser(User user);
    User updateProfile(String userId, ProfileDTO dto);
    UserProfile getProfileByUserId(String userId);

    void changePassword(String userId, String newPassword);

    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    long countByRole(User.Role role);

    // Alias method untuk backward compatibility dengan WebController lama
    default Optional<User> getUserById(String id) { return findById(id); }
    default Optional<User> getUserByEmail(String email) { return findByEmail(email); }
    default List<User> getUsersByRole(User.Role role) { return findByRole(role); }
}
