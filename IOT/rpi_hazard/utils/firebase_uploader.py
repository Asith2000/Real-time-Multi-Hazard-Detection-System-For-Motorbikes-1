"""
utils/firebase_uploader.py
==========================
Uploads detections to Firebase RTDB with the exact structure the Android
app expects:

  hazards/<uuid12> {
      hazardClass  : str    pothole | speed_bump | crosswalk | road_debris
      latitude     : float
      longitude    : float
      timestamp    : int    Unix ms
      confidence   : float  4 d.p.
      deviceId     : str
  }
"""

import logging
import time
import uuid

log = logging.getLogger(__name__)


class FirebaseUploader:
    def __init__(self, credentials_path: str, db_url: str, device_id: str):
        self._device_id = device_id
        self._ref       = None

        try:
            import firebase_admin
            from firebase_admin import credentials, db

            if not firebase_admin._apps:
                cred = credentials.Certificate(credentials_path)
                firebase_admin.initialize_app(cred, {"databaseURL": db_url})
                log.info("Firebase Admin SDK initialised.")
            else:
                log.info("Firebase already initialised.")

            self._db  = db
            self._ref = db.reference("hazards")
            log.info(f"Connected to RTDB: {db_url}")

        except Exception as e:
            log.error(f"Firebase init failed: {e}")
            log.error("Check serviceAccountKey.json path and RTDB URL in config.py.")

    def upload(self, hazard_class: str, lat: float, lng: float,
               confidence: float) -> str | None:
        """
        Write one hazard record. Returns the record key on success, None on failure.
        """
        if self._ref is None:
            log.error("Firebase not initialised — skipping upload.")
            return None

        record_id = str(uuid.uuid4()).replace("-", "")[:12]
        record = {
            "hazardClass": hazard_class,
            "latitude":    round(lat, 6),
            "longitude":   round(lng, 6),
            "timestamp":   int(time.time() * 1000),
            "confidence":  round(float(confidence), 4),
            "deviceId":    self._device_id,
        }

        try:
            self._ref.child(record_id).set(record)
            log.info(
                f"[Firebase] hazards/{record_id} → "
                f"{hazard_class} ({confidence:.1%}) @ ({lat:.6f}, {lng:.6f})"
            )
            return record_id
        except Exception as e:
            log.error(f"Upload error: {e}")
            return None
