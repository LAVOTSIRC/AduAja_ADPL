package com.plr.aduaja.controller;

import com.plr.aduaja.service.ImageMigrationService;
import com.plr.aduaja.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        MediaType.IMAGE_JPEG_VALUE, "image/jpg", MediaType.IMAGE_PNG_VALUE
    );

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png");

    @Autowired
    private StorageService storageService;

    @Autowired
    private ImageMigrationService imageMigrationService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("jenisGambar") String jenisGambar) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "File tidak boleh kosong"
            ));
        }

        if (jenisGambar == null || jenisGambar.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Parameter jenisGambar wajib diisi"
            ));
        }

        String validationError = validateImageFile(file);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", validationError
            ));
        }

        try {
            String url = storageService.upload(file, jenisGambar);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "url", url,
                "message", "Upload berhasil"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Gagal upload: " + e.getMessage()
            ));
        }
    }

    private String validateImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            return "Hanya file JPG, JPEG, dan PNG yang diperbolehkan. Tipe file: " + contentType;
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && !originalFilename.isBlank()) {
            int dotIndex = originalFilename.lastIndexOf(".");
            if (dotIndex == -1) {
                return "File tidak memiliki ekstensi. Hanya .jpg, .jpeg, dan .png yang diperbolehkan.";
            }
            String ext = originalFilename.substring(dotIndex).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                return "Ekstensi file tidak didukung. Hanya .jpg, .jpeg, dan .png yang diperbolehkan.";
            }
        }

        return null;
    }

    @PostMapping("/migrate")
    public ResponseEntity<Map<String, Object>> migrate() {
        try {
            imageMigrationService.migrateAll();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Migrasi gambar ke Supabase selesai!"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Gagal migrasi: " + e.getMessage()
            ));
        }
    }
}
