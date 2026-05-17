package com.shinji.cablevalidator

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.util.Locale

/**
 * Fragment yang bertanggung jawab untuk menampilkan proses dan hasil validasi kabel secara real-time.
 * Menerima data dari ViewModel (Bluetooth) dan memberikan umpan balik visual (LULUS/GAGAL/MENGUKUR).
 */
class ValidationFragment : Fragment() {

    // Komponen Antarmuka Pengguna (UI)
    private lateinit var tvResistanceValue: TextView
    private lateinit var tvOhmSymbol: TextView
    private lateinit var tvResultStatus: TextView
    private lateinit var cardResult: MaterialCardView
    private lateinit var btnStartValidation: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var tvInfoLength: TextView
    private lateinit var tvInfoSize: TextView
    private lateinit var tvInfoLimit: TextView
    private lateinit var tvSummary: TextView
    private lateinit var cardSummary: MaterialCardView

    // ViewModel untuk komunikasi data BLE
    private lateinit var viewModel: ScanViewModel

    // Variabel state untuk menyimpan status validasi saat ini
    private var currentLimit = 0.0f
    private var cableLength = 0.0f
    private var isStable = false
    private var lastStatusText = ""
    private var measureAnimCount = 0

    // Tabel referensi resistansi maksimal standar (Ohm per meter)
    private val resistanceTable = mapOf(
        "SNI 0.5 mm²" to 0.0390, "SNI 0.75 mm²" to 0.0260, "SNI 1.0 mm²" to 0.0195,
        "SNI 1.5 mm²" to 0.0133, "SNI 2.5 mm²" to 0.00798, "SNI 4.0 mm²" to 0.00495,
        "SNI 6.0 mm²" to 0.00330, "AWG AWG 24" to 0.0842, "AWG AWG 22" to 0.0529,
        "AWG AWG 20" to 0.0333, "AWG AWG 18" to 0.0210, "AWG AWG 16" to 0.0131,
        "AWG AWG 14" to 0.0082, "AWG AWG 12" to 0.0052, "AWG AWG 10" to 0.0032
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Menghubungkan layout XML fragment_validation ke dalam kelas ini
        val view = inflater.inflate(R.layout.fragment_validation, container, false)

        // Inisialisasi komponen UI
        tvResistanceValue = view.findViewById(R.id.tvResistanceValue)
        tvOhmSymbol = view.findViewById(R.id.tvOhmSymbol)
        tvResultStatus = view.findViewById(R.id.tvResultStatus)
        cardResult = view.findViewById(R.id.cardResult)
        btnStartValidation = view.findViewById(R.id.btnStartValidation)
        btnSave = view.findViewById(R.id.btnSave)
        tvInfoLength = view.findViewById(R.id.tvInfoLength)
        tvInfoSize = view.findViewById(R.id.tvInfoSize)
        tvInfoLimit = view.findViewById(R.id.tvInfoLimit)
        tvSummary = view.findViewById(R.id.tvSummary)
        cardSummary = view.findViewById(R.id.cardSummary)

        // Menghubungkan dengan ScanViewModel yang ada di Activity utama
        viewModel = ViewModelProvider(requireActivity())[ScanViewModel::class.java]

        setupListeners()
        setupObservers()

        return view
    }

    override fun onResume() {
        super.onResume()

        // Memuat ulang pengaturan lokal saat tampilan kembali aktif
        loadLocalUIState()

        // Mengambil data terakhir yang ada di memori ViewModel
        val lastData = viewModel.resistanceData.value

        // Jika terdapat data validasi yang selesai atau error, tampilkan kembali hasilnya
        if (lastData != null && (lastData.startsWith("LULUS") ||
                    lastData.startsWith("GAGAL") ||
                    lastData.startsWith("ERROR"))
        ) {
            handleMeasurementData(lastData)
        } else {
            resetUI()
        }
    }

    /**
     * Membaca konfigurasi (standar, ukuran, panjang) dari SharedPreferences
     * dan mengkalkulasi batas toleransi (limit) jika belum tersimpan.
     */
    private fun loadLocalUIState() {
        // Memisahkan rantai pemanggilan agar lebih mudah dibaca
        val sharedPref = requireActivity()
            .getSharedPreferences("AppConfig", Context.MODE_PRIVATE)

        val std = sharedPref.getString("standard", "SNI") ?: "SNI"
        val size = sharedPref.getString("size", "0.75 mm²") ?: "0.75 mm²"
        val lenStr = sharedPref.getString("length", "1.0") ?: "1.0"

        cableLength = lenStr.toFloatOrNull() ?: 1.0f
        currentLimit = sharedPref.getFloat("limit", 0.0f)

        // Jika limit belum disetel (0.0f), hitung secara manual dari tabel referensi
        if (currentLimit <= 0.0f) {
            val key = "$std $size"
            val ohmPerMeter = resistanceTable[key] ?: 0.0133
            currentLimit = (ohmPerMeter * cableLength).toFloat()
        }

        // Memperbarui informasi di UI berdasarkan status koneksi
        if (viewModel.isConnected) {
            tvInfoSize.text = "$std $size"
            tvInfoLength.text = "$lenStr Meter"
            tvInfoLimit.text = String.format(Locale.US, "%.5f Ω", currentLimit)
        } else {
            tvInfoSize.text = "--"
            tvInfoLength.text = "--"
            tvInfoLimit.text = "0.00000 Ω"
        }
    }

