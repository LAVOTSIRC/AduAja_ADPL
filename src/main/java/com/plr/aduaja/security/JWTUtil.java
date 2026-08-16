package com.plr.aduaja.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JWTUtil {

    // Kunci Rahasia (Pastikan minimal 256-bit / 32 karakter untuk HS256)
    private final String SECRET_KEY = "AduAjaSecretKeyYangSangatPanjangDanAmanSekaliUntukDipakai";

    // 1 hari = 86400000 ms
    private final long EXPIRATION_DATE = 86400000;

    // Menggunakan SecretKey (bukan Key biasa) dan menyertakan StandardCharsets
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email, String role) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .subject(email) // Menggantikan setSubject
                .claim("role", role)
                .issuedAt(new Date(now)) // Menggantikan setIssuedAt
                .expiration(new Date(now + EXPIRATION_DATE)) // Menggantikan setExpiration
                .signWith(getSigningKey()) // Algoritma HS256 otomatis terdeteksi dari SecretKey
                .compact();
    }

    // Tambahkan metode ini di dalam kelas JWTUtil Anda
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // Menggunakan SecretKey internal JWTUtil
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
