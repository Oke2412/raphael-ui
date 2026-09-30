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
    <div className="flex flex-col gap-2.5">
      {/* Mode Selector (Bo góc 6px, Titanium Glass) */}
      <div className="flex items-center gap-1 rounded-[6px] border border-white/[0.08] bg-[rgba(16,18,22,0.6)] p-0.5">
        {MODES.map((m) => {
          const active = m.value === outputMode
          const Icon = m.icon
          return (
            <button
              key={m.value}
              onClick={() => onSetMode(m.value)}
              className={`flex flex-1 items-center justify-center gap-1.5 rounded-[5px] px-2 py-1 text-[11px] font-medium transition-all ${
                active
                  ? "bg-[#383d47] text-white shadow-sm border border-white/10"
                  : "text-[#8a919e] hover:text-[#cbcfd6] hover:bg-white/[0.04]"
              }`}
              aria-pressed={active}
            >
              <Icon className="size-3" />
              <span className="hidden sm:inline">{m.label}</span>
            </button>
          )
        })}
      </div>

      <div className="flex items-stretch gap-2">
        {/* Push-to-Talk (Bo góc 6px, Titanium) */}
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
          className={`btn-hover-glow group relative grid aspect-square w-11 shrink-0 place-items-center rounded-[6px] border transition-all select-none ${
            isListening
              ? "border-sky-400 bg-sky-950/40 text-sky-200 shadow-[0_0_16px_rgba(56,189,248,0.4)]"
              : "border-white/[0.12] bg-[#1a1d24] text-[#cbcfd6]"
          }`}
          aria-label="Push to talk — nhấn giữ để nói"
          aria-pressed={isListening}
          title="Nhấn giữ để nói (Push-to-Talk)"
        >
          {isListening && (
            <span className="absolute inset-0 rounded-[6px] sage-pulse-ring bg-sky-400/30" />
          )}
          <Mic className="size-4 transition-colors" />
        </button>

        {/* Text prompt capsule (Bo góc 6px) */}
        <div className="flex flex-1 items-center gap-2 rounded-[6px] border border-white/[0.12] bg-[#121418] px-2.5 focus-within:border-white/30 focus-within:bg-[#181a20] transition-colors">
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
            placeholder="Hỏi Raphael hoặc nhập khẩu lệnh…"
            className="min-w-0 flex-1 bg-transparent py-2 text-xs text-[#f3f4f6] placeholder:text-[#5a6270] outline-none font-sans"
          />
          <button
            onClick={submit}
            disabled={!text.trim()}
            className="btn-hover-glow grid size-7 place-items-center rounded-[5px] bg-[#383d47] text-white disabled:opacity-30 disabled:hover:bg-[#383d47] transition-all"
            aria-label="Gửi câu hỏi"
          >
            <Send className="size-3" />
          </button>
        </div>
      </div>
    </div>
  )
}
