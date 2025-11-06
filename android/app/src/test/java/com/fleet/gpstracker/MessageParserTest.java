package com.fleet.gpstracker;

import com.fleet.gpstracker.models.GpsLocation;
import com.fleet.gpstracker.models.MessageParser;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Message Parser
 */
public class MessageParserTest {

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
    public void testParseInvalidMessage() {
        String message = "INVALID_MESSAGE";
        GpsLocation location = MessageParser.parseMessage(message);

        assertNull(location);
    }

    @Test
    public void testValidateMessage() {
        String validMessage = "GPS001|37.774900|-122.419400|270.50|2025-11-06T23:15:30Z|END";
        assertTrue(MessageParser.validateMessage(validMessage));

        String invalidMessage = "GPS001|37.774900|270.50|END";
        assertFalse(MessageParser.validateMessage(invalidMessage));
    }

    @Test
    public void testValidateCoordinates() {
        assertTrue(MessageParser.validateCoordinates(37.7749, -122.4194));
        assertTrue(MessageParser.validateCoordinates(0.0, 0.0));
        assertTrue(MessageParser.validateCoordinates(90.0, 180.0));
        assertTrue(MessageParser.validateCoordinates(-90.0, -180.0));

        assertFalse(MessageParser.validateCoordinates(91.0, 0.0));
        assertFalse(MessageParser.validateCoordinates(0.0, 181.0));
    }

    @Test
    public void testFormatMessage() {
        GpsLocation location = new GpsLocation("GPS001", 37.7749, -122.4194, 270.5, "2025-11-06T23:15:30Z");
        String message = MessageParser.formatMessage(location);

        assertNotNull(message);
        assertTrue(message.contains("GPS001"));
        assertTrue(message.contains("END"));
    }
}
