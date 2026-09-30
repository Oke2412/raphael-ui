# Thư mục Voice Cloning (VieNeu-TTS v3 Turbo)

Thư mục này cho phép bạn thêm mẫu giọng bất kỳ để VieNeu-TTS tự động sao chép (Instant Voice Cloning) chỉ từ một đoạn âm thanh ngắn 3 - 8 giây!

## Cách sử dụng:
1. Đặt file ghi âm giọng nói mẫu (định dạng `.wav`, `.mp3` hoặc `.flac`, dài từ 3 đến 8 giây, âm thanh rõ ràng không ồn) vào thư mục này.
2. Đặt tên file theo tên giọng bạn muốn, ví dụ:
   - `ban_mai.wav` (Giọng Ban Mai)
   - `raphael_sample.wav`
3. Trong `config/settings.json`, đặt:
   ```json
   "vieneu": {
     "enabled": true,
     "voice": "ban_mai",
     "device": "cuda"
   }
   ```
4. Khi khởi chạy, VieNeu-TTS sẽ tự động phát hiện file `models/voices/ban_mai.wav` và nhân bản chính xác chất giọng đó!
