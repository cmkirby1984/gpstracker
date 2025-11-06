package com.fleet.gpstracker.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for managing runtime permissions
 */
public class PermissionHelper {
    private static final String TAG = "PermissionHelper";

    // Permission request codes
    public static final int REQUEST_BLUETOOTH_PERMISSIONS = 100;
    public static final int REQUEST_NOTIFICATION_PERMISSION = 101;

    /**
     * Get required Bluetooth permissions based on Android version
     */
    public static String[] getRequiredBluetoothPermissions() {
        List<String> permissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
        } else {
            // Android 11 and below
            permissions.add(Manifest.permission.BLUETOOTH);
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        }

        return permissions.toArray(new String[0]);
    }

    /**
     * Check if all Bluetooth permissions are granted
     */
    public static boolean hasBluetoothPermissions(Context context) {
        String[] permissions = getRequiredBluetoothPermissions();

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    /**
     * Request Bluetooth permissions
     */
    public static void requestBluetoothPermissions(Activity activity) {
        String[] permissions = getRequiredBluetoothPermissions();
        ActivityCompat.requestPermissions(activity, permissions,
                                         REQUEST_BLUETOOTH_PERMISSIONS);
    }

    /**
     * Check if notification permission is granted (Android 13+)
     */
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;  // Not required on older versions
    }

    /**
     * Request notification permission
     */
    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION);
        }
    }

    /**
     * Check if all required permissions are granted
     */
    public static boolean hasAllRequiredPermissions(Context context) {
        return hasBluetoothPermissions(context) &&
               hasNotificationPermission(context);
    }

    /**
     * Request all required permissions
     */
    public static void requestAllPermissions(Activity activity) {
        List<String> permissionsToRequest = new ArrayList<>();

        // Add Bluetooth permissions if not granted
        if (!hasBluetoothPermissions(activity)) {
            String[] btPermissions = getRequiredBluetoothPermissions();
            for (String permission : btPermissions) {
                if (ContextCompat.checkSelfPermission(activity, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }
        }

        // Add notification permission if not granted
        if (!hasNotificationPermission(activity)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(activity,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    /**
     * Check if permission request was granted
     */
    public static boolean isPermissionGranted(int[] grantResults) {
        if (grantResults.length == 0) {
            return false;
        }

        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }
}
