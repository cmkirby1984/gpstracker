# ESP32 Fleet GPS Tracker Firmware

Production-ready firmware for ESP32 + NEO-6M GPS tracking system.

## Features

- GPS coordinate reading via UART
- Bluetooth Classic (SPP) communication
- 5-minute update interval
- Mock/simulation mode for testing without hardware
- Comprehensive debug logging
- Low-power sleep modes
- Automatic reconnection handling

## Quick Start

### Build Firmware
```bash
platformio run
```

### Run Tests
```bash
platformio test
```

### Flash to ESP32 (when hardware ready)
```bash
# Update upload_port in platformio.ini first
platformio run -t upload
```

### Monitor Serial Output
```bash
platformio device monitor
```

## Configuration

All configuration is in `include/config.h`:

### Critical Settings (Update when hardware arrives)
```cpp
// GPS UART pins
#define GPS_RX_PIN 16          // TODO: Verify with your wiring
#define GPS_TX_PIN 17          // TODO: Verify with your wiring

// Bluetooth settings
#define BT_DEVICE_NAME "Fleet_GPS_001"  // TODO: Set unique name

// Update interval
#define UPDATE_INTERVAL_MS 300000  // 5 minutes (in milliseconds)
```

### Mock Mode
The firmware compiles with mock GPS data enabled by default:
```cpp
#define USE_MOCK_GPS 1  // Set to 0 when real GPS hardware is connected
```

This allows testing all Bluetooth and messaging features without GPS hardware.

## Pin Connections

When hardware arrives, connect:

| ESP32 Pin | NEO-6M GPS | Notes |
|-----------|------------|-------|
| GPIO 16   | TX         | GPS transmit to ESP32 receive |
| GPIO 17   | RX         | ESP32 transmit to GPS receive |
| 3.3V      | VCC        | Power (NEO-6M uses 3.3V) |
| GND       | GND        | Ground |

See [../docs/HARDWARE_SETUP.md](../docs/HARDWARE_SETUP.md) for detailed wiring diagrams.

## Message Protocol

Firmware sends pipe-delimited messages via Bluetooth:
```
SERIAL|LAT|LON|HEADING|TIME|END
```

Example:
```
GPS001|37.7749|-122.4194|270.5|2025-11-06T23:15:30Z|END
```

## Debug Logging

Firmware includes comprehensive logging via Serial (115200 baud):

```cpp
LOG_ERROR("Critical failure");   // Level 1
LOG_WARN("Warning condition");   // Level 2
LOG_INFO("Normal operation");    // Level 3
LOG_DEBUG("Debug details");      // Level 4
LOG_VERBOSE("Detailed trace");   // Level 5
```

Set log level in `platformio.ini`:
```ini
-DLOG_LEVEL=3  ; 0=OFF, 1=ERROR, 2=WARN, 3=INFO, 4=DEBUG, 5=VERBOSE
```

## Project Structure

```
firmware/
├── include/
│   ├── config.h              # Configuration constants
│   ├── gps_module.h          # GPS interface
│   ├── bluetooth_module.h    # Bluetooth interface
│   ├── message_protocol.h    # Message formatting
│   └── logger.h              # Debug logging
├── src/
│   ├── main.cpp              # Main program loop
│   ├── gps_module.cpp        # GPS implementation
│   ├── bluetooth_module.cpp  # Bluetooth implementation
│   ├── message_protocol.cpp  # Message protocol
│   └── logger.cpp            # Logger implementation
├── test/
│   ├── test_message_protocol.cpp
│   └── test_gps_parser.cpp
└── scripts/
    ├── build.sh              # Build verification
    └── flash.sh              # Upload script
```

## Development Workflow

### Before Hardware Arrives
1. Build and test with mock mode enabled
2. Verify Bluetooth advertising works
3. Test message formatting
4. Validate all unit tests pass

### When Hardware Arrives
1. Update pin assignments in `config.h`
2. Set `USE_MOCK_GPS 0` in `config.h`
3. Flash firmware: `platformio run -t upload`
4. Monitor output: `platformio device monitor`
5. Verify GPS fix acquisition (may take 30-60 seconds outdoors)
6. Confirm Bluetooth messages are sent every 5 minutes

## Troubleshooting

### GPS Not Getting Fix
- Ensure NEO-6M has clear view of sky
- Cold start can take 30-60 seconds
- Check GPS LED is blinking (indicates satellite search)
- Verify UART connections and baud rate (9600)

### Bluetooth Not Connecting
- Check device appears in tablet's Bluetooth scan
- Verify Bluetooth name matches config
- Try power cycle of ESP32
- Check Serial logs for Bluetooth initialization errors

### Compilation Errors
- Ensure PlatformIO Core is up to date: `pio upgrade`
- Clean build: `platformio run -t clean`
- Check library dependencies are installed

## Power Consumption

Typical current draw:
- Active GPS + Bluetooth: ~80-120mA
- GPS sleep mode: ~20-30mA
- Deep sleep: ~10μA (for future optimization)

## Testing

### Unit Tests
```bash
platformio test -v
```

Tests include:
- Message protocol formatting
- GPS data parsing
- Checksum validation
- Bluetooth state machine

### Manual Testing
Use `tools/bluetooth_simulator.py` to verify messages from ESP32.

## Version History

- v1.0.0 - Initial release with mock mode support
- Hardware integration pending

## Support

See [../docs/TROUBLESHOOTING.md](../docs/TROUBLESHOOTING.md) for common issues.
