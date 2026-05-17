package com.shinji.cablevalidator

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

/**
 * Data Access Object (DAO) yang mendefinisikan operasi-operasi database
 * untuk entitas riwayat (HistoryEntity).
 * Antarmuka ini digunakan oleh Room untuk menghasilkan kode implementasi SQL.
 */
@Dao
interface HistoryDao {

    /**
     * Menyisipkan (insert) data riwayat pengujian baru ke dalam database.
     * Menggunakan 'suspend' agar dapat dijalankan secara asinkron melalui Coroutines
     * tanpa memblokir Main Thread (UI Thread).
     */
    @Insert
    suspend fun insert(history: HistoryEntity)

    /**
     * Menghapus entri riwayat pengujian yang spesifik dari database.
     * Pencocokan data yang akan dihapus berdasarkan Primary Key yang ada di HistoryEntity.
     */
    @Delete
    suspend fun delete(history: HistoryEntity)

    /**
     * Mengambil daftar riwayat pengujian berdasarkan rentang waktu (tanggal) tertentu,
     * lalu mengurutkannya dari yang paling baru ke yang paling lama (DESC).
     *
     * Mengembalikan LiveData agar antarmuka pengguna (UI) dapat secara otomatis
     * mengamati (observe) dan memperbarui tampilan saat ada perubahan data di database.
     */
    @Query(
        "SELECT * FROM history_table WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC"
    )
    fun loadHistoryByRange(startDate: Long, endDate: Long): LiveData<List<HistoryEntity>>

    /**
     * Menghapus seluruh data yang ada di dalam tabel riwayat (history_table).
     * Berguna untuk fitur 'Hapus Semua Riwayat' atau saat mengatur ulang (reset) aplikasi.
     */
    @Query("DELETE FROM history_table")
    suspend fun clearAll()
}