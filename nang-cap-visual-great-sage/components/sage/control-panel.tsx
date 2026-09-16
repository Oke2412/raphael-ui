"use client"

import { useState } from "react"
import { Mic, Send, Volume2, Type, MessageSquare } from "lucide-react"
import type { OutputMode } from "@/lib/sage-protocol"

const MODES: { value: OutputMode; label: string; icon: typeof Mic }[] = [
  { value: "voice_and_text", label: "Voice + Text", icon: MessageSquare },
  { value: "text_only", label: "Text Only", icon: Type },
  { value: "voice_only", label: "Voice Only", icon: Volume2 },
]

interface Props {
  isListening: boolean
  outputMode: OutputMode
  onStartListen: () => void
  onStopListen: () => void
  onSendText: (text: string) => void
  onSetMode: (mode: OutputMode) => void
}

export function ControlPanel({
  isListening,
  outputMode,
  onStartListen,
  onStopListen,
  onSendText,
  onSetMode,
}: Props) {
  const [text, setText] = useState("")

  const submit = () => {
    if (!text.trim()) return
    onSendText(text)
    setText("")
  }

  return (
    <div className="flex flex-col gap-3">
      {/* Output mode selector */}
      <div className="flex items-center gap-1.5 rounded-xl border border-white/8 bg-black/30 p-1">
        {MODES.map((m) => {
          const active = m.value === outputMode
          const Icon = m.icon
          return (
            <button
              key={m.value}
              onClick={() => onSetMode(m.value)}
              className="flex flex-1 items-center justify-center gap-1.5 rounded-lg px-2 py-1.5 text-[11px] font-medium tracking-wide transition-all"
              style={
                active
                  ? {
                      background: "rgba(143,233,192,0.14)",
                      color: "var(--sage-cyan)",
                      boxShadow: "inset 0 0 0 1px rgba(143,233,192,0.35)",
                    }
                  : { color: "var(--sage-text-dim)" }
              }
              aria-pressed={active}
            >
              <Icon className="size-3.5" />
              <span className="hidden sm:inline">{m.label}</span>
            </button>
          )
        })}
      </div>

      <div className="flex items-stretch gap-3">
        {/* Push-to-Talk */}
        <button
          onPointerDown={(e) => {
            e.preventDefault()
            onStartListen()
          }}
          onPointerUp={(e) => {
            e.preventDefault()
            onStopListen()
          }}
          onPointerLeave={() => {
            if (isListening) onStopListen()
          }}
          className="group relative grid aspect-square w-[72px] shrink-0 place-items-center rounded-xl border transition-all select-none"
          style={{
            borderColor: isListening ? "var(--sage-cyan-deep)" : "rgba(255,255,255,0.1)",
            background: isListening ? "rgba(143,233,192,0.16)" : "rgba(0,0,0,0.3)",
            boxShadow: isListening ? "0 0 28px rgba(143,233,192,0.45)" : "none",
          }}
          aria-label="Push to talk — nhấn giữ để nói"
          aria-pressed={isListening}
        >
          {isListening && (
            <span
              className="absolute inset-0 rounded-xl sage-pulse-ring"
              style={{ background: "rgba(143,233,192,0.35)" }}
            />
          )}
          <Mic
            className="size-6 transition-colors"
            style={{ color: isListening ? "var(--sage-cyan)" : "var(--sage-text-dim)" }}
          />
        </button>

        {/* Text prompt */}
        <div className="flex flex-1 items-center gap-2 rounded-xl border border-white/10 bg-black/30 px-3 focus-within:border-[color:var(--sage-cyan-deep)]">
          <input
            value={text}
            onChange={(e) => setText(e.target.value)}
            onKeyDown={(e) => {
              if (
                e.key === "Enter" &&
                !e.nativeEvent.isComposing &&
                e.keyCode !== 229
              ) {
                e.preventDefault()
                submit()
              }
            }}
            placeholder="Nhập câu hỏi trực tiếp cho Great Sage…"
            className="min-w-0 flex-1 bg-transparent py-3 text-sm text-[color:var(--sage-text)] outline-none placeholder:text-[color:var(--sage-text-dim)]"
          />
          <button
            onClick={submit}
            disabled={!text.trim()}
            className="grid size-9 place-items-center rounded-lg transition-all disabled:opacity-30"
            style={{ background: "rgba(143,233,192,0.14)", color: "var(--sage-cyan)" }}
            aria-label="Gửi câu hỏi"
          >
            <Send className="size-4" />
          </button>
        </div>
      </div>

      <p className="text-center text-[10px] tracking-wide text-[color:var(--sage-text-dim)]">
        Nhấn giữ nút micro để nói · thả ra để gửi · hoặc gõ &amp; nhấn Enter
      </p>
    </div>
  )
}
