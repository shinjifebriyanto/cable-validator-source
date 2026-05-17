package com.shinji.cablevalidator

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Mendefinisikan konfigurasi database utama untuk aplikasi menggunakan Room.
 * Menghubungkan entitas (tabel) yang digunakan serta mengatur versi database.
 */
@Database(entities = [HistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Menyediakan akses ke Data Access Object (DAO) untuk entitas History.
     */
    abstract fun historyDao(): HistoryDao

    companion object {
        /**
         * Menggunakan anotasi @Volatile untuk memastikan bahwa nilai INSTANCE
         * selalu sinkron dan pembaruannya langsung terlihat oleh semua thread.
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Mendapatkan instance dari AppDatabase.
         * Menerapkan pola Singleton untuk memastikan hanya ada satu instance
         * database yang terbuka dalam satu siklus hidup aplikasi.
         */
        fun getDatabase(context: Context): AppDatabase {
            // Mengembalikan INSTANCE jika tidak null, jika null maka akan masuk ke blok synchronized
            return INSTANCE ?: synchronized(this) {
                // Membangun instance database Room baru
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cable_validator_db"
                )
                    // Memisahkan pemanggilan fungsi lanjutan (chained method) ke baris baru
                    .build()

                // Menyimpan instance yang baru dibuat ke dalam variabel INSTANCE
                INSTANCE = instance

                // Mengembalikan instance
                instance
            }
        }
    }
}