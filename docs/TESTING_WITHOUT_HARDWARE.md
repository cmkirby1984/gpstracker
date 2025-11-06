# Testing Without Hardware

Complete guide to testing the Fleet GPS Tracker system before hardware arrives.

## Quick Start

Run all tests with one command:

```bash
./tools/run_all_tests.sh
```

Or run individual test suites:

```bash
# Python tests (no dependencies)
python tools/test_message_protocol.py

# Android tests
cd android && ./gradlew test

# ESP32 tests (requires PlatformIO)
cd firmware && platformio test
```

## Test Suites Overview

### 1. Python Message Protocol Tests

**Location**: `tools/test_message_protocol.py`
**Dependencies**: None (pure Python 3)
**Tests**: 15 comprehensive tests

**What it tests**:
- Message parsing and formatting
- Coordinate validation (equator, poles, date line)
- Invalid message rejection
- Checksum calculation
- Edge cases and error conditions

**Run**:
```bash
python3 tools/test_message_protocol.py
```

**Expected output**:
```
============================================================
Message Protocol Test Suite
============================================================

Basic Functionality Tests:
  ✓ Parse valid message
  ✓ Round trip format/parse

Edge Case Tests:
  ✓ Coordinates at equator
  ✓ North pole coordinates
  ✓ South pole coordinates
  ✓ International date line

Invalid Message Tests:
  ✓ Null and empty messages rejected
  ✓ Missing END marker rejected
  ✓ Wrong delimiter count rejected
  ✓ Invalid latitude rejected (> 90)
  ✓ Invalid longitude rejected (> 180)

Validation Tests:
  ✓ Coordinate validation

Checksum Tests:
  ✓ Checksum consistency

Real-World Scenario Tests:
  ✓ Multiple consecutive messages
  ✓ Different device serials

============================================================
Test Results: 15/15 passed
SUCCESS: All tests passed!
```

### 2. ESP32 Message Protocol Tests

**Location**: `firmware/test/test_message_protocol.cpp`
**Dependencies**: PlatformIO, Unity test framework
**Tests**: 25+ comprehensive tests

**What it tests**:
- Format and parse messages
- Round-trip encoding/decoding
- Boundary coordinates (poles, date line)
- Cardinal directions for heading
- Invalid coordinates (out of range)
- Null pointer handling
- Buffer overflow protection
- Checksum consistency

**Run**:
```bash
cd firmware
platformio test
```

**Expected output**:
```
test/test_message_protocol.cpp:XX:test_format_valid_message	[PASSED]
test/test_message_protocol.cpp:XX:test_parse_valid_message	[PASSED]
test/test_message_protocol.cpp:XX:test_round_trip_format_parse	[PASSED]
...
-----------------------
25 Tests 0 Failures 0 Ignored
OK
```

### 3. Android Message Parser Tests

**Location**: `android/app/src/test/java/com/fleet/gpstracker/MessageParserTest.java`
**Dependencies**: JUnit 4
**Tests**: 15 comprehensive tests

**What it tests**:
- Parse valid GPS messages
- Format GPS locations to messages
- Round-trip format/parse verification
- Boundary coordinate validation
- Cardinal direction headings
- Invalid message rejection
- Coordinate range validation
- Checksum calculation
- Real-world scenarios

**Run**:
```bash
cd android
./gradlew test
```

**Expected output**:
```
> Task :app:test

com.fleet.gpstracker.MessageParserTest > testParseValidMessage PASSED
com.fleet.gpstracker.MessageParserTest > testRoundTripFormatParse PASSED
com.fleet.gpstracker.MessageParserTest > testCoordinatesAtBoundaries PASSED
...

BUILD SUCCESSFUL in 5s
```

## Test Coverage

### Message Protocol Tests

| Category | Test Count | Coverage |
|----------|------------|----------|
| Valid messages | 3 | Format, parse, round-trip |
| Boundary coordinates | 5 | Equator, poles, date line |
| Heading values | 3 | Zero, max, cardinals |
| Invalid messages | 6 | Null, empty, malformed |
| Validation | 3 | Format, coordinates, checksums |
| Real-world | 2 | Multiple messages, devices |

**Total**: 20+ tests across all platforms

### Edge Cases Tested

**Coordinates**:
- ✓ Equator (0, 0)
- ✓ North Pole (90, 0)
- ✓ South Pole (-90, 0)
- ✓ International Date Line (0, ±180)
- ✓ High precision (6+ decimal places)

**Heading**:
- ✓ North (0°)
- ✓ East (90°)
- ✓ South (180°)
- ✓ West (270°)
- ✓ Maximum (359.99°)

**Device Serial**:
- ✓ Short ("G")
- ✓ Long ("GPS_TRACKER_001")
- ✓ Numeric ("12345")
- ✓ Alphanumeric ("GPS001")

