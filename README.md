# 🔮 Tensura (Raphael) - Real-time Local Audio Pipeline

Hệ thống **Core Audio Pipeline** xử lý giọng nói thời gian thực cho Trợ lý ảo AI phong cách **Đại Hiền Triết / Raphael (Tensura)**.
Được thiết kế và tối ưu hóa chuyên biệt cho cấu hình **NVIDIA GeForce RTX 2050 (4GB VRAM)** trên hệ điều hành Windows.

> 📢 **Dành cho Agent chuyên trách UI/UX**:
> Phần Backend này đóng vai trò máy chủ xử lý âm thanh ngầm và cung cấp kênh **WebSocket (`ws://127.0.0.1:8765`)**.
> Hãy đọc tài liệu đặc tả chi tiết tại: **[FRONTEND_CONTRACT.md](./FRONTEND_CONTRACT.md)** để kết nối các sự kiện, trạng thái, và lấy dữ liệu sóng âm thời gian thực vẽ vòng tròn ma pháp Raphael.

---

## 1. Kiến Trúc Luồng Âm Thanh & Ngân Sách 4GB VRAM

```text
[Microphone] ──> (Wake Word / Silero VAD / Hotkey) ──> (Faster-Whisper CUDA) ──> [Remote Server / Tailscale]
                                                                                               │
[Loa ngoài] <── (RVC Real-time VRAM) <── (VieNeu-TTS Stream RAM) <─────────────────────────────┘
```

| Thành phần | Vị trí thực thi | Mức chiếm dụng VRAM | Ghi chú |
| :--- | :--- | :--- | :--- |
| **Wake Word (OpenWakeWord)** | CPU | 0 MB VRAM | Luôn nghe ngầm, ~0% GPU, <25MB RAM |
| **Silero VAD** | CPU | 0 MB VRAM | Nhận biết dứt câu, ~0.5% CPU |
| **Faster-Whisper** | GPU (CUDA) | ~450 MB VRAM | Model `base`, `int8_float16` siêu tốc |
| **VieNeu-TTS** | RAM / CPU / GPU | ~600 - 800 MB VRAM | Sinh âm thanh tiếng Việt vào RAM buffer |
| **RVC Real-time** | GPU (CUDA) | ~1.2 - 1.4 GB VRAM | Biến đổi chất giọng Raphael |
| **LLM Server** | Máy chủ từ xa | 0 MB VRAM cục bộ | Giao tiếp qua mạng Tailscale |
| **TỔNG CỘNG** | — | **~2.5 GB / 4.0 GB** | An toàn, không lo tràn bộ nhớ VRAM |

---

## 2. Hướng Dẫn Thiết Lập Nhanh

### Bước 1: Chuẩn bị Thư viện Python
Máy đã có sẵn công cụ `uv`. Chỉ cần chạy lệnh sau để cài đặt toàn bộ thư viện tối ưu CUDA:
```powershell
# Kích hoạt môi trường ảo
.\.venv\Scripts\activate

# Cài đặt các gói phụ thuộc
uv pip install -r requirements.txt
```

### Bước 2: Đặt File Model RVC của Raphael
Hãy copy 2 file giọng nhân vật của bạn vào thư mục:
* `models/rvc/raphael.pth`
* `models/rvc/raphael.index` (nếu có)

*(Nếu file của bạn có tên khác, hãy mở `config/settings.json` và cập nhật đường dẫn tương ứng).*

### Bước 3: Cấu hình Địa chỉ Server LLM (Tailscale)
Mở file `config/settings.json` và sửa địa chỉ Server từ xa của bạn:
```json
"server": {
  "url": "http://100.x.y.z:8000/api/chat"
}
```

---

## 3. Khởi Động & Kiểm Thử

### 1. Khởi động Audio Pipeline Backend:
Nhấp đúp chuột vào file:
```cmd
run_pipeline.bat
```
*(Hệ thống sẽ nạp model, đăng ký phím tắt `Ctrl + Space` và mở WebSocket `ws://127.0.0.1:8765`).*

### 2. Khởi động Giao diện Frontend (Next.js Raphael UI):
Nhấp đúp chuột vào file:
```cmd
run_frontend.bat
```
Truy cập trình duyệt tại: `http://localhost:3000` để trải nghiệm 3 chế độ: **Overlay Trong suốt**, **Mini Orb góc màn hình**, và **Gemini Sidedeck Panel**.

### 3. Kiểm tra thiết bị Micro và Loa:
```powershell
.\.venv\Scripts\python.exe scripts/test_audio_devices.py
```

### 4. Kiểm thử toàn bộ các khối tính năng:
```powershell
.\.venv\Scripts\python.exe scripts/test_pipeline.py
```

---

## 4. Tệp Cấu Hình (`config/settings.json`)

* `hotkey`: Phím tắt toàn cục để bật/tắt mic (mặc định: `ctrl+space`).
* `output_mode`: Chế độ xuất:
  * `"voice_and_text"`: Cả giọng nói và văn bản.
  * `"text_only"`: Chỉ xuất chữ lên UI.
  * `"voice_only"`: Chỉ phát giọng nói ra loa.
* `vad`: Ngưỡng phát hiện tiếng nói và thời gian dứt câu.
* `rvc`: Điều chỉnh cao độ giọng (`f0_up_key`: 0 hoặc 12), thuật toán F0 (`pm`, `harvest`, `rmvpe`).
