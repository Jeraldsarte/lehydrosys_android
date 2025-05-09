package com.example.lehydrosys.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.widget.Toast
import java.util.*
import com.example.lehydrosys.R

object HydroNotificationManager {

    private const val CHANNEL_ID = "hydro_alerts_channel"
    private const val CHANNEL_NAME = "Hydroponic Alerts"
    private const val CHANNEL_DESC = "Alerts for out-of-range hydroponic sensor data"
    private const val NOTIFICATION_ID = 1 // Use a fixed notification ID for combined notifications
    private const val SHARED_PREFS_NAME = "LeHydroSysPrefs"
    private const val NOTIFICATIONS_ENABLED_KEY = "notificationsEnabled"
    private const val LAST_NOTIFICATION_TIME_KEY = "lastNotificationTime"

    private var notificationIdCounter = 1

    init {
        // Initialize channel
    }

    fun initializeChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    // This method checks if notifications are enabled before sending a notification
    private fun isNotificationsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(NOTIFICATIONS_ENABLED_KEY, true)
    }

    // This method checks if it's been at least 5 minutes since the last notification
    private fun canSendNotification(context: Context): Boolean {
        val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        val lastSentTime = prefs.getLong(LAST_NOTIFICATION_TIME_KEY, 0L)

        val currentTime = System.currentTimeMillis()
        val fiveMinutesMillis = 5 * 60 * 1000 // 5 minutes in milliseconds

        // Check if 5 minutes have passed
        return (currentTime - lastSentTime) >= fiveMinutesMillis
    }

    fun checkSensorDataAndNotify(
        context: Context,
        temperature: Float,
        humidity: Float,
        waterTemp: Float,
        ph: Float,
        tds: Float,
        waterLevel: Float
    ) {
        if (!isNotificationsEnabled(context)) {
            return // Do nothing if notifications are disabled
        }

        if (!canSendNotification(context)) {
            Log.d("Notification", "Notification skipped due to 5-minute interval.")
            return // Skip notification if it's less than 5 minutes since last sent
        }

        val OPTIMAL = mapOf(
            "Air Temperature" to 25.0..30.0,
            "Humidity" to 50.0..70.0,
            "Water Temperature" to 18.0..22.0,
            "pH Level" to 5.5..6.5,
            "TDS" to 560.0..840.0,
            "Water Level" to 30.0..100.0
        )

        // Create a map to store sensor alert messages
        val alerts = mutableMapOf<String, String>()

        if (temperature !in OPTIMAL["Air Temperature"]!!) {
            alerts["Air Temp Alert"] = "Air temp: $temperature°C (Optimal: 18–24°C)"
        }
        if (humidity !in OPTIMAL["Humidity"]!!) {
            alerts["Humidity Alert"] = "Humidity: $humidity% (Optimal: 50–70%)"
        }
        if (waterTemp !in OPTIMAL["Water Temperature"]!!) {
            alerts["Water Temp Alert"] = "Water temp: $waterTemp°C (Optimal: 18–22°C)"
        }
        if (ph !in OPTIMAL["pH Level"]!!) {
            alerts["pH Alert"] = "pH: $ph (Optimal: 5.5–6.5)"
        }
        if (tds !in OPTIMAL["TDS"]!!) {
            alerts["TDS Alert"] = "TDS: $tds ppm (Optimal: 560–840 ppm)"
        }
        if (waterLevel !in OPTIMAL["Water Level"]!!) {
            alerts["Water Level Alert"] = "Water level: $waterLevel% (Optimal: 30–50%)"
        }

        if (alerts.isNotEmpty()) {
            // Send the combined notification if there are alerts
            sendCombinedSensorNotification(context, alerts)
            // Update the last notification time
            updateLastNotificationTime(context)
        }
    }

    fun sendCombinedSensorNotification(context: Context, data: Map<String, String>) {
        if (!isNotificationsEnabled(context)) {
            return // Do nothing if notifications are disabled
        }

        val message = data.entries.joinToString("\n") { "${it.key}: ${it.value}" }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Sensor Update")
                .setContentText("Tap to view details.")
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)

        } catch (e: SecurityException) {
            Log.e("Notification", "SecurityException: Permission to post notifications denied.")
        }
    }

    private fun updateLastNotificationTime(context: Context) {
        val prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(LAST_NOTIFICATION_TIME_KEY, System.currentTimeMillis()).apply()
    }
}