    /**
     * Mengatur aksi interaksi klik pada tombol-tombol antarmuka.
     */
    private fun setupListeners() {
        btnStartValidation.setOnClickListener {
            // Jika perangkat belum terhubung, arahkan pengguna kembali ke Tab Perangkat (index 0)
            if (!viewModel.isConnected) {
                try {
                    // Pengecekan untuk ViewPager2
                    val viewPager2 = requireActivity()
                        .findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)

                    if (viewPager2 != null) {
                        viewPager2.currentItem = 0
                        return@setOnClickListener
                    }

                    // Pengecekan fallback untuk ViewPager versi lama
                    val viewPager = requireActivity()
                        .findViewById<androidx.viewpager.widget.ViewPager>(R.id.viewPager)

                    if (viewPager != null) {
                        viewPager.currentItem = 0
                        return@setOnClickListener
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Pesan darurat jika gagal memindahkan tab
                Toast.makeText(context, "Silakan buka tab Device secara manual.", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // Jika status sedang stabil (sudah ada hasil), tombol berfungsi sebagai "Reset"
            if (isStable) {
                resetUI()
                return@setOnClickListener
            }

            // Memulai proses animasi dan menyiapkan antarmuka untuk pengukuran
            measureAnimCount = 0
            tvResistanceValue.text = "-"

            tvResistanceValue.setTextColor(Color.parseColor("#FFC107")) // Kuning (Warning)
            tvOhmSymbol.setTextColor(Color.parseColor("#FFC107"))

            tvResultStatus.text = "MENYIAPKAN VALIDASI..."
            tvResultStatus.setTextColor(Color.parseColor("#FFC107"))

            isStable = false

            // Mengirimkan instruksi mulai ke perangkat keras (Hardware)
            viewModel.writeConfig("START_TEST")
        }

        btnSave.setOnClickListener {
            saveMeasurement()
        }
    }

    /**
     * Mengembalikan status antarmuka ke mode "Siap" atau "Menunggu Koneksi".
     */
    private fun resetUI() {
        viewModel.resistanceData.value = ""

        tvResistanceValue.text = "0.00000"
        isStable = false
        lastStatusText = ""

        if (viewModel.isConnected) {
            // Mode siap jika perangkat BLE terhubung
            tvResistanceValue.setTextColor(Color.parseColor("#03A9F4")) // Biru
            tvOhmSymbol.setTextColor(Color.parseColor("#03A9F4"))

            tvResultStatus.text = "SIAP DIVALIDASI"
            tvResultStatus.setTextColor(Color.parseColor("#03A9F4"))

            tvSummary.text = "Pasang kabel yang akan divalidasi ke probe. untuk memulai validasi tekan tombol Mulai Validasi."
            btnStartValidation.text = "Mulai Validasi"
        } else {
            // Mode tidak aktif jika perangkat BLE terputus
            tvResistanceValue.setTextColor(Color.parseColor("#9E9E9E")) // Abu-abu
            tvOhmSymbol.setTextColor(Color.parseColor("#9E9E9E"))

            tvResultStatus.text = "PERANGKAT BELUM TERHUBUNG"
            tvResultStatus.setTextColor(Color.parseColor("#9E9E9E"))

            tvSummary.text = "Perangkat belum terhubung. Silakan hubungkan perangkat terlebih dahulu di menu Perangkat."
            btnStartValidation.text = "Hubungkan Alat"
        }
    }

    /**
     * Memvalidasi dan menyimpan hasil pengukuran saat ini ke dalam database (Riwayat).
     */
    private fun saveMeasurement() {
        if (!isStable) {
            Toast.makeText(context, "Belum ada hasil validasi yang selesai", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val size = tvInfoSize.text.toString()
        val resistance = "${tvResistanceValue.text} Ω"

        // Mengirim instruksi simpan ke ViewModel
        viewModel.saveToHistory(
            requireContext(),
            size,
            cableLength.toString(),
            resistance,
            lastStatusText
        )

        resetUI()
    }

    /**
     * Mendaftarkan pengamat (Observers) ke LiveData untuk mendeteksi pembaruan
     * dari perangkat BLE maupun perubahan koneksi.
     */
    private fun setupObservers() {
        viewModel.resistanceData.observe(viewLifecycleOwner) { dataString ->
            // Format yang diharapkan dari Hardware: "STATUS:NILAI" (Contoh: "LULUS:0.0123")
            if (dataString != null && dataString.contains(":")) {
                handleMeasurementData(dataString)
            }
        }

        viewModel.connectionState.observe(viewLifecycleOwner) { isConnected ->
            if (!isConnected) {
                resetUI()
            }
            loadLocalUIState()
        }
    }

    /**
     * Memproses data string (protokol komunikasi) yang diterima dari perangkat keras
     * dan mengubahnya menjadi status visual di antarmuka aplikasi.
     */
    private fun handleMeasurementData(data: String) {
        try {
            // Memecah pesan string berdasarkan tanda titik dua
            val parts = data.split(":")
            val status = parts[0].trim()
            val valueStr = parts[1].trim()

            // Konversi nilai ohm dari perangkat keras (Aman dari null)
            val measuredOhm = valueStr.toFloatOrNull() ?: 0.0f

            when (status) {
                "ERROR" -> {
                    // Terjadi kesalahan pengukuran (Misal: Probe tidak menempel)
                    isStable = false
                    tvResistanceValue.text = "-----"

                    tvResistanceValue.setTextColor(Color.parseColor("#F44336")) // Merah
                    tvOhmSymbol.setTextColor(Color.parseColor("#F44336"))

                    tvResultStatus.text = "KABEL BELUM TERPASANG"
                    tvResultStatus.setTextColor(Color.parseColor("#F44336"))

                    tvSummary.text = "Pastikan kabel yang akan divalidasi terpasang ke probe dengan kuat di kedua ujung kabel sebelum menekan tombol."
                    btnStartValidation.text = "Validasi Ulang"
                }
                "MENGUKUR" -> {
                    // Animasi garis-garis selama proses mengukur
                    isStable = false
                    measureAnimCount++
                    val stripAnim = when (measureAnimCount % 6) {
                        1 -> "-"
                        2 -> "- -"
                        3 -> "- - -"
                        4 -> "- - - -"
                        5 -> "- - - - -"
                        else -> "- - - - -"
                    }
                    tvResistanceValue.text = stripAnim

                    tvResistanceValue.setTextColor(Color.parseColor("#FFC107")) // Kuning
                    tvOhmSymbol.setTextColor(Color.parseColor("#FFC107"))

                    tvResultStatus.text = "MEMVALIDASI KABEL..."
                    tvResultStatus.setTextColor(Color.parseColor("#FFC107"))

                    tvSummary.text = "Mengukur resistansi... Tahan probe agar tetap stabil."
                }
                "LULUS" -> {
                    // Pengukuran berhasil dan di bawah/sama dengan batas toleransi
                    isStable = true
                    lastStatusText = "LULUS"
                    tvResistanceValue.text = String.format(Locale.US, "%.5f", measuredOhm)

                    tvResistanceValue.setTextColor(Color.parseColor("#4CAF50")) // Hijau
                    tvOhmSymbol.setTextColor(Color.parseColor("#4CAF50"))

                    tvResultStatus.text = "LULUS (TEMBAGA MURNI)"
                    tvResultStatus.setTextColor(Color.parseColor("#4CAF50"))

                    generateProfessionalSummary(measuredOhm, true)
                    btnStartValidation.text = "Validasi Ulang (Reset)"
                }
                "GAGAL" -> {
                    // Pengukuran berhasil namun melebihi batas toleransi
                    isStable = true
                    lastStatusText = "GAGAL"
                    tvResistanceValue.text = String.format(Locale.US, "%.5f", measuredOhm)

                    tvResistanceValue.setTextColor(Color.parseColor("#F44336")) // Merah
                    tvOhmSymbol.setTextColor(Color.parseColor("#F44336"))

                    tvResultStatus.text = "GAGAL (TIDAK MURNI)"
                    tvResultStatus.setTextColor(Color.parseColor("#F44336"))

                    generateProfessionalSummary(measuredOhm, false)
                    btnStartValidation.text = "Validasi Ulang (Reset)"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Menghasilkan teks kesimpulan yang rapi dan profesional
     * berdasarkan hasil akhir pengukuran (Lulus/Gagal).
     */
    private fun generateProfessionalSummary(measured: Float, isPass: Boolean) {
        val resistanceStr = String.format(Locale.US, "%.5f", measured) + " Ω"
        val cableType = tvInfoSize.text.toString()
        val lengthStr = cableLength.toString()

        val summary = if (isPass) {
            "Hasil Validasi: MEMENUHI STANDAR (LULUS).\n\n" +
                    "Resistansi konduktor ($resistanceStr) berada dalam batas toleransi standar " +
                    "ASTM B 258/SNI untuk kabel tipe $cableType dengan panjang $lengthStr meter."
        } else {
            "Hasil Validasi: TIDAK MEMENUHI STANDAR (GAGAL).\n\n" +
                    "Resistansi konduktor ($resistanceStr) melebihi batas yang diizinkan untuk " +
                    "kabel tipe $cableType. Kemungkinan terdapat ketidakmurnian bahan atau ketidaksesuaian panjang."
        }

        tvSummary.text = summary
    }
}