package com.shinji.cablevalidator

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

/**
 * Fragment untuk menangani antarmuka konfigurasi validasi kabel.
 * Mengizinkan pengguna untuk mengatur standar, ukuran, panjang, dan toleransi kabel.
 */
class ConfigFragment : Fragment() {

    // Daftar pilihan ukuran kabel berdasarkan standar SNI dan AWG
    private val listSNI = listOf("0.5 mm²", "0.75 mm²", "1.0 mm²", "1.5 mm²", "2.5 mm²", "4.0 mm²", "6.0 mm²")
    private val listAWG = listOf("AWG 24", "AWG 22", "AWG 20", "AWG 18", "AWG 16", "AWG 14", "AWG 12", "AWG 10")

    // Tabel nilai resistansi dasar (Ohm per meter) untuk setiap kombinasi standar dan ukuran
    private val resistanceTable = mapOf(
        "SNI 0.5 mm²" to 0.0390, "SNI 0.75 mm²" to 0.0260, "SNI 1.0 mm²" to 0.0195,
        "SNI 1.5 mm²" to 0.0133, "SNI 2.5 mm²" to 0.00798, "SNI 4.0 mm²" to 0.00495,
        "SNI 6.0 mm²" to 0.00330, "AWG AWG 24" to 0.0842, "AWG AWG 22" to 0.0529,
        "AWG AWG 20" to 0.0333, "AWG AWG 18" to 0.0210, "AWG AWG 16" to 0.0131,
        "AWG AWG 14" to 0.0082, "AWG AWG 12" to 0.0052, "AWG AWG 10" to 0.0032
    )

    // Deklarasi komponen antarmuka pengguna (UI)
    private lateinit var tvCurrentConfigInfo: TextView
    private lateinit var tvLastUpdate: TextView
    private lateinit var toggleStandard: MaterialButtonToggleGroup
    private lateinit var toggleTolerance: MaterialButtonToggleGroup
    private lateinit var dropdownSize: AutoCompleteTextView
    private lateinit var etLength: TextInputEditText
    private lateinit var chipGroupLength: ChipGroup
    private lateinit var btnSaveConfig: MaterialButton

    // Deklarasi ViewModel dan utilitas lainnya
    private lateinit var viewModel: ScanViewModel
    private val handler = Handler(Looper.getMainLooper())

    // Penanda untuk mencegah pemicuan listener saat UI sedang diperbarui secara terprogram
    private var isUpdatingUI = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Menghubungkan layout XML dengan fragment
        val view = inflater.inflate(R.layout.fragment_config, container, false)

        // Inisialisasi komponen UI
        tvCurrentConfigInfo = view.findViewById(R.id.tvCurrentConfigInfo)
        tvLastUpdate = view.findViewById(R.id.tvLastUpdate)
        toggleStandard = view.findViewById(R.id.toggleStandard)
        toggleTolerance = view.findViewById(R.id.toggleTolerance)
        dropdownSize = view.findViewById(R.id.dropdownSize)
        etLength = view.findViewById(R.id.etLength)
        chipGroupLength = view.findViewById(R.id.chipGroupLength)
        btnSaveConfig = view.findViewById(R.id.btnSaveConfig)

        // Mengambil referensi ViewModel yang membagikan data antar komponen (Activity/Fragment)
        viewModel = ViewModelProvider(requireActivity())[ScanViewModel::class.java]

        // Menyiapkan konfigurasi awal UI
        setupDropdown(listSNI)
        setupUIListeners()

