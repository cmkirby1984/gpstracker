package com.fleet.gpstracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fleet.gpstracker.models.GpsLocation;
import com.fleet.gpstracker.services.GpsTrackerService;
import com.fleet.gpstracker.utils.ConfigManager;
import com.fleet.gpstracker.utils.PermissionHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Main Activity for Fleet GPS Tracker
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private TextView tvStatus;
    private TextView tvMessages;
    private TextView tvEmails;
    private TextView tvLastLocation;
    private Button btnConnect;
    private Button btnDisconnect;

    private GpsTrackerService service;
    private boolean serviceBound = false;
    private ConfigManager config;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            GpsTrackerService.LocalBinder localBinder = (GpsTrackerService.LocalBinder) binder;
            service = localBinder.getService();
            serviceBound = true;

            service.setCallback(new GpsTrackerService.ServiceCallback() {
                @Override
                public void onLocationReceived(GpsLocation location) {
                    runOnUiThread(() -> updateLocationDisplay(location));
                }

                @Override
                public void onEmailSent(boolean success) {
                    runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(MainActivity.this, "Email sent", Toast.LENGTH_SHORT).show();
                        }
                        updateStatusDisplay();
                    });
                }

                @Override
                public void onConnectionStatusChanged(boolean connected) {
                    runOnUiThread(() -> updateConnectionStatus(connected));
                }
            });

            updateStatusDisplay();
            Log.i(TAG, "Service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            service = null;
            Log.i(TAG, "Service disconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        config = new ConfigManager(this);

        initViews();
        checkPermissions();
        startService();
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tv_status);
        tvMessages = findViewById(R.id.tv_messages);
        tvEmails = findViewById(R.id.tv_emails);
        tvLastLocation = findViewById(R.id.tv_last_location);
        btnConnect = findViewById(R.id.btn_connect);
        btnDisconnect = findViewById(R.id.btn_disconnect);

        btnConnect.setOnClickListener(v -> connectToDevice());
        btnDisconnect.setOnClickListener(v -> disconnectFromDevice());

        updateConnectionStatus(false);
    }

    private void checkPermissions() {
        if (!PermissionHelper.hasAllRequiredPermissions(this)) {
            PermissionHelper.requestAllPermissions(this);
        }
    }

    private void startService() {
        Intent serviceIntent = new Intent(this, GpsTrackerService.class);
        startForegroundService(serviceIntent);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice() {
        if (!PermissionHelper.hasBluetoothPermissions(this)) {
            Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_SHORT).show();
            return;
        }

        String macAddress = config.getEsp32MacAddress();

        if (macAddress.isEmpty()) {
            Toast.makeText(this, "Configure ESP32 MAC address in settings", Toast.LENGTH_LONG).show();
            // TODO: Open settings activity
            return;
        }

        if (serviceBound && service != null) {
            service.connectToDevice(macAddress);
            tvStatus.setText("Connecting...");
        }
    }

    private void disconnectFromDevice() {
        if (serviceBound && service != null) {
            service.disconnect();
        }
    }

    private void updateConnectionStatus(boolean connected) {
        if (connected) {
            tvStatus.setText("Connected");
            tvStatus.setTextColor(getColor(android.R.color.holo_green_dark));
            btnConnect.setEnabled(false);
            btnDisconnect.setEnabled(true);
        } else {
            tvStatus.setText("Disconnected");
            tvStatus.setTextColor(getColor(android.R.color.holo_red_dark));
            btnConnect.setEnabled(true);
            btnDisconnect.setEnabled(false);
        }
    }

    private void updateStatusDisplay() {
        if (serviceBound && service != null) {
            tvMessages.setText("Messages: " + service.getMessagesReceived());
            tvEmails.setText("Emails: " + service.getEmailsSent());
        }
    }

    private void updateLocationDisplay(GpsLocation location) {
        if (location != null) {
            String locationText = String.format("Last: %s\n%s\nHeading: %s",
                location.getDeviceSerial(),
                location.getFormattedCoordinates(),
                location.getFormattedHeading());
            tvLastLocation.setText(locationText);
        }
        updateStatusDisplay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionHelper.REQUEST_BLUETOOTH_PERMISSIONS) {
            if (PermissionHelper.isPermissionGranted(grantResults)) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions required for Bluetooth", Toast.LENGTH_LONG).show();
            }
        }
    }
}
