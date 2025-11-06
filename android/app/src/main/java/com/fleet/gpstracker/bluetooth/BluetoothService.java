package com.fleet.gpstracker.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Bluetooth service for connecting to ESP32 GPS tracker
 * Handles connection, data reception, and message parsing
 */
public class BluetoothService {
    private static final String TAG = "BluetoothService";

    // Standard SPP UUID for Bluetooth Serial
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothStateMachine stateMachine;
    private final Handler mainHandler;

    private BluetoothSocket socket;
    private InputStream inputStream;
    private Thread readerThread;
    private boolean isReading;

    private MessageListener messageListener;
    private ConnectionListener connectionListener;

    public interface MessageListener {
        void onMessageReceived(String message);
    }

    public interface ConnectionListener {
        void onConnected(String deviceName);
        void onDisconnected();
        void onConnectionFailed(String error);
    }

    public BluetoothService(Context context) {
        this.context = context.getApplicationContext();
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.stateMachine = new BluetoothStateMachine();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.isReading = false;
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }

    /**
     * Connect to Bluetooth device by MAC address
     */
    @SuppressLint("MissingPermission")
    public void connect(String deviceAddress) {
        if (bluetoothAdapter == null) {
            notifyConnectionFailed("Bluetooth not supported");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            notifyConnectionFailed("Bluetooth not enabled");
            return;
        }

        if (!stateMachine.canConnect()) {
            Log.w(TAG, "Cannot connect in current state: " + stateMachine.getCurrentState());
            return;
        }

        stateMachine.processEvent(BluetoothStateMachine.Event.CONNECT);

        new Thread(() -> {
            try {
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
                Log.i(TAG, "Connecting to device: " + device.getName() + " (" + deviceAddress + ")");

                // Create socket
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID);

                // Cancel discovery to speed up connection
                bluetoothAdapter.cancelDiscovery();

                // Connect
                socket.connect();

                // Get input stream
                inputStream = socket.getInputStream();

                // Notify success
                stateMachine.processEvent(BluetoothStateMachine.Event.CONNECTION_SUCCESS);
                notifyConnected(device.getName());

                // Start reading data
                startReading();

            } catch (IOException e) {
                Log.e(TAG, "Connection failed: " + e.getMessage());
                closeSocket();
                stateMachine.processEvent(BluetoothStateMachine.Event.CONNECTION_FAILED);
                notifyConnectionFailed(e.getMessage());
            } catch (SecurityException e) {
                Log.e(TAG, "Permission denied: " + e.getMessage());
                stateMachine.processEvent(BluetoothStateMachine.Event.CONNECTION_FAILED);
                notifyConnectionFailed("Bluetooth permission denied");
            }
        }).start();
    }

    /**
     * Disconnect from device
     */
    public void disconnect() {
        Log.i(TAG, "Disconnecting...");
        stateMachine.processEvent(BluetoothStateMachine.Event.DISCONNECT);

        stopReading();
        closeSocket();

        stateMachine.processEvent(BluetoothStateMachine.Event.DISCONNECTED);
        notifyDisconnected();
    }

    /**
     * Start reading data from Bluetooth
     */
    private void startReading() {
        if (isReading) {
            return;
        }

        isReading = true;
        readerThread = new Thread(new ReaderRunnable());
        readerThread.start();
        Log.i(TAG, "Started reading Bluetooth data");
    }

    /**
     * Stop reading data
     */
    private void stopReading() {
        isReading = false;
        if (readerThread != null) {
            readerThread.interrupt();
            try {
                readerThread.join(1000);
            } catch (InterruptedException e) {
                Log.w(TAG, "Reader thread join interrupted");
            }
            readerThread = null;
        }
    }

    /**
     * Close Bluetooth socket
     */
    private void closeSocket() {
        try {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing input stream: " + e.getMessage());
        }

        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing socket: " + e.getMessage());
        }
    }

    /**
     * Reader runnable - reads data from Bluetooth
     */
    private class ReaderRunnable implements Runnable {
        private static final int BUFFER_SIZE = 1024;

        @Override
        public void run() {
            byte[] buffer = new byte[BUFFER_SIZE];
            StringBuilder messageBuffer = new StringBuilder();

            while (isReading && !Thread.interrupted()) {
                try {
                    if (inputStream == null) {
                        break;
                    }

                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead > 0) {
                        String data = new String(buffer, 0, bytesRead);
                        messageBuffer.append(data);

                        // Process complete messages (terminated by newline)
                        processMessages(messageBuffer);
                    }
                } catch (IOException e) {
                    if (isReading) {
                        Log.e(TAG, "Error reading data: " + e.getMessage());
                        stateMachine.processEvent(BluetoothStateMachine.Event.DISCONNECTED);
                        notifyDisconnected();
                        isReading = false;
                    }
                    break;
                }
            }

            Log.i(TAG, "Reader thread stopped");
        }

        private void processMessages(StringBuilder buffer) {
            String data = buffer.toString();
            int newlineIndex;

            while ((newlineIndex = data.indexOf('\n')) != -1) {
                String message = data.substring(0, newlineIndex).trim();
                data = data.substring(newlineIndex + 1);

                if (!message.isEmpty()) {
                    Log.d(TAG, "Received message: " + message);
                    notifyMessage(message);
                }
            }

            buffer.setLength(0);
            buffer.append(data);
        }
    }

    // Notification methods
    private void notifyConnected(String deviceName) {
        mainHandler.post(() -> {
            if (connectionListener != null) {
                connectionListener.onConnected(deviceName);
            }
        });
    }

    private void notifyDisconnected() {
        mainHandler.post(() -> {
            if (connectionListener != null) {
                connectionListener.onDisconnected();
            }
        });
    }

    private void notifyConnectionFailed(String error) {
        mainHandler.post(() -> {
            if (connectionListener != null) {
                connectionListener.onConnectionFailed(error);
            }
        });
    }

    private void notifyMessage(String message) {
        mainHandler.post(() -> {
            if (messageListener != null) {
                messageListener.onMessageReceived(message);
            }
        });
    }

    // Getters
    public boolean isConnected() {
        return stateMachine.isConnected();
    }

    public BluetoothStateMachine.State getState() {
        return stateMachine.getCurrentState();
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }
}
