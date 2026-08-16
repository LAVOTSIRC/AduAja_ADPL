package com.plr.aduaja.repository;

import com.plr.aduaja.model.ImageStorage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImageStorageRepository extends JpaRepository<ImageStorage, UUID> {
}
