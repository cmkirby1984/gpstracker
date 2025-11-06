#!/usr/bin/env python3
"""
Standalone Message Protocol Test Suite
Tests the GPS message protocol without any hardware or dependencies

Run with: python tools/test_message_protocol.py
"""

import sys

# Message protocol implementation (reference)
def parse_message(message):
    """Parse GPS message and return dict or None"""
    if not message or len(message) < 20:
        return None

    if not message.endswith('|END'):
        return None

    parts = message.split('|')
    if len(parts) != 6:
        return None

    try:
        device_serial = parts[0]
        latitude = float(parts[1])
        longitude = float(parts[2])
        heading = float(parts[3])
        timestamp = parts[4]
        end_marker = parts[5]

        if end_marker != 'END':
            return None

        if not validate_coordinates(latitude, longitude):
            return None

        return {
            'device_serial': device_serial,
            'latitude': latitude,
            'longitude': longitude,
            'heading': heading,
            'timestamp': timestamp,
            'valid': True
        }
    except (ValueError, IndexError):
        return None

def format_message(data):
    """Format GPS data into message"""
    if not validate_coordinates(data['latitude'], data['longitude']):
        return None

    return f"{data['device_serial']}|{data['latitude']:.6f}|{data['longitude']:.6f}|{data['heading']:.2f}|{data['timestamp']}|END"

def validate_coordinates(lat, lon):
    """Validate coordinate ranges"""
    return -90.0 <= lat <= 90.0 and -180.0 <= lon <= 180.0

def calculate_checksum(message):
    """Calculate XOR checksum"""
    checksum = 0
    for char in message:
        checksum ^= ord(char)
    return checksum

# Test suite
class TestResult:
    def __init__(self):
        self.passed = 0
        self.failed = 0
        self.tests = []

    def add_pass(self, name):
        self.passed += 1
        self.tests.append((name, True, None))
        print(f"  ✓ {name}")

    def add_fail(self, name, error):
        self.failed += 1
        self.tests.append((name, False, error))
        print(f"  ✗ {name}: {error}")

    def summary(self):
        total = self.passed + self.failed
        print(f"\n{'='*60}")
        print(f"Test Results: {self.passed}/{total} passed")
        if self.failed > 0:
            print(f"FAILED: {self.failed} tests failed")
            return False
        else:
            print("SUCCESS: All tests passed!")
            return True

