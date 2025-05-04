package com.example.lehydrosys

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.appcompat.app.ActionBarDrawerToggle
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.security.cert.CertificateException
import javax.net.ssl.*
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import android.content.SharedPreferences


class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var connectionStatus: TextView
    private var notificationsEnabled = true // Default state for notifications
    private lateinit var sharedPreferences: SharedPreferences

    private val client = getUnsafeOkHttpClient()
    private val serverUrl = "https://lehydrosys-sqfy.onrender.com/api/relay" // Replace with your web server URL

    private fun getUnsafeOkHttpClient(): OkHttpClient {
        try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                }
            )

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            return OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private suspend fun isConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return false
        }

        // Perform a real connectivity check to your server
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://lehydrosys-sqfy.onrender.com") // Replace with your server URL
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 2000 // 2 seconds timeout
                connection.connect()
                connection.responseCode == 200
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun updateConnectionStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            val isConnected = isConnected() // Perform the network check on a background thread
            withContext(Dispatchers.Main) {
                // Update the UI on the main thread
                if (isConnected) {
                    connectionStatus.text = "Online"
                    connectionStatus.setTextColor(getColor(R.color.green))
                    connectionStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.online_circle, 0)
                } else {
                    connectionStatus.text = "Offline"
                    connectionStatus.setTextColor(getColor(R.color.red))
                    connectionStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.offline_circle, 0)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectionStatus = findViewById(R.id.connection_status)
        toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        viewPager = findViewById(R.id.view_pager)
        tabLayout = findViewById(R.id.tab_layout)

        setSupportActionBar(toolbar)
        updateConnectionStatus()

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("LeHydroSysPrefs", Context.MODE_PRIVATE)
        notificationsEnabled = sharedPreferences.getBoolean("notificationsEnabled", true) // Load saved state
        updateNotificationIcon()

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.open_drawer, R.string.close_drawer
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val headerView = navView.getHeaderView(0)
        if (headerView != null) {
            val switchRelay1 = headerView.findViewById<SwitchCompat>(R.id.switch_relay1)
            val switchRelay2 = headerView.findViewById<SwitchCompat>(R.id.switch_relay2)

            switchRelay1?.setOnCheckedChangeListener { _, isChecked ->
                val command = if (isChecked) "relay1_on" else "relay1_off"
                sendRelayCommand(command)
            }

            switchRelay2?.setOnCheckedChangeListener { _, isChecked ->
                val command = if (isChecked) "relay2_on" else "relay2_off"
                sendRelayCommand(command)
            }
        } else {
            Log.e("MainActivity", "Header view is null!")
        }

        navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_about -> {
                    showDialog("About Us", "LeHydroSys is a smart hydroponics monitoring and automation system designed by J.A.S.")
                }
                R.id.nav_contact -> {
                    showDialog("Contact Us", "Email: support@lehydrosys.com\nPhone: +63 912 345 6789")
                }
            }
            drawerLayout.closeDrawers()
            true
        }

        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Real-Time Data"
                1 -> "Graphs"
                else -> "Unknown"
            }
        }.attach()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notifications -> {
                Toast.makeText(this, "Notifications clicked!", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun sendRelayCommand(command: String) {
        val json = JSONObject()
        json.put("command", command)

        val requestBody = RequestBody.create("application/json".toMediaType(), json.toString())

        val request = Request.Builder()
            .url(serverUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to send command: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "Command sent successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to send command: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
        private fun toggleNotifications() {
        notificationsEnabled = !notificationsEnabled // Toggle the state

        // Save the state in SharedPreferences
        sharedPreferences.edit().putBoolean("notificationsEnabled", notificationsEnabled).apply()

        // Update the notification icon
        updateNotificationIcon()

        // Show a Toast message
        val message = if (notificationsEnabled) {
            "Notifications enabled"
        } else {
            "Notifications disabled"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

        private fun updateNotificationIcon() {
        // Update the notification icon based on the state
        val menuItem = toolbar.menu.findItem(R.id.action_notifications)
        if (notificationsEnabled) {
            menuItem.setIcon(R.drawable.ic_notification_on) // Replace with your "enabled" icon
        } else {
            menuItem.setIcon(R.drawable.ic_notiifications_off) // Replace with your "disabled" icon
        }
    }
}