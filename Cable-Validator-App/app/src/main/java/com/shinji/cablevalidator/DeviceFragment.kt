package com.shinji.cablevalidator

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors

/**
 * Fragment yang bertanggung jawab untuk mengelola antarmuka pemindaian
 * dan koneksi perangkat Bluetooth Low Energy (BLE).
 */
class DeviceFragment : Fragment() {

    // Komponen UI untuk bagian pemindaian perangkat
    private lateinit var btnScan: MaterialButton
    private lateinit var tvDeviceCount: TextView
    private lateinit var rvDeviceList: RecyclerView
    private lateinit var deviceAdapter: DeviceAdapter

    // Komponen UI untuk bagian status koneksi perangkat
    private lateinit var tvStatusTitle: TextView
    private lateinit var tvStatusMain: TextView
    private lateinit var tvStatusSub: TextView
    private lateinit var layoutActionConnected: LinearLayout
    private lateinit var tvRssiLive: TextView
    private lateinit var btnDisconnect: MaterialButton

    // ViewModel untuk berbagi status koneksi dan data Bluetooth antar komponen
    private lateinit var viewModel: ScanViewModel
    private var bluetoothAdapter: BluetoothAdapter? = null

    // Handler untuk menjalankan tugas secara asinkron di Main Thread (UI Thread)
    private val handler = Handler(Looper.getMainLooper())

    // Durasi maksimal pemindaian perangkat (10 detik)
    private val SCAN_PERIOD: Long = 10000

    /**
     * Runnable yang bertugas untuk membaca nilai RSSI (kekuatan sinyal)
     * secara berkala setiap 2 detik ketika perangkat terhubung.
     */
    private val rssiRunnable = object : Runnable {
        @SuppressLint("MissingPermission")
        override fun run() {
            if (viewModel.isConnected) {
                // Meminta pembaruan nilai RSSI dari perangkat yang terhubung
                viewModel.bluetoothGatt?.readRemoteRssi()

                // Menjadwalkan ulang eksekusi runnable ini setelah 2 detik
                handler.postDelayed(this, 2000)
            }
        }
    }

    /**
     * Penangan (Launcher) untuk meminta izin akses yang diperlukan secara dinamis.
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Memeriksa apakah semua izin yang diminta telah diberikan
        val allGranted = permissions.entries
            .all { it.value }

        if (allGranted) {
            startBleScan(false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Menginisialisasi layout untuk fragment ini
        val view = inflater.inflate(R.layout.fragment_device, container, false)

        // Binding ID komponen UI pemindaian
        btnScan = view.findViewById(R.id.btnScan)
        tvDeviceCount = view.findViewById(R.id.tvDeviceCount)
        rvDeviceList = view.findViewById(R.id.rvDeviceList)

        // Binding ID komponen UI status perangkat
        tvStatusMain = view.findViewById(R.id.tvStatusMain)
        tvStatusSub = view.findViewById(R.id.tvStatusSub)
        layoutActionConnected = view.findViewById(R.id.layoutActionConnected)
        tvRssiLive = view.findViewById(R.id.tvRssiLive)
        btnDisconnect = view.findViewById(R.id.btnDisconnect)

        // Menginisialisasi adapter Bluetooth dari sistem
        val bluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // Mengambil instance ViewModel yang terikat dengan Activity
        viewModel = ViewModelProvider(requireActivity())[ScanViewModel::class.java]

        // Menyiapkan daftar RecyclerView
        setupRecyclerView()

        btnScan.text = "Mulai Pindai Perangkat"

        // Mengatur aksi tombol pindai (toggle antara mulai dan henti)
        btnScan.setOnClickListener {
            if (viewModel.isScanning) {
                stopBleScan()
            } else {
                checkPermissionsAndScan()
            }
        }

        // Mengatur aksi tombol putuskan koneksi
        btnDisconnect.setOnClickListener {
            viewModel.disconnect()
        }

        tvDeviceCount.text = "0 Ditemukan"

        // Memasang pengamat (observer) untuk merespons perubahan data pada ViewModel
        setupViewModelObservers()

        return view
    }

    /**
     * Mendaftarkan pengamat (observer) untuk memantau status koneksi dan nilai sinyal.
     */
    private fun setupViewModelObservers() {
        // Memantau status koneksi (terhubung atau terputus)
        viewModel.connectionState.observe(viewLifecycleOwner) { isConnected ->
            // Memaksa pembaruan menu opsi di Activity
            requireActivity().invalidateOptionsMenu()

            // Memperbarui tampilan kartu status
            updateCardUI(isConnected, viewModel.connectedDevice)

            if (isConnected) {
                // Logika otomatis saat perangkat berhasil terhubung:
                // 1. Mulai membaca RSSI secara berkala
                handler.post(rssiRunnable)
                // 2. Hentikan pemindaian agar hemat baterai
                stopBleScan()
                // 3. Bersihkan daftar perangkat yang ditemukan
                deviceAdapter.clearDevices()
                // 4. Atur ulang penghitung perangkat
                tvDeviceCount.text = "0 Ditemukan"
            } else {
                // Menghentikan pembacaan RSSI jika koneksi terputus
                handler.removeCallbacks(rssiRunnable)
            }
        }

        // Memantau pembaruan nilai kekuatan sinyal (RSSI)
        viewModel.rssiData.observe(viewLifecycleOwner) { rssi ->
            tvRssiLive.text = "$rssi dBm"
        }
    }

