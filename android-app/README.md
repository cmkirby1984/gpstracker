# GPS Tracker Android App

A simple Android application that automatically scans for and connects to a Bluetooth GPS device (ESP32), receives GPS location data, and can send email updates.

## Features

✓ **Auto-Scan** - Continuously scans for target Bluetooth device
✓ **Auto-Connect** - Automatically connects when device is found
✓ **Auto-Reconnect** - Reconnects if connection is lost
✓ **Background Service** - Runs in background even when app is minimized
✓ **Real-time Updates** - Displays GPS coordinates in real-time
✓ **Simple UI** - Clean, easy-to-use interface

---

## Requirements

- **Android Version**: Android 8.0 (API 26) or higher
- **Android Studio**: Arctic Fox or newer
- **Kotlin**: 1.8 or higher
- **Target Device**: ESP32 with GPS module broadcasting as "ESP32_TRAILER_001"

---

## Project Structure

```
android-app/
├── app/
│   ├── build.gradle                  # App dependencies
│   ├── src/
│   │   └── main/
│   │       ├── AndroidManifest.xml   # Permissions and components
│   │       ├── java/com/tracker/gps/
│   │       │   ├── MainActivity.kt   # UI and permissions
│   │       │   └── BluetoothService.kt  # Auto-connect logic
│   │       └── res/
│   │           ├── layout/
│   │           │   └── activity_main.xml  # UI layout
│   │           └── values/
│   │               └── strings.xml
├── build.gradle                      # Project-level build config
└── README.md                         # This file
```

---

## Setup Instructions

### 1. Open in Android Studio

1. Launch **Android Studio**
2. Select **File → Open**
3. Navigate to the `android-app` folder
4. Click **OK**
5. Wait for Gradle sync to complete

### 2. Configure Device Name (Optional)

If your ESP32 device has a different name, update it in `BluetoothService.kt`:

```kotlin
const val TARGET_DEVICE_NAME = "ESP32_TRAILER_001"  // Change this to your device name
```

### 3. Build the App

**Option A: Using Android Studio**
- Click **Build → Make Project** (or press `Ctrl+F9` / `Cmd+F9`)

**Option B: Using Gradle Command Line**
```bash
cd android-app
./gradlew build
```

### 4. Install on Device

**Via Android Studio:**
1. Connect your Android tablet via USB
2. Enable **Developer Options** and **USB Debugging** on the tablet
3. Click the **Run** button (green triangle) in Android Studio
4. Select your device from the list

**Via ADB Command Line:**
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## How to Use

### First Time Setup

1. **Enable Bluetooth** on your Android tablet
2. **Grant Permissions** when prompted:
   - Bluetooth
   - Location (required for Bluetooth scanning on Android 12+)
   - Notifications

3. **(Optional) Pair Device First:**
   - Go to tablet's Bluetooth settings
   - Scan for devices
   - Pair with "ESP32_TRAILER_001" (PIN: 1234 if prompted)
   - This makes initial connection faster, but not required

### Running the App

1. Open the **GPS Tracker** app
2. Tap the **Start** button
3. The app will begin scanning for the GPS device
4. When found, it will **automatically connect**
5. GPS location updates will appear on screen

**Status Messages:**
- "Scanning for ESP32_TRAILER_001..." - Searching for device
- "Connecting to..." - Connection in progress
- "Connected to GPS device" - Successfully connected
- "Connection lost - reconnecting..." - Attempting to reconnect

### Stopping the Service

- Tap the **Stop** button to stop scanning and disconnect

---

## Permissions Explained

The app requires the following permissions:

| Permission | Purpose |
|------------|---------|
| `BLUETOOTH` | Basic Bluetooth operations |
| `BLUETOOTH_ADMIN` | Enable/disable Bluetooth, start discovery |
| `BLUETOOTH_SCAN` | Scan for Bluetooth devices (Android 12+) |
| `BLUETOOTH_CONNECT` | Connect to Bluetooth devices (Android 12+) |
| `ACCESS_FINE_LOCATION` | Required for Bluetooth scanning on Android 12+ |
| `INTERNET` | For sending email updates (future feature) |
| `FOREGROUND_SERVICE` | Run background service with notification |
| `POST_NOTIFICATIONS` | Show notifications (Android 13+) |

---

## How It Works

### Auto-Connect Logic

```
1. Service starts → Begin scanning loop
2. Check paired devices first (faster)
   └─ Found? → Connect immediately
3. If not paired → Start Bluetooth discovery
4. When device found → Auto-connect
5. Once connected → Listen for GPS data
6. Process GPS messages and update UI
7. If disconnected → Return to step 1
```

### GPS Message Format

The app expects messages in this format from the ESP32:

```
SERIAL|LATITUDE|LONGITUDE|HEADING|TIMESTAMP|END\n
```

Example:
```
TRAILER_001|49.123456|-123.456789|275.50|2025-11-05T14:30:00|END
```

---

## Code Overview

### MainActivity.kt

