package com.hanafi.pelacak // Sesuaikan dengan package Anda jika berbeda

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

    @SuppressLint("MissingPermission") // Kita akan tangani izin di MainActivity nanti
    override suspend fun doWork(): Result {
        return try {
            // 1. Siapkan alat pencari lokasi
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
            
            // 2. Ambil lokasi saat ini (Akurasi seimbang agar hemat baterai)
            val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()

            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                
                Log.d("PelacakNinja", "Lokasi Ditemukan: $latitude, $longitude")

                // 3. Kirim ke server/database Anda (Gunakan fungsi jaringan bawaan agar hemat RAM)
                kirimKeServer(latitude, longitude)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("PelacakNinja", "Gagal mengambil/mengirim lokasi: ${e.message}")
            Result.retry() // Coba lagi nanti jika gagal (misal tidak ada sinyal)
        }
    }

    private suspend fun kirimKeServer(lat: Double, lon: Double) {
        // Pindah ke jalur belakang (IO) agar aplikasi tidak macet
        withContext(Dispatchers.IO) {
            try {
                // SEMENTARA: Kita akan tembak ke bot Railway Anda!
                // Ganti URL ini dengan URL Railway Anda nanti
                val urlRailway = "https://NAMA-RAILWAY-ANDA.up.railway.app/update-lokasi?lat=$lat&lon=$lon"
                
                val url = URL(urlRailway)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000 // Maksimal nunggu 5 detik
                
                val responseCode = connection.responseCode
                Log.d("PelacakNinja", "Status Pengiriman: $responseCode")
                
                connection.disconnect()
            } catch (e: Exception) {
                Log.e("PelacakNinja", "Gagal mengirim ke server: ${e.message}")
            }
        }
    }
}