"""
Faster-Whisper Speech-to-Text Engine
Chuyển đổi âm thanh thu được thành văn bản tiếng Việt cực nhanh trên GPU RTX 2050.
Tối ưu hóa VRAM bằng định dạng int8_float16 hoặc float16.
"""

import os
import numpy as np
from typing import Optional


class STTEngine:
    def __init__(
        self,
        model_size: str = "base",
        device: str = "cuda",
        compute_type: str = "int8_float16",
        language: str = "vi",
        download_root: Optional[str] = "models/whisper"
    ):
        self.model_size = model_size
        self.device = device
        self.compute_type = compute_type
        self.language = language
        self.download_root = download_root
        self.model = None
        self._init_model()

    def _init_model(self):
        try:
            from faster_whisper import WhisperModel
            os.makedirs(self.download_root, exist_ok=True)
            print(f"[STT] Đang nạp Faster-Whisper ({self.model_size}) trên {self.device} ({self.compute_type})...")
            self.model = WhisperModel(
                self.model_size,
                device=self.device,
                compute_type=self.compute_type,
                download_root=self.download_root
            )
            print(f"[STT] Nạp Faster-Whisper thành công!")
        except Exception as e:
            print(f"[STT] Cảnh báo khi nạp CUDA: {e}")
            try:
                from faster_whisper import WhisperModel
                print("[STT] Chuyển sang chế độ CPU (int8) dự phòng...")
                self.model = WhisperModel(
                    self.model_size,
                    device="cpu",
                    compute_type="int8",
                    download_root=self.download_root
                )
                print("[STT] Nạp Faster-Whisper trên CPU thành công!")
            except Exception as ex2:
                print(f"[STT] Lỗi nghiêm trọng: Không thể khởi tạo Faster-Whisper: {ex2}")
                self.model = None

    def transcribe(self, audio_data: np.ndarray) -> str:
        """
        Nhận mảng numpy float32 (16kHz mono) và chuyển đổi thành văn bản.
        """
        if self.model is None or audio_data is None or len(audio_data) == 0:
            return ""

        try:
            # Faster-Whisper hỗ trợ trực tiếp mảng numpy float32 16kHz
            segments, info = self.model.transcribe(
                audio_data,
                language=self.language,
                beam_size=3,
                temperature=0.0,
                vad_filter=False  # Đã có Silero VAD lọc ở tầng trước
            )
            transcription = " ".join([segment.text for segment in segments]).strip()
            return transcription
        except Exception as e:
            print(f"[STT] Lỗi khi nhận diện giọng nói: {e}")
            return ""
