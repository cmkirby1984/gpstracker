package com.tracker.gps

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.io.IOException
import java.io.InputStream
import java.util.UUID

class BluetoothService : Service() {

    companion object {
        private const val TAG = "BluetoothService"
        const val TARGET_DEVICE_NAME = "ESP32_TRAILER_001"
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val SCAN_INTERVAL_MS = 10000L // 10 seconds
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "gps_tracker_channel"
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var isRunning = false
    private var isConnected = false

    private val deviceFoundReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    device?.let {
                        if (checkBluetoothPermission()) {
                            if (it.name == TARGET_DEVICE_NAME) {
                                Log.d(TAG, "Found target device during discovery!")
                                bluetoothAdapter?.cancelDiscovery()
                                connectToDevice(it)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Register for Bluetooth discovery broadcasts
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(deviceFoundReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")

        // Start as foreground service
        val notification = createNotification("Starting GPS Tracker...")
        startForeground(NOTIFICATION_ID, notification)

        if (!isRunning) {
            isRunning = true
            Thread {
                runServiceLoop()
            }.start()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service stopping")
        isRunning = false
        closeConnection()
        unregisterReceiver(deviceFoundReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun runServiceLoop() {
        Log.d(TAG, "Service loop started")

        while (isRunning) {
            try {
                if (!isConnected) {
                    // Not connected - try to scan and connect
                    scanAndConnect()
                    Thread.sleep(SCAN_INTERVAL_MS)
                } else {
                    // Connected - listen for data
                    listenForData()
                }
            } catch (e: InterruptedException) {
                Log.e(TAG, "Service loop interrupted", e)
                break
            } catch (e: Exception) {
                Log.e(TAG, "Error in service loop", e)
                Thread.sleep(1000)
            }
        }

        Log.d(TAG, "Service loop ended")
    }

    private fun scanAndConnect() {
        if (!checkBluetoothPermission()) {
            updateStatus("Bluetooth permission required")
            return
        }

        val adapter = bluetoothAdapter ?: run {
            updateStatus("Bluetooth not available")
            return
        }

        if (!adapter.isEnabled) {
            updateStatus("Bluetooth is disabled")
            return
        }

        Log.d(TAG, "Scanning for device: $TARGET_DEVICE_NAME")
        updateStatus("Scanning for $TARGET_DEVICE_NAME...")

        // Step 1: Check paired devices first (faster)
        val pairedDevices = adapter.bondedDevices
        for (device in pairedDevices) {
            if (device.name == TARGET_DEVICE_NAME) {
                Log.d(TAG, "Found device in paired list!")
                connectToDevice(device)
                return
            }
        }

        // Step 2: Start discovery to find unpaired devices
        Log.d(TAG, "Device not paired. Starting discovery...")
        updateStatus("Searching for device...")

        if (adapter.isDiscovering) {
            adapter.cancelDiscovery()
        }

        adapter.startDiscovery()
        // Discovery will trigger deviceFoundReceiver when device is found
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (!checkBluetoothPermission()) return

        try {
            Log.d(TAG, "Attempting to connect to: ${device.name}")
            updateStatus("Connecting to ${device.name}...")

            // Cancel discovery to speed up connection
            bluetoothAdapter?.cancelDiscovery()

            // Close any existing connection
            closeConnection()

            // Create socket and connect
            bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            bluetoothSocket?.connect()

            if (bluetoothSocket?.isConnected == true) {
                inputStream = bluetoothSocket?.inputStream
                isConnected = true

                Log.d(TAG, "Successfully connected!")
                updateStatus("Connected to ${device.name}")
                updateNotification("Connected to GPS device")
            } else {
                Log.e(TAG, "Connection failed")
                closeConnection()
                updateStatus("Connection failed")
            }

        } catch (e: IOException) {
            Log.e(TAG, "Connection error: ${e.message}")
            closeConnection()
            updateStatus("Connection failed - retrying...")
        }
    }

    private fun listenForData() {
        val stream = inputStream ?: run {
            isConnected = false
            return
        }

        try {
            if (stream.available() > 0) {
                // Read line from stream
                val message = readLine(stream)

                if (message.isNotEmpty()) {
                    Log.d(TAG, "Received: $message")
                    processGpsMessage(message)
                }
            } else {
                Thread.sleep(100) // Small delay when no data available
            }
        } catch (e: IOException) {
            Log.e(TAG, "Connection lost: ${e.message}")
            updateStatus("Connection lost - reconnecting...")
            closeConnection()
        }
    }

    private fun readLine(stream: InputStream): String {
        val buffer = StringBuilder()
        var attempts = 0
        val maxAttempts = 100 // Prevent infinite loop

        while (attempts < maxAttempts) {
            try {
                if (stream.available() > 0) {
                    val byte = stream.read()
                    if (byte == -1) break // End of stream
                    val char = byte.toChar()

                    if (char == '\n') {
                        break // End of line
                    } else if (char != '\r') {
                        buffer.append(char)
                    }
                } else {
                    Thread.sleep(10)
                    attempts++
                }
            } catch (e: Exception) {
                break
            }
        }

        return buffer.toString().trim()
    }

    private fun processGpsMessage(message: String) {
        // Expected format: SERIAL|LAT|LON|HEADING|TIME|END
        if (!message.contains("END")) {
            Log.e(TAG, "Invalid message format - missing END marker")
            return
        }

        val parts = message.split("|")
        if (parts.size != 6) {
            Log.e(TAG, "Invalid message format - expected 6 parts, got ${parts.size}")
            return
        }

        try {
            val serial = parts[0]
            val latitude = parts[1]
            val longitude = parts[2]
            val heading = parts[3]
            val timestamp = parts[4]

            Log.d(TAG, "GPS Data - Lat: $latitude, Lon: $longitude, Heading: $heading, Time: $timestamp")

            // Validate coordinates
            val lat = latitude.toDoubleOrNull()
            val lon = longitude.toDoubleOrNull()

            if (lat == null || lon == null || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                Log.e(TAG, "Invalid GPS coordinates")
                return
            }

            // Update UI
            updateLocation(latitude, longitude, heading, timestamp)

            // TODO: Send email with location data
            // sendEmailUpdate(serial, latitude, longitude, heading, timestamp)

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing GPS message: ${e.message}")
        }
    }

    private fun closeConnection() {
        try {
            inputStream?.close()
            bluetoothSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing connection: ${e.message}")
        } finally {
            inputStream = null
            bluetoothSocket = null
            isConnected = false
        }
    }

    private fun checkBluetoothPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GPS Tracker Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "GPS Tracker background service"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(message: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GPS Tracker")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(message: String) {
        val notification = createNotification(message)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateStatus(status: String) {
        val intent = Intent(MainActivity.ACTION_UPDATE_STATUS).apply {
            putExtra(MainActivity.EXTRA_STATUS, status)
        }
        sendBroadcast(intent)
    }

    private fun updateLocation(latitude: String, longitude: String, heading: String, timestamp: String) {
        val intent = Intent(MainActivity.ACTION_UPDATE_LOCATION).apply {
            putExtra(MainActivity.EXTRA_LATITUDE, latitude)
            putExtra(MainActivity.EXTRA_LONGITUDE, longitude)
            putExtra(MainActivity.EXTRA_HEADING, heading)
            putExtra(MainActivity.EXTRA_TIMESTAMP, timestamp)
        }
        sendBroadcast(intent)
        updateNotification("Location updated: $latitude, $longitude")
    }
}
