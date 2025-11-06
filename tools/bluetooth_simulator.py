#!/usr/bin/env python3
"""
Bluetooth Mock Data Simulator
Simulates ESP32 GPS tracker sending data over Bluetooth to Android app

This tool can:
1. Generate realistic GPS messages
2. Simulate different movement patterns
3. Test edge cases and error conditions
4. Send data via Bluetooth Serial (if available) or file/socket

Usage:
    python3 bluetooth_simulator.py --help
    python3 bluetooth_simulator.py --mode moving --count 10
    python3 bluetooth_simulator.py --mode boundary --output messages.txt
"""

import sys
import time
import random
import argparse
from datetime import datetime, timedelta
import math

# Try to import Bluetooth library (optional)
try:
    import bluetooth
    BLUETOOTH_AVAILABLE = True
except ImportError:
    BLUETOOTH_AVAILABLE = False
    print("Note: PyBluez not installed. Using file/socket output mode.")
    print("      Install with: pip install pybluez")

# Message protocol implementation
MESSAGE_FORMAT = "{serial}|{lat:.6f}|{lon:.6f}|{heading:.2f}|{timestamp}|END"

class GpsSimulator:
    """Simulates GPS data with various patterns"""

    def __init__(self, device_serial="GPS001"):
        self.device_serial = device_serial
        self.latitude = 37.7749  # San Francisco default
        self.longitude = -122.4194
        self.heading = 0.0
        self.timestamp = datetime.utcnow()

    def generate_message(self, lat=None, lon=None, heading=None, timestamp=None):
        """Generate a GPS message"""
        if lat is None:
            lat = self.latitude
        if lon is None:
            lon = self.longitude
        if heading is None:
            heading = self.heading
        if timestamp is None:
            timestamp = self.timestamp.strftime("%Y-%m-%dT%H:%M:%SZ")

        message = MESSAGE_FORMAT.format(
            serial=self.device_serial,
            lat=lat,
            lon=lon,
            heading=heading,
            timestamp=timestamp
        )
        return message

    def move_north(self, distance_km=0.1):
        """Move north by specified distance"""
        # Approximately 111 km per degree latitude
        self.latitude += distance_km / 111.0
        self.heading = 0.0
        self.timestamp = datetime.utcnow()

    def move_south(self, distance_km=0.1):
        """Move south by specified distance"""
        self.latitude -= distance_km / 111.0
        self.heading = 180.0
        self.timestamp = datetime.utcnow()

    def move_east(self, distance_km=0.1):
        """Move east by specified distance"""
        # Adjust for latitude (longitude degrees vary by latitude)
        lat_rad = math.radians(self.latitude)
        km_per_degree = 111.0 * math.cos(lat_rad)
        self.longitude += distance_km / km_per_degree
        self.heading = 90.0
        self.timestamp = datetime.utcnow()

    def move_west(self, distance_km=0.1):
        """Move west by specified distance"""
        lat_rad = math.radians(self.latitude)
        km_per_degree = 111.0 * math.cos(lat_rad)
        self.longitude -= distance_km / km_per_degree
        self.heading = 270.0
        self.timestamp = datetime.utcnow()

    def move_random(self, max_distance_km=0.5):
        """Random movement"""
        angle = random.uniform(0, 360)
        distance = random.uniform(0, max_distance_km)

        # Convert to lat/lon change
        lat_rad = math.radians(self.latitude)
        lat_change = (distance * math.cos(math.radians(angle))) / 111.0
        lon_change = (distance * math.sin(math.radians(angle))) / (111.0 * math.cos(lat_rad))

        self.latitude += lat_change
        self.longitude += lon_change
        self.heading = angle
        self.timestamp = datetime.utcnow()

    def set_position(self, lat, lon, heading=0.0):
        """Set specific position"""
        self.latitude = lat
        self.longitude = lon
        self.heading = heading
        self.timestamp = datetime.utcnow()

