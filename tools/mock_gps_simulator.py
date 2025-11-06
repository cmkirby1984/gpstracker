#!/usr/bin/env python3
"""
Mock GPS Data Simulator
Generates realistic GPS coordinates for testing
"""

import random
import time
from datetime import datetime

# Base coordinates (San Francisco)
BASE_LAT = 37.7749
BASE_LON = -122.4194

def generate_gps_data():
    """Generate mock GPS coordinates"""
    lat = BASE_LAT + (random.random() - 0.5) * 0.1
    lon = BASE_LON + (random.random() - 0.5) * 0.1
    heading = random.random() * 360.0
    speed = random.random() * 50.0
    altitude = random.random() * 100.0
    satellites = random.randint(4, 12)

    return {
        'latitude': lat,
        'longitude': lon,
        'heading': heading,
        'speed': speed,
        'altitude': altitude,
        'satellites': satellites,
        'timestamp': datetime.utcnow().isoformat() + 'Z'
    }

def format_message(data, device_serial="GPS001"):
    """Format GPS data as protocol message"""
    return f"{device_serial}|{data['latitude']:.6f}|{data['longitude']:.6f}|{data['heading']:.2f}|{data['timestamp']}|END"

def main():
    print("=" * 60)
    print("Mock GPS Data Simulator")
    print("=" * 60)
    print()

    try:
        count = 0
        while True:
            data = generate_gps_data()
            message = format_message(data)

            count += 1
            print(f"[{count}] {message}")
            print(f"    Coordinates: {data['latitude']:.6f}, {data['longitude']:.6f}")
            print(f"    Heading: {data['heading']:.2f}°")
            print(f"    Satellites: {data['satellites']}")
            print()

            time.sleep(2)  # Generate new data every 2 seconds

    except KeyboardInterrupt:
        print("\nSimulator stopped")

if __name__ == "__main__":
    main()
