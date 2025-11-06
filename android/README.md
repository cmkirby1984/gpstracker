# Fleet GPS Tracker - Android Application

Android tablet application for receiving GPS data from ESP32 via Bluetooth and sending email updates.

## Features

- Bluetooth Classic (SPP) communication with ESP32
- Real-time GPS location display
- SMTP email notification system
- Room database for location history
- Foreground service for reliable background operation
- Material Design UI optimized for tablets

## Requirements

- Android 7.0 (API 24) or higher
- Bluetooth support
- Internet connection (for email sending)
- Android Studio Arctic Fox or newer (for development)

## Quick Start

### Building the App

```bash
cd android
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

### Installing on Device

```bash
./gradlew installDebug
```

### Building Release APK

```bash
./gradlew assembleRelease
```

## Configuration

Before first use, configure the following in the app:

1. **ESP32 Bluetooth Device**
   - MAC address of your ESP32
   - Device name (optional)

2. **SMTP Email Settings**
   - SMTP server (e.g., smtp.gmail.com)
   - Port (587 for TLS, 465 for SSL)
   - Username and password
   - From email address
   - Recipient email addresses (comma-separated)

### Gmail Configuration

For Gmail, you need to:
1. Enable 2-factor authentication
2. Generate an App Password
3. Use the App Password in SMTP settings

## Project Structure

```
android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/fleet/gpstracker/
│   │   │   │   ├── models/          # Data models
│   │   │   │   ├── database/        # Room database
│   │   │   │   ├── bluetooth/       # Bluetooth service
│   │   │   │   ├── email/           # Email functionality
│   │   │   │   ├── services/        # Background services
│   │   │   │   ├── utils/           # Utilities
│   │   │   │   └── MainActivity.java
│   │   │   ├── res/                 # Resources
│   │   │   └── AndroidManifest.xml
│   │   └── test/                    # Unit tests
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── README.md
```

## Key Components

### Message Parser
Parses GPS protocol messages: `SERIAL|LAT|LON|HEADING|TIME|END`

### Bluetooth Service
Manages Bluetooth SPP connection and data reception.

### GPS Tracker Service
Foreground service that coordinates Bluetooth, database, and email.

### Email Module
SMTP email sender using JavaMail API.

### Room Database
Stores location history with email send status.

## Permissions

The app requires:
- `BLUETOOTH` / `BLUETOOTH_ADMIN` (Android 11 and below)
- `BLUETOOTH_CONNECT` / `BLUETOOTH_SCAN` (Android 12+)
- `INTERNET`
- `FOREGROUND_SERVICE`
- `POST_NOTIFICATIONS` (Android 13+)

## Testing

### Unit Tests
```bash
./gradlew test
```

### With Mock Data
Use the mock data generator in `tools/mock_gps_simulator.py`

### Without ESP32
The app can be tested with Bluetooth simulator tools.

## Troubleshooting

### Bluetooth Connection Issues
- Ensure Bluetooth permissions are granted
- Check ESP32 is powered and advertising
- Verify MAC address is correct
- Try unpairing and re-pairing

### Email Sending Issues
- Verify SMTP settings
- Check internet connection
- Use `tools/test_email.py` to test SMTP config
- For Gmail, ensure App Password is used

### App Crashes
- Check logcat for errors
- Verify all permissions granted
- Clear app data and restart

## Development

### Adding Dependencies
Edit `app/build.gradle` and sync project.

### Debugging
Connect device via USB and use Android Studio debugger.

### ProGuard
Release builds use ProGuard. Rules in `proguard-rules.pro`.

## Build Variants

- **debug**: Debuggable build with logging
- **release**: Optimized build with ProGuard

## Performance

- Foreground service ensures reliability
- Database operations run on background thread
- Email sending is asynchronous

## Future Enhancements

- Settings activity for configuration
- Location history viewer
- Multiple device support
- Map view integration
- Export to CSV/KML

## License

Private project - All rights reserved

## Support

See [../docs/TROUBLESHOOTING.md](../docs/TROUBLESHOOTING.md)
