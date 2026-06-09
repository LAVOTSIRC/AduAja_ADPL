# Gap Analysis: Matriks Kebutuhan AduAja V1.0 vs Kondisi Implementasi

> Berdasarkan audit mendalam terhadap RTM (ISO/IEC/IEEE 29148:2018), evaluasi dokumen per-role, dan inspeksi langsung kode sumber proyek.
> **Tanggal audit:** 09 Juni 2026

---

## Ringkasan Eksekutif

Dari total **~80 kebutuhan wajib (SHALL)** yang terdaftar dalam RTM, sebagian besar klaster telah diimplementasikan dengan sangat baik. Namun ditemukan **15 item gap** yang terbagi ke dalam dua kategori: **⚠️ PARSIAL** (fitur ada tapi belum lengkap) dan **❌ BELUM ADA** (fitur sama sekali belum diimplementasikan).

---

## ✅ Klaster yang SUDAH Terpenuhi Penuh (Ringkasan)

| Klaster | Cakupan |
|---|---|
| Autentikasi Warga (FR-AKN-01/02/03/05) | ✅ OTP via Brevo, login email/HP |
| Seluruh alur pelaporan Warga FR-WRG-01 s/d FR-WRG-23 | ✅ Kamera real-time, GPS, Leaflet, preview, revisi, tarik laporan |
| Petugas: Absensi, Check-In/Out (FR-PTG-01/02/03/04/06) | ✅ OfficerAttendance model aktif |
| Petugas: To-Do List, SLA badge (FR-PTG-09/11/12) | ✅ Tampilan terklasifikasi + badge warna |
| Petugas: Geofencing 50m (NFR-INT-04) | ✅ `GeoUtils.isWithinRadius()` aktif |
| Petugas: Watermark foto (FR-PTG-21) | ✅ `PhotoWatermarkUtil` burn-in server-side |
| Petugas: Foto before/after wajib (FR-PTG-19/20) | ✅ Controller memblokir jika kosong |
| Petugas: Ajukan Penundaan / Eskalasi (FR-PTG-27/29/30) | ✅ `TaskPostponement` + `EscalationReason` |
| Admin Dinas: Isolasi wilayah (FR-PRS-01) | ✅ Filter `agencyId` di `AdminDinasController` |
| Admin Dinas: Beban tugas petugas (FR-PRS-02) | ✅ `activeTaskCount` di UI |
| Admin Dinas: Jeda SLA / Force Majeure (FR-JDA-01 s/d 05) | ✅ `TaskPostponement` + `SlaMonitoringServiceImpl` |
| Admin Dinas: Dasbor SLA & Eskalasi (FR-ESK-01/02) | ✅ Cron job `checkSlaViolations()` |
| Admin Dinas: Resolusi Sengketa (FR-RSL-12) | ✅ `rejectAndReprocess` / `forceClose` |
| Merge Ticket: Parent/Child logic (FR-ADM-13/14/15) | ✅ `MergeRecordServiceImpl` lengkap |
| Unmerge (FR-ADM-17/18) | ✅ `cancelMerge()` memulihkan status child |
| Deteksi duplikat 50m (FR-ADM-12) | ✅ `hasPotentialDuplicate()` haversine |
| Photo manipulation detection (FR-ADM-06) | ✅ `detectPhotoManipulation()` — selisih EXIF |
| Blokir merge setelah disposisi (FR-ADM-16) | ✅ `isMergeBlocked()` di controller |
| Disposisi ke dinas (FR-DSP-01/02/03/04/05/06) | ✅ `DispositionService` + status otomatis |
| Notifikasi status berubah (FR-ADM-10) | ✅ `notificationService.createNotification()` dipanggil di semua transisi |
| Sengketa max 1x per tiket (FR-RSL-13) | ✅ Cek di `DisputeServiceImpl.createDispute()` |
| Audit Log immutable (NFR-AUD-01/02) | ✅ `AuditLog` dengan setter package-private |
| Server-side timestamp (NFR-INT-01) | ✅ `LocalDateTime.now()` di peladen |
| OTP blokir sebelum verifikasi (NFR-SEC-02 sebagian) | ✅ Status PENDING diblokir login |
| Brute-force lockout 5x (NFR-SEC-03) | ✅ Di `AuthServiceImpl` |
| Session timeout dikonfigurasi (NFR-SEC-04) | ✅ `server.servlet.session.timeout=14400` |
| Invalidasi sesi pasca ganti sandi (NFR-SEC-05) | ✅ `session.invalidate()` di `changePassword` |
| Petugas: riwayat kinerja SLA (FR-PTG-33) | ✅ `/petugas/reports` dengan rasio |
| Auto-close 3×24 jam (FR-RSL-04/05) | ✅ `SlaMonitoringServiceImpl` scheduler |
| Warga tombol Terima/Tolak eksklusif (FR-RSL-06/07) | ✅ Lock setelah 1x aksi |
| Warga: sengketa wajib foto + narasi (FR-RSL-09/10) | ✅ `/warga/dispute` validasi payload |
| SLA monitoring Admin Pusat (FR-ESK-01/02) | ✅ Panel SLA di `/admin/sla` |
| Arsitektur MVC terpisah front/back (NFR-MNT-01) | ✅ Spring MVC + Thymeleaf |
| Berbasis web tanpa install (NFR-MNT-03) | ✅ Web app responsif |
| File size limit 20MB (NFR-INT-02 sebagian) | ✅ `application.properties` |

