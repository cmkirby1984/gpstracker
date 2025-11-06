#include "gps_module.h"
#include "logger.h"
#include <math.h>

static const char* TAG = "GPS";

GpsModule::GpsModule()
    : gpsSerial(nullptr),
      status(GPS_STATUS_UNINITIALIZED),
      lastFixTime(0),
      lastUpdateTime(0),
      mockMode(USE_MOCK_GPS),
      mockLat(MOCK_GPS_LAT),
      mockLon(MOCK_GPS_LON),
      mockHeading(MOCK_GPS_HEADING),
      mockUpdateCounter(0) {
}

GpsModule::~GpsModule() {
    if (gpsSerial) {
        delete gpsSerial;
        gpsSerial = nullptr;
    }
}

bool GpsModule::begin() {
    LOG_INFO(TAG, "Initializing GPS module...");
    status = GPS_STATUS_INITIALIZING;

    #if USE_MOCK_GPS
    mockMode = true;
    status = GPS_STATUS_MOCK_MODE;
    LOG_WARN(TAG, "GPS initialized in MOCK MODE");
    LOG_INFO(TAG, "Mock coordinates: %.6f, %.6f", mockLat, mockLon);
    return true;
    #else
    mockMode = false;
    return initHardware();
    #endif
}

bool GpsModule::initHardware() {
    LOG_INFO(TAG, "Initializing GPS hardware on Serial2");
    LOG_INFO(TAG, "GPS RX Pin: %d, TX Pin: %d", GPS_RX_PIN, GPS_TX_PIN);

    // Initialize serial port for GPS
    gpsSerial = new HardwareSerial(2);  // Use Serial2 on ESP32
    gpsSerial->begin(GPS_BAUD_RATE, SERIAL_8N1, GPS_RX_PIN, GPS_TX_PIN);

    // Wait for GPS module to initialize
    delay(1000);

    // Check if GPS is responding
    unsigned long startTime = millis();
    bool gpsResponding = false;

    LOG_INFO(TAG, "Waiting for GPS data...");

    while (millis() - startTime < 5000) {  // Wait up to 5 seconds
        while (gpsSerial->available()) {
            char c = gpsSerial->read();
            gpsParser.encode(c);

            #if LOG_GPS_SENTENCES
            if (c == '\n') {
                LOG_VERBOSE(TAG, "GPS data received");
            }
            #endif

            gpsResponding = true;
        }

        if (gpsResponding) {
            break;
        }
        delay(100);
    }

    if (gpsResponding) {
        status = GPS_STATUS_NO_FIX;
        LOG_INFO(TAG, "GPS module responding, waiting for fix...");
        return true;
    } else {
        status = GPS_STATUS_ERROR;
        LOG_ERROR(TAG, "GPS module not responding!");
        LOG_ERROR(TAG, "Check wiring and connections");
        return false;
    }
}

void GpsModule::update() {
    if (mockMode) {
        // In mock mode, just update timestamp
        lastUpdateTime = millis();
        return;
    }

    if (!gpsSerial) {
        return;
    }

    // Read available GPS data
    while (gpsSerial->available() > 0) {
        char c = gpsSerial->read();
        gpsParser.encode(c);

        #if LOG_GPS_SENTENCES && LOG_LEVEL >= LOG_LEVEL_VERBOSE
        Serial.write(c);  // Echo raw GPS data
        #endif
    }

    // Update status based on fix
    if (gpsParser.location.isValid()) {
        if (status != GPS_STATUS_FIX_ACQUIRED) {
            status = GPS_STATUS_FIX_ACQUIRED;
            LOG_INFO(TAG, "GPS fix acquired!");
            LOG_INFO(TAG, "Location: %.6f, %.6f",
                     gpsParser.location.lat(),
                     gpsParser.location.lng());
        }
        lastFixTime = millis();
        lastUpdateTime = millis();
    } else {
        // Check if we've lost fix
        if (status == GPS_STATUS_FIX_ACQUIRED &&
            (millis() - lastFixTime > GPS_FIX_TIMEOUT_MS)) {
            status = GPS_STATUS_NO_FIX;
            LOG_WARN(TAG, "GPS fix lost");
        }
    }
}

