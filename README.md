# 🔮 Tensura (Raphael) - Real-time Voice AI Companion

Hệ thống **Trợ lý ảo AI thời gian thực** phong cách **Đại Hiền Triết / Raphael (Tensura - Tensei Shitara Slime Datta Ken)**.  
Được thiết kế, xây dựng và tối ưu hóa chuyên biệt cho cấu hình phần cứng **NVIDIA GeForce RTX 2050 (4GB VRAM)** trên hệ điều hành **Windows**.

---

## 📌 Bảng Tóm Tắt Tiến Độ Dự Án (Project Milestones)

| Hạng mục | Trạng thái | Công nghệ / Thành phần | Chi tiết hoàn thành |
| :--- | :---: | :--- | :--- |
| **Wake Word & Biometrics** | ✅ Hoàn thành | openWakeWord + FFT Biometrics | Khẩu lệnh "Raphael" / "Hey Jarvis", xác thực vân giọng chính chủ chống gọi trộm |
| **VAD & STT Engine** | ✅ Hoàn thành | Silero VAD + Faster-Whisper CUDA | Nhận diện dứt câu tự động, STT tiếng Việt model `base` chạy `int8_float16` trên GPU (~450MB VRAM) |
| **Backend LLM Integration** | ✅ Hoàn thành | FastAPI / Docker (`sttap`) / Tailscale | Kết nối backend local Docker trên cổng 8000 hoặc remote LLM, truyền luồng token (Streaming) |
| **Voice Synthesis (TTS)** | ✅ Hoàn thành | VieNeu-TTS v3 Turbo + Edge-TTS | VieNeu-TTS v3 Turbo 48kHz (PyTorch CUDA Graph) & Edge-TTS Hoài My (<200ms), hỗ trợ Instant Voice Cloning |
| **Voice Conversion (RVC)** | ✅ Hoàn thành | RVC v2 + Faiss Index | Chuyển đổi chất giọng Raphael (`Raphael_Sensei.pth` 40kHz) trực tiếp trong RAM/VRAM |
| **Knowledge Cosmos Graph** | ✅ Hoàn thành | Neo4j + GraphRAG | Trực quan hóa chòm sao tri thức vũ trụ 3D/2D tương tác sau Orb Raphael |
| **WebSocket Core Server** | ✅ Hoàn thành | Python WebSocket (`ws://127.0.0.1:8765`) | Truyền dữ liệu trạng thái, transcript, streaming text và 32-band FFT audio levels thời gian thực |
| **Desktop Companion UI** | ✅ Hoàn thành | Electron & Tauri v2 | Chạy ngầm tàng hình (Stealth Daemon), khay hệ thống, 3 chế độ (Orb, Sidedeck, Fullscreen Overlay) |

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
| **TỔNG CỘNG** | — | **~2.5 GB / 4.0 GB** | Vận hành mát mẻ, an toàn, không lo tràn bộ nhớ |

---

## 2. Cấu Trúc Nhánh Git (Branching Strategy)

Kho mã nguồn được tổ chức thành 2 nhánh chính phục vụ cho từng mục đích:

* **`main` (Nhánh cốt lõi)**:
  * Chứa toàn bộ **Audio Pipeline & AI Backend** (Python, CUDA, Whisper, VieNeu, RVC, Neo4j, WebSocket).
  * Bộ công cụ kiểm thử âm thanh và backend Docker (`scripts/`).
  * Ứng dụng Desktop Companion phiên bản **Electron** (`nang-cap-visual-great-sage/`).
* **`feat/tauri-desktop` (Nhánh tối ưu hóa Desktop Companion)**:
  * Nhánh di chuyển (migration) Desktop Companion sang **Tauri v2 + Rust + React** (`tauri-desktop/`).
  * Giảm mức tiêu thụ RAM từ ~250MB (Electron) xuống **<30MB (Tauri)**.
  * Hỗ trợ native Windows tray, quản lý cửa sổ không viền mượt mà và không lo lỗi crash click-through.

---

## 3. Hướng Dẫn Cài Đặt & Thiết Lập Nhanh

### Bước 1: Chuẩn bị Môi trường Python
```powershell
# Kích hoạt môi trường ảo (đã tạo bằng uv)
.\.venv\Scripts\activate

# Cài đặt các gói phụ thuộc
uv pip install -r requirements.txt
```

### Bước 2: Cấu hình Mô hình RVC của Raphael
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

## 4. Các Kịch Bản Kiểm Thử Sẵn Có

Thư mục `scripts/` cung cấp các công cụ kiểm thử chuyên biệt:

1. **Kiểm thử âm thanh (VieNeu-TTS + RVC)**:
   * Chạy file: `scripts\test_voice.bat` (hoặc `scripts/test_voice_output.py`).
   * Tự động tổng hợp câu nói mẫu, chuyển giọng qua RVC và phát trực tiếp ra loa máy tính.
2. **Kiểm thử kết nối Backend Docker**:
   * Khởi động Docker backend trong thư mục `sttap` (`docker compose up -d` hoặc `run.bat`).
   * Chạy file: `scripts\test_backend.bat` để ping và thử nghiệm stream chat AI.
3. **Kiểm tra thiết bị âm thanh**:
   * Chạy `.\.venv\Scripts\python.exe scripts/test_audio_devices.py` để xem ID Micro và Loa.

---

## 5. Khởi Động Trợ Lý Ảo

* **Chạy ngầm hoàn toàn (Stealth Mode)**:
  * Chạy file `run_silent.vbs` để khởi động trợ lý ngầm trong khay hệ thống, không hiện cửa sổ console đen.
* **Dừng trợ lý**:
  * Chạy file `stop_assistant.bat` để ngắt an toàn toàn bộ tiến trình.
* **Phím tắt toàn cục**:
  * Bấm tổ hợp `Ctrl + Alt + Shift + \` để bật/tắt nhanh trợ lý bất cứ lúc nào.
