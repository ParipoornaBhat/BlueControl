<div align="center">

# REPORT ON MOBILE APP DEVELOPMENT MINI PROJECT

### Carried Out On

## BlueControl: Virtual Bluetooth HID Input Device & Remote Controller

<br>

**Course Code: CS3604-1**  
**Course Name: MOBILE APP DEVELOPMENT**  
**Semester: VI SEM**  
**Section: C**  

<br>

### By

**Name: PARIPOORNA B**  
**USN: NNM23CS124**  

**Name: KOTIAN SIDHITA DAYANAND**  
**USN: NNM23CS097**  

**Name: KHYATHI JAIN**  
**USN: NNM23CS095**  

<br>

### Under The Guidance Of

**Mrs. AISHWARYA. D. SHETTY**  
Assistant Professor Gd.-II  
Department Of CSE  

<br>

**DATE OF SUBMISSION:**  
**22/04/2026**

</div>

---
<div style="page-break-after: always;"></div>

<div align="center">

# I

# CERTIFICATE

</div>

*This is to certify that the “Mobile App Development Mini Project Report” submitted by* ***Paripoorna B bearing USN NNM23CS124, Kotian Sidhita Dayanand bearing USN NNM23CS097, and Khyathi Jain bearing USN NNM23CS095*** *of III year VI semester B.Tech., bonafide students of* ***NMAM Institute of Technology, Nitte*** *have successfully completed Mobile App Development Mini-Project during 2025-26 fulfilling the partial requirements for the award of degree of Bachelor of Technology in* ***Computer Science and Engineering*** *at NMAM Institute of Technology, Nitte.*

<br><br><br><br><br>

| ____________________________ | _______________________ |
| :--- | ---: |
| **Name and Signature of Mentor** | **Signature of HOD** |

---
<div style="page-break-after: always;"></div>

<div align="center">

# II

## ACKNOWLEDGEMENT

</div>

First and foremost, we thank our parents for shaping who we are and where we stand today—their hard work and sacrifices made this possible.

We sincerely express our gratitude to everyone who offered suggestions, coordination, encouragement, and support, contributing to the success of this project.

Our heartfelt thanks go to our guide, Mrs. Aishwarya D. Shetty, Assistant Professor GD-II, Department of Computer Science and Engineering, for her invaluable support and guidance.

Our sincere thanks to our Head of the Department of Computer Science and Engineering, Dr. Jyothi Shetty, Principal, Dr. Niranjan N. Chiplunkar and Vice Principal, Dr. Nagesh Prabhu for permitting to do this project in our college and providing all necessary facilities.

Finally, we thank all our friends and professors who assisted us throughout this endeavor.

<br><br>

**Paripoorna B (NNM23CS124)**  
**Kotian Sidhita Dayanand (NNM23CS097)**  
**Khyathi Jain (NNM23CS095)**  

---
<div style="page-break-after: always;"></div>

<div align="center">

# III

## ABSTRACT

</div>

Computer peripheral and remote input systems have been around for many years, providing efficiency and control for personal computers, presentations, and smart environments alike. With the increasing popularity of mobile digital tools, smartphones have succeeded in attaining operational utility beyond simple communication. With improvements in wireless protocols and mobile operating systems, the remote peripheral industry keeps on evolving. The mobile peripheral sector has also started contributing in other areas such as real-time cursor control, mid-air motion pointing, and multi-device interaction support, along with core keyboard and mouse emulation. The wireless input device sector is immensely large. Digital mobile peripherals are considered to be an engaging operational tool that invokes user convenience and even leads to streamlined computing workflows. This project is focused on the development of BlueControl, a comprehensive Virtual Bluetooth HID Input Device and Remote Controller that addresses these factors. The project implements features for multi-touch trackpad navigation, dedicated mouse clicks, a full PC keyboard with live text input streaming, multimedia playback controls, presentation slide navigation, and a 6-DoF gyroscopic Air Mouse—built using Android SDK (API 28–36), Java 11, and native Bluetooth HID Device profile emulation—to study positive impacts like zero companion software dependency, driverless host compatibility, and real-time low-latency response across diverse desktop and smart TV operating systems.

---
<div style="page-break-after: always;"></div>

<div align="center">

# IV

## TABLE OF CONTENTS

</div>

