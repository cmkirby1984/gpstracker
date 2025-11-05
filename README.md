# Trailer GPS Tracker System

## Overview
A two-part system for tracking trailer locations:
- **ESP32 Device**: Reads GPS data and transmits via Bluetooth
- **Android Tablet**: Receives GPS data and sends email updates

## Project Structure
```
trailer-gps-tracker/
├── esp32-firmware/          # ESP32 code and pseudocode
├── android-app/             # Android tablet application
└── docs/                    # Documentation and specifications
```

## System Flow
1. ESP32 reads GPS coordinates every 5 minutes
2. ESP32 sends data via Bluetooth to paired tablet
3. Tablet receives data and formats email
4. Tablet sends email with location update

## Hardware Requirements
- ESP32 development board
- GPS module (NEO-6M or similar)
- Android tablet with Bluetooth
- Power supply for ESP32 (battery or hardwired)

## Communication Protocol
- Bluetooth Classic (SPP - Serial Port Profile)
- Simple text-based message format
- 5-minute update interval
