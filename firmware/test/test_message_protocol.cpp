#include <Arduino.h>
#include <unity.h>
#include <string.h>
#include "message_protocol.h"

// ============================================================================
// COMPREHENSIVE MESSAGE PROTOCOL TEST SUITE
// Can be run without any hardware - tests all parsing logic
// ============================================================================

// Test helper
GpsData createValidGpsData() {
    GpsData data;
    strcpy(data.deviceSerial, "GPS001");
    data.latitude = 37.7749;
    data.longitude = -122.4194;
    data.heading = 270.5;
    data.speed = 55.3;
    data.altitude = 123.4;
    strcpy(data.timestamp, "2025-11-06T23:15:30Z");
    data.satellites = 8;
    data.valid = true;
    return data;
}

// ============================================================================
// BASIC FUNCTIONALITY TESTS
// ============================================================================

void test_format_valid_message() {
    GpsData data = createValidGpsData();
    char buffer[256];
    bool result = MessageProtocol::formatMessage(data, buffer, sizeof(buffer));
    TEST_ASSERT_TRUE(result);
    TEST_ASSERT_TRUE(strlen(buffer) > 0);
    TEST_ASSERT_TRUE(strstr(buffer, "GPS001") != NULL);
    TEST_ASSERT_TRUE(strstr(buffer, "END") != NULL);
}

void test_parse_valid_message() {
    const char* testMessage = "GPS001|37.774900|-122.419400|270.50|2025-11-06T23:15:30Z|END";
    GpsData data;
    bool result = MessageProtocol::parseMessage(testMessage, data);
    TEST_ASSERT_TRUE(result);
    TEST_ASSERT_EQUAL_STRING("GPS001", data.deviceSerial);
    TEST_ASSERT_FLOAT_WITHIN(0.0001, 37.7749, data.latitude);
    TEST_ASSERT_FLOAT_WITHIN(0.0001, -122.4194, data.longitude);
    TEST_ASSERT_FLOAT_WITHIN(0.01, 270.5, data.heading);
}

void test_round_trip_format_parse() {
    GpsData original = createValidGpsData();
    char buffer[256];
    MessageProtocol::formatMessage(original, buffer, sizeof(buffer));
    GpsData parsed;
    MessageProtocol::parseMessage(buffer, parsed);
    TEST_ASSERT_EQUAL_STRING(original.deviceSerial, parsed.deviceSerial);
    TEST_ASSERT_FLOAT_WITHIN(0.0001, original.latitude, parsed.latitude);
    TEST_ASSERT_FLOAT_WITHIN(0.0001, original.longitude, parsed.longitude);
}

// ============================================================================
// EDGE CASES - COORDINATES
// ============================================================================

void test_coordinates_at_equator() {
    const char* message = "GPS002|0.000000|0.000000|0.00|2025-11-06T12:00:00Z|END";
    GpsData data;
    TEST_ASSERT_TRUE(MessageProtocol::parseMessage(message, data));
    TEST_ASSERT_FLOAT_WITHIN(0.0001, 0.0, data.latitude);
}

void test_coordinates_north_pole() {
    const char* message = "GPS003|90.000000|0.000000|180.00|2025-11-06T12:00:00Z|END";
    GpsData data;
    TEST_ASSERT_TRUE(MessageProtocol::parseMessage(message, data));
    TEST_ASSERT_FLOAT_WITHIN(0.0001, 90.0, data.latitude);
}

void test_coordinates_south_pole() {
    const char* message = "GPS004|-90.000000|0.000000|0.00|2025-11-06T12:00:00Z|END";
    GpsData data;
    TEST_ASSERT_TRUE(MessageProtocol::parseMessage(message, data));
    TEST_ASSERT_FLOAT_WITHIN(0.0001, -90.0, data.latitude);
}

void test_coordinates_date_line() {
    const char* messages[] = {
        "GPS005|0.000000|180.000000|90.00|2025-11-06T12:00:00Z|END",
        "GPS006|0.000000|-180.000000|270.00|2025-11-06T12:00:00Z|END"
    };
    for (int i = 0; i < 2; i++) {
        GpsData data;
        TEST_ASSERT_TRUE(MessageProtocol::parseMessage(messages[i], data));
    }
}

void test_heading_cardinal_directions() {
    const char* messages[] = {
        "GPS010|0.0|0.0|0.00|2025-11-06T12:00:00Z|END",    // North
        "GPS011|0.0|0.0|90.00|2025-11-06T12:00:00Z|END",   // East
        "GPS012|0.0|0.0|180.00|2025-11-06T12:00:00Z|END",  // South
        "GPS013|0.0|0.0|270.00|2025-11-06T12:00:00Z|END"   // West
    };
    double expected[] = {0.0, 90.0, 180.0, 270.0};
    for (int i = 0; i < 4; i++) {
        GpsData data;
        TEST_ASSERT_TRUE(MessageProtocol::parseMessage(messages[i], data));
        TEST_ASSERT_FLOAT_WITHIN(0.01, expected[i], data.heading);
    }
}

// ============================================================================
// INVALID MESSAGE TESTS
// ============================================================================

void test_parse_null_message() {
    GpsData data;
    TEST_ASSERT_FALSE(MessageProtocol::parseMessage(nullptr, data));
}

