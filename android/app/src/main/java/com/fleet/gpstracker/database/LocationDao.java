package com.fleet.gpstracker.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import java.util.List;

/**
 * Data Access Object for GPS locations
 */
@Dao
public interface LocationDao {

    @Insert
    long insert(LocationEntity location);

    @Update
    void update(LocationEntity location);

    @Delete
    void delete(LocationEntity location);

    @Query("SELECT * FROM locations ORDER BY received_at DESC")
    LiveData<List<LocationEntity>> getAllLocations();

    @Query("SELECT * FROM locations ORDER BY received_at DESC LIMIT :limit")
    LiveData<List<LocationEntity>> getRecentLocations(int limit);

    @Query("SELECT * FROM locations WHERE id = :id")
    LocationEntity getLocationById(long id);

    @Query("SELECT * FROM locations WHERE device_serial = :deviceSerial ORDER BY received_at DESC")
    LiveData<List<LocationEntity>> getLocationsByDevice(String deviceSerial);

    @Query("SELECT * FROM locations WHERE email_sent = 0 ORDER BY received_at ASC")
    List<LocationEntity> getPendingEmailLocations();

    @Query("SELECT COUNT(*) FROM locations")
    int getLocationCount();

    @Query("SELECT COUNT(*) FROM locations WHERE email_sent = 1")
    int getEmailsSentCount();

    @Query("DELETE FROM locations WHERE received_at < :timestamp")
    void deleteOlderThan(long timestamp);

    @Query("DELETE FROM locations")
    void deleteAll();

    @Query("SELECT * FROM locations ORDER BY received_at DESC LIMIT 1")
    LocationEntity getLastLocation();

    @Query("UPDATE locations SET email_sent = 1, email_sent_at = :sentAt WHERE id = :id")
    void markEmailSent(long id, long sentAt);
}
