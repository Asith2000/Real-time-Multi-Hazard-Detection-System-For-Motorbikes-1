#!/usr/bin/env python3
"""
test_components.py
==================
Run individual subsystem tests before starting the full pipeline.
Useful for verifying wiring on first deployment.

Usage:
    python3 test_components.py --all
    python3 test_components.py --gps
    python3 test_components.py --buzzer
    python3 test_components.py --camera
    python3 test_components.py --firebase
    python3 test_components.py --model
"""

import argparse
import sys
import time

from config import Config

cfg = Config()


def test_camera():
    print("\n── Camera Test ──────────────────────────────")
    import cv2
    cap = cv2.VideoCapture(cfg.CAMERA_INDEX)
    if not cap.isOpened():
        print("FAIL: Cannot open camera.")
        return False
    ret, frame = cap.read()
    cap.release()
    if not ret or frame is None:
        print("FAIL: Could not read a frame.")
        return False
    h, w = frame.shape[:2]
    print(f"PASS: Camera opened. Frame size = {w}×{h}")
    return True


def test_gps():
    print("\n── GPS Test (10 s) ──────────────────────────")
    from utils.gps_reader import GPSReader
    gps = GPSReader(cfg.GPS_PORT, cfg.GPS_BAUD)
    print(f"Listening on {cfg.GPS_PORT} for 10 seconds…")
    deadline = time.time() + 10
    while time.time() < deadline:
        lat, lng = gps.read()
        if lat is not None:
            print(f"PASS: GPS fix received → lat={lat:.6f}  lng={lng:.6f}")
            gps.close()
            return True
        time.sleep(0.5)
    gps.close()
    print("WARN: No GPS fix within 10 s (outdoors needed, or no satellite lock yet).")
    return False


def test_buzzer():
    print("\n── Buzzer Test ──────────────────────────────")
    from utils.buzzer import Buzzer
    buzzer = Buzzer(cfg.BUZZER_PIN)
    print("Beeping 3 times…")
    buzzer.beep(duration=0.4, pulses=3)
    time.sleep(2.5)
    buzzer.cleanup()
    print("PASS: Buzzer sequence complete. Did you hear 3 beeps?")
    return True


def test_firebase():
    print("\n── Firebase Test ────────────────────────────")
    from utils.firebase_uploader import FirebaseUploader
    uploader = FirebaseUploader(
        cfg.FIREBASE_CREDENTIALS, cfg.FIREBASE_DB_URL, cfg.DEVICE_ID
    )
    record_id = uploader.upload(
        hazard_class="pothole",
        lat=6.927100,
        lng=79.861200,
        confidence=0.99,
    )
    if record_id:
        print(f"PASS: Test record uploaded → hazards/{record_id}")
        print("      Check Firebase Console to confirm.")
        return True
    else:
        print("FAIL: Upload returned None. Check serviceAccountKey.json and DB URL.")
        return False


def test_model():
    print("\n── YOLO Model Test ──────────────────────────")
    import cv2
    import numpy as np
    from utils.detector import HazardDetector

    try:
        detector = HazardDetector(cfg.MODEL_PATH, cfg.CONFIDENCE_THRESHOLD, cfg.CLASS_NAMES)
    except Exception as e:
        print(f"FAIL: Model load error: {e}")
        return False

    # Run on a blank frame (no detections expected – just checking it doesn't crash)
    blank = np.zeros((480, 640, 3), dtype=np.uint8)
    results = detector.detect(blank)
    print(f"PASS: Model inference OK on blank frame. Detections = {len(results)}")
    return True


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Component tests for Road Hazard System")
    parser.add_argument("--all",      action="store_true")
    parser.add_argument("--camera",   action="store_true")
    parser.add_argument("--gps",      action="store_true")
    parser.add_argument("--buzzer",   action="store_true")
    parser.add_argument("--firebase", action="store_true")
    parser.add_argument("--model",    action="store_true")
    args = parser.parse_args()

    if not any(vars(args).values()):
        parser.print_help()
        sys.exit(0)

    results = {}
    if args.all or args.camera:   results["Camera"]   = test_camera()
    if args.all or args.gps:      results["GPS"]      = test_gps()
    if args.all or args.buzzer:   results["Buzzer"]   = test_buzzer()
    if args.all or args.firebase: results["Firebase"] = test_firebase()
    if args.all or args.model:    results["Model"]    = test_model()

    print("\n── Results ──────────────────────────────────")
    for name, passed in results.items():
        status = "✓ PASS" if passed else "✗ FAIL"
        print(f"  {status}  {name}")
