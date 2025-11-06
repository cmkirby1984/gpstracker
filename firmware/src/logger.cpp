#include "logger.h"
#include <stdarg.h>

bool Logger::initialized = false;

void Logger::init(int baudRate) {
    if (!initialized) {
        Serial.begin(baudRate);
        // Wait for serial port to connect (useful for native USB ports)
        unsigned long startTime = millis();
        while (!Serial && (millis() - startTime < 2000)) {
            delay(10);
        }
        initialized = true;

        Serial.println();
        Serial.println(F("========================================"));
        Serial.println(F("  Fleet GPS Tracker System"));
        Serial.print(F("  Firmware: "));
        Serial.println(FIRMWARE_VERSION);
        Serial.print(F("  Build: "));
        Serial.print(BUILD_DATE);
        Serial.print(F(" "));
        Serial.println(BUILD_TIME);
        Serial.print(F("  Device: "));
        Serial.println(DEVICE_SERIAL);
        Serial.println(F("========================================"));
        Serial.println();

        #if USE_MOCK_GPS
        Serial.println(F("[WARNING] Mock GPS mode enabled"));
        #endif
    }
}

void Logger::log(int level, const char* tag, const char* format, ...) {
    if (!initialized) {
        init(SERIAL_BAUD_RATE);
    }

    if (level > LOG_LEVEL) {
        return;  // Skip if log level too verbose
    }

    // Print timestamp
    Serial.print(F("["));
    Serial.print(millis());
    Serial.print(F("] "));

    // Print level with color
    Serial.print(getLevelColor(level));
    Serial.print(getLevelString(level));
    Serial.print(COLOR_RESET);
    Serial.print(F(" "));

    // Print tag
    Serial.print(F("["));
    Serial.print(tag);
    Serial.print(F("] "));

    // Print formatted message
    char buffer[256];
    va_list args;
    va_start(args, format);
    vsnprintf(buffer, sizeof(buffer), format, args);
    va_end(args);

    Serial.println(buffer);
}

void Logger::logRaw(const char* data) {
    if (!initialized) {
        init(SERIAL_BAUD_RATE);
    }
    Serial.print(data);
}

void Logger::logHex(const char* tag, const uint8_t* data, size_t length) {
    if (!initialized) {
        init(SERIAL_BAUD_RATE);
    }

    Serial.print(F("[HEX] ["));
    Serial.print(tag);
    Serial.print(F("] "));

    for (size_t i = 0; i < length; i++) {
        if (data[i] < 0x10) {
            Serial.print(F("0"));
        }
        Serial.print(data[i], HEX);
        Serial.print(F(" "));

        if ((i + 1) % 16 == 0) {
            Serial.println();
            Serial.print(F("      "));
        }
    }
    Serial.println();
}

const char* Logger::getLevelString(int level) {
    switch (level) {
        case LOG_LEVEL_ERROR:   return "ERROR  ";
        case LOG_LEVEL_WARN:    return "WARN   ";
        case LOG_LEVEL_INFO:    return "INFO   ";
        case LOG_LEVEL_DEBUG:   return "DEBUG  ";
        case LOG_LEVEL_VERBOSE: return "VERBOSE";
        default:                return "UNKNOWN";
    }
}

const char* Logger::getLevelColor(int level) {
    switch (level) {
        case LOG_LEVEL_ERROR:   return COLOR_RED;
        case LOG_LEVEL_WARN:    return COLOR_YELLOW;
        case LOG_LEVEL_INFO:    return COLOR_GREEN;
        case LOG_LEVEL_DEBUG:   return COLOR_CYAN;
        case LOG_LEVEL_VERBOSE: return COLOR_MAGENTA;
        default:                return COLOR_RESET;
    }
}