class MockDataGenerator:
    """Generate mock GPS data for various test scenarios"""

    @staticmethod
    def generate_stationary(serial="GPS001", count=10, interval_seconds=5):
        """Generate messages from a stationary device"""
        sim = GpsSimulator(serial)
        messages = []

        for i in range(count):
            # Add small random noise to simulate GPS jitter
            lat = sim.latitude + random.uniform(-0.00001, 0.00001)
            lon = sim.longitude + random.uniform(-0.00001, 0.00001)
            messages.append(sim.generate_message(lat=lat, lon=lon))
            time.sleep(interval_seconds if interval_seconds > 0 else 0)

        return messages

    @staticmethod
    def generate_moving(serial="GPS001", count=10, interval_seconds=5):
        """Generate messages from a moving device"""
        sim = GpsSimulator(serial)
        messages = []

        for i in range(count):
            # Alternate between moving north and east
            if i % 2 == 0:
                sim.move_north(0.1)
            else:
                sim.move_east(0.1)

            messages.append(sim.generate_message())
            time.sleep(interval_seconds if interval_seconds > 0 else 0)

        return messages

    @staticmethod
    def generate_circular_route(serial="GPS001", count=12):
        """Generate messages following a circular route"""
        sim = GpsSimulator(serial)
        messages = []

        center_lat = sim.latitude
        center_lon = sim.longitude
        radius_km = 0.5

        for i in range(count):
            angle = (360.0 / count) * i
            # Calculate position on circle
            lat_offset = (radius_km * math.cos(math.radians(angle))) / 111.0
            lon_offset = (radius_km * math.sin(math.radians(angle))) / (111.0 * math.cos(math.radians(center_lat)))

            sim.set_position(
                center_lat + lat_offset,
                center_lon + lon_offset,
                angle
            )

            messages.append(sim.generate_message())

        return messages

    @staticmethod
    def generate_boundary_tests(serial="GPS001"):
        """Generate messages testing boundary conditions"""
        sim = GpsSimulator(serial)
        messages = []

        # Test cases: (lat, lon, heading, description)
        test_cases = [
            (0.0, 0.0, 0.0, "Equator"),
            (90.0, 0.0, 180.0, "North Pole"),
            (-90.0, 0.0, 0.0, "South Pole"),
            (0.0, 180.0, 90.0, "Date Line East"),
            (0.0, -180.0, 270.0, "Date Line West"),
            (37.7749, -122.4194, 270.5, "San Francisco"),
            (51.5074, -0.1278, 90.0, "London"),
            (-33.8688, 151.2093, 45.0, "Sydney"),
        ]

        for lat, lon, heading, desc in test_cases:
            sim.set_position(lat, lon, heading)
            messages.append(sim.generate_message())
            print(f"  Generated: {desc}")

        return messages

    @staticmethod
    def generate_invalid_messages():
        """Generate invalid messages for error testing"""
        messages = [
            # Missing END marker
            "GPS001|37.7749|-122.4194|270.5|2025-11-06T12:00:00Z",

            # Wrong delimiter count
            "GPS001|37.7749|270.5|2025-11-06T12:00:00Z|END",

            # Invalid latitude (> 90)
            "GPS001|91.0000|-122.4194|270.5|2025-11-06T12:00:00Z|END",

            # Invalid longitude (> 180)
            "GPS001|37.7749|181.0000|270.5|2025-11-06T12:00:00Z|END",

            # Non-numeric coordinates
            "GPS001|abc|def|270.5|2025-11-06T12:00:00Z|END",

            # Empty fields
            "GPS001|||-||END",
        ]
        return messages

    @staticmethod
    def generate_stress_test(serial="GPS001", count=100):
        """Generate many messages for stress testing"""
        sim = GpsSimulator(serial)
        messages = []

        for i in range(count):
            sim.move_random(0.1)
            messages.append(sim.generate_message())

        return messages

def send_via_bluetooth(messages, mac_address, interval_seconds=5):
    """Send messages via Bluetooth Serial"""
    if not BLUETOOTH_AVAILABLE:
        print("ERROR: PyBluez not available. Cannot send via Bluetooth.")
        print("Install with: pip install pybluez")
        return False

    try:
        print(f"Connecting to Bluetooth device: {mac_address}")
        sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
        sock.connect((mac_address, 1))  # Channel 1 for SPP

        print("Connected! Sending messages...")

        for i, message in enumerate(messages):
            sock.send(message + "\n")
            print(f"Sent {i+1}/{len(messages)}: {message}")
            time.sleep(interval_seconds)

        sock.close()
        print("All messages sent successfully!")
        return True

    except bluetooth.BluetoothError as e:
        print(f"Bluetooth error: {e}")
        return False
    except Exception as e:
        print(f"Error: {e}")
        return False

