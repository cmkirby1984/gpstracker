#!/usr/bin/env python3
"""
Bluetooth Message Simulator
Simulates ESP32 Bluetooth messages for testing Android app
"""

import socket
import time
import random
from datetime import datetime

# Base coordinates (San Francisco)
BASE_LAT = 37.7749
BASE_LON = -122.4194

def generate_message(device_serial="GPS001"):
    """Generate GPS message"""
    lat = BASE_LAT + (random.random() - 0.5) * 0.1
    lon = BASE_LON + (random.random() - 0.5) * 0.1
    heading = random.random() * 360.0
    timestamp = datetime.utcnow().isoformat() + 'Z'

    return f"{device_serial}|{lat:.6f}|{lon:.6f}|heading:.2f}|{timestamp}|END\n"

def main():
    print("=" * 60)
    print("Bluetooth Message Simulator")
    print("=" * 60)
    print()
    print("This tool simulates ESP32 Bluetooth messages.")
    print("Note: Actual Bluetooth SPP simulation requires platform-specific tools")
    print()
    print("Generated messages:")
    print()

    try:
        count = 0
        while True:
            message = generate_message()
            count += 1

            print(f"[{count}] {message.strip()}")
            time.sleep(5)  # Send every 5 seconds (simulating ESP32 5-min interval in fast mode)

    except KeyboardInterrupt:
        print("\nSimulator stopped")

if __name__ == "__main__":
    main()
