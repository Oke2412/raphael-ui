# 🔮 Tensura (Raphael) - Real-time Voice AI Companion

Hệ thống **Trợ lý ảo AI thời gian thực** phong cách **Đại Hiền Triết / Raphael (Tensura - Tensei Shitara Slime Datta Ken)**.  
Được thiết kế, xây dựng và tối ưu hóa chuyên biệt cho cấu hình phần cứng **NVIDIA GeForce RTX 2050 (4GB VRAM)** trên hệ điều hành **Windows**.

> ⚠️ **TÌNH TRẠNG DỰ ÁN**: Toàn bộ dự án đang trong giai đoạn **Nghiên cứu & Phát triển tích cực (Active Development / R&D - Alpha)**.  
> Các tính năng dưới đây đang được tích hợp, tinh chỉnh và kiểm thử liên tục; chưa có thành phần nào được đóng băng hay hoàn thiện cố định.

---

## 📌 Bảng Tiến Độ Phát Triển & Kiểm Thử (Current Status)

| Hạng mục | Trạng thái hiện tại | Công nghệ / Module | Tình trạng kiểm thử & Vấn đề tồn đọng |
| :--- | :---: | :--- | :--- |
| **Wake Word & Biometrics** | 🚧 Đang phát triển | openWakeWord + FFT Biometrics | Đã nạp model mẫu `hey_jarvis`; cần huấn luyện model riêng "Raphael" và kiểm thử độ nhạy mic thực tế |
| **VAD & STT Engine** | 🧪 Đang kiểm thử | Silero VAD + Faster-Whisper CUDA | Đã cấu hình Whisper `base` int8 trên GPU; cần đo đạc và kiểm thử độ trễ ngắt câu với người dùng thật |
| **Backend LLM Integration** | 🚧 Đang phát triển | FastAPI / Docker (`sttap`) / Tailscale | Đã tạo client kết nối cổng 8000; đang chờ khởi chạy container Docker `sttap` để test luồng chat stream |
| **Voice Synthesis (TTS)** | 🧪 Đang thử nghiệm | VieNeu-TTS v3 Turbo + Edge-TTS | Đã tích hợp VieNeu v3 Turbo nhưng phát âm chưa tự nhiên; đang thử nghiệm so sánh với Edge-TTS Hoài My |
| **Voice Conversion (RVC)** | 🧪 Đang kiểm thử | RVC v2 + Faiss Index | Đã nạp thành công `Raphael_Sensei.pth` và index trên CUDA; cần tinh chỉnh f0/pitch để giọng bớt méo |
| **Knowledge Cosmos Graph** | 🚧 Đang phát triển | Neo4j + GraphRAG | Đã dựng client kết nối và khung vẽ sao; cần nạp cơ sở tri thức thực tế của Tensura vào database |
| **WebSocket Core Server** | 🧪 Đang kiểm thử | Python WebSocket (`:8765`) | Đã mở server trao đổi dữ liệu; cần kiểm thử độ ổn định khi chạy ngầm liên tục nhiều giờ |
| **Desktop Companion UI** | 🚧 Đang phát triển | Tauri v2 + React (`tauri-desktop/`) | UI chính đã chuyển sang Tauri v2; bản Electron cũ (`nang-cap-visual-great-sage/`) được giữ tạm làm tham chiếu trong lúc hoàn thiện UI |

---

## 1. Kiến Trúc Luồng Âm Thanh & Phân Bổ 4GB VRAM

```text
[Microphone] ──> (Wake Word / Silero VAD / Hotkey) ──> (Faster-Whisper CUDA) ──> [LLM Backend / Docker :8000]
                                                                                                 │
[Loa ngoài] <── (RVC Real-time VRAM) <── (VieNeu-TTS / Edge-TTS RAM) <───────────────────────────┘
```

| Thành phần | Vị trí thực thi | Mức chiếm dụng VRAM | Ghi chú kỹ thuật |
| :--- | :--- | :--- | :--- |
| **Wake Word (OpenWakeWord)** | CPU | 0 MB VRAM | Luôn nghe ngầm, ~0% GPU, <25MB RAM |
| **Silero VAD** | CPU | 0 MB VRAM | Nhận biết dứt câu, ~0.5% CPU |
| **Faster-Whisper** | GPU (CUDA) | ~450 MB VRAM | Model `base`, `int8_float16` siêu tốc |
| **TTS Engine (VieNeu / Edge)** | GPU / RAM | ~600 - 800 MB VRAM | Sinh âm thanh tiếng Việt vào RAM buffer |
| **RVC Real-time** | GPU (CUDA) | ~1.2 - 1.4 GB VRAM | Biến đổi chất giọng Raphael |
| **LLM Server (Docker / Remote)** | CPU / Docker / Cloud | 0 MB VRAM cục bộ | Giao tiếp qua `http://127.0.0.1:8000` hoặc Tailscale |
| **TỔNG CỘNG** | — | **~2.5 GB / 4.0 GB** | Vận hành an toàn trong giới hạn 4GB VRAM của RTX 2050 |