def send_via_file(messages, filename):
    """Save messages to file"""
    try:
        with open(filename, 'w') as f:
            for message in messages:
                f.write(message + "\n")
        print(f"Saved {len(messages)} messages to {filename}")
        return True
    except Exception as e:
        print(f"Error writing file: {e}")
        return False

def send_via_socket(messages, host='localhost', port=9999, interval_seconds=5):
    """Send messages via TCP socket"""
    import socket

    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect((host, port))

        print(f"Connected to {host}:{port}. Sending messages...")

        for i, message in enumerate(messages):
            sock.send((message + "\n").encode('utf-8'))
            print(f"Sent {i+1}/{len(messages)}: {message}")
            time.sleep(interval_seconds)

        sock.close()
        print("All messages sent successfully!")
        return True

    except Exception as e:
        print(f"Socket error: {e}")
        return False

def print_messages(messages):
    """Print messages to stdout"""
    print(f"\n{'='*70}")
    print(f"Generated {len(messages)} messages:")
    print(f"{'='*70}\n")

    for i, message in enumerate(messages, 1):
        print(f"{i:3d}. {message}")

    print(f"\n{'='*70}")
    print(f"Total: {len(messages)} messages")
    print(f"{'='*70}\n")

def main():
    parser = argparse.ArgumentParser(
        description="Mock Bluetooth GPS Data Simulator",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Generate 10 moving messages
  python3 bluetooth_simulator.py --mode moving --count 10

  # Test boundary conditions
  python3 bluetooth_simulator.py --mode boundary

  # Generate invalid messages for error testing
  python3 bluetooth_simulator.py --mode invalid

  # Save to file
  python3 bluetooth_simulator.py --mode moving --count 20 --output gps_data.txt

  # Send via TCP socket (requires listener on port 9999)
  python3 bluetooth_simulator.py --mode moving --count 10 --socket localhost:9999

  # Send via Bluetooth (requires PyBluez and device MAC)
  python3 bluetooth_simulator.py --mode moving --bluetooth AA:BB:CC:DD:EE:FF
        """
    )

    parser.add_argument('--mode', choices=['stationary', 'moving', 'circular', 'boundary', 'invalid', 'stress'],
                       default='moving', help='Simulation mode')
    parser.add_argument('--count', type=int, default=10, help='Number of messages to generate')
    parser.add_argument('--serial', default='GPS001', help='Device serial number')
    parser.add_argument('--interval', type=int, default=5, help='Interval between messages (seconds)')
    parser.add_argument('--output', help='Output file path')
    parser.add_argument('--bluetooth', metavar='MAC', help='Send via Bluetooth to MAC address')
    parser.add_argument('--socket', metavar='HOST:PORT', help='Send via TCP socket')
    parser.add_argument('--print-only', action='store_true', help='Only print messages to stdout')

    args = parser.parse_args()

    # Generate messages based on mode
    print(f"Generating {args.mode} mode data...")

    if args.mode == 'stationary':
        messages = MockDataGenerator.generate_stationary(args.serial, args.count, 0)
    elif args.mode == 'moving':
        messages = MockDataGenerator.generate_moving(args.serial, args.count, 0)
    elif args.mode == 'circular':
        messages = MockDataGenerator.generate_circular_route(args.serial, args.count)
    elif args.mode == 'boundary':
        messages = MockDataGenerator.generate_boundary_tests(args.serial)
    elif args.mode == 'invalid':
        messages = MockDataGenerator.generate_invalid_messages()
        print("Note: These are INVALID messages for error testing!")
    elif args.mode == 'stress':
        messages = MockDataGenerator.generate_stress_test(args.serial, args.count)
    else:
        print(f"Unknown mode: {args.mode}")
        return 1

    print(f"Generated {len(messages)} messages\n")

    # Output messages
    if args.print_only:
        print_messages(messages)
    elif args.output:
        send_via_file(messages, args.output)
        print("\nMessages saved! You can now:")
        print(f"  - View them: cat {args.output}")
        print(f"  - Use in tests: python3 tools/test_message_protocol.py < {args.output}")
    elif args.bluetooth:
        send_via_bluetooth(messages, args.bluetooth, args.interval)
    elif args.socket:
        host, port = args.socket.split(':')
        send_via_socket(messages, host, int(port), args.interval)
    else:
        # Default: print to stdout
        print_messages(messages)
        print("\nTip: Use --output to save to file, or --bluetooth to send to device")

    return 0

if __name__ == "__main__":
    sys.exit(main())
