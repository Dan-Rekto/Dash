package com.example.dash

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.android.service.MqttService
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import kotlin.math.log


class MainActivity : AppCompatActivity() {
    private lateinit var switchAuto: Switch
    private lateinit var switchSiram: Switch
    private var penentu = true
    private lateinit var mqttClient: MqttAndroidClient
    private val serverUri = "ssl://b11b4954395c46ceae511d12f7916b17.s1.eu.hivemq.cloud:8883"
    private val clientId  = MqttClient.generateClientId()
    private val topicSubscribe = "Ddash/esp"
    private val topicPublish   = "Ddash/apk"
    val mqttPass = "Smamda123"
    var tralali:Boolean = false
    companion object {
        private const val PREFS_NAME = "DashPrefs"
        private const val KEY_AUTO_SWITCH = "auto_switch_state"
        private const val KEY_SIRAM_SWITCH = "siram_switch_state"
        private const val KEY_WIB = "wib_value"
    }
    private lateinit var statusespoff: ImageView
    private val handler = Handler(Looper.getMainLooper())
    private var lastOnTime = 0L
    var i:Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        switchAuto = findViewById(R.id.switchAuto)
        switchSiram = findViewById(R.id.switchSiram)
        val statusIv = findViewById<ImageView>(R.id.status)
        val statusIv2 = findViewById<ImageView>(R.id.status2)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        switchAuto.isChecked = prefs.getBoolean(KEY_AUTO_SWITCH, false)
        switchSiram.isChecked = prefs.getBoolean(KEY_SIRAM_SWITCH, false)

        // Restore wib TextView value
        val wib = findViewById<TextView>(R.id.tvLastTime)
        wib.text = prefs.getString(KEY_WIB, "")

        switchAuto.setOnCheckedChangeListener { _, isChecked ->
            val cmd = if (isChecked) "1" else "0"
            auto()
            savePreferences()
            Toast.makeText(this,
                if (isChecked) "Penyiram Otomatis Akan Menyala Jam 10 dan Jam 16!" else "Penyiram Otomatis Dimatikan",
                Toast.LENGTH_SHORT).show()
        }

        switchSiram.setOnCheckedChangeListener { _, isChecked ->
            val cmd = if (isChecked) "A" else "B"
            publishMessage(cmd)
            savePreferences()
            Toast.makeText(this,
                if (isChecked) "Penyiram Menyala!" else "Penyiram Mati!",
                Toast.LENGTH_SHORT).show()
        }

        val REQUEST_CODE_MQTT = 42  // any unique integer

        val pendingFlags = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
            else -> FLAG_UPDATE_CURRENT
        }

// 3) Build the PendingIntent with an actual Intent & proper requestCode
        val notificationIntent = Intent(this, MainActivity::class.java)
