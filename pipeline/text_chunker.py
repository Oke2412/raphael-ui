"""
Text Chunker for Real-time Streaming
Gọt tỉa và chia nhỏ dòng token stream từ LLM thành các mệnh đề/câu hoàn chỉnh
ngay khi gặp các dấu câu (., !, ?, ;, :) để nạp gối đầu ngay vào VieNeu-TTS mà không phải chờ cả đoạn.
"""

import re
from typing import Generator, List


class StreamTextChunker:
    def __init__(self, min_chunk_words: int = 3):
        self.min_chunk_words = min_chunk_words
        self.buffer = ""
        # Các ký tự ngắt câu / ngắt mệnh đề
        self.split_pattern = re.compile(r'([.?!;:\n]+)')

    def feed(self, token: str) -> List[str]:
        """
        Nhận 1 token mới và trả về danh sách các câu/cụm từ hoàn chỉnh sẵn sàng đọc.
        """
        self.buffer += token
        chunks_to_speak = []

        # Tách theo dấu câu
        parts = self.split_pattern.split(self.buffer)
        
        # Nếu parts có độ dài > 1 thì có ít nhất 1 dấu ngắt câu đã xuất hiện
        if len(parts) > 1:
            # parts dạng: [clause1, punct1, clause2, punct2, remainder]
            reconstructed = ""
            i = 0
            while i < len(parts) - 1:
                sentence = (parts[i] + parts[i+1]).strip()
                words = sentence.split()
                if len(words) >= self.min_chunk_words:
                    chunks_to_speak.append(sentence)
                else:
                    # Nếu câu quá ngắn (ví dụ "Vâng, "), tích lũy thêm vào cụm tiếp theo
                    if i + 2 < len(parts):
                        parts[i+2] = sentence + " " + parts[i+2]
                    else:
                        reconstructed = sentence
                i += 2
            
            # Phần dư chưa dứt câu lưu lại cho lần nạp tiếp theo
            self.buffer = reconstructed + (parts[-1] if len(parts) % 2 == 1 else "")

        return chunks_to_speak

    def flush(self) -> List[str]:
        """
        Xả toàn bộ phần chữ còn sót lại trong buffer khi LLM kết thúc stream.
        """
        chunks = []
        remaining = self.buffer.strip()
        if remaining:
            chunks.append(remaining)
        self.buffer = ""
        return chunks
