package com.example.smsreceiver

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "channel_id",
                "Listener",
                NotificationManager.IMPORTANCE_HIGH
            )

            val notificationService = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationService.createNotificationChannel(channel)
        }
    }
}