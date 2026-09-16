"use client"

import { useEffect, useRef } from "react"
import { MessageSquare } from "lucide-react"
import { NoteFrame } from "./note-frame"
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
    <NoteFrame
      title="Dialogue Log"
      jp="対話記録"
      icon={<MessageSquare className="size-4" />}
      className={className}
      bodyClassName="p-0"
    >
      <div className="sage-scroll h-full max-h-full space-y-3 overflow-y-auto px-4 py-3">
        {transcript.length === 0 && (
          <p className="py-8 text-center text-xs text-[color:var(--sage-text-dim)]">
            Nhấn giữ Push-to-Talk hoặc nhập văn bản để bắt đầu đối thoại với Great Sage.
          </p>
        )}
        {transcript.map((entry) => (
          <Bubble key={entry.id} entry={entry} />
        ))}
        <div ref={endRef} />
      </div>
    </NoteFrame>
  )
}

function Bubble({ entry }: { entry: TranscriptEntry }) {
  const isUser = entry.role === "user"
  return (
    <div className={`sage-rise flex ${isUser ? "justify-end" : "justify-start"}`}>
      <div
        className={`max-w-[86%] rounded-xl px-3.5 py-2.5 text-sm leading-relaxed ${
          isUser
            ? "rounded-br-sm bg-white/5 text-[color:var(--sage-text)]"
            : "rounded-bl-sm text-[color:var(--sage-gold)]"
        }`}
        style={
          isUser
            ? { border: "1px solid rgba(255,255,255,0.08)" }
            : {
                border: "1px solid rgba(255,233,168,0.25)",
                background: "rgba(255,233,168,0.06)",
              }
        }
      >
        <span className="mb-1 block text-[9px] font-semibold uppercase tracking-[0.28em] opacity-60">
          {isUser ? "Rimuru" : "Raphael"}
        </span>
        <span className={entry.streaming ? "sage-caret" : undefined}>{entry.text}</span>
      </div>
    </div>
  )
}
