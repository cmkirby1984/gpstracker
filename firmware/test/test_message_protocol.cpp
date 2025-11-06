#include <Arduino.h>
#include <unity.h>
#include <string.h>
#include "message_protocol.h"

// Test message formatting
void test_format_message() {
    GpsData data;
    strcpy(data.deviceSerial, "GPS001");
    data.latitude = 37.7749;
    data.longitude = -122.4194;
    data.heading = 270.5;
    data.speed = 0.0;
    data.altitude = 50.0;
    strcpy(data.timestamp, "2025-11-06T23:15:30Z");
    data.satellites = 8;
    data.valid = true;

    char buffer[256];
    bool result = MessageProtocol::formatMessage(data, buffer, sizeof(buffer));

    TEST_ASSERT_TRUE(result);
    TEST_ASSERT_TRUE(strlen(buffer) > 0);
    TEST_ASSERT_TRUE(strstr(buffer, "GPS001") != NULL);
    TEST_ASSERT_TRUE(strstr(buffer, "END") != NULL);
}

// Test message parsing
void test_parse_message() {
    const char* testMessage = "GPS001|37.774900|-122.419400|270.50|2025-11-06T23:15:30Z|END";
    GpsData data;

    bool result = MessageProtocol::parseMessage(testMessage, data);

    TEST_ASSERT_TRUE(result);
    TEST_ASSERT_EQUAL_STRING("GPS001", data.deviceSerial);
    TEST_ASSERT_FLOAT_WITHIN(0.0001, 37.7749, data.latitude);
    TEST_ASSERT_FLOAT_WITHIN(0.0001, -122.4194, data.longitude);
    TEST_ASSERT_FLOAT_WITHIN(0.01, 270.5, data.heading);
    TEST_ASSERT_TRUE(data.valid);
}

// Test message validation
void test_validate_message_valid() {
    const char* validMessage = "GPS001|37.774900|-122.419400|270.50|2025-11-06T23:15:30Z|END";
    bool result = MessageProtocol::validateMessage(validMessage);
    TEST_ASSERT_TRUE(result);
}

void test_validate_message_missing_end() {
    const char* invalidMessage = "GPS001|37.774900|-122.419400|270.50|2025-11-06T23:15:30Z";
    bool result = MessageProtocol::validateMessage(invalidMessage);
    TEST_ASSERT_FALSE(result);
}

void test_validate_message_wrong_delimiter_count() {
    const char* invalidMessage = "GPS001|37.774900|270.50|2025-11-06T23:15:30Z|END";
    bool result = MessageProtocol::validateMessage(invalidMessage);
    TEST_ASSERT_FALSE(result);
}

// Test coordinate validation
void test_validate_coordinates_valid() {
    TEST_ASSERT_TRUE(MessageProtocol::validateCoordinates(37.7749, -122.4194));
    TEST_ASSERT_TRUE(MessageProtocol::validateCoordinates(0.0, 0.0));
    TEST_ASSERT_TRUE(MessageProtocol::validateCoordinates(90.0, 180.0));
    TEST_ASSERT_TRUE(MessageProtocol::validateCoordinates(-90.0, -180.0));
}

void test_validate_coordinates_invalid() {
    TEST_ASSERT_FALSE(MessageProtocol::validateCoordinates(91.0, 0.0));
    TEST_ASSERT_FALSE(MessageProtocol::validateCoordinates(-91.0, 0.0));
    TEST_ASSERT_FALSE(MessageProtocol::validateCoordinates(0.0, 181.0));
    TEST_ASSERT_FALSE(MessageProtocol::validateCoordinates(0.0, -181.0));
}

// Test checksum calculation
void test_calculate_checksum() {
    const char* message1 = "GPS001|37.774900|-122.419400|270.50|2025-11-06T23:15:30Z|END";
    const char* message2 = "GPS001|37.774900|-122.419400|270.50|2025-11-06T23:15:30Z|END";
    const char* message3 = "GPS002|37.774900|-122.419400|270.50|2025-11-06T23:15:30Z|END";

    uint16_t checksum1 = MessageProtocol::calculateChecksum(message1);
    uint16_t checksum2 = MessageProtocol::calculateChecksum(message2);
    uint16_t checksum3 = MessageProtocol::calculateChecksum(message3);

    TEST_ASSERT_EQUAL(checksum1, checksum2);  // Same message, same checksum
    TEST_ASSERT_NOT_EQUAL(checksum1, checksum3);  // Different message, different checksum
}

// Test round-trip (format then parse)
void test_format_parse_roundtrip() {
    GpsData original;
    strcpy(original.deviceSerial, "GPS999");
    original.latitude = 51.5074;
    original.longitude = -0.1278;
    original.heading = 180.0;
    original.speed = 25.5;
    original.altitude = 100.0;
    strcpy(original.timestamp, "2025-11-06T12:00:00Z");
    original.satellites = 12;
    original.valid = true;

    char buffer[256];
    bool formatResult = MessageProtocol::formatMessage(original, buffer, sizeof(buffer));
    TEST_ASSERT_TRUE(formatResult);

    GpsData parsed;
    bool parseResult = MessageProtocol::parseMessage(buffer, parsed);
    TEST_ASSERT_TRUE(parseResult);

    TEST_ASSERT_EQUAL_STRING(original.deviceSerial, parsed.deviceSerial);
    TEST_ASSERT_FLOAT_WITHIN(0.0001, original.latitude, parsed.latitude);
    TEST_ASSERT_FLOAT_WITHIN(0.0001, original.longitude, parsed.longitude);
    TEST_ASSERT_FLOAT_WITHIN(0.01, original.heading, parsed.heading);
    TEST_ASSERT_EQUAL_STRING(original.timestamp, parsed.timestamp);
}

void setup() {
    delay(2000);  // Wait for serial

    UNITY_BEGIN();

    RUN_TEST(test_format_message);
    RUN_TEST(test_parse_message);
    RUN_TEST(test_validate_message_valid);
    RUN_TEST(test_validate_message_missing_end);
    RUN_TEST(test_validate_message_wrong_delimiter_count);
    RUN_TEST(test_validate_coordinates_valid);
    RUN_TEST(test_validate_coordinates_invalid);
    RUN_TEST(test_calculate_checksum);
    RUN_TEST(test_format_parse_roundtrip);

    UNITY_END();
}

void loop() {
    // Nothing to do here
}
