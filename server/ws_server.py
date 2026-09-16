"""
WebSocket Server
Cầu nối truyền thông giữa Core Audio Pipeline và Giao diện Tensura UI.
Phát sóng (Broadcast) dữ liệu sóng âm, trạng thái và tiếp nhận các lệnh điều khiển từ Frontend.
"""

import json
import asyncio
import websockets
from typing import Set
from pipeline.manager import PipelineManager


class TensuraWebSocketServer:
    def __init__(self, manager: PipelineManager, host: str = "127.0.0.1", port: int = 8765):
        self.manager = manager
        self.host = host
        self.port = port
        self.connected_clients: Set[websockets.WebSocketServerProtocol] = set()
        self.loop: Optional[asyncio.AbstractEventLoop] = None
        self._bind_manager_callbacks()

    def _bind_manager_callbacks(self):
        """Gắn các hàm callback của PipelineManager để bắn tin qua WebSocket."""
        self.manager.on_state_change = lambda state: self.broadcast_sync({
            "type": "STATE_CHANGE",
            "state": state,
            "data": {"status": state}
        })

        self.manager.on_audio_levels = lambda levels: self.broadcast_sync({
            "type": "AUDIO_LEVELS",
            "mic_level": levels.get("rms", 0.0) if levels.get("source") == "mic" else 0.0,
            "speaker_level": levels.get("rms", 0.0) if levels.get("source") == "speaker" else 0.0,
            "spectrum": levels.get("spectrum", []),
            "data": levels
        })

        self.manager.on_wake_word = lambda kw: self.broadcast_sync({
            "type": "WAKE_WORD_DETECTED",
            "keyword": kw,
            "data": {"keyword": kw}
        })

        self.manager.on_graph_data = lambda graph: self.broadcast_sync({
            "type": "GRAPH_DATA",
            "data": graph
        })

        self.manager.on_graph_highlight = lambda highlight: self.broadcast_sync({
            "type": "GRAPH_QUERY",
            "query": highlight.get("query", ""),
            "nodes": [{"id": nid, "label": nid} for nid in highlight.get("highlighted_nodes", [])],
            "rels": highlight.get("highlighted_links", []),
            "hits": len(highlight.get("highlighted_nodes", [])),
            "data": highlight
        })

        self.manager.on_user_transcript = lambda text: self.broadcast_sync({
            "type": "USER_TRANSCRIPT",
            "text": text,
            "data": {"text": text, "is_final": True}
        })

        self.manager.on_assistant_stream = lambda delta, full_text: self.broadcast_sync({
            "type": "ASSISTANT_STREAM",
            "token": delta,
            "text": full_text,
            "data": {"delta": delta, "full_text": full_text}
        })

        self.manager.on_assistant_done = lambda full_text: self.broadcast_sync({
            "type": "ASSISTANT_STREAM",
            "token": "",
            "done": True,
            "full_text": full_text,
            "data": {"full_text": full_text}
        })

        self.manager.on_error = lambda code, msg: self.broadcast_sync({
            "type": "ERROR",
            "code": code,
            "message": msg,
            "data": {"code": code, "message": msg}
        })

    def broadcast_sync(self, message: dict):
        """Gọi an toàn từ các thread của PipelineManager sang Asyncio Loop của WebSocket."""
        if self.loop and self.loop.is_running():
            asyncio.run_coroutine_threadsafe(self.broadcast(message), self.loop)

    async def broadcast(self, message: dict):
        if not self.connected_clients:
            return
        payload = json.dumps(message, ensure_ascii=False)
        disconnected = set()
        for client in self.connected_clients:
            try:
                await client.send(payload)
            except websockets.exceptions.ConnectionClosed:
                disconnected.add(client)
        self.connected_clients -= disconnected

    async def _handle_connection(self, websocket, path=None):
        self.connected_clients.add(websocket)
        print(f"[WS] Client đã kết nối từ {websocket.remote_address}")

        # Gửi ngay trạng thái hiện tại cho Client vừa vào
        await websocket.send(json.dumps({
            "type": "state_change",
            "data": {"status": self.manager.current_state}
        }, ensure_ascii=False))

        # Gửi ngay toàn bộ dữ liệu đồ thị tri thức để dựng vũ trụ sao sau Orb
        try:
            cosmos_graph = self.manager.get_cosmos_graph_data()
            await websocket.send(json.dumps({
                "type": "graph_data",
                "data": cosmos_graph
            }, ensure_ascii=False))
        except Exception as ex_g:
            print(f"[WS] Không thể gửi dữ liệu đồ thị ban đầu: {ex_g}")

        try:
            async for message in websocket:
                try:
                    data = json.loads(message)
                    raw_action = str(data.get("action") or data.get("type", ""))
                    action = raw_action.lower().replace("-", "_")

                    if action in ["start_listen"]:
                        self.manager.start_listening()
                    elif action in ["stop_listen"]:
                        self.manager.stop_listening()
                    elif action in ["send_text", "send_text_prompt"]:
                        text = data.get("text") or data.get("data", {}).get("text", "")
                        if text:
                            self.manager.process_text_prompt(text)
                    elif action in ["set_output_mode"]:
                        mode = data.get("mode") or data.get("data", {}).get("mode", "voice_and_text")
                        self.manager.set_output_mode(mode)
                    elif action in ["set_wake_word"]:
                        enabled = data.get("enabled") if "enabled" in data else data.get("data", {}).get("enabled", True)
                        self.manager.set_wake_word_enabled(enabled)
                    elif action in ["get_graph_data"]:
                        graph = self.manager.get_cosmos_graph_data()
                        await websocket.send(json.dumps({
                            "type": "GRAPH_DATA",
                            "data": graph
                        }, ensure_ascii=False))
                    elif action in ["query_graph"]:
                        q = data.get("query") or data.get("data", {}).get("query", "")
                        highlight = self.manager.cosmos_graph.query_subgraph(q)
                        await websocket.send(json.dumps({
                            "type": "GRAPH_QUERY",
                            "data": highlight
                        }, ensure_ascii=False))
                    elif action in ["cancel"]:
                        self.manager.cancel()
                    elif action in ["update_config"]:
                        cfg = data.get("config") or data.get("data", {})
                        self.manager.update_config(cfg)
                    elif action in ["ping"]:
                        await websocket.send(json.dumps({"type": "PONG"}))
                except json.JSONDecodeError:
                    print("[WS] Nhận bản tin không đúng định dạng JSON.")
        except websockets.exceptions.ConnectionClosed:
            pass
        finally:
            self.connected_clients.remove(websocket)
            print(f"[WS] Client đã ngắt kết nối.")

    async def run_server(self):
        self.loop = asyncio.get_running_loop()
        # Khởi động manager
        self.manager.start()
        print(f"[WS] Tensura WebSocket Server đang lắng nghe tại ws://{self.host}:{self.port}")
        async with websockets.serve(self._handle_connection, self.host, self.port):
            await asyncio.Future()  # Chạy vô hạn