bool GpsModule::hasFix() {
    if (mockMode) {
        return true;  // Mock mode always has "fix"
    }

    return (status == GPS_STATUS_FIX_ACQUIRED) &&
           gpsParser.location.isValid() &&
           (millis() - lastFixTime < GPS_FIX_TIMEOUT_MS);
}

bool GpsModule::getData(GpsData& data) {
    if (mockMode) {
        generateMockData(data);
        return true;
    }

    if (!hasFix()) {
        LOG_WARN(TAG, "No GPS fix available");
        data.valid = false;
        return false;
    }

    // Populate GPS data structure
    strncpy(data.deviceSerial, DEVICE_SERIAL, MAX_SERIAL_LENGTH - 1);
    data.deviceSerial[MAX_SERIAL_LENGTH - 1] = '\0';

    data.latitude = gpsParser.location.lat();
    data.longitude = gpsParser.location.lng();
    data.heading = gpsParser.course.deg();
    data.speed = gpsParser.speed.kmph();
    data.altitude = gpsParser.altitude.meters();
    data.satellites = gpsParser.satellites.value();

    // Build timestamp from GPS time
    buildTimestamp(data.timestamp, MAX_TIMESTAMP_LENGTH);

    data.valid = true;

    LOG_DEBUG(TAG, "GPS Data: %.6f, %.6f, heading=%.2f, sats=%d",
              data.latitude, data.longitude, data.heading, data.satellites);

    return true;
}

GpsStatus GpsModule::getStatus() {
    return status;
}

uint8_t GpsModule::getSatellites() {
    if (mockMode) {
        return 8;  // Mock 8 satellites
    }
    return gpsParser.satellites.value();
}

double GpsModule::getHdop() {
    if (mockMode) {
        return 1.2;  // Mock good HDOP
    }
    return gpsParser.hdop.hdop();
}

unsigned long GpsModule::getLastUpdateAge() {
    return millis() - lastUpdateTime;
}

void GpsModule::reset() {
    LOG_INFO(TAG, "Resetting GPS module...");

    if (gpsSerial) {
        gpsSerial->end();
        delay(100);
        gpsSerial->begin(GPS_BAUD_RATE, SERIAL_8N1, GPS_RX_PIN, GPS_TX_PIN);
    }

    status = GPS_STATUS_INITIALIZING;
    lastFixTime = 0;
    lastUpdateTime = 0;

    LOG_INFO(TAG, "GPS reset complete");
}

void GpsModule::setMockMode(bool enable) {
    mockMode = enable;
    if (enable) {
        status = GPS_STATUS_MOCK_MODE;
        LOG_INFO(TAG, "Mock mode enabled");
    } else {
        initHardware();
    }
}

void GpsModule::setMockCoordinates(double lat, double lon, double heading) {
    mockLat = lat;
    mockLon = lon;
    mockHeading = heading;
    LOG_INFO(TAG, "Mock coordinates updated: %.6f, %.6f, heading=%.2f",
             lat, lon, heading);
}

void GpsModule::generateMockData(GpsData& data) {
    // Generate realistic-looking mock GPS data
    strncpy(data.deviceSerial, DEVICE_SERIAL, MAX_SERIAL_LENGTH - 1);
    data.deviceSerial[MAX_SERIAL_LENGTH - 1] = '\0';

    // Add slight variation to coordinates (simulate movement)
    double variation = (mockUpdateCounter % 100) * 0.0001;
    data.latitude = mockLat + variation;
    data.longitude = mockLon + variation;
    data.heading = mockHeading;
    data.speed = 0.0;
    data.altitude = MOCK_GPS_ALTITUDE;
    data.satellites = 8;

    // Generate timestamp
    MessageProtocol::getCurrentTimestamp(data.timestamp, MAX_TIMESTAMP_LENGTH);

    data.valid = true;
    mockUpdateCounter++;

    LOG_DEBUG(TAG, "Mock GPS Data: %.6f, %.6f", data.latitude, data.longitude);
}

bool GpsModule::isFixValid() {
    if (mockMode) {
        return true;
    }

    if (!gpsParser.location.isValid()) {
        return false;
    }

    // Check satellite count
    if (gpsParser.satellites.value() < MIN_SATELLITES) {
        LOG_WARN(TAG, "Insufficient satellites: %d (min %d)",
                 gpsParser.satellites.value(), MIN_SATELLITES);
        return false;
    }

    // Check HDOP
    double hdop = gpsParser.hdop.hdop();
    if (hdop > MAX_HDOP) {
        LOG_WARN(TAG, "Poor HDOP: %.2f (max %.2f)", hdop, MAX_HDOP);
        return false;
    }

    return true;
}

