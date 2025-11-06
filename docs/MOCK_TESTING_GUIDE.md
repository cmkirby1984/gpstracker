# Mock Testing Guide

Complete guide to testing the Fleet GPS Tracker system with mock data before hardware arrives.

## Overview

This guide shows how to verify your message parsing and validation logic works correctly **without any hardware**. You can test the complete flow from GPS message generation through parsing and validation.

## Table of Contents

- [Quick Start](#quick-start)
- [Available Tools](#available-tools)
- [Testing Workflows](#testing-workflows)
- [Mock Data Scenarios](#mock-data-scenarios)
- [Integration Testing](#integration-testing)
- [Android App Testing](#android-app-testing)
- [Troubleshooting](#troubleshooting)

## Quick Start

### 1. Run All Unit Tests

```bash
# Run complete test suite (Python, Android, ESP32)
./tools/run_all_tests.sh
```

### 2. Generate Mock GPS Messages

```bash
# Generate 10 moving vehicle messages
python3 tools/bluetooth_simulator.py --mode moving --count 10

# Test boundary conditions
python3 tools/bluetooth_simulator.py --mode boundary

# Generate invalid messages for error testing
python3 tools/bluetooth_simulator.py --mode invalid
```

### 3. Run Integration Tests

```bash
# Run complete integration test suite
python3 tools/integration_test.py

# Run with verbose output
python3 tools/integration_test.py --verbose

# Run specific scenario
python3 tools/integration_test.py --scenario moving
```

## Available Tools

### 1. Message Protocol Test Suite

**File**: `tools/test_message_protocol.py`

Standalone Python test suite with zero dependencies.

**Features**:
- 15 comprehensive tests
- Parse and format validation
- Coordinate boundary testing
- Invalid message rejection
- Checksum verification

**Usage**:
```bash
python3 tools/test_message_protocol.py
```

**Expected Output**:
```
============================================================
Message Protocol Test Suite
============================================================

Basic Functionality Tests:
   Parse valid message
   Round trip format/parse

Edge Case Tests:
   Coordinates at equator
   North pole coordinates
   South pole coordinates
   International date line

...

Test Results: 15/15 passed
SUCCESS: All tests passed!
```

### 2. Bluetooth Mock Data Simulator

**File**: `tools/bluetooth_simulator.py`

Generate realistic GPS messages simulating ESP32 behavior.

**Features**:
- Multiple simulation modes
- Realistic movement patterns
- Boundary condition testing
- Invalid message generation
- Output to file, socket, or Bluetooth

**Modes**:

| Mode | Description | Use Case |
|------|-------------|----------|
| `stationary` | Device not moving (GPS jitter only) | Parking lot testing |
| `moving` | Device moving north/east | Vehicle in motion |
| `circular` | Device following circular route | Route testing |
| `boundary` | Test boundary coordinates | Edge case validation |
| `invalid` | Generate invalid messages | Error handling |
| `stress` | Generate 100+ random messages | Load testing |

**Usage Examples**:

```bash
# Generate 10 moving messages and print to screen
python3 tools/bluetooth_simulator.py --mode moving --count 10

# Save 20 messages to file
python3 tools/bluetooth_simulator.py --mode moving --count 20 --output gps_data.txt

# Test all boundary conditions
python3 tools/bluetooth_simulator.py --mode boundary

# Generate invalid messages for error testing
python3 tools/bluetooth_simulator.py --mode invalid

# Stress test with 100 random messages
python3 tools/bluetooth_simulator.py --mode stress --count 100

# Send via TCP socket (requires listener)
python3 tools/bluetooth_simulator.py --mode moving --socket localhost:9999

# Send via Bluetooth (requires PyBluez)
python3 tools/bluetooth_simulator.py --mode moving --bluetooth AA:BB:CC:DD:EE:FF
```

### 3. Integration Test Suite

**File**: `tools/integration_test.py`

End-to-end testing of message protocol workflows.

**Tests**:
- Basic message flow (format ’ parse ’ validate)
- Boundary conditions
- Invalid message rejection
- Moving vehicle scenarios
- Multiple device tracking
- Checksum validation
- Stress testing (100+ messages)

**Usage**:

```bash
# Run all integration tests
python3 tools/integration_test.py

# Verbose mode for debugging
python3 tools/integration_test.py --verbose

# Run specific test scenarios
python3 tools/integration_test.py --scenario basic
python3 tools/integration_test.py --scenario boundary
python3 tools/integration_test.py --scenario moving
python3 tools/integration_test.py --scenario devices
python3 tools/integration_test.py --scenario checksum
python3 tools/integration_test.py --scenario stress
```

**Expected Output**:
```
======================================================================
  Fleet GPS Tracker - Integration Test Suite
======================================================================

¶ Testing Basic Message Flow
======================================================================
   Format message
   Parse message
   Latitude preserved
   Longitude preserved
   Device serial preserved

...

======================================================================
  Test Summary
======================================================================

  Total Tests:    45
  Passed:         45 (100.0%)
  Failed:         0

   SUCCESS: All integration tests passed!

  Your message parsing logic is working correctly.
  Ready for hardware integration!

======================================================================
```

### 4. Unified Test Runner

**File**: `tools/run_all_tests.sh`

Runs all test suites (Python, Android, ESP32) in one command.

**Usage**:
```bash
./tools/run_all_tests.sh
```

**What It Runs**:
1. Python message protocol tests (15 tests)
2. Android JUnit tests (15 tests)
3. ESP32 PlatformIO tests (25+ tests)

## Testing Workflows

### Workflow 1: Verify Message Protocol

Test that message parsing works correctly:

```bash
# Step 1: Run unit tests
python3 tools/test_message_protocol.py

# Step 2: Generate sample messages
python3 tools/bluetooth_simulator.py --mode moving --count 5 --output test_messages.txt

# Step 3: Verify messages are valid
cat test_messages.txt
```

**Sample Output**:
```
GPS001|37.774900|-122.419400|0.00|2025-11-06T12:00:00Z|END
GPS001|37.775800|-122.419400|0.00|2025-11-06T12:00:01Z|END
GPS001|37.775800|-122.418500|90.00|2025-11-06T12:00:02Z|END
...
```

### Workflow 2: Test Boundary Conditions

Verify edge cases are handled:

```bash
# Generate boundary test messages
python3 tools/bluetooth_simulator.py --mode boundary --output boundary_tests.txt

# Review messages
cat boundary_tests.txt

# Parse and validate with Python
python3 -c "
from tools.test_message_protocol import parse_message
with open('boundary_tests.txt') as f:
    for line in f:
        data = parse_message(line.strip())
        if data:
            print(f'Valid: {line.strip()}')
        else:
            print(f'Invalid: {line.strip()}')
"
```

### Workflow 3: Test Error Handling

Verify invalid messages are rejected:

```bash
# Generate invalid messages
python3 tools/bluetooth_simulator.py --mode invalid --output invalid_tests.txt

# All of these should be rejected
python3 -c "
from tools.test_message_protocol import parse_message
with open('invalid_tests.txt') as f:
    for line in f:
        data = parse_message(line.strip())
        if data is None:
            print(f' Correctly rejected: {line.strip()}')
        else:
            print(f' Should have rejected: {line.strip()}')
"
```

### Workflow 4: Simulate Moving Vehicle

Test realistic vehicle tracking:

```bash
# Generate moving vehicle path
python3 tools/bluetooth_simulator.py --mode circular --count 12 --output vehicle_path.txt

# Visualize the path (coordinates only)
cat vehicle_path.txt | cut -d'|' -f2,3
```

**Output**:
```
37.774900|-122.419400  (Starting point)
37.778402|-122.418527  (Northeast)
37.779691|-122.415031  (East)
37.777189|-122.411535  (Southeast)
37.771398|-122.410662  (South)
...
```

### Workflow 5: Integration Testing

Run complete end-to-end tests:

```bash
# Full integration test
python3 tools/integration_test.py

# Or run specific scenarios
python3 tools/integration_test.py --scenario moving
python3 tools/integration_test.py --scenario devices
python3 tools/integration_test.py --scenario stress
```

## Mock Data Scenarios

### Scenario 1: Stationary Device

**Use Case**: Truck parked overnight

```bash
python3 tools/bluetooth_simulator.py --mode stationary --count 20 --output stationary.txt
```

**Characteristics**:
- Same location (with GPS jitter)
- Realistic coordinate noise
- Tests duplicate handling

### Scenario 2: Moving Vehicle

**Use Case**: Truck on delivery route

```bash
python3 tools/bluetooth_simulator.py --mode moving --count 50 --output delivery_route.txt
```

**Characteristics**:
- Coordinates change over time
- Heading updates
- Tests tracking functionality

### Scenario 3: Circular Route

**Use Case**: Fleet vehicle patrolling area

```bash
python3 tools/bluetooth_simulator.py --mode circular --count 24 --output patrol.txt
```

**Characteristics**:
- Returns to starting point
- 360° heading coverage
- Tests route completion

### Scenario 4: Multiple Vehicles

**Use Case**: Fleet tracking

```bash
# Generate data for 5 trucks
for i in {1..5}; do
    python3 tools/bluetooth_simulator.py --mode moving --count 10 \
        --serial "TRUCK$i" --output "truck${i}_data.txt"
done

# Combine all data
cat truck*_data.txt > fleet_data.txt
```

### Scenario 5: Error Conditions

**Use Case**: Test error handling

```bash
python3 tools/bluetooth_simulator.py --mode invalid --output errors.txt
```

**Tests**:
- Missing END marker
- Invalid coordinates
- Malformed messages
- Non-numeric values

## Integration Testing

### Test Entire Message Flow

```bash
# Run complete integration test
python3 tools/integration_test.py --verbose
```

**What This Tests**:

1. **Basic Flow**
   - Generate ’ Format ’ Parse ’ Validate
   - Round-trip accuracy
   - Data preservation

2. **Boundary Conditions**
   - Equator, poles, date line
   - Valid range enforcement
   - Invalid coordinate rejection

3. **Invalid Messages**
   - Null/empty handling
   - Format validation
   - Error recovery

4. **Real-World Scenarios**
   - Moving vehicles
   - Multiple devices
   - Checksums
   - Stress testing

### Continuous Integration

Add to your CI/CD pipeline:

```yaml
# .github/workflows/test.yml
name: Mock Testing

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2

      - name: Set up Python
        uses: actions/setup-python@v2
        with:
          python-version: '3.x'

      - name: Run message protocol tests
        run: python3 tools/test_message_protocol.py

      - name: Run integration tests
        run: python3 tools/integration_test.py

      - name: Test boundary conditions
        run: |
          python3 tools/bluetooth_simulator.py --mode boundary --output boundary.txt
          test -f boundary.txt

      - name: Test invalid message rejection
        run: |
          python3 tools/bluetooth_simulator.py --mode invalid --output invalid.txt
          test -f invalid.txt
```

## Android App Testing

### Manual Testing with Mock Data

1. **Generate test messages**:
```bash
python3 tools/bluetooth_simulator.py --mode moving --count 20 --output android_test.txt
```

2. **Method 1: Use ADB to push file**:
```bash
adb push android_test.txt /sdcard/Download/
```

3. **Method 2: Run TCP server** (if app supports socket):
```bash
# Terminal 1: Start listener
nc -l 9999

# Terminal 2: Send data
python3 tools/bluetooth_simulator.py --mode moving --count 10 --socket localhost:9999
```

4. **Method 3: Bluetooth simulation** (Linux with PyBluez):
```bash
# Install PyBluez
pip install pybluez

# Find Android device MAC
hcitool scan

# Send data
python3 tools/bluetooth_simulator.py --mode moving --bluetooth AA:BB:CC:DD:EE:FF
```

### Android Unit Tests

Run Android message parser tests:

```bash
cd android
./gradlew test

# View results
cat app/build/reports/tests/testDebugUnitTest/index.html
```

## Troubleshooting

### Issue: Tests Fail to Run

**Symptom**: `python3: command not found` or `ModuleNotFoundError`

**Solution**:
```bash
# Check Python version
python3 --version

# Ensure Python 3.x installed
# Linux/Mac: sudo apt install python3
# Or: brew install python3
```

### Issue: Bluetooth Simulator Can't Connect

**Symptom**: `PyBluez not available`

**Solution**:
```bash
# PyBluez is optional - use file or socket mode instead
python3 tools/bluetooth_simulator.py --mode moving --output messages.txt

# Or install PyBluez (Linux):
sudo apt-get install python3-bluez
pip install pybluez
```

### Issue: Messages Don't Parse

**Symptom**: `parse_message()` returns `None`

**Debug**:
```bash
# Check message format
python3 -c "
from tools.test_message_protocol import parse_message
msg = 'GPS001|37.7749|-122.4194|270.5|2025-11-06T12:00:00Z|END'
print('Message:', msg)
data = parse_message(msg)
print('Parsed:', data)
"
```

**Common Issues**:
- Missing `|END` terminator
- Wrong number of `|` delimiters (need exactly 5)
- Coordinates out of range
- Message too short (< 20 characters)

### Issue: Integration Tests Fail

**Symptom**: Some integration tests fail

**Debug**:
```bash
# Run with verbose output
python3 tools/integration_test.py --verbose

# Run specific failing test
python3 tools/integration_test.py --scenario boundary
```

### Issue: Android Tests Don't Run

**Symptom**: `./gradlew test` fails

**Solution**:
```bash
cd android

# Clean and retry
./gradlew clean test

# Check Java version (need 17+)
java -version

# If wrong version, install JDK 17
```

### Issue: ESP32 Tests Don't Compile

**Symptom**: PlatformIO errors

**Solution**:
```bash
cd firmware

# Update PlatformIO
pip install --upgrade platformio

# Clean and rebuild
platformio run -t clean
platformio test
```

## Performance Benchmarks

Expected test execution times:

| Test Suite | Time | Notes |
|------------|------|-------|
| Python message tests | < 1s | Pure Python, instant |
| Integration tests | 1-2s | Includes stress test |
| Bluetooth simulator | 1-10s | Depends on count/interval |
| Android unit tests | 5-10s | Includes Gradle build |
| ESP32 tests | 10-30s | Includes compilation |
| All tests | 30-60s | Complete suite |

## Next Steps

After all mock tests pass:

1.  **Message protocol validated** - Ready for hardware
2.  **Parsing logic verified** - Ready for integration
3. ’ **Hardware arrives** - Flash firmware
4. ’ **Connect devices** - Pair Bluetooth
5. ’ **Integration test** - ESP32 ’ Android
6. ’ **Deploy** - Production ready

See [FIRST_BOOT.md](FIRST_BOOT.md) for hardware setup guide.

## Summary

You now have complete mock testing infrastructure:

 **Unit Tests**: Verify parsing and formatting
 **Mock Data Generator**: Create realistic GPS messages
 **Integration Tests**: End-to-end workflow validation
 **Boundary Testing**: Edge case coverage
 **Error Testing**: Invalid input handling
 **Stress Testing**: Load and performance validation

**Everything works without hardware!**

Run this to verify:
```bash
./tools/run_all_tests.sh && python3 tools/integration_test.py
```

If all tests pass, your system is ready for hardware integration.
