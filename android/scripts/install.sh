#!/bin/bash
# Android app installation script

set -e

echo "========================================="
echo "  Android App Installation Script"
echo "========================================="
echo ""

cd ..

# Check for connected devices
echo "Checking for connected devices..."
adb devices

echo ""
echo "Installing debug APK..."
./gradlew installDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================="
    echo "  Installation Successful!"
    echo "========================================="
    echo ""
    echo "App installed on device"
    echo "Launch from app drawer: Fleet GPS Tracker"
    echo ""
else
    echo ""
    echo "✗ Installation failed!"
    echo ""
    echo "Troubleshooting:"
    echo "  1. Check device is connected: adb devices"
    echo "  2. Enable USB debugging on device"
    echo "  3. Accept USB debugging prompt on device"
    exit 1
fi
