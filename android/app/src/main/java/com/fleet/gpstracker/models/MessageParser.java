package com.fleet.gpstracker.models;

import android.util.Log;

/**
 * Message Parser for GPS data protocol
 * Format: SERIAL|LAT|LON|HEADING|TIME|END
 * Example: GPS001|37.7749|-122.4194|270.5|2025-11-06T23:15:30Z|END
 */
public class MessageParser {
    private static final String TAG = "MessageParser";
    private static final String DELIMITER = "\\|";
    private static final String END_MARKER = "END";
    private static final int EXPECTED_FIELDS = 6;

    /**
     * Parse GPS message into GpsLocation object
     * @param message Raw message string
     * @return GpsLocation object or null if parsing fails
     */
    public static GpsLocation parseMessage(String message) {
        if (message == null || message.isEmpty()) {
            Log.e(TAG, "Null or empty message");
            return null;
        }

        // Validate message format
        if (!validateMessage(message)) {
            Log.e(TAG, "Message validation failed: " + message);
            return null;
        }

        try {
            // Split message by delimiter
            String[] fields = message.split(DELIMITER);

            if (fields.length != EXPECTED_FIELDS) {
                Log.e(TAG, "Invalid field count: " + fields.length + " (expected " + EXPECTED_FIELDS + ")");
                return null;
            }

            // Parse fields
            String deviceSerial = fields[0].trim();
            double latitude = Double.parseDouble(fields[1].trim());
            double longitude = Double.parseDouble(fields[2].trim());
            double heading = Double.parseDouble(fields[3].trim());
            String timestamp = fields[4].trim();
            String endMarker = fields[5].trim();

            // Validate end marker
            if (!END_MARKER.equals(endMarker)) {
                Log.e(TAG, "Invalid end marker: " + endMarker);
                return null;
            }

            // Create GpsLocation object
            GpsLocation location = new GpsLocation(deviceSerial, latitude, longitude,
                                                  heading, timestamp);

            // Validate coordinates
            if (!location.validateCoordinates()) {
                Log.e(TAG, "Invalid coordinates: " + location.getFormattedCoordinates());
                return null;
            }

            Log.d(TAG, "Successfully parsed: " + location.toString());
            return location;

        } catch (NumberFormatException e) {
            Log.e(TAG, "Number parsing error: " + e.getMessage());
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Parsing error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Validate message format
     * @param message Raw message string
     * @return true if valid format
     */
    public static boolean validateMessage(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }

        // Check minimum length
        if (message.length() < 20) {
            Log.e(TAG, "Message too short: " + message.length());
            return false;
        }

        // Check for END marker
        if (!message.contains(END_MARKER)) {
            Log.e(TAG, "Missing END marker");
            return false;
        }

        // Count delimiters (should be 5)
        int delimiterCount = message.length() - message.replace("|", "").length();
        if (delimiterCount != 5) {
            Log.e(TAG, "Invalid delimiter count: " + delimiterCount + " (expected 5)");
            return false;
        }

        return true;
    }

    /**
     * Format GpsLocation into protocol message
     * @param location GpsLocation object
     * @return Formatted message string
     */
    public static String formatMessage(GpsLocation location) {
        if (location == null || !location.isValid()) {
            Log.e(TAG, "Invalid location for formatting");
            return null;
        }

        return String.format("%s|%.6f|%.6f|%.2f|%s|END",
                           location.getDeviceSerial(),
                           location.getLatitude(),
                           location.getLongitude(),
                           location.getHeading(),
                           location.getTimestamp());
    }

    /**
     * Validate coordinate ranges
     * @param lat Latitude
     * @param lon Longitude
     * @return true if valid
     */
    public static boolean validateCoordinates(double lat, double lon) {
        return lat >= -90.0 && lat <= 90.0 &&
               lon >= -180.0 && lon <= 180.0;
    }

    /**
     * Calculate checksum for message (for future use)
     * @param message Message string
     * @return Checksum value
     */
    public static int calculateChecksum(String message) {
        int checksum = 0;
        for (char c : message.toCharArray()) {
            checksum ^= (int) c;
        }
        return checksum;
    }
}
