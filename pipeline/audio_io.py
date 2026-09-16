"""
Audio Input / Output Engine
Xử lý thu âm từ Microphone và phát âm thanh ra Loa ngoài hoàn toàn trong bộ nhớ RAM (In-Memory Buffer).
Tính toán RMS và phổ tần số FFT 32 dải tần theo thời gian thực để gửi cho UI Visualizer.
"""

import queue
import threading
import time
import numpy as np
import sounddevice as sd
from typing import Callable, Optional, List


def calculate_audio_levels(audio_chunk: np.ndarray, num_bands: int = 32) -> dict:
    """
    Tính toán RMS, Peak và FFT spectrum (từ 0.0 đến 1.0) từ một đoạn audio buffer.
    audio_chunk: float32 mảng 1D.
    """
    if len(audio_chunk) == 0:
        return {"rms": 0.0, "peak": 0.0, "spectrum": [0.0] * num_bands}

    # Đảm bảo mảng float32
    samples = audio_chunk.astype(np.float32)
    peak = float(np.max(np.abs(samples)))
    rms = float(np.sqrt(np.mean(samples ** 2)))

    # Giới hạn RMS và Peak trong khoảng 0.0 - 1.0
    peak = min(1.0, peak)
    rms = min(1.0, rms * 2.5)  # Scale nhẹ cho visualizer nhạy hơn

    # Tính Fast Fourier Transform (FFT) cho phổ âm thanh
    try:
        n_fft = 512
        if len(samples) >= n_fft:
            windowed = samples[:n_fft] * np.hanning(n_fft)
            fft_data = np.abs(np.fft.rfft(windowed))
            # Chia thành num_bands dải tần số
            band_size = len(fft_data) // num_bands
            spectrum: List[float] = []
            for i in range(num_bands):
                start = i * band_size
                end = start + band_size
                val = float(np.mean(fft_data[start:end])) if end > start else 0.0
                # Chuẩn hóa log scale
                normalized = min(1.0, np.log1p(val * 10) / 3.0)
                spectrum.append(round(normalized, 3))
        else:
            spectrum = [round(rms * 0.8, 3)] * num_bands
    except Exception:
        spectrum = [round(rms, 3)] * num_bands

    return {
        "rms": round(rms, 3),
        "peak": round(peak, 3),
        "spectrum": spectrum
    }


class AudioInputRecorder:
    """
    Thu âm Microphone qua sounddevice callback không nghẽn luồng.
    """
    def __init__(
        self,
        sample_rate: int = 16000,
        channels: int = 1,
        device_index: Optional[int] = None,
        chunk_duration_ms: int = 30,
        level_callback: Optional[Callable[[dict], None]] = None
    ):
        self.sample_rate = sample_rate
        self.channels = channels
        self.device_index = device_index
        self.chunk_size = int(sample_rate * (chunk_duration_ms / 1000.0))
        self.level_callback = level_callback
        
        self.audio_queue: queue.Queue = queue.Queue()
        self.stream: Optional[sd.InputStream] = None
        self.is_recording = False
        self._lock = threading.Lock()

    def _audio_callback(self, indata, frames, time_info, status):
        if status and status.input_overflow:
            pass
        data = indata[:, 0].copy()  # Lấy kênh đơn mono
        self.audio_queue.put(data)

        if self.level_callback:
            levels = calculate_audio_levels(data)
            levels["source"] = "mic"
            self.level_callback(levels)

    def start(self):
        with self._lock:
            if self.is_recording:
                return
            self.audio_queue = queue.Queue()
            self.stream = sd.InputStream(
                samplerate=self.sample_rate,
                channels=self.channels,
                device=self.device_index,
                blocksize=self.chunk_size,
                dtype="float32",
                callback=self._audio_callback
            )
            self.stream.start()
            self.is_recording = True

    def stop(self):
        with self._lock:
            if not self.is_recording:
                return
            self.is_recording = False
            if self.stream:
                self.stream.stop()
                self.stream.close()
                self.stream = None

    def read_chunk(self, timeout: float = 0.1) -> Optional[np.ndarray]:
        try:
            return self.audio_queue.get(timeout=timeout)
        except queue.Empty:
            return None

    def clear(self):
        while not self.audio_queue.empty():
            try:
                self.audio_queue.get_nowait()
            except queue.Empty:
                break


class AudioOutputPlayer:
    """
    Phát âm thanh ra loa ngoài trực tiếp từ mảng numpy (In-Memory Buffer).
    Hỗ trợ ngắt lời ngay lập tức (Interrupt) và gửi dữ liệu sóng âm cho UI khi đang phát.
    """
    def __init__(
        self,
        sample_rate: int = 44100,
        channels: int = 1,
        device_index: Optional[int] = None,
        level_callback: Optional[Callable[[dict], None]] = None
    ):
        self.sample_rate = sample_rate
        self.channels = channels
        self.device_index = device_index
        self.level_callback = level_callback
        
        self.playback_queue: queue.Queue = queue.Queue()
        self.is_playing = False
        self._stop_requested = threading.Event()
        self._play_thread: Optional[threading.Thread] = None

    def play_numpy(self, audio_data: np.ndarray, sample_rate: Optional[int] = None):
        """
        Đưa mảng numpy audio vào hàng đợi phát âm thanh.
        """
        sr = sample_rate or self.sample_rate
        self.playback_queue.put((audio_data, sr))
        if not self.is_playing:
            self._start_worker()

    def _start_worker(self):
        self._stop_requested.clear()
        self.is_playing = True
        self._play_thread = threading.Thread(target=self._worker_loop, daemon=True)
        self._play_thread.start()

    def _worker_loop(self):
        while not self._stop_requested.is_set():
            try:
                audio_data, sr = self.playback_queue.get(timeout=0.2)
            except queue.Empty:
                if self.playback_queue.empty():
                    break
                continue

            # Đảm bảo audio_data là float32 1D
            if audio_data.dtype != np.float32:
                audio_data = audio_data.astype(np.float32)
                if np.max(np.abs(audio_data)) > 1.0:
                    audio_data = audio_data / 32768.0

            # Phát chunk theo khối nhỏ để vừa phát vừa tính FFT sóng âm
            chunk_size = int(sr * 0.05)  # 50ms mỗi chunk
            total_samples = len(audio_data)
            
            with sd.OutputStream(samplerate=sr, channels=1, device=self.device_index, dtype="float32") as stream:
                for i in range(0, total_samples, chunk_size):
                    if self._stop_requested.is_set():
                        break
                    chunk = audio_data[i:i + chunk_size]
                    if len(chunk) < chunk_size:
                        chunk = np.pad(chunk, (0, chunk_size - len(chunk)))
                    
                    stream.write(chunk)
                    
                    if self.level_callback:
                        levels = calculate_audio_levels(chunk)
                        levels["source"] = "speaker"
                        self.level_callback(levels)

            self.playback_queue.task_done()

        self.is_playing = False

    def stop(self):
        """Dừng phát âm thanh ngay lập tức (Interrupt)."""
        self._stop_requested.set()
        # Xóa hàng đợi phát
        while not self.playback_queue.empty():
            try:
                self.playback_queue.get_nowait()
                self.playback_queue.task_done()
            except Exception:
                break
        self.is_playing = False
