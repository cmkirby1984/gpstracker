#!/bin/bash
# Android app build script

set -e

echo "========================================="
echo "  Android App Build Script"
echo "========================================="
echo ""

# Check if Gradle wrapper exists
if [ ! -f "../gradlew" ]; then
    echo "ERROR: gradlew not found!"
    echo "Run this script from android/scripts directory"
    exit 1
fi

cd ..

# Clean
echo "Cleaning project..."
./gradlew clean
echo "✓ Clean complete"
echo ""

# Build
echo "Building app..."
./gradlew assembleDebug
if [ $? -eq 0 ]; then
    echo "✓ Build successful!"
else
    echo "✗ Build failed!"
    exit 1
fi
echo ""

# Run tests
echo "Running tests..."
./gradlew test
if [ $? -eq 0 ]; then
    echo "✓ Tests passed!"
else
    echo "✗ Tests failed!"
    exit 1
fi
echo ""

echo "========================================="
echo "  Build Complete!"
echo "========================================="
echo ""
echo "APK location: app/build/outputs/apk/debug/"
echo ""
echo "To install:"
echo "  ./gradlew installDebug"
echo ""
