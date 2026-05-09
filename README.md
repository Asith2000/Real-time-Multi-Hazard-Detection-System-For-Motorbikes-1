# REAL-TIME MULTI-HAZARD DETECTION SYSTEM FOR MOTORBIKES: AN INTEGRATED EMBEDDED VISION AND COMMUNITY MAPPING APPROACH

AI-powered real-time motorcycle road hazard detection and community mapping system using YOLO26n, Raspberry Pi 4 Model B, Firebase RTDB, and an Android application.

---

## 🚀 Project Overview

This project detects road hazards such as potholes, speed bumps, crosswalks, and road debris in real time using a Raspberry Pi-mounted camera and a custom-trained YOLO26n model. Detected hazards are geo-tagged using GPS coordinates and uploaded to Firebase Realtime Database. A companion Android app visualizes hazards on Google Maps and provides proximity-based rider alerts.

The system aims to improve motorcycle rider safety through edge AI and community-driven hazard mapping.

---

## ✨ Features

* Real-time road hazard detection
* YOLO26n edge AI inference on Raspberry Pi
* GPS-based hazard geo-tagging
* Firebase cloud database integration
* Android app with live Google Maps visualization
* Rider buzzer alert system
* Community-shared hazard mapping
* Proximity-based warning alerts
* Lightweight and low-cost deployment

---

## 🛣️ Detection Classes

| Class       | Description            |
| ----------- | ---------------------- |
| Pothole     | Road surface cavity    |
| Speed Bump  | Raised road obstacle   |
| Crosswalk   | Pedestrian crossing    |
| Road Debris | Foreign object on road |

---

## 🧰 Hardware Components

| Component                     | Purpose                   |
| ----------------------------- | ------------------------- |
| Raspberry Pi 4 Model B        | Edge AI processing        |
| Pi Camera v2 / Sony IMX219    | Real-time video capture   |
| NEO-7M GPS Module             | GPS geo-tagging           |
| Active Buzzer                 | Rider alert notifications |
| Power Supply / Buck Converter | System power management   |

---

## 💻 Software Stack

* Python
* YOLO26n
* TensorFlow Lite INT8
* OpenCV
* picamera2
* Firebase Realtime Database
* Android Studio (Java)
* Google Maps SDK

---

## 📂 Project Structure

```bash
rpi_hazard/
├── main.py
├── config.py
├── test_components.py
├── serviceAccountKey.json
├── models/
│   └── best.pt
└── utils/
    ├── detector.py
    ├── gps_reader.py
    ├── buzzer.py
    └── firebase_uploader.py
```

---

## ⚙️ Raspberry Pi Setup

### Install Dependencies

```bash
sudo apt update && sudo apt full-upgrade -y

sudo apt install -y \
python3-pip \
python3-opencv \
libatlas-base-dev \
python3-serial \
python3-picamera2
```

### Install PyTorch & YOLO

```bash
sudo pip3 install torch torchvision \
--index-url https://download.pytorch.org/whl/cpu \
--break-system-packages

sudo pip3 install ultralytics \
firebase-admin \
pynmea2 \
RPi.GPIO \
--break-system-packages
```

---

## ▶️ Run the System

```bash
cd ~/rpi_hazard
python3 main.py
```

---

## 📡 Firebase RTDB Structure

```json
{
  "hazards": {
    "<uuid>": {
      "hazardClass": "pothole",
      "latitude": 6.9271,
      "longitude": 79.8612,
      "timestamp": 1711425600000,
      "confidence": 0.92,
      "deviceId": "rpi-unit-01"
    }
  }
}
```

---

## 📱 Android Application

The Android application provides:

* Live Google Maps hazard visualization
* Firebase synchronization
* Rider proximity alerts
* User authentication
* Hazard marker color categorization

---

## 🧠 Model Training

The YOLO26n model was trained on a custom dataset of 2,344 annotated images containing:

* Potholes
* Speed bumps
* Crosswalks
* Road debris

Training was performed using Google Colab with GPU acceleration.

---

## 🔔 Alert System

The system triggers different buzzer patterns based on hazard type to provide instant rider feedback while driving.

---

## 🌍 Community Hazard Mapping

All detected hazards are stored in Firebase and shared across users through the Android application, enabling a crowd-sourced road safety ecosystem.

---

## 📌 Future Improvements

* Rugged weatherproof hardware enclosure
* Night-time hazard detection enhancement
* Additional road hazard classes
* Mobile push notifications
* Offline map caching
* Cloud analytics dashboard

---

## 👨‍💻 Developed For

Research project focused on improving motorcycle road safety using Edge AI, IoT, Computer Vision, and Community Mapping technologies.

| Field           | Details                                                                        |
| --------------- | ------------------------------------------------------------------------------ |
| Project Title   | Road Multi-Hazard Detection and Community Mapping System for Motorcycle Safety |
| Author          | P.L. Asith Jayasahan Liyanage                                                  |
| Registration No | SEU/IS/19/ICT/031                                                              |
| Supervisor      | Mr. M.S. Suhail Razeeth, Probationary Lecturer, Department of ICT              |
| Institution     | South Eastern University of Sri Lanka                                          |
| Faculty         | Faculty of Technology                                                          |
| Degree          | BICT (Hons) in Software Technologies                                           |
| Year            | 2026                                                                           |


---

## 📄 License

This project is developed for academic and research purposes.
