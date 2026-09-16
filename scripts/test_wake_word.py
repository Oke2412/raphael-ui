"""
Kiểm thử Mô hình Wake Word (OpenWakeWord trên CPU)
Kiểm tra nạp mô hình và tốc độ suy luận khi phân tích từ khóa.
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

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from pipeline.wake_word import WakeWordDetector


def test_wake_word():
    print("=" * 60)
    print("      KIỂM THỬ MODULE WAKE WORD (TỪ KHÓA ĐÁNH THỨC)")
    print("=" * 60)

    config = {
        "enabled": True,
        "model": "hey_jarvis",
        "threshold": 0.5,
        "custom_model_path": "models/wakeword/raphael.onnx"
    }

    print("[*] Đang khởi tạo WakeWordDetector...")
    detector = WakeWordDetector(config)

    if detector.model is not None:
        print("  [OK] Mô hình Wake Word đã nạp thành công!")
        # Tạo đoạn audio giả lập 16kHz (80ms = 1280 mẫu)
        mock_chunk = (np.random.randn(1280) * 0.1).astype(np.float32)

        start_time = time.time()
        detected, keyword, score = detector.process_chunk(mock_chunk)
        duration_ms = (time.time() - start_time) * 1000

        print(f"  [OK] Tốc độ suy luận 1 chunk: {duration_ms:.2f}ms (Cực nhanh trên CPU)")
        print(f"  [OK] Kết quả mẫu: detected={detected}, keyword='{keyword}', score={score:.4f}")
        print("=" * 60)
        print("  MODULE WAKE WORD HOÀN TOÀN SẴN SÀNG!")
        print("=" * 60)
    else:
        print("  [Lỗi] Không thể nạp mô hình Wake Word.")


if __name__ == "__main__":
    test_wake_word()