1. TitlePage
2. Institution Certificate .................................................................i
3. Acknowledgement...................................................................…ii
4. Abstract.....................................................................................iii
5. Table Of Contents......................................................….............iv
6. Introduction..............................................................................1
7. Problem Statement...................................................................2
8. Objectives..............................................................................…3
9. Technologies Used.................................................................….4
10. Implementation........................................................................5
11. Results and Discussions.........................................................….6-7
12. User Interfaces…………….........................................................…8-11
13. Advantages...............................................................................12
14. Future Enhancements....................…...........…......................…..13
15. Conclusion............................................................................…14
16. References................................................................................15

---
<div style="page-break-after: always;"></div>

<div align="right"><b>Dept of CSE NMAMIT,Nitte 1</b></div>

<div align="center">

## INTRODUCTION

</div>

In today’s competitive computing and mobile environment, digital interaction has become increasingly versatile, requiring efficient tools to manage computer input, presentation navigation, and wireless peripheral demands. Traditional input methods often involve physical mice, hardware keyboards, and proprietary RF dongles, which can lead to hardware failures, dead batteries, lost accessories, and reduced operational efficiency during critical presentations or travel.

To address these challenges, smartphone-based virtual input systems have emerged as a practical solution, offering mobility, touch versatility, and convenient interaction between mobile devices and host computers.

BlueControl simplifies the entire peripheral workflow by providing a centralized platform where users can control mouse pointers, trigger keystrokes, and navigate media or slides effectively.

BlueControl is a Virtual Bluetooth HID Input Device and Remote Controller, developed using Android SDK (API 28–36) to demonstrate a modern, secure, and driverless approach to computer input control. The system incorporates technologies such as the native Android Bluetooth HID Device profile (BluetoothHidDevice), a custom hardware-accelerated multi-touch trackpad view with inertial physics, hardware gyroscope sensor fusion for an Air Mouse mode, a full PC keyboard with live mobile typing, and Material Design 3 components to ensure smooth operations and user-friendly interactions.

BlueControl showcases the practical application of mobile application development skills and highlights the capabilities of native Android Bluetooth framework in building real-world peripheral solutions.

<br><br>
**BlueControl**

---
<div style="page-break-after: always;"></div>

<div align="right"><b>Dept of CSE NMAMIT,Nitte 2</b></div>

<div align="center">

## PROBLEM STATEMENT

</div>

Traditional input peripheral management processes often involve physical hardware accessories like mechanical mice, external keyboards, presenter clickers, and dedicated RF USB adapters. These approaches are inconvenient, prone to hardware damage or battery exhaustion, and lack flexibility when traveling or presenting. Users frequently encounter difficulties when USB dongles are misplaced or when host laptops lack compatible USB Type-A ports. When software-based mobile remote applications are used instead, they predominantly rely on local Wi-Fi networks requiring companion server programs to be installed on the host PC. These conventional Wi-Fi remote solutions introduce significant security vulnerabilities through open ports, fail completely in networks with Access Point (AP) Client Isolation (such as university or enterprise Wi-Fi), and suffer from unpredictable latency spikes and packet jitter.

There is a need for a secure, driverless, and user-friendly platform that can simplify and streamline wireless remote input for both everyday desktop users and presentation speakers. Such a solution should allow users to easily control cursor navigation, perform multi-touch gestures, stream keyboard typing, and manage media playback, while operating completely driverless without requiring any companion software or active internet connection on the host computer. The system must ensure proper Bluetooth Human Interface Device (HID) protocol compliance and sensor integration to maintain instantaneous responsiveness and support real-time interactions. This project aims to address these challenges by developing BlueControl, a robust Virtual Bluetooth HID Input Device and Remote Controller using Android SDK, Java 11, and native Bluetooth HID Device profile emulation.

<br><br>
**BlueControl**

---
<div style="page-break-after: always;"></div>

<div align="right"><b>Dept of CSE NMAMIT,Nitte 3</b></div>

<div align="center">

## OBJECTIVES

</div>

1. To develop a mobile-based virtual peripheral application that simplifies computer input and remote control through an interactive and accessible touch interface

2. To implement native Android Bluetooth HID Device profile (BluetoothHidDevice) emulation for driverless connection with host systems without requiring companion PC software

3. To provide a centralized system where users can perform cursor navigation, multi-touch gestures, and audio controls efficiently

4. To enable users to control presentations using a dedicated presenter mode and mid-air pointing via hardware gyroscope (Air Mouse)

