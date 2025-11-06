#ifndef BLUETOOTH_MODULE_H
#define BLUETOOTH_MODULE_H

#include <Arduino.h>
#include <BluetoothSerial.h>
#include "config.h"

// ============================================================================
// BLUETOOTH MODULE
// ============================================================================
// Manages Bluetooth Classic (SPP) communication for sending GPS data
// Handles connection state, reconnection, and message transmission
// ============================================================================

// Check if Bluetooth is available
#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error "Bluetooth is not enabled! Please enable it in platformio.ini"
#endif

// Bluetooth connection states
enum BluetoothState {
    BT_STATE_UNINITIALIZED,
    BT_STATE_INITIALIZING,
    BT_STATE_IDLE,              // Ready but not connected
    BT_STATE_ADVERTISING,       // Discoverable, waiting for connection
    BT_STATE_CONNECTING,
    BT_STATE_CONNECTED,
    BT_STATE_DISCONNECTED,
    BT_STATE_ERROR
};

// Bluetooth event types
enum BluetoothEvent {
    BT_EVENT_NONE,
    BT_EVENT_INITIALIZED,
    BT_EVENT_CONNECTED,
    BT_EVENT_DISCONNECTED,
    BT_EVENT_DATA_SENT,
    BT_EVENT_DATA_RECEIVED,
    BT_EVENT_ERROR
};

// Bluetooth module class
class BluetoothModule {
public:
    BluetoothModule();
    ~BluetoothModule();

    // Initialize Bluetooth module
    // Returns: true if initialization successful
    bool begin(const char* deviceName);

    // Update Bluetooth state (call regularly in loop)
    void update();

    // Check if Bluetooth is connected to a device
    bool isConnected();

    // Send message via Bluetooth
    // Returns: true if message sent successfully
    bool sendMessage(const char* message);

    // Get current Bluetooth state
    BluetoothState getState();

    // Get connected device name
    const char* getConnectedDeviceName();

    // Get number of messages sent
    uint32_t getMessagesSent();

    // Get number of connection attempts
    uint32_t getConnectionAttempts();

    // Disconnect from current device
    void disconnect();

    // Restart Bluetooth (reinitialize)
    bool restart();

    // Enable/disable Bluetooth discoverability
    void setDiscoverable(bool discoverable);

    // Get diagnostic information
    void printDiagnostics();

    // Callbacks for Bluetooth events (optional)
    typedef void (*EventCallback)(BluetoothEvent event);
    void setEventCallback(EventCallback callback);

private:
    BluetoothSerial btSerial;        // ESP32 Bluetooth Serial
    BluetoothState state;
    BluetoothState previousState;
    EventCallback eventCallback;

    char deviceName[32];
    char connectedDeviceName[32];

    uint32_t messagesSent;
    uint32_t bytesSent;
    uint32_t connectionAttempts;
    uint32_t disconnectCount;

    unsigned long lastStateChange;
    unsigned long lastConnectionAttempt;
    unsigned long lastHeartbeat;

    bool discoverable;

    // State management
    void changeState(BluetoothState newState);
    void handleStateTransition();

    // Connection management
    bool checkConnection();
    void handleDisconnection();
    void attemptReconnection();

    // Send heartbeat to keep connection alive
    void sendHeartbeat();

    // Event notification
    void notifyEvent(BluetoothEvent event);
};

// Bluetooth utility functions
namespace BluetoothUtils {
    // Get Bluetooth MAC address
    String getMacAddress();

    // Check if Bluetooth is supported on this hardware
    bool isBluetoothSupported();

    // Get Bluetooth signal strength (if available)
    int getSignalStrength();

    // Format Bluetooth diagnostics
    void formatDiagnostics(char* buffer, size_t bufferSize);
}

#endif // BLUETOOTH_MODULE_H
