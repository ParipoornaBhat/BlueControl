package com.example.mad_nnm23cs124;

import android.Manifest;
import android.annotation.SuppressLint;
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
import java.util.Objects;

@RequiresApi(api = Build.VERSION_CODES.P)
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private BluetoothHidManager hidManager;
    private TextView tvHeaderTitle, tvConnectionStatus, tvDeviceName;
    private View layoutTrackpad, layoutClickTop, layoutClickBottom;
    private View layoutMouseView, layoutKeyboardView, layoutMediaView, layoutPresenterView;
    private Button btnAirMouse;
    private TextView btnNavMouse, btnNavKeyboard, btnNavMedia, btnNavPresenter, btnNavMore;

    private float lastX, lastY;
    private SensorManager sensorManager;
    private Sensor gyroSensor;
    private boolean isGyroActive = false;
    private float mouseSensitivity = 1.0f;
    private boolean clickButtonsOnTop = false;
    private boolean disableTrackpadOnGyro = true;

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!Objects.equals(granted, Boolean.TRUE)) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    initBluetooth();
                } else {
                    Toast.makeText(this, "Bluetooth permissions required for remote control", Toast.LENGTH_LONG).show();
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

        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        tvDeviceName = findViewById(R.id.tvDeviceName);

        layoutMouseView = findViewById(R.id.layoutMouseView);
        layoutKeyboardView = findViewById(R.id.layoutKeyboardView);
        layoutMediaView = findViewById(R.id.layoutMediaView);
        layoutPresenterView = findViewById(R.id.layoutPresenterView);

        layoutTrackpad = findViewById(R.id.layoutTrackpad);
        layoutClickTop = findViewById(R.id.layoutClickTop);
        layoutClickBottom = findViewById(R.id.layoutClickBottom);

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
        } catch (Exception ignored) {
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

    @SuppressLint("MissingPermission")
    private void initBluetooth() {
        try {
            if (hidManager == null) {
                hidManager = new BluetoothHidManager(this);
            } else {
                hidManager.initProfileProxy(this);
            }

            hidManager.setConnectionListener((device, connected) -> {
                if (connected) {
                    tvConnectionStatus.setText("Connected");
                    tvConnectionStatus.setTextColor(getColor(android.R.color.holo_green_light));
                    String name = (device != null && device.getName() != null) ? device.getName() : "Bluetooth Device";
                    tvDeviceName.setText(name);
                    Toast.makeText(this, "Connected to " + name, Toast.LENGTH_SHORT).show();
                } else {
                    tvConnectionStatus.setText("Not Connected");
                    tvConnectionStatus.setTextColor(getColor(android.R.color.holo_red_light));
                    tvDeviceName.setText("Tap to connect Bluetooth HID");
                }
            });
        } catch (Exception ignored) {}
    }

    @SuppressLint({"MissingPermission", "ClickableViewAccessibility"})
    private void setupControls() {
        // Active device selector header tap
        tvDeviceName.setOnClickListener(v -> {
            if (hidManager != null) {
                DeviceListDialog.show(this, hidManager, device -> {
                    hidManager.connectDevice(device);
                    String name = (device != null && device.getName() != null) ? device.getName() : "Device";
                    Toast.makeText(this, "Connecting to " + name + "...", Toast.LENGTH_SHORT).show();
                });
            }
        });

        // Air Mouse Toggle Button
        btnAirMouse.setOnClickListener(v -> toggleAirMouse());

        // Settings Button
        findViewById(R.id.btnSettings).setOnClickListener(v -> showSettingsDialog());

        // Mouse Media Row
        findViewById(R.id.btnVolMinus).setOnClickListener(v -> sendConsumer(0x00EA));
        findViewById(R.id.btnVolPlus).setOnClickListener(v -> sendConsumer(0x00E9));
        findViewById(R.id.btnMute).setOnClickListener(v -> sendConsumer(0x00E2));
        findViewById(R.id.btnPlayPause).setOnClickListener(v -> sendConsumer(0x00CD));

        // Click Buttons
        View.OnClickListener leftClickListener = v -> sendMouse((byte) 0x01, (byte) 0, (byte) 0, (byte) 0);
        View.OnClickListener rightClickListener = v -> sendMouse((byte) 0x02, (byte) 0, (byte) 0, (byte) 0);

        findViewById(R.id.btnLeftClickBottom).setOnClickListener(leftClickListener);
        findViewById(R.id.btnRightClickBottom).setOnClickListener(rightClickListener);
        findViewById(R.id.btnLeftClickTop).setOnClickListener(leftClickListener);
        findViewById(R.id.btnRightClickTop).setOnClickListener(rightClickListener);

        // Quick Action Shortcuts
        findViewById(R.id.btnUndo).setOnClickListener(v -> sendShortcut((byte) 0x01, (byte) 0x1D));
        findViewById(R.id.btnRedo).setOnClickListener(v -> sendShortcut((byte) 0x01, (byte) 0x1C));
        findViewById(R.id.btnCopy).setOnClickListener(v -> sendShortcut((byte) 0x01, (byte) 0x06));
        findViewById(R.id.btnPaste).setOnClickListener(v -> sendShortcut((byte) 0x01, (byte) 0x19));

        // Touchpad Touch Movement
        layoutTrackpad.setOnTouchListener((v, event) -> {
            if (isGyroActive && disableTrackpadOnGyro) return true;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = event.getX();
                    lastY = event.getY();
                    v.performClick();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float currentX = event.getX();
                    float currentY = event.getY();
                    byte dx = (byte) ((currentX - lastX) * mouseSensitivity);
                    byte dy = (byte) ((currentY - lastY) * mouseSensitivity);
                    if (dx != 0 || dy != 0) {
                        sendMouse((byte) 0, dx, dy, (byte) 0);
                        lastX = currentX;
                        lastY = currentY;
                    }
                    break;
            }
            return true;
        });

        // Setup Keyboard Keys
        setupKeyboardControls();

        // Setup Media Remote Controls
        setupMediaRemoteControls();

        // Setup Presenter Controls
        setupPresenterControls();

        // Bottom Navigation Bar Mode Switching
        btnNavMouse.setOnClickListener(v -> switchMode("Mouse"));
        btnNavKeyboard.setOnClickListener(v -> switchMode("Keyboard"));
        btnNavMedia.setOnClickListener(v -> switchMode("Media"));
        btnNavPresenter.setOnClickListener(v -> switchMode("Presenter"));

        btnNavMore.setOnClickListener(v -> {
            MoreBottomSheetDialog bottomSheet = new MoreBottomSheetDialog();
            bottomSheet.setOnMenuSelectedListener(option -> {
                switch (option) {
                    case "Keyboard":
                        switchMode("Keyboard");
                        break;
                    case "Media":
                        switchMode("Media");
                        break;
                    case "Presenter":
                        switchMode("Presenter");
                        break;
                    case "Settings":
                        showSettingsDialog();
                        break;
                    default:
                        Toast.makeText(this, option + " selected", Toast.LENGTH_SHORT).show();
                        break;
                }
            });
            bottomSheet.show(getSupportFragmentManager(), "MoreBottomSheet");
        });
    }

    private void toggleAirMouse() {
        isGyroActive = !isGyroActive;
        if (isGyroActive) {
            if (gyroSensor != null && sensorManager != null) {
                sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME);
                btnAirMouse.setText("AIR MOUSE ON");
                btnAirMouse.setTextColor(Color.WHITE);
                btnAirMouse.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
                Toast.makeText(this, "Air Mouse ON", Toast.LENGTH_SHORT).show();
            } else {
                isGyroActive = false;
                Toast.makeText(this, "Gyroscope sensor unavailable", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (sensorManager != null) {
                sensorManager.unregisterListener(this);
            }
            btnAirMouse.setText("AIR MOUSE OFF");
            btnAirMouse.setTextColor(Color.parseColor("#94A3B8"));
            btnAirMouse.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1E293B")));
            Toast.makeText(this, "Air Mouse OFF", Toast.LENGTH_SHORT).show();
        }
    }

    private void switchMode(String mode) {
        layoutMouseView.setVisibility(View.GONE);
        layoutKeyboardView.setVisibility(View.GONE);
        layoutMediaView.setVisibility(View.GONE);
        layoutPresenterView.setVisibility(View.GONE);

        resetNavTabStyle(btnNavMouse);
        resetNavTabStyle(btnNavKeyboard);
        resetNavTabStyle(btnNavMedia);
        resetNavTabStyle(btnNavPresenter);
        resetNavTabStyle(btnNavMore);

        switch (mode) {
            case "Mouse":
                layoutMouseView.setVisibility(View.VISIBLE);
                highlightNavTab(btnNavMouse);
                tvHeaderTitle.setText("Mouse / Trackpad");
                break;
            case "Keyboard":
                layoutKeyboardView.setVisibility(View.VISIBLE);
                highlightNavTab(btnNavKeyboard);
                tvHeaderTitle.setText("Full PC Keyboard");
                break;
            case "Media":
                layoutMediaView.setVisibility(View.VISIBLE);
                highlightNavTab(btnNavMedia);
                tvHeaderTitle.setText("Multimedia Remote");
                break;
            case "Presenter":
                layoutPresenterView.setVisibility(View.VISIBLE);
                highlightNavTab(btnNavPresenter);
                tvHeaderTitle.setText("Presenter Remote");
                break;
        }
    }

    private void highlightNavTab(TextView tv) {
        tv.setTextColor(Color.WHITE);
        tv.getBackground().setTint(Color.parseColor("#2563EB"));
    }

    private void resetNavTabStyle(TextView tv) {
        tv.setTextColor(Color.parseColor("#94A3B8"));
        tv.getBackground().setTint(Color.TRANSPARENT);
    }

    private void setupKeyboardControls() {
        int[] keyIds = {
                R.id.btnKeyQ, R.id.btnKeyW, R.id.btnKeyE, R.id.btnKeyR, R.id.btnKeyT, R.id.btnKeyY, R.id.btnKeyU, R.id.btnKeyI, R.id.btnKeyO, R.id.btnKeyP,
                R.id.btnKeyA, R.id.btnKeyS, R.id.btnKeyD, R.id.btnKeyF, R.id.btnKeyG, R.id.btnKeyH, R.id.btnKeyJ, R.id.btnKeyK, R.id.btnKeyL,
                R.id.btnKeyZ, R.id.btnKeyX, R.id.btnKeyC, R.id.btnKeyV, R.id.btnKeyB, R.id.btnKeyN, R.id.btnKeyM
        };
        byte[] keyCodes = {
                0x14, 0x1A, 0x08, 0x15, 0x17, 0x1C, 0x15, 0x0C, 0x12, 0x13,
                0x04, 0x16, 0x07, 0x09, 0x0A, 0x0B, 0x0D, 0x0E, 0x0F,
                0x1D, 0x1B, 0x06, 0x19, 0x05, 0x11, 0x10
        };

        for (int i = 0; i < keyIds.length; i++) {
            final byte code = keyCodes[i];
            findViewById(keyIds[i]).setOnClickListener(v -> sendShortcut((byte) 0, code));
        }

        findViewById(R.id.btnKeySpace).setOnClickListener(v -> sendShortcut((byte) 0, (byte) 0x2C));
        findViewById(R.id.btnKeyEnter).setOnClickListener(v -> sendShortcut((byte) 0, (byte) 0x28));
        findViewById(R.id.btnKeyBackspace).setOnClickListener(v -> sendShortcut((byte) 0, (byte) 0x2A));
        findViewById(R.id.btnKeyEsc).setOnClickListener(v -> sendShortcut((byte) 0, (byte) 0x29));
        findViewById(R.id.btnKeyDel).setOnClickListener(v -> sendShortcut((byte) 0, (byte) 0x4C));

        findViewById(R.id.btnKeyCtrl).setOnClickListener(v -> Toast.makeText(this, "Ctrl modifier active", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnKeyAlt).setOnClickListener(v -> Toast.makeText(this, "Alt modifier active", Toast.LENGTH_SHORT).show());
    }

    private void setupMediaRemoteControls() {
        findViewById(R.id.btnDpadUp).setOnClickListener(v -> sendShortcut((byte) 0, (byte) 0x52));
        findViewById(R.id.btnDpadDown).setOnClickListener(v -> sendShortcut((byte) 0, (byte) 0x51));
        findViewById(R.id.btnDpadLeft).setOnClickListener(v -> sendShortcut((byte) 0, (byte) 0x50));
        findViewById(R.id.btnDpadRight).setOnClickListener(v -> sendShortcut((byte) 0, (byte) 0x4F));
        findViewById(R.id.btnDpadOk).setOnClickListener(v -> sendShortcut((byte) 0, (byte) 0x28));

        findViewById(R.id.btnMediaPrev).setOnClickListener(v -> sendConsumer(0x00B6));
        findViewById(R.id.btnMediaNext).setOnClickListener(v -> sendConsumer(0x00B5));
        findViewById(R.id.btnMediaMute).setOnClickListener(v -> sendConsumer(0x00E2));
        findViewById(R.id.btnMediaPlayPauseLarge).setOnClickListener(v -> sendConsumer(0x00CD));
    }

    private void setupPresenterControls() {
        findViewById(R.id.btnPresenterPrevSlide).setOnClickListener(v -> sendShortcut((byte) 0, (byte) 0x4B)); // Page Up
        findViewById(R.id.btnPresenterNextSlide).setOnClickListener(v -> sendShortcut((byte) 0, (byte) 0x4E)); // Page Down
        findViewById(R.id.btnPresenterStart).setOnClickListener(v -> sendShortcut((byte) 0, (byte) 0x3E));     // F5
        findViewById(R.id.btnPresenterBlank).setOnClickListener(v -> sendShortcut((byte) 0, (byte) 0x05));     // B key
    }

    private void showSettingsDialog() {
        String[] options = {
                "Sensitivity: Slow (0.5x)",
                "Sensitivity: Normal (1.0x)",
                "Sensitivity: Fast (1.5x)",
                "Sensitivity: Turbo (2.0x)",
                "Click Buttons Position: " + (clickButtonsOnTop ? "Top" : "Bottom"),
                "Disable Trackpad on Gyro: " + (disableTrackpadOnGyro ? "Enabled" : "Disabled")
        };

        new AlertDialog.Builder(this)
                .setTitle("BlueControl Settings")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            mouseSensitivity = 0.5f;
                            Toast.makeText(this, "Sensitivity updated", Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            mouseSensitivity = 1.0f;
                            Toast.makeText(this, "Sensitivity updated", Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            mouseSensitivity = 1.5f;
                            Toast.makeText(this, "Sensitivity updated", Toast.LENGTH_SHORT).show();
                            break;
                        case 3:
                            mouseSensitivity = 2.0f;
                            Toast.makeText(this, "Sensitivity updated", Toast.LENGTH_SHORT).show();
                            break;
                        case 4:
                            clickButtonsOnTop = !clickButtonsOnTop;
                            layoutClickTop.setVisibility(clickButtonsOnTop ? View.VISIBLE : View.GONE);
                            layoutClickBottom.setVisibility(clickButtonsOnTop ? View.GONE : View.VISIBLE);
                            Toast.makeText(this, "Click buttons moved to " + (clickButtonsOnTop ? "Top" : "Bottom"), Toast.LENGTH_SHORT).show();
                            break;
                        case 5:
                            disableTrackpadOnGyro = !disableTrackpadOnGyro;
                            Toast.makeText(this, "Disable trackpad on Gyro: " + disableTrackpadOnGyro, Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    private void sendShortcut(byte modifier, byte keyCode) {
        if (hidManager != null) hidManager.sendKeyReport(modifier, keyCode);
    }

    private void sendMouse(byte buttons, byte dx, byte dy, byte wheel) {
        if (hidManager != null) hidManager.sendMouseReport(buttons, dx, dy, wheel);
    }

    private void sendConsumer(int usageCode) {
        if (hidManager != null) hidManager.sendConsumerReport(usageCode);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isGyroActive && event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float gyroX = event.values[1];
            float gyroY = event.values[0];

            byte dx = (byte) (-gyroX * 10.0f * mouseSensitivity);
            byte dy = (byte) (gyroY * 10.0f * mouseSensitivity);

            if (dx != 0 || dy != 0) {
                sendMouse((byte) 0, dx, dy, (byte) 0);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onPause() {
        super.onPause();
        if (isGyroActive) {
            toggleAirMouse();
        }
    }
}
