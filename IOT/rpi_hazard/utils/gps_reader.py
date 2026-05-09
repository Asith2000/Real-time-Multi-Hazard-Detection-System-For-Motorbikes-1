"""
utils/gps_reader.py
===================
Background-threaded NMEA reader for the NEO-7M GPS module.
Reads from UART (/dev/ttyAMA0) or USB (/dev/ttyUSB0).

Returns the latest valid (latitude, longitude) fix, or (None, None).
"""

import logging
import time
import threading

import serial
import pynmea2

log = logging.getLogger(__name__)


class GPSReader:
    def __init__(self, port: str, baud: int, timeout_s: float = 2.0):
        self._timeout = timeout_s
        self._lat: float | None = None
        self._lng: float | None = None
        self._lock = threading.Lock()

        try:
            self._serial = serial.Serial(port, baudrate=baud, timeout=1)
            log.info(f"GPS serial opened: {port} @ {baud}")
        except serial.SerialException as e:
            log.error(f"Cannot open GPS port {port}: {e}")
            self._serial = None
            return

        self._thread = threading.Thread(target=self._read_loop, daemon=True)
        self._thread.start()

    def read(self) -> tuple:
        """Return (latitude, longitude) or (None, None) if no fix yet."""
        with self._lock:
            return self._lat, self._lng

    def close(self):
        if self._serial and self._serial.is_open:
            self._serial.close()
            log.info("GPS serial closed.")

    def _read_loop(self):
        if self._serial is None:
            return
        while True:
            try:
                raw  = self._serial.readline()
                line = raw.decode("ascii", errors="replace").strip()

                if not line.startswith(("$GPRMC", "$GPGGA", "$GNRMC", "$GNGGA")):
                    continue

                msg = pynmea2.parse(line)

                # Skip void / no-fix sentences
                if hasattr(msg, "status") and msg.status == "V":
                    continue

                lat = float(msg.latitude)  if getattr(msg, "latitude",  "") != "" else 0.0
                lng = float(msg.longitude) if getattr(msg, "longitude", "") != "" else 0.0

                if lat == 0.0 and lng == 0.0:
                    continue

                with self._lock:
                    self._lat = lat
                    self._lng = lng

            except pynmea2.ParseError:
                pass
            except serial.SerialException as e:
                log.error(f"GPS serial error: {e}")
                time.sleep(1)
            except Exception as e:
                log.warning(f"GPS read warning: {e}")