- Handles UI and user interactions
- Requests runtime permissions
- Starts/stops the Bluetooth service
- Receives and displays location updates via BroadcastReceiver

**Key Methods:**
- `checkPermissions()` - Request all required permissions
- `startService()` - Start the background Bluetooth service
- `stopService()` - Stop the service
- `statusUpdateReceiver` - Receive updates from service

### BluetoothService.kt

- Background service that runs continuously
- Auto-scans for target Bluetooth device
- Auto-connects when device is found
- Listens for GPS data and parses messages
- Sends updates to MainActivity via broadcasts

**Key Methods:**
- `runServiceLoop()` - Main service loop
- `scanAndConnect()` - Scan for device and attempt connection
- `connectToDevice()` - Establish Bluetooth connection
- `listenForData()` - Read data from Bluetooth socket
- `processGpsMessage()` - Parse and validate GPS data

---

## Customization

### Change Scan Interval

In `BluetoothService.kt`:
```kotlin
private const val SCAN_INTERVAL_MS = 10000L  // 10 seconds (change as needed)
```

### Change Device Name

In `BluetoothService.kt`:
```kotlin
const val TARGET_DEVICE_NAME = "ESP32_TRAILER_001"  // Your device name
```

### Add Email Functionality

The email sending code is commented out. To enable:

1. Add dependencies to `build.gradle`:
```gradle
implementation 'com.sun.mail:android-mail:1.6.7'
implementation 'com.sun.mail:android-activation:1.6.7'
```

2. Implement email sending in `processGpsMessage()` method
3. Store email credentials securely (use Android Keystore)

---

## Troubleshooting

### "Bluetooth permission required"
- Go to Settings → Apps → GPS Tracker → Permissions
- Enable all requested permissions

### "Device not found"
- Ensure ESP32 is powered on
- Verify ESP32 Bluetooth name matches `TARGET_DEVICE_NAME`
- Try manually pairing via tablet Bluetooth settings first

### "Connection failed"
- Check ESP32 is broadcasting on SPP profile (UUID: 00001101-...)
- Ensure no other app is connected to the ESP32
- Try restarting both devices

### Service stops running
- Disable battery optimization for the app:
  - Settings → Apps → GPS Tracker → Battery → Unrestricted
- The app runs as a foreground service, but some manufacturers aggressively kill apps

### No location updates
- Check ESP32 serial output for errors
- Verify GPS message format matches expected format
- Look at Logcat in Android Studio (filter by "BluetoothService")

---

## Testing Without ESP32

To test the app without an ESP32 device:

1. Change `TARGET_DEVICE_NAME` to any Bluetooth device you have
2. The app will connect but won't receive valid GPS messages (that's OK for testing)
3. Or use a Bluetooth Serial Terminal app on another phone to send test messages

**Test Message:**
```
TEST_001|49.282729|-123.120738|90.00|2025-11-05T12:00:00|END
```

---

## Debugging

### View Logs

Use Android Studio Logcat:
1. Run the app on connected device
2. Open **Logcat** tab at bottom of Android Studio
3. Filter by **"BluetoothService"** tag

**Useful log messages:**
- "Scanning for device..."
- "Found target device..."
- "Successfully connected!"
- "Received: [message]"
- "GPS Data - Lat: X, Lon: Y..."

### Enable Verbose Logging

In `BluetoothService.kt`, all logs use the `Log.d()` tag. These appear in Logcat automatically.

---

## Building for Release

### Create Signed APK

1. **Build → Generate Signed Bundle/APK**
2. Select **APK**
3. Create or select a keystore
4. Choose **release** build variant
5. Click **Finish**

The signed APK will be in: `app/release/app-release.apk`

### For Production Use

Before releasing:
- [ ] Change app icon (`ic_launcher`)
- [ ] Update app name in `strings.xml`
- [ ] Enable ProGuard for code obfuscation
- [ ] Add crash reporting (Firebase Crashlytics)
- [ ] Implement email credential encryption
- [ ] Add analytics if needed
- [ ] Test on multiple Android versions

---

## Future Enhancements

Possible additions:
- ✓ Email sending with SMTP (code structure ready)
- ✓ Map view showing current location
- ✓ Location history with database storage
- ✓ Multiple device support
- ✓ Export location data to CSV
- ✓ Geofencing alerts
- ✓ Battery level monitoring for ESP32

---

## Technical Details

- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Language**: Kotlin
- **Bluetooth Profile**: SPP (Serial Port Profile)
- **UUID**: `00001101-0000-1000-8000-00805F9B34FB`
- **Architecture**: Service + Activity with BroadcastReceiver

---

## License

This is a simple starter project - feel free to modify and extend as needed.

---

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review logs in Android Studio Logcat
3. Verify ESP32 is sending correct message format
4. Ensure all permissions are granted

---

**Quick Start Summary:**

1. Open project in Android Studio
2. Build and install on tablet
3. Enable Bluetooth
4. Tap Start
5. App auto-connects to ESP32
6. Location updates appear automatically

That's it! Simple and straightforward. ✓
