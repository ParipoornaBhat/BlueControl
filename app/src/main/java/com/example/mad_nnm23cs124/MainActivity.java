package com.example.mad_nnm23cs124;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mad_nnm23cs124.bluetooth.BluetoothHidManager;
import com.example.mad_nnm23cs124.ui.DeviceListDialog;
import com.example.mad_nnm23cs124.ui.MoreBottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.P)
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private BluetoothHidManager hidManager;
    private TextView tvConnectionStatus, tvDeviceName;
    private View layoutTrackpad;
    private Button btnAirMouse;
    private float lastX, lastY;

    private SensorManager sensorManager;
    private Sensor gyroSensor;
    private boolean isGyroActive = false;
    private float mouseSensitivity = 1.0f;

    private Button btnNavMouse, btnNavKeyboard, btnNavMedia, btnNavPresenter, btnNavMore;

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!Boolean.TRUE.equals(granted)) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    initBluetooth();
                } else {
                    Toast.makeText(this, "Bluetooth permissions are required to use HID remote features", Toast.LENGTH_LONG).show();
                    tvConnectionStatus.setText("Permission Denied");
                    tvConnectionStatus.setTextColor(getColor(android.R.color.holo_red_light));
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            EdgeToEdge.enable(this);
        } catch (Exception ignored) {}

        setContentView(R.layout.activity_main);

        View rootView = findViewById(R.id.main);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        tvDeviceName = findViewById(R.id.tvDeviceName);
        layoutTrackpad = findViewById(R.id.layoutTrackpad);
        btnAirMouse = findViewById(R.id.btnAirMouse);

        btnNavMouse = findViewById(R.id.btnNavMouse);
        btnNavKeyboard = findViewById(R.id.btnNavKeyboard);
        btnNavMedia = findViewById(R.id.btnNavMedia);
        btnNavPresenter = findViewById(R.id.btnNavPresenter);
        btnNavMore = findViewById(R.id.btnNavMore);

        try {
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            if (sensorManager != null) {
                gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            }
        } catch (Exception e) {
            gyroSensor = null;
        }

        setupControls();
        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            List<String> neededPermissions = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }

            if (!neededPermissions.isEmpty()) {
                permissionLauncher.launch(neededPermissions.toArray(new String[0]));
            } else {
                initBluetooth();
            }
        } else {
            initBluetooth();
        }
    }

    private void initBluetooth() {
        try {
            if (hidManager == null) {
                hidManager = new BluetoothHidManager(this);
            } else {
                hidManager.initProfileProxy();
            }

            hidManager.setConnectionListener((device, connected) -> {
                if (connected) {
                    tvConnectionStatus.setText("Connected");
                    tvConnectionStatus.setTextColor(getColor(android.R.color.holo_green_light));
                    tvDeviceName.setText(device.getName() != null ? device.getName() : "Bluetooth Device");
                    Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
                } else {
                    tvConnectionStatus.setText("Not Connected");
                    tvConnectionStatus.setTextColor(getColor(android.R.color.holo_red_light));
                    tvDeviceName.setText("Tap to connect Bluetooth HID");
                }
            });

            tvDeviceName.setOnClickListener(v -> {
                if (hidManager != null) {
                    DeviceListDialog.show(this, hidManager, device -> {
                        hidManager.connectDevice(device);
                        Toast.makeText(this, "Connecting to " + device.getName() + "...", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Bluetooth HID initialization error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupControls() {
        // Media buttons
        findViewById(R.id.btnVolMinus).setOnClickListener(v -> { if (hidManager != null) hidManager.sendConsumerReport(0x00EA); });
        findViewById(R.id.btnVolPlus).setOnClickListener(v -> { if (hidManager != null) hidManager.sendConsumerReport(0x00E9); });
        findViewById(R.id.btnMute).setOnClickListener(v -> { if (hidManager != null) hidManager.sendConsumerReport(0x00E2); });
        findViewById(R.id.btnPlayPause).setOnClickListener(v -> { if (hidManager != null) hidManager.sendConsumerReport(0x00CD); });

        // Mouse clicks
        findViewById(R.id.btnLeftClick).setOnClickListener(v -> { if (hidManager != null) hidManager.sendMouseReport((byte) 0x01, (byte) 0, (byte) 0, (byte) 0); });
        findViewById(R.id.btnRightClick).setOnClickListener(v -> { if (hidManager != null) hidManager.sendMouseReport((byte) 0x02, (byte) 0, (byte) 0, (byte) 0); });

        // Quick Actions (Undo: Ctrl+Z, Redo: Ctrl+Y, Copy: Ctrl+C, Paste: Ctrl+V)
        findViewById(R.id.btnUndo).setOnClickListener(v -> sendShortcut((byte) 0x01, (byte) 0x1D));
        findViewById(R.id.btnRedo).setOnClickListener(v -> sendShortcut((byte) 0x01, (byte) 0x1C));
        findViewById(R.id.btnCopy).setOnClickListener(v -> sendShortcut((byte) 0x01, (byte) 0x06));
        findViewById(R.id.btnPaste).setOnClickListener(v -> sendShortcut((byte) 0x01, (byte) 0x19));

        // Settings Button (Mouse Sensitivity)
        findViewById(R.id.btnSettings).setOnClickListener(v -> showSettingsDialog());

        // Modern Floating Bottom Navigation Bar
        if (btnNavMouse != null) {
            btnNavMouse.setOnClickListener(v -> {
                selectNavTab(btnNavMouse);
                Toast.makeText(this, "Mouse & Trackpad mode", Toast.LENGTH_SHORT).show();
            });
        }
        if (btnNavKeyboard != null) {
            btnNavKeyboard.setOnClickListener(v -> {
                selectNavTab(btnNavKeyboard);
                Toast.makeText(this, "Full PC Keyboard mode", Toast.LENGTH_SHORT).show();
            });
        }
        if (btnNavMedia != null) {
            btnNavMedia.setOnClickListener(v -> {
                selectNavTab(btnNavMedia);
                Toast.makeText(this, "Multimedia Remote mode", Toast.LENGTH_SHORT).show();
            });
        }
        if (btnNavPresenter != null) {
            btnNavPresenter.setOnClickListener(v -> {
                selectNavTab(btnNavPresenter);
                Toast.makeText(this, "Presenter Mode", Toast.LENGTH_SHORT).show();
            });
        }

        // 5th option: "More ☰" slides up bottom sheet drawer from bottom
        if (btnNavMore != null) {
            btnNavMore.setOnClickListener(v -> {
                MoreBottomSheetDialog bottomSheet = new MoreBottomSheetDialog();
                bottomSheet.setOnMenuSelectedListener(option -> {
                    Toast.makeText(this, "Selected: " + option, Toast.LENGTH_SHORT).show();
                    if ("Settings".equals(option)) {
                        showSettingsDialog();
                    }
                });
                bottomSheet.show(getSupportFragmentManager(), "MoreBottomSheet");
            });
        }

        // Hold-to-Use Air Mouse (Gyro) Button
        if (btnAirMouse != null) {
            btnAirMouse.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isGyroActive = true;
                        if (gyroSensor != null && sensorManager != null) {
                            sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME);
                            btnAirMouse.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
                        } else {
                            Toast.makeText(this, "Gyroscope sensor not available", Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        isGyroActive = false;
                        if (gyroSensor != null && sensorManager != null) {
                            sensorManager.unregisterListener(this, gyroSensor);
                            btnAirMouse.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1E293B")));
                        }
                        return true;
                }
                return false;
            });
        }

        // Trackpad Touch Movement
        if (layoutTrackpad != null) {
            layoutTrackpad.setOnTouchListener((v, event) -> {
                if (isGyroActive) return true; // Disable touch trackpad when gyro is active
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = event.getX();
                        lastY = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float currentX = event.getX();
                        float currentY = event.getY();
                        byte dx = (byte) ((currentX - lastX) * mouseSensitivity);
                        byte dy = (byte) ((currentY - lastY) * mouseSensitivity);
                        if (dx != 0 || dy != 0) {
                            if (hidManager != null) {
                                hidManager.sendMouseReport((byte) 0, dx, dy, (byte) 0);
                            }
                            lastX = currentX;
                            lastY = currentY;
                        }
                        break;
                }
                return true;
            });
        }
    }

    private void selectNavTab(Button activeBtn) {
        Button[] navButtons = {btnNavMouse, btnNavKeyboard, btnNavMedia, btnNavPresenter, btnNavMore};
        for (Button btn : navButtons) {
            if (btn == null) continue;
            if (btn == activeBtn) {
                btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
                btn.setTextColor(Color.WHITE);
                ViewCompat.setElevation(btn, 4f);
            } else {
                btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#00000000")));
                btn.setTextColor(Color.parseColor("#94A3B8"));
                ViewCompat.setElevation(btn, 0f);
            }
        }
    }

    private void showSettingsDialog() {
        String[] options = {"Slow (0.5x)", "Normal (1.0x)", "Fast (1.5x)", "Turbo (2.0x)"};
        float[] values = {0.5f, 1.0f, 1.5f, 2.0f};

        int checkedItem = 1;
        if (mouseSensitivity == 0.5f) checkedItem = 0;
        else if (mouseSensitivity == 1.0f) checkedItem = 1;
        else if (mouseSensitivity == 1.5f) checkedItem = 2;
        else if (mouseSensitivity == 2.0f) checkedItem = 3;

        new AlertDialog.Builder(this)
                .setTitle("Mouse Sensitivity")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    mouseSensitivity = values[which];
                    Toast.makeText(this, "Sensitivity set to " + options[which], Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .show();
    }

    private void sendShortcut(byte modifier, byte keyCode) {
        if (hidManager != null) {
            hidManager.sendKeyReport(modifier, keyCode);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isGyroActive && event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float gyroX = event.values[1]; // Roll / Pitch mapping
            float gyroY = event.values[0];

            byte dx = (byte) (-gyroX * 10.0f * mouseSensitivity);
            byte dy = (byte) (gyroY * 10.0f * mouseSensitivity);

            if (dx != 0 || dy != 0) {
                if (hidManager != null) {
                    hidManager.sendMouseReport((byte) 0, dx, dy, (byte) 0);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isGyroActive && sensorManager != null) {
            sensorManager.unregisterListener(this);
            isGyroActive = false;
        }
    }
}
