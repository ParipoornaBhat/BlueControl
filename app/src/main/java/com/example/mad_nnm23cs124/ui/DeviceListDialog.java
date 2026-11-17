package com.example.mad_nnm23cs124.ui;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.example.mad_nnm23cs124.R;
import com.example.mad_nnm23cs124.bluetooth.BluetoothHidManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.P)
public class DeviceListDialog {
    private static final String TAG = "DeviceListDialog";

    public interface OnDeviceSelectedListener {
        void onDeviceSelected(BluetoothDevice device);
    }

    @SuppressLint("MissingPermission")
    public static void show(Context context, BluetoothHidManager hidManager, OnDeviceSelectedListener listener) {
        if (context == null || hidManager == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setContentView(R.layout.dialog_bluetooth_devices);

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackgroundResource(android.R.color.transparent);
            try {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            } catch (Exception e) {
                Log.w(TAG, "Failed to configure BottomSheetBehavior", e);
            }
        }

        TextView btnDialogClose = dialog.findViewById(R.id.btnDialogClose);
        TextView tvHidStatus = dialog.findViewById(R.id.tvHidStatus);
        TextView tvAutoReconnectStatus = dialog.findViewById(R.id.tvAutoReconnectStatus);
        TextView tvBtAdapterStatus = dialog.findViewById(R.id.tvBtAdapterStatus);
        Button btnResetHid = dialog.findViewById(R.id.btnResetHid);
        Button btnEnableBt = dialog.findViewById(R.id.btnEnableBt);
        Button btnMakeDiscoverable = dialog.findViewById(R.id.btnMakeDiscoverable);
        Button btnOpenBtSettings = dialog.findViewById(R.id.btnOpenBtSettings);
        TextView btnRefreshDevices = dialog.findViewById(R.id.btnRefreshDevices);
        LinearLayout llDeviceList = dialog.findViewById(R.id.llDeviceList);
        View layoutEmptyState = dialog.findViewById(R.id.layoutEmptyState);

        if (btnDialogClose != null) {
            btnDialogClose.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnResetHid != null) {
            btnResetHid.setOnClickListener(v -> {
                hidManager.forceResetRegistration();
                Toast.makeText(context, "Resetting Bluetooth HID Service...", Toast.LENGTH_SHORT).show();
                if (tvHidStatus != null) {
                    tvHidStatus.setText("🟡 Resetting SDP profile...");
                    tvHidStatus.setTextColor(Color.parseColor("#EAB308"));
                }
            });
        }

        Runnable updateStatus = () -> {
            if (tvBtAdapterStatus != null) {
                boolean btEnabled = hidManager.isBluetoothEnabled();
                if (btEnabled) {
                    tvBtAdapterStatus.setText("Bluetooth Adapter: Enabled");
                    tvBtAdapterStatus.setTextColor(Color.parseColor("#94A3B8"));
                    if (btnEnableBt != null) btnEnableBt.setVisibility(View.GONE);
                } else {
                    tvBtAdapterStatus.setText("Bluetooth Adapter: Disabled");
                    tvBtAdapterStatus.setTextColor(Color.parseColor("#EF4444"));
                    if (btnEnableBt != null) btnEnableBt.setVisibility(View.VISIBLE);
                }
            }

            if (tvAutoReconnectStatus != null) {
                String lastName = hidManager.getLastConnectedDeviceName();
                if (lastName != null && !lastName.isEmpty()) {
                    tvAutoReconnectStatus.setText("⚡ Auto-Reconnect: Active (" + lastName + ")");
                    tvAutoReconnectStatus.setTextColor(Color.parseColor("#38BDF8"));
                } else {
                    tvAutoReconnectStatus.setText("⚡ Auto-Reconnect: Ready for next device");
                    tvAutoReconnectStatus.setTextColor(Color.parseColor("#94A3B8"));
                }
            }

            if (tvHidStatus != null) {
                if (hidManager.isAppRegistered()) {
                    tvHidStatus.setText("🟢 HID Profile: Registered & Ready");
                    tvHidStatus.setTextColor(Color.parseColor("#22C55E"));
                } else if (hidManager.isProfileProxyReady()) {
                    tvHidStatus.setText("🟡 HID Profile: Initializing SDP...");
                    tvHidStatus.setTextColor(Color.parseColor("#EAB308"));
                } else {
                    tvHidStatus.setText("🔴 HID Profile: Not Ready (Turn BT ON)");
                    tvHidStatus.setTextColor(Color.parseColor("#EF4444"));
                }
            }
        };

        Runnable populateDevices = () -> {
            if (llDeviceList == null) return;
            llDeviceList.removeAllViews();

            List<BluetoothDevice> bondedDevices;
            try {
                bondedDevices = hidManager.getBondedDevices();
            } catch (SecurityException se) {
                bondedDevices = new ArrayList<>();
            }

            if (bondedDevices == null || bondedDevices.isEmpty()) {
                llDeviceList.setVisibility(View.GONE);
                if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
                return;
            }

            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
            llDeviceList.setVisibility(View.VISIBLE);

            LayoutInflater inflater = LayoutInflater.from(context);
            BluetoothDevice activeConnected = hidManager.getConnectedDevice();

            for (BluetoothDevice device : bondedDevices) {
                View itemView = inflater.inflate(R.layout.item_bluetooth_device, llDeviceList, false);

                TextView tvIcon = itemView.findViewById(R.id.tvItemIcon);
                TextView tvName = itemView.findViewById(R.id.tvItemName);
                TextView tvSubtext = itemView.findViewById(R.id.tvItemSubtext);
                TextView tvStatus = itemView.findViewById(R.id.tvItemStatus);
                Button btnAction = itemView.findViewById(R.id.btnItemAction);

                String name = "Unknown Device";
                try {
                    if (device.getName() != null && !device.getName().isEmpty()) {
                        name = device.getName();
                    }
                } catch (SecurityException ignored) {}
                tvName.setText(name);

                String addr = device.getAddress() != null ? device.getAddress() : "00:00:00:00:00:00";
                tvIcon.setText(getDeviceIcon(device));
                tvSubtext.setText(getDeviceCategory(device) + " • " + addr);

                int connState = hidManager.getConnectionState(device);
                boolean isConnected = (activeConnected != null && addr.equalsIgnoreCase(activeConnected.getAddress()))
                        || connState == BluetoothProfile.STATE_CONNECTED;
                boolean isConnecting = connState == BluetoothProfile.STATE_CONNECTING;

                final String finalName = name;

                if (isConnected) {
                    tvStatus.setText("Connected (Active HID)");
                    tvStatus.setTextColor(Color.parseColor("#22C55E"));
                    btnAction.setText("Disconnect");
                    btnAction.setEnabled(true);
                    btnAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DC2626")));
                    btnAction.setOnClickListener(v -> {
                        hidManager.disconnectDevice(device);
                        Toast.makeText(context, "Disconnecting from " + finalName + "...", Toast.LENGTH_SHORT).show();
                        btnAction.setText("Disconnecting...");
                        btnAction.setEnabled(false);
                    });
                } else if (isConnecting) {
                    tvStatus.setText("Connecting...");
                    tvStatus.setTextColor(Color.parseColor("#F59E0B"));
                    btnAction.setText("Connecting...");
                    btnAction.setEnabled(false);
                    btnAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#475569")));
                } else {
                    tvStatus.setText("Paired");
                    tvStatus.setTextColor(Color.parseColor("#64748B"));
                    btnAction.setText("Connect");
                    btnAction.setEnabled(true);
                    btnAction.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
                    btnAction.setOnClickListener(v -> {
                        btnAction.setText("Connecting...");
                        btnAction.setEnabled(false);
                        if (listener != null) {
                            listener.onDeviceSelected(device);
                        } else {
                            hidManager.connectDevice(device);
                        }
                        Toast.makeText(context, "Connecting to " + finalName + "...", Toast.LENGTH_SHORT).show();
                    });
                }

                llDeviceList.addView(itemView);
            }
        };

        if (btnEnableBt != null) {
            btnEnableBt.setOnClickListener(v -> {
                try {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(enableBtIntent);
                } catch (Exception e) {
                    Toast.makeText(context, "Failed to prompt Bluetooth: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnMakeDiscoverable != null) {
            btnMakeDiscoverable.setOnClickListener(v -> {
                try {
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
                    discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(discoverableIntent);
                    Toast.makeText(context, "Phone is discoverable for 2 minutes. Pair from your Laptop now!", Toast.LENGTH_LONG).show();
                } catch (SecurityException se) {
                    Toast.makeText(context, "Bluetooth permission required to make phone discoverable", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnOpenBtSettings != null) {
            btnOpenBtSettings.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context, "Could not open Bluetooth settings", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnRefreshDevices != null) {
            btnRefreshDevices.setOnClickListener(v -> {
                updateStatus.run();
                populateDevices.run();
                Toast.makeText(context, "Device list refreshed", Toast.LENGTH_SHORT).show();
            });
        }

        updateStatus.run();
        populateDevices.run();

        BluetoothHidManager.ConnectionListener connectionListener = (dev, state) -> {
            updateStatus.run();
            populateDevices.run();
        };
        BluetoothHidManager.AppRegistrationListener regListener = (registered) -> {
            updateStatus.run();
        };

        hidManager.addConnectionListener(connectionListener);
        hidManager.addRegistrationListener(regListener);

        dialog.setOnDismissListener(d -> {
            hidManager.removeConnectionListener(connectionListener);
            hidManager.removeRegistrationListener(regListener);
        });

        try {
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing DeviceListDialog", e);
        }
    }

    private static String getDeviceIcon(BluetoothDevice device) {
        try {
            BluetoothClass bc = device.getBluetoothClass();
            if (bc != null) {
                int major = bc.getMajorDeviceClass();
                if (major == BluetoothClass.Device.Major.COMPUTER) {
                    return "💻";
                } else if (major == BluetoothClass.Device.Major.PHONE) {
                    return "📱";
                } else if (major == BluetoothClass.Device.Major.AUDIO_VIDEO) {
                    return "🎧";
                } else if (major == BluetoothClass.Device.Major.PERIPHERAL) {
                    return "⌨️";
                }
            }
        } catch (SecurityException ignored) {}
        return "💻";
    }

    private static String getDeviceCategory(BluetoothDevice device) {
        try {
            BluetoothClass bc = device.getBluetoothClass();
            if (bc != null) {
                int major = bc.getMajorDeviceClass();
                if (major == BluetoothClass.Device.Major.COMPUTER) {
                    return "Laptop / PC";
                } else if (major == BluetoothClass.Device.Major.PHONE) {
                    return "Phone / Tablet";
                } else if (major == BluetoothClass.Device.Major.AUDIO_VIDEO) {
                    return "Audio Device";
                } else if (major == BluetoothClass.Device.Major.PERIPHERAL) {
                    return "Input Peripheral";
                }
            }
        } catch (SecurityException ignored) {}
        return "Bluetooth Device";
    }
}
