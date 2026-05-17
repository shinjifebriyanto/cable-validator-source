package com.shinji.cablevalidator

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Merepresentasikan struktur tabel 'history_table' di dalam database Room.
 * Model data ini digunakan untuk menyimpan riwayat hasil pengujian atau validasi kabel.
 */
@Entity(tableName = "history_table")
data class HistoryEntity(

    /**
     * Kunci utama (Primary Key) yang mengidentifikasi setiap baris data secara unik.
     * Nilainya akan dihasilkan secara otomatis (auto-increment) oleh database.
     */
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /**
     * Nama perangkat Bluetooth atau identitas alat ukur yang digunakan
     * (misalnya: "CV-A101X").
     */
    val deviceName: String,

    /**
     * Keterangan mengenai jenis atau spesifikasi kabel yang diuji
     * (misalnya: "SNI 1.5 mm²").
     */
    val cableType: String,

    /**
     * Panjang kabel yang digunakan dalam pengujian, biasanya dalam satuan meter
     * (misalnya: "1.0").
     */
    val length: String,

    /**
     * Hasil pengukuran resistansi kabel yang diterima dari perangkat
     * (misalnya: "0.0133 Ω").
     */
    val resistance: String,

    /**
     * Kesimpulan status pengujian berdasarkan toleransi yang diatur
     * (nilainya berupa: "LULUS" atau "GAGAL").
     */
    val status: String,

    /**
     * Waktu pencatatan riwayat disimpan dalam format epoch time (milidetik).
     * Berguna untuk keperluan penyortiran (sorting) data riwayat.
     */
    val timestamp: Long
)