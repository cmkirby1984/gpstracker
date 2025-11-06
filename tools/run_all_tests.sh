#!/bin/bash
# Unified Test Runner - Run ALL tests without hardware
# This script runs the complete test suite for the GPS tracking system

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "================================================================"
echo "  Fleet GPS Tracker - Complete Test Suite"
echo "  Running all tests without hardware"
echo "================================================================"
echo ""

TOTAL_PASSED=0
TOTAL_FAILED=0

# ============================================================================
# PYTHON PROTOCOL TESTS
# ============================================================================

echo "▶ Running Python Protocol Tests..."
echo "================================================================"

if python3 "$SCRIPT_DIR/test_message_protocol.py"; then
    echo "✓ Python tests PASSED"
    ((TOTAL_PASSED++))
else
    echo "✗ Python tests FAILED"
    ((TOTAL_FAILED++))
fi

echo ""

# ============================================================================
# ANDROID TESTS
# ============================================================================

echo "▶ Running Android JUnit Tests..."
echo "================================================================"

cd "$PROJECT_ROOT/android"

if [ -f "gradlew" ]; then
    if ./gradlew test --console=plain 2>&1 | grep -q "BUILD SUCCESSFUL"; then
        echo "✓ Android tests PASSED"
        ((TOTAL_PASSED++))
    else
        echo "✗ Android tests FAILED"
        ((TOTAL_FAILED++))
    fi
else
    echo "⚠ Gradle wrapper not found, skipping Android tests"
    echo "  (This is OK if you haven't set up Gradle yet)"
fi

cd "$PROJECT_ROOT"

echo ""

# ============================================================================
# ESP32 TESTS (if PlatformIO available)
# ============================================================================

echo "▶ Checking ESP32 PlatformIO Tests..."
echo "================================================================"

if command -v platformio &> /dev/null; then
    cd "$PROJECT_ROOT/firmware"

    echo "Running PlatformIO tests..."
    if platformio test 2>&1 | grep -q "PASSED"; then
        echo "✓ ESP32 tests PASSED"
        ((TOTAL_PASSED++))
    else
        echo "✗ ESP32 tests FAILED"
        ((TOTAL_FAILED++))
    fi

    cd "$PROJECT_ROOT"
else
    echo "⚠ PlatformIO not found, skipping ESP32 tests"
    echo "  Install with: pip install platformio"
    echo "  (This is OK if you're only testing message parsing)"
fi

echo ""

# ============================================================================
# SUMMARY
# ============================================================================

echo "================================================================"
echo "  Test Summary"
echo "================================================================"
echo ""
echo "Tests Passed: $TOTAL_PASSED"
echo "Tests Failed: $TOTAL_FAILED"
echo ""

if [ $TOTAL_FAILED -eq 0 ]; then
    echo "✓ SUCCESS: All available tests passed!"
    echo ""
    exit 0
else
    echo "✗ FAILURE: Some tests failed"
    echo ""
    exit 1
fi
