#include "bluetooth_module.h"
#include "logger.h"

static const char* TAG = "Bluetooth";

BluetoothModule::BluetoothModule()
    : state(BT_STATE_UNINITIALIZED),
      previousState(BT_STATE_UNINITIALIZED),
      eventCallback(nullptr),
      messagesSent(0),
      bytesSent(0),
      connectionAttempts(0),
      disconnectCount(0),
      lastStateChange(0),
      lastConnectionAttempt(0),
      lastHeartbeat(0),
      discoverable(true) {

    memset(deviceName, 0, sizeof(deviceName));
    memset(connectedDeviceName, 0, sizeof(connectedDeviceName));
}

BluetoothModule::~BluetoothModule() {
    if (state != BT_STATE_UNINITIALIZED) {
        btSerial.end();
    }
}

bool BluetoothModule::begin(const char* name) {
    LOG_INFO(TAG, "Initializing Bluetooth module...");
    state = BT_STATE_INITIALIZING;

    if (!name || strlen(name) == 0) {
        LOG_ERROR(TAG, "Invalid device name");
        state = BT_STATE_ERROR;
        return false;
    }

    strncpy(deviceName, name, sizeof(deviceName) - 1);
    deviceName[sizeof(deviceName) - 1] = '\0';

    // Initialize Bluetooth Serial
    if (!btSerial.begin(deviceName)) {
        LOG_ERROR(TAG, "Bluetooth initialization failed!");
        state = BT_STATE_ERROR;
        return false;
    }

    LOG_INFO(TAG, "Bluetooth initialized successfully");
    LOG_INFO(TAG, "Device name: %s", deviceName);
    LOG_INFO(TAG, "MAC Address: %s", BluetoothUtils::getMacAddress().c_str());

    changeState(BT_STATE_ADVERTISING);

    return true;
}

void BluetoothModule::update() {
    // Check connection status
    bool connected = btSerial.hasClient();

    // Handle state transitions based on connection
    if (connected && state != BT_STATE_CONNECTED) {
        changeState(BT_STATE_CONNECTED);
    } else if (!connected && state == BT_STATE_CONNECTED) {
        handleDisconnection();
    }

    // Send periodic heartbeat when connected
    if (state == BT_STATE_CONNECTED) {
        if (millis() - lastHeartbeat > 60000) {  // Every 60 seconds
            sendHeartbeat();
        }
    }

    // Handle incoming data (for future bidirectional communication)
    if (btSerial.available()) {
        // TODO: Implement command reception if needed
        char c = btSerial.read();
        LOG_DEBUG(TAG, "Received: %c", c);
    }
}

bool BluetoothModule::isConnected() {
    return (state == BT_STATE_CONNECTED) && btSerial.hasClient();
}

bool BluetoothModule::sendMessage(const char* message) {
    if (!message) {
        LOG_ERROR(TAG, "Null message pointer");
        return false;
    }

    if (!isConnected()) {
        LOG_WARN(TAG, "Cannot send message - not connected");
        return false;
    }

    size_t messageLen = strlen(message);
    size_t sent = btSerial.print(message);
    btSerial.print("\n");  // Add newline terminator
    sent++;

    if (sent == messageLen + 1) {
        messagesSent++;
        bytesSent += sent;

        #if LOG_BT_EVENTS
        LOG_INFO(TAG, "Message sent (%d bytes): %s", sent, message);
        #endif

        notifyEvent(BT_EVENT_DATA_SENT);
        return true;
    } else {
        LOG_ERROR(TAG, "Message send failed (sent %d of %d bytes)",
                  sent, messageLen + 1);
        return false;
    }
}

BluetoothState BluetoothModule::getState() {
    return state;
}

const char* BluetoothModule::getConnectedDeviceName() {
    if (isConnected()) {
        return connectedDeviceName;
    }
    return "";
}

uint32_t BluetoothModule::getMessagesSent() {
    return messagesSent;
}

uint32_t BluetoothModule::getConnectionAttempts() {
    return connectionAttempts;
}

void BluetoothModule::disconnect() {
    if (state == BT_STATE_CONNECTED) {
        LOG_INFO(TAG, "Disconnecting...");
        btSerial.disconnect();
        changeState(BT_STATE_DISCONNECTED);
    }
}

bool BluetoothModule::restart() {
    LOG_INFO(TAG, "Restarting Bluetooth...");

    btSerial.end();
    delay(1000);

    bool success = begin(deviceName);

    if (success) {
        LOG_INFO(TAG, "Bluetooth restarted successfully");
    } else {
        LOG_ERROR(TAG, "Bluetooth restart failed");
    }

    return success;
}

