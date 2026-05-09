"""
config.py  –  All configurable parameters for the Road Hazard Detection System.
Edit the values in this file before deploying to the Raspberry Pi.
"""


class Config:
    # ── Model ──────────────────────────────────────────────────────────────
    # Path to your trained YOLOv8 weights file (.pt)
    MODEL_PATH          = "models/best.pt"

    # Minimum detection confidence to trigger an upload (0.0 – 1.0)
    CONFIDENCE_THRESHOLD = 0.55

    # Class names MUST match training label order exactly
    # ID 0 → pothole, 1 → speed_bump, 2 → crosswalk, 3 → road_debris
    CLASS_NAMES = ["pothole", "speed_bump", "crosswalk", "road_debris"]

    # ── Camera ────────────────────────────────────────────────────────────
    CAMERA_INDEX  = 0       # 0 = Pi Camera / first USB webcam
    FRAME_WIDTH   = 640
    FRAME_HEIGHT  = 480
    FRAME_INTERVAL_S = 0.5  # process one frame every N seconds

    # ── GPS (NEO-7M) ──────────────────────────────────────────────────────
    GPS_PORT = "/dev/ttyAMA0"   # UART on Pi 4; use /dev/ttyUSB0 for USB adapter
    GPS_BAUD = 9600
    GPS_TIMEOUT_S = 2.0         # max wait per GPS read attempt

    # ── Buzzer ────────────────────────────────────────────────────────────
    BUZZER_PIN      = 17        # BCM GPIO pin connected to buzzer IN/+ wire
    BUZZER_DURATION_S = 0.3     # duration of each buzz pulse

    # ── Firebase ──────────────────────────────────────────────────────────
    # Download from Firebase Console → Project Settings → Service Accounts
    FIREBASE_CREDENTIALS = "serviceAccountKey.json"
    FIREBASE_DB_URL      = "https://roadhazardmap-default-rtdb.asia-southeast1.firebasedatabase.app"
    DEVICE_ID            = "rpi-unit-01"   # unique identifier for this Pi unit

    # ── Deduplication ────────────────────────────────────────────────────
    # Don't re-upload the same class if the Pi is within this many metres
    DEDUP_RADIUS_M   = 15.0
    # ...and within this many seconds of the last upload for that class
    DEDUP_COOLDOWN_S = 5.0
