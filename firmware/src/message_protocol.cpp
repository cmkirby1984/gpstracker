#include "message_protocol.h"
#include "logger.h"
#include <string.h>
#include <stdio.h>

static const char* TAG = "MessageProtocol";

bool MessageProtocol::formatMessage(const GpsData& data, char* buffer, size_t bufferSize) {
    if (!buffer || bufferSize < MAX_MESSAGE_LENGTH) {
        LOG_ERROR(TAG, "Invalid buffer parameters");
        return false;
    }

    // Validate data
    if (!data.valid) {
        LOG_WARN(TAG, "Attempting to format invalid GPS data");
    }

    if (!validateCoordinates(data.latitude, data.longitude)) {
        LOG_ERROR(TAG, "Invalid coordinates: lat=%.6f, lon=%.6f",
                  data.latitude, data.longitude);
        return false;
    }

    // Format message: SERIAL|LAT|LON|HEADING|TIME|END
    int written = snprintf(buffer, bufferSize,
                          "%s|%.6f|%.6f|%.2f|%s|END",
                          data.deviceSerial,
                          data.latitude,
                          data.longitude,
                          data.heading,
                          data.timestamp);

    if (written < 0 || written >= (int)bufferSize) {
        LOG_ERROR(TAG, "Message formatting failed or truncated");
        return false;
    }

    #if LOG_MESSAGE_PROTOCOL
    LOG_DEBUG(TAG, "Formatted message: %s", buffer);
    #endif

    return true;
}

bool MessageProtocol::parseMessage(const char* message, GpsData& data) {
    if (!message) {
        LOG_ERROR(TAG, "Null message pointer");
        return false;
    }

    // Validate message format
    if (!validateMessage(message)) {
        LOG_ERROR(TAG, "Message validation failed");
        return false;
    }

    // Create mutable copy for parsing
    char messageCopy[MAX_MESSAGE_LENGTH];
    strncpy(messageCopy, message, MAX_MESSAGE_LENGTH - 1);
    messageCopy[MAX_MESSAGE_LENGTH - 1] = '\0';

    // Parse fields
    char* token;
    int fieldIndex = 0;

    // Field 0: Device Serial
    token = strtok(messageCopy, "|");
    if (token) {
        strncpy(data.deviceSerial, token, MAX_SERIAL_LENGTH - 1);
        data.deviceSerial[MAX_SERIAL_LENGTH - 1] = '\0';
        fieldIndex++;
    }

    // Field 1: Latitude
    token = strtok(NULL, "|");
    if (token) {
        data.latitude = atof(token);
        fieldIndex++;
    }

    // Field 2: Longitude
    token = strtok(NULL, "|");
    if (token) {
        data.longitude = atof(token);
        fieldIndex++;
    }

    // Field 3: Heading
    token = strtok(NULL, "|");
    if (token) {
        data.heading = atof(token);
        fieldIndex++;
    }

    // Field 4: Timestamp
    token = strtok(NULL, "|");
    if (token) {
        strncpy(data.timestamp, token, MAX_TIMESTAMP_LENGTH - 1);
        data.timestamp[MAX_TIMESTAMP_LENGTH - 1] = '\0';
        fieldIndex++;
    }

    // Field 5: END marker
    token = strtok(NULL, "|");
    if (!token || strcmp(token, "END") != 0) {
        LOG_ERROR(TAG, "Missing or invalid END marker");
        return false;
    }

    // Validate parsed data
    if (fieldIndex < MIN_MESSAGE_FIELDS) {
        LOG_ERROR(TAG, "Insufficient fields parsed: %d", fieldIndex);
        return false;
    }

    if (!validateCoordinates(data.latitude, data.longitude)) {
        LOG_ERROR(TAG, "Parsed invalid coordinates");
        return false;
    }

    data.valid = true;

    #if LOG_MESSAGE_PROTOCOL
    LOG_DEBUG(TAG, "Parsed message successfully");
    #endif

    return true;
}

