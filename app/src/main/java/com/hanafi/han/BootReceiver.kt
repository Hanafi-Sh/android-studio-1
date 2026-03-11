package com.hanafi.han

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Jika sinyal yang diterima adalah "HP Selesai Menyala"
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("PelacakNinja", "HP menyala! Membangkitkan ulang radar pelacak...")

            // Siapkan ulang jadwal pekerja (WorkManager) persis seperti di MainActivity
            val requestPelacak = PeriodicWorkRequestBuilder<LocationWorker>(15, TimeUnit.MINUTES).build()
            
            // Masukkan kembali ke sistem Android
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "PelacakSilumanWork",
                ExistingPeriodicWorkPolicy.KEEP, 
                requestPelacak
            )
        }
    }
}