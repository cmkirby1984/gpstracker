# Hardware Setup Guide

Complete wiring and configuration guide for ESP32 + NEO-6M GPS module.

## Components Required

- ESP32 development board
- NEO-6M GPS module
- Android tablet with Bluetooth
- USB cable for ESP32 programming
- Power supply (USB or battery)
- Jumper wires

## ESP32 Pin Configuration

### NEO-6M GPS Module Connections

| ESP32 Pin | NEO-6M Pin | Wire Color | Notes |
|-----------|------------|------------|-------|
| GPIO 16   | TX         | Yellow     | GPS transmits to ESP32 |
| GPIO 17   | RX         | Green      | ESP32 transmits to GPS |
| 3.3V      | VCC        | Red        | **Use 3.3V, NOT 5V!** |
| GND       | GND        | Black      | Common ground |

**CRITICAL**: NEO-6M uses 3.3V logic. DO NOT connect to 5V!

### Wiring Diagram

```
ESP32                    NEO-6M GPS
┌─────────┐             ┌──────────┐
│         │             │          │
│  GPIO16 ├─────────────┤ TX       │
│  (RX)   │   Yellow    │          │
│         │             │          │
│  GPIO17 ├─────────────┤ RX       │
│  (TX)   │   Green     │          │
│         │             │          │
│  3.3V   ├─────────────┤ VCC      │
│         │   Red       │          │
│         │             │          │
│  GND    ├─────────────┤ GND      │
│         │   Black     │          │
└─────────┘             └──────────┘
```

## Configuration Steps

### 1. Physical Assembly

1. **Power off** ESP32 before wiring
2. Connect wires according to table above
3. Double-check VCC is on 3.3V, NOT 5V
4. Ensure good connections (solder if permanent)
5. Mount GPS module with clear sky view

### 2. Firmware Configuration

Edit `firmware/include/config.h`:

```cpp
// Verify pin assignments
#define GPS_RX_PIN 16
#define GPS_TX_PIN 17

// Set unique Bluetooth name
#define BT_DEVICE_NAME "FleetGPS_001"  // Change for each device

// Set device serial number
#define DEVICE_SERIAL "GPS001"  // Unique identifier
```

### 3. Flash Firmware

```bash
cd firmware
platformio run -t upload --upload-port /dev/ttyUSB0
```

Replace `/dev/ttyUSB0` with your port (check with `ls /dev/tty*`).

## GPS Module Setup

### Antenna Placement

- Mount with clear view of sky
- Away from metal objects
- Horizontal orientation preferred
- Outdoors or near window for best results

### First GPS Fix

- Cold start can take 30-60 seconds
- GPS LED will blink during satellite search
- Solid LED indicates fix acquired
- Move outdoors if fix not acquired within 2 minutes

## Power Considerations

### USB Power

- Simplest option for testing
- Requires USB cable to computer or power adapter
- Current draw: ~100-150mA

### Battery Power

- Use 3.7V LiPo battery with proper protection circuit
- Add voltage regulator if needed
- Expected runtime: 10-20 hours (depends on battery capacity)
- Add deep sleep for extended runtime

## Verification

### 1. Serial Monitor Test

```bash
platformio device monitor
```

Expected output:
```
Fleet GPS Tracker System
Firmware: 1.0.0
Device: GPS001
GPS initialized successfully
Bluetooth initialized successfully
System Ready!
```

### 2. GPS Fix Verification

After 30-60 seconds outdoors:
```
GPS fix acquired!
Location: 37.774900, -122.419400
Satellites: 8
```

### 3. Bluetooth Test

On Android tablet:
1. Open Bluetooth settings
2. Scan for devices
3. Should see "FleetGPS_001" (or your configured name)
4. Pair if needed (PIN: 1234)

## Troubleshooting

### GPS Module Issues

**Problem**: No GPS data received
- Check wiring (TX/RX might be swapped)
- Verify 3.3V power
- Ensure GPS module LED is on
- Try outdoors with clear sky view

**Problem**: GPS LED blinking but no fix
- Wait longer (cold start can take 60+ seconds)
- Move to location with better sky view
- Check antenna connection
- GPS may be defective if never gets fix

### ESP32 Issues

**Problem**: Won't flash firmware
- Check USB cable (must be data cable, not charge-only)
- Try holding BOOT button during flash
- Check correct COM port selected
- Driver issues: Install CP210x or CH340 drivers

**Problem**: Bluetooth not appearing
- Verify Bluetooth enabled in config
- Check BT_DEVICE_NAME is unique
- Power cycle ESP32
- Check for other devices with same name

### Wiring Issues

**Problem**: Intermittent connection
- Check all wire connections
- Solder connections for permanent install
- Use shorter wires if possible
- Check for loose breadboard connections

## Advanced Configuration

### Multiple Devices

For fleet deployment:
1. Set unique `BT_DEVICE_NAME` for each ESP32
2. Set unique `DEVICE_SERIAL` for each ESP32
3. Label physical devices to match configuration
4. Document MAC addresses

### Enclosure

Recommendations:
- Waterproof enclosure for outdoor use
- GPS antenna outside enclosure (or use window)
- Ventilation holes for heat dissipation
- Access to USB port for programming
- Status LED visible externally

### Power Optimization

For battery operation, enable in `config.h`:
```cpp
#define ENABLE_SLEEP_MODE 1
#define SLEEP_BETWEEN_UPDATES 1
```

## Safety Notes

- Never exceed 3.3V on GPS module
- Use proper power supply ratings
- Don't short circuit power pins
- Disconnect power during wiring
- Use insulated enclosure for permanent install

## Testing Checklist

- [ ] Wiring verified against diagram
- [ ] 3.3V confirmed on GPS VCC
- [ ] Firmware flashed successfully
- [ ] Serial monitor shows startup messages
- [ ] GPS fix acquired (outdoors)
- [ ] Bluetooth device visible on tablet
- [ ] Messages received by Android app
- [ ] Email notifications working

## Support

For issues, see [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
