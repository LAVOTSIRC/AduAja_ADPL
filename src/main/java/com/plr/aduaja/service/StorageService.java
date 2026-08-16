package com.plr.aduaja.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String upload(MultipartFile file, String folder);
    String uploadBase64(String base64Data, String folder);
}
