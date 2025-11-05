# Android Tablet Application Pseudocode

## Configuration
```
BLUETOOTH_DEVICE_NAME = "ESP32_TRAILER_001"
EMAIL_RECIPIENT = "fleet@company.com"
EMAIL_SENDER = "trailer.tracker@company.com"
EMAIL_PASSWORD = "app_specific_password"
SMTP_SERVER = "smtp.gmail.com"
SMTP_PORT = 587
```

## Main Application Flow

```
APPLICATION START

// ============================================
// INITIALIZATION PHASE
// ============================================

FUNCTION onCreate():
    1. Set up user interface
       - Display connection status
       - Show last received location
       - Display log of sent emails
    
    2. Check permissions
       - Bluetooth permission
       - Internet permission
       - Location permission (if needed)
    
    3. Initialize Bluetooth manager
       - Get Bluetooth adapter
       - Check if Bluetooth is enabled
       - If disabled, prompt user to enable
    
    4. Load configuration
       - Read email settings from secure storage
       - Read paired device info
    
    5. Start background service
       - Service runs even when app is minimized
       - Maintains Bluetooth connection
       - Handles data reception and email sending
    
    6. Display "Ready - Waiting for ESP32 connection"

END FUNCTION


// ============================================
// BACKGROUND SERVICE
// ============================================

FUNCTION BluetoothService_onStartCommand():
    """
    This service runs continuously in background
    """
    
    WHILE service is running:
        
        // Step 1: Check Bluetooth state
        IF Bluetooth is OFF:
            Show notification "Bluetooth disabled"
            Wait 5 seconds
            CONTINUE to next loop iteration
        END IF
        
        
        // Step 2: Check if connected to ESP32
        IF NOT connected to ESP32:
            Print "Searching for ESP32 device..."
            Show notification "Searching for trailer..."
            
            connected = ATTEMPT_BLUETOOTH_CONNECTION()
            
            IF NOT connected:
                Wait 10 seconds
                CONTINUE to next loop iteration
            END IF
        END IF
        
        
        // Step 3: We are connected - listen for data
        Show notification "Connected to trailer"
        Update UI status to "Connected"
        
        received_data = LISTEN_FOR_DATA()
        
        IF received_data is NULL or EMPTY:
            // Connection lost or no data
            CONTINUE to next loop iteration
        END IF
        
        
        // Step 4: Parse received data
        parsed_data = PARSE_MESSAGE(received_data)
        
        IF parsed_data is INVALID:
            Log error "Invalid data format received"
            CONTINUE to next loop iteration
        END IF
        
        
        // Step 5: Update UI with new location
        UPDATE_UI_WITH_LOCATION(parsed_data)
        
        
        // Step 6: Send email with location update
        email_sent = SEND_EMAIL(parsed_data)
        
        IF email_sent:
            Log "Email sent successfully"
            Show notification "Location update sent"
            Update last_email_time to NOW
        ELSE:
            Log error "Failed to send email"
            Show notification "Email failed - will retry"
        END IF
        
    END WHILE

END FUNCTION


// ============================================
// BLUETOOTH CONNECTION FUNCTIONS
// ============================================

FUNCTION ATTEMPT_BLUETOOTH_CONNECTION():
    """
    Attempts to connect to the ESP32 device
    Returns TRUE if successful, FALSE otherwise
    """
    
    // Step 1: Get list of paired devices
    paired_devices = Bluetooth.getPairedDevices()
    
    esp32_device = NULL
    
    FOR EACH device IN paired_devices:
        IF device.name EQUALS BLUETOOTH_DEVICE_NAME:
            esp32_device = device
            BREAK
        END IF
    END FOR
    
    
    // Step 2: If not paired, try discovery
    IF esp32_device is NULL:
        Print "ESP32 not in paired devices, starting discovery..."
        
        Bluetooth.startDiscovery()
        Wait up to 30 seconds for discovery
        
        discovered_devices = Bluetooth.getDiscoveredDevices()
        
        FOR EACH device IN discovered_devices:
            IF device.name EQUALS BLUETOOTH_DEVICE_NAME:
                esp32_device = device
                BREAK
            END IF
        END FOR
    END IF
    
    
    // Step 3: No device found
    IF esp32_device is NULL:
        Print "ESP32 device not found"
        RETURN FALSE
    END IF
    
    
    // Step 4: Attempt connection
    Print "Connecting to " + esp32_device.name
    
    TRY:
        bluetooth_socket = esp32_device.createRfcommSocket()
        bluetooth_socket.connect()
        
        IF bluetooth_socket.isConnected():
            Print "Successfully connected"
            input_stream = bluetooth_socket.getInputStream()
            RETURN TRUE
        ELSE:
            RETURN FALSE
        END IF
        
    CATCH connection_error:
        Print "Connection failed: " + connection_error
        RETURN FALSE
    END TRY

END FUNCTION


FUNCTION LISTEN_FOR_DATA():
    """
    Listens for incoming data from ESP32
    Returns received message or NULL if error/timeout
    """
    
    IF bluetooth_socket is NULL or NOT connected:
        RETURN NULL
    END IF
    
    TRY:
        // Read from input stream with timeout
        Set read timeout to 60 seconds
        
        buffer = empty byte array
        
        WHILE TRUE:
            IF input_stream has data available:
                byte_read = input_stream.read()
                
                IF byte_read EQUALS newline character:
                    // End of message
                    message = CONVERT buffer to string
                    RETURN message
                ELSE:
                    ADD byte_read to buffer
                END IF
            END IF
            
            IF timeout reached:
                RETURN NULL
            END IF
        END WHILE
        
    CATCH read_error:
        Print "Read error: " + read_error
        Close bluetooth_socket
        bluetooth_socket = NULL
        RETURN NULL
    END TRY

END FUNCTION


// ============================================
// DATA PARSING
// ============================================

FUNCTION PARSE_MESSAGE(raw_message):
    """
    Parses message format: SERIAL|LAT|LON|HEADING|TIME|END
    Returns data structure or NULL if invalid
    """
    
    Print "Parsing: " + raw_message
    
    // Step 1: Check for message terminator
    IF raw_message does NOT contain "END":
        Print "Invalid message - missing END marker"
        RETURN NULL
    END IF
    
    
    // Step 2: Split by delimiter
    parts = SPLIT raw_message by "|"
    
    IF parts.length is NOT EQUAL to 6:
        Print "Invalid message - wrong number of fields"
        RETURN NULL
    END IF
    
    
    // Step 3: Extract fields
    TRY:
        data.serial_number = parts[0]
        data.latitude = PARSE_FLOAT(parts[1])
        data.longitude = PARSE_FLOAT(parts[2])
        data.heading = PARSE_FLOAT(parts[3])
        data.timestamp = parts[4]
        
        // Validate ranges
        IF data.latitude < -90 OR data.latitude > 90:
            THROW "Invalid latitude"
        END IF
        
        IF data.longitude < -180 OR data.longitude > 180:
            THROW "Invalid longitude"
        END IF
        
        IF data.heading < 0 OR data.heading > 360:
            THROW "Invalid heading"
        END IF
        
        Print "Parsed successfully:"
        Print "  Serial: " + data.serial_number
        Print "  Location: " + data.latitude + ", " + data.longitude
        Print "  Heading: " + data.heading
        Print "  Time: " + data.timestamp
        
        RETURN data
        
    CATCH parse_error:
        Print "Parse error: " + parse_error
        RETURN NULL
    END TRY

END FUNCTION


// ============================================
// EMAIL FUNCTIONS
// ============================================

FUNCTION SEND_EMAIL(location_data):
    """
    Sends email with location information
    Returns TRUE if successful, FALSE otherwise
    """
    
    // Step 1: Format email content
    subject = "Trailer Location Update - " + location_data.serial_number
    
    body = "Trailer Location Update\n\n"
    body = body + "Serial Number: " + location_data.serial_number + "\n"
    body = body + "Latitude: " + location_data.latitude + "\n"
    body = body + "Longitude: " + location_data.longitude + "\n"
    body = body + "Heading: " + location_data.heading + " degrees\n"
    body = body + "Timestamp: " + location_data.timestamp + "\n\n"
    body = body + "Google Maps Link:\n"
    body = body + "https://www.google.com/maps?q="
    body = body + location_data.latitude + "," + location_data.longitude
    
    
    // Step 2: Send email using background thread
    TRY:
        // Configure SMTP properties
        smtp_properties = CREATE_SMTP_PROPERTIES()
        
        // Create mail session
        mail_session = CREATE_MAIL_SESSION(smtp_properties)
        
        // Create message
        email_message = CREATE_EMAIL_MESSAGE(
            session = mail_session,
            from = EMAIL_SENDER,
            to = EMAIL_RECIPIENT,
            subject = subject,
            body = body
        )
        
        // Send message
        SMTP.send(email_message)
        
        Print "Email sent successfully to " + EMAIL_RECIPIENT
        Log_to_database(location_data, "SUCCESS")
        
        RETURN TRUE
        
    CATCH email_error:
        Print "Email error: " + email_error
        Log_to_database(location_data, "FAILED")
        
        RETURN FALSE
    END TRY

END FUNCTION


FUNCTION CREATE_SMTP_PROPERTIES():
    """
    Creates SMTP configuration properties
    """
    
    properties = new Properties()
    properties.put("mail.smtp.auth", "true")
    properties.put("mail.smtp.starttls.enable", "true")
    properties.put("mail.smtp.host", SMTP_SERVER)
    properties.put("mail.smtp.port", SMTP_PORT)
    properties.put("mail.smtp.ssl.trust", SMTP_SERVER)
    
    RETURN properties

END FUNCTION


// ============================================
// UI UPDATE FUNCTIONS
// ============================================

FUNCTION UPDATE_UI_WITH_LOCATION(location_data):
    """
    Updates the app UI with latest location
    """
    
    RUN on UI thread:
        // Update status text
        status_text.setText("Last Update: " + location_data.timestamp)
        
        // Update location display
        latitude_text.setText("Lat: " + location_data.latitude)
        longitude_text.setText("Lon: " + location_data.longitude)
        heading_text.setText("Heading: " + location_data.heading + "°")
        
        // Update map marker if map view exists
        IF map_view exists:
            Update marker position on map
        END IF
        
        // Add to history log
        Add entry to history_listview
    END RUN

END FUNCTION


// ============================================
// PERSISTENCE / LOGGING
// ============================================

FUNCTION Log_to_database(location_data, status):
    """
    Saves location updates to local database
    Useful for troubleshooting and history
    """
    
    database_entry = {
        serial_number: location_data.serial_number,
        latitude: location_data.latitude,
        longitude: location_data.longitude,
        heading: location_data.heading,
        timestamp: location_data.timestamp,
        email_status: status,
        received_at: CURRENT_TIME()
    }
    
    database.insert(database_entry)
    
    // Keep only last 1000 entries to save space
    IF database.count() > 1000:
        database.delete_oldest()
    END IF

END FUNCTION


// ============================================
// ERROR RECOVERY
// ============================================

FUNCTION HANDLE_CONNECTION_LOST():
    Print "Connection lost - attempting reconnect"
    
    Close bluetooth_socket
    bluetooth_socket = NULL
    
    Show notification "Connection lost - reconnecting..."
    
    Wait 5 seconds
    // Loop will automatically attempt reconnection
END FUNCTION


FUNCTION HANDLE_EMAIL_FAILURE(location_data):
    Print "Email failed - queuing for retry"
    
    // Store failed send attempt
    failed_queue.add(location_data)
    
    // Try to resend queued messages later
    Schedule retry in 5 minutes
END FUNCTION

```

## Message Reception Flow Diagram

```
[ESP32 sends data] 
    ↓
[Tablet Bluetooth receives]
    ↓
[Parse message format]
    ↓
[Validate GPS coordinates]
    ↓
[Update UI display]
    ↓
[Format email message]
    ↓
[Send via SMTP]
    ↓
[Log to database]
    ↓
[Wait for next message]
```

## Notes

**Bluetooth Connection:**
- Use SPP (Serial Port Profile) UUID: 00001101-0000-1000-8000-00805F9B34FB
- Automatic reconnection after connection loss
- Keep socket open continuously

**Email Handling:**
- Use app-specific password (not regular account password)
- Run email sending in background thread (not main UI thread)
- Queue failed emails for retry
- Consider rate limiting to avoid SMTP throttling

**Power Management:**
- Android may kill background services to save battery
- Use foreground service with persistent notification
- Consider wake locks if needed

**Security:**
- Store email credentials in Android Keystore
- Use encrypted preferences for sensitive data
- Validate all incoming data before processing
