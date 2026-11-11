# FIPXEZ Hybrid Security Audit 0.2.2

Paket build FIPXEZ versi 0.2.2. Starter yang dapat dijalankan di Windows: FastAPI backend, PC worker, Android Flutter control panel, antrean scan, audit log, dan analisis temuan lokal berbasis aturan.

## Batasan dan keamanan
- Hanya untuk aset yang dimiliki/dikelola atau memiliki izin tertulis. Simpan nomor tiket/rujukan izin saat mendaftarkan target.
- Pemeriksaan pasif: HTTPS, sertifikat TLS, dan security headers. Tidak ada exploit, brute force, bypass, credential collection, stealth, atau modifikasi target.
- Analisis lokal memberi indikasi, bukan bukti kerentanan. Remediasi hanya berupa saran; tidak ada perubahan otomatis.
- Gunakan hanya di jaringan privat/lab. HTTP LAN untuk prototipe saja; jangan expose port 8000 ke internet. Gunakan hanya pada Wi-Fi privat tepercaya. Owner token tidak disimpan permanen oleh APK. Produksi wajib HTTPS, secrets manager, firewall, backup, dan hardening.
- `config.env` berisi token owner/worker. Jangan kirim atau unggah file itu.

## A. Setup Windows 10
1. Extract ZIP ke folder biasa, mis. `C:\FIPXEZ`.
2. Buka PowerShell di folder itu. Jika policy memblokir skrip, jalankan `Set-ExecutionPolicy -Scope Process Bypass`.
3. Jalankan `./Setup-FIPXEZ.ps1`. Script membuat token acak, virtual environment, dan memasang dependency.
4. Buka dua jendela PowerShell di folder proyek. Jalankan `./Run-Backend.ps1` pada jendela pertama dan `./Run-Worker.ps1` pada jendela kedua.
5. Tes backend di PC: `http://127.0.0.1:8000/health`; dokumentasi API: `http://127.0.0.1:8000/docs`.

## B. Sambungkan Android di Wi-Fi yang sama
1. Cari IPv4 PC dengan `ipconfig` (contoh `192.168.1.10`).
2. Di Windows Firewall, izinkan TCP 8000 hanya pada Private network. Jangan buat port-forwarding router.
3. Build dan install APK (bagian C).
4. Di aplikasi isi Backend URL `http://IP-PC:8000`, lalu Owner token dari `config.env`. Token hanya dipakai selama aplikasi berjalan dan tidak disimpan oleh aplikasi. Tekan Test koneksi.
5. Daftarkan hostname publik yang diizinkan dan isi referensi izin/tiket. Pilih Scan. Worker harus sedang berjalan.

## C. Build APK Android
Install Flutter SDK stable, Android Studio/Android SDK, dan terima lisensi (`flutter doctor --android-licenses`). Pastikan `flutter doctor` tidak menunjukkan error Android toolchain. Lalu jalankan `./Build-APK.ps1`.
APK hasil: `android/build/app/outputs/flutter-apk/app-release.apk`.

Jika build dari Android Studio/PC lain, buka folder `android/`, jalankan `flutter pub get`, lalu `flutter build apk --release`.

## D. Hasil dan API
- `GET /health` — status backend.
- `POST /targets` — daftar target berizin.
- `POST /scans` — antrekan pemeriksaan.
- `GET /scans/{id}` — status dan hasil.
- `GET /scans/{id}/analysis` — analisis indikasi lokal.
- `GET /audit` — jejak aktivitas.

Owner token memakai header `X-Owner-Token`; worker token terpisah memakai `X-Worker-Token`.

## Kebutuhan
Windows 10 64-bit, Python 3.11+, Flutter stable + Android SDK untuk APK. PC i3/RAM 8 GB cukup untuk worker ringan; GPU tidak dibutuhkan.

## Build APK otomatis lewat GitHub Actions

Folder `.github/workflows/build-android.yml` berisi workflow untuk membangun APK di runner cloud GitHub. Workflow sudah memperbaiki lokasi AndroidManifest agar izin INTERNET dan HTTP LAN diterapkan pada file manifest yang benar. Build juga menjalankan tes backend dan `flutter analyze` sebelum membuat APK (jadi PC lokal tidak perlu memasang Android SDK). Untuk menjalankannya, source harus berada di repository GitHub milik pengguna:

1. Buat repository baru di GitHub, lalu upload/push isi folder `FIPXEZ`.
2. Buka tab **Actions** pada repository dan aktifkan Actions bila diminta.
3. Pilih **Build FIPXEZ Android APK** → **Run workflow**.
4. Setelah status selesai, buka run tersebut dan unduh artifact **FIPXEZ-Android-APK**.

Workflow ini menghasilkan APK release yang belum ditandatangani dengan kunci distribusi milik pemilik produk. Untuk rilis Play Store atau distribusi komersial, buat dan simpan signing keystore sendiri. APK juga tetap memerlukan backend/worker aktif dan URL backend yang dapat dijangkau HP; APK bukan layanan cloud mandiri.
