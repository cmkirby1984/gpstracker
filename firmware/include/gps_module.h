#ifndef GPS_MODULE_H
#define GPS_MODULE_H

#include <Arduino.h>
#include <TinyGPSPlus.h>
#include "config.h"
#include "message_protocol.h"

// ============================================================================
// GPS MODULE INTERFACE
// ============================================================================
// Manages NEO-6M GPS module communication and data parsing
// Supports both real GPS hardware and mock simulation mode
// ============================================================================

// GPS status enumeration
enum GpsStatus {
    GPS_STATUS_UNINITIALIZED,
    GPS_STATUS_INITIALIZING,
    GPS_STATUS_NO_FIX,
    GPS_STATUS_FIX_ACQUIRED,
    GPS_STATUS_ERROR,
    GPS_STATUS_MOCK_MODE
};

// GPS module class
class GpsModule {
public:
    GpsModule();
    ~GpsModule();

    // Initialize GPS module
    // Returns: true if initialization successful
    bool begin();

    // Update GPS data (call regularly in loop)
    // Reads available GPS data from serial port
    void update();

    // Check if GPS has valid fix
    // Returns: true if GPS fix is valid and location data is available
    bool hasFix();

    // Get current GPS data
    // Returns: true if data is valid and copied to output parameter
    bool getData(GpsData& data);

    // Get current GPS status
    GpsStatus getStatus();

    // Get number of satellites in view
    uint8_t getSatellites();

    // Get HDOP (horizontal dilution of precision)
    // Lower is better (< 5 is good)
    double getHdop();

    // Get time since last valid GPS data
    // Returns: milliseconds since last update
    unsigned long getLastUpdateAge();

    // Force GPS module reset
    void reset();

    // Enable/disable mock mode at runtime
    void setMockMode(bool enable);

    // Update mock GPS coordinates (for testing)
    void setMockCoordinates(double lat, double lon, double heading);

    // Get diagnostic information
    void printDiagnostics();

private:
    TinyGPSPlus gpsParser;           // TinyGPS++ parser instance
    HardwareSerial* gpsSerial;       // Serial port for GPS
    GpsStatus status;
    unsigned long lastFixTime;
    unsigned long lastUpdateTime;
    bool mockMode;

    // Mock GPS data
    double mockLat;
    double mockLon;
    double mockHeading;
    uint32_t mockUpdateCounter;

    // Initialize hardware serial for GPS
    bool initHardware();

    // Read and parse GPS data from serial
    bool readGpsData();

    // Generate mock GPS data
    void generateMockData(GpsData& data);

    // Validate GPS fix quality
    bool isFixValid();

    // Convert GPS time to timestamp
    void buildTimestamp(char* buffer, size_t bufferSize);
};

// GPS utility functions
namespace GpsUtils {
    // Calculate distance between two GPS coordinates (meters)
    double calculateDistance(double lat1, double lon1, double lat2, double lon2);

    // Calculate bearing between two GPS coordinates (degrees)
    double calculateBearing(double lat1, double lon1, double lat2, double lon2);

    // Convert GPS coordinates to human-readable format
    void formatCoordinates(double lat, double lon, char* buffer, size_t bufferSize);

    // Check if coordinates are within valid range
    bool isValidCoordinate(double lat, double lon);
}

#endif // GPS_MODULE_H
