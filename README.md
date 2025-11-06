# Fleet GPS Tracking System

A complete GPS tracking solution using ESP32 + NEO-6M GPS module with Android tablet interface.

## System Overview

**Hardware Components:**
- ESP32 microcontroller
- NEO-6M GPS module
- Android tablet (Bluetooth receiver)

**Communication Flow:**
```
ESP32 + GPS → Bluetooth → Android Tablet → Email (SMTP)
   (5 min intervals)
```

**Message Protocol:**
```
SERIAL|LAT|LON|HEADING|TIME|END
```

## Project Structure

```
├── firmware/          # ESP32 Arduino/PlatformIO project
├── android/           # Android application
├── docs/              # Comprehensive documentation
└── tools/             # Development and testing utilities
```

## Quick Start

### Prerequisites
- PlatformIO Core or Arduino IDE (for ESP32 firmware)
- Android Studio (for Android app)
- Python 3.8+ (for testing tools)

### ESP32 Firmware Setup
```bash
cd firmware
platformio run          # Build firmware
platformio test         # Run unit tests
platformio run -t upload  # Flash to device (when hardware ready)
```

See [firmware/README.md](firmware/README.md) for detailed instructions.

### Android App Setup
```bash
cd android
./gradlew build         # Build app
./gradlew test          # Run tests
./gradlew installDebug  # Install on device
```

See [android/README.md](android/README.md) for detailed instructions.

## Documentation

- [Hardware Setup Guide](docs/HARDWARE_SETUP.md) - Wiring and pin configurations
- [First Boot Procedure](docs/FIRST_BOOT.md) - Initial setup steps
- [Testing Checklist](docs/TESTING_CHECKLIST.md) - Phase-by-phase testing
- [Configuration Guide](docs/CONFIGURATION_GUIDE.md) - All settings explained
- [Troubleshooting](docs/TROUBLESHOOTING.md) - Common issues and solutions

## Development Workflow

### Before Hardware Arrives
1. All code compiles and passes tests
2. Use mock/simulation modes for development
3. Test message parsing with mock data
4. Configure email settings

### When Hardware Arrives
1. Follow [HARDWARE_SETUP.md](docs/HARDWARE_SETUP.md) for wiring
2. Update pin configurations in `firmware/include/config.h`
3. Flash firmware to ESP32
4. Follow [FIRST_BOOT.md](docs/FIRST_BOOT.md) for initial setup
5. Use [TESTING_CHECKLIST.md](docs/TESTING_CHECKLIST.md) for validation

## Configuration

### ESP32 Configuration
Edit `firmware/include/config.h`:
- GPS serial pins
- Bluetooth device name
- Update interval (default: 5 minutes)
- Debug logging level

### Android Configuration
Edit `android/app/src/main/res/values/config.xml`:
- SMTP server settings
- Email recipients
- Database retention period
- Bluetooth connection parameters

## Testing

### Without Hardware (Pre-arrival)
```bash
# Test ESP32 message protocol
cd firmware && platformio test

# Test Android message parser
cd android && ./gradlew test

# Simulate GPS data
python tools/mock_gps_simulator.py

# Simulate Bluetooth communication
python tools/bluetooth_simulator.py

# Test email sending
python tools/test_email.py
```

### With Hardware
Follow the [TESTING_CHECKLIST.md](docs/TESTING_CHECKLIST.md)

## Build Status

- ✅ ESP32 firmware compiles successfully
- ✅ Android app builds successfully
- ✅ Unit tests pass
- ⏳ Hardware integration pending

## License

Private project - All rights reserved

## Support

For issues or questions, refer to [TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)
