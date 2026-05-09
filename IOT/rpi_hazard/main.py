#!/usr/bin/env python3
"""
main.py  —  Road Hazard Detection System
=========================================
Raspberry Pi 4  |  Pi Camera (picamera2/libcamera)  |  NEO-7M GPS  |  Active Buzzer

Each hazard class triggers a distinct buzzer pattern:
    pothole     → 3 long blasts          (— — —)
    speed_bump  → 2 medium double-pulses  (— —  — —)
    crosswalk   → 4 rapid short ticks     (· · · ·)
    road_debris → long-short-short        (— · ·)
"""

import time
import logging
import signal
import sys
import math

from config import Config
from utils.gps_reader import GPSReader
from utils.buzzer import Buzzer
from utils.firebase_uploader import FirebaseUploader
from utils.detector import HazardDetector, CameraCapture

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler("hazard_detection.log"),
    ],
)
log = logging.getLogger(__name__)

_running = True

def _handle_signal(sig, frame):
    global _running
    log.info("Shutdown signal received.")
    _running = False

signal.signal(signal.SIGINT,  _handle_signal)
signal.signal(signal.SIGTERM, _handle_signal)


def _haversine_m(lat1, lng1, lat2, lng2):
    R = 6_371_000
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp = math.radians(lat2 - lat1)
    dl = math.radians(lng2 - lng1)
    a  = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return R * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))


class DedupTracker:
    def __init__(self, radius_m=15.0, cooldown_s=5.0):
        self._radius   = radius_m
        self._cooldown = cooldown_s
        self._last     = {}

    def is_duplicate(self, hazard_class, lat, lng):
        if hazard_class not in self._last:
            return False
        plat, plng, pt = self._last[hazard_class]
        if time.time() - pt < self._cooldown:
            return True
        return _haversine_m(plat, plng, lat, lng) < self._radius

    def record(self, hazard_class, lat, lng):
        self._last[hazard_class] = (lat, lng, time.time())


def main():
    cfg = Config()

    log.info("Initialising subsystems…")
    camera   = CameraCapture(cfg.FRAME_WIDTH, cfg.FRAME_HEIGHT)
    detector = HazardDetector(cfg.MODEL_PATH, cfg.CONFIDENCE_THRESHOLD, cfg.CLASS_NAMES)
    gps      = GPSReader(cfg.GPS_PORT, cfg.GPS_BAUD)
    buzzer   = Buzzer(cfg.BUZZER_PIN)
    uploader = FirebaseUploader(cfg.FIREBASE_CREDENTIALS, cfg.FIREBASE_DB_URL, cfg.DEVICE_ID)
    dedup    = DedupTracker(radius_m=cfg.DEDUP_RADIUS_M, cooldown_s=cfg.DEDUP_COOLDOWN_S)

    log.info("Detection loop started. Press Ctrl+C to stop.")
    log.info("Buzzer patterns: pothole=3 long  speed_bump=2 double  crosswalk=4 short  road_debris=long-short-short")
    last_frame_time = 0.0

    try:
        while _running:
            now = time.time()
            if now - last_frame_time < cfg.FRAME_INTERVAL_S:
                time.sleep(0.05)
                continue
            last_frame_time = now

            frame = camera.read()
            if frame is None:
                log.warning("Frame capture failed — retrying…")
                time.sleep(0.2)
                continue

            detections = detector.detect(frame)
            if not detections:
                continue

            lat, lng = gps.read()
            if lat is None:
                log.warning("No GPS fix yet — skipping upload.")
                buzzer.beep(duration=0.1)   # short single beep = no GPS
                continue

            for class_name, confidence, _bbox in detections:
                if dedup.is_duplicate(class_name, lat, lng):
                    log.debug(f"Dedup skip: {class_name} @ ({lat:.5f}, {lng:.5f})")
                    continue

                log.info(f"Detected: {class_name}  conf={confidence:.2f}  @ ({lat:.6f}, {lng:.6f})")

                # Play the class-specific buzzer pattern
                buzzer.alert(class_name)

                # Upload to Firebase RTDB
                record_id = uploader.upload(class_name, lat, lng, confidence)
                if record_id:
                    log.info(f"Uploaded → hazards/{record_id}")
                    dedup.record(class_name, lat, lng)
                else:
                    log.error("Upload failed — will retry next detection.")

    finally:
        log.info("Cleaning up…")
        camera.release()
        gps.close()
        buzzer.cleanup()
        log.info("Done.")


if __name__ == "__main__":
    main()
