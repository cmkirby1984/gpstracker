# Simple Auto-Connect Android App for GPS Device

## Overview
A simple Android app that continuously scans for a Bluetooth GPS device and automatically connects when detected.

---

## Configuration
```kotlin
// Device to search for
const val TARGET_DEVICE_NAME = "ESP32_TRAILER_001"
const val SCAN_INTERVAL_MS = 10000  // Scan every 10 seconds
const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"
```

---

## Main Application Logic

### 1. App Startup
```
FUNCTION onCreate():
    1. Request Bluetooth permissions
    2. Check if Bluetooth is enabled
       - If not, prompt user to enable
    3. Start AutoConnectService
    4. Display status: "Scanning for GPS device..."
END
```

---

### 2. Auto-Connect Service (Runs in Background)
```
SERVICE AutoConnectService:

    VARIABLE connectionState = DISCONNECTED
    VARIABLE bluetoothSocket = NULL
    VARIABLE targetDevice = NULL

    FUNCTION onStart():
        WHILE service is running:

            // Check if already connected
            IF connectionState == CONNECTED:
                // Listen for GPS data
                LISTEN_FOR_DATA()
                CONTINUE
            END IF

            // Not connected - try to find and connect
            SCAN_AND_CONNECT()

            // Wait before next scan attempt
            SLEEP(SCAN_INTERVAL_MS)

        END WHILE
    END

END SERVICE
```

---

### 3. Scanning for Device
```
FUNCTION SCAN_AND_CONNECT():

    PRINT "Scanning for Bluetooth devices..."
    UPDATE_UI("Scanning...")

    // Step 1: Check paired devices first (faster)
    pairedDevices = Bluetooth.getPairedDevices()

    FOR EACH device IN pairedDevices:
        IF device.name == TARGET_DEVICE_NAME:
            PRINT "Found device in paired list!"
            ATTEMPT_CONNECTION(device)
            RETURN
        END IF
    END FOR

    // Step 2: Device not paired - start discovery
    PRINT "Device not paired. Starting discovery..."
    Bluetooth.startDiscovery()

    // Wait for discovery to complete (typically 12 seconds)
    WAIT_FOR_DISCOVERY_COMPLETE()

    // Step 3: Check discovered devices
    discoveredDevices = Bluetooth.getDiscoveredDevices()

    FOR EACH device IN discoveredDevices:
        IF device.name == TARGET_DEVICE_NAME:
            PRINT "Found device during discovery!"
            ATTEMPT_CONNECTION(device)
            RETURN
        END IF
    END FOR

    PRINT "Device not found. Will retry in " + SCAN_INTERVAL_MS + "ms"
    UPDATE_UI("Device not found - retrying...")

END FUNCTION
```

---

### 4. Connecting to Device
```
FUNCTION ATTEMPT_CONNECTION(device):

    PRINT "Attempting to connect to: " + device.name
    UPDATE_UI("Connecting to " + device.name + "...")

    TRY:
        // Create Bluetooth socket using SPP UUID
        bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)

        // Cancel discovery to speed up connection
        Bluetooth.cancelDiscovery()

        // Attempt connection
        bluetoothSocket.connect()

        // Check if connected
        IF bluetoothSocket.isConnected():
            PRINT "✓ Successfully connected!"
            connectionState = CONNECTED
            UPDATE_UI("Connected to GPS device")
            SHOW_NOTIFICATION("GPS Tracker Connected")
        ELSE:
            PRINT "✗ Connection failed"
            CLOSE_SOCKET()
        END IF

    CATCH IOException as error:
        PRINT "Connection error: " + error.message
        UPDATE_UI("Connection failed - retrying...")
        CLOSE_SOCKET()

    END TRY

END FUNCTION
```

---

### 5. Listening for GPS Data
```
FUNCTION LISTEN_FOR_DATA():

    IF bluetoothSocket == NULL OR NOT bluetoothSocket.isConnected():
        connectionState = DISCONNECTED
        RETURN
    END IF

    TRY:
        inputStream = bluetoothSocket.getInputStream()

        // Read data with timeout
        IF inputStream.available() > 0:

            // Read until newline character
            message = READ_LINE_FROM_STREAM(inputStream)

            PRINT "Received: " + message

            // Process the GPS data
            PROCESS_GPS_MESSAGE(message)

        END IF

    CATCH IOException as error:
        PRINT "Connection lost: " + error.message
        UPDATE_UI("Connection lost - reconnecting...")
        CLOSE_SOCKET()
        connectionState = DISCONNECTED

    END TRY

END FUNCTION
```

