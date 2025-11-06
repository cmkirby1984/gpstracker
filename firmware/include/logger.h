#ifndef LOGGER_H
#define LOGGER_H

#include <Arduino.h>
#include "config.h"

// ============================================================================
// DEBUG LOGGING SYSTEM
// ============================================================================
// Provides unified logging interface with severity levels
// Usage: LOG_INFO("GPS fix acquired: %d satellites", satCount);
// ============================================================================

// Log Level Definitions
#define LOG_LEVEL_OFF     0
#define LOG_LEVEL_ERROR   1
#define LOG_LEVEL_WARN    2
#define LOG_LEVEL_INFO    3
#define LOG_LEVEL_DEBUG   4
#define LOG_LEVEL_VERBOSE 5

// ANSI Color Codes for Serial Terminal
#define COLOR_RESET   "\033[0m"
#define COLOR_RED     "\033[31m"
#define COLOR_YELLOW  "\033[33m"
#define COLOR_GREEN   "\033[32m"
#define COLOR_CYAN    "\033[36m"
#define COLOR_MAGENTA "\033[35m"

// Logger class for centralized logging
class Logger {
public:
    static void init(int baudRate = 115200);
    static void log(int level, const char* tag, const char* format, ...);
    static void logRaw(const char* data);
    static void logHex(const char* tag, const uint8_t* data, size_t length);

    static const char* getLevelString(int level);
    static const char* getLevelColor(int level);

private:
    static bool initialized;
};

// Convenience macros for logging
#if LOG_LEVEL >= LOG_LEVEL_ERROR
    #define LOG_ERROR(tag, fmt, ...) Logger::log(LOG_LEVEL_ERROR, tag, fmt, ##__VA_ARGS__)
#else
    #define LOG_ERROR(tag, fmt, ...)
#endif

#if LOG_LEVEL >= LOG_LEVEL_WARN
    #define LOG_WARN(tag, fmt, ...) Logger::log(LOG_LEVEL_WARN, tag, fmt, ##__VA_ARGS__)
#else
    #define LOG_WARN(tag, fmt, ...)
#endif

#if LOG_LEVEL >= LOG_LEVEL_INFO
    #define LOG_INFO(tag, fmt, ...) Logger::log(LOG_LEVEL_INFO, tag, fmt, ##__VA_ARGS__)
#else
    #define LOG_INFO(tag, fmt, ...)
#endif

#if LOG_LEVEL >= LOG_LEVEL_DEBUG
    #define LOG_DEBUG(tag, fmt, ...) Logger::log(LOG_LEVEL_DEBUG, tag, fmt, ##__VA_ARGS__)
#else
    #define LOG_DEBUG(tag, fmt, ...)
#endif

#if LOG_LEVEL >= LOG_LEVEL_VERBOSE
    #define LOG_VERBOSE(tag, fmt, ...) Logger::log(LOG_LEVEL_VERBOSE, tag, fmt, ##__VA_ARGS__)
#else
    #define LOG_VERBOSE(tag, fmt, ...)
#endif

// Simplified macros without tag (uses file name)
#define LOG_E(fmt, ...) LOG_ERROR(__FILE__, fmt, ##__VA_ARGS__)
#define LOG_W(fmt, ...) LOG_WARN(__FILE__, fmt, ##__VA_ARGS__)
#define LOG_I(fmt, ...) LOG_INFO(__FILE__, fmt, ##__VA_ARGS__)
#define LOG_D(fmt, ...) LOG_DEBUG(__FILE__, fmt, ##__VA_ARGS__)
#define LOG_V(fmt, ...) LOG_VERBOSE(__FILE__, fmt, ##__VA_ARGS__)

// Special purpose logging
#define LOG_RAW(data) Logger::logRaw(data)
#define LOG_HEX(tag, data, len) Logger::logHex(tag, data, len)

#endif // LOGGER_H
