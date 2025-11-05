"""
ESP32 GPS Tracker Main Firmware
Reads GPS data from Neo-6M module and transmits via Bluetooth
"""

import time
from machine import UART, Pin
from bluetooth import BLE
from gps_parser import GPSParser
import config

# LED indicator pin (built-in LED on most ESP32 boards)
led = Pin(2, Pin.OUT)

# GPS UART setup
# Neo-6M default is 9600 baud
# TX2 = GPIO17, RX2 = GPIO16 (you can change these in config)
gps_uart = UART(2, baudrate=config.GPS_BAUD_RATE,
                tx=config.GPS_TX_PIN, rx=config.GPS_RX_PIN,
                timeout=1000)

# GPS parser instance
gps = GPSParser()

# Bluetooth setup (using Bluetooth Classic/SPP)
# Note: For full Bluetooth Classic, you may need to use esp32.Bluetooth()
# This is a simplified version - see notes below
try:
    from machine import Bluetooth
    bt = Bluetooth()
except:
    print("WARNING: Bluetooth Classic not available in this build")
    print("You may need to use BLE or a different MicroPython firmware")
    bt = None

def blink_led(times=1, duration=0.1):
    """Blink LED for visual feedback"""
    for _ in range(times):
        led.on()
        time.sleep(duration)
        led.off()
        time.sleep(duration)

def setup_bluetooth():
    """Initialize Bluetooth in discoverable mode"""
    if bt is None:
        print("Bluetooth not available")
        return False

    try:
        bt.init()
        bt.set_name(config.BLUETOOTH_NAME)
        bt.discoverable(True)
        print(f"Bluetooth initialized: {config.BLUETOOTH_NAME}")
        print("Waiting for connection...")
        return True
    except Exception as e:
        print(f"Bluetooth setup error: {e}")
        return False

def read_gps_data(max_attempts=20):
    """
    Read and parse GPS data from UART
    Returns GPS data dict or None if no valid fix
    """
    print("Reading GPS data...")
    attempts = 0

    while attempts < max_attempts:
        if gps_uart.any():
            try:
                line = gps_uart.readline()
                if line:
                    sentence = line.decode('ascii', 'ignore').strip()

                    # Parse the NMEA sentence
                    if gps.parse_nmea_sentence(sentence):
                        if gps.is_valid():
                            return gps.get_location_data()
            except Exception as e:
                print(f"GPS read error: {e}")

        attempts += 1
        time.sleep(0.5)

    print("No valid GPS fix acquired")
    return None

def create_message(gps_data):
    """
    Create formatted message according to protocol
    Format: SERIAL|LAT|LON|HEADING|TIME|END
    """
    try:
        # Get current date (you may need to set RTC for accurate date)
        # For now, just using time from GPS
        timestamp = gps_data['timestamp'] if gps_data['timestamp'] else "00:00:00"

        message = (
            f"{config.DEVICE_SERIAL_NUMBER}|"
            f"{gps_data['latitude']:.6f}|"
            f"{gps_data['longitude']:.6f}|"
            f"{gps_data['heading']:.2f}|"
            f"{timestamp}|"
            f"END"
        )

        return message
    except Exception as e:
        print(f"Error creating message: {e}")
        return None

def send_bluetooth_message(message):
    """
    Send message via Bluetooth
    Returns True if successful
    """
    if bt is None:
        print("Bluetooth not available - printing message instead:")
        print(f">>> {message}")
        return False

    try:
        if bt.connected():
            bt.write(message + "\n")
            return True
        else:
            print("Bluetooth not connected")
            return False
    except Exception as e:
        print(f"Bluetooth send error: {e}")
        return False

def send_serial_message(message):
    """
    Alternative: send via serial/USB for debugging
    """
    print(f"GPS DATA: {message}")

def main():
    """Main program loop"""
    print("=" * 50)
    print("ESP32 GPS Tracker Starting...")
    print(f"Device: {config.DEVICE_SERIAL_NUMBER}")
    print(f"Update Interval: {config.GPS_UPDATE_INTERVAL / 1000}s")
    print("=" * 50)

    # Setup
    print("\nInitializing GPS module...")
    print(f"GPS UART: TX={config.GPS_TX_PIN}, RX={config.GPS_RX_PIN}, Baud={config.GPS_BAUD_RATE}")

    print("\nInitializing Bluetooth...")
    bt_available = setup_bluetooth()

    # Ready indicator
    blink_led(3, 0.2)
    print("\nSystem Ready!")
    print("-" * 50)

    # Main loop
    while True:
        try:
            # Check Bluetooth connection
            bt_connected = bt and bt.connected() if bt_available else False

            if not bt_connected and bt_available:
                print("\nWaiting for Bluetooth connection...")
                blink_led(1, 0.5)
                time.sleep(2)
                continue

            if bt_connected:
                led.on()
                print("\nBluetooth connected!")

            # Read GPS data
            gps_data = read_gps_data()

            if gps_data is None:
                print("GPS fix not available, retrying...")
                blink_led(2, 0.1)
                time.sleep(10)
                continue

            # Display GPS info
            print(f"\nGPS Fix Acquired:")
            print(f"  Latitude:   {gps_data['latitude']:.6f}")
            print(f"  Longitude:  {gps_data['longitude']:.6f}")
            print(f"  Heading:    {gps_data['heading']:.2f}°")
            print(f"  Time:       {gps_data['timestamp']}")
            print(f"  Satellites: {gps_data['satellites']}")

            # Create message
            message = create_message(gps_data)

            if message:
                print(f"\nFormatted Message: {message}")

                # Send via Bluetooth
                if bt_connected:
                    success = send_bluetooth_message(message)
                    if success:
                        print("✓ Data sent via Bluetooth")
                        blink_led(3, 0.1)
                    else:
                        print("✗ Failed to send via Bluetooth")
                else:
                    # Fallback to serial output
                    send_serial_message(message)

            # Wait for next update
            wait_seconds = config.GPS_UPDATE_INTERVAL / 1000
            print(f"\nWaiting {wait_seconds}s until next update...")
            print("-" * 50)

            led.off()
            time.sleep(wait_seconds)

        except KeyboardInterrupt:
            print("\n\nShutting down...")
            led.off()
            break
        except Exception as e:
            print(f"\nError in main loop: {e}")
            blink_led(5, 0.1)
            time.sleep(5)

if __name__ == "__main__":
    main()
