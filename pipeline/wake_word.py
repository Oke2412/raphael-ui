"""
Wake Word Detection Engine (Phát hiện từ khóa đánh thức trợ lý)
Chạy hoàn toàn trên CPU bằng OpenWakeWord (ONNX), tiêu thụ ~0% GPU và < 25MB RAM.
Luôn lắng nghe ngầm khi ở trạng thái idle, khi nghe thấy từ khóa (ví dụ: "Hey Jarvis", "Raphael")
sẽ đánh thức hệ thống và phát tín hiệu tới UI.
"""

import os
import numpy as np
from typing import Optional, Dict, Any, Tuple


class WakeWordDetector:
    def __init__(self, config: Dict[str, Any]):
        self.enabled = config.get("enabled", True)
        self.threshold = config.get("threshold", 0.5)
        self.model_name = config.get("model", "hey_jarvis")
        self.custom_model_path = config.get("custom_model_path", "models/wakeword/raphael.onnx")
        self.model = None
        self._init_model()

    def _init_model(self):
        if not self.enabled:
            print("[WakeWord] Đang ở trạng thái tắt trong cấu hình.")
            return

        try:
            import openwakeword
            from openwakeword.model import Model

            # Kiểm tra nếu có mô hình tùy biến (ví dụ: raphael.onnx)
            if os.path.exists(self.custom_model_path):
                print(f"[WakeWord] Đang nạp mô hình tùy biến: {self.custom_model_path}...")
                self.model = Model(wakeword_models=[self.custom_model_path], inference_framework="onnx")
                print("[WakeWord] Nạp mô hình tùy biến thành công!")
                return

            # Nạp mô hình có sẵn của openwakeword (mặc định: hey_jarvis)
            print(f"[WakeWord] Đang nạp mô hình Wake Word: {self.model_name}...")
            self.model = Model(wakeword_models=[self.model_name], inference_framework="onnx")
            print(f"[WakeWord] Nạp mô hình Wake Word '{self.model_name}' thành công trên CPU!")
        except Exception as e:
            print(f"[WakeWord] Cảnh báo khởi tạo: {e}")
            self.model = None

    def process_chunk(self, audio_chunk: np.ndarray) -> Tuple[bool, str, float]:
        """
        Nhận mảng numpy float32 hoặc int16 (16kHz).
        Trả về: (detected: bool, keyword: str, score: float)
        """
        if not self.enabled or self.model is None or len(audio_chunk) == 0:
            return False, "", 0.0

        try:
            # openwakeword yêu cầu mảng int16 (16-bit PCM)
            if audio_chunk.dtype == np.float32:
                # Chuyển float32 (-1.0 to 1.0) sang int16
                samples_int16 = (np.clip(audio_chunk, -1.0, 1.0) * 32767).astype(np.int16)
            else:
                samples_int16 = audio_chunk.astype(np.int16)

            # Dự đoán xác suất từ khóa
            prediction = self.model.predict(samples_int16)

            for mdl_name, scores in self.model.prediction_buffer.items():
                if len(scores) > 0:
                    current_score = float(scores[-1])
                    if current_score >= self.threshold:
                        # Reset bộ đệm để tránh kích hoạt lặp
                        self.model.reset()
                        return True, mdl_name, current_score

            return False, "", 0.0
        except Exception as e:
            return False, "", 0.0

    def reset(self):
        if self.model is not None:
            try:
                self.model.reset()
            except Exception:
                pass
