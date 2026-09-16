"""
Tensura Real-time AI Assistant - Core Audio Pipeline Entrypoint
Khởi động Core Pipeline và WebSocket Server kết nối Frontend.
"""

import sys

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass

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


if __name__ == "__main__":
    main()
