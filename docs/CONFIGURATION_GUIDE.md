# Configuration Guide

Complete reference for all configuration options.

## ESP32 Firmware Configuration

All settings in `firmware/include/config.h`

### Hardware Configuration

#### GPS Module Settings
```cpp
#define GPS_RX_PIN 16              // ESP32 RX pin (GPS TX)
#define GPS_TX_PIN 17              // ESP32 TX pin (GPS RX)
#define GPS_BAUD_RATE 9600         // NEO-6M default
```
**Change if**: Using different pins or GPS module

#### Bluetooth Settings
```cpp
#define BT_DEVICE_NAME "FleetGPS_001"  // Unique name for each device
#define BT_PIN "1234"                  // Pairing PIN
#define BT_ENABLE_SSP true             // Secure Simple Pairing
```
**Change to**: Unique name for each fleet device

### Timing Configuration

```cpp
#define UPDATE_INTERVAL_MS 300000   // 5 minutes
```
**Options**:
- `60000` = 1 minute (frequent updates)
- `300000` = 5 minutes (default)
- `900000` = 15 minutes (battery saving)
- `1800000` = 30 minutes (extended battery)

```cpp
#define GPS_READ_TIMEOUT_MS 5000      // Wait for GPS data
#define GPS_FIX_TIMEOUT_MS 120000     // Wait for GPS fix
#define BT_CONNECT_TIMEOUT_MS 10000   // Wait for BT connection
```

### Device Identification

```cpp
#define DEVICE_SERIAL "GPS001"     // Unique per device
#define FIRMWARE_VERSION "1.0.0"   // Update with releases
```
**Format**: GPS001, GPS002, GPS003, etc.

### Mock Mode

```cpp
#define USE_MOCK_GPS 1             // 1=mock, 0=real GPS
```
**Development**: Set to 1
**Production**: Set to 0

### Mock Coordinates

```cpp
#define MOCK_GPS_LAT 37.7749       // San Francisco
#define MOCK_GPS_LON -122.4194
#define MOCK_GPS_HEADING 270.5
```
**Change to**: Your test location

### Logging Configuration

```cpp
#define LOG_LEVEL 3                // 0-5 verbosity
```
**Levels**:
- `0` = OFF (production)
- `1` = ERROR (critical only)
- `2` = WARN (warnings)
- `3` = INFO (normal, default)
- `4` = DEBUG (detailed)
- `5` = VERBOSE (everything)

```cpp
#define LOG_GPS_SENTENCES 1        // Log raw GPS data
#define LOG_BT_EVENTS 1           // Log Bluetooth events
#define LOG_MESSAGE_PROTOCOL 1    // Log messages
```

### GPS Validation

```cpp
#define MIN_SATELLITES 4           // Minimum for valid fix
#define MAX_HDOP 5.0              // Maximum HDOP
```
**Lower MIN_SATELLITES**: Less accurate but faster fix
**Higher MIN_SATELLITES**: More accurate but slower fix

### Serial Debug

```cpp
#define SERIAL_BAUD_RATE 115200    // Serial monitor speed
```

### Advanced Features

```cpp
#define ENABLE_SLEEP_MODE 0        // Power saving
#define SLEEP_BETWEEN_UPDATES 0    // Sleep when idle
#define ENABLE_WATCHDOG 1          // Automatic recovery
#define WATCHDOG_TIMEOUT_SEC 30    // Reset if frozen
```

## Android App Configuration

Settings managed via `ConfigManager` (SharedPreferences).

### ESP32 Connection

```java
config.setEsp32MacAddress("AA:BB:CC:DD:EE:FF");
config.setEsp32DeviceName("FleetGPS_001");
```
**Find MAC**: Check serial monitor on ESP32 boot

### SMTP Email Settings

#### Gmail Configuration
```java
config.setSmtpHost("smtp.gmail.com");
config.setSmtpPort(587);
config.setSmtpUseTls(true);
config.setSmtpUser("your-email@gmail.com");
config.setSmtpPassword("your-app-password");  // Not regular password!
config.setSmtpFromEmail("your-email@gmail.com");
config.setSmtpToEmails("recipient1@example.com,recipient2@example.com");
```

#### Other SMTP Providers

**Outlook/Office 365**:
```java
config.setSmtpHost("smtp.office365.com");
config.setSmtpPort(587);
config.setSmtpUseTls(true);
```

**Yahoo**:
```java
config.setSmtpHost("smtp.mail.yahoo.com");
config.setSmtpPort(587);
config.setSmtpUseTls(true);
```

**Custom Server**:
```java
config.setSmtpHost("mail.yourdomain.com");
config.setSmtpPort(587);  // Or 465 for SSL
config.setSmtpUseTls(true);  // Or false for SSL
```

### App Behavior

