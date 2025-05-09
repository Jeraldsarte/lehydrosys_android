package com.example.lehydrosys

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.lehydrosys.connectivity.AndroidConnectivityObserver
import com.example.lehydrosys.connectivity.ConnectivityViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import androidx.appcompat.app.ActionBarDrawerToggle

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var connectionStatus: TextView
    private var notificationsEnabled = true
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var connectivityViewModel: ConnectivityViewModel

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        connectionStatus = findViewById(R.id.connection_status)
        toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        viewPager = findViewById(R.id.view_pager)
        tabLayout = findViewById(R.id.tab_layout)

        setSupportActionBar(toolbar)
        askNotificationPermission()

        // Load preferences
        sharedPreferences = getSharedPreferences("LeHydroSysPrefs", Context.MODE_PRIVATE)
        notificationsEnabled = sharedPreferences.getBoolean("notificationsEnabled", true)
        updateNotificationIcon()

        // Setup navigation drawer
        setupDrawer()

        // Setup ViewPager and TabLayout
        setupViewPagerAndTabs()

        // Observe network connectivity using the ConnectivityObserver
        val connectivityObserver = AndroidConnectivityObserver(applicationContext)
        connectivityViewModel = ConnectivityViewModel(connectivityObserver)

        lifecycleScope.launch {
            connectivityViewModel.isConnected.collect { isConnected ->
                updateUI(isConnected)
            }
        }
    }

    private fun updateUI(isConnected: Boolean) {
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

    private fun setupDrawer() {
        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_drawer, R.string.close_drawer)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_about -> showDialog("About Us", "LeHydroSys is a smart hydroponics monitoring and automation system designed by J.A.S.")
                R.id.nav_contact -> showDialog("Contact Us", "Email: support@lehydrosys.com\nPhone: +63 912 345 6789")
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun setupViewPagerAndTabs() {
        viewPager.adapter = ViewPagerAdapter(this)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Real-Time Data"
                1 -> "Graphs"
                else -> "Unknown"
            }
        }.attach()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun toggleNotifications() {
        notificationsEnabled = !notificationsEnabled
        sharedPreferences.edit().putBoolean("notificationsEnabled", notificationsEnabled).apply()
        updateNotificationIcon()

        val message = if (notificationsEnabled) "Notifications enabled" else "Notifications disabled"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateNotificationIcon() {
        val menuItem = toolbar.menu.findItem(R.id.action_notifications)
        if (notificationsEnabled) {
            menuItem.setIcon(R.drawable.ic_notification_on)
        } else {
            menuItem.setIcon(R.drawable.ic_notifications_off)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notifications -> {
                toggleNotifications()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
