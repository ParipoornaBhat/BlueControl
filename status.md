# BlueControl - Project Status Summary

> [!NOTE]
> This status document summarizes the development state, architecture, and features of **BlueControl** (non-sensitive overview).

## Project Overview
- **App Name**: BlueControl
- **Repository**: [https://github.com/ParipoornaBhat/BlueControl.git](https://github.com/ParipoornaBhat/BlueControl.git)
- **Minimum Android SDK**: API 28 (Android 9.0 Pie) for native Bluetooth HID Device profile support.
- **Target SDK**: API 36

---

## Implemented Features & Architecture

### 1. Bluetooth HID Core (`BluetoothHidManager.java`)
- Registers as a Bluetooth HID peripheral device (`BluetoothHidDevice`).
- Manages HID report descriptors for:
  - **Keyboard Report (ID 1)**: Key scan codes and modifier keys (`Ctrl`, `Alt`, `Shift`, etc.).
  - **Mouse Report (ID 2)**: Relative pointer movement (`dx`, `dy`), scroll wheel, and click buttons.
  - **Consumer Control Report (ID 3)**: Multimedia transport keys (Volume up/down, mute, play/pause).

### 2. Active Target Device Selector (`DeviceListDialog.java`)
- Queries bonded and paired Bluetooth devices.
- Provides an interactive dialog allowing the user to select the **active target device** when multiple devices are connected or paired.

### 3. Remote Dashboard & Trackpad (`MainActivity.java` & `activity_main.xml`)
- **Touch Trackpad**: Smooth touch-to-cursor movement and vertical scrolling.
- **Quick Audio Controls**: Dedicated buttons for `VOL-`, `VOL+`, `MUTE`, and `PLAY/PAUSE`.
- **Mouse Click Buttons**: Dedicated left and right click buttons.
- **Macro Shortcuts**: Quick action buttons for `UNDO`, `REDO`, `COPY`, and `PASTE`.

### 4. Hold-to-Use Air Mouse (Gyroscope)
- Push-to-use air mouse mode: pressing and holding the **AIR MOUSE** button engages the device gyroscope (`Sensor.TYPE_GYROSCOPE`) to control the cursor in mid-air. Releasing the button switches back to touch trackpad mode.

### 5. Mouse Sensitivity Settings
- Accessible via the settings gear icon (`⚙`) in the header.
- Allows configuring cursor speed between **Slow (0.5x)**, **Normal (1.0x)**, **Fast (1.5x)**, and **Turbo (2.0x)**.

---

## Version Control & Security
- Successfully committed and pushed to GitHub (`https://github.com/ParipoornaBhat/BlueControl.git`).
- Credentials, tokens (`.env`), and author identities have been cleaned up from global configuration to ensure no sensitive data remains on shared devices.
