package com.fleet.gpstracker.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Configuration manager for app settings
 * Uses SharedPreferences for persistent storage
 */
public class ConfigManager {
    private static final String TAG = "ConfigManager";
    private static final String PREFS_NAME = "FleetGpsTrackerPrefs";

    // Configuration keys
    private static final String KEY_ESP32_MAC_ADDRESS = "esp32_mac_address";
    private static final String KEY_ESP32_DEVICE_NAME = "esp32_device_name";
    private static final String KEY_SMTP_HOST = "smtp_host";
    private static final String KEY_SMTP_PORT = "smtp_port";
    private static final String KEY_SMTP_USER = "smtp_user";
    private static final String KEY_SMTP_PASSWORD = "smtp_password";
    private static final String KEY_SMTP_USE_TLS = "smtp_use_tls";
    private static final String KEY_SMTP_FROM_EMAIL = "smtp_from_email";
    private static final String KEY_SMTP_TO_EMAILS = "smtp_to_emails";
    private static final String KEY_AUTO_CONNECT = "auto_connect";
    private static final String KEY_AUTO_SEND_EMAIL = "auto_send_email";
    private static final String KEY_FIRST_RUN = "first_run";

    private final SharedPreferences prefs;

    public ConfigManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ESP32 Configuration
    public String getEsp32MacAddress() {
        return prefs.getString(KEY_ESP32_MAC_ADDRESS, "");
    }

    public void setEsp32MacAddress(String macAddress) {
        prefs.edit().putString(KEY_ESP32_MAC_ADDRESS, macAddress).apply();
    }

    public String getEsp32DeviceName() {
        return prefs.getString(KEY_ESP32_DEVICE_NAME, "FleetGPS_001");
    }

    public void setEsp32DeviceName(String deviceName) {
        prefs.edit().putString(KEY_ESP32_DEVICE_NAME, deviceName).apply();
    }

    // SMTP Configuration
    public String getSmtpHost() {
        return prefs.getString(KEY_SMTP_HOST, "smtp.gmail.com");
    }

    public void setSmtpHost(String host) {
        prefs.edit().putString(KEY_SMTP_HOST, host).apply();
    }

    public int getSmtpPort() {
        return prefs.getInt(KEY_SMTP_PORT, 587);
    }

    public void setSmtpPort(int port) {
        prefs.edit().putInt(KEY_SMTP_PORT, port).apply();
    }

    public String getSmtpUser() {
        return prefs.getString(KEY_SMTP_USER, "");
    }

    public void setSmtpUser(String user) {
        prefs.edit().putString(KEY_SMTP_USER, user).apply();
    }

    public String getSmtpPassword() {
        return prefs.getString(KEY_SMTP_PASSWORD, "");
    }

    public void setSmtpPassword(String password) {
        prefs.edit().putString(KEY_SMTP_PASSWORD, password).apply();
    }

    public boolean getSmtpUseTls() {
        return prefs.getBoolean(KEY_SMTP_USE_TLS, true);
    }

    public void setSmtpUseTls(boolean useTls) {
        prefs.edit().putBoolean(KEY_SMTP_USE_TLS, useTls).apply();
    }

    public String getSmtpFromEmail() {
        return prefs.getString(KEY_SMTP_FROM_EMAIL, "");
    }

    public void setSmtpFromEmail(String email) {
        prefs.edit().putString(KEY_SMTP_FROM_EMAIL, email).apply();
    }

    public String getSmtpToEmails() {
        return prefs.getString(KEY_SMTP_TO_EMAILS, "");
    }

    public void setSmtpToEmails(String emails) {
        prefs.edit().putString(KEY_SMTP_TO_EMAILS, emails).apply();
    }

    public String[] getSmtpToEmailsArray() {
        String emails = getSmtpToEmails();
        if (emails.isEmpty()) {
            return new String[0];
        }
        return emails.split(",");
    }

    // App Behavior
    public boolean getAutoConnect() {
        return prefs.getBoolean(KEY_AUTO_CONNECT, false);
    }

    public void setAutoConnect(boolean autoConnect) {
        prefs.edit().putBoolean(KEY_AUTO_CONNECT, autoConnect).apply();
    }

    public boolean getAutoSendEmail() {
        return prefs.getBoolean(KEY_AUTO_SEND_EMAIL, true);
    }

    public void setAutoSendEmail(boolean autoSend) {
        prefs.edit().putBoolean(KEY_AUTO_SEND_EMAIL, autoSend).apply();
    }

    public boolean isFirstRun() {
        return prefs.getBoolean(KEY_FIRST_RUN, true);
    }

    public void setFirstRunComplete() {
        prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply();
    }

    // Validation
    public boolean isSmtpConfigured() {
        return !getSmtpHost().isEmpty() &&
               !getSmtpUser().isEmpty() &&
               !getSmtpPassword().isEmpty() &&
               !getSmtpFromEmail().isEmpty() &&
               !getSmtpToEmails().isEmpty();
    }

    public boolean isEsp32Configured() {
        return !getEsp32MacAddress().isEmpty();
    }

    // Clear configuration
    public void clearAll() {
        prefs.edit().clear().apply();
        Log.i(TAG, "All configuration cleared");
    }
}
