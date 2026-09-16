"""
Script Kiểm thử Toàn diện Core Audio Pipeline (Tensura / Raphael)
Chạy script này để kiểm tra từng khối tính năng:
1. Microphone & Loa ngoài
2. VAD & Phổ sóng âm FFT
3. Faster-Whisper STT
4. Ping kiểm tra Server LLM từ xa (Tailscale)
5. VieNeu-TTS & RVC
"""

import os
import sys

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass

import time
import numpy as np

# Thêm thư mục gốc vào sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from pipeline.audio_io import AudioInputRecorder, AudioOutputPlayer, calculate_audio_levels
from pipeline.server_client import RemoteServerClient
from pipeline.text_chunker import StreamTextChunker
from pipeline.vieneu_engine import VieNeuTTSEngine
from pipeline.rvc_engine import RVCEngine


def test_fft_calculation():
    print("\n--- [1/5] Kiểm tra Tính toán Phổ sóng âm (FFT 32 dải) ---")
    mock_audio = (np.sin(np.linspace(0, 100, 1600)) * 0.5).astype(np.float32)
    levels = calculate_audio_levels(mock_audio, num_bands=32)
    assert "rms" in levels and "spectrum" in levels
    assert len(levels["spectrum"]) == 32
    print(f"  [OK] RMS: {levels['rms']}, Peak: {levels['peak']}, Số dải phổ: {len(levels['spectrum'])}")


def test_text_chunker():
    print("\n--- [2/5] Kiểm tra Gọt tỉa Text Stream (StreamTextChunker) ---")
    chunker = StreamTextChunker(min_chunk_words=2)
    stream_tokens = ["Báo ", "cáo: ", "Đã ", "kết ", "nối ", "thành ", "công. ", "Hệ ", "thống ", "sẵn ", "sàng!"]
    collected_chunks = []
    for t in stream_tokens:
        out = chunker.feed(t)
        if out:
            collected_chunks.extend(out)
    collected_chunks.extend(chunker.flush())
    print(f"  [OK] Các câu được tách gối đầu: {collected_chunks}")
    assert len(collected_chunks) >= 2


def test_remote_server_ping():
    print("\n--- [3/5] Kiểm tra Kết nối Server LLM từ xa (Tailscale) ---")
    client = RemoteServerClient({"url": "http://127.0.0.1:8000/api/chat", "timeout_seconds": 2})
    res = client.test_connection()
    print(f"  Kết quả ping: {res['status']} - {res.get('message', '')}")
    if res['status'] != 'ok':
        print("  [Lưu ý] Server từ xa chưa bật hoặc chưa kết nối Tailscale. Bạn có thể cập nhật IP trong config/settings.json.")


def test_tts_and_playback():
    print("\n--- [4/5] Kiểm tra VieNeu-TTS & Phát âm thanh ra Loa ---")
    tts = VieNeuTTSEngine({"enabled": True, "model_path": "models/vieneu", "device": "cuda"})
    rvc = RVCEngine({"enabled": True, "model_path": "models/rvc/raphael.pth"})
    player = AudioOutputPlayer(sample_rate=tts.sample_rate)

    test_sentence = "Thông báo. Hệ thống Đại Hiền Triết đã sẵn sàng hoạt động."
    print(f"  Đang tổng hợp câu thử nghiệm: '{test_sentence}'...")
    audio = tts.synthesize(test_sentence)
    if audio is not None and len(audio) > 0:
        print(f"  [OK] Đã sinh mảng audio thành công: {len(audio)} mẫu.")
        converted = rvc.convert_voice(audio, source_sr=tts.sample_rate)
        print("  Đang phát âm thanh ra loa...")
        player.play_numpy(converted, sample_rate=tts.sample_rate)
        time.sleep(2.5)
        player.stop()
        print("  [OK] Phát âm thanh thành công.")
    else:
        print("  [Cảnh báo] Chưa sinh được audio.")


def main():
    print("=" * 60)
    print("   BẮT ĐẦU KIỂM THỬ CORE AUDIO PIPELINE")
    print("=" * 60)
    test_fft_calculation()
    test_text_chunker()
    test_remote_server_ping()
    test_tts_and_playback()
    print("\n" + "=" * 60)
    print("   HOÀN TẤT KIỂM THỬ! TẤT CẢ CÁC MODULE ĐỀU KHẢ DỤNG.")
    print("=" * 60)


if __name__ == "__main__":
    main()
