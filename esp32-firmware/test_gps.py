"""
Simple GPS Test Script
Use this to verify your Neo-6M GPS module is connected and working
Run this before using the main firmware to troubleshoot GPS issues
"""

from machine import UART
import time

# GPS UART Configuration
# Adjust these if you're using different pins
GPS_TX_PIN = 17  # ESP32 TX to GPS RX
GPS_RX_PIN = 16  # ESP32 RX to GPS TX
GPS_BAUD = 9600

print("=" * 60)
print("ESP32 GPS Module Test")
print("=" * 60)
print(f"\nConfiguration:")
print(f"  ESP32 TX Pin: GPIO {GPS_TX_PIN}")
print(f"  ESP32 RX Pin: GPIO {GPS_RX_PIN}")
print(f"  Baud Rate: {GPS_BAUD}")
print("\nInitializing UART...")

# Initialize GPS UART
gps_uart = UART(2, baudrate=GPS_BAUD, tx=GPS_TX_PIN, rx=GPS_RX_PIN, timeout=1000)

print("UART initialized successfully!")
print("\n" + "=" * 60)
print("Reading raw GPS data (NMEA sentences)...")
print("Press Ctrl+C to stop")
print("=" * 60 + "\n")

try:
    line_count = 0
    gpgga_count = 0
    gprmc_count = 0

    while True:
        if gps_uart.any():
            try:
                line = gps_uart.readline()
                if line:
                    sentence = line.decode('ascii', 'ignore').strip()

                    # Print the raw NMEA sentence
                    print(sentence)

                    # Count sentence types
                    line_count += 1
                    if '$GPGGA' in sentence or '$GNGGA' in sentence:
                        gpgga_count += 1
                    elif '$GPRMC' in sentence or '$GNRMC' in sentence:
                        gprmc_count += 1

                    # Print statistics every 50 lines
                    if line_count % 50 == 0:
                        print("\n" + "-" * 60)
                        print(f"Statistics: {line_count} lines received")
                        print(f"  GPGGA sentences: {gpgga_count}")
                        print(f"  GPRMC sentences: {gprmc_count}")
                        print("-" * 60 + "\n")

            except Exception as e:
                print(f"Error reading data: {e}")

        time.sleep(0.1)

except KeyboardInterrupt:
    print("\n\n" + "=" * 60)
    print("Test stopped by user")
    print("=" * 60)
    print(f"\nFinal Statistics:")
    print(f"  Total lines: {line_count}")
    print(f"  GPGGA sentences: {gpgga_count}")
    print(f"  GPRMC sentences: {gprmc_count}")
    print("\nInterpretation:")

    if line_count == 0:
        print("  ❌ No data received!")
        print("  Check:")
        print("    - Wiring (TX/RX might be swapped)")
        print("    - GPS module power (should have LED on)")
        print("    - Correct GPIO pins in config")
    elif line_count > 0:
        print("  ✓ GPS module is communicating!")
        if gpgga_count > 0 or gprmc_count > 0:
            print("  ✓ Receiving GPS data sentences")
            print("\n  Look for lines containing valid coordinates:")
            print("    - Lines starting with $GPGGA or $GPRMC")
            print("    - If you see empty fields (,,,), GPS doesn't have a fix yet")
            print("    - Take device outside for better signal")
        else:
            print("  ⚠️  Receiving data but no GPS sentences detected")
            print("  This might be normal if GPS hasn't acquired satellites yet")

    print("\n" + "=" * 60)
