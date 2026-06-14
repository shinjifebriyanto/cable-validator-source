# ⚡ Cable Validator - Integrated System Project

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![ESP32](https://img.shields.io/badge/ESP32-000000?style=for-the-badge&logo=espressif&logoColor=white)
![Bluetooth](https://img.shields.io/badge/Bluetooth_BLE-0082FC?style=for-the-badge&logo=bluetooth&logoColor=white)

Proyek ini merupakan sistem integrasi instrumen penguji kualitas kabel hantaran listrik berbasis **Android** dan **Bluetooth Low Energy (BLE)**. Sistem ini dirancang untuk mengukur nilai resistansi konduktor kabel secara presisi, lalu memvalidasinya secara otomatis berdasarkan standar referensi nasional dan internasional (**SNI** dan **ASTM AWG**).

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

Aplikasi Android berfungsi sebagai *User Interface* (UI) utama untuk memantau proses pengujian, melakukan kalkulasi standar, dan menyimpan data hasil pengujian tanpa harus terikat dengan layar pada instrumen fisik.

### ✨ Fitur Utama Aplikasi
* **🔗 Perangkat (Pindai & Hubungkan):** Manajemen koneksi nirkabel menggunakan teknologi BLE. Memastikan transfer data hasil pembacaan sensor berlangsung cepat, stabil, dan sangat hemat daya.
* **⚙️ Konfigurasi Parameter:** Modul pengaturan variabel pengujian yang fleksibel, meliputi penentuan standar acuan (SNI metrik / ASTM AWG), ukuran penampang, panjang kabel uji, dan persentase batas toleransi hambatan.
* **📊 Validasi Real-time:** Menampilkan nilai resistansi kabel yang diuji secara langsung dan melakukan komparasi otomatis dengan ambang batas standar untuk menentukan status kelayakan (**LULUS** atau **GAGAL**).
* **📂 Riwayat Pengujian:** Sistem pencatatan lokal (*local storage*) terintegrasi untuk menyimpan rekam jejak hasil pengujian, lengkap dengan detail parameter kabel dan waktu pengujian untuk keperluan audit atau pelaporan.

---

## 🛠️ Perangkat Keras: Instrumen & PCB

Instrumen fisik dibangun dengan mengimplementasikan metode pengukuran resistansi rendah yang mampu mengeliminasi galat ukur dari kabel *probe*, dikendalikan oleh mikrokontroler sebagai pusat pemrosesan nirkabel.

### 📐 Spesifikasi Teknis Hardware
* **Metode Pengukuran:** Menggunakan **Metode Kelvin 4-Wire (4-Terminal Sensing)**. Metode ini memisahkan jalur injeksi arus (*Force*) dan jalur pembacaan tegangan (*Sense*) untuk mengeliminasi resistansi dari kabel *probe* dan kontak konektor, sehingga hasil ukur murni hanya nilai resistansi kabel uji.
* **Sirkuit Pengukur (Sensor):** Rangkaian menggunakan **LM317** sebagai sumber arus konstan (Constant Current Source) yang diinjeksikan ke kabel uji. Penurunan tegangan (*voltage drop*) kemudian dibaca secara presisi tinggi menggunakan IC eksternal **ADC 16-bit ADS1115**.
* **Mikrokontroler Utama:** **ESP32-C3 Supermini**, bertugas mengolah data digital dari ADC dan mengirimkannya secara nirkabel via modul BLE terintegrasi.
* **Sistem Daya:** Manajemen pengisian daya baterai terintegrasi menggunakan modul **TP4056** dengan sirkuit *DC Step-Up* untuk menjamin stabilitas suplai tegangan ke komponen pengukur.

---

### 🖼️ Dokumentasi Visual Desain Hardware

Berikut adalah visualisasi teknis untuk rancangan perangkat keras *Cable Validator*:

#### 1. Pandangan Papan PCB (Top & Bottom View)

Seksi ini menampilkan desain papan sirkuit cetak (*Printed Circuit Board*) yang dirancang khusus untuk mengintegrasikan seluruh komponen elektronik dalam bentuk yang ringkas dan fungsional.

<table width="100%">
  <tr>
    <td width="50%" align="center">
      <b>Tampak Atas (Top View)</b><br>
      <i>Menunjukkan tata letak komponen pemrosesan (ESP32-C3, IC ADC) dan konektor Kelvin 4-Wire</i>
    </td>
    <td width="50%" align="center">
      <b>Tampak Bawah (Bottom View)</b><br>
      <i>Menunjukkan jalur routing dan dudukan baterai 18650 terintegrasi untuk efisiensi ruang</i>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img alt="Cable-Validator-PCB-Top" src="https://github.com/user-attachments/assets/5aa5bd52-d2bd-4239-8569-bc9866fc5a41" width="100%">
    </td>
    <td align="center">
      <img alt="Cable-Validator-PCB-Bottom" src="https://github.com/user-attachments/assets/d67cd7f0-1b65-4771-b2ae-2fd1b8e2c23b" width="100%">
    </td>
  </tr>
</table>

**Deskripsi Singkat PCB:** Desain PCB ini dirancang dengan memisahkan jalur tembaga untuk terminal *Force* (injeksi arus) dan *Sense* (pembacaan tegangan) agar sesuai dengan prinsip Kelvin 4-wire. Komponen sensitif ditempatkan di lapisan atas, sementara lapisan bawah didominasi oleh dudukan baterai 18650 untuk menyeimbangkan pusat gravitasi instrumen.

#### 2. Diagram Skematik Sirkuit

Diagram skematik ini mengilustrasikan koneksi logis lengkap dari semua komponen elektronik dalam sistem.

<p align="center">
  <img alt="Cable-Validator-Schematic" src="https://github.com/user-attachments/assets/21f80cfc-e7b3-42d4-9e74-b0ecda771b35" width="900">
</p>

**Deskripsi Singkat Skematik:** Diagram ini merepresentasikan arsitektur utama instrumen: LM317 diatur untuk memberikan arus konstan melewati terminal *Force*. Tegangan jatuh melintasi kabel uji dideteksi oleh terminal *Sense* dan dikonversi menjadi data digital beresolusi tinggi oleh ADS1115 (melalui jalur I2C). Data tersebut kemudian diproses oleh ESP32-C3 sebelum ditransmisikan. Diagram ini juga mencakup sub-sistem manajemen daya baterai lithium dan periferal *buzzer/LED* sebagai indikator status alat.

#### 🔍 Pratinjau PCB Interaktif

Klik tombol di bawah ini untuk melihat rancangan jalur PCB secara interaktif (bisa digeser, di-zoom, dan melihat setiap lapisan tembaga 2D/3D) langsung dari *browser*:

<p align="center">
  👉 <b><a href="https://gerber.tools/gerber-viewer?share=fDts5l7maSLolbcJjtFgiummQvWTvmqb5kG4MpRrfefiDbhK" target="_blank">Buka Interactive Gerber Viewer ➔</a></b>
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
