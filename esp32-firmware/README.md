# ESP32 GPS Tracker Firmware

MicroPython firmware for ESP32 with Neo-6M GPS module. Reads GPS coordinates and transmits them via Bluetooth to a paired Android tablet.

## Hardware Requirements

- **ESP32 Development Board** (ESP32-DevKitC or similar)
- **Neo-6M GPS Module** with antenna
- **USB Cable** for programming and power
- **Jumper Wires** for connections
- **Power Supply** (can use USB power bank or hardwire to vehicle)

## Wiring Diagram

### Neo-6M to ESP32 Connections

```
Neo-6M GPS Module          ESP32 Board
==================         ===========
VCC (3.3V-5V)     ------>  3.3V or 5V
GND               ------>  GND
TX                ------>  GPIO 16 (RX2)
RX                ------>  GPIO 17 (TX2)
```

### Wiring Notes

- Neo-6M VCC can accept 3.3V-5V (check your module specifications)
- Connect Neo-6M TX to ESP32 RX (GPIO 16)
- Connect Neo-6M RX to ESP32 TX (GPIO 17)
- Make sure antenna has clear view of sky for best GPS reception

## Software Requirements

### 1. Install MicroPython on ESP32

Download and flash MicroPython firmware:

```bash
# Install esptool
pip install esptool

# Erase flash (replace /dev/ttyUSB0 with your port)
esptool.py --chip esp32 --port /dev/ttyUSB0 erase_flash

# Flash MicroPython firmware
esptool.py --chip esp32 --port /dev/ttyUSB0 write_flash -z 0x1000 esp32-micropython.bin
```

Download MicroPython firmware from: https://micropython.org/download/esp32/

### 2. Install Required Tools

```bash
# Install ampy for file transfer
pip install adafruit-ampy

# Or use rshell
pip install rshell

# Or use Thonny IDE (recommended for beginners)
# Download from: https://thonny.org/
```

## Installation

### Using ampy

```bash
# Set your serial port
export AMPY_PORT=/dev/ttyUSB0

# Upload files to ESP32
ampy put config.py
ampy put gps_parser.py
ampy put main.py

# Verify files were uploaded
ampy ls
```

### Using rshell

```bash
# Connect to ESP32
rshell -p /dev/ttyUSB0

# Copy files
cp config.py /pyboard/
cp gps_parser.py /pyboard/
cp main.py /pyboard/

# Exit rshell
exit
```

### Using Thonny IDE

1. Open Thonny
2. Go to Tools → Options → Interpreter
3. Select "MicroPython (ESP32)"
4. Choose your serial port
5. Open each .py file and click "Save" → "MicroPython device"
6. Save as the same filename

## Configuration

Edit `config.py` to customize settings:

```python
# Device identifier (change for each tracker)
DEVICE_SERIAL_NUMBER = "TRAILER_001"

# GPS update interval (milliseconds)
GPS_UPDATE_INTERVAL = 300000  # 5 minutes

# GPS UART pins (change if using different pins)
GPS_TX_PIN = 17
GPS_RX_PIN = 16

# Bluetooth name
BLUETOOTH_NAME = "ESP32_TRAILER_001"
```

## Usage

### Running the Firmware

The firmware will start automatically if saved as `main.py`. To run manually:

```bash
# Connect via serial terminal
screen /dev/ttyUSB0 115200

# Or use ampy
ampy run main.py
```

### Expected Output

```
==================================================
ESP32 GPS Tracker Starting...
Device: TRAILER_001
Update Interval: 300.0s
==================================================

Initializing GPS module...
GPS UART: TX=17, RX=16, Baud=9600

Initializing Bluetooth...
Bluetooth initialized: ESP32_TRAILER_001
Waiting for connection...

System Ready!
--------------------------------------------------

Reading GPS data...
GPS Fix Acquired:
  Latitude:   49.123456
  Longitude:  -123.456789
  Heading:    275.50°
  Time:       14:30:00
  Satellites: 8

Formatted Message: TRAILER_001|49.123456|-123.456789|275.50|14:30:00|END
✓ Data sent via Bluetooth

Waiting 300.0s until next update...
```

## LED Indicators

The built-in LED (GPIO 2) provides visual feedback:

- **3 quick blinks** at startup = System ready
- **Slow blink** (0.5s) = Waiting for Bluetooth connection
- **Solid ON** = Bluetooth connected
- **3 quick blinks** after sending = Data transmitted successfully
- **5 quick blinks** = Error occurred

## Troubleshooting

### GPS Module Not Working

1. **Check wiring**: Verify TX/RX are not swapped
2. **Check antenna**: Ensure antenna has clear view of sky
3. **Wait for fix**: GPS can take 30-60 seconds for initial fix (cold start)
4. **Test with serial monitor**: GPS should output NMEA sentences every second

### Bluetooth Not Available

The code uses Bluetooth Classic (SPP), which may not be available in all MicroPython builds.

**Solutions:**

1. **Use standard MicroPython build** with Bluetooth Classic support
2. **Alternative**: Modify code to use BLE (Bluetooth Low Energy)
3. **For testing**: Code will output to serial/USB if Bluetooth unavailable

### No GPS Fix

- Move to location with clear view of sky
- Check that GPS module LED is blinking (indicates searching)
- Wait at least 60 seconds for initial fix
- Verify GPS module is powered (should have power LED on)

### Files Not Uploading

- Check serial port permissions: `sudo usermod -a -G dialout $USER`
- Try different USB cable
- Reset ESP32 before uploading
- Verify MicroPython is installed: `ampy run -n` should show `>>>` prompt

## Testing Without Android Tablet

You can test the system by monitoring serial output:

```bash
# Connect with screen
screen /dev/ttyUSB0 115200

# Or use minicom
minicom -D /dev/ttyUSB0 -b 115200

# Or use Python serial
python -m serial.tools.miniterm /dev/ttyUSB0 115200
```

The GPS data will be printed to serial even without Bluetooth connection.

## Message Format

Format: `SERIAL|LAT|LON|HEADING|TIME|END`

Example: `TRAILER_001|49.123456|-123.456789|275.50|14:30:00|END`

## Power Options

### USB Power Bank
- Simple solution for testing
- Limited runtime (depends on battery capacity)
- Good for 8-24 hours depending on battery size

### Hardwired to Vehicle
- Connect to 12V vehicle power with voltage regulator
- Use 12V to 5V step-down converter (e.g., LM2596)
- Add fuse for protection
- Provides continuous power

### Battery with Solar
- Use LiPo battery with solar panel
- Add charging circuit (TP4056 or similar)
- Best for long-term outdoor use

## File Structure

```
esp32-firmware/
├── config.py          # Configuration settings
├── gps_parser.py      # GPS NMEA parser module
├── main.py            # Main firmware program
├── PSEUDOCODE.md      # Algorithm pseudocode
└── README.md          # This file
```

## Next Steps

1. Upload firmware to ESP32
2. Test GPS reception outdoors
3. Pair with Android tablet via Bluetooth
4. Configure Android app to receive GPS data
5. Test complete system

## Additional Resources

- [ESP32 MicroPython Documentation](https://docs.micropython.org/en/latest/esp32/quickref.html)
- [Neo-6M GPS Module Datasheet](https://www.u-blox.com/en/product/neo-6-series)
- [NMEA Sentence Format](https://www.gpsinformation.org/dale/nmea.htm)

## License

This project is part of the Trailer GPS Tracker System.
