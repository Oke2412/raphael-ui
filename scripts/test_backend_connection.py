"""
Tensura Raphael - Docker / Remote Backend Connection Tester
Kiểm tra kết nối và luồng trả lời (Stream Chat) tới Backend LLM (Docker / Tailscale / Local).
"""

import os
import sys
import time
import json
import argparse
import requests

# Đảm bảo mã hóa UTF-8 cho console Windows
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if PROJECT_ROOT not in sys.path:
    sys.path.insert(0, PROJECT_ROOT)

from pipeline.server_client import RemoteServerClient


def main():
    parser = argparse.ArgumentParser(description="Test Backend Connection")
    parser.add_argument(
        "--url",
        type=str,
        default="",
        help="URL của backend (để trống sẽ lấy từ config/settings.json)",
    )
    parser.add_argument(
        "--prompt",
        type=str,
        default="Chào bạn, hãy giới thiệu ngắn gọn bạn là ai trong 1 câu.",
        help="Câu hỏi thử nghiệm gửi lên backend",
    )
    args = parser.parse_args()

    print("=" * 65)
    print("   🌐 TENSURA RAPHAEL — KIỂM TRA KẾT NỐI BACKEND DOCKER / LOCAL")
    print("=" * 65)

    # Đọc cấu hình
    config_path = os.path.join(PROJECT_ROOT, "config", "settings.json")
    with open(config_path, "r", encoding="utf-8") as f:
        config = json.load(f)

    server_cfg = config.get("server", {})
    if args.url:
        server_cfg["url"] = args.url

    target_url = server_cfg.get("url", "http://127.0.0.1:8000/api/chat/message")
    print(f"\n[*] Địa chỉ Backend mục tiêu: {target_url}")

    client = RemoteServerClient(server_cfg)

    # 1. Kiểm tra Ping kết nối
    print("[1/2] Đang kiểm tra phản hồi từ máy chủ (Ping test)...")
    res = client.test_connection()
    if res["status"] != "ok":
        print(f"\n[!] THẤT BẠI: {res.get('message', 'Không thể kết nối')}")
        print("\n💡 Hướng dẫn khắc phục:")
        print("  1. Hãy đảm bảo Docker Desktop đã được mở trên máy.")
        print("  2. Khởi chạy container backend tại thư mục 'd:\\project\\sttap':")
        print("     > cd d:\\project\\sttap")
        print("     > docker compose up -d")
        print("  3. Hoặc nếu không dùng Docker, bạn có thể chạy trực tiếp bằng:")
        print("     > cd d:\\project\\sttap")
        print("     > run.bat")
        print("=" * 65)
        return 1

    print(f"[✓] Kết nối thành công! (Mã phản hồi HTTP: {res.get('code')})")

    # 2. Thử nghiệm gửi Chat Stream
    print(f"\n[2/2] Thử nghiệm gửi tin nhắn mẫu:")
    print(f"      Prompt: \"{args.prompt}\"")
    print("\n[AI Phản hồi]: ", end="", flush=True)

    stream_t0 = time.time()
    collected = ""
    try:
        for chunk in client.stream_query(args.prompt):
            print(chunk, end="", flush=True)
            collected += chunk
        print()
    except Exception as e:
        print(f"\n[!] Lỗi khi nhận dữ liệu stream: {e}")
        return 1

    stream_t1 = time.time()
    print(f"\n[✓] Hoàn thành luồng phản hồi ({len(collected)} ký tự trong {stream_t1 - stream_t0:.2f}s)!")
    print("=" * 65)
    return 0


if __name__ == "__main__":
    sys.exit(main())