**Invalid Inputs**:
- ✓ Null message
- ✓ Empty message
- ✓ Missing END marker
- ✓ Wrong delimiter count
- ✓ Too many fields
- ✓ Latitude > 90° or < -90°
- ✓ Longitude > 180° or < -180°
- ✓ Non-numeric coordinates
- ✓ Message too short
- ✓ Buffer overflow attempts

## Test Data Examples

### Valid Messages

```
GPS001|37.774900|-122.419400|270.50|2025-11-06T23:15:30Z|END
GPS002|0.000000|0.000000|0.00|2025-11-06T12:00:00Z|END
GPS003|90.000000|0.000000|180.00|2025-11-06T12:00:00Z|END
GPS004|-90.000000|0.000000|0.00|2025-11-06T12:00:00Z|END
GPS005|0.000000|180.000000|90.00|2025-11-06T12:00:00Z|END
```

### Invalid Messages

```
GPS001|37.7|-122.4|270|2025-11-06T12:00:00Z           # Missing END
GPS001|37.7|270|2025-11-06T12:00:00Z|END              # Missing field
GPS001|91.0|-122.4|270|2025-11-06T12:00:00Z|END       # Lat > 90
GPS001|37.7|181.0|270|2025-11-06T12:00:00Z|END        # Lon > 180
GPS001,37.7,-122.4,270,2025-11-06T12:00:00Z,END       # Wrong delimiter
```

## Running Specific Test Categories

### Test Only Parsing

```bash
# Python
python3 tools/test_message_protocol.py

# Android
cd android && ./gradlew test --tests "*MessageParserTest"

# ESP32
cd firmware && platformio test -f test_message_protocol
```

### Test with Verbose Output

```bash
# Python (already verbose)
python3 tools/test_message_protocol.py

# Android
cd android && ./gradlew test --info

# ESP32
cd firmware && platformio test -v
```

## Continuous Integration

Add to your CI pipeline:

```yaml
# .github/workflows/test.yml
name: Test Suite
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

      - name: Run Python tests
        run: python3 tools/test_message_protocol.py

      - name: Set up Java
        uses: actions/setup-java@v2
        with:
          java-version: '17'

      - name: Run Android tests
        run: cd android && ./gradlew test

      - name: Install PlatformIO
        run: pip install platformio

      - name: Run ESP32 tests
        run: cd firmware && platformio test
```

## Test Development

### Adding New Tests

**Python** (`tools/test_message_protocol.py`):
```python
# Add after existing tests
data = parse_message("YOUR_TEST_MESSAGE")
if data and YOUR_CONDITION:
    result.add_pass("Your test name")
else:
    result.add_fail("Your test name", "Error description")
```

**ESP32** (`firmware/test/test_message_protocol.cpp`):
```cpp
void test_your_new_test() {
    const char* message = "GPS001|...|END";
    GpsData data;
    bool result = MessageProtocol::parseMessage(message, data);
    TEST_ASSERT_TRUE(result);
    // Add more assertions
}

// Add to setup():
RUN_TEST(test_your_new_test);
```

**Android** (`android/app/src/test/java/com/fleet/gpstracker/MessageParserTest.java`):
```java
@Test
public void testYourNewTest() {
    String message = "GPS001|...|END";
    GpsLocation location = MessageParser.parseMessage(message);
    assertNotNull(location);
    // Add more assertions
}
```

## Troubleshooting Tests

### Python Tests Fail

```bash
# Check Python version (need 3.x)
python3 --version

# Run with more detail
python3 -v tools/test_message_protocol.py
```

### Android Tests Fail

```bash
# Clean and retry
cd android
./gradlew clean test

# Check Java version (need 17+)
java -version
```

### ESP32 Tests Fail

```bash
# Update PlatformIO
pip install --upgrade platformio

# Clean and retry
cd firmware
platformio run -t clean
platformio test
```

## Pre-Deployment Checklist

Before deploying to hardware:

- [ ] All Python tests pass
- [ ] All Android tests pass
- [ ] All ESP32 tests pass
- [ ] No compiler warnings
- [ ] Mock mode tested
- [ ] Message protocol validated
- [ ] Coordinate validation works
- [ ] Invalid input rejection works

## Performance Benchmarks

Expected test execution times:

| Test Suite | Time | Notes |
|------------|------|-------|
| Python | < 1s | Pure Python, no dependencies |
| Android | 5-10s | Includes Gradle build |
| ESP32 | 10-30s | Includes compilation |
| All tests | 30-60s | Via run_all_tests.sh |

## Next Steps

After all tests pass:

1. ✅ Message protocol validated
2. ✅ Ready for hardware integration
3. → Follow [FIRST_BOOT.md](FIRST_BOOT.md) when hardware arrives
4. → Run integration tests with real GPS
5. → Test Bluetooth communication
6. → Verify email sending

## Support

If tests fail unexpectedly:

1. Check test output for specific errors
2. Verify all dependencies installed
3. See [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
4. Review test code for recent changes
