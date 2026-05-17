package com.shinji.cablevalidator

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * Activity pembuka (Welcome/Onboarding) yang bertugas untuk meminta dan memvalidasi
 * izin akses Lokasi dan Bluetooth dari pengguna sebelum masuk ke menu utama aplikasi.
 */
class WelcomeActivity : AppCompatActivity() {

    // Deklarasi Komponen Antarmuka Pengguna (UI)
    private lateinit var btnLocation: MaterialButton
    private lateinit var btnBluetooth: MaterialButton
    private lateinit var btnStartApp: MaterialButton

    private lateinit var cardLocation: MaterialCardView
    private lateinit var tvTitleLocation: TextView
    private lateinit var tvDescLocation: TextView

    private lateinit var cardBluetooth: MaterialCardView
    private lateinit var tvTitleBluetooth: TextView
    private lateinit var tvDescBluetooth: TextView

    // Status perizinan saat ini
    private var isLocationGranted = false
    private var isBluetoothGranted = false

    // =========================================================
    // 1. PELUNCUR IZIN LOKASI (LOCATION PERMISSION LAUNCHER)
    // =========================================================
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        // Memeriksa apakah akses lokasi presisi tinggi atau rendah diberikan
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        isLocationGranted = fineGranted || coarseGranted

        if (isLocationGranted) {
            setUIForLocationGranted()
            checkAllPermissionsDone()
        } else {
            Toast.makeText(this, "Izin lokasi diperlukan untuk mencari perangkat.", Toast.LENGTH_SHORT)
                .show()
        }
    }

    // =========================================================
    // 2. PELUNCUR IZIN BLUETOOTH (BLUETOOTH PERMISSION LAUNCHER)
    // =========================================================
    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        // Memastikan semua izin Bluetooth yang diminta telah disetujui
        isBluetoothGranted = permissions.entries.all { it.value }

        if (isBluetoothGranted) {
            setUIForBluetoothGranted()
            checkAllPermissionsDone()
        } else {
            Toast.makeText(this, "Izin Bluetooth diperlukan untuk koneksi perangkat.", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Memuat preferensi sistem (SharedPreferences)
        val sharedPref = getSharedPreferences("AppSetting", Context.MODE_PRIVATE)

        // Menerapkan tema aplikasi (Terang/Gelap) berdasarkan penyimpanan lokal
        val isDarkMode = sharedPref.getBoolean("is_dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // Mengecek apakah pengguna sudah pernah menyelesaikan pengaturan awal
        // Jika sudah, langsung arahkan ke MainActivity dan tutup layar Welcome
        if (sharedPref.getBoolean("is_setup_done", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Memuat tata letak UI jika pengguna baru pertama kali membuka aplikasi
        setContentView(R.layout.activity_welcome)

        // Inisialisasi binding untuk seluruh komponen UI
        btnLocation = findViewById(R.id.btnLocation)
        btnBluetooth = findViewById(R.id.btnBluetooth)
        btnStartApp = findViewById(R.id.btnStartApp)

        cardLocation = findViewById(R.id.cardLocation)
        tvTitleLocation = findViewById(R.id.tvTitleLocation)
        tvDescLocation = findViewById(R.id.tvDescLocation)

        cardBluetooth = findViewById(R.id.cardBluetooth)
        tvTitleBluetooth = findViewById(R.id.tvTitleBluetooth)
        tvDescBluetooth = findViewById(R.id.tvDescBluetooth)

        // Mengecek status perizinan yang mungkin sudah diberikan sebelumnya
        checkExistingPermissions()

        // Listener untuk tombol permintaan izin Lokasi
        btnLocation.setOnClickListener {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        // Listener untuk tombol permintaan izin Bluetooth
        btnBluetooth.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Untuk Android 12 (API 31) ke atas memerlukan izin Bluetooth spesifik
                bluetoothPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                )
            } else {
                // Untuk Android 11 ke bawah, izin Bluetooth otomatis termasuk dalam izin Lokasi
                isBluetoothGranted = true
                setUIForBluetoothGranted()
                checkAllPermissionsDone()
            }
        }

        // Listener untuk tombol mulai aplikasi
        btnStartApp.setOnClickListener {
            saveSetupStatusAndProceed()
        }
    }

    // ==========================================================
    // FUNGSI PENDUKUNG (UTILITY FUNCTIONS)
    // ==========================================================

    /**
     * Memeriksa apakah izin telah diberikan secara otomatis (misalnya dari pengaturan sistem)
     * saat aplikasi baru pertama kali dimuat.
     */
    private fun checkExistingPermissions() {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocation || hasCoarseLocation) {
            isLocationGranted = true
            setUIForLocationGranted()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasScan = ContextCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED

            val hasConnect = ContextCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED

            if (hasScan && hasConnect) {
                isBluetoothGranted = true
                setUIForBluetoothGranted()
            }
        } else {
            // Android versi lama
            if (isLocationGranted) {
                isBluetoothGranted = true
                setUIForBluetoothGranted()
            }
        }

        checkAllPermissionsDone()
    }

    /**
     * Mengambil nilai warna teks standar (Hitam pada Light Mode, Putih pada Dark Mode)
     * yang diatur oleh tema sistem (Material Design).
     */
    private fun getDefaultTextColor(): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
        return typedValue.data
    }

    /**
     * Memperbarui antarmuka pengguna pada kartu Lokasi saat izin berhasil diberikan.
     */
    private fun setUIForLocationGranted() {
        cardLocation.strokeColor = ContextCompat.getColor(this, R.color.brand_blue)
        cardLocation.setCardBackgroundColor(ContextCompat.getColor(this, R.color.blue_card_trans))

        tvTitleLocation.setTextColor(ContextCompat.getColor(this, R.color.brand_blue))
        tvTitleLocation.text = "Lokasi"

        // Mengembalikan warna keterangan menjadi standar sistem
        tvDescLocation.setTextColor(getDefaultTextColor())

        btnLocation.text = "Diizinkan"
        btnLocation.isEnabled = false

        // Membuka kunci tombol Bluetooth jika izinnya belum diberikan
        if (!isBluetoothGranted) {
            btnBluetooth.isEnabled = true
            btnBluetooth.setBackgroundColor(ContextCompat.getColor(this, R.color.brand_blue))
            btnBluetooth.setTextColor(Color.WHITE)
        }
    }

    /**
     * Memperbarui antarmuka pengguna pada kartu Bluetooth saat izin berhasil diberikan.
     */
    private fun setUIForBluetoothGranted() {
        cardBluetooth.strokeColor = ContextCompat.getColor(this, R.color.brand_blue)
        cardBluetooth.setCardBackgroundColor(ContextCompat.getColor(this, R.color.blue_card_trans))

        tvTitleBluetooth.setTextColor(ContextCompat.getColor(this, R.color.brand_blue))
        tvTitleBluetooth.text = "Perangkat Terdekat"

        // Mengembalikan warna keterangan menjadi standar sistem
        tvDescBluetooth.setTextColor(getDefaultTextColor())

        btnBluetooth.text = "Diizinkan"
        btnBluetooth.isEnabled = false
    }

    /**
     * Memeriksa apakah seluruh izin yang diwajibkan telah terpenuhi.
     * Jika ya, aktifkan tombol untuk masuk ke aplikasi utama.
     */
    private fun checkAllPermissionsDone() {
        if (isLocationGranted && isBluetoothGranted) {
            btnStartApp.isEnabled = true
            btnStartApp.setBackgroundColor(ContextCompat.getColor(this, R.color.brand_blue))
            btnStartApp.setTextColor(Color.WHITE)
        }
    }

    /**
     * Menyimpan status onboarding agar halaman ini tidak ditampilkan kembali,
     * lalu menavigasikan pengguna ke menu utama (MainActivity).
     */
    private fun saveSetupStatusAndProceed() {
        // Memisahkan metode berantai ke baris baru agar lebih rapi
        getSharedPreferences("AppSetting", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_setup_done", true)
            .apply()

        startActivity(Intent(this, MainActivity::class.java))
        finish() // Menutup WelcomeActivity dari tumpukan (stack) navigasi
    }
}