void BluetoothModule::setDiscoverable(bool enable) {
    discoverable = enable;
    // ESP32 BluetoothSerial is always discoverable when advertising
    LOG_INFO(TAG, "Discoverable: %s", enable ? "YES" : "NO");
}

void BluetoothModule::printDiagnostics() {
    LOG_INFO(TAG, "=== Bluetooth Diagnostics ===");
    LOG_INFO(TAG, "Device Name: %s", deviceName);
    LOG_INFO(TAG, "MAC Address: %s", BluetoothUtils::getMacAddress().c_str());
    LOG_INFO(TAG, "State: %d", state);
    LOG_INFO(TAG, "Connected: %s", isConnected() ? "YES" : "NO");

    if (isConnected()) {
        LOG_INFO(TAG, "Connected to: %s", connectedDeviceName);
    }

    LOG_INFO(TAG, "Messages Sent: %lu", messagesSent);
    LOG_INFO(TAG, "Bytes Sent: %lu", bytesSent);
    LOG_INFO(TAG, "Connection Attempts: %lu", connectionAttempts);
    LOG_INFO(TAG, "Disconnects: %lu", disconnectCount);
    LOG_INFO(TAG, "============================");
}

void BluetoothModule::setEventCallback(EventCallback callback) {
    eventCallback = callback;
}

void BluetoothModule::changeState(BluetoothState newState) {
    if (state == newState) {
        return;
    }

    previousState = state;
    state = newState;
    lastStateChange = millis();

    const char* stateNames[] = {
        "UNINITIALIZED", "INITIALIZING", "IDLE", "ADVERTISING",
        "CONNECTING", "CONNECTED", "DISCONNECTED", "ERROR"
    };

    LOG_INFO(TAG, "State: %s -> %s",
             stateNames[previousState], stateNames[newState]);

    handleStateTransition();
}

void BluetoothModule::handleStateTransition() {
    switch (state) {
        case BT_STATE_CONNECTED:
            LOG_INFO(TAG, "Bluetooth connection established");
            connectionAttempts++;
            // TODO: Get connected device name from btSerial
            strncpy(connectedDeviceName, "Unknown", sizeof(connectedDeviceName) - 1);
            notifyEvent(BT_EVENT_CONNECTED);
            break;

        case BT_STATE_DISCONNECTED:
            LOG_INFO(TAG, "Bluetooth disconnected");
            disconnectCount++;
            notifyEvent(BT_EVENT_DISCONNECTED);
            // Return to advertising
            changeState(BT_STATE_ADVERTISING);
            break;

        case BT_STATE_ADVERTISING:
            LOG_INFO(TAG, "Bluetooth advertising (discoverable)");
            break;

        case BT_STATE_ERROR:
            LOG_ERROR(TAG, "Bluetooth error state");
            notifyEvent(BT_EVENT_ERROR);
            break;

        default:
            break;
    }
}

bool BluetoothModule::checkConnection() {
    return btSerial.hasClient();
}

void BluetoothModule::handleDisconnection() {
    LOG_WARN(TAG, "Connection lost");
    changeState(BT_STATE_DISCONNECTED);
}

void BluetoothModule::attemptReconnection() {
    // Bluetooth Serial will automatically accept new connections
    // No explicit reconnection needed
    if (state != BT_STATE_ADVERTISING) {
        changeState(BT_STATE_ADVERTISING);
    }
}

void BluetoothModule::sendHeartbeat() {
    // Send a simple heartbeat message
    const char* heartbeat = "HEARTBEAT";
    if (btSerial.println(heartbeat)) {
        LOG_DEBUG(TAG, "Heartbeat sent");
    }
    lastHeartbeat = millis();
}

void BluetoothModule::notifyEvent(BluetoothEvent event) {
    if (eventCallback) {
        eventCallback(event);
    }
}

// BluetoothUtils namespace implementation
namespace BluetoothUtils {
    String getMacAddress() {
        uint8_t mac[6];
        esp_read_mac(mac, ESP_MAC_BT);

        char macStr[18];
        snprintf(macStr, sizeof(macStr), "%02X:%02X:%02X:%02X:%02X:%02X",
                 mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);

        return String(macStr);
    }

    bool isBluetoothSupported() {
        #if defined(CONFIG_BT_ENABLED) && defined(CONFIG_BLUEDROID_ENABLED)
        return true;
        #else
        return false;
        #endif
    }

    int getSignalStrength() {
        // TODO: Implement RSSI reading if supported
        return -1;  // Not implemented
    }

    void formatDiagnostics(char* buffer, size_t bufferSize) {
        snprintf(buffer, bufferSize,
                 "BT MAC: %s | Supported: %s",
                 getMacAddress().c_str(),
                 isBluetoothSupported() ? "YES" : "NO");
    }
}