// (add extras to notificationIntent if you like)

        val pendingIntent = PendingIntent.getActivity(
            this,
            REQUEST_CODE_MQTT,
            notificationIntent,
            pendingFlags
        )
        // 1) Inisialisasi MQTT client
        mqttClient = MqttAndroidClient(this, serverUri, clientId)

        // 2) Setup callback untuk inbound messages & koneksi loss
        mqttClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                Log.w("MQTT", "Connection lost: ${cause?.message}")
            }
            override fun messageArrived(topic: String, message: MqttMessage) {
                val payload = message.toString()
                Log.d("MQTT", "Received on $topic → $payload")
                runOnUiThread {
                    Toast.makeText(this@MainActivity,
                        "Msg from $topic:\n$payload",
                        Toast.LENGTH_SHORT).show()
                }
            }
            override fun deliveryComplete(token: IMqttDeliveryToken?) { /*no-op*/ }
        })

        // 3) Connect options (clean session)
        val options = MqttConnectOptions().apply {
            isCleanSession = true
            userName = "Ddash"       // your username
            password = mqttPass.toCharArray()  // your password
        }

        if (mqttClient.isConnected){
            runOnUiThread { statusIv2.setVisibility(INVISIBLE) }
        }else{
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT", "Connected to $serverUri")
                    subscribeToTopic()
                }
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MQTT", "Failed to connect: ${exception?.message}")
                }
            })
            runOnUiThread { statusIv2.setVisibility(VISIBLE) }
        }

        // 4) Hubungkan

    }

    private fun savePreferences() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean(KEY_AUTO_SWITCH, switchAuto.isChecked)
        editor.putBoolean(KEY_SIRAM_SWITCH, switchSiram.isChecked)
        val wib = findViewById<TextView>(R.id.tvLastTime)
        editor.putString(KEY_WIB, wib.text.toString())
        editor.apply()
    }

    private fun kirim() {
        lifecycleScope.launch(Dispatchers.IO) {
            while (penentu) {
                val cmdAuto = if (switchAuto.isChecked) "1" else "0"
                val cmdSiram = if (switchSiram.isChecked) "A" else "B"
                publishMessage(cmdSiram)
                Thread.sleep(2000) // Di background thread, boleh
            }
        }
    }

    private fun auto() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cmdAuto = if (switchAuto.isChecked) "1" else "0"
                publishMessage(cmdAuto)
                Log.d("Auto", "Auto mode message sent: $cmdAuto")
            } catch (e: Exception) {
                Log.e("Auto", "Error sending auto mode message: ${e.message}")
            }
        }
    }
    private fun kirim1() {
        lifecycleScope.launch(Dispatchers.IO) {
            while (penentu) {
                val cmdAuto = if (switchAuto.isChecked) "1" else "0"
                val cmdSiram = if (switchSiram.isChecked) "A" else "B"
                Thread.sleep(2000) // Di background thread, boleh
            }
        }
    }

    override fun onResume() {
        super.onResume()
        penentu = true
        kirim()
        receiveMessages()

    }


    private fun subscribeToTopic() {
        mqttClient.subscribe(topicSubscribe, 0, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT", "Subscribed to $topicSubscribe")
            }
            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT", "Subscribe failed: ${exception?.message}")
            }
        })
    }

    private fun publishMessage(payload: String) {
        val options = MqttConnectOptions().apply {
            isCleanSession = true
            userName = "Ddash"       // your username
            password = mqttPass.toCharArray()  // your password
        }
        val statusIv = findViewById<ImageView>(R.id.status)
        val statusIv2 = findViewById<ImageView>(R.id.status2)
        // Always check isConnected first
        if (!mqttClient.isConnected){
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTT", "Connected to $serverUri")
                    subscribeToTopic()
                }
                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MQTT", "Failed to connect: ${exception?.message}")
                }
            })
            runOnUiThread { statusIv2.setVisibility(VISIBLE) }
            Log.w("MQTT","Client not connected, dropping publish")
            return
        }
        runOnUiThread { statusIv2.setVisibility(INVISIBLE) }
        // Publish with listener so you get success/failure callbacks
        val msg = MqttMessage(payload.toByteArray()).apply {
            qos = 0
            isRetained = false
        }
        mqttClient.publish(topicPublish, msg, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT","Published $payload ✔")
                runOnUiThread { statusIv2.setVisibility(INVISIBLE) }
            }
            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT","Publish failed: ${exception?.message}")
                runOnUiThread { statusIv.setVisibility(VISIBLE) }
            }
        })
    }
    private fun receiveMessages() {
        val wib = findViewById<TextView>(R.id.tvLastTime)
        val statusespon = findViewById<ImageView>(R.id.status3)
        val statusespoff = findViewById<ImageView>(R.id.status4)
        var ms = 0
        val delay = 10000
        lastOnTime = System.currentTimeMillis()
        mqttClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable) {
                subscribeToTopic()
            }
            override fun messageArrived(topic: String, message: MqttMessage) {
                val payload = message.toString()
                // Cek apakah payload bukan "ON" dan cocok format timestamp YYYY-MM-DD HH:MM:SS
                if (payload != "ON" && payload.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"))) {
                    runOnUiThread {
                        wib.text = payload
                        savePreferences()
                    }
                }else  if (payload == "ON") {
                    // Catat waktu terima ON
                    lastOnTime = System.currentTimeMillis()
                    // Hide segera
                    runOnUiThread {
                        statusespoff.visibility = View.INVISIBLE
                    }
                    // Schedule pengecekan 10 detik kemudian
                    handler.postDelayed({
                        // Hanya tampilkan kalau memang sudah >10 detik sejak terakhir ON
                        if (System.currentTimeMillis() - lastOnTime >= 10_000) {
                            statusespoff.visibility = View.VISIBLE
                        }
                    }, 10_000)
                }
            }
            override fun deliveryComplete(token: IMqttDeliveryToken) {
                // Acknowledgement on delivery complete
            }
        })
    }
    override fun onDestroy() {
        savePreferences()
        super.onDestroy()
    }

}
