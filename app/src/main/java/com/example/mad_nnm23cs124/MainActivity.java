package com.example.mad_nnm23cs124;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
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
import com.example.mad_nnm23cs124.ui.AdvancedTrackpadView;
import com.example.mad_nnm23cs124.ui.DeviceListDialog;
import com.example.mad_nnm23cs124.ui.MoreBottomSheetDialog;
import com.example.mad_nnm23cs124.utils.ThemeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RequiresApi(api = Build.VERSION_CODES.P)
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private BluetoothHidManager hidManager;
    private TextView tvHeaderTitle, tvConnectionStatus, tvDeviceName;
    private Button btnBluetooth;
    private View layoutTrackpad, layoutClickTop, layoutClickBottom;
    private View layoutMouseView, layoutKeyboardView, layoutMediaView, layoutPresenterView;
    private Button btnAirMouse, btnKeyCtrl, btnKeyAlt;
    private TextView btnNavMouse, btnNavKeyboard, btnNavMedia, btnNavPresenter, btnNavMore;

    // Live Mobile Typing & Speech-to-Text Input Bar
    private EditText etSendText;
    private View btnSendText;
    private View btnLiveMic;

    private Vibrator vibrator;
    private SensorManager sensorManager;
    private Sensor gyroSensor;
    private boolean isGyroActive = false;
    public static final String PREF_KEY_INVERT_SCROLL = "trackpad_invert_scroll";
    public static final String PREF_KEY_INVERT_GYRO_X = "gyro_invert_x";
    public static final String PREF_KEY_INVERT_GYRO_Y = "gyro_invert_y";
    public static final String PREF_KEY_SENSITIVITY = "mouse_sensitivity";

    private float mouseSensitivity = 1.0f;
    private boolean invertScroll = true;
    private boolean invertGyroX = false;
    private boolean invertGyroY = false;
    private boolean clickButtonsOnTop = false;
    private boolean disableTrackpadOnGyro = true;

    private boolean isCtrlActive = false;
    private boolean isAltActive = false;
    private boolean isLeftClickHeld = false;
    private boolean isRightClickHeld = false;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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
                    if (tvConnectionStatus != null) {
                        tvConnectionStatus.setText("● Permission Denied");
                        tvConnectionStatus.setTextColor(Color.parseColor("#EF4444"));
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> speechRecognizerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) {
                        String spokenText = matches.get(0);
                        if (etSendText != null) {
                            etSendText.setText(spokenText);
                            etSendText.setSelection(spokenText.length());
                        }
                        sendStringToHid(spokenText);
                    }
                }
            }
    );

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_TURNING_OFF:
                    case BluetoothAdapter.STATE_OFF:
                        updateConnectionStatusUI(null, BluetoothProfile.STATE_DISCONNECTED);
                        if (tvConnectionStatus != null) {
                            tvConnectionStatus.setText("● Bluetooth Off");
                            tvConnectionStatus.setTextColor(Color.parseColor("#EF4444"));
                        }
                        if (tvDeviceName != null) {
                            tvDeviceName.setText("Bluetooth is disabled");
                        }
                        break;
                    case BluetoothAdapter.STATE_ON:
                        if (hidManager != null) {
                            hidManager.initProfileProxy(MainActivity.this);
                            hidManager.retryRegistration();
                            mainHandler.postDelayed(() -> hidManager.autoReconnect(), 1000);
                        }
                        updateConnectionStatusUI(null, BluetoothProfile.STATE_DISCONNECTED);
                        break;
                }
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                if (hidManager != null && !hidManager.isConnected()) {
                    Log.d("MainActivity", "Screen turned ON. Triggering auto-reconnect...");
                    hidManager.autoReconnect();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeManager.applyTheme(this);

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

        initHaptics();

        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        tvDeviceName = findViewById(R.id.tvDeviceName);
        btnBluetooth = findViewById(R.id.btnBluetooth);

        layoutMouseView = findViewById(R.id.layoutMouseView);
        layoutKeyboardView = findViewById(R.id.layoutKeyboardView);
        layoutMediaView = findViewById(R.id.layoutMediaView);
        layoutPresenterView = findViewById(R.id.layoutPresenterView);

        layoutTrackpad = findViewById(R.id.layoutTrackpad);
        layoutClickTop = findViewById(R.id.layoutClickTop);
        layoutClickBottom = findViewById(R.id.layoutClickBottom);

        btnAirMouse = findViewById(R.id.btnAirMouse);
        btnKeyCtrl = findViewById(R.id.btnKeyCtrl);
        btnKeyAlt = findViewById(R.id.btnKeyAlt);

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

        SharedPreferences prefs = getSharedPreferences(ThemeManager.PREF_NAME, Context.MODE_PRIVATE);
        invertScroll = prefs.getBoolean(PREF_KEY_INVERT_SCROLL, true);
        invertGyroX = prefs.getBoolean(PREF_KEY_INVERT_GYRO_X, false);
        invertGyroY = prefs.getBoolean(PREF_KEY_INVERT_GYRO_Y, false);
        mouseSensitivity = prefs.getFloat(PREF_KEY_SENSITIVITY, 1.0f);

        setupControls();
        setupLiveTypingControls();
        checkAndRequestPermissions();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(bluetoothStateReceiver, filter);

        // Default open Mouse / Trackpad view
        switchMode("Mouse");
    }

    private void initHaptics() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vm != null) {
                    vibrator = vm.getDefaultVibrator();
                }
            } else {
                vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            }
        } catch (Exception ignored) {}
    }

    private void performHaptic() {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(15);
                }
            }
        } catch (Exception ignored) {}
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
            hidManager = BluetoothHidManager.getInstance(this);
            hidManager.initProfileProxy(this);
            hidManager.setConnectionListener(this::updateConnectionStatusUI);

            BluetoothDevice current = hidManager.getConnectedDevice();
            if (current != null) {
                int state = hidManager.getConnectionState(current);
                updateConnectionStatusUI(current, state != 0 ? state : BluetoothProfile.STATE_CONNECTED);
            } else {
                updateConnectionStatusUI(null, BluetoothProfile.STATE_DISCONNECTED);
                mainHandler.postDelayed(() -> {
                    if (hidManager != null && !hidManager.isConnected()) {
                        hidManager.autoReconnect();
                    }
                }, 800);
            }
        } catch (Exception ignored) {}
    }

    private void updateConnectionStatusUI(BluetoothDevice device, int state) {
        mainHandler.post(() -> {
            if (tvConnectionStatus == null) return;
            switch (state) {
                case BluetoothProfile.STATE_CONNECTED:
                    tvConnectionStatus.setText("● Connected");
                    tvConnectionStatus.setTextColor(Color.parseColor("#22C55E"));
                    String name = null;
                    if (device != null) {
                        try {
                            name = device.getName();
                        } catch (SecurityException ignored) {}
                    }
                    if (name == null || name.trim().isEmpty()) {
                        name = "Bluetooth HID Connected";
                    }
                    if (tvDeviceName != null) {
                        tvDeviceName.setText(name);
                    }
                    if (btnBluetooth != null) {
                        btnBluetooth.setText("⚡ DEVICES");
                    }
                    break;

                case BluetoothProfile.STATE_CONNECTING:
                    tvConnectionStatus.setText("● Connecting...");
                    tvConnectionStatus.setTextColor(Color.parseColor("#F59E0B"));
                    String connectingName = null;
                    if (device != null) {
                        try {
                            connectingName = device.getName();
                        } catch (SecurityException ignored) {}
                    }
                    if (tvDeviceName != null) {
                        tvDeviceName.setText(connectingName != null ? ("Connecting to " + connectingName + "...") : "Connecting to device...");
                    }
                    if (btnBluetooth != null) {
                        btnBluetooth.setText("⏳ PAIRING");
                    }
                    break;

                case BluetoothProfile.STATE_DISCONNECTING:
                    tvConnectionStatus.setText("● Disconnecting...");
                    tvConnectionStatus.setTextColor(Color.parseColor("#F59E0B"));
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                default:
                    tvConnectionStatus.setText("● Not Connected");
                    tvConnectionStatus.setTextColor(Color.parseColor("#EF4444"));
                    String lastName = (hidManager != null) ? hidManager.getLastConnectedDeviceName() : null;
                    if (lastName != null && !lastName.trim().isEmpty()) {
                        if (tvDeviceName != null) {
                            tvDeviceName.setText("Tap to connect: " + lastName);
                        }
                        if (btnBluetooth != null) {
                            btnBluetooth.setText("⚡ RECONNECT");
                        }
                    } else {
                        if (tvDeviceName != null) {
                            tvDeviceName.setText("Tap to connect Bluetooth HID");
                        }
                        if (btnBluetooth != null) {
                            btnBluetooth.setText("⚡ PAIR");
                        }
                    }
                    break;
            }
        });
    }

    private void openDeviceListDialog() {
        performHaptic();
        if (hidManager != null) {
            DeviceListDialog.show(this, hidManager, device -> {
                hidManager.connectDevice(device);
                String name = "Device";
                try {
                    if (device != null && device.getName() != null) {
                        name = device.getName();
                    }
                } catch (SecurityException ignored) {}
                Toast.makeText(this, "Connecting to " + name + "...", Toast.LENGTH_SHORT).show();
            });
        } else {
            Toast.makeText(this, "Bluetooth HID Manager is initializing...", Toast.LENGTH_SHORT).show();
        }
    }

    private void bindClick(int id, View.OnClickListener listener) {
        View view = findViewById(id);
        if (view != null) {
            view.setOnClickListener(v -> {
                performHaptic();
                listener.onClick(v);
            });
        }
    }

    @SuppressLint({"MissingPermission"})
    private void setupControls() {
        if (btnBluetooth != null) {
            btnBluetooth.setOnClickListener(v -> {
                performHaptic();
                if (hidManager != null && !hidManager.isConnected() && hidManager.getLastConnectedDevice() != null) {
                    Toast.makeText(this, "Connecting to " + hidManager.getLastConnectedDeviceName() + "...", Toast.LENGTH_SHORT).show();
                    hidManager.autoReconnect();
                } else {
                    openDeviceListDialog();
                }
            });
            btnBluetooth.setOnLongClickListener(v -> {
                performHaptic();
                openDeviceListDialog();
                return true;
            });
        }
        if (tvDeviceName != null) {
            tvDeviceName.setOnClickListener(v -> {
                performHaptic();
                if (hidManager != null && !hidManager.isConnected() && hidManager.getLastConnectedDevice() != null) {
                    Toast.makeText(this, "Connecting to " + hidManager.getLastConnectedDeviceName() + "...", Toast.LENGTH_SHORT).show();
                    hidManager.autoReconnect();
                } else {
                    openDeviceListDialog();
                }
            });
        }
        if (tvConnectionStatus != null) {
            tvConnectionStatus.setOnClickListener(v -> openDeviceListDialog());
        }

        if (btnAirMouse != null) {
            btnAirMouse.setOnClickListener(v -> {
                performHaptic();
                toggleAirMouse();
            });
            btnAirMouse.setOnLongClickListener(v -> {
                performHaptic();
                invertGyroX = !invertGyroX;
                invertGyroY = !invertGyroY;
                SharedPreferences p = getSharedPreferences(ThemeManager.PREF_NAME, Context.MODE_PRIVATE);
                p.edit().putBoolean(PREF_KEY_INVERT_GYRO_X, invertGyroX)
                        .putBoolean(PREF_KEY_INVERT_GYRO_Y, invertGyroY).apply();
                Toast.makeText(this, "Air Mouse axes flipped (" + (invertGyroX ? "Inverted" : "Normal") + ")", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        bindClick(R.id.btnSettings, v -> showSettingsDialog());

        bindClick(R.id.btnVolMinus, v -> sendConsumer(0x00EA));
        bindClick(R.id.btnVolPlus, v -> sendConsumer(0x00E9));
        bindClick(R.id.btnMute, v -> sendConsumer(0x00E2));
        bindClick(R.id.btnPlayPause, v -> sendConsumer(0x00CD));

        View.OnTouchListener leftTouchListener = (v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isLeftClickHeld = true;
                    performHaptic();
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
                    sendMouse((byte) 0x01, (byte) 0, (byte) 0, (byte) 0);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isLeftClickHeld = false;
                    performHaptic();
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1E293B")));
                    sendMouse((byte) 0x00, (byte) 0, (byte) 0, (byte) 0);
                    return true;
            }
            return false;
        };

        View.OnTouchListener rightTouchListener = (v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isRightClickHeld = true;
                    performHaptic();
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
                    sendMouse((byte) 0x02, (byte) 0, (byte) 0, (byte) 0);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isRightClickHeld = false;
                    performHaptic();
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1E293B")));
                    sendMouse((byte) 0x00, (byte) 0, (byte) 0, (byte) 0);
                    return true;
            }
            return false;
        };

        View btnLeftBottom = findViewById(R.id.btnLeftClickBottom);
        if (btnLeftBottom != null) btnLeftBottom.setOnTouchListener(leftTouchListener);

        View btnRightBottom = findViewById(R.id.btnRightClickBottom);
        if (btnRightBottom != null) btnRightBottom.setOnTouchListener(rightTouchListener);

        View btnLeftTop = findViewById(R.id.btnLeftClickTop);
        if (btnLeftTop != null) btnLeftTop.setOnTouchListener(leftTouchListener);

        View btnRightTop = findViewById(R.id.btnRightClickTop);
        if (btnRightTop != null) btnRightTop.setOnTouchListener(rightTouchListener);

        bindClick(R.id.btnUndo, v -> sendShortcut((byte) 0x01, (byte) 0x1D));
        bindClick(R.id.btnRedo, v -> sendShortcut((byte) 0x01, (byte) 0x1C));
        bindClick(R.id.btnCopy, v -> sendShortcut((byte) 0x01, (byte) 0x06));
        bindClick(R.id.btnPaste, v -> sendShortcut((byte) 0x01, (byte) 0x19));

        // Advanced Trackpad with Multi-Touch Gestures & Haptic Feedback
        if (layoutTrackpad instanceof AdvancedTrackpadView) {
            AdvancedTrackpadView trackpad = (AdvancedTrackpadView) layoutTrackpad;
            trackpad.setInvertScroll(invertScroll);
            trackpad.setSensitivity(mouseSensitivity);
            trackpad.setTrackpadEventListener(new AdvancedTrackpadView.TrackpadEventListener() {
                @Override
                public void onMouseMove(byte dx, byte dy) {
                    if (isGyroActive && disableTrackpadOnGyro) return;
                    byte buttons = 0;
                    if (isLeftClickHeld) buttons |= 0x01;
                    if (isRightClickHeld) buttons |= 0x02;
                    sendMouse(buttons, (byte) (dx * mouseSensitivity), (byte) (dy * mouseSensitivity), (byte) 0);
                }

                @Override
                public void onMouseDragStart() {
                    performHaptic();
                    sendMouse((byte) 0x01, (byte) 0, (byte) 0, (byte) 0);
                }

                @Override
                public void onMouseDrag(byte dx, byte dy) {
                    if (isGyroActive && disableTrackpadOnGyro) return;
                    sendMouse((byte) 0x01, (byte) (dx * mouseSensitivity), (byte) (dy * mouseSensitivity), (byte) 0);
                }

                @Override
                public void onMouseDragEnd() {
                    performHaptic();
                    byte buttons = 0;
                    if (isLeftClickHeld) buttons |= 0x01;
                    if (isRightClickHeld) buttons |= 0x02;
                    sendMouse(buttons, (byte) 0, (byte) 0, (byte) 0);
                }

                @Override
                public void onMouseScroll(byte wheel) {
                    sendMouse((byte) 0, (byte) 0, (byte) 0, wheel);
                }

                @Override
                public void onLeftClick() {
                    performHaptic();
                    sendMouse((byte) 0x01, (byte) 0, (byte) 0, (byte) 0);
                    mainHandler.postDelayed(() -> {
                        byte buttons = 0;
                        if (isLeftClickHeld) buttons |= 0x01;
                        if (isRightClickHeld) buttons |= 0x02;
                        sendMouse(buttons, (byte) 0, (byte) 0, (byte) 0);
                    }, 50);
                }

                @Override
                public void onRightClick() {
                    performHaptic();
                    sendMouse((byte) 0x02, (byte) 0, (byte) 0, (byte) 0);
                    mainHandler.postDelayed(() -> {
                        byte buttons = 0;
                        if (isLeftClickHeld) buttons |= 0x01;
                        if (isRightClickHeld) buttons |= 0x02;
                        sendMouse(buttons, (byte) 0, (byte) 0, (byte) 0);
                    }, 50);
                }

                @Override
                public void onThreeFingerSwipe() {
                    performHaptic();
                    sendShortcut((byte) 0x08, (byte) 0x2B); // Windows + Tab
                }

                @Override
                public void onThreeFingerSwipe(int direction) {
                    performHaptic();
                    if (direction == AdvancedTrackpadView.SWIPE_DIRECTION_DOWN) {
                        sendShortcut((byte) 0x08, (byte) 0x07); // Win + D (Show Desktop)
                    } else if (direction == AdvancedTrackpadView.SWIPE_DIRECTION_LEFT || direction == AdvancedTrackpadView.SWIPE_DIRECTION_RIGHT) {
                        sendShortcut((byte) 0x04, (byte) 0x2B); // Alt + Tab (Switch App)
                    } else {
                        sendShortcut((byte) 0x08, (byte) 0x2B); // Windows + Tab (Task View)
                    }
                }
            });
        }

        setupKeyboardControls();
        setupMediaRemoteControls();
        setupPresenterControls();

        if (btnNavMouse != null) btnNavMouse.setOnClickListener(v -> { performHaptic(); switchMode("Mouse"); });
        if (btnNavKeyboard != null) btnNavKeyboard.setOnClickListener(v -> { performHaptic(); switchMode("Keyboard"); });
        if (btnNavMedia != null) btnNavMedia.setOnClickListener(v -> { performHaptic(); switchMode("Media"); });
        if (btnNavPresenter != null) btnNavPresenter.setOnClickListener(v -> { performHaptic(); switchMode("Presenter"); });

        if (btnNavMore != null) {
            btnNavMore.setOnClickListener(v -> {
                performHaptic();
                MoreBottomSheetDialog bottomSheet = new MoreBottomSheetDialog();
                bottomSheet.setOnMenuSelectedListener(option -> {
                    performHaptic();
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
                        case "Bluetooth":
                        case "Bluetooth Devices":
                        case "Bluetooth Manager":
                            openDeviceListDialog();
                            break;
                        default:
                            Toast.makeText(this, option + " selected", Toast.LENGTH_SHORT).show();
                            break;
                    }
                });
                bottomSheet.show(getSupportFragmentManager(), "MoreBottomSheet");
            });
        }
    }

    private void setupLiveTypingControls() {
        etSendText = findViewById(R.id.etLiveInput);
        btnSendText = findViewById(R.id.btnLiveSend);
        btnLiveMic = findViewById(R.id.btnLiveMic);

        if (btnSendText != null) {
            btnSendText.setOnClickListener(v -> {
                performHaptic();
                handleSendInputText();
            });
        }

        if (btnLiveMic != null) {
            btnLiveMic.setOnClickListener(v -> {
                performHaptic();
                try {
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now to type on laptop...");
                    speechRecognizerLauncher.launch(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Voice recognition not available on this device", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (etSendText != null) {
            etSendText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    performHaptic();
                    handleSendInputText();
                    return true;
                }
                return false;
            });

            etSendText.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DEL && etSendText.getText().length() == 0) {
                        performHaptic();
                        sendShortcut((byte) 0x00, (byte) 0x2A); // Backspace
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        performHaptic();
                        handleSendInputText();
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private void handleSendInputText() {
        if (etSendText == null) return;
        String text = etSendText.getText().toString();
        if (!text.isEmpty()) {
            sendStringToHid(text);
            etSendText.setText("");
        } else {
            sendShortcut((byte) 0x00, (byte) 0x28); // Enter
        }
    }

    public void sendStringToHid(String text) {
        if (text == null || text.isEmpty()) return;
        if (hidManager == null || hidManager.getConnectedDevice() == null) {
            Toast.makeText(this, "No connected PC / Laptop. Pair via Bluetooth first.", Toast.LENGTH_SHORT).show();
            return;
        }

        long delayMs = 0;
        final long stepMs = 55; // 55ms gives time for key down + key release reports

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            HidKey key = charToHidKey(ch);
            if (key != null) {
                mainHandler.postDelayed(() -> sendShortcut(key.modifier, key.keyCode), delayMs);
                delayMs += stepMs;
            }
        }
    }

    private static class HidKey {
        final byte modifier;
        final byte keyCode;

        HidKey(byte modifier, byte keyCode) {
            this.modifier = modifier;
            this.keyCode = keyCode;
        }
    }

    private static HidKey charToHidKey(char c) {
        if (c >= 'a' && c <= 'z') {
            return new HidKey((byte) 0x00, (byte) (0x04 + (c - 'a')));
        }
        if (c >= 'A' && c <= 'Z') {
            return new HidKey((byte) 0x02, (byte) (0x04 + (c - 'A'))); // Left Shift
        }
        if (c >= '1' && c <= '9') {
            return new HidKey((byte) 0x00, (byte) (0x1E + (c - '1')));
        }
        if (c == '0') {
            return new HidKey((byte) 0x00, (byte) 0x27);
        }
        switch (c) {
            case ' ':  return new HidKey((byte) 0x00, (byte) 0x2C); // Space
            case '\n':
            case '\r': return new HidKey((byte) 0x00, (byte) 0x28); // Enter
            case '\t': return new HidKey((byte) 0x00, (byte) 0x2B); // Tab
            case '\b': return new HidKey((byte) 0x00, (byte) 0x2A); // Backspace
            case '!':  return new HidKey((byte) 0x02, (byte) 0x1E);
            case '@':  return new HidKey((byte) 0x02, (byte) 0x1F);
            case '#':  return new HidKey((byte) 0x02, (byte) 0x20);
            case '$':  return new HidKey((byte) 0x02, (byte) 0x21);
            case '%':  return new HidKey((byte) 0x02, (byte) 0x22);
            case '^':  return new HidKey((byte) 0x02, (byte) 0x23);
            case '&':  return new HidKey((byte) 0x02, (byte) 0x24);
            case '*':  return new HidKey((byte) 0x02, (byte) 0x25);
            case '(':  return new HidKey((byte) 0x02, (byte) 0x26);
            case ')':  return new HidKey((byte) 0x02, (byte) 0x27);
            case '-':  return new HidKey((byte) 0x00, (byte) 0x2D);
            case '_':  return new HidKey((byte) 0x02, (byte) 0x2D);
            case '=':  return new HidKey((byte) 0x00, (byte) 0x2E);
            case '+':  return new HidKey((byte) 0x02, (byte) 0x2E);
            case '[':  return new HidKey((byte) 0x00, (byte) 0x2F);
            case '{':  return new HidKey((byte) 0x02, (byte) 0x2F);
            case ']':  return new HidKey((byte) 0x00, (byte) 0x30);
            case '}':  return new HidKey((byte) 0x02, (byte) 0x30);
            case '\\': return new HidKey((byte) 0x00, (byte) 0x31);
            case '|':  return new HidKey((byte) 0x02, (byte) 0x31);
            case ';':  return new HidKey((byte) 0x00, (byte) 0x33);
            case ':':  return new HidKey((byte) 0x02, (byte) 0x33);
            case '\'': return new HidKey((byte) 0x00, (byte) 0x34);
            case '"':  return new HidKey((byte) 0x02, (byte) 0x34);
            case '`':  return new HidKey((byte) 0x00, (byte) 0x35);
            case '~':  return new HidKey((byte) 0x02, (byte) 0x35);
            case ',':  return new HidKey((byte) 0x00, (byte) 0x36);
            case '<':  return new HidKey((byte) 0x02, (byte) 0x36);
            case '.':  return new HidKey((byte) 0x00, (byte) 0x37);
            case '>':  return new HidKey((byte) 0x02, (byte) 0x37);
            case '/':  return new HidKey((byte) 0x00, (byte) 0x38);
            case '?':  return new HidKey((byte) 0x02, (byte) 0x38);
            default:   return null;
        }
    }

    private void toggleAirMouse() {
        isGyroActive = !isGyroActive;
        if (btnAirMouse == null) return;

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
            btnAirMouse.setTextColor(Color.WHITE);
            btnAirMouse.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1E293B")));
            Toast.makeText(this, "Air Mouse OFF", Toast.LENGTH_SHORT).show();
        }
    }

    private void switchMode(String mode) {
        if (layoutMouseView != null) layoutMouseView.setVisibility(View.GONE);
        if (layoutKeyboardView != null) layoutKeyboardView.setVisibility(View.GONE);
        if (layoutMediaView != null) layoutMediaView.setVisibility(View.GONE);
        if (layoutPresenterView != null) layoutPresenterView.setVisibility(View.GONE);

        resetNavTabStyle(btnNavMouse);
        resetNavTabStyle(btnNavKeyboard);
        resetNavTabStyle(btnNavMedia);
        resetNavTabStyle(btnNavPresenter);
        resetNavTabStyle(btnNavMore);

        switch (mode) {
            case "Mouse":
                if (layoutMouseView != null) layoutMouseView.setVisibility(View.VISIBLE);
                highlightNavTab(btnNavMouse);
                if (tvHeaderTitle != null) tvHeaderTitle.setText("Mouse / Trackpad");
                break;
            case "Keyboard":
                if (layoutKeyboardView != null) layoutKeyboardView.setVisibility(View.VISIBLE);
                highlightNavTab(btnNavKeyboard);
                if (tvHeaderTitle != null) tvHeaderTitle.setText("Full PC Keyboard");
                break;
            case "Media":
                if (layoutMediaView != null) layoutMediaView.setVisibility(View.VISIBLE);
                highlightNavTab(btnNavMedia);
                if (tvHeaderTitle != null) tvHeaderTitle.setText("Multimedia Remote");
                break;
            case "Presenter":
                if (layoutPresenterView != null) layoutPresenterView.setVisibility(View.VISIBLE);
                highlightNavTab(btnNavPresenter);
                if (tvHeaderTitle != null) tvHeaderTitle.setText("Presenter Remote");
                break;
        }
    }

    private void highlightNavTab(TextView tv) {
        if (tv == null) return;
        tv.setTextColor(Color.WHITE);
        tv.setBackgroundResource(R.drawable.bg_nav_pill);
    }

    private void resetNavTabStyle(TextView tv) {
        if (tv == null) return;
        tv.setTextColor(Color.parseColor("#94A3B8"));
        tv.setBackgroundColor(Color.TRANSPARENT);
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
            bindClick(keyIds[i], v -> sendKeyWithModifiers(code));
        }

        bindClick(R.id.btnKeySpace, v -> sendKeyWithModifiers((byte) 0x2C));
        bindClick(R.id.btnKeyEnter, v -> sendKeyWithModifiers((byte) 0x28));
        bindClick(R.id.btnKeyBackspace, v -> sendKeyWithModifiers((byte) 0x2A));
        bindClick(R.id.btnKeyEsc, v -> sendKeyWithModifiers((byte) 0x29));
        bindClick(R.id.btnKeyDel, v -> sendKeyWithModifiers((byte) 0x4C));

        bindClick(R.id.btnKeyF1, v -> sendKeyWithModifiers((byte) 0x3A));
        bindClick(R.id.btnKeyF2, v -> sendKeyWithModifiers((byte) 0x3B));
        bindClick(R.id.btnKeyF3, v -> sendKeyWithModifiers((byte) 0x3C));
        bindClick(R.id.btnKeyF4, v -> sendKeyWithModifiers((byte) 0x3D));
        bindClick(R.id.btnKeyF5, v -> sendKeyWithModifiers((byte) 0x3E));
        bindClick(R.id.btnKeyF6, v -> sendKeyWithModifiers((byte) 0x3F));
        bindClick(R.id.btnKeyF7, v -> sendKeyWithModifiers((byte) 0x40));
        bindClick(R.id.btnKeyF8, v -> sendKeyWithModifiers((byte) 0x41));
        bindClick(R.id.btnKeyF9, v -> sendKeyWithModifiers((byte) 0x42));
        bindClick(R.id.btnKeyF10, v -> sendKeyWithModifiers((byte) 0x43));
        bindClick(R.id.btnKeyF11, v -> sendKeyWithModifiers((byte) 0x44));
        bindClick(R.id.btnKeyF12, v -> sendKeyWithModifiers((byte) 0x45));

        if (btnKeyCtrl != null) {
            btnKeyCtrl.setOnClickListener(v -> {
                performHaptic();
                isCtrlActive = !isCtrlActive;
                btnKeyCtrl.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(isCtrlActive ? "#2563EB" : "#334155")));
            });
        }
        if (btnKeyAlt != null) {
            btnKeyAlt.setOnClickListener(v -> {
                performHaptic();
                isAltActive = !isAltActive;
                btnKeyAlt.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(isAltActive ? "#2563EB" : "#334155")));
            });
        }
    }

    private void sendKeyWithModifiers(byte keyCode) {
        byte modifier = 0;
        if (isCtrlActive) modifier |= 0x01;
        if (isAltActive) modifier |= 0x04;

        sendShortcut(modifier, keyCode);

        if (isCtrlActive || isAltActive) {
            isCtrlActive = false;
            isAltActive = false;
            if (btnKeyCtrl != null) btnKeyCtrl.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#334155")));
            if (btnKeyAlt != null) btnKeyAlt.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#334155")));
        }
    }

    private void setupMediaRemoteControls() {
        bindClick(R.id.btnDpadUp, v -> sendShortcut((byte) 0, (byte) 0x52));
        bindClick(R.id.btnDpadDown, v -> sendShortcut((byte) 0, (byte) 0x51));
        bindClick(R.id.btnDpadLeft, v -> sendShortcut((byte) 0, (byte) 0x50));
        bindClick(R.id.btnDpadRight, v -> sendShortcut((byte) 0, (byte) 0x4F));
        bindClick(R.id.btnDpadOk, v -> sendShortcut((byte) 0, (byte) 0x28));

        bindClick(R.id.btnMediaPrev, v -> sendConsumer(0x00B6));
        bindClick(R.id.btnMediaNext, v -> sendConsumer(0x00B5));
        bindClick(R.id.btnMediaMute, v -> sendConsumer(0x00E2));
        bindClick(R.id.btnMediaPlayPauseLarge, v -> sendConsumer(0x00CD));
    }

    private void setupPresenterControls() {
        bindClick(R.id.btnPresenterPrevSlide, v -> sendShortcut((byte) 0, (byte) 0x4B)); // Page Up
        bindClick(R.id.btnPresenterNextSlide, v -> sendShortcut((byte) 0, (byte) 0x4E)); // Page Down
        bindClick(R.id.btnPresenterStart, v -> sendShortcut((byte) 0, (byte) 0x3E));     // F5
        bindClick(R.id.btnPresenterBlank, v -> sendShortcut((byte) 0, (byte) 0x05));     // B key
    }

    private void showSettingsDialog() {
        performHaptic();
        boolean isDark = ThemeManager.isDarkMode(this);
        SharedPreferences prefs = getSharedPreferences(ThemeManager.PREF_NAME, Context.MODE_PRIVATE);

        String[] options = {
                "Scroll Direction: " + (invertScroll ? "Natural (Down scrolls Down)" : "Traditional (Down scrolls Up)"),
                "Gyroscope X (Horizontal): " + (invertGyroX ? "Inverted" : "Normal"),
                "Gyroscope Y (Vertical): " + (invertGyroY ? "Inverted" : "Normal"),
                "Sensitivity: Slow (0.5x)",
                "Sensitivity: Normal (1.0x)",
                "Sensitivity: Fast (1.5x)",
                "Sensitivity: Turbo (2.0x)",
                "Click Buttons Position: " + (clickButtonsOnTop ? "Top" : "Bottom"),
                "Disable Trackpad on Gyro: " + (disableTrackpadOnGyro ? "Enabled" : "Disabled"),
                "Theme Mode: " + (isDark ? "Dark (Switch to Light)" : "Light (Switch to Dark)")
        };

        new AlertDialog.Builder(this)
                .setTitle("BlueControl Settings")
                .setItems(options, (dialog, which) -> {
                    performHaptic();
                    switch (which) {
                        case 0:
                            invertScroll = !invertScroll;
                            if (layoutTrackpad instanceof AdvancedTrackpadView) {
                                ((AdvancedTrackpadView) layoutTrackpad).setInvertScroll(invertScroll);
                            }
                            prefs.edit().putBoolean(PREF_KEY_INVERT_SCROLL, invertScroll).apply();
                            Toast.makeText(this, "Scroll Direction: " + (invertScroll ? "Natural" : "Traditional"), Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            invertGyroX = !invertGyroX;
                            prefs.edit().putBoolean(PREF_KEY_INVERT_GYRO_X, invertGyroX).apply();
                            Toast.makeText(this, "Gyro X Direction: " + (invertGyroX ? "Inverted" : "Normal"), Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            invertGyroY = !invertGyroY;
                            prefs.edit().putBoolean(PREF_KEY_INVERT_GYRO_Y, invertGyroY).apply();
                            Toast.makeText(this, "Gyro Y Direction: " + (invertGyroY ? "Inverted" : "Normal"), Toast.LENGTH_SHORT).show();
                            break;
                        case 3:
                            mouseSensitivity = 0.5f;
                            prefs.edit().putFloat(PREF_KEY_SENSITIVITY, mouseSensitivity).apply();
                            if (layoutTrackpad instanceof AdvancedTrackpadView) {
                                ((AdvancedTrackpadView) layoutTrackpad).setSensitivity(mouseSensitivity);
                            }
                            Toast.makeText(this, "Sensitivity updated to 0.5x", Toast.LENGTH_SHORT).show();
                            break;
                        case 4:
                            mouseSensitivity = 1.0f;
                            prefs.edit().putFloat(PREF_KEY_SENSITIVITY, mouseSensitivity).apply();
                            if (layoutTrackpad instanceof AdvancedTrackpadView) {
                                ((AdvancedTrackpadView) layoutTrackpad).setSensitivity(mouseSensitivity);
                            }
                            Toast.makeText(this, "Sensitivity updated to 1.0x", Toast.LENGTH_SHORT).show();
                            break;
                        case 5:
                            mouseSensitivity = 1.5f;
                            prefs.edit().putFloat(PREF_KEY_SENSITIVITY, mouseSensitivity).apply();
                            if (layoutTrackpad instanceof AdvancedTrackpadView) {
                                ((AdvancedTrackpadView) layoutTrackpad).setSensitivity(mouseSensitivity);
                            }
                            Toast.makeText(this, "Sensitivity updated to 1.5x", Toast.LENGTH_SHORT).show();
                            break;
                        case 6:
                            mouseSensitivity = 2.0f;
                            prefs.edit().putFloat(PREF_KEY_SENSITIVITY, mouseSensitivity).apply();
                            if (layoutTrackpad instanceof AdvancedTrackpadView) {
                                ((AdvancedTrackpadView) layoutTrackpad).setSensitivity(mouseSensitivity);
                            }
                            Toast.makeText(this, "Sensitivity updated to 2.0x", Toast.LENGTH_SHORT).show();
                            break;
                        case 7:
                            clickButtonsOnTop = !clickButtonsOnTop;
                            if (layoutClickTop != null) layoutClickTop.setVisibility(clickButtonsOnTop ? View.VISIBLE : View.GONE);
                            if (layoutClickBottom != null) layoutClickBottom.setVisibility(clickButtonsOnTop ? View.GONE : View.VISIBLE);
                            Toast.makeText(this, "Click buttons moved to " + (clickButtonsOnTop ? "Top" : "Bottom"), Toast.LENGTH_SHORT).show();
                            break;
                        case 8:
                            disableTrackpadOnGyro = !disableTrackpadOnGyro;
                            Toast.makeText(this, "Disable trackpad on Gyro: " + disableTrackpadOnGyro, Toast.LENGTH_SHORT).show();
                            break;
                        case 9:
                            ThemeManager.setDarkMode(this, !isDark);
                            recreate();
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
            int rotation = Surface.ROTATION_0;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    rotation = getDisplay() != null ? getDisplay().getRotation() : Surface.ROTATION_0;
                } else {
                    rotation = getWindowManager().getDefaultDisplay().getRotation();
                }
            } catch (Exception ignored) {}

            float rawX = event.values[1]; // roll/yaw in portrait
            float rawY = event.values[0]; // pitch in portrait

            float hAxis = rawX;
            float vAxis = rawY;

            // Handle device display rotation dynamically
            switch (rotation) {
                case Surface.ROTATION_90:
                    hAxis = -rawY;
                    vAxis = rawX;
                    break;
                case Surface.ROTATION_180:
                    hAxis = -rawX;
                    vAxis = -rawY;
                    break;
                case Surface.ROTATION_270:
                    hAxis = rawY;
                    vAxis = -rawX;
                    break;
            }

            // Normal vs Inverted: flipped from old opposite signs
            float signX = invertGyroX ? -1.0f : 1.0f;
            float signY = invertGyroY ? 1.0f : -1.0f;

            byte dx = (byte) (signX * hAxis * 10.0f * mouseSensitivity);
            byte dy = (byte) (signY * vAxis * 10.0f * mouseSensitivity);

            if (dx != 0 || dy != 0) {
                sendMouse((byte) 0, dx, dy, (byte) 0);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onResume() {
        super.onResume();
        if (hidManager != null) {
            hidManager.initProfileProxy(this);
            BluetoothDevice current = hidManager.getConnectedDevice();
            if (current != null) {
                int state = hidManager.getConnectionState(current);
                updateConnectionStatusUI(current, state != 0 ? state : BluetoothProfile.STATE_CONNECTED);
            } else {
                updateConnectionStatusUI(null, BluetoothProfile.STATE_DISCONNECTED);
                mainHandler.postDelayed(() -> {
                    if (hidManager != null && !hidManager.isConnected()) {
                        hidManager.autoReconnect();
                    }
                }, 500);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isGyroActive) {
            toggleAirMouse();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(bluetoothStateReceiver);
        } catch (Exception ignored) {}
    }
}
