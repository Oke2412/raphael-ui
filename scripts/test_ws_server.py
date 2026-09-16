"""
Kiểm thử kết nối WebSocket tới Core Audio Pipeline
Kiểm tra xem WebSocket Server có phản hồi đúng các sự kiện và nhận diện ping/pong hay không.
"""

import sys
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass

import json
import asyncio
import websockets


async def test_ws_client():
    uri = "ws://127.0.0.1:8765"
    print(f"[*] Đang thử kết nối tới WebSocket Server: {uri}...")
    try:
        async with websockets.connect(uri) as websocket:
            print("  [OK] Kết nối WebSocket thành công!")
            
            # Đọc thông điệp chào mừng (state_change ban đầu)
            greeting = await asyncio.wait_for(websocket.recv(), timeout=3.0)
            print(f"  [Nhận từ Server]: {greeting}")

            # Gửi tin nhắn ping
            print("  [*] Đang gửi tin nhắn: {'action': 'ping'}...")
            await websocket.send(json.dumps({"action": "ping"}))

            # Nhận pong
            response = await asyncio.wait_for(websocket.recv(), timeout=3.0)
            print(f"  [Phản hồi từ Server]: {response}")
            
            data = json.loads(response)
            if data.get("type") == "pong":
                print("  [OK] WebSocket hoạt động hoàn hảo!")
            else:
                print(f"  [Thông tin] Nhận sự kiện khác: {data}")
    except Exception as e:
        print(f"  [Lỗi] Không thể kết nối tới {uri}: {e}")
        print("  (Hãy chắc chắn rằng main.py hoặc run_pipeline.bat đang chạy)")


if __name__ == "__main__":
    asyncio.run(test_ws_client())
