"""
Configuration file for ESP32 GPS Tracker
Modify these settings to match your hardware setup
"""

# Device identification
DEVICE_SERIAL_NUMBER = "TRAILER_001"

# GPS Module Settings (Neo-6M)
GPS_BAUD_RATE = 9600           # Neo-6M default baud rate
GPS_TX_PIN = 17                # ESP32 TX pin connected to Neo-6M RX
GPS_RX_PIN = 16                # ESP32 RX pin connected to Neo-6M TX

# Update interval (in milliseconds)
# 300000 = 5 minutes
GPS_UPDATE_INTERVAL = 300000

# Bluetooth Settings
BLUETOOTH_NAME = "ESP32_TRAILER_001"
BLUETOOTH_PIN = "1234"         # Optional pairing PIN

# LED indicator pin (most ESP32 boards have built-in LED on GPIO 2)
LED_PIN = 2

# Debug mode (enables verbose serial output)
DEBUG_MODE = True
