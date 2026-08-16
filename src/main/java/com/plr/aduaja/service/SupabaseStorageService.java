package com.plr.aduaja.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "supabase")
public class SupabaseStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.anon-key}")
    private String anonKey;

    @Value("${supabase.bucket}")
    private String bucket;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public String upload(MultipartFile file, String jenisGambar) {
        try {
            String fileName = generateFileName(file.getOriginalFilename());
            String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + jenisGambar + "/" + fileName;

            HttpHeaders headers = buildHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
            ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String publicUrl = supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + jenisGambar + "/" + fileName;
                log.info("Upload sukses ke Supabase: {}", publicUrl);
                return publicUrl;
            } else {
                throw new RuntimeException("Gagal upload ke Supabase: " + response.getStatusCode() + " - " + response.getBody());
            }
        } catch (IOException e) {
            throw new RuntimeException("Gagal membaca file: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadBase64(String base64Data, String jenisGambar) {
        if (base64Data == null || base64Data.isBlank()) {
            return null;
        }

        try {
            String mimeType = "image/jpeg";
            byte[] imageBytes;

            if (base64Data.contains(",")) {
                String[] parts = base64Data.split(",");
                String header = parts[0];
                if (header.contains("png")) mimeType = "image/png";
                else mimeType = "image/jpeg";
                imageBytes = Base64.getDecoder().decode(parts[1]);
            } else {
                imageBytes = Base64.getDecoder().decode(base64Data);
            }

            if (!"image/jpeg".equals(mimeType) && !"image/png".equals(mimeType)) {
                log.warn("Tipe file tidak didukung untuk Base64 upload: {}", mimeType);
                return null;
            }

            String ext = switch (mimeType) {
                case "image/png" -> ".png";
                default -> ".jpg";
            };

            String fileName = UUID.randomUUID().toString() + ext;
            String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + jenisGambar + "/" + fileName;

            HttpHeaders headers = buildHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            HttpEntity<byte[]> entity = new HttpEntity<>(imageBytes, headers);
            ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String publicUrl = supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + jenisGambar + "/" + fileName;
                log.info("Upload Base64 sukses ke Supabase: {}", publicUrl);
                return publicUrl;
            } else {
                throw new RuntimeException("Gagal upload Base64 ke Supabase: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Gagal upload Base64 ke Supabase: {}", e.getMessage(), e);
            return null;
        }
    }

    private String generateFileName(String originalFilename) {
        String ext = ".jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + ext;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + anonKey);
        headers.set("apikey", anonKey);
        return headers;
    }
}
