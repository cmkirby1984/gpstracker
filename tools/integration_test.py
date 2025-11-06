#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Integration Test Suite
Tests the complete message parsing flow with mock data

This script validates the entire message protocol workflow:
1. Generate mock GPS messages
2. Parse messages with validation
3. Verify coordinate handling
4. Test error conditions
5. Simulate real-world scenarios

Usage:
    python3 integration_test.py
    python3 integration_test.py --verbose
    python3 integration_test.py --scenario all
"""

import sys
import argparse
from datetime import datetime

# Import test message protocol implementation
sys.path.append('tools')
from test_message_protocol import parse_message, format_message, validate_coordinates, calculate_checksum

class IntegrationTest:
    """Integration test runner"""

    def __init__(self, verbose=False):
        self.verbose = verbose
        self.passed = 0
        self.failed = 0
        self.tests = []

    def log(self, message):
        """Log if verbose mode enabled"""
        if self.verbose:
            print(f"  [DEBUG] {message}")

    def test(self, name, condition, error_msg=""):
        """Record test result"""
        if condition:
            self.passed += 1
            print(f"   {name}")
            self.log("PASSED")
        else:
            self.failed += 1
            print(f"   {name}")
            if error_msg:
                print(f"    Error: {error_msg}")
            self.log(f"FAILED: {error_msg}")

    def test_basic_message_flow(self):
        """Test basic message generation and parsing"""
        print("\n� Testing Basic Message Flow")
        print("="*70)

        # Generate a message
        original_data = {
            'device_serial': 'GPS001',
            'latitude': 37.7749,
            'longitude': -122.4194,
            'heading': 270.5,
            'timestamp': '2025-11-06T12:00:00Z'
        }

        self.log(f"Original data: {original_data}")

        # Format message
        message = format_message(original_data)
        self.test("Format message", message is not None, "Failed to format message")
        self.log(f"Formatted: {message}")

        # Parse message back
        parsed_data = parse_message(message)
        self.test("Parse message", parsed_data is not None, "Failed to parse message")
        self.log(f"Parsed data: {parsed_data}")

        # Verify round-trip accuracy
        if parsed_data:
            lat_match = abs(parsed_data['latitude'] - original_data['latitude']) < 0.000001
            self.test("Latitude preserved", lat_match,
                     f"Expected {original_data['latitude']}, got {parsed_data['latitude']}")

            lon_match = abs(parsed_data['longitude'] - original_data['longitude']) < 0.000001
            self.test("Longitude preserved", lon_match,
                     f"Expected {original_data['longitude']}, got {parsed_data['longitude']}")

            serial_match = parsed_data['device_serial'] == original_data['device_serial']
            self.test("Device serial preserved", serial_match,
                     f"Expected {original_data['device_serial']}, got {parsed_data['device_serial']}")

    def test_boundary_conditions(self):
        """Test coordinate boundary conditions"""
        print("\n� Testing Boundary Conditions")
        print("="*70)

        test_cases = [
            ("Equator", 0.0, 0.0, True),
            ("North Pole", 90.0, 0.0, True),
            ("South Pole", -90.0, 0.0, True),
            ("Date Line East", 0.0, 180.0, True),
            ("Date Line West", 0.0, -180.0, True),
            ("Beyond North Pole", 91.0, 0.0, False),
            ("Beyond South Pole", -91.0, 0.0, False),
            ("Beyond Date Line East", 0.0, 181.0, False),
            ("Beyond Date Line West", 0.0, -181.0, False),
        ]

        for name, lat, lon, should_be_valid in test_cases:
            is_valid = validate_coordinates(lat, lon)
            expected = "valid" if should_be_valid else "invalid"
            actual = "valid" if is_valid else "invalid"

            self.test(f"{name} ({lat}, {lon}) is {expected}",
                     is_valid == should_be_valid,
                     f"Expected {expected}, got {actual}")

    def test_invalid_messages(self):
        """Test invalid message rejection"""
        print("\n� Testing Invalid Message Rejection")
        print("="*70)

        invalid_messages = [
            ("Null message", None),
            ("Empty message", ""),
            ("Missing END", "GPS001|37.7749|-122.4194|270.5|2025-11-06T12:00:00Z"),
            ("Wrong delimiter count", "GPS001|37.7749|270.5|2025-11-06T12:00:00Z|END"),
            ("Invalid latitude", "GPS001|91.0|-122.4194|270.5|2025-11-06T12:00:00Z|END"),
            ("Invalid longitude", "GPS001|37.7749|181.0|270.5|2025-11-06T12:00:00Z|END"),
            ("Non-numeric lat", "GPS001|abc|-122.4194|270.5|2025-11-06T12:00:00Z|END"),
            ("Too short", "GPS|1|2|3|4|END"),
        ]

        for name, invalid_msg in invalid_messages:
            result = parse_message(invalid_msg)
            self.test(f"{name} rejected", result is None,
                     f"Should reject but got: {result}")

    def test_moving_vehicle_scenario(self):
        """Test realistic moving vehicle scenario"""
        print("\n� Testing Moving Vehicle Scenario")
        print("="*70)

        # Simulate a vehicle moving north then east
        positions = [
            (37.7749, -122.4194, 0.0, "Starting position"),
            (37.7759, -122.4194, 0.0, "Moved north"),
            (37.7759, -122.4184, 90.0, "Turned east"),
            (37.7759, -122.4174, 90.0, "Continued east"),
        ]

        messages = []
        for lat, lon, heading, desc in positions:
            data = {
                'device_serial': 'TRUCK001',
                'latitude': lat,
                'longitude': lon,
                'heading': heading,
                'timestamp': datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")
            }

            message = format_message(data)
            self.test(f"{desc} message generated", message is not None)

            parsed = parse_message(message)
            self.test(f"{desc} message parsed", parsed is not None)

            if parsed:
                messages.append(parsed)
                self.log(f"Position: ({parsed['latitude']:.6f}, {parsed['longitude']:.6f}) heading {parsed['heading']}�")

        # Verify we got all messages
        self.test("All positions tracked", len(messages) == len(positions),
                 f"Expected {len(positions)} messages, got {len(messages)}")

    def test_multiple_devices(self):
        """Test multiple devices sending data"""
        print("\n� Testing Multiple Devices")
        print("="*70)

        devices = ['GPS001', 'GPS002', 'TRUCK1', 'TRUCK2', 'FLEET_A']

        device_messages = {}
        for device_serial in devices:
            data = {
                'device_serial': device_serial,
                'latitude': 37.7749,
                'longitude': -122.4194,
                'heading': 180.0,
                'timestamp': '2025-11-06T12:00:00Z'
            }

            message = format_message(data)
            parsed = parse_message(message)

            self.test(f"Device {device_serial} message valid", parsed is not None)

            if parsed:
                device_messages[device_serial] = parsed
                self.log(f"Device {device_serial}: {message}")

        # Verify all devices tracked separately
        self.test("All devices tracked", len(device_messages) == len(devices),
                 f"Expected {len(devices)} devices, got {len(device_messages)}")

        # Verify device serials preserved
        for device_serial in devices:
            if device_serial in device_messages:
                matches = device_messages[device_serial]['device_serial'] == device_serial
                self.test(f"Device {device_serial} serial preserved", matches)

    def test_checksum_validation(self):
        """Test checksum calculations"""
        print("\n� Testing Checksum Validation")
        print("="*70)

        message = "GPS001|37.774900|-122.419400|270.50|2025-11-06T12:00:00Z|END"

        # Calculate checksum twice
        cs1 = calculate_checksum(message)
        cs2 = calculate_checksum(message)

        self.test("Checksum consistent", cs1 == cs2,
                 f"Got {cs1} and {cs2}")

        # Verify different messages have different checksums
        message2 = "GPS002|37.774900|-122.419400|270.50|2025-11-06T12:00:00Z|END"
        cs3 = calculate_checksum(message2)

        self.test("Different messages have different checksums", cs1 != cs3,
                 f"Both got {cs1}")

        self.log(f"Message 1 checksum: {cs1}")
        self.log(f"Message 2 checksum: {cs3}")

    def test_stress_conditions(self):
        """Test stress conditions"""
        print("\n� Testing Stress Conditions")
        print("="*70)

        # Generate many messages
        message_count = 100
        messages = []

        import random
        for i in range(message_count):
            data = {
                'device_serial': f'GPS{i:03d}',
                'latitude': random.uniform(-90, 90),
                'longitude': random.uniform(-180, 180),
                'heading': random.uniform(0, 360),
                'timestamp': datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")
            }

            message = format_message(data)
            if message:
                parsed = parse_message(message)
                if parsed:
                    messages.append(parsed)

        success_rate = (len(messages) / message_count) * 100
        self.test(f"Parsed {message_count} messages", len(messages) == message_count,
                 f"Only parsed {len(messages)}/{message_count} ({success_rate:.1f}%)")

        self.log(f"Successfully processed {len(messages)}/{message_count} messages")

    def run_all_tests(self):
        """Run all integration tests"""
        print("\n" + "="*70)
        print("  Fleet GPS Tracker - Integration Test Suite")
        print("="*70)

        self.test_basic_message_flow()
        self.test_boundary_conditions()
        self.test_invalid_messages()
        self.test_moving_vehicle_scenario()
        self.test_multiple_devices()
        self.test_checksum_validation()
        self.test_stress_conditions()

        self.print_summary()

    def print_summary(self):
        """Print test summary"""
        total = self.passed + self.failed
        success_rate = (self.passed / total * 100) if total > 0 else 0

        print("\n" + "="*70)
        print("  Test Summary")
        print("="*70)
        print(f"\n  Total Tests:    {total}")
        print(f"  Passed:         {self.passed} ({success_rate:.1f}%)")
        print(f"  Failed:         {self.failed}")

        if self.failed == 0:
            print("\n   SUCCESS: All integration tests passed!")
            print("\n  Your message parsing logic is working correctly.")
            print("  Ready for hardware integration!")
        else:
            print("\n   FAILURE: Some tests failed")
            print("\n  Review the failures above and fix the issues.")

        print("\n" + "="*70 + "\n")

        return self.failed == 0

def main():
    parser = argparse.ArgumentParser(
        description="Integration test suite for GPS message protocol",
        formatter_class=argparse.RawDescriptionHelpFormatter
    )

    parser.add_argument('--verbose', '-v', action='store_true',
                       help='Enable verbose output')
    parser.add_argument('--scenario', choices=['basic', 'boundary', 'invalid', 'moving',
                                               'devices', 'checksum', 'stress', 'all'],
                       default='all', help='Run specific test scenario')

    args = parser.parse_args()

    tester = IntegrationTest(verbose=args.verbose)

    print(f"\nRunning integration tests (scenario: {args.scenario})...")

    if args.scenario == 'all':
        success = tester.run_all_tests()
    else:
        print("\n" + "="*70)
        print(f"  Running {args.scenario} tests only")
        print("="*70)

        if args.scenario == 'basic':
            tester.test_basic_message_flow()
        elif args.scenario == 'boundary':
            tester.test_boundary_conditions()
        elif args.scenario == 'invalid':
            tester.test_invalid_messages()
        elif args.scenario == 'moving':
            tester.test_moving_vehicle_scenario()
        elif args.scenario == 'devices':
            tester.test_multiple_devices()
        elif args.scenario == 'checksum':
            tester.test_checksum_validation()
        elif args.scenario == 'stress':
            tester.test_stress_conditions()

        tester.print_summary()
        success = tester.failed == 0

    return 0 if success else 1

if __name__ == "__main__":
    sys.exit(main())
