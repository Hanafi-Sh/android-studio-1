package com.hanafi.han

import android.provider.Settings
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val idUnik = androidId?.take(6)?.uppercase() ?: "UNKNOWN"
        val targetId = "HP_$idUnik"
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Cari elemen di layar berdasarkan ID di XML Anda
        val inputNama = findViewById<EditText>(R.id.inputNamaPerangkat)
        val btnMulai = findViewById<Button>(R.id.btnMulai) // Pastikan ID ini sesuai dengan di XML

        // 2. Pasang aksi HANYA SATU KALI saat tombol diklik
        btnMulai.setOnClickListener {
            // Ambil teks yang diketik pengguna
            val namaDiketik = inputNama.text.toString().trim()

            // Simpan ke Brankas Lokal HP
            val sharedPref = getSharedPreferences("PelacakPrefs", Context.MODE_PRIVATE)
            sharedPref.edit().putString("nama_input", namaDiketik).apply()

            Toast.makeText(this, "Melacak sebagai: ${if(namaDiketik.isEmpty()) "Anonim" else namaDiketik}", Toast.LENGTH_SHORT).show()

            // PENTING: Panggil fungsi cek izin dulu, jangan langsung jalankanWorker
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
            // Jika izin aman, cek baterai!
            cekBateraiDanJalankan()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Jika izin lokasi (foreground) diberikan, cek lagi (ini akan memicu permintaan background jika perlu)
            cekIzinDanJalankan()
        } else if (requestCode == 2 && grantResults.isNotEmpty()) {
            // Izin background
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cekBateraiDanJalankan()
            } else {
                Toast.makeText(this, "Pelacakan berjalan, namun mungkin berhenti saat aplikasi ditutup.", Toast.LENGTH_LONG).show()
                cekBateraiDanJalankan()
            }
        } else if (requestCode == 1) {
            Toast.makeText(this, "Aplikasi butuh izin lokasi untuk melacak!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cekBateraiDanJalankan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            val packageName = packageName
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
                Toast.makeText(this, "Mohon izinkan 'Unrestricted' agar pelacak tidak mati.", Toast.LENGTH_LONG).show()
            }
        }
        jalankanWorker()
    }

    private fun jalankanWorker() {
        val workManager = WorkManager.getInstance(applicationContext)

        // 1. PELURU INSTAN: Tembak sinyal SEKARANG JUGA untuk testing!
        val pelacakInstan = OneTimeWorkRequestBuilder<LocationWorker>().build()
        workManager.enqueue(pelacakInstan)

        // 2. JADWAL RUTIN: Tembak setiap 15 menit sekali
        val pelacakRequest = PeriodicWorkRequestBuilder<LocationWorker>(15, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(
            "PelacakSilumanWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            pelacakRequest
        )

        Toast.makeText(this, "Sinyal pertama ditembakkan! Jadwal 15 menit diaktifkan.", Toast.LENGTH_LONG).show()
    }
}