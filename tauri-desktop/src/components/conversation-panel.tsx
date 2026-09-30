"use client"

import { useEffect, useRef } from "react"
import { MessageSquare } from "lucide-react"
import type { TranscriptEntry } from "@/lib/use-sage-socket"

export function ConversationPanel({
  transcript,
  className,
}: {
  transcript: TranscriptEntry[]
  className?: string
}) {
  const endRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth", block: "end" })
  }, [transcript])

  return (
    <div
      className={`flex flex-col rounded-[10px] border border-white/[0.08] bg-[#121419]/90 backdrop-blur-xl ${
        className ?? ""
      }`}
    >
      <div className="flex items-center justify-between border-b border-white/[0.06] px-3 py-2 bg-white/[0.02]">
        <div className="flex items-center gap-2">
          <MessageSquare className="size-3 text-[#adb3be]" />
          <span className="text-[11px] font-medium tracking-wider text-[#cbcfd6] font-mono">
            HỘI THOẠI RAPHAEL
          </span>
        </div>
        <span className="text-[10px] font-mono text-[#69707e]">
          {transcript.length} tin nhắn
        </span>
      </div>

      <div className="sage-scroll flex-1 space-y-2.5 overflow-y-auto p-3 text-xs">
        {transcript.length === 0 && (
          <p className="py-8 text-center text-xs text-[#69707e]">
            Nhấn giữ Micro hoặc gõ câu hỏi để bắt đầu đối thoại với Raphael.
          </p>
        )}
        {transcript.map((entry) => (
          <Bubble key={entry.id} entry={entry} />
        ))}
        <div ref={endRef} />
      </div>
    </div>
  )
}

function Bubble({ entry }: { entry: TranscriptEntry }) {
  const isUser = entry.role === "user"
  return (
    <div className={`sage-rise flex ${isUser ? "justify-end" : "justify-start"}`}>
      <div
        className={`max-w-[88%] rounded-[6px] p-2.5 leading-relaxed text-xs ${
          isUser
            ? "border border-white/[0.12] bg-[#252830] text-[#f3f4f6]"
            : "border border-[rgba(200,205,215,0.14)] bg-[#181b22] text-[#e2e4e8]"
        }`}
      >
        <div className="mb-1 flex items-center gap-1.5 text-[9px] font-mono font-semibold uppercase tracking-wider text-[#adb3be]">
          <span
            className={`inline-block size-1.5 rounded-[2px] ${
              isUser ? "bg-sky-400" : "bg-emerald-400"
            }`}
          />
          <span>{isUser ? "Rimuru" : "Raphael"}</span>
        </div>
        <div className={entry.streaming ? "sage-caret" : undefined}>
          {entry.text}
        </div>
      </div>
    </div>
  )
}
