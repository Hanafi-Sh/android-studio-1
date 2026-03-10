package com.hanafi.han

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Mencari tombol di layar berdasarkan ID-nya
        val btnMulai = findViewById<Button>(R.id.btnMulai)

        btnMulai.setOnClickListener {
            cekIzinDanJalankan()
        }
    }

    private fun cekIzinDanJalankan() {
        val izinLokasi = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        if (izinLokasi) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val izinBackground = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (!izinBackground) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 2)
                    Toast.makeText(this, "Pilih 'Allow all the time' agar bisa melacak saat ditutup", Toast.LENGTH_LONG).show()
                    return
                }
            }
            jalankanWorker()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1
            )
        }
    }

    private fun jalankanWorker() {
        val pelacakRequest = PeriodicWorkRequestBuilder<LocationWorker>(15, TimeUnit.MINUTES).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "PelacakSilumanWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            pelacakRequest
        )

        Toast.makeText(this, "Pelacak diaktifkan! Anda bisa menutup aplikasi ini.", Toast.LENGTH_LONG).show()
    }
}