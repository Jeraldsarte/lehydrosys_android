package com.example.lehydrosys

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.OkHttpClient
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Callback
import okhttp3.Call
import okhttp3.Response
import java.io.IOException

class FirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if notifications are enabled
        val prefs = getSharedPreferences("LeHydroSysPrefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("notificationsEnabled", true)) return

        val title = remoteMessage.notification?.title ?: "LeHydroSys Alert"
        val body = remoteMessage.notification?.body ?: "You have a new notification."

        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "lehydrosys_alerts"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "LeHydroSys Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification_on) // Use your notification icon
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    override fun onNewToken(token: String) {
    super.onNewToken(token)
    // Send this token to your server
    sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        // Example using OkHttp
        val client = OkHttpClient()
        val requestBody = FormBody.Builder()
            .add("fcmToken", token)
            .build()
        val request = Request.Builder()
            .url("https://lehydrosys-sqfy.onrender.com/api/register_token") // Replace with your server endpoint
            .post(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {}
        })
    }
}