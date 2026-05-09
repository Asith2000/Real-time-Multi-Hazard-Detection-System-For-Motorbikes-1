"""
utils/buzzer.py
===============
Driver for a continuous (active) buzzer on a Raspberry Pi GPIO pin.

Each hazard class has a distinct buzz pattern so the rider can
identify the hazard type by sound alone — no need to look at the screen.

Pattern key  (ON ms, OFF ms, repeats)
──────────────────────────────────────
pothole      : 3 long bursts          — serious road damage, urgent
speed_bump   : 2 medium double-pulses — moderate, rhythmic warning
crosswalk    : 4 rapid short ticks    — pedestrian zone, quick attention
road_debris  : long-short-short       — scattered hazard, distinctive
"""

import logging
import time
import threading

log = logging.getLogger(__name__)

# ── Pattern definitions ────────────────────────────────────────────────────
# Each entry is a list of (on_seconds, off_seconds) pairs played in sequence.
# The rider learns each pattern by association with the hazard type.

PATTERNS = {
    # 3 long blasts  — — —
    "pothole": [
        (0.6, 0.15),
        (0.6, 0.15),
        (0.6, 0.00),
    ],

    # 2 medium double-pulses  — —   — —
    "speed_bump": [
        (0.25, 0.10),
        (0.25, 0.35),
        (0.25, 0.10),
        (0.25, 0.00),
    ],

    # 4 rapid short ticks  · · · ·
    "crosswalk": [
        (0.10, 0.10),
        (0.10, 0.10),
        (0.10, 0.10),
        (0.10, 0.00),
    ],

    # Long-short-short  — · ·  (morse D, easy to remember as "Debris")
    "road_debris": [
        (0.50, 0.15),
        (0.15, 0.15),
        (0.15, 0.00),
    ],

    # Generic fallback — single medium beep
    "default": [
        (0.30, 0.00),
    ],
}


class Buzzer:
    def __init__(self, pin: int):
        self._pin  = pin
        self._lock = threading.Lock()

        try:
            import RPi.GPIO as GPIO
            self._GPIO = GPIO
            GPIO.setmode(GPIO.BCM)
            GPIO.setwarnings(False)
            GPIO.setup(pin, GPIO.OUT, initial=GPIO.LOW)
            log.info(f"Buzzer initialised on GPIO BCM {pin}.")
        except (ImportError, RuntimeError) as e:
            log.warning(f"GPIO unavailable ({e}). Buzzer will be simulated in logs.")
            self._GPIO = None

    # ── Public API ─────────────────────────────────────────────────────────

    def alert(self, hazard_class: str):
        """
        Play the buzz pattern for a specific hazard class.
        Non-blocking — runs in a daemon thread.

        Patterns:
            pothole     → 3 long blasts          (— — —)
            speed_bump  → 2 medium double-pulses  (— —  — —)
            crosswalk   → 4 rapid short ticks     (· · · ·)
            road_debris → long-short-short         (— · ·)
        """
        pattern = PATTERNS.get(hazard_class, PATTERNS["default"])
        log.info(f"Buzzer: '{hazard_class}' pattern → {pattern}")
        t = threading.Thread(
            target=self._play_pattern,
            args=(pattern,),
            daemon=True,
        )
        t.start()

    def beep(self, duration: float = 0.3, pulses: int = 1, gap: float = 0.12):
        """Simple beep — used for GPS-no-fix warning and backward compatibility."""
        pattern = [(duration, gap)] * (pulses - 1) + [(duration, 0.0)]
        t = threading.Thread(
            target=self._play_pattern,
            args=(pattern,),
            daemon=True,
        )
        t.start()

    # ── Internal ───────────────────────────────────────────────────────────

    def _play_pattern(self, pattern: list):
        with self._lock:   # only one pattern plays at a time
            for on_s, off_s in pattern:
                self._set(True)
                time.sleep(on_s)
                self._set(False)
                if off_s > 0:
                    time.sleep(off_s)

    def _set(self, on: bool):
        if self._GPIO is None:
            log.debug(f"[BUZZER SIM] {'ON ' if on else 'OFF'}")
            return
        self._GPIO.output(
            self._pin,
            self._GPIO.HIGH if on else self._GPIO.LOW,
        )

    def cleanup(self):
        self._set(False)
        if self._GPIO is not None:
            try:
                self._GPIO.cleanup(self._pin)
                log.info("GPIO cleaned up.")
            except Exception:
                pass
