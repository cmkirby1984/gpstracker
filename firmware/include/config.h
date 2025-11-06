#ifndef CONFIG_H
#define CONFIG_H

// ============================================================================
// FLEET GPS TRACKER CONFIGURATION
// ============================================================================
// Update these values when hardware arrives and before deployment
// ============================================================================

// ----------------------------------------------------------------------------
// HARDWARE CONFIGURATION
// ----------------------------------------------------------------------------

// GPS Module UART Configuration (NEO-6M)
// TODO: Verify these pin assignments match your actual wiring
#define GPS_RX_PIN 16              // ESP32 RX pin (connects to GPS TX)
#define GPS_TX_PIN 17              // ESP32 TX pin (connects to GPS RX)
#define GPS_BAUD_RATE 9600         // NEO-6M default baud rate

// GPS Module Type
#define GPS_MODULE_NEO6M           // Supported: NEO6M, NEO7M, NEO8M

// ----------------------------------------------------------------------------
// BLUETOOTH CONFIGURATION
// ----------------------------------------------------------------------------

// Bluetooth Device Identity
// TODO: Set a unique name for each device in your fleet
#define BT_DEVICE_NAME "FleetGPS_001"
#define BT_PIN "1234"              // Optional: Bluetooth pairing PIN

// Bluetooth Serial Profile
#define BT_ENABLE_SSP true         // Secure Simple Pairing

// ----------------------------------------------------------------------------
// TIMING CONFIGURATION
// ----------------------------------------------------------------------------

// Update Interval (how often to send GPS updates)
#define UPDATE_INTERVAL_MS 300000  // 5 minutes (300,000 ms)
                                   // TODO: Adjust based on your tracking needs
                                   // Options: 60000 (1 min), 300000 (5 min),
                                   //          900000 (15 min), 1800000 (30 min)

// GPS Reading Timeout
#define GPS_READ_TIMEOUT_MS 5000   // Max time to wait for GPS data

// GPS Fix Timeout
#define GPS_FIX_TIMEOUT_MS 120000  // Max time to wait for GPS fix (2 minutes)

// Bluetooth Connection Timeout
#define BT_CONNECT_TIMEOUT_MS 10000 // Max time to wait for BT connection

// ----------------------------------------------------------------------------
// MOCK/SIMULATION MODE
// ----------------------------------------------------------------------------

// Mock GPS Mode (for testing without hardware)
// Set to 0 when real GPS module is connected
#ifndef USE_MOCK_GPS
#define USE_MOCK_GPS 1             // 1 = Use mock data, 0 = Use real GPS
#endif

// Mock GPS Test Coordinates (San Francisco)
#define MOCK_GPS_LAT 37.7749
#define MOCK_GPS_LON -122.4194
#define MOCK_GPS_HEADING 270.5
#define MOCK_GPS_SPEED 0.0
#define MOCK_GPS_ALTITUDE 50.0

// ----------------------------------------------------------------------------
// DEVICE IDENTIFICATION
// ----------------------------------------------------------------------------

// Serial Number / Device ID
// TODO: Set unique identifier for each device
#define DEVICE_SERIAL "GPS001"     // Format: GPS001, GPS002, etc.

// Firmware Version
#define FIRMWARE_VERSION "1.0.0"
#define BUILD_DATE __DATE__
#define BUILD_TIME __TIME__

// ----------------------------------------------------------------------------
// LOGGING CONFIGURATION
// ----------------------------------------------------------------------------

// Serial Debug Output
#define SERIAL_BAUD_RATE 115200    // Serial monitor baud rate

// Log Levels (set via platformio.ini -DLOG_LEVEL=X)
#ifndef LOG_LEVEL
#define LOG_LEVEL 3                // 0=OFF, 1=ERROR, 2=WARN, 3=INFO, 4=DEBUG, 5=VERBOSE
#endif

// Enable/Disable Specific Log Categories
#define LOG_GPS_SENTENCES 1        // Log raw GPS NMEA sentences
#define LOG_BT_EVENTS 1            // Log Bluetooth events
#define LOG_MESSAGE_PROTOCOL 1     // Log message formatting

// ----------------------------------------------------------------------------
// POWER MANAGEMENT
// ----------------------------------------------------------------------------

// Power Saving Features
#define ENABLE_SLEEP_MODE 0        // TODO: Enable for battery operation
#define SLEEP_BETWEEN_UPDATES 0    // Sleep between GPS updates

// Low Battery Threshold (for future battery monitoring)
#define LOW_BATTERY_VOLTAGE 3.3    // Volts

// ----------------------------------------------------------------------------
// ADVANCED CONFIGURATION
// ----------------------------------------------------------------------------

// GPS Data Validation
#define MIN_SATELLITES 4           // Minimum satellites for valid fix
#define MAX_HDOP 5.0               // Maximum HDOP for valid fix

// Bluetooth Buffer Sizes
#define BT_BUFFER_SIZE 256         // Bluetooth TX buffer size

// Message Protocol
#define MESSAGE_DELIMITER '|'
#define MESSAGE_TERMINATOR "END"

// Watchdog Timer
#define ENABLE_WATCHDOG 1          // Enable hardware watchdog
#define WATCHDOG_TIMEOUT_SEC 30    // Watchdog timeout in seconds

// LED Indicators (if external LEDs connected)
#define LED_GPS_FIX_PIN -1         // -1 = disabled, GPIO pin number if used
#define LED_BT_CONNECTED_PIN -1    // -1 = disabled, GPIO pin number if used
#define LED_STATUS_PIN 2           // Built-in LED on most ESP32 boards

// ----------------------------------------------------------------------------
// FEATURE FLAGS
// ----------------------------------------------------------------------------

#define ENABLE_GPS 1               // Enable GPS functionality
#define ENABLE_BLUETOOTH 1         // Enable Bluetooth functionality
#define ENABLE_SD_LOGGING 0        // TODO: Enable for SD card data logging
#define ENABLE_BATTERY_MONITOR 0   // TODO: Enable for battery voltage monitoring

// ----------------------------------------------------------------------------
// VALIDATION AND CHECKS
// ----------------------------------------------------------------------------

#if UPDATE_INTERVAL_MS < 10000
#warning "Update interval less than 10 seconds may drain battery quickly"
#endif

#if USE_MOCK_GPS
#warning "Mock GPS mode enabled - disable for production use"
#endif

#if !ENABLE_GPS && !USE_MOCK_GPS
#error "GPS disabled and mock mode off - no GPS data source!"
#endif

// ----------------------------------------------------------------------------
// HELPER MACROS
// ----------------------------------------------------------------------------

#define ARRAY_SIZE(arr) (sizeof(arr) / sizeof((arr)[0]))

// Convert minutes to milliseconds
#define MINUTES_TO_MS(min) ((min) * 60 * 1000)

// Convert hours to milliseconds
#define HOURS_TO_MS(hrs) ((hrs) * 60 * 60 * 1000)

#endif // CONFIG_H