---

## ❌ / ⚠️ DAFTAR GAP YANG BELUM TERPENUHI

Berikut adalah **15 item gap** yang ditemukan berdasarkan inspeksi kode secara langsung:

---

### 🔴 GAP PRIORITAS TINGGI (Wajib segera diselesaikan)

---

**[GAP-01] FR-AKN-08/09/10 — Alur Pembuatan Akun Admin Baru via UI**
- **Kebutuhan:** Sistem harus bisa membuat akun Administrator baru dengan ID unik, batas wilayah, role, **mengirim tautan aktivasi / sandi sementara**, dan **memaksa ganti sandi saat login pertama**.
- **Kondisi saat ini:** Untuk Petugas (PENDING → force change password) sudah berjalan. Namun untuk **Admin Pusat / Admin Dinas baru**, tidak ada endpoint UI yang memfasilitasi pembuatan akun dari dalam sistem — hanya tersedia via `DataSeeder`. `AdminAuthController` tidak memiliki alur "buat admin baru + kirim kredensial sementara". Petugas baru sudah ter-handle (line 151 `AdminAuthController`), tapi Admin baru belum.
- **Dampak:** Tidak ada mekanisme onboarding Admin baru yang terdokumentasi dalam sistem live.
- **Rekomendasi:** Tambahkan endpoint `POST /admin/create-admin` atau `POST /admin/pusat/create-admin` dengan logika generate sandi sementara + kirim via email + set status PENDING untuk Admin.

---

**[GAP-02] FR-AKN-14 — Pemulihan Sandi Petugas via Nomor Seluler**
- **Kebutuhan:** Sistem menyediakan fungsionalitas pemulihan / pembaruan kata sandi khusus Petugas dengan **verifikasi keamanan nomor seluler** (bukan email OTP).
- **Kondisi saat ini:** `WargaAuthController` mendukung lupa sandi via OTP email. Untuk Petugas hanya ada `/petugas/forgot-password` dan `/petugas/forgot-password-verify`, namun mekanisme verifikasinya menggunakan email atau password reset link — **bukan verifikasi nomor HP/seluler** secara spesifik.
- **Dampak:** Petugas yang tidak ingat email terdaftar tidak punya jalur pemulihan via HP.
- **Rekomendasi:** Tambahkan alur verifikasi OTP ke nomor HP untuk endpoint lupa sandi Petugas.

---