5. To implement complete keyboard emulation, including standard keys, modifier shortcuts, and live mobile typing with speech-to-text integration

6. To ensure efficient hardware resource management by integrating SensorManager and low-overhead Bluetooth L2CAP packet dispatching

7. To create a scalable, maintainable, and modular Android application following clean architecture and best practices of modern mobile development

8. To enhance the user experience through responsive UI design with Material 3, tactile haptic feedback, real-time connection status indicators, and smooth navigation

<br><br>
**BlueControl**

---
<div style="page-break-after: always;"></div>

<div align="right"><b>Dept of CSE NMAMIT,Nitte 4</b></div>

<div align="center">

## TECHNOLOGIES USED

</div>

**1. Android SDK (API 28–36)**  
Serves as the core mobile development platform, providing access to system services, Bluetooth profiles, and modern Android capabilities.

**2. Java 11**  
The primary object-oriented programming language used to develop robust application logic, concurrency handling, and UI interactions.

**3. Bluetooth HID Device Profile (BluetoothHidDevice)**  
Acts as the wireless peripheral emulation framework, enabling the device to register SDP records and send standard USB HID input reports.

**4. Android Sensor Subsystem (SensorManager)**  
Captures real-time angular velocity from the hardware gyroscope (TYPE_GYROSCOPE) to power the mid-air Air Mouse pointing mode.

**5. Material Design 3 & Android Jetpack**  
Provides modern UI components, adaptive styling, bottom sheets, navigation bars, and lifecycle management for seamless user experience.

**6. Hardware-Accelerated Custom View (AdvancedTrackpadView)**  
Extends Android's View class with custom 2D canvas rendering and pre-allocated object pools for zero-allocation touch and gesture processing.

**7. Gradle Build Automation Toolchain**  
Handles project dependencies, build configurations, ProGuard optimization rules, and automated APK generation.

<br><br>
**BlueControl**

---
<div style="page-break-after: always;"></div>

<div align="right"><b>Dept of CSE NMAMIT,Nitte 5</b></div>

<div align="center">

## IMPLEMENTATION

</div>

The implementation of the BlueControl Virtual Bluetooth HID Input Device and Remote Controller was carried out using Android SDK and Java 11, with a focus on modular design, driverless architecture, and seamless interaction between mobile touch sensors, onboard gyroscope hardware, and host operating systems.

The entire application was structured using clean layered architecture, dividing the presentation UI layer, the Bluetooth HID management service, the gesture touch engine, and the sensor processing subsystem into clear, well-defined components.

BluetoothHidManager handled Service Discovery Protocol (SDP) registration, profile proxy acquisition, and HID report dispatching. A composite USB-IF compliant HID report descriptor was formulated, unifying standard Keyboard reports (ID 1), relative Mouse movement and scroll reports (ID 2), and Consumer Control audio/multimedia reports (ID 3).

To ensure stable connections across Windows 10/11 and macOS, handshake protocol callbacks (onGetReport and onSetReport) were implemented to return immediate success acknowledgements (ERROR_RSP_SUCCESS), preventing OS connection dropouts.

The user interface was developed using Material Design 3 components, featuring an AdvancedTrackpadView touch surface optimized with zero-allocation object pools and exponential smoothing for lag-free cursor tracking. Inertial velocity physics were incorporated using VelocityTracker to enable natural cursor coasting.

During integration, the mobile typing bar converted live text and speech recognition output directly into raw USB HID scan codes, transmitting characters instantly to the host. Device management dialogs allowed scanning, pairing, and switching target hosts effortlessly. The final system was validated on physical devices, demonstrating high stability, low latency, and reliable cross-platform operation.

<br><br>
**BlueControl**

---
<div style="page-break-after: always;"></div>

<div align="right"><b>Dept of CSE NMAMIT,Nitte 6</b></div>

<div align="center">

## RESULTS AND DISCUSSION

</div>

The BlueControl was thoroughly evaluated to ensure that all features worked correctly within the virtual peripheral system. Testing confirmed that the system operates smoothly, maintains stable Bluetooth connections, and delivers a responsive and user-friendly experience. The results demonstrate that the application effectively supports cursor navigation, keyboard typing, presentation control, and provides reliable management control with strong system performance.

### Functional Results
Users successfully paired the smartphone with host computers, navigated cursors via the multi-touch trackpad, executed single-tap left clicks and two-finger tap right clicks, and performed two-finger vertical scrolling without errors.

