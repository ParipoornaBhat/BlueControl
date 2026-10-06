package com.example.mad_nnm23cs124.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.mad_nnm23cs124.bluetooth.BluetoothHidManager;

import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.P)
public class DeviceListDialog {

    public interface OnDeviceSelectedListener {
        void onDeviceSelected(BluetoothDevice device);
    }

    @SuppressLint("MissingPermission")
    public static void show(Context context, BluetoothHidManager hidManager, OnDeviceSelectedListener listener) {
        List<BluetoothDevice> bondedDevices = hidManager.getBondedDevices();
        if (bondedDevices.isEmpty()) {
            new AlertDialog.Builder(context)
                    .setTitle("Bluetooth Devices")
                    .setMessage("No paired Bluetooth devices found. Please pair your computer or tablet in Android Bluetooth settings first.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        String[] deviceNames = new String[bondedDevices.size()];
        for (int i = 0; i < bondedDevices.size(); i++) {
            BluetoothDevice dev = bondedDevices.get(i);
            String name = dev.getName() != null ? dev.getName() : "Unknown Device";
            if (dev.equals(hidManager.getConnectedDevice())) {
                name += " (Connected)";
            }
            deviceNames[i] = name;
        }

        new AlertDialog.Builder(context)
                .setTitle("Select Active Target Device")
                .setItems(deviceNames, (dialog, which) -> {
                    BluetoothDevice selected = bondedDevices.get(which);
                    listener.onDeviceSelected(selected);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
