package com.example.lehydrosys

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

class DisplayFragment : Fragment() {

    private lateinit var txtAirTemp: TextView
    private lateinit var txtHumidity: TextView
    private lateinit var txtWaterTemp: TextView
    private lateinit var txtWaterLevel: TextView
    private lateinit var txtPh: TextView
    private lateinit var txtTds: TextView

    private var lastUpdateTime: Long = 0
    private val timeoutThreshold = 30000L // 30 seconds
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 5000L // 5 seconds

    private lateinit var client: OkHttpClient

    // Replace ThingSpeak URL with your web server's API endpoint
    private val serverUrl = "https://lehydrosys-sqfy.onrender.com/api/data/latest"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_display, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        client = getSecureOkHttpClient()

        txtAirTemp = view.findViewById(R.id.txt_air_temp)
        txtHumidity = view.findViewById(R.id.txt_humidity)
        txtWaterTemp = view.findViewById(R.id.txt_water_temp)
        txtWaterLevel = view.findViewById(R.id.txt_water_level)
        txtPh = view.findViewById(R.id.txt_ph)
        txtTds = view.findViewById(R.id.txt_tds)

        applyThemeColors()

        startDataFetching()
        startConnectionMonitor()
    }

private fun applyThemeColors() {
    val textColor = resources.getColor(R.color.colorOnBackgroundDark, null)

    txtAirTemp.setTextColor(textColor)
    txtHumidity.setTextColor(textColor)
    txtWaterTemp.setTextColor(textColor)
    txtWaterLevel.setTextColor(textColor)
    txtPh.setTextColor(textColor)
    txtTds.setTextColor(textColor)
}

    private fun startDataFetching() {
        handler.post(object : Runnable {
            override fun run() {
                fetchDataFromServer()
                handler.postDelayed(this, updateInterval)
            }
        })
    }

    private fun fetchDataFromServer() {
        val request = Request.Builder()
            .url(serverUrl)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast("Failed to fetch data: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { responseBody ->
                    updateUI(responseBody)
                }
            }
        })
    }

    private fun startConnectionMonitor() {
        Thread {
            while (true) {
                val currentTime = System.currentTimeMillis()
                val timeSinceLast = currentTime - lastUpdateTime
                if (lastUpdateTime != 0L && timeSinceLast > timeoutThreshold) {
                    activity?.runOnUiThread {
                        showToast("No data received from server")
                    }
                    lastUpdateTime = 0
                }
                Thread.sleep(10000)
            }
        }.start()
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(payload: String) {
        lastUpdateTime = System.currentTimeMillis()

        activity?.runOnUiThread {
            try {
                val jsonData = JSONObject(payload)

                // Assuming the API returns a JSON object with these fields
                val airTemp = jsonData.optDouble("temperature", 0.0)
                val humidity = jsonData.optDouble("humidity", 0.0)
                val waterTemp = jsonData.optDouble("waterTemp", 0.0)
                val waterLevel = jsonData.optDouble("distance", 0.0)
                val ph = jsonData.optDouble("ph", 0.0)
                val tds = jsonData.optDouble("tds", 0.0)

                txtAirTemp.text = getString(R.string.air_temperature, airTemp)
                txtHumidity.text = getString(R.string.humidity_d, humidity)
                txtWaterTemp.text = getString(R.string.water_temperature, waterTemp)
                txtWaterLevel.text = getString(R.string.water_level, waterLevel)
                txtPh.text = getString(R.string.ph_level_d, ph)
                txtTds.text = getString(R.string.tds, tds)

            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Error parsing server data")
            }
        }
    }

    private fun getSecureOkHttpClient(): OkHttpClient {
        val trustAllCertificates = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(trustAllCertificates), SecureRandom())

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCertificates)
            .hostnameVerifier { _, _ -> true }
            .build()
    }
}