---

### 6. Processing GPS Data
```
FUNCTION PROCESS_GPS_MESSAGE(message):

    // Expected format: SERIAL|LAT|LON|HEADING|TIME|END

    IF NOT message.contains("END"):
        PRINT "Invalid message format"
        RETURN
    END IF

    parts = message.split("|")

    IF parts.length != 6:
        PRINT "Invalid message - wrong field count"
        RETURN
    END IF

    serial = parts[0]
    latitude = PARSE_FLOAT(parts[1])
    longitude = PARSE_FLOAT(parts[2])
    heading = PARSE_FLOAT(parts[3])
    timestamp = parts[4]

    PRINT "GPS Location:"
    PRINT "  Lat: " + latitude
    PRINT "  Lon: " + longitude
    PRINT "  Heading: " + heading
    PRINT "  Time: " + timestamp

    // Update UI with location
    UPDATE_UI_LOCATION(latitude, longitude, heading, timestamp)

    // Send email notification
    SEND_EMAIL_UPDATE(serial, latitude, longitude, heading, timestamp)

END FUNCTION
```

---

### 7. Helper Functions
```
FUNCTION CLOSE_SOCKET():
    TRY:
        IF bluetoothSocket != NULL:
            bluetoothSocket.close()
            bluetoothSocket = NULL
        END IF
    CATCH:
        // Ignore errors during close
    END TRY
END FUNCTION


FUNCTION UPDATE_UI(statusMessage):
    // Run on main UI thread
    RUN_ON_UI_THREAD:
        statusTextView.setText(statusMessage)
    END RUN
END FUNCTION


FUNCTION SHOW_NOTIFICATION(message):
    // Show persistent notification
    notification = CREATE_NOTIFICATION(
        title: "GPS Tracker",
        text: message,
        icon: ICON_BLUETOOTH
    )
    DISPLAY_NOTIFICATION(notification)
END FUNCTION
```

---

## Simple UI Layout

```
┌─────────────────────────────────┐
│  GPS Tracker - Auto Connect     │
├─────────────────────────────────┤
│                                 │
│  Status: [Scanning...]          │
│                                 │
│  Device: ESP32_TRAILER_001      │
│                                 │
│  ───────────────────────────    │
│                                 │
│  Last Location:                 │
│    Latitude:  ---.------        │
│    Longitude: ---.------        │
│    Heading:   ---°              │
│    Time:      ----               │
│                                 │
│  ───────────────────────────    │
│                                 │
│  [ ● Bluetooth: ON ]            │
│                                 │
└─────────────────────────────────┘
```

---

## Permissions Required (AndroidManifest.xml)
```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

---

## Key Features

✓ **Automatic Scanning** - Continuously scans every 10 seconds
✓ **Auto-Connect** - Connects immediately when device is found
✓ **Paired Device Priority** - Checks paired devices first (faster)
✓ **Background Service** - Runs even when app is minimized
✓ **Auto-Reconnect** - Reconnects if connection is lost
✓ **Simple & Lightweight** - Minimal code, easy to understand

---

## Flow Diagram

```
START
  ↓
Check Permissions
  ↓
Start Background Service
  ↓
┌─────────────────────────┐
│  SCANNING LOOP          │
│  ↓                      │
│  Check Paired Devices   │
│  ↓                      │
│  Found? ──YES──→ Connect│
│  ↓ NO                   │
│  Start Discovery        │
│  ↓                      │
│  Found? ──YES──→ Connect│
│  ↓ NO                   │
│  Wait 10 seconds        │
│  ↓                      │
│  [Loop Back]            │
└─────────────────────────┘
          │
    CONNECTED
          ↓
    Listen for Data
          ↓
    Process GPS Message
          ↓
    Update UI + Send Email
          ↓
    Connection Lost? → Back to Scanning
```

---

## Notes

- **Keep Screen On**: Consider using wake locks if tablet screen needs to stay on
- **Battery Optimization**: Android may kill the background service - need to disable battery optimization for the app
- **Foreground Service**: Use a foreground service with persistent notification to keep the service running
- **Error Handling**: All Bluetooth operations should be wrapped in try-catch blocks
- **Thread Safety**: Bluetooth operations should run on background thread, UI updates on main thread

---

## Next Steps

1. Implement this logic in Android Studio
2. Test scanning and auto-connection
3. Add GPS data processing
4. Add email sending functionality
5. Test with actual ESP32 device