bool MessageProtocol::validateMessage(const char* message) {
    if (!message) {
        return false;
    }

    size_t len = strlen(message);
    if (len < 20 || len >= MAX_MESSAGE_LENGTH) {
        LOG_ERROR(TAG, "Invalid message length: %d", len);
        return false;
    }

    // Check for END terminator
    if (strstr(message, "END") == NULL) {
        LOG_ERROR(TAG, "Missing END terminator");
        return false;
    }

    // Count delimiters (should be 5: SERIAL|LAT|LON|HEADING|TIME|END)
    int delimiterCount = 0;
    for (size_t i = 0; i < len; i++) {
        if (message[i] == MESSAGE_DELIMITER) {
            delimiterCount++;
        }
    }

    if (delimiterCount != 5) {
        LOG_ERROR(TAG, "Invalid delimiter count: %d (expected 5)", delimiterCount);
        return false;
    }

    return true;
}

uint16_t MessageProtocol::calculateChecksum(const char* message) {
    uint16_t checksum = 0;
    while (*message) {
        checksum ^= (uint8_t)(*message++);
    }
    return checksum;
}

void MessageProtocol::formatTimestamp(int year, int month, int day,
                                     int hour, int minute, int second,
                                     char* buffer, size_t bufferSize) {
    // Format: YYYY-MM-DDTHH:MM:SSZ (ISO 8601)
    snprintf(buffer, bufferSize, "%04d-%02d-%02dT%02d:%02d:%02dZ",
             year, month, day, hour, minute, second);
}

void MessageProtocol::getCurrentTimestamp(char* buffer, size_t bufferSize) {
    // TODO: Implement RTC support for accurate timestamps
    // For now, use millis() as a placeholder
    unsigned long currentMillis = millis();
    unsigned long seconds = currentMillis / 1000;
    unsigned long minutes = seconds / 60;
    unsigned long hours = minutes / 60;

    int h = hours % 24;
    int m = minutes % 60;
    int s = seconds % 60;

    // Placeholder format (not actual wall clock time)
    snprintf(buffer, bufferSize, "1970-01-01T%02d:%02d:%02dZ", h, m, s);
}

bool MessageProtocol::validateCoordinates(double lat, double lon) {
    // Latitude: -90 to +90
    // Longitude: -180 to +180
    if (lat < -90.0 || lat > 90.0) {
        LOG_ERROR(TAG, "Latitude out of range: %.6f", lat);
        return false;
    }

    if (lon < -180.0 || lon > 180.0) {
        LOG_ERROR(TAG, "Longitude out of range: %.6f", lon);
        return false;
    }

    return true;
}

void MessageProtocol::formatCoordinate(double coord, char* buffer,
                                      size_t bufferSize, int precision) {
    snprintf(buffer, bufferSize, "%.*f", precision, coord);
}

int MessageProtocol::countFields(const char* message) {
    int count = 1;  // Start at 1 (at least one field if string not empty)
    for (size_t i = 0; message[i] != '\0'; i++) {
        if (message[i] == MESSAGE_DELIMITER) {
            count++;
        }
    }
    return count;
}

bool MessageProtocol::extractField(const char* message, int fieldIndex,
                                  char* buffer, size_t bufferSize) {
    // Implementation for extracting specific field
    // (Helper function for future use)
    // TODO: Implement if needed
    return false;
}

// MessageUtils namespace implementation
namespace MessageUtils {
    void sanitizeString(char* str) {
        if (!str) return;

        size_t len = strlen(str);
        for (size_t i = 0; i < len; i++) {
            // Remove control characters
            if (str[i] < 32 && str[i] != '\n' && str[i] != '\r' && str[i] != '\t') {
                str[i] = ' ';
            }
            // Remove delimiter character to prevent injection
            if (str[i] == MESSAGE_DELIMITER) {
                str[i] = '-';
            }
        }
    }

    void escapeString(const char* input, char* output, size_t outputSize) {
        // Simple implementation - copy and sanitize
        strncpy(output, input, outputSize - 1);
        output[outputSize - 1] = '\0';
        sanitizeString(output);
    }

    void trimString(char* str) {
        if (!str) return;

        // Trim leading whitespace
        size_t len = strlen(str);
        size_t start = 0;
        while (start < len && isspace((unsigned char)str[start])) {
            start++;
        }

        // Shift string left
        if (start > 0) {
            memmove(str, str + start, len - start + 1);
            len -= start;
        }

        // Trim trailing whitespace
        while (len > 0 && isspace((unsigned char)str[len - 1])) {
            str[--len] = '\0';
        }
    }
}
