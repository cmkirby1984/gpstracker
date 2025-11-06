/**
 * Fleet GPS Tracker - Main Program
 * ESP32 + NEO-6M GPS Module
 *
 * This firmware reads GPS coordinates and transmits them via Bluetooth
 * to a paired Android tablet every 5 minutes.
 *
 * Protocol: SERIAL|LAT|LON|HEADING|TIME|END
 */

#include <Arduino.h>
#include "config.h"
#include "logger.h"
#include "gps_module.h"
#include "bluetooth_module.h"
#include "message_protocol.h"

// ============================================================================
// GLOBAL OBJECTS
// ============================================================================

GpsModule gps;
BluetoothModule bluetooth;

// ============================================================================
// STATE TRACKING
// ============================================================================

unsigned long lastGpsUpdate = 0;
unsigned long lastStatusPrint = 0;
bool systemReady = false;

// Statistics
uint32_t totalUpdatesSent = 0;
uint32_t failedUpdates = 0;

// ============================================================================
// FUNCTION DECLARATIONS
// ============================================================================

void setupSystem();
void loopSystem();
void sendGpsUpdate();
void printStatus();
void handleBluetoothEvent(BluetoothEvent event);
void blinkStatusLed(int times, int delayMs);

// ============================================================================
// ARDUINO SETUP
// ============================================================================

void setup() {
    setupSystem();
}

// ============================================================================
// ARDUINO LOOP
// ============================================================================

void loop() {
    loopSystem();
}

// ============================================================================
// SYSTEM INITIALIZATION
// ============================================================================

void setupSystem() {
    // Initialize status LED if configured
    #if LED_STATUS_PIN >= 0
    pinMode(LED_STATUS_PIN, OUTPUT);
    digitalWrite(LED_STATUS_PIN, LOW);
    #endif

    // Initialize logger
    Logger::init(SERIAL_BAUD_RATE);

    LOG_INFO("MAIN", "========================================");
    LOG_INFO("MAIN", "  Fleet GPS Tracker System");
    LOG_INFO("MAIN", "  Starting initialization...");
    LOG_INFO("MAIN", "========================================");

    // Print configuration
    LOG_INFO("MAIN", "Configuration:");
    LOG_INFO("MAIN", "  Device Serial: %s", DEVICE_SERIAL);
    LOG_INFO("MAIN", "  Firmware: %s", FIRMWARE_VERSION);
    LOG_INFO("MAIN", "  Update Interval: %lu ms", UPDATE_INTERVAL_MS);
    LOG_INFO("MAIN", "  Mock GPS: %s", USE_MOCK_GPS ? "ENABLED" : "DISABLED");

    // Initialize GPS module
    LOG_INFO("MAIN", "Initializing GPS...");
    if (gps.begin()) {
        LOG_INFO("MAIN", "GPS initialized successfully");
        blinkStatusLed(2, 200);
    } else {
        LOG_ERROR("MAIN", "GPS initialization failed!");
        blinkStatusLed(5, 100);
        // Continue anyway - may be in mock mode
    }

    // Initialize Bluetooth module
    LOG_INFO("MAIN", "Initializing Bluetooth...");
    if (bluetooth.begin(BT_DEVICE_NAME)) {
        LOG_INFO("MAIN", "Bluetooth initialized successfully");
        blinkStatusLed(3, 200);
    } else {
        LOG_ERROR("MAIN", "Bluetooth initialization failed!");
        blinkStatusLed(10, 100);
        // This is critical - cannot continue without Bluetooth
        LOG_ERROR("MAIN", "SYSTEM HALTED - Bluetooth required");
        while (1) {
            delay(1000);
        }
    }

    // Set Bluetooth event callback
    bluetooth.setEventCallback(handleBluetoothEvent);

    // Print diagnostics
    LOG_INFO("MAIN", "System diagnostics:");
    gps.printDiagnostics();
    bluetooth.printDiagnostics();

    systemReady = true;
    LOG_INFO("MAIN", "========================================");
    LOG_INFO("MAIN", "  System Ready!");
    LOG_INFO("MAIN", "  Waiting for Bluetooth connection...");
    LOG_INFO("MAIN", "========================================");

    // Blink to indicate ready
    blinkStatusLed(3, 500);
}

// ============================================================================
// MAIN SYSTEM LOOP
// ============================================================================

void loopSystem() {
    // Update modules
    gps.update();
    bluetooth.update();

    // Check if it's time to send GPS update
    unsigned long currentTime = millis();

    if (currentTime - lastGpsUpdate >= UPDATE_INTERVAL_MS) {
        lastGpsUpdate = currentTime;
        sendGpsUpdate();
    }

    // Print status periodically
    if (currentTime - lastStatusPrint >= 30000) {  // Every 30 seconds
        lastStatusPrint = currentTime;
        printStatus();
    }

    // Small delay to prevent tight looping
    delay(100);

    // Blink LED if connected
    #if LED_STATUS_PIN >= 0
    if (bluetooth.isConnected()) {
        static unsigned long lastBlink = 0;
        if (currentTime - lastBlink >= 2000) {
            lastBlink = currentTime;
            digitalWrite(LED_STATUS_PIN, !digitalRead(LED_STATUS_PIN));
        }
    }
    #endif
}