        return view
    }

    override fun onResume() {
        super.onResume()
        // Memuat status konfigurasi terakhir saat fragment kembali aktif
        loadLocalUIState()
    }

    /**
     * Membaca pengaturan yang tersimpan di SharedPreferences dan
     * memperbarui elemen UI agar sesuai dengan data tersebut.
     */
    private fun loadLocalUIState() {
        isUpdatingUI = true

        val sharedPref = requireActivity().getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        val std = sharedPref.getString("standard", "SNI") ?: "SNI"
        val size = sharedPref.getString("size", "0.75 mm²") ?: "0.75 mm²"
        val len = sharedPref.getString("length", "1.0") ?: "1.0"

        // Mengambil nilai toleransi dengan default 10%
        val tolerancePercent = sharedPref.getInt("tolerance", 10)

        // Memperbarui informasi koneksi dan status pada UI
        if (viewModel.isConnected) {
            tvCurrentConfigInfo.text = "$std $size ($len M) • Toleransi $tolerancePercent%"
            tvLastUpdate.text = "Konfigurasi yang Tersimpan di Perangkat"
            tvLastUpdate.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            tvCurrentConfigInfo.text = "Belum Terhubung"
            tvLastUpdate.text = "Hubungkan ke Perangkat terlebih dahulu"

            val colorOnSurfaceVariant = MaterialColors.getColor(
                tvLastUpdate,
                com.google.android.material.R.attr.colorOnSurfaceVariant
            )
            tvLastUpdate.setTextColor(colorOnSurfaceVariant)
        }

        // Menyesuaikan tombol standar dan dropdown (SNI/AWG)
        if (std.equals("SNI", true)) {
            if (toggleStandard.checkedButtonId != R.id.btnSni) {
                toggleStandard.check(R.id.btnSni)
            }
            setupDropdown(listSNI)
        } else {
            if (toggleStandard.checkedButtonId != R.id.btnAwg) {
                toggleStandard.check(R.id.btnAwg)
            }
            setupDropdown(listAWG)
        }

        // Mencocokkan nilai persentase dengan pilihan tombol toleransi
        when (tolerancePercent) {
            5 -> toggleTolerance.check(R.id.btnTolKetat)
            10 -> toggleTolerance.check(R.id.btnTolNormal)
            15 -> toggleTolerance.check(R.id.btnTolLonggar)
            else -> toggleTolerance.check(R.id.btnTolNormal)
        }

        // Memperbarui isian ukuran dan panjang menggunakan handler agar berjalan aman di Main Thread
        handler.post { dropdownSize.setText(size, false) }
        etLength.setText(len)
        syncChipWithLength(len)

        // Membebaskan status pembaruan UI setelah penundaan singkat
        handler.postDelayed({ isUpdatingUI = false }, 200)
    }

    /**
     * Mensinkronkan status tombol chip (panjang kabel) dengan nilai yang ada pada teks input.
     */
    private fun syncChipWithLength(lengthVal: String) {
        chipGroupLength.clearCheck()
        val lenFloat = lengthVal.toFloatOrNull() ?: return

        for (i in 0 until chipGroupLength.childCount) {
            val chip = chipGroupLength.getChildAt(i) as Chip

            // Membersihkan teks unit " Meter" untuk mengambil nilai numeriknya
            val chipVal = chip.text.toString()
                .replace(" Meter", "")
                .trim()
                .toFloatOrNull()

            if (chipVal != null && chipVal == lenFloat) {
                chip.isChecked = true
                break // Menghentikan iterasi setelah chip yang sesuai ditemukan
            }
        }
    }

    /**
     * Mendaftarkan semua event listener untuk interaksi pengguna.
     */
    private fun setupUIListeners() {

        // Listener untuk perubahan standar kabel (SNI atau AWG)
        toggleStandard.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isUpdatingUI) return@addOnButtonCheckedListener
            if (isChecked) {
                val items = if (checkedId == R.id.btnSni) listSNI else listAWG
                setupDropdown(items)
                dropdownSize.setText(items[0], false)
            }
        }

        // Listener untuk pilihan cepat panjang kabel melalui ChipGroup
        chipGroupLength.setOnCheckedChangeListener { group, checkedId ->
            if (isUpdatingUI) return@setOnCheckedChangeListener
            if (checkedId != View.NO_ID) {
                val chip = group.findViewById<Chip>(checkedId)

                etLength.setText(chip.text.toString().replace(" Meter", ""))
                etLength.clearFocus()
            }
        }

        // Listener untuk tombol simpan konfigurasi
        btnSaveConfig.setOnClickListener {
            // Validasi koneksi perangkat
            if (!viewModel.isConnected) {
                Toast.makeText(requireContext(), "Hubungkan perangkat terlebih dahulu", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // Validasi isian panjang
            if (etLength.text.toString().isEmpty()) return@setOnClickListener

            // Mengumpulkan data dari UI
            val standard = if (toggleStandard.checkedButtonId == R.id.btnSni) "SNI" else "AWG"
            val size = dropdownSize.text.toString()
            val lengthStr = etLength.text.toString()
            val lengthFloat = lengthStr.toFloatOrNull() ?: 1.0f

            // Mengambil persentase dari tombol toleransi yang aktif
            val tolerancePercent = when (toggleTolerance.checkedButtonId) {
                R.id.btnTolKetat -> 5
                R.id.btnTolNormal -> 10
                R.id.btnTolLonggar -> 15
                else -> 10
            }

            // Mencari nilai resistansi dasar dari tabel
            val key = "$standard $size"
            val ohmPerMeter = resistanceTable[key] ?: 0.0133

            // Mengkalkulasi batas maksimal resistansi (Batas Dasar * Pengali Toleransi)
            val baseResistance = ohmPerMeter * lengthFloat
            val multiplier = 1.0f + (tolerancePercent / 100.0f)
            val maxResistance = baseResistance * multiplier

            // Memformat limit menjadi string dengan batas 5 desimal
            val formattedLimit = String.format(Locale.US, "%.5f", maxResistance)

            // Mengirim konfigurasi ke perangkat (hardware)
            viewModel.writeConfig("LIMIT:$formattedLimit")

            // Menyimpan konfigurasi ke SharedPreferences secara berantai
            val sharedPref = requireActivity().getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
            sharedPref.edit()
                .apply {
                    putString("standard", standard)
                    putString("size", size)
                    putString("length", lengthStr)
                    putInt("tolerance", tolerancePercent)
                    putFloat("limit", maxResistance.toFloat())
                    apply() // Eksekusi penyimpanan ke lokal (asinkron)
                }

            // Memperbarui antarmuka pengguna untuk menampilkan pesan sukses
            tvCurrentConfigInfo.text = "$standard $size ($lengthStr M) • Toleransi $tolerancePercent%"
            tvLastUpdate.text = "Batas Max $formattedLimit Ω Terkirim ke Perangkat"
            tvLastUpdate.setTextColor(Color.parseColor("#4CAF50")) // Warna hijau sukses

            // Memberikan notifikasi konfirmasi tambahan setelah jeda waktu
            handler.postDelayed({
                if (isAdded && viewModel.isConnected) {
                    tvLastUpdate.text = "Berhasil Tersimpan di Perangkat"
                }
            }, 2500)
        }
    }

    /**
     * Memperbarui data untuk komponen dropdown.
     */
    private fun setupDropdown(items: List<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items)
        dropdownSize.setAdapter(adapter)
    }
}