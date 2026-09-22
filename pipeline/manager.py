"""
Pipeline Coordinator & State Machine (Nhạc trưởng điều phối toàn bộ hệ thống)
Quản lý vòng đời trạng thái: idle -> listening -> transcribing -> thinking -> speaking -> idle
Lắng nghe Phím tắt toàn cục (Global Hotkey), VAD, kết nối Server từ xa và bắn sự kiện ra WebSocket.
"""

import json
import time
import queue
import threading
import numpy as np
from typing import Callable, Optional, Dict, Any

from pipeline.audio_io import AudioInputRecorder, AudioOutputPlayer
from pipeline.vad_detector import SileroVADDetector
from pipeline.wake_word import WakeWordDetector
from pipeline.stt_engine import STTEngine
from pipeline.server_client import RemoteServerClient
from pipeline.text_chunker import StreamTextChunker
from pipeline.vieneu_engine import VieNeuTTSEngine
from pipeline.rvc_engine import RVCEngine
from pipeline.neo4j_client import Neo4jCosmosClient


class PipelineManager:
    def __init__(self, config_path: str = "config/settings.json"):
        self.config_path = config_path
        self.config = self._load_config()

        # Callbacks gửi dữ liệu ra ngoài (cho WebSocket Server)
        self.on_state_change: Optional[Callable[[str], None]] = None
        self.on_audio_levels: Optional[Callable[[dict], None]] = None
        self.on_wake_word: Optional[Callable[[str], None]] = None
        self.on_graph_data: Optional[Callable[[dict], None]] = None
        self.on_graph_highlight: Optional[Callable[[dict], None]] = None
        self.on_user_transcript: Optional[Callable[[str], None]] = None
        self.on_assistant_stream: Optional[Callable[[str, str], None]] = None
        self.on_assistant_done: Optional[Callable[[str], None]] = None
        self.on_error: Optional[Callable[[str, str], None]] = None

        self.current_state = "idle"
        self.output_mode = self.config.get("system", {}).get("output_mode", "voice_and_text")
        self._cancel_flag = threading.Event()
        self._is_running = False

        # Khởi tạo các thành phần cốt lõi
        print("[Pipeline] Khởi tạo các thành phần Audio IO...")
        self.recorder = AudioInputRecorder(
            sample_rate=self.config["audio"]["sample_rate"],
            channels=self.config["audio"]["channels"],
            device_index=self.config["audio"]["mic_device_index"],
            level_callback=self._handle_audio_levels
        )

        self.player = AudioOutputPlayer(
            sample_rate=self.config["audio"]["output_sample_rate"],
            device_index=self.config["audio"]["speaker_device_index"],
            level_callback=self._handle_audio_levels
        )

        print("[Pipeline] Khởi tạo VAD Detector...")
        self.vad = SileroVADDetector(
            threshold=self.config["vad"]["threshold"],
            sampling_rate=self.config["audio"]["sample_rate"],
            min_speech_duration_ms=self.config["vad"]["min_speech_duration_ms"],
            min_silence_duration_ms=self.config["vad"]["min_silence_duration_ms"]
        )

        print("[Pipeline] Khởi tạo Wake Word Detector (CPU ONNX)...")
        self.wake_word = WakeWordDetector(self.config.get("wake_word", {}))

        print("[Pipeline] Khởi tạo STT Engine...")
        self.stt = STTEngine(
            model_size=self.config["whisper"]["model_size"],
            device=self.config["whisper"]["device"],
            compute_type=self.config["whisper"]["compute_type"],
            language=self.config["whisper"]["language"]
        )

        print("[Pipeline] Khởi tạo Remote Server Client...")
        self.server_client = RemoteServerClient(self.config["server"])

        print("[Pipeline] Khởi tạo VieNeu-TTS Engine...")
        self.tts = VieNeuTTSEngine(self.config["vieneu"])

        print("[Pipeline] Khởi tạo RVC Engine...")
        self.rvc = RVCEngine(self.config["rvc"])

        print("[Pipeline] Khởi tạo Neo4j Cosmos Graph Client...")
        self.cosmos_graph = Neo4jCosmosClient(self.config.get("neo4j", {}))

        # Luồng lắng nghe VAD liên tục
        self._vad_thread: Optional[threading.Thread] = None

    def _load_config(self) -> Dict[str, Any]:
        try:
            with open(self.config_path, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"[Pipeline] Lỗi đọc config: {e}. Dùng cấu hình mặc định.")
            return {}

    def _set_state(self, new_state: str):
        self.current_state = new_state
        print(f"[State] -> {new_state}")
        if self.on_state_change:
            self.on_state_change(new_state)

        # Khi quay về idle, nếu bật Wake Word thì tự động mở lại mic để lắng nghe từ khóa
        if new_state == "idle" and hasattr(self, "wake_word") and self.wake_word.enabled:
            if not self.recorder.is_recording:
                self.recorder.start()
                print("[Pipeline] Sẵn sàng lắng nghe từ khóa Wake Word...")

    def _handle_audio_levels(self, levels: dict):
        if self.on_audio_levels:
            self.on_audio_levels(levels)

    def start(self):
        """Khởi động toàn bộ dịch vụ ngầm."""
        self._is_running = True
        self._setup_hotkey()
        self._set_state("idle")

        # Bật luồng lắng nghe ngầm (Wake Word + VAD)
        self._vad_thread = threading.Thread(target=self._vad_listening_loop, daemon=True)
        self._vad_thread.start()

        # Nếu Wake Word bật, kích hoạt recorder để luôn lắng nghe từ khóa
        if self.wake_word.enabled and not self.recorder.is_recording:
            self.recorder.start()
            print("[Pipeline] Micro đang lắng nghe ngầm từ khóa Wake Word...")

        print("[Pipeline] Hệ thống đã sẵn sàng phục vụ!")

    def stop(self):
        self._is_running = False
        self.recorder.stop()
        self.player.stop()
        self._set_state("idle")

    def _setup_hotkey(self):
        hotkey = self.config.get("system", {}).get("hotkey", "ctrl+space")
        try:
            import keyboard
            keyboard.add_hotkey(hotkey, self.toggle_listening)
            print(f"[Hotkey] Đã đăng ký phím tắt toàn cục: '{hotkey}'")
        except Exception as e:
            print(f"[Hotkey] Không thể đăng ký phím tắt ({e}). Vẫn có thể kích hoạt qua UI hoặc VAD.")

    def toggle_listening(self):
        """Bật/tắt lắng nghe thủ công (Push-to-Talk / Hotkey)."""
        if self.current_state == "idle":
            self.start_listening()
        elif self.current_state == "listening":
            self.stop_listening()

    def start_listening(self):
        if self.current_state not in ["idle", "speaking"]:
            return
        # Nếu đang nói mà kích hoạt thì ngắt lời trợ lý ngay
        self.cancel()
        self._set_state("listening")
        self.vad.reset()
        self.recorder.start()

    def stop_listening(self):
        if self.current_state != "listening":
            return
        self.recorder.stop()
        self._set_state("transcribing")
        # Thu thập toàn bộ âm thanh còn lại trong queue
        chunks = []
        while True:
            c = self.recorder.read_chunk(timeout=0.05)
            if c is None:
                break
            chunks.append(c)
        
        if chunks:
            full_audio = np.concatenate(chunks)
            threading.Thread(target=self._process_user_audio, args=(full_audio,), daemon=True).start()
        else:
            self._set_state("idle")

    def _vad_listening_loop(self):
        """Vòng lặp lắng nghe ngầm: Wake Word khi idle, VAD dứt câu khi listening."""
        while self._is_running:
            # 1. Khi ở trạng thái IDLE và Wake Word bật: Liên tục bắt từ khóa
            if self.current_state == "idle" and self.wake_word.enabled:
                chunk = self.recorder.read_chunk(timeout=0.05)
                if chunk is not None:
                    # 1a. Kiểm tra qua OpenWakeWord (Hey Jarvis hoặc model ONNX tùy biến)
                    detected, keyword, score = self.wake_word.process_chunk(chunk)
                    if detected:
                        print(f"[WakeWord] Đã phát hiện từ khóa '{keyword}' (Score: {score:.2f})!")
                        if self.on_wake_word:
                            self.on_wake_word(keyword)
                        self.start_listening()
                        continue

                    # 1b. Nhận diện từ khóa "Raphael" / "Great Sage" trực tiếp bằng VAD + Whisper CUDA
                    is_speaking, completed_audio = self.vad.process_chunk(chunk)
                    if completed_audio is not None and self.current_state == "idle":
                        threading.Thread(target=self._check_wake_word_phrase, args=(completed_audio,), daemon=True).start()

            # 2. Khi ở trạng thái LISTENING: VAD kiểm tra khi nào người dùng dứt câu
            elif self.current_state == "listening":
                chunk = self.recorder.read_chunk(timeout=0.05)
                if chunk is not None:
                    is_speaking, completed_audio = self.vad.process_chunk(chunk)
                    if completed_audio is not None:
                        # Người dùng đã dứt câu!
                        print("[VAD] Phát hiện dứt câu nói.")
                        self.recorder.stop()
                        self._set_state("transcribing")
                        threading.Thread(target=self._process_user_audio, args=(completed_audio,), daemon=True).start()
            else:
                time.sleep(0.05)

    def _check_wake_word_phrase(self, audio: np.ndarray):
        """Nhận diện nhanh từ khóa khi idle bằng Faster-Whisper trên CUDA (~50ms)."""
        try:
            duration_sec = len(audio) / float(self.config["audio"]["sample_rate"])
            if duration_sec > 4.5:
                return

            text = self.stt.transcribe(audio).strip().lower()
            if not text:
                return

            # Các biến thể cách gọi Raphael & Great Sage bằng tiếng Việt / tiếng Anh
            wake_phrases = [
                "raphael", "rafael", "ra fa el", "ra-pha-el", "la fa en",
                "great sage", "greatsage", "sage",
                "đại hiền triết", "huyền triết", "hiền triết",
                "hey jarvis", "jarvis"
            ]

            is_wake = any(phrase in text for phrase in wake_phrases)
            if is_wake and self.current_state == "idle":
                print(f"[WakeWord] Phát hiện khẩu lệnh gọi: '{text}'!")
                if self.on_wake_word:
                    self.on_wake_word("raphael")
                self.start_listening()
        except Exception as e:
            print(f"[WakeWord] Lỗi kiểm tra từ khóa: {e}")

    def _process_user_audio(self, audio_data: np.ndarray):
        """Nhận audio -> STT -> gửi lên Server."""
        try:
            print("[STT] Đang nhận diện âm thanh...")
            transcript = self.stt.transcribe(audio_data)
            print(f"[STT Kết Quả]: '{transcript}'")
            
            if not transcript.strip():
                print("[STT] Không nhận diện được từ nào rõ ràng.")
                self._set_state("idle")
                return

            if self.on_user_transcript:
                self.on_user_transcript(transcript)

            # Đẩy tiếp câu hỏi sang LLM
            self.process_text_prompt(transcript)
        except Exception as e:
            print(f"[Pipeline] Lỗi xử lý âm thanh người dùng: {e}")
            self._set_state("idle")

    def process_text_prompt(self, text: str):
        """Nhận text trực tiếp (từ STT hoặc khung chat UI) -> Gọi LLM Server."""
        self._cancel_flag.clear()
        self._set_state("thinking")

        # Kích hoạt hiệu ứng chòm sao vũ trụ Neo4j tương ứng với câu hỏi
        if self.on_graph_highlight:
            try:
                highlight_data = self.cosmos_graph.query_subgraph(text)
                self.on_graph_highlight(highlight_data)
            except Exception as ex_g:
                print(f"[Graph] Lỗi highlight: {ex_g}")

        threading.Thread(target=self._query_and_speak_stream, args=(text,), daemon=True).start()

    def get_cosmos_graph_data(self) -> dict:
        """Lấy toàn bộ mạng lưới sao đồ thị tri thức để Frontend vẽ vũ trụ 3D/2D."""
        return self.cosmos_graph.get_cosmos_graph()

    def _query_and_speak_stream(self, prompt: str):
        """Xử lý stream token từ Remote Server và nạp gối đầu sang TTS."""
        chunker = StreamTextChunker(min_chunk_words=2)
        full_response = ""
        is_first_chunk = True

        try:
            for token in self.server_client.stream_query(prompt):
                if self._cancel_flag.is_set():
                    print("[Pipeline] Yêu cầu đã bị hủy bởi người dùng.")
                    break

                full_response += token
                if self.on_assistant_stream:
                    self.on_assistant_stream(token, full_response)

                # Nếu ở chế độ có âm thanh (voice_and_text hoặc voice_only)
                if self.output_mode != "text_only":
                    speakable_sentences = chunker.feed(token)
                    for sentence in speakable_sentences:
                        if is_first_chunk:
                            self._set_state("speaking")
                            is_first_chunk = False
                        self._speak_sentence(sentence)

            # Xả nốt phần chữ còn lại trong buffer
            if not self._cancel_flag.is_set() and self.output_mode != "text_only":
                for remaining in chunker.flush():
                    if is_first_chunk:
                        self._set_state("speaking")
                        is_first_chunk = False
                    self._speak_sentence(remaining)

        except Exception as e:
            print(f"[Pipeline] Lỗi trong luồng phản hồi: {e}")
            if self.on_error:
                self.on_error("STREAM_ERROR", str(e))

        if self.on_assistant_done:
            self.on_assistant_done(full_response)

        # Chờ phát xong âm thanh rồi về idle
        if self.output_mode != "text_only":
            while self.player.is_playing and not self._cancel_flag.is_set():
                time.sleep(0.1)

        self._set_state("idle")

    def _speak_sentence(self, sentence: str):
        """VieNeu-TTS -> RVC -> Phát loa (Toàn bộ trong RAM)."""
        if not sentence.strip():
            return
        print(f"[TTS] Đang đọc câu: '{sentence}'")
        raw_audio = self.tts.synthesize(sentence)
        if raw_audio is not None and len(raw_audio) > 0:
            converted_audio = self.rvc.convert_voice(raw_audio, source_sr=self.tts.sample_rate)
            self.player.play_numpy(converted_audio, sample_rate=self.tts.sample_rate)

    def cancel(self):
        """Hủy bỏ phản hồi hiện tại, ngắt tiếng loa và về idle."""
        self._cancel_flag.set()
        self.player.stop()
        self._set_state("idle")

    def set_output_mode(self, mode: str):
        if mode in ["voice_and_text", "text_only", "voice_only"]:
            self.output_mode = mode
            print(f"[Mode] Chế độ xuất chuyển thành: {mode}")

    def set_wake_word_enabled(self, enabled: bool):
        self.wake_word.enabled = enabled
        print(f"[WakeWord] Trạng thái Wake Word: {'BẬT' if enabled else 'TẮT'}")
        if not enabled and self.current_state == "idle" and self.recorder.is_recording:
            self.recorder.stop()
        elif enabled and self.current_state == "idle" and not self.recorder.is_recording:
            self.recorder.start()

    def update_config(self, new_config: dict):
        if "remote_server_url" in new_config:
            self.server_client.update_url(new_config["remote_server_url"])
        if "output_mode" in new_config:
            self.set_output_mode(new_config["output_mode"])
        if "rvc_enabled" in new_config:
            self.rvc.enabled = new_config["rvc_enabled"]
        if "wake_word_enabled" in new_config:
            self.set_wake_word_enabled(new_config["wake_word_enabled"])