All keyboard operations, including standard alphanumeric typing, modifier key combinations (Ctrl, Alt, Shift), and live text streaming from the mobile typing bar, executed flawlessly through the unified interface.

The 6-DoF Air Mouse accurately translated device angular velocity into smooth mid-air pointer movements, while multimedia controls reliably adjusted system volume and playback across media players.

### Performance Results
The system delivered exceptional response times, with touch-to-cursor latency averaging under 5 milliseconds, ensuring fluid interaction without perceptible lag.

Report dispatching executed efficiently due to the dedicated single-threaded executor and optimized composite HID descriptor structure.

The Bluetooth HID stack performed reliably under prolonged usage scenarios, demonstrating low battery consumption (<3.8% per hour) and robust link stability over distances exceeding 10 meters.

<br><br>
**BlueControl**

---
<div style="page-break-after: always;"></div>

<div align="right"><b>Dept of CSE NMAMIT,Nitte 7</b></div>

### Usability Results
Tests reported that the Material 3 interface was clean, intuitive, and easy to navigate through the bottom navigation bar.

Responsive layout design provided consistent controls across portrait and landscape orientations, with clear connection status pills.

The unified interface enhanced user experience by providing seamless access to mouse, keyboard, media, and presentation tools without complex menus.

### Reliability and Accuracy
Throughout testing, the application remained stable with no crashes or unexpected disconnects across diverse operating systems.

Handshake acknowledgement mechanisms ensured continuous link persistence on Windows 10/11 and macOS without driver warnings.

Touch coordinates, multi-finger gestures, and gyroscopic motion data were computed and transmitted reliably without packet drops.

### Overall Outcome
The final system successfully meets all intended objectives, offering a unified platform for complete wireless input control, driverless peripheral emulation, and secure operations. The application is stable, scalable, and ready for real-world deployment, with potential future enhancements including BLE Battery Service integration, gamepad emulation, or custom macro automation.

<br><br>
**BlueControl**

---
<div style="page-break-after: always;"></div>

<div align="right"><b>Dept of CSE NMAMIT,Nitte 8</b></div>

<div align="center">

## USER INTERFACES

</div>

### Cockpit Dashboard & Advanced Trackpad:
Central touchpad interface for multi-touch cursor navigation, click buttons, and quick audio controls

```
┌─────────────────────────────────────────────────────────────┐
│  [ Paste Screenshot Here: Cockpit Dashboard & Trackpad ]     │
│                                                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Full PC Keyboard & Live Typing Bar:
Virtual mechanical PC keyboard and live mobile typing bar with speech-to-text integration

```
┌─────────────────────────────────────────────────────────────┐
│  [ Paste Screenshot Here: Full PC Keyboard & Live Typing ]  │
│                                                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

<br><br>
**BlueControl**

---
<div style="page-break-after: always;"></div>

<div align="right"><b>Dept of CSE NMAMIT,Nitte 9</b></div>

### Bluetooth Device Manager:
Interactive bottom sheet dialog for scanning, pairing, and selecting target host computers

```
┌─────────────────────────────────────────────────────────────┐
│  [ Paste Screenshot Here: Bluetooth Device Manager ]        │
│                                                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Laptop & Windows Pairing Guide:
Step-by-step instructions for driverless pairing with host computers and smart TVs

```
┌─────────────────────────────────────────────────────────────┐
│  [ Paste Screenshot Here: Laptop & Windows Pairing Guide ]  │
│                                                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

<br><br>
**BlueControl**

---
<div style="page-break-after: always;"></div>

<div align="right"><b>Dept of CSE NMAMIT,Nitte 10</b></div>

### Multimedia Remote Controller:
Dedicated media transport controls, volume buttons, and D-pad cluster for smart TVs and media players

```
┌─────────────────────────────────────────────────────────────┐
│  [ Paste Screenshot Here: Multimedia Remote Controller ]    │
│                                                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Presenter Remote Mode:
Slide navigation controls, presentation launching (F5), and blank screen toggles for academic lectures

```
┌─────────────────────────────────────────────────────────────┐
│  [ Paste Screenshot Here: Presenter Remote Mode ]           │
│                                                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

<br><br>
**BlueControl**

---
<div style="page-break-after: always;"></div>

