package com.shinji.cablevalidator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

/**
 * Fragment yang berfungsi untuk menampilkan halaman panduan atau referensi aplikasi.
 * Digunakan untuk memberikan informasi dan instruksi penggunaan kepada pengguna.
 */
class GuideFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Mengonversi (inflate) file desain XML 'fragment_guide' menjadi objek View
        // agar dapat dirender dan ditampilkan pada layar antarmuka pengguna
        return inflater.inflate(
            R.layout.fragment_guide,
            container,
            false
        )
    }
}