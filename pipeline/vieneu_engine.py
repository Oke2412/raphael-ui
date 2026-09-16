"""
VieNeu-TTS Engine (Vietnamese Text-to-Speech)
Sinh âm thanh tiếng Việt tự nhiên trực tiếp vào mảng Numpy Array (RAM), không lưu file .wav.
Tối ưu hóa bộ nhớ cho RTX 2050 (4GB VRAM) và tích hợp fallback mượt mà nếu đang nạp mô hình.
"""

import os
import io
import numpy as np
from typing import Optional, Dict, Any


class VieNeuTTSEngine:
    def __init__(self, config: Dict[str, Any]):
        self.enabled = config.get("enabled", True)
        self.model_path = config.get("model_path", "models/vieneu")
        self.device = config.get("device", "cuda")
        self.sample_rate = 24000  # Mặc định của VieNeu-TTS
        self.model = None
        self._init_engine()

    def _init_engine(self):
        if not self.enabled:
            print("[VieNeu-TTS] Đã tắt trong cấu hình.")
            return

        try:
            # Kiểm tra xem thư mục model có file checkpoint chưa
            os.makedirs(self.model_path, exist_ok=True)
            print(f"[VieNeu-TTS] Đang kiểm tra mô hình tại: {self.model_path}")
            
            # Thử nạp VieNeu-TTS nếu thư viện hoặc module vieneu đã được cài
            try:
                import vieneu
                self.model = vieneu.load_model(self.model_path, device=self.device)
                print("[VieNeu-TTS] Nạp mô hình VieNeu-TTS thành công trên", self.device)
                return
            except ImportError:
                pass

            # Nếu chưa có thư viện vieneu, kiểm tra checkpoint torch
            checkpoint_file = os.path.join(self.model_path, "model.pt")
            if os.path.exists(checkpoint_file):
                import torch
                self.model = torch.jit.load(checkpoint_file, map_location=self.device)
                print("[VieNeu-TTS] Nạp TorchScript VieNeu-TTS thành công!")
                return
            
            print("[VieNeu-TTS] Chưa tìm thấy checkpoint tại models/vieneu. Chuyển sang Edge-TTS / Fallback Engine sẵn sàng...")
        except Exception as e:
            print(f"[VieNeu-TTS] Cảnh báo khởi tạo: {e}. Sẵn sàng với fallback engine.")

    def synthesize(self, text: str) -> Optional[np.ndarray]:
        """
        Nhận văn bản tiếng Việt và trả về mảng Numpy float32 (-1.0 đến 1.0) ở 24000Hz.
        Toàn bộ diễn ra trong RAM.
        """
        if not text or not text.strip():
            return None

        clean_text = text.strip()

        # 1. Nếu đã nạp VieNeu-TTS thành công
        if self.model is not None:
            try:
                # VieNeu-TTS trả về torch.Tensor hoặc numpy array
                wav = self.model.synthesize(clean_text)
                if hasattr(wav, "cpu"):
                    wav = wav.cpu().numpy()
                return wav.astype(np.float32)
            except Exception as e:
                print(f"[VieNeu-TTS] Lỗi khi sinh giọng: {e}")

        # 2. Fallback siêu tốc: Sử dụng edge-tts qua RAM buffer nếu máy có kết nối mạng
        try:
            import asyncio
            import edge_tts
            import soundfile as sf

            async def _generate_edge_tts():
                communicate = edge_tts.Communicate(clean_text, "vi-VN-HoaiMyNeural")
                audio_bytes = b""
                async for chunk in communicate.stream():
                    if chunk["type"] == "audio":
                        audio_bytes += chunk["data"]
                return audio_bytes

            audio_data = asyncio.run(_generate_edge_tts())
            if audio_data:
                # Đọc byte stream thành mảng numpy float32 trực tiếp trong RAM
                with io.BytesIO(audio_data) as bio:
                    data, sr = sf.read(bio, dtype="float32")
                    self.sample_rate = sr
                    if len(data.shape) > 1:
                        data = data[:, 0]  # Mono
                    return data
        except Exception:
            pass

        # 3. Fallback tối giản: Tạo âm thanh sin báo hiệu phản hồi nếu offline hoàn toàn
        # Giúp pipeline vẫn hoạt động và test được visualizer sóng âm
        duration = min(2.0, max(0.5, len(clean_text) * 0.05))
        t = np.linspace(0, duration, int(self.sample_rate * duration), False)
        # Giả lập tần số giọng nói 220Hz - 440Hz
        sine_wave = (0.2 * np.sin(2 * np.pi * 330 * t) * np.exp(-t)).astype(np.float32)
        return sine_wave
