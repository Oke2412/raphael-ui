"""
Kiểm tra thiết bị âm thanh (Microphone & Loa) trên Windows.
Chạy script này để xem danh sách thiết bị và Device Index tương ứng để điền vào config/settings.json nếu muốn đổi mic/loa.
"""

import sys
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass
import sounddevice as sd


def list_audio_devices():
    print("=" * 60)
    print("        DANH SÁCH THIẾT BỊ ÂM THANH TRÊN HỆ THỐNG")
    print("=" * 60)
    devices = sd.query_devices()
    default_input = sd.default.device[0]
    default_output = sd.default.device[1]

    print("\n[🎙️ MICROPHONE / INPUT DEVICES]:")
    for i, dev in enumerate(devices):
        if dev['max_input_channels'] > 0:
            is_default = " (MẶC ĐỊNH)" if i == default_input else ""
            print(f"  [{i}] {dev['name']}{is_default} - {dev['default_samplerate']}Hz")

    print("\n[🔊 LOA / TAI NGHE / OUTPUT DEVICES]:")
    for i, dev in enumerate(devices):
        if dev['max_output_channels'] > 0:
            is_default = " (MẶC ĐỊNH)" if i == default_output else ""
            print(f"  [{i}] {dev['name']}{is_default} - {dev['default_samplerate']}Hz")

    print("\n" + "=" * 60)
    print("Mẹo: Nếu muốn dùng một thiết bị cụ thể, hãy copy số [Index] ở trên")
    print("và điền vào 'mic_device_index' hoặc 'speaker_device_index' trong config/settings.json.")
    print("=" * 60)


if __name__ == "__main__":
    list_audio_devices()