**[GAP-03] FR-ESK-03 — Hard-Lock UI saat Tiket "Terlambat" (Forced Audit Input)**
- **Kebutuhan:** Sistem **memaksa** Admin/Petugas mengisi alasan kronologis sebelum bisa memodifikasi tiket yang sudah melewati batas SLA. Ini bukan sekadar label merah — ini **blocking form** (hard-lock).
- **Kondisi saat ini:** Tiket berstatus `TERLAMBAT` ditampilkan di dasbor dan tersedia endpoint review (`/admin/sla/review-overdue`), namun tidak ada **mekanisme pemblokiran paksa** pada aksi-aksi lain (assign, update status) sampai alasan diisi. Evaluasi sebelumnya juga mengklasifikasikan ini **⚠️ PARSIAL**.
- **Dampak:** Tiket terlambat masih bisa diproses tanpa accountability log yang dipaksakan.
- **Rekomendasi:** Implementasikan interceptor atau guard di controller yang mengecek apakah tiket berstatus TERLAMBAT dan `overdueReviewed == false`; jika ya, redirect ke form audit terlebih dahulu.

---

**[GAP-04] FR-PTG-16 — Deep-Link Navigasi Eksternal (Google Maps / Waze)**
- **Kebutuhan:** Sistem menautkan tombol langsung ke aplikasi navigasi eksternal (Google Maps / Waze) dengan parameter koordinat GPS tiket otomatis terisi.
- **Kondisi saat ini:** Inspeksi template `petugas/task-detail.html` dan `petugas/task-execution.html` **tidak menemukan satu pun** referensi ke `maps.google.com`, `waze.com`, `geo:`, atau deep-link navigasi apapun. Evaluasi sebelumnya juga menyatakan **⚠️ PARSIAL**.
- **Dampak:** Petugas harus secara manual membuka Maps dan mengetik koordinat — friction operasional di lapangan.
- **Rekomendasi:** Tambahkan tombol "Buka di Google Maps" dan "Buka di Waze" di halaman detail tugas, dengan format URL `https://www.google.com/maps/dir/?api=1&destination={lat},{lng}` dan `https://waze.com/ul?ll={lat},{lng}&navigate=yes`.

---

**[GAP-05] FR-RSL-14/15/17 — Notifikasi & Resolusi Independen untuk Warga Child Ticket**
- **Kebutuhan:** Saat Parent Ticket selesai, **semua Warga dalam merge group** (termasuk pemilik Child Ticket) menerima notifikasi secara independen. Setiap Warga punya hak Terima/Tolak masing-masing, dan Parent Ticket baru `SELESAI` setelah **semua** Warga menyetujui atau timeout.
- **Kondisi saat ini:**
  - `MergeRecordServiceImpl.createMerge()` mengirim notifikasi ke reporter child saat merge — ✅
  - `ConfirmationServiceImpl` menangani konfirmasi Warga
  - Namun dari inspeksi `ConfirmationServiceImpl`, tidak ditemukan logika yang memanggil `getAllChildReportsForParent()` untuk mengirim notifikasi ke **semua warga child** saat petugas menandai selesai.
  - FR-RSL-17 (Parent Ticket SELESAI hanya setelah semua warga di merge group konfirmasi) belum terverifikasi implementasinya.
- **Dampak:** Warga pemilik Child Ticket mungkin tidak mendapat notifikasi konfirmasi dan tidak punya hak Terima/Tolak.
- **Rekomendasi:** Pastikan `ConfirmationServiceImpl.createConfirmation()` (atau saat petugas submit selesai) mengirim `ConfirmationRequest` ke **semua reporter dalam merge group**, bukan hanya reporter Parent.

---