void test_parse_empty_message() {
    GpsData data;
    TEST_ASSERT_FALSE(MessageProtocol::parseMessage("", data));
}

void test_parse_missing_end_marker() {
    const char* message = "GPS001|37.774900|-122.419400|270.50|2025-11-06T12:00:00Z";
    GpsData data;
    TEST_ASSERT_FALSE(MessageProtocol::parseMessage(message, data));
}

void test_parse_wrong_delimiter_count() {
    const char* message = "GPS001|37.774900|270.50|2025-11-06T12:00:00Z|END";  // Missing longitude
    GpsData data;
    TEST_ASSERT_FALSE(MessageProtocol::parseMessage(message, data));
}

void test_parse_invalid_latitude() {
    const char* messages[] = {
        "GPS001|91.000000|-122.419400|270.50|2025-11-06T12:00:00Z|END",   // > 90
        "GPS001|-91.000000|-122.419400|270.50|2025-11-06T12:00:00Z|END"   // < -90
    };
    for (int i = 0; i < 2; i++) {
        GpsData data;
        TEST_ASSERT_FALSE(MessageProtocol::parseMessage(messages[i], data));
    }
}

void test_parse_invalid_longitude() {
    const char* messages[] = {
        "GPS001|37.774900|181.000000|270.50|2025-11-06T12:00:00Z|END",    // > 180
        "GPS001|37.774900|-181.000000|270.50|2025-11-06T12:00:00Z|END"    // < -180
    };
    for (int i = 0; i < 2; i++) {
        GpsData data;
        TEST_ASSERT_FALSE(MessageProtocol::parseMessage(messages[i], data));
    }
}

// ============================================================================
// VALIDATION TESTS
// ============================================================================

void test_validate_message() {
    TEST_ASSERT_TRUE(MessageProtocol::validateMessage("GPS001|37.7|-122.4|270|2025-11-06T12:00:00Z|END"));
    TEST_ASSERT_FALSE(MessageProtocol::validateMessage(nullptr));
    TEST_ASSERT_FALSE(MessageProtocol::validateMessage(""));
    TEST_ASSERT_FALSE(MessageProtocol::validateMessage("GPS001|37.7|-122.4|270|2025-11-06T12:00:00Z"));  // No END
}

void test_validate_coordinates() {
    TEST_ASSERT_TRUE(MessageProtocol::validateCoordinates(0.0, 0.0));
    TEST_ASSERT_TRUE(MessageProtocol::validateCoordinates(90.0, 180.0));
    TEST_ASSERT_TRUE(MessageProtocol::validateCoordinates(-90.0, -180.0));
    TEST_ASSERT_FALSE(MessageProtocol::validateCoordinates(91.0, 0.0));
    TEST_ASSERT_FALSE(MessageProtocol::validateCoordinates(0.0, 181.0));
}

// ============================================================================
// CHECKSUM TESTS
// ============================================================================

void test_checksum_consistency() {
    const char* msg1 = "GPS001|37.774900|-122.419400|270.50|2025-11-06T23:15:30Z|END";
    const char* msg2 = "GPS001|37.774900|-122.419400|270.50|2025-11-06T23:15:30Z|END";
    const char* msg3 = "GPS002|37.774900|-122.419400|270.50|2025-11-06T23:15:30Z|END";
    TEST_ASSERT_EQUAL(MessageProtocol::calculateChecksum(msg1), MessageProtocol::calculateChecksum(msg2));
    TEST_ASSERT_NOT_EQUAL(MessageProtocol::calculateChecksum(msg1), MessageProtocol::calculateChecksum(msg3));
}

// ============================================================================
// FORMAT ERROR TESTS
// ============================================================================

void test_format_errors() {
    GpsData data = createValidGpsData();
    char buffer[256];
    TEST_ASSERT_FALSE(MessageProtocol::formatMessage(data, nullptr, 256));  // Null buffer
    TEST_ASSERT_FALSE(MessageProtocol::formatMessage(data, buffer, 10));    // Too small
    data.latitude = 91.0;  // Invalid
    TEST_ASSERT_FALSE(MessageProtocol::formatMessage(data, buffer, sizeof(buffer)));
}

// ============================================================================
// MAIN TEST RUNNER
// ============================================================================

void setup() {
    delay(2000);
    UNITY_BEGIN();

    // Basic
    RUN_TEST(test_format_valid_message);
    RUN_TEST(test_parse_valid_message);
    RUN_TEST(test_round_trip_format_parse);

    // Edge cases
    RUN_TEST(test_coordinates_at_equator);
    RUN_TEST(test_coordinates_north_pole);
    RUN_TEST(test_coordinates_south_pole);
    RUN_TEST(test_coordinates_date_line);
    RUN_TEST(test_heading_cardinal_directions);

    // Invalid messages
    RUN_TEST(test_parse_null_message);
    RUN_TEST(test_parse_empty_message);
    RUN_TEST(test_parse_missing_end_marker);
    RUN_TEST(test_parse_wrong_delimiter_count);
    RUN_TEST(test_parse_invalid_latitude);
    RUN_TEST(test_parse_invalid_longitude);

    // Validation
    RUN_TEST(test_validate_message);
    RUN_TEST(test_validate_coordinates);

    // Checksums
    RUN_TEST(test_checksum_consistency);

    // Format errors
    RUN_TEST(test_format_errors);

    UNITY_END();
}

void loop() {}
