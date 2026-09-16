"""
Remote Server Client (Tailscale / Network Bridge)
Gửi văn bản người dùng nói lên Server LLM từ xa qua mạng nội bộ hoặc Tailscale.
Nhận dòng chữ trả về theo cơ chế Stream (SSE / Chunks) để đẩy ngay sang TTS.
"""

import json
import requests
from typing import Generator, Optional, Dict, Any


class RemoteServerClient:
    def __init__(self, config: Dict[str, Any]):
        self.url = config.get("url", "http://127.0.0.1:8000/api/chat")
        self.api_type = config.get("api_type", "generic_stream")
        self.timeout = config.get("timeout_seconds", 30)

    def update_url(self, new_url: str):
        self.url = new_url

    def test_connection(self) -> Dict[str, Any]:
        """
        Kiểm tra ping tới Server LLM (Tailscale).
        """
        try:
            # Thử gửi HEAD hoặc GET request kiểm tra phản hồi
            resp = requests.get(self.url, timeout=3)
            return {"status": "ok", "code": resp.status_code, "message": "Kết nối thành công"}
        except requests.exceptions.ConnectionError:
            return {"status": "error", "message": "Không thể kết nối (Connection Refused). Hãy kiểm tra lại IP Tailscale và bật server."}
        except requests.exceptions.Timeout:
            return {"status": "error", "message": "Quá thời gian kết nối (Timeout). Mạng Tailscale có thể đang chập chờn."}
        except Exception as e:
            return {"status": "error", "message": str(e)}

    def stream_query(self, prompt: str) -> Generator[str, None, None]:
        """
        Gửi prompt và sinh ra các token/từ (stream) ngay khi server trả lời.
        """
        payload = {
            "prompt": prompt,
            "stream": True,
            "messages": [
                {"role": "system", "content": "Bạn là Đại Hiền Triết (Raphael) trong Tensura. Hãy trả lời súc tích, ngắn gọn, chuẩn xác dưới 2 câu."},
                {"role": "user", "content": prompt}
            ]
        }

        try:
            with requests.post(
                self.url,
                json=payload,
                stream=True,
                timeout=self.timeout,
                headers={"Content-Type": "application/json"}
            ) as response:
                if response.status_code != 200:
                    yield f"[Lỗi Máy Chủ: HTTP {response.status_code}]"
                    return

                for line in response.iter_lines(decode_unicode=True):
                    if not line:
                        continue
                    
                    # Xử lý chuẩn SSE: "data: ..."
                    if line.startswith("data: "):
                        line_data = line[6:].strip()
                        if line_data == "[DONE]":
                            break
                        try:
                            parsed = json.loads(line_data)
                            # Định dạng OpenAI: choices[0].delta.content
                            if "choices" in parsed and len(parsed["choices"]) > 0:
                                delta = parsed["choices"][0].get("delta", {}).get("content", "")
                                if delta:
                                    yield delta
                            # Định dạng Ollama / Generic: {"response": "..."} hoặc {"token": "..."}
                            elif "response" in parsed:
                                yield parsed["response"]
                            elif "token" in parsed:
                                yield parsed["token"]
                            elif "text" in parsed:
                                yield parsed["text"]
                        except json.JSONDecodeError:
                            yield line_data
                    else:
                        # Dòng text thô hoặc JSON trực tiếp
                        try:
                            parsed = json.loads(line)
                            if "response" in parsed:
                                yield parsed["response"]
                            elif "text" in parsed:
                                yield parsed["text"]
                        except json.JSONDecodeError:
                            yield line
        except requests.exceptions.RequestException as e:
            yield f"[Lỗi kết nối Server: {str(e)}]"
