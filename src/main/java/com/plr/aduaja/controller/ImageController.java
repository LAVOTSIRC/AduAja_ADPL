package com.plr.aduaja.controller;

import com.plr.aduaja.model.ImageStorage;
import com.plr.aduaja.repository.ImageStorageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Autowired
    private ImageStorageRepository imageStorageRepository;

    @GetMapping("/{imageId}")
    public ResponseEntity<byte[]> getImage(@PathVariable UUID imageId) {
        return imageStorageRepository.findById(imageId)
                .map(image -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.parseMediaType(image.getContentType()));
                    headers.setContentLength(image.getData().length);
                    headers.setCacheControl(CacheControl.maxAge(Duration.ofDays(1)));
                    return new ResponseEntity<>(image.getData(), headers, HttpStatus.OK);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