def run_tests():
    result = TestResult()

    print("="*60)
    print("Message Protocol Test Suite")
    print("="*60)
    print()

    # BASIC FUNCTIONALITY
    print("Basic Functionality Tests:")

    # Test 1: Parse valid message
    msg = "GPS001|37.774900|-122.419400|270.50|2025-11-06T23:15:30Z|END"
    data = parse_message(msg)
    if data and data['device_serial'] == 'GPS001' and abs(data['latitude'] - 37.7749) < 0.0001:
        result.add_pass("Parse valid message")
    else:
        result.add_fail("Parse valid message", f"Got {data}")

    # Test 2: Round trip
    original = {'device_serial': 'GPS999', 'latitude': 51.5074, 'longitude': -0.1278,
                'heading': 180.0, 'timestamp': '2025-11-06T12:00:00Z'}
    formatted = format_message(original)
    parsed = parse_message(formatted)
    if parsed and abs(parsed['latitude'] - original['latitude']) < 0.0001:
        result.add_pass("Round trip format/parse")
    else:
        result.add_fail("Round trip format/parse", f"Got {parsed}")

    print()

    # EDGE CASES
    print("Edge Case Tests:")

    # Test 3: Equator
    data = parse_message("GPS002|0.000000|0.000000|0.00|2025-11-06T12:00:00Z|END")
    if data and data['latitude'] == 0.0 and data['longitude'] == 0.0:
        result.add_pass("Coordinates at equator")
    else:
        result.add_fail("Coordinates at equator", f"Got {data}")

    # Test 4: North pole
    data = parse_message("GPS003|90.000000|0.000000|180.00|2025-11-06T12:00:00Z|END")
    if data and abs(data['latitude'] - 90.0) < 0.0001:
        result.add_pass("North pole coordinates")
    else:
        result.add_fail("North pole coordinates", f"Got {data}")

    # Test 5: South pole
    data = parse_message("GPS004|-90.000000|0.000000|0.00|2025-11-06T12:00:00Z|END")
    if data and abs(data['latitude'] + 90.0) < 0.0001:
        result.add_pass("South pole coordinates")
    else:
        result.add_fail("South pole coordinates", f"Got {data}")

    # Test 6: Date line
    data = parse_message("GPS005|0.000000|180.000000|90.00|2025-11-06T12:00:00Z|END")
    if data and abs(data['longitude'] - 180.0) < 0.0001:
        result.add_pass("International date line")
    else:
        result.add_fail("International date line", f"Got {data}")

    print()

    # INVALID MESSAGES
    print("Invalid Message Tests:")

    # Test 7: Null/empty
    if parse_message(None) is None and parse_message("") is None:
        result.add_pass("Null and empty messages rejected")
    else:
        result.add_fail("Null and empty messages rejected", "Should return None")

    # Test 8: Missing END marker
    data = parse_message("GPS001|37.7749|-122.4194|270.5|2025-11-06T12:00:00Z")
    if data is None:
        result.add_pass("Missing END marker rejected")
    else:
        result.add_fail("Missing END marker rejected", f"Got {data}")

    # Test 9: Wrong delimiter count
    data = parse_message("GPS001|37.7749|270.5|2025-11-06T12:00:00Z|END")
    if data is None:
        result.add_pass("Wrong delimiter count rejected")
    else:
        result.add_fail("Wrong delimiter count rejected", f"Got {data}")

    # Test 10: Invalid latitude
    data = parse_message("GPS001|91.000000|-122.4194|270.5|2025-11-06T12:00:00Z|END")
    if data is None:
        result.add_pass("Invalid latitude rejected (> 90)")
    else:
        result.add_fail("Invalid latitude rejected", f"Got {data}")

    # Test 11: Invalid longitude
    data = parse_message("GPS001|37.7749|181.000000|270.5|2025-11-06T12:00:00Z|END")
    if data is None:
        result.add_pass("Invalid longitude rejected (> 180)")
    else:
        result.add_fail("Invalid longitude rejected", f"Got {data}")

    print()

    # VALIDATION
    print("Validation Tests:")

    # Test 12: Coordinate validation
    valid_tests = [
        (0.0, 0.0), (90.0, 180.0), (-90.0, -180.0), (37.7749, -122.4194)
    ]
    invalid_tests = [
        (91.0, 0.0), (-91.0, 0.0), (0.0, 181.0), (0.0, -181.0)
    ]

    all_valid = all(validate_coordinates(lat, lon) for lat, lon in valid_tests)
    all_invalid = not any(validate_coordinates(lat, lon) for lat, lon in invalid_tests)

    if all_valid and all_invalid:
        result.add_pass("Coordinate validation")
    else:
        result.add_fail("Coordinate validation", "Some validations incorrect")

    print()

    # CHECKSUMS
    print("Checksum Tests:")

    # Test 13: Checksum consistency
    msg1 = "GPS001|37.774900|-122.419400|270.50|2025-11-06T23:15:30Z|END"
    msg2 = "GPS001|37.774900|-122.419400|270.50|2025-11-06T23:15:30Z|END"
    msg3 = "GPS002|37.774900|-122.419400|270.50|2025-11-06T23:15:30Z|END"

    cs1 = calculate_checksum(msg1)
    cs2 = calculate_checksum(msg2)
    cs3 = calculate_checksum(msg3)

    if cs1 == cs2 and cs1 != cs3:
        result.add_pass("Checksum consistency")
    else:
        result.add_fail("Checksum consistency", f"cs1={cs1}, cs2={cs2}, cs3={cs3}")

    print()

    # REAL-WORLD SCENARIOS
    print("Real-World Scenario Tests:")

    # Test 14: Multiple consecutive messages
    messages = [
        "GPS001|37.774900|-122.419400|270.50|2025-11-06T12:00:00Z|END",
        "GPS001|37.774901|-122.419401|270.51|2025-11-06T12:05:00Z|END",
        "GPS001|37.774902|-122.419402|270.52|2025-11-06T12:10:00Z|END"
    ]

    all_parsed = all(parse_message(msg) is not None for msg in messages)
    if all_parsed:
        result.add_pass("Multiple consecutive messages")
    else:
        result.add_fail("Multiple consecutive messages", "Some failed to parse")

    # Test 15: Different device serials
    devices = ['GPS001', 'GPS002', 'TRUCK1']
    messages = [
        f"{dev}|37.7749|-122.4194|270.5|2025-11-06T12:00:00Z|END"
        for dev in devices
    ]

    parsed_devices = [parse_message(msg)['device_serial'] for msg in messages]
    if parsed_devices == devices:
        result.add_pass("Different device serials")
    else:
        result.add_fail("Different device serials", f"Got {parsed_devices}")

    print()

    return result.summary()

if __name__ == "__main__":
    success = run_tests()
    sys.exit(0 if success else 1)
