package com.plr.aduaja.controller;

import com.plr.aduaja.model.Report;
import com.plr.aduaja.dto.CreateReportDTO;
import com.plr.aduaja.model.Report.ReportStatus;
import com.plr.aduaja.model.User;
import com.plr.aduaja.repository.UserRepository;
import com.plr.aduaja.service.ReportService;
import com.plr.aduaja.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/reports")
public class ReportApiController {

    @Autowired
    private ReportService reportService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    // ROUTE PINTAR NYA BOY
    @GetMapping("/my-reports")
    public ResponseEntity<List<Report>> getSmartReports(java.security.Principal principal) {

        // 1. Kenali Siapa yang Datang
        String emailUser = principal.getName();
        User currentUser = userRepository.findByEmail(emailUser)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        String userRole = currentUser.getRole().name(); // Misal mengembalikan "WARGA" atau "ADMIN"
        String userIdAsli = currentUser.getUserId();

        // 2. Persimpangan Logika Berdasarkan Role
        List<Report> hasilLaporan;

        if (userRole.equals("ADMIN_PUSAT")) {
            // Admin Pusat melihat semuanya
            hasilLaporan = reportService.getAllReports();
        }
        else if (userRole.equals("ADMIN_DINAS")) {
            // Logika lanjutan: Ambil region dinas, cari laporan di region itu
            // (Sementara kita biarkan kosong, kau bisa kembangkan nanti)
            hasilLaporan = new java.util.ArrayList<>();
        }
        else {
            // Default (WARGA biasa): Hanya melihat miliknya sendiri
            hasilLaporan = reportService.getReportsByUser(userIdAsli);
        }

        // 3. Kembalikan data yang sudah tersaring dengan aman!
        return ResponseEntity.ok(hasilLaporan);
    }


    @GetMapping
    public ResponseEntity<List<Report>> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Report> getReportById(@PathVariable String id) {
        Optional<Report> report = reportService.getReportById(id);
        return report.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ticket/{ticketNumber}")
    public ResponseEntity<Report> getReportByTicketNumber(@PathVariable String ticketNumber) {
        Optional<Report> report = reportService.getReportByTicketNumber(ticketNumber);
        return report.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Report>> getReportsByStatus(@PathVariable ReportStatus status) {
        return ResponseEntity.ok(reportService.getReportsByStatus(status));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Report>> getReportsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(reportService.getReportsByUser(userId));
    }

    @GetMapping("/disposisi-ready")
    public ResponseEntity<List<Report>> getReportsForDisposisi() {
        return ResponseEntity.ok(reportService.getReportsForDisposisi());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Report>> searchReports(@RequestParam String q) {
        return ResponseEntity.ok(reportService.searchReports(q));
    }

    @PostMapping
    public ResponseEntity<Report> createReport(
            @RequestBody CreateReportDTO kardusDariAndroid, // 1. Tangkap pakai kardus (DTO)
            java.security.Principal principal) {
        try {
            // Cari Warga yang lapor
            String emailWarga = principal.getName();
            User pelapor = userRepository.findByEmail(emailWarga)
                    .orElseThrow(() -> new RuntimeException("Warga tidak ditemukan"));
            String idWargaAsli = pelapor.getUserId();

            // 2. Siapkan Rak Besi Baru yang masih kosong
            Report laporanBaru = new Report();

            // 3. Pindahkan barang dari Kardus ke Rak Besi (Mapping Manual)
            laporanBaru.setDescription(kardusDariAndroid.getDescription());
            laporanBaru.setLocationHint(kardusDariAndroid.getLocationHint());
            // Mengubah Double dari DTO menjadi BigDecimal untuk Entity
            laporanBaru.setLatitude(kardusDariAndroid.getLatitude());
            laporanBaru.setLongitude(kardusDariAndroid.getLongitude());
            laporanBaru.setPhotoBase64(kardusDariAndroid.getPhotoBase64());

            // PENTING: Kategori dan Region belum kita pindahkan, karena butuh trik khusus!
            // (Kita biarkan kosong dulu untuk sementara)

            // 4. Serahkan Rak Besi (laporanBaru) ke Service
            Report createdReport = reportService.createReport(laporanBaru, idWargaAsli);

            return ResponseEntity.status(HttpStatus.CREATED).body(createdReport);
        } catch (Exception e) {
            log.error("Gagal buat report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Report> updateStatus(@PathVariable String id,
                                                @RequestParam ReportStatus status) {
        try {
            Report report = reportService.updateStatus(id, status);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Gagal update status report {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getReportCounts() {
        Map<String, Long> counts = Map.of(
                "menunggu_verifikasi", reportService.countByStatus(ReportStatus.MENUNGGU_VERIFIKASI),
                "diterima", reportService.countByStatus(ReportStatus.DITERIMA),
                "ditugaskan", reportService.countByStatus(ReportStatus.DITUGASKAN),
                "selesai", reportService.countByStatus(ReportStatus.SELESAI),
                "sengketa", reportService.countByStatus(ReportStatus.SENGKETA)
        );
        return ResponseEntity.ok(counts);
    }
}