**[GAP-06] FR-RGN-01 — Geocoding Balik (Kelurahan/Kecamatan dari Koordinat GPS)**
- **Kebutuhan:** Sistem mendeteksi nama administratif wilayah (kelurahan, kecamatan) dari koordinat GPS menggunakan layanan Geocoding eksternal.
- **Kondisi saat ini:** Pencarian kode (`geocod`, `reverseGeo`, `kelurahan`, `kecamatan`, `nominatim`, `LocationService`) di seluruh codebase **tidak menemukan hasil**. Field `locationHint` diisi manual oleh Warga, bukan hasil geocoding otomatis.
- **Dampak:** Laporan tidak memiliki nama administratif terstandar yang terisi otomatis; validasi yurisdiksi berbasis teks rentan kesalahan.
- **Rekomendasi:** Integrasikan Nominatim (OpenStreetMap, gratis) atau Google Geocoding API. Panggil saat laporan dibuat, simpan hasil ke field `region` atau `locationHint` secara otomatis.

---

### 🟡 GAP PRIORITAS MENENGAH (Penting untuk kualitas sistem)

---

**[GAP-07] NFR-INT-05 — API Rate Limiter / Anti-DDoS**
- **Kebutuhan:** Sistem menerapkan pembatasan laju request (rate limiting / token bucket) untuk mencegah serangan DDoS dan spam laporan.
- **Kondisi saat ini:** Pencarian `RateLimiter` di seluruh codebase **tidak menemukan hasil**. `SecurityConfig.java` tidak mengandung filter rate limiting. Satu-satunya proteksi adalah brute-force lockout 5x login.
- **Dampak:** Endpoint publik (submit laporan, login) rentan terhadap banjir request.
- **Rekomendasi:** Implementasikan `Bucket4j` atau Spring rate-limiting filter. Minimal batasi endpoint `/warga/submit-report` dan `/admin/login` dengan sliding window rate limiter.

---

**[GAP-08] NFR-INT-02 — Validasi Ketat Ekstensi File (JPG/PNG Only)**
- **Kebutuhan:** Sistem hanya menerima file dengan ekstensi JPG, JPEG, PNG — validasi di level service/logic, bukan hanya ukuran.
- **Kondisi saat ini:** `application.properties` membatasi ukuran upload (20MB), namun dari pencarian `strictContentType` / `imageExtension` / `validateFileType` di codebase **tidak ditemukan** validasi tipe file secara eksplisit di level service. Validasi mungkin hanya di sisi frontend.
- **Dampak:** Pengguna bisa mengunggah file berekstensi lain yang menyamar sebagai gambar.
- **Rekomendasi:** Tambahkan validasi `MultipartFile.getContentType()` di `UploadController.java` atau service terkait untuk memastikan hanya `image/jpeg` dan `image/png` yang diterima.

---

**[GAP-09] NFR-REL-01/03 — Ketahanan Sinyal Lemah & Auto-Retry Upload**
- **Kebutuhan:** Sistem mampu menyimpan data form sementara di browser cache dan melanjutkan upload otomatis saat koneksi pulih. Kompresi gambar di sisi klien sebelum upload.
- **Kondisi saat ini:** Tidak ditemukan implementasi `localStorage` auto-save, Service Worker, atau client-side image compression di template HTML Warga/Petugas.
- **Dampak:** Petugas di lapangan dengan sinyal lemah kehilangan progress pengisian form jika koneksi terputus.
- **Rekomendasi:** Implementasikan auto-save form ke `localStorage` setiap N detik di form laporan dan task execution. Tambahkan kompresi gambar via `canvas.toBlob()` sebelum submit.

---

**[GAP-10] NFR-USA-02 — Animasi Visual Pulse/Glow untuk Tiket "Terlambat"**
- **Kebutuhan:** Tiket dengan status SLA `TERLAMBAT` menampilkan indikator visual ekstrem berupa animasi berkelap-kelip / pulsing glow untuk menangkap perhatian langsung.
- **Kondisi saat ini:** Badge CSS merah sudah ada (`bg-red-100 text-red-600`), namun tidak ada animasi `@keyframes pulse` atau `animate-ping` (Tailwind). Evaluasi sebelumnya juga menyatakan **⚠️ PARSIAL**.
- **Dampak:** Dashboard terasa kurang urgent; potensi administrator melewatkan tiket terlambat di layar penuh.
- **Rekomendasi:** Tambahkan class Tailwind `animate-ping` atau `animate-pulse` pada badge tiket terlambat, atau definisikan CSS keyframe `@keyframes glow { 0%,100%{box-shadow:0 0 5px red} 50%{box-shadow:0 0 20px red} }`.

