# ESP32 Firmware Pseudocode

## Configuration
```
DEVICE_SERIAL_NUMBER = "TRAILER_001"
GPS_UPDATE_INTERVAL = 300000  // 5 minutes in milliseconds
BLUETOOTH_NAME = "ESP32_TRAILER_001"
GPS_BAUD_RATE = 9600
BLUETOOTH_PIN = "1234"  // Optional pairing PIN
```

## Main Program Flow

```
PROGRAM START

// ============================================
// INITIALIZATION PHASE
// ============================================

FUNCTION setup():
    1. Initialize serial communication for debugging
       - Set baud rate to 115200
       - Print "ESP32 Trailer Tracker Starting..."
    
    2. Initialize GPS module
       - Set GPS serial pins (TX/RX)
       - Set baud rate to GPS_BAUD_RATE
       - Wait for GPS to acquire satellite fix
       - Print "GPS initialized"
    
    3. Initialize Bluetooth module
       - Set Bluetooth name to BLUETOOTH_NAME
       - Set Bluetooth as discoverable
       - Enable SPP (Serial Port Profile)
       - Print "Bluetooth initialized and discoverable"
    
    4. Initialize LED indicator (optional)
       - Set LED pin as output
       - Blink LED to show system ready
    
    5. Print "System ready - waiting for Bluetooth connection"

END FUNCTION


// ============================================
// MAIN LOOP
// ============================================

FUNCTION loop():
    
    // Step 1: Check Bluetooth connection status
    IF Bluetooth is NOT connected:
        Print "Waiting for Bluetooth connection..."
        Blink LED slowly (to indicate searching)
        Wait 1 second
        RETURN to start of loop
    END IF
    
    // Bluetooth IS connected
    Set LED to solid ON
    Print "Bluetooth connected"
    
    
    // Step 2: Read GPS data
    Print "Reading GPS data..."
    
    gps_data = READ_GPS_DATA()
    
    IF gps_data is INVALID:
        Print "GPS fix not available, waiting..."
        Wait 10 seconds
        RETURN to start of loop
    END IF
    
    
    // Step 3: Extract GPS values
    latitude = gps_data.latitude
    longitude = gps_data.longitude
    heading = gps_data.heading
    timestamp = gps_data.timestamp
    
    Print "GPS Fix acquired:"
    Print "  Lat: " + latitude
    Print "  Lon: " + longitude
    Print "  Heading: " + heading
    
    
    // Step 4: Package data into message
    message = CREATE_MESSAGE(
        serial_number = DEVICE_SERIAL_NUMBER,
        lat = latitude,
        lon = longitude,
        heading = heading,
        time = timestamp
    )
    
    Print "Message formatted: " + message
    
    
    // Step 5: Send via Bluetooth
    success = SEND_BLUETOOTH_MESSAGE(message)
    
    IF success:
        Print "Data sent successfully"
        Blink LED quickly 3 times (success indicator)
    ELSE:
        Print "Failed to send data"
        // Will retry next cycle
    END IF
    
    
    // Step 6: Wait for next update cycle
    Print "Waiting " + (GPS_UPDATE_INTERVAL/1000) + " seconds until next update..."
    Wait GPS_UPDATE_INTERVAL milliseconds

END FUNCTION (loop repeats automatically)


// ============================================
// HELPER FUNCTIONS
// ============================================

FUNCTION READ_GPS_DATA():
    """
    Reads and parses GPS data from the module
    Returns GPS data structure or INVALID if no fix
    """
    
    max_attempts = 10
    attempt = 0
    
    WHILE attempt < max_attempts:
        IF GPS has new data available:
            raw_data = GPS.read()
            
            IF raw_data contains valid fix:
                gps_data.latitude = PARSE_LATITUDE(raw_data)
                gps_data.longitude = PARSE_LONGITUDE(raw_data)
                gps_data.heading = PARSE_HEADING(raw_data)
                gps_data.timestamp = PARSE_TIME(raw_data)
                gps_data.valid = TRUE
                
                RETURN gps_data
            END IF
        END IF
        
        attempt = attempt + 1
        Wait 1 second
    END WHILE
    
    // No valid GPS fix after all attempts
    gps_data.valid = FALSE
    RETURN gps_data

END FUNCTION


FUNCTION CREATE_MESSAGE(serial_number, lat, lon, heading, time):
    """
    Formats GPS data into a standard message string
    Format: SERIAL|LAT|LON|HEADING|TIME
    """
    
    message = serial_number + "|"
    message = message + STRING(lat, 6_decimal_places) + "|"
    message = message + STRING(lon, 6_decimal_places) + "|"
    message = message + STRING(heading, 2_decimal_places) + "|"
    message = message + time + "|"
    message = message + "END"
    
    RETURN message

END FUNCTION


FUNCTION SEND_BLUETOOTH_MESSAGE(message):
    """
    Sends message via Bluetooth
    Returns TRUE if successful, FALSE otherwise
    """
    
    IF Bluetooth is NOT connected:
        RETURN FALSE
    END IF
    
    TRY:
        Bluetooth.write(message)
        Bluetooth.write("\n")  // Newline as message terminator
        RETURN TRUE
    CATCH error:
        Print "Bluetooth send error: " + error
        RETURN FALSE
    END TRY

END FUNCTION


// ============================================
// ERROR HANDLING
// ============================================

FUNCTION handle_gps_error():
    Print "GPS module error - attempting restart"
    Reset GPS module
    Wait 5 seconds
    Re-initialize GPS
END FUNCTION

FUNCTION handle_bluetooth_disconnect():
    Print "Bluetooth disconnected"
    Set LED to slow blink
    Wait for reconnection
END FUNCTION

```

## Message Format Specification

**Format:** `SERIAL|LAT|LON|HEADING|TIME|END`

**Example:**
```
TRAILER_001|49.123456|-123.456789|275.50|2025-11-05T14:30:00|END
```

**Fields:**
- SERIAL: Unique trailer identifier
- LAT: Latitude (decimal degrees, 6 decimal places)
- LON: Longitude (decimal degrees, 6 decimal places)  
- HEADING: Direction in degrees (0-360, 2 decimal places)
- TIME: ISO 8601 timestamp
- END: Message terminator

## Power Management (Future Enhancement)

```
FUNCTION enter_deep_sleep():
    // For battery-powered operation
    Configure wake-up timer for GPS_UPDATE_INTERVAL
    Disable Bluetooth
    Disable GPS
    Enter deep sleep mode
END FUNCTION

FUNCTION wake_from_sleep():
    Re-initialize GPS
    Re-initialize Bluetooth
    Resume normal operation
END FUNCTION
```

## Notes
- GPS typically needs 30-60 seconds for initial fix (cold start)
- Bluetooth reconnection should be automatic after pairing
- Consider adding battery monitoring if running on battery
- LED indicators help with debugging in the field
