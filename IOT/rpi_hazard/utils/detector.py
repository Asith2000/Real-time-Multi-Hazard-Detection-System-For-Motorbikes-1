"""
utils/detector.py
=================
YOLOv8 inference using TFLite runtime — no PyTorch / ultralytics needed.

Model:  best_float32.tflite  (exported from YOLOv8 via ultralytics on a PC)

YOLOv8 TFLite output layout
----------------------------
The model has a single output tensor shaped:
    [1, 8, 8400]   for a 640×640 input
    │  │   └─ 8400 anchor candidates
    │  └───── 8 values per anchor:  cx, cy, w, h, conf_cls0, conf_cls1, conf_cls2, conf_cls3
    └──────── batch size (always 1 here)

We transpose to [8400, 8], then for each row:
  - cx, cy, w, h  are box coords in [0,1] relative to input size
  - columns 4..7  are per-class confidence scores (no separate objectness)
  - best_class  = argmax(cols 4..7)
  - confidence  = max(cols 4..7)
"""

import logging
import cv2
import numpy as np

log = logging.getLogger(__name__)


class HazardDetector:
    """
    Loads a YOLOv8 float32 TFLite model and runs frame-level inference.

    Returns a list of (class_name, confidence, (x1,y1,x2,y2)) per frame.
    Bounding boxes are in pixel coordinates of the *original* frame.
    """

    def __init__(self, model_path: str, confidence_threshold: float,
                 class_names: list, input_size: int = 640):
        self._conf       = confidence_threshold
        self._names      = class_names
        self._input_size = input_size

        log.info(f"Loading TFLite model: {model_path}")
        try:
            from tflite_runtime.interpreter import Interpreter
        except ImportError:
            # Fall back to the full TensorFlow Lite package
            from tensorflow.lite.python.interpreter import Interpreter

        self._interpreter = Interpreter(model_path=model_path)
        self._interpreter.allocate_tensors()

        self._input_details  = self._interpreter.get_input_details()
        self._output_details = self._interpreter.get_output_details()

        # Log expected input shape so mismatches are obvious
        input_shape = self._input_details[0]['shape']
        log.info(f"Model input shape: {input_shape}")
        log.info("TFLite model loaded successfully.")

    # ── Public API ─────────────────────────────────────────────────────────
    def detect(self, frame) -> list:
        """
        Run inference on a BGR OpenCV frame (numpy uint8 array).

        Returns list of (class_name, confidence, (x1, y1, x2, y2)).
        Coordinates are pixel values in the original frame dimensions.
        """
        orig_h, orig_w = frame.shape[:2]

        # 1. Pre-process: resize → RGB → normalise → add batch dim
        blob = self._preprocess(frame)

        # 2. Inference
        self._interpreter.set_tensor(self._input_details[0]['index'], blob)
        self._interpreter.invoke()
        raw = self._interpreter.get_tensor(self._output_details[0]['index'])

        # 3. Post-process
        return self._postprocess(raw, orig_w, orig_h)

    # ── Internal helpers ───────────────────────────────────────────────────
    def _preprocess(self, frame):
        """Resize, convert BGR→RGB, normalise to [0,1], add batch dimension."""
        resized = cv2.resize(frame, (self._input_size, self._input_size))
        rgb     = cv2.cvtColor(resized, cv2.COLOR_BGR2RGB)
        norm    = rgb.astype(np.float32) / 255.0
        return np.expand_dims(norm, axis=0)   # shape: (1, H, W, 3)

    def _postprocess(self, raw_output, orig_w: int, orig_h: int) -> list:
        """
        Parse YOLOv8 TFLite output and return filtered detections.

        raw_output shape: (1, 8, 8400)  — batch, values, anchors
        """
        # Squeeze batch dim → (8, 8400), then transpose → (8400, 8)
        preds = raw_output[0].T   # (8400, 8)

        results = []
        for row in preds:
            cx, cy, w, h = row[0], row[1], row[2], row[3]
            class_scores = row[4:]                      # shape (4,)
            class_id     = int(np.argmax(class_scores))
            confidence   = float(class_scores[class_id])

            if confidence < self._conf:
                continue
            if class_id >= len(self._names):
                continue

            # Convert normalised cx,cy,w,h → pixel x1,y1,x2,y2
            x1 = int((cx - w / 2) * orig_w)
            y1 = int((cy - h / 2) * orig_h)
            x2 = int((cx + w / 2) * orig_w)
            y2 = int((cy + h / 2) * orig_h)

            # Clamp to frame bounds
            x1, y1 = max(0, x1), max(0, y1)
            x2, y2 = min(orig_w, x2), min(orig_h, y2)

            results.append((self._names[class_id], confidence, (x1, y1, x2, y2)))

        # Non-maximum suppression to remove duplicate boxes
        return self._nms(results)

    @staticmethod
    def _nms(detections: list, iou_threshold: float = 0.45) -> list:
        """Simple class-aware NMS."""
        if not detections:
            return []

        # Group by class
        by_class: dict = {}
        for name, conf, box in detections:
            by_class.setdefault(name, []).append((conf, box))

        kept = []
        for name, items in by_class.items():
            items.sort(key=lambda x: x[0], reverse=True)
            while items:
                best_conf, best_box = items.pop(0)
                kept.append((name, best_conf, best_box))
                items = [
                    (c, b) for c, b in items
                    if HazardDetector._iou(best_box, b) < iou_threshold
                ]
        return kept

    @staticmethod
    def _iou(box_a, box_b) -> float:
        """Intersection-over-Union for two (x1,y1,x2,y2) boxes."""
        ax1, ay1, ax2, ay2 = box_a
        bx1, by1, bx2, by2 = box_b
        inter_x1 = max(ax1, bx1)
        inter_y1 = max(ay1, by1)
        inter_x2 = min(ax2, bx2)
        inter_y2 = min(ay2, by2)
        inter    = max(0, inter_x2 - inter_x1) * max(0, inter_y2 - inter_y1)
        area_a   = (ax2 - ax1) * (ay2 - ay1)
        area_b   = (bx2 - bx1) * (by2 - by1)
        union    = area_a + area_b - inter
        return inter / union if union > 0 else 0.0