```java
config.setAutoConnect(true);      // Auto-connect on startup
config.setAutoSendEmail(true);    // Auto-send emails for locations
```

## PlatformIO Configuration

File: `firmware/platformio.ini`

### Build Flags

```ini
build_flags =
    -DCORE_DEBUG_LEVEL=3           # ESP32 debug level
    -DUSE_MOCK_GPS=1               # Mock mode
    -DLOG_LEVEL=3                  # App log level
```

### Upload Settings

```ini
upload_port = /dev/ttyUSB0         # Linux
upload_port = /dev/cu.usbserial    # macOS
upload_port = COM3                 # Windows
upload_speed = 115200
```

### Library Versions

```ini
lib_deps =
    TinyGPSPlus@^1.0.3
    BluetoothSerial
```

## Android Gradle Configuration

File: `android/app/build.gradle`

### Version Numbers

```gradle
versionCode 1                      # Increment for each release
versionName "1.0.0"               # Semantic versioning
```

### Build Variants

```gradle
debug {
    applicationIdSuffix ".debug"
    versionNameSuffix "-DEBUG"
}

release {
    minifyEnabled true
    shrinkResources true
}
```

### SDK Versions

```gradle
minSdk 24                         # Android 7.0+
targetSdk 34                      # Latest stable
compileSdk 34
```

## Configuration Best Practices

### For Development

**ESP32**:
- `USE_MOCK_GPS = 1`
- `LOG_LEVEL = 4` (DEBUG)
- `UPDATE_INTERVAL_MS = 30000` (30 seconds for faster testing)

**Android**:
- Debug build variant
- Test SMTP credentials
- Single test recipient

### For Production

**ESP32**:
- `USE_MOCK_GPS = 0`
- `LOG_LEVEL = 1` (ERROR only)
- `UPDATE_INTERVAL_MS = 300000` (5 minutes)
- Unique `DEVICE_SERIAL` per device
- Unique `BT_DEVICE_NAME` per device

**Android**:
- Release build variant
- Production SMTP credentials
- All required recipients
- Auto-connect enabled

### For Battery Operation

**ESP32**:
```cpp
#define ENABLE_SLEEP_MODE 1
#define SLEEP_BETWEEN_UPDATES 1
#define UPDATE_INTERVAL_MS 900000  // 15 minutes
```

### For High-Frequency Tracking

**ESP32**:
```cpp
#define UPDATE_INTERVAL_MS 60000  // 1 minute
#define MIN_SATELLITES 4           // Accept with 4 sats
```

## Environment-Specific Configuration

### Indoor Testing

```cpp
#define GPS_FIX_TIMEOUT_MS 300000  // 5 minutes (longer)
#define MIN_SATELLITES 3            // Accept weaker fix
```

### Vehicle Installation

```cpp
#define UPDATE_INTERVAL_MS 60000   // 1 minute when moving
#define MIN_SATELLITES 5            // Better accuracy
```

### Fixed Installation

```cpp
#define UPDATE_INTERVAL_MS 1800000 // 30 minutes
#define ENABLE_SLEEP_MODE 1         // Save power
```

## Security Configuration

### Bluetooth Security

```cpp
#define BT_ENABLE_SSP true         // Secure pairing
#define BT_PIN "1234"              // Change default PIN
```

### Android Security

- Store SMTP password encrypted (handled automatically)
- Don't hardcode credentials in source
- Use environment variables for builds

## Troubleshooting Configuration Issues

### GPS Not Working

Check:
- `GPS_RX_PIN` and `GPS_TX_PIN` match wiring
- `GPS_BAUD_RATE` matches GPS module (usually 9600)
- `USE_MOCK_GPS` is 0 for real GPS

### Bluetooth Not Connecting

Check:
- `BT_DEVICE_NAME` is unique
- MAC address correct in Android app
- Bluetooth permissions granted

### No Email Sending

Check:
- SMTP host and port correct
- Using app password (for Gmail)
- TLS/SSL settings match server
- Internet connection available
- Recipient emails valid

## Configuration Validation

Before deployment, verify:

```bash
# ESP32
cd firmware
grep "USE_MOCK_GPS 0" include/config.h
grep "DEVICE_SERIAL" include/config.h

# Build and check
platformio run

# Android
cd android
./gradlew build
```

## Configuration Backup

Important files to backup:
- `firmware/include/config.h`
- Android SharedPreferences (exported via settings)
- List of device serials and MAC addresses
- SMTP credentials (securely)

## Default Values Summary

| Setting | Default | Production |
|---------|---------|------------|
| Update Interval | 5 min | 5-15 min |
| Mock GPS | ON | OFF |
| Log Level | INFO | ERROR |
| Min Satellites | 4 | 4-5 |
| Auto Connect | OFF | ON |
| Auto Email | ON | ON |

## Support

For configuration issues, see [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
