#ifndef MESSAGE_PROTOCOL_H
#define MESSAGE_PROTOCOL_H

#include <Arduino.h>
#include "config.h"

// ============================================================================
// MESSAGE PROTOCOL
// ============================================================================
// Defines the pipe-delimited message format for GPS data transmission
// Format: SERIAL|LAT|LON|HEADING|TIME|END
// Example: GPS001|37.7749|-122.4194|270.5|2025-11-06T23:15:30Z|END
// ============================================================================

// Message field limits
#define MAX_MESSAGE_LENGTH 256
#define MAX_SERIAL_LENGTH 20
#define MAX_TIMESTAMP_LENGTH 32

// Message validation
#define MIN_MESSAGE_FIELDS 6
#define MAX_MESSAGE_FIELDS 6

// GPS data structure
struct GpsData {
    char deviceSerial[MAX_SERIAL_LENGTH];
    double latitude;
    double longitude;
    double heading;           // Degrees (0-360)
    double speed;             // km/h
    double altitude;          // meters
    char timestamp[MAX_TIMESTAMP_LENGTH];  // ISO 8601 format
    uint8_t satellites;
    bool valid;               // True if GPS fix is valid
};

// Message Protocol class
class MessageProtocol {
public:
    // Format GPS data into protocol message
    // Returns: true if message formatted successfully
    static bool formatMessage(const GpsData& data, char* buffer, size_t bufferSize);

    // Parse protocol message into GPS data
    // Returns: true if message parsed successfully
    static bool parseMessage(const char* message, GpsData& data);

    // Validate message format
    // Returns: true if message format is valid
    static bool validateMessage(const char* message);

    // Calculate message checksum (for future use)
    static uint16_t calculateChecksum(const char* message);

    // Format ISO 8601 timestamp from GPS time
    static void formatTimestamp(int year, int month, int day,
                               int hour, int minute, int second,
                               char* buffer, size_t bufferSize);

    // Get current timestamp (from RTC or system time)
    static void getCurrentTimestamp(char* buffer, size_t bufferSize);

    // Validate coordinate ranges
    static bool validateCoordinates(double lat, double lon);

    // Format latitude/longitude with proper precision
    static void formatCoordinate(double coord, char* buffer, size_t bufferSize, int precision = 6);

private:
    // Helper function to count fields in message
    static int countFields(const char* message);

    // Helper function to extract field from message
    static bool extractField(const char* message, int fieldIndex,
                           char* buffer, size_t bufferSize);
};

// Utility functions for message handling
namespace MessageUtils {
    // Clean string for transmission (remove invalid characters)
    void sanitizeString(char* str);

    // Escape special characters if needed
    void escapeString(const char* input, char* output, size_t outputSize);

    // Trim whitespace
    void trimString(char* str);
}

#endif // MESSAGE_PROTOCOL_H