// ============================================================================
// GPS UPDATE SENDING
// ============================================================================

void sendGpsUpdate() {
    LOG_INFO("MAIN", "--- GPS Update Cycle ---");

    // Check if Bluetooth is connected
    if (!bluetooth.isConnected()) {
        LOG_WARN("MAIN", "Skipping update - Bluetooth not connected");
        failedUpdates++;
        return;
    }

    // Get GPS data
    GpsData gpsData;
    if (!gps.getData(gpsData)) {
        LOG_ERROR("MAIN", "Failed to get GPS data");
        failedUpdates++;
        return;
    }

    // Format message
    char message[MAX_MESSAGE_LENGTH];
    if (!MessageProtocol::formatMessage(gpsData, message, sizeof(message))) {
        LOG_ERROR("MAIN", "Failed to format message");
        failedUpdates++;
        return;
    }

    // Send message via Bluetooth
    if (bluetooth.sendMessage(message)) {
        totalUpdatesSent++;
        LOG_INFO("MAIN", "Update sent successfully (#%lu)", totalUpdatesSent);
        LOG_INFO("MAIN", "Location: %.6f, %.6f",
                 gpsData.latitude, gpsData.longitude);

        blinkStatusLed(1, 100);
    } else {
        LOG_ERROR("MAIN", "Failed to send message");
        failedUpdates++;
    }

    LOG_INFO("MAIN", "--- End Update Cycle ---");
}

// ============================================================================
// STATUS REPORTING
// ============================================================================

void printStatus() {
    LOG_INFO("MAIN", "========================================");
    LOG_INFO("MAIN", "  System Status");
    LOG_INFO("MAIN", "========================================");
    LOG_INFO("MAIN", "Uptime: %lu ms", millis());
    LOG_INFO("MAIN", "Free Heap: %lu bytes", ESP.getFreeHeap());

    LOG_INFO("MAIN", "GPS Status: %d", gps.getStatus());
    LOG_INFO("MAIN", "GPS Fix: %s", gps.hasFix() ? "YES" : "NO");
    LOG_INFO("MAIN", "Satellites: %d", gps.getSatellites());

    LOG_INFO("MAIN", "Bluetooth State: %d", bluetooth.getState());
    LOG_INFO("MAIN", "Bluetooth Connected: %s",
             bluetooth.isConnected() ? "YES" : "NO");

    LOG_INFO("MAIN", "Updates Sent: %lu", totalUpdatesSent);
    LOG_INFO("MAIN", "Failed Updates: %lu", failedUpdates);
    LOG_INFO("MAIN", "Success Rate: %.1f%%",
             totalUpdatesSent > 0 ?
             (100.0 * totalUpdatesSent / (totalUpdatesSent + failedUpdates)) : 0.0);

    LOG_INFO("MAIN", "Next update in: %lu ms",
             UPDATE_INTERVAL_MS - (millis() - lastGpsUpdate));

    LOG_INFO("MAIN", "========================================");
}

// ============================================================================
// BLUETOOTH EVENT HANDLER
// ============================================================================

void handleBluetoothEvent(BluetoothEvent event) {
    switch (event) {
        case BT_EVENT_CONNECTED:
            LOG_INFO("MAIN", "!!! Bluetooth device connected !!!");
            blinkStatusLed(5, 100);
            // Send immediate update on connection
            sendGpsUpdate();
            break;

        case BT_EVENT_DISCONNECTED:
            LOG_WARN("MAIN", "!!! Bluetooth device disconnected !!!");
            blinkStatusLed(3, 200);
            break;

        case BT_EVENT_DATA_SENT:
            LOG_DEBUG("MAIN", "Data sent via Bluetooth");
            break;

        case BT_EVENT_ERROR:
            LOG_ERROR("MAIN", "Bluetooth error occurred");
            blinkStatusLed(10, 50);
            break;

        default:
            break;
    }
}

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

void blinkStatusLed(int times, int delayMs) {
    #if LED_STATUS_PIN >= 0
    for (int i = 0; i < times; i++) {
        digitalWrite(LED_STATUS_PIN, HIGH);
        delay(delayMs);
        digitalWrite(LED_STATUS_PIN, LOW);
        delay(delayMs);
    }
    #endif
}

// ============================================================================
// ADDITIONAL FEATURES (TODO for future enhancements)
// ============================================================================

// TODO: Implement deep sleep mode for battery operation
// TODO: Add battery voltage monitoring
// TODO: Implement SD card logging for offline storage
// TODO: Add geofencing alerts
// TODO: Implement remote configuration via Bluetooth commands
// TODO: Add support for multiple paired devices
// TODO: Implement OTA (Over-The-Air) firmware updates
