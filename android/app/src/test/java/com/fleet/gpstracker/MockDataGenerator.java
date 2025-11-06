package com.fleet.gpstracker;

import com.fleet.gpstracker.models.GpsLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mock data generator for testing
 */
public class MockDataGenerator {

    private static final Random random = new Random();

    // San Francisco area coordinates for testing
    private static final double BASE_LAT = 37.7749;
    private static final double BASE_LON = -122.4194;

    /**
     * Generate mock GPS location
     */
    public static GpsLocation generateLocation() {
        double lat = BASE_LAT + (random.nextDouble() - 0.5) * 0.1;
        double lon = BASE_LON + (random.nextDouble() - 0.5) * 0.1;
        double heading = random.nextDouble() * 360.0;

        String timestamp = String.format("2025-11-06T%02d:%02d:%02dZ",
            random.nextInt(24), random.nextInt(60), random.nextInt(60));

        GpsLocation location = new GpsLocation("GPS001", lat, lon, heading, timestamp);
        location.setSpeed(random.nextDouble() * 50.0);
        location.setAltitude(random.nextDouble() * 100.0);
        location.setSatellites(4 + random.nextInt(9));

        return location;
    }

    /**
     * Generate list of mock locations
     */
    public static List<GpsLocation> generateLocations(int count) {
        List<GpsLocation> locations = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            locations.add(generateLocation());
        }
        return locations;
    }

    /**
     * Generate mock GPS message
     */
    public static String generateMessage() {
        GpsLocation location = generateLocation();
        return String.format("%s|%.6f|%.6f|%.2f|%s|END",
            location.getDeviceSerial(),
            location.getLatitude(),
            location.getLongitude(),
            location.getHeading(),
            location.getTimestamp());
    }
}
