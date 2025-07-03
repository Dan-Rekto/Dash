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
class MainActivity : AppCompatActivity() {
    private lateinit var switchAuto: Switch
    private lateinit var switchSiram: Switch
    private val IOT_DEVICE_IP = "192.168.1.112"
    private val IOT_DEVICE_PORT = 8888
    private var penentu = true
    val satu = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        switchAuto = findViewById(R.id.switchAuto)
        switchSiram = findViewById(R.id.switchSiram)

        switchAuto.setOnCheckedChangeListener { _, isChecked ->
            val cmd = if (isChecked) "1" else "0"
            sendCommand(cmd)
            Toast.makeText(this,
                if (isChecked) "Penyiram Otomatis Akan Menyala Jam 10 dan Jam 16!" else "Penyiram Otomatis Dimatikan",
                Toast.LENGTH_SHORT).show()
        }

        switchSiram.setOnCheckedChangeListener { _, isChecked ->
            val cmd = if (isChecked) "A" else "B"
            sendCommand(cmd)
            Toast.makeText(this,
                if (isChecked) "Penyiram Menyala!" else "Penyiram Mati!",
                Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendCommand(command: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("TcpClient", "Sending command: $command")
                Socket(IOT_DEVICE_IP, IOT_DEVICE_PORT).use { socket ->
                    socket.outputStream.write(command.toByteArray())
                }
                Log.d("TcpClient", "Command sent successfully.")
            } catch (e: Exception) {
                Log.e("TcpClient", "Error: ${e.message}")
            }
        }
    }

    private fun kirim() {
        lifecycleScope.launch(Dispatchers.IO) {
            while (penentu) {
                val cmdAuto = if (switchAuto.isChecked) "1" else "0"
                val cmdSiram = if (switchSiram.isChecked) "A" else "B"

                sendCommand(cmdSiram)
                Thread.sleep(2000) // Di background thread, boleh
            }
        }
    }

    private fun kirim1() {
        lifecycleScope.launch(Dispatchers.IO) {
            while (penentu) {
                val cmdAuto = if (switchAuto.isChecked) "1" else "0"
                val cmdSiram = if (switchSiram.isChecked) "A" else "B"

                sendCommand(cmdAuto)
                Thread.sleep(2000) // Di background thread, boleh
            }
        }
    }

    override fun onResume() {
        super.onResume()
        penentu = true
        kirim()
    }

}
