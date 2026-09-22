"""
Tensura Real-time AI Assistant - Core Audio Pipeline Entrypoint
Khởi động Core Pipeline và WebSocket Server kết nối Frontend.
"""

import sys

import os

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
LOGS_DIR = os.path.join(BASE_DIR, "logs")
os.makedirs(LOGS_DIR, exist_ok=True)
LOG_FILE = os.path.join(LOGS_DIR, "pipeline.log")

os.environ["PYTHONIOENCODING"] = "utf-8"

class TeeLogger:
    def __init__(self, file_path, stream):
        self.file = open(file_path, "a", encoding="utf-8", buffering=1)
        self.stream = stream

    def write(self, data):
        try:
            self.file.write(data)
        except Exception:
            pass
        if self.stream:
            try:
                self.stream.write(data)
            except Exception:
                pass

    def flush(self):
        try:
            self.file.flush()
        except Exception:
            pass
        if self.stream and hasattr(self.stream, "flush"):
            try:
                self.stream.flush()
            except Exception:
                pass

if sys.platform == "win32":
    try:
        if sys.stdout and hasattr(sys.stdout, "reconfigure"):
            sys.stdout.reconfigure(encoding="utf-8", errors="replace", line_buffering=True)
        if sys.stderr and hasattr(sys.stderr, "reconfigure"):
            sys.stderr.reconfigure(encoding="utf-8", errors="replace", line_buffering=True)
    except Exception:
        pass

sys.stdout = TeeLogger(LOG_FILE, sys.stdout)
sys.stderr = TeeLogger(LOG_FILE, sys.stderr)

import asyncio
from pipeline.manager import PipelineManager
from server.ws_server import TensuraWebSocketServer


def main():
    print("=" * 65)
    print("   🔮 TENSURA (RAPHAEL) REAL-TIME AUDIO PIPELINE BACKEND")
    print("   Tối ưu hóa phần cứng NVIDIA GeForce RTX 2050 (4GB VRAM)")
    print("=" * 65)

    manager = PipelineManager("config/settings.json")
    server = TensuraWebSocketServer(
        manager,
        host=manager.config.get("system", {}).get("ws_host", "127.0.0.1"),
        port=manager.config.get("system", {}).get("ws_port", 8765)
    )

    try:
        asyncio.run(server.run_server())
    except KeyboardInterrupt:
        print("\n[Hệ thống] Đang dừng Audio Pipeline...")
        manager.stop()
        print("[Hệ thống] Đã dừng hoàn toàn. Tạm biệt!")
        sys.exit(0)
    except Exception as e:
        import traceback
        print(f"\n[Lỗi nghiêm trọng]: {e}")
        traceback.print_exc()
        manager.stop()
        sys.exit(1)


if __name__ == "__main__":
    main()
