package com.shinji.cablevalidator

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * ViewModel yang bertanggung jawab mengelola proses pemindaian (scanning),
 * koneksi komunikasi Bluetooth Low Energy (BLE), dan penyimpanan database riwayat.
 * Menjaga data agar tetap aman dan tidak hilang saat terjadi perubahan siklus hidup UI (seperti rotasi layar).
 */
class ScanViewModel : ViewModel() {

    // =========================================================================
    // 1. DATA PEMINDAIAN (SCANNING)
    // =========================================================================
    var scannedDevices = ArrayList<ScannedDevice>()
    var isScanning: Boolean = false

    // =========================================================================
    // 2. LIVE DATA (Dipantau oleh UI)
    // =========================================================================
    val connectionState = MutableLiveData<Boolean>(false)
    val connectedDeviceData = MutableLiveData<BluetoothDevice?>()
    val configData = MutableLiveData<String>()
    val rssiData = MutableLiveData<Int>()

    // Menyimpan data status pengukuran (misal: LULUS, GAGAL, MENGUKUR, atau ERROR)
    val resistanceData = MutableLiveData<String>()

    // =========================================================================
    // 3. KONFIGURASI UUID BLE (Sesuai dengan hardware ESP32/Nordic)
    // =========================================================================
    val SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    val CHAR_WRITE_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    val CHAR_READ_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
    val DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    @SuppressLint("StaticFieldLeak")
    var bluetoothGatt: BluetoothGatt? = null

    val isConnected: Boolean
        get() = connectionState.value == true

    val connectedDevice: BluetoothDevice?
        get() = connectedDeviceData.value

    private val handler = Handler(Looper.getMainLooper())

    /**
     * Memulai koneksi GATT ke perangkat Bluetooth yang dipilih.
     */
    @SuppressLint("MissingPermission")
    fun connect(context: Context, device: BluetoothDevice) {
        // Putuskan koneksi sebelumnya jika ada
        disconnect()

        // Membersihkan sisa data pengukuran di memori saat mulai koneksi baru
        resistanceData.postValue("")

        connectedDeviceData.postValue(device)
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    /**
     * Memutuskan koneksi dengan aman.
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        try {
            bluetoothGatt?.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        closeConnection()
    }

    /**
     * Menutup koneksi dan membersihkan referensi objek GATT untuk mencegah kebocoran memori (Memory Leak).
     */
    @SuppressLint("MissingPermission")
    private fun closeConnection() {
        try {
            bluetoothGatt?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        bluetoothGatt = null
        connectionState.postValue(false)
        connectedDeviceData.postValue(null)

        // Membersihkan sisa data saat koneksi terputus
        resistanceData.postValue("")
    }

    /**
     * Mengirimkan data string (konfigurasi) ke perangkat keras melalui BLE.
     */
    @SuppressLint("MissingPermission")
    fun writeConfig(dataString: String) {
        if (bluetoothGatt == null) return

        val service = bluetoothGatt?.getService(SERVICE_UUID)
        val charWrite = service?.getCharacteristic(CHAR_WRITE_UUID)

        if (charWrite != null) {
            charWrite.setValue(dataString)
            bluetoothGatt?.writeCharacteristic(charWrite)
        }
    }

    /**
     * Meminta untuk membaca data konfigurasi dari perangkat keras.
     */
    @SuppressLint("MissingPermission")
    fun readConfig() {
        if (bluetoothGatt == null) return

        val service = bluetoothGatt?.getService(SERVICE_UUID)
        val charRead = service?.getCharacteristic(CHAR_READ_UUID)

        if (charRead != null) {
            bluetoothGatt?.readCharacteristic(charRead)
        }
    }

    /**
     * Callback yang menangani semua respon asinkron dari operasi Bluetooth GATT.
     */
    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            // Jika terjadi kesalahan pada level protokol GATT
            if (status != BluetoothGatt.GATT_SUCCESS) {
                closeConnection()
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionState.postValue(true)
                connectedDeviceData.postValue(gatt?.device)

                // Menunggu sejenak sebelum memulai penemuan layanan (Service Discovery)
                handler.postDelayed({
                    gatt?.discoverServices()
                }, 500)

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                closeConnection()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt?.getService(SERVICE_UUID)
                val charRead = service?.getCharacteristic(CHAR_READ_UUID)

                if (charRead != null) {
                    // Beri izin aplikasi untuk mendengarkan notifikasi data dari perangkat lunak mikrokontroler (ESP32/lainnya)
                    gatt.setCharacteristicNotification(charRead, true)

                    val descriptor = charRead.getDescriptor(DESCRIPTOR_UUID)
                    if (descriptor != null) {
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                    }

                    // Membaca konfigurasi awal setelah layanan berhasil dikonfigurasi
                    handler.postDelayed({
                        readConfig()
                    }, 300)
                }
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                rssiData.postValue(rssi)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            processData(characteristic)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            processData(characteristic)
        }
    }

    /**
     * Memproses dan memilah data yang diterima dari perangkat Bluetooth (Hardware).
     */
    private fun processData(characteristic: BluetoothGattCharacteristic) {
        if (characteristic.uuid == CHAR_READ_UUID) {
            val data = characteristic.getStringValue(0)

            if (data != null) {
                // 1. Memilah data: Jika pesan diawali dengan "CFG:", itu adalah Konfigurasi Limit/Toleransi
                if (data.startsWith("CFG:")) {
                    configData.postValue(data)
                }
                // 2. Selain itu (IDLE, MENGUKUR, LULUS, GAGAL, ERROR), diteruskan sebagai Data Pengukuran real-time
                else {
                    resistanceData.postValue(data)
                }
            }
        }
    }

    /**
     * Dipanggil ketika ViewModel dihancurkan. Memastikan koneksi terputus dengan aman.
     */
    override fun onCleared() {
        super.onCleared()
        disconnect()
    }

    // =========================================================================
    // 4. FUNGSI DATABASE (ROOM) MENGGUNAKAN COROUTINES
    // =========================================================================

    /**
     * Menyimpan hasil pengujian ke dalam database lokal.
     */
    fun saveToHistory(
        context: Context,
        specs: String,
        length: String,
        resistance: String,
        status: String
    ) {
        // Menjalankan operasi database di thread Background (IO) agar UI tidak terhenti (lag)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                val deviceName = connectedDeviceData.value?.name ?: "Unknown Device"

                val historyItem = HistoryEntity(
                    deviceName = deviceName,
                    cableType = specs,
                    length = length,
                    resistance = resistance,
                    status = status,
                    timestamp = System.currentTimeMillis()
                )

                db.historyDao().insert(historyItem)

                // Beralih kembali ke Main Thread (UI) untuk menampilkan Toast konfirmasi
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Data berhasil disimpan ke Riwayat!", Toast.LENGTH_SHORT)
                        .show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    /**
     * Menghapus sebuah entri riwayat spesifik dari database lokal.
     */
    fun deleteHistoryItem(context: Context, item: HistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                db.historyDao().delete(item)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Data berhasil dihapus", Toast.LENGTH_SHORT)
                        .show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}