---

**[GAP-11] NFR-LEG-01 — Penyensoran Otomatis Data Sensitif Warga di Antarmuka Publik**
- **Kebutuhan:** NIK, koordinat rumah pelapor, dan data identitas sensitif harus disensor/disamarkan saat ditampilkan di portal yang bisa dilihat umum.
- **Kondisi saat ini:** Pencarian `mask`, `censor`, `sensor`, `anonymize` di seluruh codebase dan template **tidak menemukan implementasi** penyensoran data sensitif. Data pelapor (nama, email) ditampilkan penuh di berbagai view.
- **Dampak:** Pelanggaran privasi — data pribadi Warga bisa terekspos ke pihak tidak berwenang.
- **Rekomendasi:** Implementasikan helper di Thymeleaf atau DTO yang menyensor email (`j***@gmail.com`), nama belakang, dan membulatkan koordinat GPS ke 2 desimal saat ditampilkan di konteks non-owner.

---

**[GAP-12] NFR-INT-03 — Penolakan Laporan saat Akurasi GPS > 20 Meter**
- **Kebutuhan:** Jika akurasi sinyal GPS perangkat melebihi deviasi 20 meter, sistem harus menolak penerimaan koordinat dan meminta pengambilan ulang.
- **Kondisi saat ini:** Template Warga menggunakan `navigator.geolocation.getCurrentPosition()`, namun tidak ditemukan pengecekan `position.coords.accuracy > 20` dalam logika JavaScript di form pelaporan.
- **Dampak:** Koordinat laporan yang tidak akurat bisa menyebabkan salah yurisdiksi dan geofencing error petugas.
- **Rekomendasi:** Tambahkan validasi di JavaScript: `if (position.coords.accuracy > 20) { showError("Sinyal GPS tidak akurat, coba lagi di area terbuka"); return; }`.

---

### 🟢 GAP PRIORITAS RENDAH (Penyempurnaan kosmetik/opsional-hampir-wajib)

---

**[GAP-13] NFR-SEC-02 — Validasi Domain Email Resmi Instansi Pemerintah untuk Admin**
- **Kebutuhan:** Sistem memastikan pendaftaran akun Administrator menggunakan alamat email domain resmi instansi pemerintah (misal `@dinas.go.id`).
- **Kondisi saat ini:** Tidak ada validasi domain email di `AdminAuthController` atau `UserServiceImpl`. Bahkan `DataSeeder` menggunakan `admin@aduaja.go.id` sebagai fallback hardcoded.
- **Dampak:** Admin bisa didaftarkan dengan email personal (Gmail, Yahoo) yang tidak terstandar.
- **Rekomendasi:** Tambahkan validasi di level DTO atau service: `if (!email.endsWith(".go.id")) throw ...`. Bisa dikonfigurasi via `application.properties`.

---

**[GAP-14] NFR-AUD-03 — Error Log untuk Kegagalan Sistem Kritis**
- **Kebutuhan:** Sistem mencatat setiap kejadian galat, kegagalan sinkronisasi (offline-to-online) ke dalam error log tersendiri untuk keperluan debugging.
- **Kondisi saat ini:** Beberapa service menggunakan `log.error()` via SLF4J (terlihat di `DisputeServiceImpl`, `AdminPusatController`), namun tidak ada dedicated **error log table** di database atau structured error logging yang terpisah dari audit log bisnis. Logging hanya ke stdout.
- **Dampak:** Investigasi insiden teknis sulit tanpa riwayat error terstruktur.
- **Rekomendasi:** Konfigurasi `logback-spring.xml` dengan file appender terpisah untuk level ERROR, atau tambahkan tabel `SystemErrorLog` di database.