---

## 2. Cấu Trúc Mã Nguồn

Repo dùng một nhánh chính `main`; mọi thay đổi đi qua nhánh chủ đề ngắn hạn (`chore/`, `fix/`, `feat/`) rồi merge bằng pull request.

* **Backend & Audio Pipeline** (Python): `main.py`, `server/`, `pipeline/`, `config/`, `scripts/`.
* **Desktop Companion chính** (Tauri v2 + Rust + React): `tauri-desktop/`.
  * UI React nằm trong `src/`, backend native Windows (DWM, click-through, global shortcut) nằm trong `src-tauri/`.
  * Mục tiêu: giảm RAM từ ~250MB (Electron) xuống **<30MB**.
* **Legacy Electron** (`nang-cap-visual-great-sage/`): bản Next.js + Electron cũ, **giữ tạm để tham chiếu UI** cho tới khi quá trình chuyển giao hoàn tất. Không dùng cho bản phát hành mới.
* **Dữ liệu nặng không đưa lên git**: `.venv/`, `models/`, `extracted_greatsage/`, `outputs/`, `logs/`, `node_modules/`, `dist/`, `.next/`, `src-tauri/target/`. Xem `.gitignore`.

> Repo được phát triển trên Windows; `.gitattributes` chuẩn hoá EOL (`*.bat`, `*.vbs` = CRLF; mã nguồn = LF) để checkout trên Linux/macOS không làm hỏng script.

---

## 3. Hướng Dẫn Cài Đặt Môi Trường

### Bước 1: Chuẩn bị Môi trường Python
```powershell
# Kích hoạt môi trường ảo (đã tạo bằng uv)
.\.venv\Scripts\activate

# Cài đặt các gói phụ thuộc
uv pip install -r requirements.txt
```

### Bước 2: Đặt File Mô hình RVC của Raphael
Đảm bảo 2 file model đã nằm trong thư mục `models/rvc/`:
* `models/rvc/Raphael_Sensei.pth`
* `models/rvc/added_IVF160_Flat_nprobe_1_Raphael_Sensei_v2.index`

### Bước 3: Cấu hình Tệp `config/settings.json`
```json
{
  "server": {
    "url": "http://127.0.0.1:8000/api/chat/message",
    "api_type": "generic_stream"
  },
  "vieneu": {
    "enabled": true,
    "voice": "Ban Mai",
    "device": "cuda"
  },
  "rvc": {
    "enabled": true,
    "model_path": "models/rvc/Raphael_Sensei.pth",
    "index_path": "models/rvc/added_IVF160_Flat_nprobe_1_Raphael_Sensei_v2.index"
  }
}
```

---

## 4. Các Công Cụ & Kịch Bản Kiểm Thử

Dự án cung cấp sẵn các script để hỗ trợ việc kiểm thử từng thành phần riêng lẻ:

1. **Kiểm thử âm thanh (TTS + RVC)**:
   * Chạy file: `scripts\test_voice.bat` (hoặc `scripts/test_voice_output.py`).
   * Tổng hợp văn bản mẫu, chạy qua RVC và xuất file kiểm tra tại `outputs/test_voice_output.wav`.
2. **Kiểm thử kết nối Backend Docker**:
   * Khởi động Docker backend trong thư mục `sttap` (`docker compose up -d` hoặc `run.bat`).
   * Chạy file: `scripts\test_backend.bat` để ping kiểm tra kết nối và thử luồng phản hồi chat stream.
3. **Kiểm tra thiết bị âm thanh phần cứng**:
   * Chạy `.\.venv\Scripts\python.exe scripts/test_audio_devices.py` để xem ID Micro và Loa thực tế.

---

## 5. Khởi Động & Điều Khiển Trợ Lý

* **Khởi động chạy ngầm (Stealth Mode)**: Chạy file `run_silent.vbs`.
* **Dừng trợ lý an toàn**: Chạy file `stop_assistant.bat`.
* **Phím tắt gọi trợ lý toàn cục**: `Ctrl + Alt + Shift + \`.