void GpsModule::buildTimestamp(char* buffer, size_t bufferSize) {
    if (gpsParser.date.isValid() && gpsParser.time.isValid()) {
        // Use actual GPS time
        MessageProtocol::formatTimestamp(
            gpsParser.date.year(),
            gpsParser.date.month(),
            gpsParser.date.day(),
            gpsParser.time.hour(),
            gpsParser.time.minute(),
            gpsParser.time.second(),
            buffer,
            bufferSize
        );
    } else {
        // Fallback to system time
        MessageProtocol::getCurrentTimestamp(buffer, bufferSize);
    }
}

void GpsModule::printDiagnostics() {
    LOG_INFO(TAG, "=== GPS Diagnostics ===");
    LOG_INFO(TAG, "Status: %d", status);
    LOG_INFO(TAG, "Mock Mode: %s", mockMode ? "YES" : "NO");

    if (mockMode) {
        LOG_INFO(TAG, "Mock Coordinates: %.6f, %.6f", mockLat, mockLon);
    } else {
        LOG_INFO(TAG, "Has Fix: %s", hasFix() ? "YES" : "NO");
        LOG_INFO(TAG, "Location Valid: %s",
                 gpsParser.location.isValid() ? "YES" : "NO");
        LOG_INFO(TAG, "Satellites: %d", gpsParser.satellites.value());
        LOG_INFO(TAG, "HDOP: %.2f", gpsParser.hdop.hdop());

        if (gpsParser.location.isValid()) {
            LOG_INFO(TAG, "Latitude: %.6f", gpsParser.location.lat());
            LOG_INFO(TAG, "Longitude: %.6f", gpsParser.location.lng());
            LOG_INFO(TAG, "Altitude: %.2f m", gpsParser.altitude.meters());
            LOG_INFO(TAG, "Speed: %.2f km/h", gpsParser.speed.kmph());
            LOG_INFO(TAG, "Course: %.2f deg", gpsParser.course.deg());
        }

        LOG_INFO(TAG, "Chars Processed: %d", gpsParser.charsProcessed());
        LOG_INFO(TAG, "Sentences with Fix: %d", gpsParser.sentencesWithFix());
        LOG_INFO(TAG, "Failed Checksum: %d", gpsParser.failedChecksum());
    }

    LOG_INFO(TAG, "Last Update Age: %lu ms", getLastUpdateAge());
    LOG_INFO(TAG, "=====================");
}

// GpsUtils namespace implementation
namespace GpsUtils {
    double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Haversine formula for distance calculation
        const double R = 6371000.0;  // Earth radius in meters

        double phi1 = lat1 * DEG_TO_RAD;
        double phi2 = lat2 * DEG_TO_RAD;
        double deltaPhi = (lat2 - lat1) * DEG_TO_RAD;
        double deltaLambda = (lon2 - lon1) * DEG_TO_RAD;

        double a = sin(deltaPhi / 2.0) * sin(deltaPhi / 2.0) +
                   cos(phi1) * cos(phi2) *
                   sin(deltaLambda / 2.0) * sin(deltaLambda / 2.0);

        double c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a));

        return R * c;
    }

    double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        // Calculate bearing between two points
        double phi1 = lat1 * DEG_TO_RAD;
        double phi2 = lat2 * DEG_TO_RAD;
        double deltaLambda = (lon2 - lon1) * DEG_TO_RAD;

        double y = sin(deltaLambda) * cos(phi2);
        double x = cos(phi1) * sin(phi2) -
                   sin(phi1) * cos(phi2) * cos(deltaLambda);

        double bearing = atan2(y, x) * RAD_TO_DEG;

        // Normalize to 0-360
        return fmod((bearing + 360.0), 360.0);
    }

    void formatCoordinates(double lat, double lon, char* buffer, size_t bufferSize) {
        snprintf(buffer, bufferSize, "%.6f, %.6f", lat, lon);
    }

    bool isValidCoordinate(double lat, double lon) {
        return (lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0);
    }
}
