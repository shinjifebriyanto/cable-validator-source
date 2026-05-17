package com.shinji.cablevalidator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter untuk menampilkan daftar riwayat pengujian/validasi kabel pada RecyclerView.
 * Mengelola perubahan antarmuka secara dinamis berdasarkan status kelulusan (Lulus/Gagal).
 */
class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    // Daftar internal untuk menyimpan data entitas riwayat
    private var historyList = emptyList<HistoryEntity>()

    // Fungsi callback (panggilan balik) untuk menangani kejadian klik pada item
    private var onItemClickCallback: ((HistoryEntity) -> Unit)? = null

    /**
     * Mendaftarkan fungsi callback yang akan dieksekusi ketika pengguna mengklik salah satu item riwayat.
     */
    fun setOnItemClickCallback(onItemClick: (HistoryEntity) -> Unit) {
        this.onItemClickCallback = onItemClick
    }

    /**
     * Memperbarui daftar data riwayat dan meminta adapter untuk merender ulang tampilan (refresh).
     */
    fun setData(newList: List<HistoryEntity>) {
        this.historyList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Memisahkan pemanggilan berantai (chained methods) ke baris baru agar lebih mudah dibaca
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_history, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = historyList[position]
        val context = holder.itemView.context

        // Menyisipkan data teks utama ke dalam komponen UI
        // Catatan: Warna dasar (Hitam/Putih) akan otomatis menyesuaikan tema aplikasi (Terang/Gelap)
        holder.tvDeviceName.text = item.deviceName
        holder.tvSpecs.text = "${item.cableType} • ${item.length} Meter"
        holder.tvResistance.text = item.resistance

        // Memformat nilai timestamp menjadi format tanggal dan waktu yang mudah dipahami manusia
        val fmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        holder.tvDate.text = fmt.format(Date(item.timestamp))
        holder.tvStatusText.text = item.status

        try {
            // Evaluasi status kelulusan untuk menentukan tema warna pada setiap kartu (item list)
            if (item.status.equals("LULUS", ignoreCase = true)) {

                // === KONFIGURASI TEMA WARNA HIJAU (LULUS) ===
                val colorDark = ContextCompat.getColor(context, R.color.status_lulus_dark) // Warna Hijau Tua
                val colorBg   = ContextCompat.getColor(context, R.color.status_lulus_bg)   // Latar Hijau Lembut (Soft)

                // 1. Mengatur warna latar (background) dan garis tepi (stroke) pada Kartu Utama
                holder.cardRoot.setCardBackgroundColor(colorBg)
                holder.cardRoot.strokeColor = colorDark

                // 2. Mengatur warna dan latar teks status ("LULUS") dengan efek sudut melengkung
                holder.tvStatusText.setTextColor(colorDark)
                holder.tvStatusText.background = ContextCompat.getDrawable(context, R.drawable.bg_status_badge)
                holder.tvStatusText.background.setTint(colorBg)

                // 3. Mengatur ikon status menjadi tanda centang (Check) beserta lingkarannya
                holder.ivStatus.setImageResource(R.drawable.ic_check_circle)
                holder.ivStatus.setColorFilter(colorDark)
                // Memanfaatkan latar lingkaran (circle) yang sudah tersedia
                holder.ivStatus.background = ContextCompat.getDrawable(context, R.drawable.bg_circle_soft)
                holder.ivStatus.background.setTint(colorBg)

            } else {

                // === KONFIGURASI TEMA WARNA MERAH (GAGAL) ===
                val colorDark = ContextCompat.getColor(context, R.color.status_gagal_dark) // Warna Merah Tua
                val colorBg   = ContextCompat.getColor(context, R.color.status_gagal_bg)   // Latar Merah Lembut (Soft)

                // 1. Mengatur warna latar (background) dan garis tepi (stroke) pada Kartu Utama
                holder.cardRoot.setCardBackgroundColor(colorBg)
                holder.cardRoot.strokeColor = colorDark

                // 2. Mengatur warna dan latar teks status ("GAGAL") dengan efek sudut melengkung
                holder.tvStatusText.setTextColor(colorDark)
                holder.tvStatusText.background = ContextCompat.getDrawable(context, R.drawable.bg_status_badge)
                holder.tvStatusText.background.setTint(colorBg)

                // 3. Mengatur ikon status menjadi tanda silang (Cancel) beserta lingkarannya
                holder.ivStatus.setImageResource(R.drawable.ic_cancel)
                holder.ivStatus.setColorFilter(colorDark)
                holder.ivStatus.background = ContextCompat.getDrawable(context, R.drawable.bg_circle_soft)
                holder.ivStatus.background.setTint(colorBg)
            }
        } catch (e: Exception) {
            // Menangani kesalahan rendering UI agar aplikasi tidak tertutup paksa (force close)
            e.printStackTrace()
        }

        // Mendaftarkan event listener untuk memicu callback ketika sebuah kartu diklik
        holder.itemView.setOnClickListener {
            onItemClickCallback?.invoke(item)
        }
    }

    override fun getItemCount(): Int {
        return historyList.size
    }

    /**
     * ViewHolder bertugas untuk menyimpan referensi dari masing-masing elemen antarmuka (View)
     * di dalam layout `list_item_history`, sehingga mempercepat proses pemuatan dan pengikatan data.
     */
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardRoot: MaterialCardView = itemView.findViewById(R.id.cardRoot)
        val tvDeviceName: TextView = itemView.findViewById(R.id.tvDeviceName)
        val tvSpecs: TextView = itemView.findViewById(R.id.tvSpecs)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvResistance: TextView = itemView.findViewById(R.id.tvResistance)
        val tvStatusText: TextView = itemView.findViewById(R.id.tvStatusText)
        val ivStatus: ImageView = itemView.findViewById(R.id.ivStatus)
    }
}