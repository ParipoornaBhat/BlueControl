package com.example.mad_nnm23cs124.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@RequiresApi(api = Build.VERSION_CODES.P)
public class BluetoothHidManager {
    private static final String TAG = "BluetoothHidManager";

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
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
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        initProfileProxy();
    }

    public void initProfileProxy() {
        if (bluetoothAdapter != null && hidDevice == null) {
            try {
                bluetoothAdapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE);
            } catch (SecurityException se) {
                Log.w(TAG, "BLUETOOTH_CONNECT permission not yet granted: " + se.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Failed to getProfileProxy: " + e.getMessage(), e);
            }
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
    public void registerApp() {
        if (hidDevice == null) return;

        try {
            BluetoothHidDeviceAppSdpSettings sdpSettings = new BluetoothHidDeviceAppSdpSettings(
                    "BlueControl",
                    "Bluetooth Keyboard & Mouse",
                    "BlueControl",
                    BluetoothHidDevice.SUBCLASS1_COMBO,
                    HID_REPORT_DESCRIPTOR
            );

            hidDevice.registerApp(sdpSettings, null, null, executor, hidCallback);
        } catch (SecurityException se) {
            Log.w(TAG, "SecurityException while registering HID app: " + se.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error registering HID app: " + e.getMessage(), e);
        }
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
                Log.d(TAG, "Connected to: " + (device != null ? device.getName() : "Unknown"));
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

    public boolean isBluetoothEnabled() {
        try {
            return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
        } catch (Exception e) {
            return false;
        }
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
                Log.w(TAG, "SecurityException fetching bonded devices: " + se.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Error fetching bonded devices: " + e.getMessage(), e);
            }
        }
        return list;
    }

    @SuppressLint("MissingPermission")
    public void connectDevice(BluetoothDevice device) {
        if (hidDevice != null && device != null) {
            try {
                hidDevice.connect(device);
            } catch (SecurityException se) {
                Log.w(TAG, "SecurityException connecting device: " + se.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Error connecting device: " + e.getMessage(), e);
            }
        }
    }

    @SuppressLint("MissingPermission")
    public void disconnectDevice(BluetoothDevice device) {
        if (hidDevice != null && device != null) {
            try {
                hidDevice.disconnect(device);
            } catch (SecurityException se) {
                Log.w(TAG, "SecurityException disconnecting device: " + se.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Error disconnecting device: " + e.getMessage(), e);
            }
        }
    }

    @SuppressLint("MissingPermission")
    public void sendKeyReport(byte modifier, byte keycode) {
        if (hidDevice == null || connectedDevice == null) return;
        try {
            byte[] report = new byte[]{modifier, 0, keycode, 0, 0, 0, 0, 0};
            hidDevice.sendReport(connectedDevice, 1, report);

            mainHandler.postDelayed(() -> {
                try {
                    byte[] releaseReport = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
                    if (hidDevice != null && connectedDevice != null) {
                        hidDevice.sendReport(connectedDevice, 1, releaseReport);
                    }
                } catch (Exception ignored) {}
            }, 50);
        } catch (Exception e) {
            Log.e(TAG, "Error sending key report: " + e.getMessage());
        }
    }

    @SuppressLint("MissingPermission")
    public void sendMouseReport(byte buttons, byte dx, byte dy, byte wheel) {
        if (hidDevice == null || connectedDevice == null) return;
        try {
            byte[] report = new byte[]{buttons, dx, dy, wheel};
            hidDevice.sendReport(connectedDevice, 2, report);
        } catch (Exception e) {
            Log.e(TAG, "Error sending mouse report: " + e.getMessage());
        }
    }

    @SuppressLint("MissingPermission")
    public void sendConsumerReport(int usageCode) {
        if (hidDevice == null || connectedDevice == null) return;
        try {
            byte[] report = new byte[]{(byte) (usageCode & 0xFF), (byte) ((usageCode >> 8) & 0xFF)};
            hidDevice.sendReport(connectedDevice, 3, report);

            mainHandler.postDelayed(() -> {
                try {
                    byte[] release = new byte[]{0, 0};
                    if (hidDevice != null && connectedDevice != null) {
                        hidDevice.sendReport(connectedDevice, 3, release);
                    }
                } catch (Exception ignored) {}
            }, 50);
        } catch (Exception e) {
            Log.e(TAG, "Error sending consumer report: " + e.getMessage());
        }
    }

    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }

    public BluetoothDevice getConnectedDevice() {
        return connectedDevice;
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
