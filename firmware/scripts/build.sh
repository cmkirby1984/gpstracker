#!/bin/bash
# Build verification script for ESP32 firmware

set -e  # Exit on error

echo "========================================="
echo "  ESP32 Firmware Build Script"
echo "========================================="
echo ""

# Check if platformio is installed
if ! command -v platformio &> /dev/null; then
    echo "ERROR: PlatformIO not found!"
    echo "Install with: pip install platformio"
    exit 1
fi

echo "✓ PlatformIO found"
echo ""

# Clean previous build
echo "Cleaning previous build..."
platformio run -t clean
echo "✓ Clean complete"
echo ""

# Build firmware
echo "Building firmware..."
platformio run
if [ $? -eq 0 ]; then
    echo "✓ Build successful!"
else
    echo "✗ Build failed!"
    exit 1
fi
echo ""

# Run tests
echo "Running unit tests..."
platformio test
if [ $? -eq 0 ]; then
    echo "✓ Tests passed!"
else
    echo "✗ Tests failed!"
    exit 1
fi
echo ""

# Check code size
echo "Firmware size:"
platformio run -t size
echo ""

echo "========================================="
echo "  Build Complete!"
echo "========================================="
echo ""
echo "To flash to device:"
echo "  1. Connect ESP32 via USB"
echo "  2. Update upload_port in platformio.ini"
echo "  3. Run: platformio run -t upload"
echo ""
