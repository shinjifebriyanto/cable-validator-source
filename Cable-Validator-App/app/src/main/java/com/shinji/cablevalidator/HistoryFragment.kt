package com.shinji.cablevalidator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment yang bertanggung jawab untuk menampilkan, memfilter,
 * dan mengelola riwayat hasil validasi kabel.
 */
class HistoryFragment : Fragment() {

    // Komponen antarmuka pengguna (UI)
    private lateinit var rvHistory: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvSummary: TextView

    // Komponen dropdown untuk filter
    private lateinit var dropdownDevice: AutoCompleteTextView
    private lateinit var dropdownTime: AutoCompleteTextView

    // Dependensi Database, Adapter, dan ViewModel
    private lateinit var db: AppDatabase
    private lateinit var adapter: HistoryAdapter
    private lateinit var viewModel: ScanViewModel

    // Penyimpanan data lokal untuk keperluan filter (tanpa perlu query ulang ke DB)
    private var allHistoryList: List<HistoryEntity> = emptyList()
    private var selectedDeviceFilter: String = "Semua"
    private var selectedTimeFilter: String = "Semua Riwayat"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Menghubungkan layout XML fragment_history ke dalam class ini
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        // Inisialisasi binding untuk komponen UI
        rvHistory = view.findViewById(R.id.rvHistory)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        tvSummary = view.findViewById(R.id.tvSummary)
        dropdownDevice = view.findViewById(R.id.dropdownDevice)
        dropdownTime = view.findViewById(R.id.dropdownTime)

        // Menyiapkan ViewModel dan Database
        viewModel = ViewModelProvider(requireActivity())[ScanViewModel::class.java]
        db = AppDatabase.getDatabase(requireContext())

        // Konfigurasi awal komponen tampilan dan data
        setupRecyclerView()
        setupTimeDropdown()
        setupDeviceDropdown(listOf("Semua"))
        loadAllData()

