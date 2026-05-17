package com.shinji.cablevalidator

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Activity utama aplikasi yang bertindak sebagai wadah (container) untuk berbagai Fragment.
 * Mengelola bilah alat (Toolbar), navigasi tab (TabLayout), serta pengaturan tema (Terang/Gelap).
 */
class MainActivity : AppCompatActivity() {

    // Komponen antarmuka pengguna
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var toolbar: Toolbar

    // ViewModel untuk membagikan data dan status koneksi ke semua Fragment
    private lateinit var viewModel: ScanViewModel

    // Status tema saat ini
    private var isDarkMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Pengecekan dan penerapan tema sebelum memuat tampilan (layout)
        val sharedPref = getSharedPreferences("AppSetting", Context.MODE_PRIVATE)
        isDarkMode = sharedPref.getBoolean("is_dark_mode", false)
        applyTheme(isDarkMode)

        // Memuat tata letak utama
        setContentView(R.layout.activity_main)

        // Inisialisasi ViewModel
        viewModel = ViewModelProvider(this)[ScanViewModel::class.java]

        // 2. Inisialisasi komponen tampilan berdasarkan ID
        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        // 3. Konfigurasi Bilah Alat (Toolbar)
        setSupportActionBar(toolbar)

        // Mengatur judul aplikasi dan menghilangkan bayangan (elevation) agar menyatu dengan tab
        supportActionBar?.title = "Cable Validator"
        supportActionBar?.elevation = 0f

        // Konfigurasi lanjutan untuk sistem navigasi dan pemantauan status
        setupTabs()
        setupObservers()
    }

    /**
     * Memantau perubahan status koneksi dari ViewModel.
     * Saat status berubah (terhubung/terputus), menu bilah alat akan disegarkan ulang.
     */
    private fun setupObservers() {
        viewModel.connectionState.observe(this) { isConnected ->
            // invalidateOptionsMenu() akan memicu pemanggilan ulang onPrepareOptionsMenu
            invalidateOptionsMenu()
        }
    }

    /**
     * Menghubungkan berkas XML menu kustom ke bilah alat utama.
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    /**
     * Dipanggil sebelum menu ditampilkan ke pengguna.
     * Digunakan untuk memperbarui ikon atau warna secara dinamis berdasarkan status aplikasi saat ini.
     */
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        // A. Mengatur Ikon Tema (Terang/Gelap)
        val themeItem = menu?.findItem(R.id.action_theme)
        if (isDarkMode) {
            themeItem?.setIcon(R.drawable.ic_mode_day)
        } else {
            themeItem?.setIcon(R.drawable.ic_mode_night)
        }

        // B. Mengatur Warna Indikator Status Koneksi (Status Dot)
        val statusItem = menu?.findItem(R.id.action_status)
        // mutate() digunakan agar perubahan warna tidak memengaruhi ikon lain yang menggunakan sumber daya yang sama
        val icon = statusItem?.icon?.mutate()

        if (viewModel.isConnected) {
            // Memberikan warna hijau jika perangkat terhubung
            icon?.setTint(Color.parseColor("#4CAF50"))
        } else {
            // Memberikan warna merah jika perangkat terputus
            icon?.setTint(Color.parseColor("#F44336"))
        }

        statusItem?.icon = icon

        return super.onPrepareOptionsMenu(menu)
    }

    /**
     * Menangani interaksi (klik) dari pengguna terhadap item-item pada menu bilah alat.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_theme -> {
                // Membalikkan status tema saat ini
                isDarkMode = !isDarkMode

                // Menyimpan pengaturan tema baru ke SharedPreferences secara berantai namun rapi
                getSharedPreferences("AppSetting", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("is_dark_mode", isDarkMode)
                    .apply()

                // Menerapkan perubahan tema ke tampilan
                applyTheme(isDarkMode)

                // Menyegarkan menu agar ikon tema yang baru termuat
                invalidateOptionsMenu()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Menyiapkan ViewPager dan TabLayout untuk navigasi antar lima halaman (Fragment) aplikasi.
     */
    private fun setupTabs() {
        // Menggunakan FragmentStateAdapter untuk mengelola Fragment secara efisien
        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 5 // Total 5 halaman

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> DeviceFragment()
                    1 -> ConfigFragment()
                    2 -> ValidationFragment()
                    3 -> HistoryFragment()
                    4 -> GuideFragment()
                    else -> DeviceFragment() // Nilai cadangan (fallback)
                }
            }
        }

        viewPager.adapter = adapter
        // Mengatur jumlah halaman di luar layar yang tetap dijaga di memori untuk transisi halus
        viewPager.offscreenPageLimit = 1

        // Menghubungkan TabLayout dengan ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Perangkat"
                1 -> "Konfigurasi"
                2 -> "Validasi"
                3 -> "Riwayat"
                4 -> "Panduan"
                else -> ""
            }
        }.attach()
    }

    /**
     * Menerapkan pengaturan mode malam (Dark Mode) di seluruh aplikasi.
     */
    private fun applyTheme(isDark: Boolean) {
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}