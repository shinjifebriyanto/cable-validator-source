package com.shinji.cablevalidator

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

/**
 * Model data untuk menyimpan informasi perangkat Bluetooth yang dipindai
 * beserta kekuatan sinyal (RSSI) yang diterima.
 */
data class ScannedDevice(
    val device: BluetoothDevice,
    val rssi: Int
)

/**
 * Adapter RecyclerView untuk menampilkan daftar perangkat Bluetooth yang ditemukan.
 * Menangani interaksi klik pada tombol hubungkan melalui fungsi callback (onClick).
 */
class DeviceAdapter(
    private val onClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    // Daftar internal untuk menyimpan data perangkat yang berhasil dipindai
    private var devices = ArrayList<ScannedDevice>()

    /**
     * Mengambil daftar perangkat saat ini.
     * Berguna untuk menyimpan status (state) ke dalam ViewModel
     * agar data tidak hilang saat terjadi perubahan konfigurasi.
     */
    fun getDevices(): ArrayList<ScannedDevice> {
        return devices
    }

    /**
     * Memulihkan daftar perangkat dari data yang telah tersimpan sebelumnya
     * (misalnya setelah perubahan rotasi layar atau tema).
     */
    fun setDevices(savedList: ArrayList<ScannedDevice>) {
        // Membuat salinan (copy) daftar baru untuk mencegah mutasi data yang tidak diinginkan
        devices = ArrayList(savedList)

        // Memperbarui seluruh tampilan antarmuka
        notifyDataSetChanged()
    }

    /**
     * Menambahkan perangkat baru ke dalam daftar atau memperbarui perangkat
     * yang sudah ada jika alamat MAC-nya sama.
     */
    fun addDevice(newDevice: ScannedDevice) {
        // Memeriksa indeks perangkat berdasarkan alamat MAC (MAC address)
        val index = devices.indexOfFirst { it.device.address == newDevice.device.address }

        if (index != -1) {
            // Jika perangkat sudah terdaftar, perbarui hanya nilai sinyalnya (RSSI)
            devices[index] = newDevice

            // Memberi tahu adapter untuk memperbarui item pada indeks tertentu
            notifyItemChanged(index)
        } else {
            // Jika perangkat belum terdaftar, tambahkan sebagai entri baru di akhir daftar
            devices.add(newDevice)

            // Memberi tahu adapter bahwa ada item baru yang disisipkan
            notifyItemInserted(devices.size - 1)
        }
    }

    /**
     * Menghapus semua daftar perangkat dari memori tampilan.
     */
    fun clearDevices() {
        devices.clear()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return devices.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        // Memisahkan pemanggilan berantai ke baris baru agar lebih rapi
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_device, parent, false)

        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    /**
     * ViewHolder yang merepresentasikan tata letak (layout) untuk setiap item perangkat
     * di dalam RecyclerView.
     */
    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // Inisialisasi komponen UI untuk setiap baris item
        private val tvName: TextView = itemView.findViewById(R.id.tvDeviceName)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvMacAddress)
        private val tvRssi: TextView = itemView.findViewById(R.id.tvRssi)
        private val btnConnect: MaterialButton = itemView.findViewById(R.id.btnConnect)

        /**
         * Mengikat data (binding) dari ScannedDevice ke komponen antarmuka pengguna.
         */
        @SuppressLint("MissingPermission")
        fun bind(data: ScannedDevice) {
            // Menampilkan nama perangkat, atau "Unknown Device" jika tidak memiliki nama
            tvName.text = data.device.name ?: "Unknown Device"
            tvAddress.text = data.device.address
            tvRssi.text = "${data.rssi} dBm"

            // Mendaftarkan listener untuk tombol hubungkan (connect)
            btnConnect.setOnClickListener {
                // Memanggil callback onClick yang diteruskan dari Activity/Fragment
                onClick(data.device)
            }
        }
    }
}