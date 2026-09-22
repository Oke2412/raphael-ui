"""
Tensura Raphael - Voice Biometrics Enrollment (Ghi danh vân giọng chính chủ)
Thu âm mẫu giọng nói của bạn trong 5 giây và trích xuất vector đặc trưng vân giọng
lưu vào file 'models/voiceprint.npy'.
Sau khi ghi danh, Raphael chỉ thức dậy khi nhận diện ĐÚNG giọng nói của bạn!
"""

import os
import sys
import time
import numpy as np
import sounddevice as sd

SAMPLE_RATE = 16000
DURATION_SECONDS = 5.0
OUTPUT_FILE = os.path.join(os.path.dirname(__file__), "..", "models", "voiceprint.npy")


def main():
    print("=" * 65)
    print("  🔮 TENSURA RAPHAEL - GHI DANH VÂN GIỌNG CHÍNH CHỦ (VOICE BIOMETRICS)")
    print("=" * 65)
    print("  Quy trình:")
    print("  1. Chuẩn bị nói liên tục trong 5 giây sau tiếng đếm ngược.")
    print("  2. Khẩu lệnh gợi ý khi thu âm: 'Raphael, ta là chủ nhân của ngươi.'")
    print("  3. Vector đặc trưng âm sắc sẽ được lưu vào: models/voiceprint.npy")
    print("=" * 65)

    input("\nNhấn [ENTER] khi bạn đã sẵn sàng bắt đầu đếm ngược...")

    for i in range(3, 0, -1):
        print(f"Chuẩn bị nói sau: {i}...")
        time.sleep(1)

    print("\n[🎙️ ĐANG THU ÂM...] Hãy nói vào micro ngay bây giờ:")
    total_frames = int(DURATION_SECONDS * SAMPLE_RATE)
    
    try:
        recording = sd.rec(total_frames, samplerate=SAMPLE_RATE, channels=1, dtype='float32')
        # Hiển thị thanh tiến trình
        for _ in range(int(DURATION_SECONDS * 10)):
            sys.stdout.write("█")
            sys.stdout.flush()
            time.sleep(0.1)
        sd.wait()
        print("\n[✓] Thu âm hoàn tất!")
    except Exception as e:
        print(f"\n[!] Lỗi thiết bị micro: {e}")
        return

    audio = recording.flatten()
    # Kiểm tra mức năng lượng âm thanh
    rms = np.sqrt(np.mean(audio**2))
    if rms < 0.01:
        print("\n[!] Âm lượng thu được quá nhỏ (RMS < 0.01). Vui lòng kiểm tra lại micro và thử lại.")
        return

    print(f"[*] Phân tích đặc trưng âm sắc (Mức tín hiệu RMS: {rms:.4f})...")
    # Trích xuất phổ năng lượng âm sắc
    fft_data = np.abs(np.fft.rfft(audio[:16000]))
    norm = np.linalg.norm(fft_data)
    if norm > 0:
        fft_data = fft_data / norm

    os.makedirs(os.path.dirname(OUTPUT_FILE), exist_ok=True)
    np.save(OUTPUT_FILE, fft_data)

    print("=" * 65)
    print(f"  🎉 GHI DANH THÀNH CÔNG VÂN GIỌNG CHÍNH CHỦ!")
    print(f"  File mẫu đã được lưu tại: {os.path.abspath(OUTPUT_FILE)}")
    print("  Từ nay, Raphael chỉ kích hoạt khi phát hiện đúng giọng nói của bạn!")
    print("=" * 65)


if __name__ == "__main__":
    main()
