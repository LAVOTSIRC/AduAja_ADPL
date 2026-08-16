package com.plr.aduaja.controller;

import com.plr.aduaja.dto.LoginDTO;
import com.plr.aduaja.model.User;
import com.plr.aduaja.repository.UserRepository;
import com.plr.aduaja.security.JWTUtil;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/auth") // ini alamat loketnya
public class AuthApiController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JWTUtil jwtUtil;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> loginApi(@RequestBody LoginDTO loginRequest) {

        // 1. Cari user di datbase berdasarkan email dari loginRequest
        Optional<User> userOptional = userRepository.findByEmail((loginRequest.getEmail()));

        if(userOptional.isEmpty()) {
            return ResponseEntity.status(401).body("Eror: Email tidak ditemukan!");
        }

        // 2. Jika user Tidak Ada, tolak!
        User user = userOptional.get();

        // 3. Cocokkan password mentah dari request dengan password acak di database
        // TULIS KODEMU DI SINI: Gunakan passwordEncoder untuk mencocokkan loginRequest.getPassword() dengan user.getPassword()
        boolean isPasswordMatch = passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash());

        if(!isPasswordMatch) {
            return ResponseEntity.status(401).body("Eror: Password Salah!");
        }

        // 4. Jika password benar, cetak Token JWT
        // TULIS KODEMU DI SINI: Gunakan jwtUtil untuk mencetak token berisi username dan role user

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        // 5. Kembalikan token ke Androdi dalam format JSON
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("role", user.getRole().name());

        return ResponseEntity.ok(response);

    }
}
