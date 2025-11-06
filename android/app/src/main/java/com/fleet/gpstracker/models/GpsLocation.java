package com.fleet.gpstracker.models;

import java.util.Date;

/**
 * GPS Location data model
 * Represents a GPS coordinate with metadata
 */
public class GpsLocation {
    private String deviceSerial;
    private double latitude;
    private double longitude;
    private double heading;
    private double speed;
    private double altitude;
    private String timestamp;
    private int satellites;
    private Date receivedAt;
    private boolean valid;

    public GpsLocation() {
        this.receivedAt = new Date();
        this.valid = false;
    }

    public GpsLocation(String deviceSerial, double latitude, double longitude,
                      double heading, String timestamp) {
        this.deviceSerial = deviceSerial;
        this.latitude = latitude;
        this.longitude = longitude;
        this.heading = heading;
        this.timestamp = timestamp;
        this.receivedAt = new Date();
        this.valid = true;
    }

    // Getters and setters
    public String getDeviceSerial() {
        return deviceSerial;
    }

    public void setDeviceSerial(String deviceSerial) {
        this.deviceSerial = deviceSerial;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getHeading() {
        return heading;
    }

    public void setHeading(double heading) {
        this.heading = heading;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getSatellites() {
        return satellites;
    }

    public void setSatellites(int satellites) {
        this.satellites = satellites;
    }

    public Date getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Date receivedAt) {
        this.receivedAt = receivedAt;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    // Validation methods
    public boolean validateCoordinates() {
        return latitude >= -90.0 && latitude <= 90.0 &&
               longitude >= -180.0 && longitude <= 180.0;
    }

    // Format for display
    public String getFormattedCoordinates() {
        return String.format("%.6f, %.6f", latitude, longitude);
    }

    public String getFormattedHeading() {
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int index = (int) Math.round(((heading % 360) / 45.0)) % 8;
        return String.format("%.1f° %s", heading, directions[index]);
    }

    // Google Maps URL
    public String getGoogleMapsUrl() {
        return String.format("https://www.google.com/maps?q=%.6f,%.6f",
                           latitude, longitude);
    }

    @Override
    public String toString() {
        return String.format("GpsLocation{serial='%s', lat=%.6f, lon=%.6f, heading=%.2f, timestamp='%s'}",
                           deviceSerial, latitude, longitude, heading, timestamp);
    }
}
