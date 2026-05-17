# ⚡ Cable Validator - Integrated System Project

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![ESP32](https://img.shields.io/badge/ESP32-000000?style=for-the-badge&logo=espressif&logoColor=white)
![Bluetooth](https://img.shields.io/badge/Bluetooth_BLE-0082FC?style=for-the-badge&logo=bluetooth&logoColor=white)

Proyek ini merupakan sistem integrasi instrumen penguji kualitas kabel hantaran listrik berbasis **Android** dan **Bluetooth Low Energy (BLE)**. Sistem ini dirancang untuk mengukur nilai resistansi konduktor kabel secara riil, lalu memvalidasinya secara otomatis berdasarkan standar referensi nasional dan internasional (**SNI** dan **ASTM AWG**).

---

## 📂 Struktur Repositori

```text
/
├── Cable-Validator-App/         # Source code aplikasi mobile Android (Android Studio)
├── Cable-Validator-Device/      # Direktori utama Perangkat Keras & Firmware
│   ├── CBV-SF26X/               # Firmware/program mikrokontroler ESP32 (.ino)
│   ├── CBV-SF26X-PCB/           # Berkas produksi PCB Gerber (.ZIP)
│   └── CBV-SF26X-Schematic/     # Diagram skematik sirkuit (.PDF)
└── readme.md                    # Dokumentasi utama sistem
```

---

## 📱 Perangkat Lunak: Aplikasi Android

Aplikasi Android berfungsi sebagai *User Interface* (UI) utama untuk memantau proses pengujian, melakukan kalkulasi standar, dan menyimpan data hasil pengujian.

### ✨ Fitur Utama Aplikasi
* **🔗 Perangkat (Pindai & Hubungkan):** Manajemen koneksi nirkabel menggunakan teknologi BLE untuk menghubungkan *smartphone* dengan instrumen keras secara hemat daya dan stabil.
* **⚙️ Konfigurasi Parameter:** Modul pengaturan variabel pengujian yang fleksibel, meliputi penentuan standar acuan (SNI metrik / ASTM AWG), ukuran penampang, panjang kabel, dan persentase batas toleransi.
* **📊 Validasi Real-time:** Menampilkan grafik pembacaan nilai resistansi secara langsung dan melakukan komparasi otomatis dengan ambang batas standar untuk menentukan status kelayakan (**LULUS** atau **GAGAL**).
* **📂 Riwayat Pengujian:** Sistem pencatatan lokal (*local storage*) terintegrasi untuk menyimpan rekam jejak hasil pengujian lengkap dengan detail parameter kabel dan waktu pengujian.

---

## 🛠️ Perangkat Keras: Instrumen & PCB

Instrumen fisik dibangun menggunakan mikrokontroler sebagai pusat kendali data sensor dan rangkaian pengkondisi sinyal analog yang presisi.

### 📐 Spesifikasi Teknis Hardware
* **Mikrokontroler Utama:** ESP32 Series (Modul BLE Terintegrasi).
* **Sirkuit Pengukur:** Rangkaian pembagi tegangan (*voltage divider*) presisi tinggi yang dikombinasikan dengan penguat operasional dan IC ADC eksternal untuk membaca nilai resistansi rendah.
* **Sistem Daya:** Manajemen pengisian daya baterai terintegrasi menggunakan modul TP4056 dengan sirkuit *DC Step-Up*.

---

### 🖼️ Dokumentasi Visual Desain Hardware

Berikut adalah visualisasi teknis untuk rancangan perangkat keras *Cable Validator*:

#### 1. Pandangan Papan PCB (Top & Bottom View)

Seksi ini menampilkan desain papan sirkuit cetak (*Printed Circuit Board*) yang dirancang khusus untuk mengintegrasikan seluruh komponen elektronik dalam bentuk yang ringkas dan fungsional.

<table width="100%">
  <tr>
    <td width="50%" align="center">
      <b>Tampak Atas (Top View)</b><br>
      <i>Menunjukkan tata letak komponen utama (ESP32, IC, Connector)</i>
    </td>
    <td width="50%" align="center">
      <b>Tampak Bawah (Bottom View)</b><br>
      <i>Menunjukkan jalur routing dan dudukan baterai 18650 terintegrasi</i>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="Cable-Validator-Device/Cable-Validator-PCB-Top.jpg" alt="PCB Top View" width="100%">
    </td>
    <td align="center">
      <img src="Cable-Validator-Device/Cable-Validator-PCB-Bottom.jpg" alt="PCB Bottom View" width="100%">
    </td>
  </tr>
</table>

**Deskripsi Singkat PCB:** Desain PCB ini mengoptimalkan ruang dengan menempatkan komponen pemrosesan (ESP32) dan sirkuit pengukur sensitif di lapisan atas, sementara lapisan bawah didominasi oleh dudukan baterai holder 18650 untuk keseimbangan berat dan efisiensi ruang boks alat.

#### 2. Diagram Skematik Sirkuit

Diagram skematik ini mengilustrasikan koneksi logis lengkap dari semua komponen elektronik dalam sistem.

<p align="center">
  <img src="Cable-Validator-Device/Cable-Validator-Schematic.jpg" alt="Diagram Skematik Cable Validator" width="900">
</p>

**Deskripsi Singkat Skematik:** Diagram ini mencakup sub-sistem vital: manajemen daya (LM317 Adjustable Current Source & TP4056 Battery Charger), sirkuit sensing (Sense/Force connectors), pengolah data (ESP32-C3 Supermini), dan periferal pendukung (ADS1115 ADC, Buzzer, LED Indikator).

#### 🔍 Pratinjau PCB Interaktif

Klik tombol di bawah ini untuk melihat rancangan jalur PCB secara interaktif (bisa digeser, di-zoom, dan melihat setiap lapisan tembaga 2D/3D) langsung dari *browser*:

<p align="center">
  👉 <b><a href="MASUKKAN_LINK_SHARE_DANNIE_CC_DI_SINI" target="_blank">Buka Interactive Gerber Viewer ➔</a></b>
</p>

---

## 📥 Unduh & Distribusi Aplikasi

Halaman rilis siap pakai (`.apk`) dapat diunduh melalui tautan resmi di bawah ini:

**[🌐 Buka Halaman Unduhan Resmi Cable Validator](https://shinjifebriyanto.github.io/cable-validator-download/)**

---

## 👤 Pengembang Proyek

* **Nama:** Shinji Febriyanto  
* **NIM:** 5221011040  
* **Program Studi:** S1 Teknik Komputer (Angkatan 2022)  
* **Institusi:** Universitas Teknologi Yogyakarta
