"""
Silero VAD (Voice Activity Detection) Engine
Phát hiện giọng nói và dứt câu thông minh chạy cực nhẹ trên CPU (~0% GPU).
Tự động cắt âm thanh ngay khi người dùng ngưng nói để gửi sang Whisper STT.
"""

import os
import torch
import numpy as np
from typing import Optional, List, Tuple


class SileroVADDetector:
    def __init__(
        self,
        threshold: float = 0.5,
        sampling_rate: int = 16000,
        min_speech_duration_ms: int = 250,
        min_silence_duration_ms: int = 600
    ):
        self.threshold = threshold
        self.sampling_rate = sampling_rate
        self.min_speech_samples = int(sampling_rate * (min_speech_duration_ms / 1000.0))
        self.min_silence_samples = int(sampling_rate * (min_silence_duration_ms / 1000.0))
        
        self.model = None
        self._load_model()
        self.reset()

    def _load_model(self):
        try:
            # Tải mô hình Silero VAD nhẹ (~1.5MB) qua torch hub hoặc torch.jit
            model, _ = torch.hub.load(
                repo_or_dir='snakers4/silero-vad',
                model='silero_vad',
                force_reload=False,
                onnx=False
            )
            self.model = model
            self.model.eval()
        except Exception as e:
            print(f"[VAD] Không thể tải online từ torch.hub, sử dụng bộ phát hiện năng lượng dự phòng: {e}")
            self.model = None

    def reset(self):
        """Khởi tạo lại trạng thái VAD."""
        if self.model is not None and hasattr(self.model, 'reset_states'):
            self.model.reset_states()
        self.is_speaking = False
        self.speech_buffer: List[np.ndarray] = []
        self.silence_samples_count = 0
        self.total_speech_samples = 0

    def process_chunk(self, audio_chunk: np.ndarray) -> Tuple[bool, Optional[np.ndarray]]:
        """
        Xử lý một đoạn audio chunk (khoảng 30-50ms, float32, 16kHz).
        Trả về: (is_currently_speaking, completed_speech_audio_or_None)
        Khi người dùng vừa dứt câu, completed_speech_audio sẽ là mảng numpy hoàn chỉnh.
        """
        if audio_chunk is None or len(audio_chunk) == 0:
            return False, None

        chunk_samples = len(audio_chunk)
        speech_prob = 0.0

        if self.model is not None:
            try:
                # Silero yêu cầu chunk 512 samples ở 16kHz
                tensor_chunk = torch.from_numpy(audio_chunk).float()
                if len(tensor_chunk) >= 512:
                    tensor_chunk = tensor_chunk[:512]
                else:
                    tensor_chunk = torch.nn.functional.pad(tensor_chunk, (0, 512 - len(tensor_chunk)))
                
                with torch.no_grad():
                    speech_prob = float(self.model(tensor_chunk, self.sampling_rate).item())
            except Exception:
                speech_prob = self._fallback_energy_prob(audio_chunk)
        else:
            speech_prob = self._fallback_energy_prob(audio_chunk)

        is_speech = speech_prob >= self.threshold

        if is_speech:
            self.is_speaking = True
            self.speech_buffer.append(audio_chunk)
            self.total_speech_samples += chunk_samples
            self.silence_samples_count = 0
            return True, None
        else:
            if self.is_speaking:
                # Đang trong câu nói nhưng gặp khoảng lặng
                self.speech_buffer.append(audio_chunk)
                self.silence_samples_count += chunk_samples

                # Nếu khoảng lặng vượt ngưỡng dứt câu (min_silence_duration_ms)
                if self.silence_samples_count >= self.min_silence_samples:
                    if self.total_speech_samples >= self.min_speech_samples:
                        # Đã hoàn thành một câu nói hợp lệ!
                        completed_audio = np.concatenate(self.speech_buffer)
                        self.reset()
                        return False, completed_audio
                    else:
                        # Tiếng động quá ngắn (<250ms), bỏ qua nhiễu
                        self.reset()
                        return False, None
            return False, None

    def _fallback_energy_prob(self, chunk: np.ndarray) -> float:
        """Bộ phát hiện năng lượng dự phòng nếu chưa tải được Silero model."""
        energy = np.sqrt(np.mean(chunk ** 2))
        return 0.8 if energy > 0.02 else 0.1
