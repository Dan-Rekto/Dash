package com.example.dash

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Switch
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.Socket
import com.example.dash.R


class MainActivity : AppCompatActivity() {

    // --- CHANGE THIS to the IP address from your ESP8266/ESP32 ---
    private val IOT_DEVICE_IP = "192.168.1.112"
    // -----------------------------------------------------------
    private val IOT_DEVICE_PORT = 8888

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make sure you have a layout file named 'main.xml' in your res/layout folder
        setContentView(R.layout.main)

        // It's better to use SwitchCompat from the Material library
        val switchAuto: Switch = findViewById(R.id.switchAuto)
        val switchSiram: Switch = findViewById(R.id.switchSiram)

        // For a Switch, it's best to use setOnCheckedChangeListener
        // This code block will execute whenever the switch is toggled ON or OFF
        switchAuto.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // When the switch is toggled ON
                sendCommand("1")
                Toast.makeText(this, "Penyiram Otomatis Akan Menyala Jam 10 dan Jam 16!", Toast.LENGTH_LONG).show()
            } else {
                // When the switch is toggled OFF
                sendCommand("0")
                Toast.makeText(this, "Penyiram Otomatis Dimatikan", Toast.LENGTH_SHORT).show()
            }
        }

        // You can do the same for your other switch
        switchSiram.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Send a different command for this switch if you want, e.g., "A"
                sendCommand("A")
                Toast.makeText(this, "Penyiram Menyala!", Toast.LENGTH_SHORT).show()
            } else {
                // e.g., "B"
                sendCommand("B")
                Toast.makeText(this, "Penyiram Mati!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendCommand(command: String) {
        // Use Kotlin Coroutines to run network code on a background thread
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("TcpClient", "Sending command: $command")
                val socket = Socket(IOT_DEVICE_IP, IOT_DEVICE_PORT)
                socket.outputStream.write(command.toByteArray())
                socket.close()
                Log.d("TcpClient", "Command sent successfully.")
            } catch (e: Exception) {
                // Handle errors
                Log.e("TcpClient", "Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}