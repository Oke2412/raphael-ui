"""
Tensura Raphael - Audio & Voice Output Test Script
Kiểm tra âm thanh phát ra từ VieNeu-TTS (hoặc Fallback) và RVC (Raphael Voice Model).
Xuất file wav ra outputs/test_voice_output.wav và phát trực tiếp qua loa PC.
"""

import os
import sys
import time
import json
import argparse
import numpy as np
import soundfile as sf
import sounddevice as sd

# Đảm bảo mã hóa UTF-8 cho console Windows
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

# Thêm thư mục gốc vào path
PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if PROJECT_ROOT not in sys.path:
    sys.path.insert(0, PROJECT_ROOT)

from pipeline.vieneu_engine import VieNeuTTSEngine
from pipeline.rvc_engine import RVCEngine


def main():
    parser = argparse.ArgumentParser(description="Test VieNeu-TTS and RVC Voice Output")
    parser.add_argument(
        "--text",
        type=str,
        default="<< BÁO CÁO >> Khởi động thành công hệ thống trợ lý Raphael. Giọng nói và trạng thái âm thanh đã sẵn sàng.",
        help="Nội dung câu nói cần tổng hợp âm thanh",
    )
    parser.add_argument(
        "--no-play",
        action="store_true",
        help="Không phát âm thanh ra loa, chỉ lưu file wav",
    )
    parser.add_argument(
        "--output",
        type=str,
        default="outputs/test_voice_output.wav",
        help="Đường dẫn file wav lưu kết quả",
    )
    args = parser.parse_args()

    print("=" * 65)
    print("   🔮 TENSURA RAPHAEL — KIỂM THỬ ÂM THANH VIENEU & RVC")
    print("=" * 65)
    print(f"\n[*] Văn bản thử nghiệm:\n    \"{args.text}\"\n")

    # Đọc cấu hình
    config_path = os.path.join(PROJECT_ROOT, "config", "settings.json")
    with open(config_path, "r", encoding="utf-8") as f:
        config = json.load(f)

    # 1. Khởi tạo VieNeu-TTS Engine
    print("[1/3] Khởi tạo VieNeu-TTS Engine...")
    t0 = time.time()
    tts = VieNeuTTSEngine(config.get("vieneu", {}))
    print(f"      Sample rate: {tts.sample_rate}Hz")

    # 2. Khởi tạo RVC Engine
    print("\n[2/3] Khởi tạo RVC Engine (Mô hình Raphael)...")
    rvc = RVCEngine(config.get("rvc", {}))
    print(f"      RVC Model loaded: {'ĐÃ NẠP' if rvc.model_loaded else 'BYPASS (Chưa nạp)'}")
    if rvc.model_loaded:
        print(f"      Model file: {rvc.model_path}")
        print(f"      Target SR: {rvc.target_sr}Hz")

    # 3. Tiến hành tổng hợp âm thanh
    print("\n[3/3] Đang tổng hợp giọng nói từ văn bản...")
    synth_t0 = time.time()
    audio = tts.synthesize(args.text)
    synth_t1 = time.time()

    if audio is None or len(audio) == 0:
        print("[!] LỖI: Không thể sinh âm thanh từ văn bản.")
        return 1

    print(f"      Thời gian sinh TTS: {synth_t1 - synth_t0:.2f}s")
    print(f"      Số mẫu (Samples): {len(audio):,}")
    duration = len(audio) / tts.sample_rate
    print(f"      Độ dài âm thanh: {duration:.2f} giây")

    # 4. Chuyển đổi qua RVC
    rvc_t0 = time.time()
    final_audio = rvc.convert_voice(audio, source_sr=tts.sample_rate)
    rvc_t1 = time.time()
    print(f"      Thời gian xử lý RVC: {rvc_t1 - rvc_t0:.2f}s")

    # 5. Lưu file âm thanh .wav
    out_path = os.path.join(PROJECT_ROOT, args.output)
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    sf.write(out_path, final_audio, tts.sample_rate)
    print(f"\n[✓] Đã lưu file âm thanh tại: {out_path} ({os.path.getsize(out_path):,} bytes)")

    # 6. Phát trực tiếp qua loa PC
    if not args.no_play:
        print("\n[*] Đang phát âm thanh qua loa máy tính...")
        try:
            sd.play(final_audio, samplerate=tts.sample_rate)
            sd.wait()
            print("[✓] Đã phát xong!")
        except Exception as e:
            print(f"[!] Lỗi khi phát qua loa: {e}")

    print("\n" + "=" * 65)
    print("   HOÀN TẤT KIỂM THỬ ÂM THANH!")
    print("=" * 65)
    return 0


if __name__ == "__main__":
    sys.exit(main())
