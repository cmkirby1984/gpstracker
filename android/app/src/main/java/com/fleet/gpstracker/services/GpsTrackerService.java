package com.fleet.gpstracker.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.fleet.gpstracker.MainActivity;
import com.fleet.gpstracker.R;
import com.fleet.gpstracker.bluetooth.BluetoothService;
import com.fleet.gpstracker.database.AppDatabase;
import com.fleet.gpstracker.database.LocationDao;
import com.fleet.gpstracker.database.LocationEntity;
import com.fleet.gpstracker.email.SmtpEmailer;
import com.fleet.gpstracker.models.GpsLocation;
import com.fleet.gpstracker.models.MessageParser;
import com.fleet.gpstracker.utils.ConfigManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Foreground service for GPS tracking
 * Manages Bluetooth connection, data reception, and email sending
 */
public class GpsTrackerService extends Service {
    private static final String TAG = "GpsTrackerService";
    private static final String CHANNEL_ID = "FleetGpsTrackerChannel";
    private static final int NOTIFICATION_ID = 1;

    private final IBinder binder = new LocalBinder();

    private BluetoothService bluetoothService;
    private SmtpEmailer emailer;
    private ConfigManager config;
    private LocationDao locationDao;
    private ExecutorService executorService;

    private boolean isRunning;
    private int messagesReceived;
    private int emailsSent;

    private ServiceCallback callback;

    public interface ServiceCallback {
        void onLocationReceived(GpsLocation location);
        void onEmailSent(boolean success);
        void onConnectionStatusChanged(boolean connected);
    }

    public class LocalBinder extends Binder {
        public GpsTrackerService getService() {
            return GpsTrackerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service created");

        // Initialize components
        bluetoothService = new BluetoothService(this);
        emailer = new SmtpEmailer();
        config = new ConfigManager(this);
        locationDao = AppDatabase.getInstance(this).locationDao();
        executorService = Executors.newSingleThreadExecutor();

        // Configure emailer from settings
        configureEmailer();

        // Set up Bluetooth listeners
        setupBluetoothListeners();

        isRunning = false;
        messagesReceived = 0;
        emailsSent = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service started");
        isRunning = true;

        // Start foreground service
        startForeground(NOTIFICATION_ID, createNotification("GPS Tracker Running"));

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Service destroyed");

        isRunning = false;

        if (bluetoothService != null) {
            bluetoothService.disconnect();
        }

        if (executorService != null) {
            executorService.shutdown();
        }
    }

    /**
     * Set up Bluetooth message and connection listeners
     */
    private void setupBluetoothListeners() {
        bluetoothService.setMessageListener(message -> {
            Log.d(TAG, "Received message: " + message);
            messagesReceived++;

            // Parse message
            GpsLocation location = MessageParser.parseMessage(message);

            if (location != null && location.isValid()) {
                handleGpsLocation(location);
            } else {
                Log.e(TAG, "Failed to parse message: " + message);
            }

            updateNotification("Messages: " + messagesReceived + ", Emails: " + emailsSent);
        });

        bluetoothService.setConnectionListener(new BluetoothService.ConnectionListener() {
            @Override
            public void onConnected(String deviceName) {
                Log.i(TAG, "Connected to: " + deviceName);
                updateNotification("Connected to " + deviceName);

                if (callback != null) {
                    callback.onConnectionStatusChanged(true);
                }
            }

            @Override
            public void onDisconnected() {
                Log.i(TAG, "Disconnected");
                updateNotification("Disconnected");

                if (callback != null) {
                    callback.onConnectionStatusChanged(false);
                }
            }

            @Override
            public void onConnectionFailed(String error) {
                Log.e(TAG, "Connection failed: " + error);
                updateNotification("Connection failed");

                if (callback != null) {
                    callback.onConnectionStatusChanged(false);
                }
            }
        });
    }

    /**
     * Handle received GPS location
     */
    private void handleGpsLocation(GpsLocation location) {
        Log.i(TAG, "Processing GPS location: " + location.toString());

        // Save to database
        executorService.execute(() -> {
            LocationEntity entity = new LocationEntity();
            entity.setDeviceSerial(location.getDeviceSerial());
            entity.setLatitude(location.getLatitude());
            entity.setLongitude(location.getLongitude());
            entity.setHeading(location.getHeading());
            entity.setSpeed(location.getSpeed());
            entity.setAltitude(location.getAltitude());
            entity.setTimestamp(location.getTimestamp());
            entity.setSatellites(location.getSatellites());
            entity.setReceivedAt(System.currentTimeMillis());

            long id = locationDao.insert(entity);
            Log.d(TAG, "Location saved to database with ID: " + id);

            // Send email if configured
            if (config.getAutoSendEmail() && config.isSmtpConfigured()) {
                sendEmailForLocation(location, id);
            }
        });

        // Notify callback
        if (callback != null) {
            callback.onLocationReceived(location);
        }
    }

    /**
     * Send email for GPS location
     */
    private void sendEmailForLocation(GpsLocation location, long locationId) {
        Log.i(TAG, "Sending email for location: " + locationId);

        boolean success = emailer.sendLocationUpdate(location);

        if (success) {
            emailsSent++;
            locationDao.markEmailSent(locationId, System.currentTimeMillis());
            Log.i(TAG, "Email sent successfully");
        } else {
            Log.e(TAG, "Email sending failed");
        }

        if (callback != null) {
            callback.onEmailSent(success);
        }

        updateNotification("Messages: " + messagesReceived + ", Emails: " + emailsSent);
    }

    /**
     * Configure emailer from settings
     */
    private void configureEmailer() {
        emailer.configure(
            config.getSmtpHost(),
            config.getSmtpPort(),
            config.getSmtpUser(),
            config.getSmtpPassword(),
            false,  // SSL
            config.getSmtpUseTls(),  // TLS
            config.getSmtpFromEmail(),
            config.getSmtpToEmailsArray()
        );
    }

    /**
     * Create notification for foreground service
     */
    private Notification createNotification(String contentText) {
        createNotificationChannel();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fleet GPS Tracker")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)  // TODO: Use custom icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }

    /**
     * Update notification text
     */
    private void updateNotification(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(text));
        }
    }

    /**
     * Create notification channel (Android 8.0+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "GPS Tracker Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Fleet GPS Tracker background service");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // Public methods for controlling service
    public void connectToDevice(String macAddress) {
        Log.i(TAG, "Connecting to device: " + macAddress);
        bluetoothService.connect(macAddress);
    }

    public void disconnect() {
        Log.i(TAG, "Disconnecting");
        bluetoothService.disconnect();
    }

    public boolean isConnected() {
        return bluetoothService != null && bluetoothService.isConnected();
    }

    public int getMessagesReceived() {
        return messagesReceived;
    }

    public int getEmailsSent() {
        return emailsSent;
    }

    public void setCallback(ServiceCallback callback) {
        this.callback = callback;
    }

    public BluetoothService getBluetoothService() {
        return bluetoothService;
    }
}
