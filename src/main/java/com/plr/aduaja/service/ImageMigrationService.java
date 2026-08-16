package com.plr.aduaja.service;

import com.plr.aduaja.model.DisputeRecord;
import com.plr.aduaja.model.Report;
import com.plr.aduaja.model.TaskEvidence;
import com.plr.aduaja.model.UserProfile;
import com.plr.aduaja.repository.DisputeRecordRepository;
import com.plr.aduaja.repository.ReportRepository;
import com.plr.aduaja.repository.TaskEvidenceRepository;
import com.plr.aduaja.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class ImageMigrationService {

    private static final Logger log = LoggerFactory.getLogger(ImageMigrationService.class);

    @Autowired
    private StorageService storageService;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private TaskEvidenceRepository taskEvidenceRepository;

    @Autowired
    private DisputeRecordRepository disputeRecordRepository;

    @Transactional
    public void migrateAll() {
        migrateReports();
        migrateUserProfiles();
        migrateTaskEvidences();
        migrateDisputeRecords();
        log.info("Migrasi semua gambar ke Supabase selesai!");
    }

    public void migrateReports() {
        List<Report> reports = reportRepository.findAll();
        int count = 0;
        for (Report r : reports) {
            String photo = r.getPhotoBase64();
            if (photo != null && !photo.isBlank() && !photo.startsWith("http")) {
                String url = storageService.uploadBase64(photo, "laporan");
                if (url != null) {
                    r.setPhotoBase64(url);
                    reportRepository.save(r);
                    count++;
                    log.info("Migrasi Report {}: {}", r.getReportId(), url);
                }
            }
        }
        log.info("Migrasi {} foto laporan ke Supabase", count);
    }

    public void migrateUserProfiles() {
        List<UserProfile> profiles = userProfileRepository.findAll();
        int count = 0;
        for (UserProfile p : profiles) {
            String photo = p.getProfilePhotoUrl();
            if (photo != null && !photo.isBlank() && !photo.startsWith("http")) {
                try {
                    String url = uploadLocalFile(photo, "profile");
                    if (url != null) {
                        p.setProfilePhotoUrl(url);
                        userProfileRepository.save(p);
                        count++;
                        log.info("Migrasi Profile {}: {}", p.getProfileId(), url);
                    }
                } catch (Exception e) {
                    log.warn("Gagal migrasi profile {}: {}", p.getProfileId(), e.getMessage());
                }
            }
        }
        log.info("Migrasi {} foto profil ke Supabase", count);
    }

    public void migrateTaskEvidences() {
        List<TaskEvidence> evidences = taskEvidenceRepository.findAll();
        int count = 0;
        for (TaskEvidence e : evidences) {
            String photo = e.getPhotoUrl();
            if (photo != null && !photo.isBlank() && !photo.startsWith("http")) {
                String url = storageService.uploadBase64(photo, "bukti");
                if (url != null) {
                    e.setPhotoUrl(url);
                    taskEvidenceRepository.save(e);
                    count++;
                    log.info("Migrasi TaskEvidence {}: {}", e.getEvidenceId(), url);
                }
            }
        }
        log.info("Migrasi {} foto bukti tugas ke Supabase", count);
    }

    public void migrateDisputeRecords() {
        List<DisputeRecord> disputes = disputeRecordRepository.findAll();
        int count = 0;
        for (DisputeRecord d : disputes) {
            String photo = d.getEvidencePhotoUrl();
            if (photo != null && !photo.isBlank() && !photo.startsWith("http")) {
                String url = storageService.uploadBase64(photo, "sengketa");
                if (url != null) {
                    d.setEvidencePhotoUrl(url);
                    disputeRecordRepository.save(d);
                    count++;
                    log.info("Migrasi DisputeRecord {}: {}", d.getDisputeId(), url);
                }
            }
        }
        log.info("Migrasi {} foto sengketa ke Supabase", count);
    }

    private String uploadLocalFile(String photoPath, String jenisGambar) throws IOException {
        Path filePath;
        if (photoPath.startsWith("/profile-photos/")) {
            filePath = Paths.get("uploads/profile-photos/", photoPath.replace("/profile-photos/", ""));
        } else {
            filePath = Paths.get(photoPath);
        }

        if (!Files.exists(filePath)) {
            log.warn("File tidak ditemukan: {}", filePath);
            return null;
        }

        byte[] bytes = Files.readAllBytes(filePath);
        String mimeType = Files.probeContentType(filePath);
        if (mimeType == null) mimeType = "image/jpeg";

        String ext = "";
        String name = filePath.getFileName().toString();
        if (name.contains(".")) ext = name.substring(name.lastIndexOf("."));

        java.util.Base64.Encoder encoder = java.util.Base64.getEncoder();
        String base64 = "data:" + mimeType + ";base64," + encoder.encodeToString(bytes);

        return storageService.uploadBase64(base64, jenisGambar);
    }
}
