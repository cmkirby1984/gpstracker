package com.fleet.gpstracker.bluetooth;

import android.util.Log;

/**
 * Bluetooth connection state machine
 * Manages connection states and transitions
 */
public class BluetoothStateMachine {
    private static final String TAG = "BluetoothStateMachine";

    public enum State {
        IDLE,           // Not connected, not scanning
        SCANNING,       // Scanning for devices
        CONNECTING,     // Attempting connection
        CONNECTED,      // Connected and ready
        DISCONNECTING,  // Disconnecting
        ERROR           // Error state
    }

    public enum Event {
        START_SCAN,
        STOP_SCAN,
        DEVICE_FOUND,
        CONNECT,
        CONNECTION_SUCCESS,
        CONNECTION_FAILED,
        DISCONNECT,
        DISCONNECTED,
        ERROR_OCCURRED
    }

    private State currentState;
    private StateChangeListener listener;

    public interface StateChangeListener {
        void onStateChanged(State oldState, State newState);
    }

    public BluetoothStateMachine() {
        this.currentState = State.IDLE;
    }

    public void setStateChangeListener(StateChangeListener listener) {
        this.listener = listener;
    }

    /**
     * Process event and transition state
     */
    public void processEvent(Event event) {
        State oldState = currentState;
        State newState = transition(currentState, event);

        if (oldState != newState) {
            Log.i(TAG, String.format("State transition: %s -> %s (event: %s)",
                                    oldState, newState, event));
            currentState = newState;

            if (listener != null) {
                listener.onStateChanged(oldState, newState);
            }
        }
    }

    /**
     * Determine next state based on current state and event
     */
    private State transition(State current, Event event) {
        switch (current) {
            case IDLE:
                switch (event) {
                    case START_SCAN:
                        return State.SCANNING;
                    case CONNECT:
                        return State.CONNECTING;
                    case ERROR_OCCURRED:
                        return State.ERROR;
                    default:
                        return current;
                }

            case SCANNING:
                switch (event) {
                    case STOP_SCAN:
                        return State.IDLE;
                    case DEVICE_FOUND:
                        return State.SCANNING;  // Stay in scanning
                    case CONNECT:
                        return State.CONNECTING;
                    case ERROR_OCCURRED:
                        return State.ERROR;
                    default:
                        return current;
                }

            case CONNECTING:
                switch (event) {
                    case CONNECTION_SUCCESS:
                        return State.CONNECTED;
                    case CONNECTION_FAILED:
                        return State.IDLE;
                    case ERROR_OCCURRED:
                        return State.ERROR;
                    default:
                        return current;
                }

            case CONNECTED:
                switch (event) {
                    case DISCONNECT:
                        return State.DISCONNECTING;
                    case DISCONNECTED:
                        return State.IDLE;
                    case ERROR_OCCURRED:
                        return State.ERROR;
                    default:
                        return current;
                }

            case DISCONNECTING:
                switch (event) {
                    case DISCONNECTED:
                        return State.IDLE;
                    case ERROR_OCCURRED:
                        return State.ERROR;
                    default:
                        return current;
                }

            case ERROR:
                // Can only recover from error by external reset
                if (event == Event.DISCONNECT || event == Event.DISCONNECTED) {
                    return State.IDLE;
                }
                return current;

            default:
                Log.e(TAG, "Unknown state: " + current);
                return State.ERROR;
        }
    }

    public State getCurrentState() {
        return currentState;
    }

    public boolean isConnected() {
        return currentState == State.CONNECTED;
    }

    public boolean isConnecting() {
        return currentState == State.CONNECTING;
    }

    public boolean canConnect() {
        return currentState == State.IDLE || currentState == State.SCANNING;
    }

    public void reset() {
        Log.i(TAG, "Resetting state machine to IDLE");
        State oldState = currentState;
        currentState = State.IDLE;

        if (listener != null && oldState != currentState) {
            listener.onStateChanged(oldState, currentState);
        }
    }
}