---

**[GAP-15] FR-RSL-04 — Timer Countdown Visual di Halaman Warga**
- **Kebutuhan:** Grafik timer hitung mundur 3×24 jam yang jelas dan mendesak (urgency pulse) terlihat di halaman detail tiket Warga saat status "Menunggu Validasi".
- **Kondisi saat ini:** Backend `SlaMonitoringServiceImpl` sudah menghitung dan mengeksekusi auto-close, namun UI Warga **tidak menampilkan countdown visual** yang berdetak. Evaluasi sebelumnya menyatakan **⚠️ PARSIAL**.
- **Dampak:** Warga tidak tahu bahwa ada deadline 3 hari untuk merespons; banyak yang mungkin melewatkan tanpa sadar.
- **Rekomendasi:** Tambahkan komponen JavaScript countdown di `warga/report-detail.html` (atau setara) yang menghitung sisa waktu dari `deadline` yang dikirim server dan menampilkannya dalam format `HH:MM:SS` dengan efek warna merah saat < 6 jam.

---

## Tabel Rekap Gap

| # | ID Gap | Requirement | Prioritas | Status |
|---|---|---|---|---|
| 1 | GAP-01 | FR-AKN-08/09/10 | 🔴 Tinggi | ❌ Belum Ada |
| 2 | GAP-02 | FR-AKN-14 | 🔴 Tinggi | ⚠️ Parsial |
| 3 | GAP-03 | FR-ESK-03 | 🔴 Tinggi | ⚠️ Parsial |
| 4 | GAP-04 | FR-PTG-16 | 🔴 Tinggi | ❌ Belum Ada |
| 5 | GAP-05 | FR-RSL-14/15/17 | 🔴 Tinggi | ⚠️ Parsial |
| 6 | GAP-06 | FR-RGN-01 | 🔴 Tinggi | ❌ Belum Ada |
| 7 | GAP-07 | NFR-INT-05 | 🟡 Menengah | ❌ Belum Ada |
| 8 | GAP-08 | NFR-INT-02 | 🟡 Menengah | ⚠️ Parsial |
| 9 | GAP-09 | NFR-REL-01/03 | 🟡 Menengah | ❌ Belum Ada |
| 10 | GAP-10 | NFR-USA-02 | 🟡 Menengah | ⚠️ Parsial |
| 11 | GAP-11 | NFR-LEG-01 | 🟡 Menengah | ❌ Belum Ada |
| 12 | GAP-12 | NFR-INT-03 | 🟡 Menengah | ❌ Belum Ada |
| 13 | GAP-13 | NFR-SEC-02 | 🟢 Rendah | ⚠️ Parsial |
| 14 | GAP-14 | NFR-AUD-03 | 🟢 Rendah | ⚠️ Parsial |
| 15 | GAP-15 | FR-RSL-04 | 🟢 Rendah | ⚠️ Parsial |

---

## Catatan Arsitektural

> [!NOTE]
> Gap-06 (FR-RGN-01 Geocoding) adalah **satu-satunya gap yang menuntut integrasi layanan eksternal baru** (API Nominatim / Google). Semua gap lainnya dapat diselesaikan murni dengan perubahan kode internal tanpa dependensi eksternal baru.

> [!IMPORTANT]
> Gap-01 (pembuatan admin baru) dan Gap-05 (notifikasi child ticket pada selesai) adalah yang paling kritikal secara fungsional karena menyentuh jalur utama operasional sistem yang tidak bisa disiasati tanpa intervensi database langsung.

> [!TIP]
> Gap-04 (FR-PTG-16 navigasi) adalah yang paling mudah dan cepat diimplementasikan — hanya perlu tambah 2 baris anchor tag HTML di template petugas. Rekomendasi dikerjakan lebih awal untuk quick win.

