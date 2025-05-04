    package com.example.lehydrosys

    import android.content.Context
    import android.os.Handler
    import android.os.Looper
    import android.util.Log
    import org.eclipse.paho.android.service.MqttAndroidClient
    import org.eclipse.paho.client.mqttv3.*
    import java.io.InputStream
    import java.security.KeyStore
    import java.security.cert.CertificateFactory
    import javax.net.ssl.SSLContext
    import javax.net.ssl.TrustManagerFactory

    class MqttClient {

        private lateinit var mqttClient: MqttAndroidClient
        private val mqttBroker = "ssl://c2801802.ala.us-east-1.emqxsl.com:8883"
        private val mqttTopic = "iot/sensors"
        private val clientId = "AndroidApp-${System.currentTimeMillis()}"
        private val pingHandler = Handler(Looper.getMainLooper())
        private var keepAliveInterval: Long = 60_000 // Default to 60 seconds

        fun connectMQTT(context: Context) {
            mqttClient = MqttAndroidClient(context, mqttBroker, clientId)

            // Ensure AlarmPingSender is not being used
            val mqttOptions = MqttConnectOptions().apply {
                isAutomaticReconnect = true
                isCleanSession = true

                // SSL Certificate Setup
                try {
                    val caInputStream: InputStream = context.resources.openRawResource(R.raw.emqxsl_ca)
                    val certificateFactory = CertificateFactory.getInstance("X.509")
                    val ca = certificateFactory.generateCertificate(caInputStream)

                    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
                    keyStore.load(null, null)
                    keyStore.setCertificateEntry("ca", ca)

                    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                    trustManagerFactory.init(keyStore)

                    val sslContext = SSLContext.getInstance("TLS")
                    sslContext.init(null, trustManagerFactory.trustManagers, null)

                    socketFactory = sslContext.socketFactory
                } catch (e: Exception) {
                    Log.e("MqttClient", "SSL certificate setup error", e)
                }
            }

            // Connect using the custom ping sender
            try {
                mqttClient.connect(mqttOptions, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d("MqttClient", "Connected to MQTT broker")
                        subscribeToTopic()
                        startPingTask()  // Start custom ping task
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e("MqttClient", "Failed to connect to MQTT broker", exception)
                    }
                })
            } catch (e: MqttException) {
                Log.e("MqttClient", "MQTT connection error", e)
            }
        }

        private fun subscribeToTopic() {
            try {
                val qos = 1 // Quality of Service (QoS) level for subscription
                mqttClient.subscribe(mqttTopic, qos, object : IMqttMessageListener {
                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        message?.let {
                            val payload = String(it.payload)
                            Log.d("MqttClient", "Received message: $payload")
                        }
                    }
                })
                Log.d("MqttClient", "Successfully subscribed to topic: $mqttTopic")
            } catch (e: MqttException) {
                Log.e("MqttClient", "Subscription error", e)
            }
        }

        // Custom ping task that checks connectivity every keepAliveInterval milliseconds
        private fun startPingTask() {
            // Use a handler to simulate the ping check
            pingHandler.postDelayed(object : Runnable {
                override fun run() {
                    checkConnectivity()
                    pingHandler.postDelayed(this, keepAliveInterval) // Repeat the check task
                }
            }, keepAliveInterval)
        }

        // Manually checking the connection status and reconnecting if disconnected
        private fun checkConnectivity() {
            try {
                if (!mqttClient.isConnected) {
                    Log.d("MqttClient", "Client is disconnected, attempting to reconnect.")
                    reconnectMQTT() // Reconnect if disconnected
                }
            } catch (e: MqttException) {
                Log.e("MqttClient", "Error checking connection status", e)
            }
        }

        // Attempt to reconnect if the client gets disconnected
        private fun reconnectMQTT() {
            try {
                val options = MqttConnectOptions().apply {
                    isAutomaticReconnect = true
                    isCleanSession = true
                }
                mqttClient.connect(options, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d("MqttClient", "Reconnected to MQTT broker")
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e("MqttClient", "Reconnect failed", exception)
                    }
                })
            } catch (e: MqttException) {
                Log.e("MqttClient", "Error during reconnection", e)
            }
        }

        // Optional: Stop the ping task when disconnecting
        fun stopPingTask() {
            pingHandler.removeCallbacksAndMessages(null)
            Log.d("MqttClient", "Ping task stopped")
        }

        fun publishMessage(message: String, qos: Int = 1) {
            val mqttMessage = MqttMessage(message.toByteArray())
            mqttMessage.qos = qos
            try {
                mqttClient.publish(mqttTopic, mqttMessage)
                Log.d("MqttClient", "Message published: $message")
            } catch (e: MqttException) {
                Log.e("MqttClient", "Failed to publish message", e)
            }
        }

        fun disconnectMQTT() {
            try {
                mqttClient.disconnect(null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d("MqttClient", "Disconnected from MQTT broker")
                        stopPingTask() // Stop ping task on disconnect
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e("MqttClient", "Failed to disconnect from MQTT broker", exception)
                    }
                })
            } catch (e: MqttException) {
                Log.e("MqttClient", "Error during MQTT disconnection", e)
            }
        }
    }
