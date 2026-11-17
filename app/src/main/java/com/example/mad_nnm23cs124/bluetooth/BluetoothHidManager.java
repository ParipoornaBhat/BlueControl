package com.example.mad_nnm23cs124.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@RequiresApi(api = Build.VERSION_CODES.P)
public class BluetoothHidManager {
    private static final String TAG = "BluetoothHidManager";

    public static final int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    public static final int STATE_CONNECTING = BluetoothProfile.STATE_CONNECTING;
    public static final int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;
    public static final int STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;

    private final Context appContext;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothHidDevice hidDevice;
    private BluetoothDevice connectedDevice;
    private volatile boolean isAppRegistered = false;

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();
    private final List<AppRegistrationListener> registrationListeners = new CopyOnWriteArrayList<>();

    public interface ConnectionListener {
        void onConnectionStateChanged(BluetoothDevice device, int state);
    }

    public interface AppRegistrationListener {
        void onAppRegistrationChanged(boolean registered);
    }

    public BluetoothHidManager(Context context) {
        this.appContext = context != null ? context.getApplicationContext() : null;
        BluetoothManager bm = (context != null) ? context.getSystemService(BluetoothManager.class) : null;
        this.bluetoothAdapter = (bm != null) ? bm.getAdapter() : BluetoothAdapter.getDefaultAdapter();
        initProfileProxy(context);
    }

    public synchronized void initProfileProxy(Context context) {
        Context ctx = (context != null) ? context.getApplicationContext() : appContext;
        if (bluetoothAdapter != null && hidDevice == null && bluetoothAdapter.isEnabled() && ctx != null) {
            bluetoothAdapter.getProfileProxy(ctx, profileListener, BluetoothProfile.HID_DEVICE);
        }
    }