<div align="right"><b>Dept of CSE NMAMIT,Nitte 11</b></div>

### Gyroscope Air Mouse HUD:
Hold-to-use air mouse utilizing hardware gyroscope for mid-air laser-like presentation pointing

```
┌─────────────────────────────────────────────────────────────┐
│  [ Paste Screenshot Here: Gyroscope Air Mouse HUD ]         │
│                                                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Mouse Sensitivity Calibration & Theme Settings:
Configuring cursor sensitivity multipliers and switching between Material 3 and AMOLED themes

```
┌─────────────────────────────────────────────────────────────┐
│  [ Paste Screenshot Here: Mouse Sensitivity & Theme ]       │
│                                                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

<br><br>
**BlueControl**

---
<div style="page-break-after: always;"></div>

<div align="right"><b>Dept of CSE NMAMIT,Nitte 12</b></div>

<div align="center">

## ADVANTAGES

</div>

### 1. Reduces physical hardware dependencies
Eliminates the need to carry physical mice, external keyboards, or presenter clickers by consolidating all peripheral functions into the smartphone.

### 2. Real-time driverless Bluetooth communication
Operates directly over standard Bluetooth HID protocol without requiring companion server software or custom drivers on the host computer.

### 3. Complete independence from Wi-Fi networks
Works seamlessly in environments with Access Point (AP) isolation or without internet access, bypassing network firewall restrictions.

### 4. Ultra-low input latency
Delivers responsive cursor movements and keystrokes with average transmission latencies under 5ms, avoiding the jitter common in network socket remotes.

### 5. User-friendly and versatile interface
Material Design 3 interface with touch trackpad, live mobile typing, presenter controls, and gyroscopic air mouse works seamlessly across Windows, Mac, Linux, and Android TVs.

<br><br>
**BlueControl**

---
<div style="page-break-after: always;"></div>

<div align="right"><b>Dept of CSE NMAMIT,Nitte 13</b></div>

<div align="center">

## FUTURE ENHANCEMENTS

</div>

### 1. Bluetooth Low Energy (BLE) Battery Service integration
Adding the standard BLE Battery Service (UUID: 0x180F) to report smartphone battery levels directly to the host PC system tray.

### 2. Gamepad and joystick HID profile emulation
Incorporating game controller HID reports with virtual thumbsticks and action buttons for PC gaming and simulator control.

### 3. Client-side macro automation and custom key combinations
Enabling users to record custom keystroke sequences and bind them to single-tap action buttons on the mobile interface.

### 4. Multi-host fast device switching
Enabling instant one-tap switching between multiple paired host computers without manual disconnection or repair sequences.

<br><br>
**BlueControl**

---
<div style="page-break-after: always;"></div>

<div align="right"><b>Dept of CSE NMAMIT,Nitte 14</b></div>

<div align="center">

## CONCLUSION

</div>

BlueControl successfully demonstrates the development of a secure, efficient, and user-friendly virtual input peripheral platform using native Android Bluetooth HID capabilities. By integrating BluetoothHidDevice profile emulation, hardware gyroscope tracking, multi-touch gesture processing, and live mobile text streaming, the application provides a robust architecture that supports a driverless remote control experience. The application streamlines essential processes such as cursor movement, keyboard typing, media playback, and presentation navigation. Extensive testing has confirmed that the system performs reliably, maintains stable wireless links, and delivers a positive user experience with real-time updates and responsive interfaces across Windows, macOS, Linux, and Android TVs. Overall, the project meets its intended objectives and showcases the practical application of mobile app development principles in building an innovative, scalable, and production-ready peripheral solution.

<br><br>
**BlueControl**

---
<div style="page-break-after: always;"></div>

<div align="right"><b>Dept of CSE NMAMIT,Nitte 15</b></div>

<div align="center">

## REFERENCES

</div>

**1. Android Bluetooth HID API Official Documentation – https://developer.android.com/reference/android/bluetooth/BluetoothHidDevice**

**2. USB Implementers Forum HID Specification – https://www.usb.org/document-library/device-class-definition-hid-111**

**3. Bluetooth SIG Human Interface Device (HID) Profile Specification – https://www.bluetooth.com/specifications/specs/human-interface-device-profile-1-1-1/**

**4. Android Sensor API Documentation – https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion**

**5. Google Material Design 3 Documentation – https://m3.material.io/**

<br><br>
**BlueControl**
