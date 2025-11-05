package com.tracker.gps

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var deviceNameTextView: TextView
    private lateinit var latitudeTextView: TextView
    private lateinit var longitudeTextView: TextView
    private lateinit var headingTextView: TextView
    private lateinit var timestampTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
        const val ACTION_UPDATE_STATUS = "com.tracker.gps.UPDATE_STATUS"
        const val ACTION_UPDATE_LOCATION = "com.tracker.gps.UPDATE_LOCATION"
        const val EXTRA_STATUS = "status"
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        const val EXTRA_HEADING = "heading"
        const val EXTRA_TIMESTAMP = "timestamp"
    }

    private val statusUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_UPDATE_STATUS -> {
                    val status = intent.getStringExtra(EXTRA_STATUS) ?: "Unknown"
                    statusTextView.text = status
                }
                ACTION_UPDATE_LOCATION -> {
                    val lat = intent.getStringExtra(EXTRA_LATITUDE) ?: "--"
                    val lon = intent.getStringExtra(EXTRA_LONGITUDE) ?: "--"
                    val heading = intent.getStringExtra(EXTRA_HEADING) ?: "--"
                    val timestamp = intent.getStringExtra(EXTRA_TIMESTAMP) ?: "--"

                    latitudeTextView.text = "Latitude: $lat"
                    longitudeTextView.text = "Longitude: $lon"
                    headingTextView.text = "Heading: $heading°"
                    timestampTextView.text = "Time: $timestamp"
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        statusTextView = findViewById(R.id.statusTextView)
        deviceNameTextView = findViewById(R.id.deviceNameTextView)
        latitudeTextView = findViewById(R.id.latitudeTextView)
        longitudeTextView = findViewById(R.id.longitudeTextView)
        headingTextView = findViewById(R.id.headingTextView)
        timestampTextView = findViewById(R.id.timestampTextView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)

        deviceNameTextView.text = "Device: ${BluetoothService.TARGET_DEVICE_NAME}"

        // Set up button listeners
        startButton.setOnClickListener {
            if (checkPermissions()) {
                startService()
            }
        }

        stopButton.setOnClickListener {
            stopService()
        }

        // Check Bluetooth availability
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Request permissions
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        // Register broadcast receiver
        val filter = IntentFilter().apply {
            addAction(ACTION_UPDATE_STATUS)
            addAction(ACTION_UPDATE_LOCATION)
        }
        registerReceiver(statusUpdateReceiver, filter, RECEIVER_NOT_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        // Unregister broadcast receiver
        unregisterReceiver(statusUpdateReceiver)
    }

    private fun checkPermissions(): Boolean {
        val permissions = mutableListOf<String>()

        // Bluetooth permissions (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        // Location permission (required for Bluetooth scanning)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        return if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                Toast.makeText(this, "Permissions granted. Tap Start to begin.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions required for Bluetooth functionality", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startService() {
        // Check if Bluetooth is enabled
        if (bluetoothAdapter?.isEnabled == false) {
            Toast.makeText(this, "Please enable Bluetooth first", Toast.LENGTH_LONG).show()
            return
        }

        val serviceIntent = Intent(this, BluetoothService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        statusTextView.text = "Service starting..."
        Toast.makeText(this, "GPS Tracker started", Toast.LENGTH_SHORT).show()
    }

    private fun stopService() {
        val serviceIntent = Intent(this, BluetoothService::class.java)
        stopService(serviceIntent)

        statusTextView.text = "Service stopped"
        Toast.makeText(this, "GPS Tracker stopped", Toast.LENGTH_SHORT).show()
    }
}
