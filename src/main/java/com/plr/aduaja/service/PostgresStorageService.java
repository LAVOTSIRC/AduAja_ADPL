package com.plr.aduaja.service;

import com.plr.aduaja.model.ImageStorage;
import com.plr.aduaja.repository.ImageStorageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "postgres", matchIfMissing = true)
public class PostgresStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(PostgresStorageService.class);

    @Autowired
    private ImageStorageRepository imageStorageRepository;

    @Override
    public String upload(MultipartFile file, String folder) {
        try {
            String originalFilename = file.getOriginalFilename();
            String fileName = generateFileName(originalFilename);
            String contentType = file.getContentType();
            if (contentType == null) contentType = "image/jpeg";

            byte[] data = file.getBytes();

            ImageStorage image = new ImageStorage(fileName, contentType, data, folder);
            image = imageStorageRepository.save(image);

            String url = "/api/images/" + image.getImageId();
            log.info("Upload ke PostgreSQL sukses: {} ({} bytes)", url, data.length);
            return url;
        } catch (IOException e) {
            throw new RuntimeException("Gagal membaca file: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadBase64(String base64Data, String folder) {
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
                log.warn("Tipe file tidak didukung: {}", mimeType);
                return null;
            }

            String ext = switch (mimeType) {
                case "image/png" -> ".png";
                default -> ".jpg";
            };

            String fileName = UUID.randomUUID() + ext;

            ImageStorage image = new ImageStorage(fileName, mimeType, imageBytes, folder);
            image = imageStorageRepository.save(image);

            String url = "/api/images/" + image.getImageId();
            log.info("Upload Base64 ke PostgreSQL sukses: {} ({} bytes)", url, imageBytes.length);
            return url;
        } catch (Exception e) {
            log.error("Gagal upload Base64 ke PostgreSQL: {}", e.getMessage(), e);
            return null;
        }
    }

    private String generateFileName(String originalFilename) {
        String ext = ".jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID() + ext;
    }
}
