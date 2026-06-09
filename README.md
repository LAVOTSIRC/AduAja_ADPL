# AduAja — Aplikasi Pelaporan Masyarakat

**AduAja** adalah platform pengaduan masyarakat berbasis web yang memungkinkan warga melaporkan masalah infrastruktur, kebersihan, dan fasilitas umum. Laporan diverifikasi oleh Admin Pusat, diteruskan ke dinas terkait, lalu ditindaklanjuti oleh petugas lapangan — semua dalam satu alur kerja yang transparan dan terukur.

---

## Kelompok PLR-PEMUDA LEGEND REVOLUSIONER

| No | Nama | NIM |
|---|---|---|
| 1 | Riyan Ansari Harahap | 241401003 |
| 2 | Christein Akadojuanrich Habayaki Purba | 241401012 |
| 3 | El Fahreza Sufi | 241401042 |
| 4 | Cristoval Pratama Siahaan | 241401057 |
| 5 | M. Zidan Ruriano AG | 241401063 |
| 6 | Akief Maulana Aulia | 241401072 |

---

## Fitur Utama

- **Warga** — Buat laporan dengan foto + GPS + peta interaktif, pantau status, konfirmasi atau ajukan sengketa
- **Admin Pusat** — Validasi, disposisi ke dinas, merge laporan duplikat, monitor SLA
- **Admin Dinas** — Assign petugas, kelola progress, jeda/resume SLA, tangani sengketa, reassign tugas
- **Petugas Lapangan** — Check-in/out GPS, terima tugas, selesaikan dengan foto before-after (watermark otomatis), ajukan penundaan

---

## Tech Stack

| Komponen | Teknologi |
|---|---|
| Backend | Java 26, Spring Boot 3.x, Maven |
| Database | H2 (file-based) |
| Frontend | Thymeleaf, Tailwind CSS, Leaflet.js |
| Auth | Spring Security + Session |
| Eksternal | Supabase (storage), SMTP (email) |

---

## Cara Menjalankan

1. **Clone repositori**
   ```
   git clone https://github.com/LAVOTSIRC/AduAja.git
   cd AduAja
   ```
2. **Copy `.env.example` ke `.env`** lalu isi konfigurasi (minimal sudah bisa jalan dengan default)
3. **Jalankan**
   ```
   mvn spring-boot:run
   ```
4. Buka **http://localhost:8080**

> Panduan lengkap ada di [`TUTORIAL_RUN.md`](TUTORIAL_RUN.md) — termasuk daftar akun, data awal, dan 6 skenario demo.

---

## Lisensi

Proyek ini dibuat untuk keperluan tugas mata kuliah **Pemrograman Berorientasi Objek**.
