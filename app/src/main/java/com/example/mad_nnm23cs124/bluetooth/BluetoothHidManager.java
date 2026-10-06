package com.example.mad_nnm23cs124.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.BluetoothHidDeviceAppQosSettings;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BluetoothHidManager {
    private static final String TAG = "BluetoothHidManager";

    private final Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothHidDevice hidDevice;
    private BluetoothDevice connectedDevice;
    private boolean isRegistered = false;

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ConnectionListener connectionListener;

    public interface ConnectionListener {
        void onConnectionStateChanged(BluetoothDevice device, boolean connected);
    }

    public BluetoothHidManager(Context context) {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            bluetoothAdapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE);
        }
    }

    private final BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = (BluetoothHidDevice) proxy;
                registerApp();
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null;
                isRegistered = false;
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void registerApp() {
        if (hidDevice == null) return;

        BluetoothHidDeviceAppSdpSettings sdpSettings = new BluetoothHidDeviceAppSdpSettings(
                "BlueControl",
                "Bluetooth Keyboard & Mouse",
                "BlueControl",
                BluetoothHidDevice.SUBCLASS1_COMBO,
                HID_REPORT_DESCRIPTOR
        );

        hidDevice.registerApp(sdpSettings, null, null, executor, hidCallback);
    }

    private final BluetoothHidDevice.Callback hidCallback = new BluetoothHidDevice.Callback() {
        @Override
        public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean registered) {
            isRegistered = registered;
            Log.d(TAG, "HID App registered: " + registered);
        }

        @Override
        public void onConnectionStateChanged(BluetoothDevice device, int state) {
            if (state == BluetoothProfile.STATE_CONNECTED) {
                connectedDevice = device;
                Log.d(TAG, "Connected to: " + device.getName());
                if (connectionListener != null) {
                    mainHandler.post(() -> connectionListener.onConnectionStateChanged(device, true));
                }
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from: " + (device != null ? device.getName() : "unknown"));
                if (connectedDevice != null && connectedDevice.equals(device)) {
                    connectedDevice = null;
                }
                if (connectionListener != null) {
                    mainHandler.post(() -> connectionListener.onConnectionStateChanged(device, false));
                }
            }
        }
    };

    @SuppressLint("MissingPermission")
    public void sendKeyReport(byte modifier, byte keycode) {
        if (hidDevice == null || connectedDevice == null) return;
        // Report ID 1: Keyboard report [modifier, reserved, keycode1, keycode2, ...]
        byte[] report = new byte[]{modifier, 0, keycode, 0, 0, 0, 0, 0};
        hidDevice.sendReport(connectedDevice, 1, report);

        // Send key release after a short delay
        mainHandler.postDelayed(() -> {
            byte[] releaseReport = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
            if (hidDevice != null && connectedDevice != null) {
                hidDevice.sendReport(connectedDevice, 1, releaseReport);
            }
        }, 50);
    }

    @SuppressLint("MissingPermission")
    public void sendMouseReport(byte buttons, byte dx, byte dy, byte wheel) {
        if (hidDevice == null || connectedDevice == null) return;
        // Report ID 2: Mouse report [buttons, dx, dy, wheel]
        byte[] report = new byte[]{buttons, dx, dy, wheel};
        hidDevice.sendReport(connectedDevice, 2, report);
    }

    @SuppressLint("MissingPermission")
    public void sendConsumerReport(int usageCode) {
        if (hidDevice == null || connectedDevice == null) return;
        // Report ID 3: Consumer control (media) [usage_lo, usage_hi]
        byte[] report = new byte[]{(byte) (usageCode & 0xFF), (byte) ((usageCode >> 8) & 0xFF)};
        hidDevice.sendReport(connectedDevice, 3, report);

        mainHandler.postDelayed(() -> {
            byte[] release = new byte[]{0, 0};
            if (hidDevice != null && connectedDevice != null) {
                hidDevice.sendReport(connectedDevice, 3, release);
            }
        }, 50);
    }

    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }

    public BluetoothDevice getConnectedDevice() {
        return connectedDevice;
    }

    // Standard HID Report Descriptor for Keyboard (ID 1), Mouse (ID 2), and Consumer Control (ID 3)
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
