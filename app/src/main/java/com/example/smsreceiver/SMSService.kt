package com.example.smsreceiver

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.provider.Telephony
import android.telephony.SmsMessage
import androidx.core.app.NotificationCompat
import com.example.smsreceiver.db.SMSDao
import com.example.smsreceiver.db.SMSDatabase
import com.example.smsreceiver.model.SMS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SMSService : Service() {

    private var serviceScope = CoroutineScope(Dispatchers.IO)
    private var smsReceiver: SMSReceiver = SMSReceiver()
    private lateinit var dao: SMSDao

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        dao = SMSDatabase.create(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        registerReceiver(smsReceiver, IntentFilter("android.provider.Telephony.SMS_RECEIVED"))
        startForeground(1, getNotification())
        return START_STICKY
    }

    private fun getNotification(): Notification {
        return NotificationCompat
            .Builder(this, "channel_id")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("SMS Tracker")
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    inner class SMSReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                val message = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                val number = message.getOrNull(0)?.displayOriginatingAddress ?: "Unknown"
                val content = message.getOrNull(0)?.displayMessageBody ?: "Unreadable SMS content"

                serviceScope.launch {
                    dao.insertSMS(
                        SMS(
                            number = number,
                            body = content
                        )
                    )
                }
            }
        }
    }
}