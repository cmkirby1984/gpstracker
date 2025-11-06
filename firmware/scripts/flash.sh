#!/bin/bash
# Flash script for ESP32 firmware

set -e  # Exit on error

echo "========================================="
echo "  ESP32 Firmware Flash Script"
echo "========================================="
echo ""

# Check if platformio is installed
if ! command -v platformio &> /dev/null; then
    echo "ERROR: PlatformIO not found!"
    echo "Install with: pip install platformio"
    exit 1
fi

# Check for ESP32 device
echo "Checking for ESP32 device..."
if [ -z "$1" ]; then
    echo "Available serial ports:"
    platformio device list
    echo ""
    echo "Usage: $0 <port>"
    echo "Example: $0 /dev/ttyUSB0"
    exit 1
fi

PORT=$1
echo "Using port: $PORT"
echo ""

# Build if needed
echo "Building firmware..."
platformio run
echo "✓ Build complete"
echo ""

# Flash firmware
echo "Flashing firmware to ESP32..."
echo "Make sure ESP32 is connected and in download mode if required"
platformio run -t upload --upload-port $PORT

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================="
    echo "  Flash Successful!"
    echo "========================================="
    echo ""
    echo "To monitor serial output:"
    echo "  platformio device monitor -p $PORT"
    echo ""
else
    echo ""
    echo "✗ Flash failed!"
    echo ""
    echo "Troubleshooting:"
    echo "  1. Check USB cable connection"
    echo "  2. Verify correct port ($PORT)"
    echo "  3. Try holding BOOT button during flash"
    echo "  4. Check device permissions: sudo usermod -a -G dialout \$USER"
    exit 1
fi