    private final BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = (BluetoothHidDevice) proxy;
                Log.d(TAG, "HID Device proxy connected. Registering SDP app...");
                registerApp();
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.d(TAG, "HID Device proxy disconnected.");
                hidDevice = null;
                isAppRegistered = false;
                notifyAppRegistration(false);
            }
        }
    };

    @SuppressLint("MissingPermission")
    public boolean registerApp() {
        if (hidDevice == null) {
            Log.w(TAG, "Cannot register app: hidDevice is null. Trying to acquire proxy...");
            initProfileProxy(appContext);
            return false;
        }

        BluetoothHidDeviceAppSdpSettings sdpSettings = new BluetoothHidDeviceAppSdpSettings(
                "BlueControl",
                "Bluetooth Keyboard & Mouse",
                "BlueControl",
                BluetoothHidDevice.SUBCLASS1_COMBO,
                HID_REPORT_DESCRIPTOR
        );

        try {
            return hidDevice.registerApp(sdpSettings, null, null, executor, hidCallback);
        } catch (Exception e) {
            Log.e(TAG, "Failed to register HID app", e);
            return false;
        }
    }

    @SuppressLint("MissingPermission")
    public void unregisterApp() {
        if (hidDevice != null && isAppRegistered) {
            try {
                hidDevice.unregisterApp();
            } catch (Exception e) {
                Log.w(TAG, "Error unregistering app", e);
            }
            isAppRegistered = false;
        }
    }

    public void retryRegistration() {
        if (hidDevice != null && !isAppRegistered) {
            registerApp();
        } else if (hidDevice == null) {
            initProfileProxy(appContext);
        }
    }

    private final BluetoothHidDevice.Callback hidCallback = new BluetoothHidDevice.Callback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean registered) {
            isAppRegistered = registered;
            Log.d(TAG, "onAppStatusChanged registered=" + registered + ", pluggedDevice=" + (pluggedDevice != null ? pluggedDevice.getName() : "none"));

            if (registered && pluggedDevice != null) {
                connectedDevice = pluggedDevice;
                notifyConnectionState(pluggedDevice, BluetoothProfile.STATE_CONNECTED);
            } else if (!registered) {
                connectedDevice = null;
            }

            notifyAppRegistration(registered);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChanged(BluetoothDevice device, int state) {
            String name = (device != null) ? device.getName() : "Unknown";
            Log.d(TAG, "onConnectionStateChanged device=" + name + ", state=" + stateToString(state));

            switch (state) {
                case BluetoothProfile.STATE_CONNECTED:
                    connectedDevice = device;
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    if (Objects.equals(connectedDevice, device)) {
                        connectedDevice = null;
                    }
                    break;
                default:
                    break;
            }

            notifyConnectionState(device, state);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onGetReport(BluetoothDevice device, byte type, byte id, int bufferSize) {
            Log.d(TAG, "onGetReport type=" + type + ", id=" + id + ", bufferSize=" + bufferSize);
            if (hidDevice == null || device == null) return;

            if (type == BluetoothHidDevice.REPORT_TYPE_INPUT) {
                if (id == 1) { // Keyboard
                    hidDevice.replyReport(device, type, id, new byte[8]);
                } else if (id == 2) { // Mouse
                    hidDevice.replyReport(device, type, id, new byte[4]);
                } else if (id == 3) { // Consumer Control
                    hidDevice.replyReport(device, type, id, new byte[2]);
                } else {
                    hidDevice.reportError(device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID);
                }
            } else if (type == BluetoothHidDevice.REPORT_TYPE_OUTPUT || type == BluetoothHidDevice.REPORT_TYPE_FEATURE) {
                byte[] empty = new byte[Math.max(1, Math.min(bufferSize > 0 ? bufferSize : 1, 8))];
                hidDevice.replyReport(device, type, id, empty);
            } else {
                hidDevice.reportError(device, BluetoothHidDevice.ERROR_RSP_NOT_SUPPORTED);
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onSetReport(BluetoothDevice device, byte type, byte id, byte[] data) {
            Log.d(TAG, "onSetReport type=" + type + ", id=" + id + ", dataLen=" + (data != null ? data.length : 0));
            if (hidDevice != null && device != null) {
                // Windows 10/11 & macOS require this handshake ACK (ERROR_RSP_SUCCESS) to maintain connection
                hidDevice.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS);
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onSetProtocol(BluetoothDevice device, byte protocol) {
            Log.d(TAG, "onSetProtocol protocol=" + protocol);
            if (hidDevice != null && device != null) {
                hidDevice.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS);
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onInterruptData(BluetoothDevice device, byte reportId, byte[] data) {
            Log.d(TAG, "onInterruptData device=" + (device != null ? device.getName() : "unknown") + ", reportId=" + reportId + ", len=" + (data != null ? data.length : 0));
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onVirtualCableUnplug(BluetoothDevice device) {
            Log.d(TAG, "onVirtualCableUnplug device=" + (device != null ? device.getName() : "unknown"));
            if (Objects.equals(connectedDevice, device)) {
                connectedDevice = null;
            }
            notifyConnectionState(device, BluetoothProfile.STATE_DISCONNECTED);
        }
    };

    private void notifyConnectionState(BluetoothDevice device, int state) {
        mainHandler.post(() -> {
            for (ConnectionListener listener : connectionListeners) {
                try {
                    listener.onConnectionStateChanged(device, state);
                } catch (Exception e) {
                    Log.e(TAG, "Error in connection listener", e);
                }
            }
        });
    }

    private void notifyAppRegistration(boolean registered) {
        mainHandler.post(() -> {
            for (AppRegistrationListener listener : registrationListeners) {
                try {
                    listener.onAppRegistrationChanged(registered);
                } catch (Exception e) {
                    Log.e(TAG, "Error in registration listener", e);
                }
            }
        });
    }

    public static String stateToString(int state) {
        switch (state) {
            case BluetoothProfile.STATE_CONNECTED:
                return "CONNECTED";
            case BluetoothProfile.STATE_CONNECTING:
                return "CONNECTING";
            case BluetoothProfile.STATE_DISCONNECTING:
                return "DISCONNECTING";
            case BluetoothProfile.STATE_DISCONNECTED:
                return "DISCONNECTED";
            default:
                return "UNKNOWN (" + state + ")";
        }
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public boolean isAppRegistered() {
        return isAppRegistered;
    }

    public boolean isConnected() {
        return connectedDevice != null;
    }

    public boolean isProfileProxyReady() {
        if (hidDevice == null && isBluetoothEnabled()) {
            initProfileProxy(appContext);
        }
        return hidDevice != null;
    }

    @SuppressLint("MissingPermission")
    public List<BluetoothDevice> getBondedDevices() {
        List<BluetoothDevice> list = new ArrayList<>();
        if (bluetoothAdapter != null) {
            try {
                Set<BluetoothDevice> bonded = bluetoothAdapter.getBondedDevices();
                if (bonded != null) {
                    list.addAll(bonded);
                }
            } catch (SecurityException se) {
                Log.w(TAG, "Missing permission to get bonded devices", se);
            }
        }
        return list;
    }

    @SuppressLint("MissingPermission")
    public BluetoothDevice getConnectedDevice() {
        if (connectedDevice != null) {
            return connectedDevice;
        }
        if (hidDevice != null) {
            try {
                List<BluetoothDevice> list = hidDevice.getConnectedDevices();
                if (list != null && !list.isEmpty()) {
                    connectedDevice = list.get(0);
                    return connectedDevice;
                }
            } catch (SecurityException se) {
                Log.w(TAG, "Missing permission to get connected devices", se);
            }
        }
        return null;
    }

    @SuppressLint("MissingPermission")
    public int getConnectionState(BluetoothDevice device) {
        if (hidDevice != null && device != null) {
            try {
                return hidDevice.getConnectionState(device);
            } catch (SecurityException se) {
                Log.w(TAG, "Missing permission to get connection state", se);
            }
        }
        return BluetoothProfile.STATE_DISCONNECTED;
    }

    @SuppressLint("MissingPermission")
    public boolean connectDevice(BluetoothDevice device) {
        if (device == null) return false;
        if (hidDevice == null) {
            initProfileProxy(appContext);
            Log.w(TAG, "hidDevice is not ready yet. Initializing proxy...");
            return false;
        }
        try {
            Log.d(TAG, "Initiating connect to: " + device.getName());
            return hidDevice.connect(device);
        } catch (SecurityException se) {
            Log.e(TAG, "Missing permission to connect device", se);
        } catch (Exception e) {
            Log.e(TAG, "Error connecting to device", e);
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    public boolean disconnectDevice(BluetoothDevice device) {
        if (device == null) return false;
        if (hidDevice == null) return false;
        try {
            Log.d(TAG, "Initiating disconnect from: " + device.getName());
            return hidDevice.disconnect(device);
        } catch (SecurityException se) {
            Log.e(TAG, "Missing permission to disconnect device", se);
        } catch (Exception e) {
            Log.e(TAG, "Error disconnecting from device", e);
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    public void sendKeyReport(byte modifier, byte keycode) {
        sendKeyPress(modifier, keycode, 50);
    }

    @SuppressLint("MissingPermission")
    public void sendKeyDown(byte modifier, byte keycode) {
        BluetoothDevice target = getConnectedDevice();
        if (hidDevice == null || target == null) return;
        byte[] report = new byte[]{modifier, 0, keycode, 0, 0, 0, 0, 0};
        try {
            hidDevice.sendReport(target, 1, report);
        } catch (SecurityException se) {
            Log.e(TAG, "Missing permission to send key down report", se);
        }
    }

    @SuppressLint("MissingPermission")
    public void sendKeyUp() {
        BluetoothDevice target = getConnectedDevice();
        if (hidDevice == null || target == null) return;
        byte[] report = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
        try {
            hidDevice.sendReport(target, 1, report);
        } catch (SecurityException se) {
            Log.e(TAG, "Missing permission to send key up report", se);
        }
    }

    @SuppressLint("MissingPermission")
    public void sendKeyPress(byte modifier, byte keycode, long releaseDelayMs) {
        BluetoothDevice target = getConnectedDevice();
        if (hidDevice == null || target == null) return;
        byte[] report = new byte[]{modifier, 0, keycode, 0, 0, 0, 0, 0};
        try {
            hidDevice.sendReport(target, 1, report);
        } catch (SecurityException se) {
            Log.e(TAG, "Missing permission to send report", se);
            return;
        }

        mainHandler.postDelayed(() -> {
            byte[] releaseReport = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
            BluetoothDevice current = getConnectedDevice();
            if (hidDevice != null && current != null) {
                try {
                    hidDevice.sendReport(current, 1, releaseReport);
                } catch (SecurityException se) {
                    Log.e(TAG, "Missing permission to send release report", se);
                }
            }
        }, Math.max(20, releaseDelayMs));
    }

    @SuppressLint("MissingPermission")
    public void sendString(String text) {
        if (text == null || text.isEmpty()) return;
        long delay = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            byte[] modAndKey = charToHidCode(c);
            if (modAndKey != null) {
                byte mod = modAndKey[0];
                byte key = modAndKey[1];
                final long scheduledDelay = delay;
                mainHandler.postDelayed(() -> sendKeyPress(mod, key, 30), scheduledDelay);
                delay += 50;
            }
        }
    }

    public static byte[] charToHidCode(char c) {
        if (c >= 'a' && c <= 'z') {
            return new byte[]{0, (byte) (0x04 + (c - 'a'))};
        } else if (c >= 'A' && c <= 'Z') {
            return new byte[]{0x02, (byte) (0x04 + (c - 'A'))}; // Shift modifier
        } else if (c >= '1' && c <= '9') {
            return new byte[]{0, (byte) (0x1E + (c - '1'))};
        } else if (c == '0') {
            return new byte[]{0, 0x27};
        } else if (c == ' ') {
            return new byte[]{0, 0x2C};
        } else if (c == '\n') {
            return new byte[]{0, 0x28};
        } else if (c == '\b') {
            return new byte[]{0, 0x2A};
        } else if (c == '\t') {
            return new byte[]{0, 0x2B};
        } else if (c == '-') {
            return new byte[]{0, 0x2D};
        } else if (c == '=') {
            return new byte[]{0, 0x2E};
        } else if (c == '[') {
            return new byte[]{0, 0x2F};
        } else if (c == ']') {
            return new byte[]{0, 0x30};
        } else if (c == '\\') {
            return new byte[]{0, 0x31};
        } else if (c == ';') {
            return new byte[]{0, 0x33};
        } else if (c == '\'') {
            return new byte[]{0, 0x34};
        } else if (c == '`') {
            return new byte[]{0, 0x35};
        } else if (c == ',') {
            return new byte[]{0, 0x36};
        } else if (c == '.') {
            return new byte[]{0, 0x37};
        } else if (c == '/') {
            return new byte[]{0, 0x38};
        } else if (c == '!') {
            return new byte[]{0x02, 0x1E};
        } else if (c == '@') {
            return new byte[]{0x02, 0x1F};
        } else if (c == '#') {
            return new byte[]{0x02, 0x20};
        } else if (c == '$') {
            return new byte[]{0x02, 0x21};
        } else if (c == '%') {
            return new byte[]{0x02, 0x22};
        } else if (c == '^') {
            return new byte[]{0x02, 0x23};
        } else if (c == '&') {
            return new byte[]{0x02, 0x24};
        } else if (c == '*') {
            return new byte[]{0x02, 0x25};
        } else if (c == '(') {
            return new byte[]{0x02, 0x26};
        } else if (c == ')') {
            return new byte[]{0x02, 0x27};
        } else if (c == '_') {
            return new byte[]{0x02, 0x2D};
        } else if (c == '+') {
            return new byte[]{0x02, 0x2E};
        } else if (c == '{') {
            return new byte[]{0x02, 0x2F};
        } else if (c == '}') {
            return new byte[]{0x02, 0x30};
        } else if (c == '|') {
            return new byte[]{0x02, 0x31};
        } else if (c == ':') {
            return new byte[]{0x02, 0x33};
        } else if (c == '"') {
            return new byte[]{0x02, 0x34};
        } else if (c == '~') {
            return new byte[]{0x02, 0x35};
        } else if (c == '<') {
            return new byte[]{0x02, 0x36};
        } else if (c == '>') {
            return new byte[]{0x02, 0x37};
        } else if (c == '?') {
            return new byte[]{0x02, 0x38};
        }
        return null;
    }

    @SuppressLint("MissingPermission")
    public void sendMouseReport(byte buttons, byte dx, byte dy, byte wheel) {
        BluetoothDevice target = getConnectedDevice();
        if (hidDevice == null || target == null) return;
        byte[] report = new byte[]{buttons, dx, dy, wheel};
        try {
            hidDevice.sendReport(target, 2, report);
        } catch (SecurityException se) {
            Log.e(TAG, "Missing permission to send mouse report", se);
        }
    }

    @SuppressLint("MissingPermission")
    public void sendConsumerReport(int usageCode) {
        BluetoothDevice target = getConnectedDevice();
        if (hidDevice == null || target == null) return;
        byte[] report = new byte[]{(byte) (usageCode & 0xFF), (byte) ((usageCode >> 8) & 0xFF)};
        try {
            hidDevice.sendReport(target, 3, report);
        } catch (SecurityException se) {
            Log.e(TAG, "Missing permission to send consumer report", se);
            return;
        }

        mainHandler.postDelayed(() -> {
            byte[] release = new byte[]{0, 0};
            BluetoothDevice current = getConnectedDevice();
            if (hidDevice != null && current != null) {
                try {
                    hidDevice.sendReport(current, 3, release);
                } catch (SecurityException se) {
                    Log.e(TAG, "Missing permission to send consumer release", se);
                }
            }
        }, 50);
    }

    public void addConnectionListener(ConnectionListener listener) {
        if (listener != null && !connectionListeners.contains(listener)) {
            connectionListeners.add(listener);
        }
    }

    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    public void setConnectionListener(ConnectionListener listener) {
        connectionListeners.clear();
        if (listener != null) {
            connectionListeners.add(listener);
        }
    }

    public void addRegistrationListener(AppRegistrationListener listener) {
        if (listener != null && !registrationListeners.contains(listener)) {
            registrationListeners.add(listener);
        }
    }

    public void removeRegistrationListener(AppRegistrationListener listener) {
        registrationListeners.remove(listener);
    }

    public void setRegistrationListener(AppRegistrationListener listener) {
        registrationListeners.clear();
        if (listener != null) {
            registrationListeners.add(listener);
        }
    }

    @SuppressLint("MissingPermission")
    public void cleanup(Context context) {
        unregisterApp();
        if (bluetoothAdapter != null && hidDevice != null) {
            try {
                bluetoothAdapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice);
            } catch (Exception e) {
                Log.w(TAG, "Error closing profile proxy", e);
            }
            hidDevice = null;
        }
    }

    private static final byte[] HID_REPORT_DESCRIPTOR = {
            (byte) 0x05, (byte) 0x01, // Usage Page (Generic Desktop)
            (byte) 0x09, (byte) 0x06, // Usage (Keyboard)
            (byte) 0xA1, (byte) 0x01, // Collection (Application)
            (byte) 0x85, (byte) 0x01, // Report ID (1)
            (byte) 0x05, (byte) 0x07, //   Usage Page (Key Codes)
            (byte) 0x19, (byte) 0xE0, //   Usage Minimum (224)
            (byte) 0x29, (byte) 0xE7, //   Usage Maximum (231)
            (byte) 0x15, (byte) 0x00, //   Logical Minimum (0)
            (byte) 0x25, (byte) 0x01, //   Logical Maximum (1)
            (byte) 0x75, (byte) 0x01, //   Report Size (1)
            (byte) 0x95, (byte) 0x08, //   Report Count (8)
            (byte) 0x81, (byte) 0x02, //   Input (Data, Variable, Absolute) - Modifier byte
            (byte) 0x75, (byte) 0x08, //   Report Size (8)
            (byte) 0x95, (byte) 0x01, //   Report Count (1)
            (byte) 0x81, (byte) 0x01, //   Input (Constant) - Reserved byte
            (byte) 0x19, (byte) 0x00, //   Usage Minimum (0)
            (byte) 0x29, (byte) 0x65, //   Usage Maximum (101)
            (byte) 0x15, (byte) 0x00, //   Logical Minimum (0)
            (byte) 0x25, (byte) 0x65, //   Logical Maximum (101)
            (byte) 0x75, (byte) 0x08, //   Report Size (8)
            (byte) 0x95, (byte) 0x06, //   Report Count (6)
            (byte) 0x81, (byte) 0x00, //   Input (Data, Array) - Key arrays
            (byte) 0xC0,              // End Collection

            // Mouse
            (byte) 0x05, (byte) 0x01, // Usage Page (Generic Desktop)
            (byte) 0x09, (byte) 0x02, // Usage (Mouse)
            (byte) 0xA1, (byte) 0x01, // Collection (Application)
            (byte) 0x85, (byte) 0x02, // Report ID (2)
            (byte) 0x09, (byte) 0x01, //   Usage (Pointer)
            (byte) 0xA1, (byte) 0x00, //   Collection (Physical)
            (byte) 0x05, (byte) 0x09, //     Usage Page (Buttons)
            (byte) 0x19, (byte) 0x01, //     Usage Minimum (1)
            (byte) 0x29, (byte) 0x03, //     Usage Maximum (3)
            (byte) 0x15, (byte) 0x00, //     Logical Minimum (0)
            (byte) 0x25, (byte) 0x01, //     Logical Maximum (1)
            (byte) 0x75, (byte) 0x01, //     Report Size (1)
            (byte) 0x95, (byte) 0x03, //     Report Count (3)
            (byte) 0x81, (byte) 0x02, //     Input (Data, Variable, Absolute) - Buttons
            (byte) 0x75, (byte) 0x05, //     Report Size (5)
            (byte) 0x95, (byte) 0x01, //     Report Count (1)
            (byte) 0x81, (byte) 0x01, //     Input (Constant) - Padding
            (byte) 0x05, (byte) 0x01, //     Usage Page (Generic Desktop)
            (byte) 0x09, (byte) 0x30, //     Usage (X)
            (byte) 0x09, (byte) 0x31, //     Usage (Y)
            (byte) 0x09, (byte) 0x38, //     Usage (Wheel)
            (byte) 0x15, (byte) 0x81, //     Logical Minimum (-127)
            (byte) 0x25, (byte) 0x7F, //     Logical Maximum (127)
            (byte) 0x75, (byte) 0x08, //     Report Size (8)
            (byte) 0x95, (byte) 0x03, //     Report Count (3)
            (byte) 0x81, (byte) 0x06, //     Input (Data, Variable, Relative)
            (byte) 0xC0,              //   End Collection
            (byte) 0xC0,              // End Collection

            // Consumer Control (Media)
            (byte) 0x05, (byte) 0x0C, // Usage Page (Consumer)
            (byte) 0x09, (byte) 0x01, // Usage (Consumer Control)
            (byte) 0xA1, (byte) 0x01, // Collection (Application)
            (byte) 0x85, (byte) 0x03, // Report ID (3)
            (byte) 0x15, (byte) 0x00, //   Logical Minimum (0)
            (byte) 0x26, (byte) 0x3C, (byte) 0x02, // Logical Maximum (572)
            (byte) 0x19, (byte) 0x00, //   Usage Minimum (0)
            (byte) 0x2A, (byte) 0x3C, (byte) 0x02, // Usage Maximum (572)
            (byte) 0x75, (byte) 0x10, //   Report Size (16)
            (byte) 0x95, (byte) 0x01, //   Report Count (1)
            (byte) 0x81, (byte) 0x00, //   Input (Data, Array)
            (byte) 0xC0               // End Collection
    };
}