    /**
     * Memperbarui antarmuka pengguna pada kartu status berdasarkan kondisi koneksi.
     */
    @SuppressLint("MissingPermission")
    private fun updateCardUI(isConnected: Boolean, device: BluetoothDevice?) {
        if (isConnected && device != null) {
            val deviceName = device.name ?: "Unknown Device"
            val deviceMac = device.address ?: ""

            tvStatusMain.text = deviceName

            // Mengambil warna tema secara dinamis untuk teks utama
            val colorOnSurface = MaterialColors.getColor(
                tvStatusMain,
                com.google.android.material.R.attr.colorOnSurface
            )
            tvStatusMain.setTextColor(colorOnSurface)

            tvStatusSub.text = "Terhubung • $deviceMac"
            tvStatusSub.setTextColor(Color.parseColor("#4CAF50")) // Warna hijau sukses

            layoutActionConnected.visibility = View.VISIBLE
        } else {
            tvStatusMain.text = "Belum Terhubung"

            val colorOnSurface = MaterialColors.getColor(
                tvStatusMain,
                com.google.android.material.R.attr.colorOnSurface
            )
            tvStatusMain.setTextColor(colorOnSurface)

            tvStatusSub.text = "Hubungkan ke Perangkat terlebih dahulu"

            val colorOnSurfaceVariant = MaterialColors.getColor(
                tvStatusSub,
                com.google.android.material.R.attr.colorOnSurfaceVariant
            )
            tvStatusSub.setTextColor(colorOnSurfaceVariant)

            layoutActionConnected.visibility = View.GONE
            tvRssiLive.text = "- dBm"
        }
    }

    override fun onResume() {
        super.onResume()
        updateCardUI(viewModel.isConnected, viewModel.connectedDevice)

        // Memulai ulang pemindaian hanya jika sebelumnya sedang memindai dan belum terhubung
        if (viewModel.isScanning && !viewModel.isConnected) {
            startBleScan(true)
        }
        // Melanjutkan pembacaan RSSI jika sudah terhubung
        if (viewModel.isConnected) {
            handler.post(rssiRunnable)
        }
    }

    override fun onPause() {
        super.onPause()
        // Menghentikan pemindaian dan pembaruan RSSI saat fragment tidak terlihat (masuk background)
        bluetoothAdapter?.bluetoothLeScanner
            ?.stopScan(scanCallback)

        handler.removeCallbacks(rssiRunnable)
    }

    /**
     * Fungsi pembantu untuk menghubungkan perangkat yang dipilih dari daftar.
     */
    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        stopBleScan()
        viewModel.connect(requireContext(), device)
    }

    /**
     * Menyiapkan adapter dan layout manager untuk daftar perangkat.
     */
    private fun setupRecyclerView() {
        deviceAdapter = DeviceAdapter { device -> connectToDevice(device) }
        rvDeviceList.layoutManager = LinearLayoutManager(context)
        rvDeviceList.adapter = deviceAdapter
    }

    /**
     * Memeriksa izin akses yang diperlukan berdasarkan versi Android sebelum melakukan pemindaian.
     */
    private fun checkPermissionsAndScan() {
        if (bluetoothAdapter?.isEnabled == false) return

        val permissionsToRequest = mutableListOf<String>()

        // Android 12 (API 31) ke atas memerlukan izin Bluetooth yang lebih spesifik
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // Android versi lama memerlukan izin lokasi untuk memindai Bluetooth
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Menyaring izin mana saja yang belum diberikan oleh pengguna
        val missingPermissions = permissionsToRequest
            .filter {
                ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
            }

        if (missingPermissions.isEmpty()) {
            startBleScan(false)
        } else {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    /**
     * Memulai proses pemindaian perangkat Bluetooth Low Energy (BLE).
     */
    @SuppressLint("MissingPermission")
    private fun startBleScan(isResuming: Boolean) {
        // Mencegah pemindaian jika sudah ada perangkat yang terhubung
        if (viewModel.isConnected) return

        if (!isResuming) {
            deviceAdapter.clearDevices()
            viewModel.scannedDevices.clear()
        }

        viewModel.isScanning = true
        btnScan.text = "Hentikan Pemindaian Perangkat"

        // Memperbarui teks penghitung perangkat
        tvDeviceCount.text = if (isResuming) {
            "${deviceAdapter.itemCount} Ditemukan"
        } else {
            "Mencari..."
        }

        // Memulai pemindaian secara aman menggunakan safe call berantai
        bluetoothAdapter?.bluetoothLeScanner
            ?.startScan(scanCallback)

        // Menjadwalkan penghentian otomatis setelah batas waktu (SCAN_PERIOD) habis
        handler.postDelayed({
            if (viewModel.isScanning) {
                stopBleScan()
            }
        }, SCAN_PERIOD)
    }

    /**
     * Menghentikan proses pemindaian perangkat BLE secara manual.
     */
    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        viewModel.isScanning = false
        btnScan.text = "Mulai Pindai Perangkat"
        tvDeviceCount.text = "${deviceAdapter.itemCount} Ditemukan"

        // Menghentikan pemindaian secara aman
        bluetoothAdapter?.bluetoothLeScanner
            ?.stopScan(scanCallback)
    }

    /**
     * Callback yang akan dipanggil setiap kali pemindai menemukan perangkat Bluetooth baru.
     */
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                // Mengabaikan perangkat yang tidak memiliki nama
                if (it.device.name.isNullOrEmpty()) return

                // Menambahkan perangkat yang valid ke dalam adapter
                val newDevice = ScannedDevice(it.device, it.rssi)
                deviceAdapter.addDevice(newDevice)

                tvDeviceCount.text = "${deviceAdapter.itemCount} Ditemukan"

                // Memperbarui daftar cadangan di ViewModel agar tidak hilang saat rotasi layar
                viewModel.scannedDevices = deviceAdapter.getDevices()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            // Penanganan jika proses pemindaian gagal diinisialisasi
            viewModel.isScanning = false
            btnScan.text = "Mulai Pindai Perangkat"
        }
    }
}