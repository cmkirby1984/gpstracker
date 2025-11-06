package com.fleet.gpstracker.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

/**
 * Room database entity for storing GPS locations
 */
@Entity(tableName = "locations")
public class LocationEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "device_serial")
    private String deviceSerial;

    @ColumnInfo(name = "latitude")
    private double latitude;

    @ColumnInfo(name = "longitude")
    private double longitude;

    @ColumnInfo(name = "heading")
    private double heading;

    @ColumnInfo(name = "speed")
    private double speed;

    @ColumnInfo(name = "altitude")
    private double altitude;

    @ColumnInfo(name = "timestamp")
    private String timestamp;

    @ColumnInfo(name = "satellites")
    private int satellites;

    @ColumnInfo(name = "received_at")
    private long receivedAt;

    @ColumnInfo(name = "email_sent")
    private boolean emailSent;

    @ColumnInfo(name = "email_sent_at")
    private long emailSentAt;

    // Constructors
    public LocationEntity() {
        this.receivedAt = System.currentTimeMillis();
        this.emailSent = false;
        this.emailSentAt = 0;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

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

    public long getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(long receivedAt) {
        this.receivedAt = receivedAt;
    }

    public boolean isEmailSent() {
        return emailSent;
    }

    public void setEmailSent(boolean emailSent) {
        this.emailSent = emailSent;
    }

    public long getEmailSentAt() {
        return emailSentAt;
    }

    public void setEmailSentAt(long emailSentAt) {
        this.emailSentAt = emailSentAt;
    }
}