        return view
    }

    /**
     * Menyiapkan RecyclerView dan menghubungkannya dengan HistoryAdapter.
     */
    private fun setupRecyclerView() {
        adapter = HistoryAdapter()
        rvHistory.layoutManager = LinearLayoutManager(context)
        rvHistory.adapter = adapter

        // Menambahkan listener ketika salah satu item riwayat diklik
        adapter.setOnItemClickCallback { item ->
            showDetailDialog(item)
        }
    }

    /**
     * Menampilkan dialog detail dari riwayat yang dipilih menggunakan BottomSheetDialog.
     * Terdapat juga fungsi untuk menghapus data riwayat tersebut.
     */
    private fun showDetailDialog(item: HistoryEntity) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_dialog_history_detail, null)

        val tvDevice = view.findViewById<TextView>(R.id.tvDetailDevice)
        val tvDate = view.findViewById<TextView>(R.id.tvDetailDate)
        val tvSpecs = view.findViewById<TextView>(R.id.tvDetailSpecs)
        val tvRes = view.findViewById<TextView>(R.id.tvDetailResistance)
        val tvSummaryText = view.findViewById<TextView>(R.id.tvDetailSummary)
        val btnDelete = view.findViewById<MaterialButton>(R.id.btnDelete)

        // Memasukkan data ke dalam elemen antarmuka dialog
        tvDevice.text = item.deviceName
        tvSpecs.text = "${item.cableType} • ${item.length} Meter"
        tvRes.text = item.resistance

        // Memformat timestamp menjadi format waktu yang mudah dibaca
        val fmt = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
        tvDate.text = fmt.format(Date(item.timestamp))

        // Mengatur teks rangkuman berdasarkan status kelulusan pengujian
        if (item.status.equals("LULUS", ignoreCase = true)) {
            tvSummaryText.text = "Hasil Validasi: MEMENUHI STANDAR (LULUS).\n\n" +
                    "Resistansi konduktor (${item.resistance}) berada dalam batas toleransi " +
                    "standar ASTM B 258/SNI untuk kabel tipe ${item.cableType} dengan panjang ${item.length} meter."
        } else {
            tvSummaryText.text = "Hasil Validasi: TIDAK MEMENUHI STANDAR (GAGAL).\n\n" +
                    "Resistansi konduktor (${item.resistance}) melebihi batas yang diizinkan " +
                    "untuk kabel tipe ${item.cableType}. Kemungkinan terdapat ketidakmurnian bahan atau ketidaksesuaian panjang."
        }

        // === KONFIGURASI TOMBOL HAPUS ===
        btnDelete.setOnClickListener {
            // Memuat tampilan kustom untuk dialog peringatan penghapusan
            val warningView = layoutInflater.inflate(R.layout.layout_dialog_warning, null)

            // Membangun AlertDialog secara berantai yang dipisah barisnya agar rapi
            val deleteDialog = android.app.AlertDialog.Builder(requireContext())
                .setView(warningView)
                .setCancelable(false)
                .create()

            // Membuat latar belakang dialog menjadi transparan agar desain kustom (rounded corners) terlihat
            deleteDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val btnCancel = warningView.findViewById<View>(R.id.btnCancelDelete)
            val btnConfirm = warningView.findViewById<View>(R.id.btnConfirmDelete)

            // Aksi membatalkan penghapusan
            btnCancel.setOnClickListener {
                deleteDialog.dismiss()
            }

            // Aksi mengonfirmasi penghapusan data
            btnConfirm.setOnClickListener {
                viewModel.deleteHistoryItem(requireContext(), item)
                deleteDialog.dismiss()
                dialog.dismiss()
            }

            deleteDialog.show()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    /**
     * Menyiapkan opsi waktu pada dropdown filter (Semua, Hari Ini, 7 Hari, 30 Hari).
     */
    private fun setupTimeDropdown() {
        val timeOptions = listOf("Semua Riwayat", "Hari Ini", "7 Hari Terakhir", "30 Hari Terakhir")
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, timeOptions)

        dropdownTime.setAdapter(arrayAdapter)
        dropdownTime.setText(timeOptions[0], false)

        // Listener saat salah satu opsi waktu dipilih
        dropdownTime.setOnItemClickListener { _, _, position, _ ->
            selectedTimeFilter = timeOptions[position]
            applyFilters()
        }
    }

    /**
     * Menyiapkan opsi nama perangkat pada dropdown filter.
     */
    private fun setupDeviceDropdown(deviceNames: List<String>) {
        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, deviceNames)

        dropdownDevice.setAdapter(arrayAdapter)

        // Listener saat salah satu opsi perangkat dipilih
        dropdownDevice.setOnItemClickListener { parent, _, position, _ ->
            selectedDeviceFilter = parent.getItemAtPosition(position).toString()
            applyFilters()
        }
    }

    /**
     * Mengambil seluruh data riwayat dari database menggunakan LiveData.
     * Perubahan data pada tabel akan otomatis memicu blok observe ini.
     */
    private fun loadAllData() {
        // Menggunakan rentang waktu yang sangat jauh ke depan untuk memastikan semua data terambil
        val farFuture = System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 100)

        db.historyDao().loadHistoryByRange(0, farFuture)
            .observe(viewLifecycleOwner) { list ->
                allHistoryList = list
                updateDeviceFilterList(list)
                applyFilters()
            }
    }

    /**
     * Mengekstrak nama-nama perangkat yang unik dari daftar riwayat yang ada,
     * kemudian memperbarui opsi di dalam dropdown perangkat.
     */
    private fun updateDeviceFilterList(list: List<HistoryEntity>) {
        // Memisahkan pemanggilan berantai untuk transformasi list
        val uniqueDevices = list
            .map { it.deviceName }
            .distinct()
            .toMutableList()

        // Menambahkan opsi "Semua" di urutan paling atas
        uniqueDevices.add(0, "Semua")

        setupDeviceDropdown(uniqueDevices)

        // Menjaga agar tampilan filter tetap sinkron
        if (selectedDeviceFilter == "Semua") {
            dropdownDevice.setText("Semua", false)
        }
    }

    /**
     * Menerapkan filter gabungan berdasarkan opsi 'Waktu' dan 'Perangkat' yang dipilih.
     * Mengatur visibilitas layout kosong atau daftar hasil filternya.
     */
    private fun applyFilters() {
        var filteredList = allHistoryList

        // 1. Filter berdasarkan nama perangkat (jika bukan "Semua")
        if (selectedDeviceFilter != "Semua") {
            filteredList = filteredList.filter { it.deviceName == selectedDeviceFilter }
        }

        val now = System.currentTimeMillis()

        // 2. Filter berdasarkan rentang waktu
        filteredList = when (selectedTimeFilter) {
            "Hari Ini" -> {
                val startOfDay = getStartOfDay(now)
                filteredList.filter { it.timestamp >= startOfDay }
            }
            "7 Hari Terakhir" -> {
                val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000)
                filteredList.filter { it.timestamp >= sevenDaysAgo }
            }
            "30 Hari Terakhir" -> {
                val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
                filteredList.filter { it.timestamp >= thirtyDaysAgo }
            }
            else -> filteredList // Untuk "Semua Riwayat"
        }

        // 3. Memperbarui antarmuka berdasarkan hasil penyaringan (filter)
        if (filteredList.isEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
            rvHistory.visibility = View.GONE
            tvSummary.text = "0 Data ditemukan"
        } else {
            layoutEmpty.visibility = View.GONE
            rvHistory.visibility = View.VISIBLE
            adapter.setData(filteredList)
            tvSummary.text = "Menampilkan ${filteredList.size} riwayat data"
        }
    }

    /**
     * Fungsi utilitas untuk mendapatkan waktu persis di jam 00:00:00
     * pada hari dari timestamp yang diberikan.
     */
    private fun getStartOfDay(time: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = time
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)

        return cal.timeInMillis
    }
}