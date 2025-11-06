package com.fleet.gpstracker;

import com.fleet.gpstracker.models.GpsLocation;
import com.fleet.gpstracker.models.MessageParser;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive Message Parser Test Suite
 * Run with: cd android && ./gradlew test
 */
public class MessageParserTest {

    // BASIC FUNCTIONALITY
    @Test
    public void testParseValidMessage() {
        String message = "GPS001|37.774900|-122.419400|270.50|2025-11-06T23:15:30Z|END";
        GpsLocation location = MessageParser.parseMessage(message);
        assertNotNull(location);
        assertEquals("GPS001", location.getDeviceSerial());
        assertEquals(37.7749, location.getLatitude(), 0.0001);
        assertEquals(-122.4194, location.getLongitude(), 0.0001);
        assertEquals(270.5, location.getHeading(), 0.01);
        assertTrue(location.isValid());
    }

    @Test
    public void testRoundTripFormatParse() {
        GpsLocation original = new GpsLocation("GPS999", 51.5074, -0.1278, 180.0, "2025-11-06T12:00:00Z");
        String message = MessageParser.formatMessage(original);
        GpsLocation parsed = MessageParser.parseMessage(message);
        assertNotNull(parsed);
        assertEquals(original.getDeviceSerial(), parsed.getDeviceSerial());
        assertEquals(original.getLatitude(), parsed.getLatitude(), 0.0001);
        assertEquals(original.getLongitude(), parsed.getLongitude(), 0.0001);
    }

    // EDGE CASES - COORDINATES
    @Test
    public void testCoordinatesAtBoundaries() {
        String[] messages = {
            "GPS002|0.000000|0.000000|0.00|2025-11-06T12:00:00Z|END",
            "GPS003|90.000000|0.000000|180.00|2025-11-06T12:00:00Z|END",
            "GPS004|-90.000000|0.000000|0.00|2025-11-06T12:00:00Z|END",
            "GPS005|0.000000|180.000000|90.00|2025-11-06T12:00:00Z|END",
            "GPS006|0.000000|-180.000000|270.00|2025-11-06T12:00:00Z|END"
        };
        for (String msg : messages) {
            GpsLocation location = MessageParser.parseMessage(msg);
            assertNotNull(location);
            assertTrue(location.validateCoordinates());
        }
    }

    @Test
    public void testHeadingCardinalDirections() {
        String[] messages = {
            "GPS010|0.0|0.0|0.00|2025-11-06T12:00:00Z|END",
            "GPS011|0.0|0.0|90.00|2025-11-06T12:00:00Z|END",
            "GPS012|0.0|0.0|180.00|2025-11-06T12:00:00Z|END",
            "GPS013|0.0|0.0|270.00|2025-11-06T12:00:00Z|END"
        };
        double[] expected = {0.0, 90.0, 180.0, 270.0};
        for (int i = 0; i < messages.length; i++) {
            GpsLocation loc = MessageParser.parseMessage(messages[i]);
            assertNotNull(loc);
            assertEquals(expected[i], loc.getHeading(), 0.01);
        }
    }

    // INVALID MESSAGES
    @Test
    public void testParseInvalidMessages() {
        assertNull(MessageParser.parseMessage(null));
        assertNull(MessageParser.parseMessage(""));
        assertNull(MessageParser.parseMessage("GPS001|37.7|-122.4|270|2025-11-06T12:00:00Z"));  // No END
        assertNull(MessageParser.parseMessage("GPS001|37.7|270|2025-11-06T12:00:00Z|END"));  // Missing field
    }

    @Test
    public void testParseInvalidCoordinates() {
        String[] messages = {
            "GPS001|91.000000|-122.419400|270.50|2025-11-06T12:00:00Z|END",   // Lat > 90
            "GPS001|-91.000000|-122.419400|270.50|2025-11-06T12:00:00Z|END",  // Lat < -90
            "GPS001|37.774900|181.000000|270.50|2025-11-06T12:00:00Z|END",    // Lon > 180
            "GPS001|37.774900|-181.000000|270.50|2025-11-06T12:00:00Z|END"    // Lon < -180
        };
        for (String msg : messages) {
            GpsLocation location = MessageParser.parseMessage(msg);
            assertNull(location);
        }
    }

    // VALIDATION
    @Test
    public void testValidateMessage() {
        assertTrue(MessageParser.validateMessage("GPS001|37.7|-122.4|270|2025-11-06T12:00:00Z|END"));
        assertFalse(MessageParser.validateMessage(null));
        assertFalse(MessageParser.validateMessage(""));
        assertFalse(MessageParser.validateMessage("GPS001|END"));
    }

    @Test
    public void testValidateCoordinates() {
        assertTrue(MessageParser.validateCoordinates(0.0, 0.0));
        assertTrue(MessageParser.validateCoordinates(90.0, 180.0));
        assertTrue(MessageParser.validateCoordinates(-90.0, -180.0));
        assertFalse(MessageParser.validateCoordinates(91.0, 0.0));
        assertFalse(MessageParser.validateCoordinates(0.0, 181.0));
    }

    // CHECKSUMS
    @Test
    public void testChecksumConsistency() {
        String msg1 = "GPS001|37.774900|-122.419400|270.50|2025-11-06T23:15:30Z|END";
        String msg2 = "GPS001|37.774900|-122.419400|270.50|2025-11-06T23:15:30Z|END";
        String msg3 = "GPS002|37.774900|-122.419400|270.50|2025-11-06T23:15:30Z|END";
        assertEquals(MessageParser.calculateChecksum(msg1), MessageParser.calculateChecksum(msg2));
        assertNotEquals(MessageParser.calculateChecksum(msg1), MessageParser.calculateChecksum(msg3));
    }
}
