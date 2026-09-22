"use client"

import { useState, useEffect } from "react"
import {
  Maximize2,
  Minimize2,
  Network,
  PanelRight,
  Settings,
  Sparkles,
  Monitor,
  Layers,
  X,
  Activity,
  EyeOff,
  Power,
} from "lucide-react"
import { STATE_META } from "@/lib/sage-protocol"
import { useSageSocket } from "@/lib/use-sage-socket"
import { SageCoreCanvas } from "./sage-core-canvas"
import { GraphBackground } from "./graph-background"
import { NoteFrame } from "./note-frame"
import {
  ConnectionPill,
  StateBadge,
} from "./state-badge"
import { ConversationPanel } from "./conversation-panel"
import { ControlPanel } from "./control-panel"
import { ConfigDialog } from "./config-dialog"

export type VisualMode = "overlay" | "docked" | "sidedeck" | "hidden"

function playWakeChime() {
  if (typeof window === "undefined") return
  try {
    const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext
    const ctx = new AudioCtx()
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.type = "sine"
    osc.frequency.setValueAtTime(587.33, ctx.currentTime) // D5
    osc.frequency.exponentialRampToValueAtTime(880, ctx.currentTime + 0.14) // A5
    gain.gain.setValueAtTime(0.2, ctx.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.45)
    osc.connect(gain)
    gain.connect(ctx.destination)
    osc.start()
    osc.stop(ctx.currentTime + 0.45)
  } catch (_) {}
}

export function SageConsole() {
  const sage = useSageSocket()
  // Mặc định khởi chạy ngầm 100% Stealth (hidden)
  const [visualMode, setVisualMode] = useState<VisualMode>("hidden")
  const [configOpen, setConfigOpen] = useState(false)
  const [previewDesktop, setPreviewDesktop] = useState(false)
  const [isElectron, setIsElectron] = useState(false)
  const [startupEnabled, setStartupEnabled] = useState(false)
  const accent = STATE_META[sage.state].color

  useEffect(() => {
    if (typeof window !== "undefined" && Boolean(window.electronAPI?.isElectron)) {
      setIsElectron(true)
      window.electronAPI?.onSetVisualMode((mode) => {
        setVisualMode(mode)
      })
      if (window.electronAPI?.getStartupStatus) {
        window.electronAPI.getStartupStatus().then((en) => setStartupEnabled(Boolean(en)))
      }
    } else {
      // Khi mở trên trình duyệt thường (Next.js dev/browser test), hiển thị Overlay
      setVisualMode("overlay")
    }
  }, [])

  const handleToggleStartup = async () => {
    if (typeof window !== "undefined" && window.electronAPI?.toggleStartup) {
      const res = await window.electronAPI.toggleStartup(!startupEnabled)
      setStartupEnabled(res)
    }
  }

  const changeVisualMode = (mode: VisualMode) => {
    setVisualMode(mode)
    if (typeof window !== "undefined" && window.electronAPI) {
      window.electronAPI.setVisualMode(mode)
    }
  }

  // Tự động thức dậy mở Overlay toàn màn hình khi wake word được phát hiện hoặc khi micro bắt đầu lắng nghe
  useEffect(() => {
    if (sage.wakeCount > 0 && visualMode !== "overlay") {
      playWakeChime()
      changeVisualMode("overlay")
    }
  }, [sage.wakeCount])

  useEffect(() => {
    if (sage.state === "listening" && visualMode === "hidden") {
      playWakeChime()
      changeVisualMode("overlay")
    }
  }, [sage.state])

  // Lắng nghe phím Escape để nhanh chóng ẩn về chế độ chạy ngầm
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        if (visualMode !== "hidden") {
          changeVisualMode("hidden")
        }
      }
    }
    window.addEventListener("keydown", handleKeyDown)
    return () => window.removeEventListener("keydown", handleKeyDown)
  }, [visualMode])

  // Trong chế độ fullscreen overlay: truyền click-through xuyên qua các vùng trống để không chặn desktop
  useEffect(() => {
    if (typeof window === "undefined" || !window.electronAPI) return
    if (visualMode !== "overlay") {
      window.electronAPI.setIgnoreMouseEvents(false)
      return
    }

    const handleMouseMove = (e: MouseEvent) => {
      const target = e.target as HTMLElement | null
      const interactive = target?.closest(".pointer-events-auto, button, input, textarea, a, [role='button']")
      if (interactive) {
        window.electronAPI?.setIgnoreMouseEvents(false)
      } else {
        window.electronAPI?.setIgnoreMouseEvents(true, { forward: true })
      }
    }

    window.addEventListener("mousemove", handleMouseMove)
    return () => {
      window.removeEventListener("mousemove", handleMouseMove)
      window.electronAPI?.setIgnoreMouseEvents(false)
    }
  }, [visualMode])

  // Lấy câu nói/stream mới nhất hiển thị phụ đề nổi
  const latestAssistantEntry = [...sage.transcript].reverse().find((t) => t.role === "assistant")
  const latestUserEntry = [...sage.transcript].reverse().find((t) => t.role === "user")

  if (visualMode === "hidden") {
    return <div className="sage-root pointer-events-none opacity-0 select-none" />
  }

  return (
    <div className="sage-root relative min-h-dvh overflow-hidden select-none bg-transparent">
      {/* ========================================================================= */}
      {/* NỀN GIẢ LẬP DESKTOP (Tùy chọn khi xem trên trình duyệt chuẩn Edge/Chrome)  */}
      {/* ========================================================================= */}
      {previewDesktop && (
        <div className="pointer-events-none absolute inset-0 z-0 overflow-hidden bg-gradient-to-br from-[#070b16] via-[#03060c] to-[#010204]">
          {/* Cửa sổ mẫu Desktop Windows */}
          <div className="absolute top-16 left-12 w-[620px] max-w-[90vw] h-[360px] rounded-xl border border-white/10 bg-slate-900/40 p-4 shadow-2xl backdrop-blur-sm">
            <div className="flex items-center gap-2 mb-3 pb-2 border-b border-white/10">
              <span className="size-3 rounded-full bg-red-500/80" />
              <span className="size-3 rounded-full bg-yellow-500/80" />
              <span className="size-3 rounded-full bg-green-500/80" />
              <span className="text-[11px] font-mono text-slate-400 ml-2">pipeline/manager.py — Visual Studio Code</span>
            </div>
            <div className="font-mono text-xs text-slate-400/80 space-y-1">
              <p className="text-purple-400">class TensuraRaphaelManager:</p>
              <p className="pl-4 text-emerald-400">"""Tensura Raphael Real-time Voice & Visual Assistant"""</p>
              <p className="pl-4">def __init__(self):</p>
              <p className="pl-8 text-cyan-300">self.wake_word = OpenWakeWord("raphael") # ~6ms latency</p>
              <p className="pl-8 text-cyan-300">self.vad = SileroVAD()</p>
              <p className="pl-8 text-cyan-300">self.stt = FasterWhisper(device="cuda")</p>
              <p className="pl-8 text-cyan-300">self.tts = VieNeuTTS()</p>
              <p className="pl-8 text-cyan-300">self.rvc = RVCEngine(model="raphael.pth")</p>
              <p className="pl-8 text-cyan-300">self.neo4j = Neo4jCosmosClient()</p>
            </div>
          </div>
          {/* Taskbar Windows ở cạnh dưới */}
          <div className="absolute bottom-0 inset-x-0 h-10 border-t border-white/10 bg-slate-950/70 backdrop-blur-md flex items-center justify-between px-4">
            <div className="flex items-center gap-3 text-xs text-slate-400">
              <span className="font-bold text-sky-400">⊞ Windows</span>
              <span className="px-2 py-0.5 rounded bg-white/10">VS Code</span>
              <span className="px-2 py-0.5 rounded bg-white/10">Terminal</span>
            </div>
            <div className="text-xs text-slate-400 font-mono">
              RTX 2050 · Windows 11
            </div>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* CHẾ ĐỘ 1: FULLSCREEN TRANSPARENT OVERLAY (Mặc định khi thức dậy)          */}
      {/* ========================================================================= */}
      {visualMode === "overlay" && (
        <div className="fixed inset-0 z-40 flex flex-col justify-between p-4 sm:p-6 bg-transparent pointer-events-none transition-all duration-500 animate-in fade-in">
          {/* Vũ trụ sao tri thức Neo4j Cosmos Graph (Nằm ở lớp nền đằng sau Orb) */}
          <GraphBackground
            pulse={sage.graphPulse}
            accent={accent}
            className="pointer-events-none absolute inset-0 h-full w-full opacity-75"
          />

          {/* Góc trên bên phải: Các nút chuyển chế độ & Nút Ẩn ngầm */}
          <header className="relative z-10 flex items-center justify-end p-2 pointer-events-auto">
            <div className="flex items-center gap-1.5 rounded-full border border-white/10 bg-black/40 px-2.5 py-1 backdrop-blur-md transition-opacity hover:opacity-100 opacity-60">
              <button
                onClick={() => changeVisualMode("overlay")}
                className={`grid size-7 place-items-center rounded-full transition-colors ${
                  visualMode === "overlay"
                    ? "bg-white/20 text-white"
                    : "text-white/60 hover:bg-white/10 hover:text-white"
                }`}
                title="Toàn màn hình (Overlay)"
              >
                <Maximize2 className="size-3.5" />
              </button>
              <button
                onClick={() => changeVisualMode("docked")}
                className="grid size-7 place-items-center rounded-full text-white/60 hover:bg-white/10 hover:text-white transition-colors"
                title="Thu nhỏ về góc (Mini Orb)"
              >
                <Minimize2 className="size-3.5" />
              </button>
              <button
                onClick={() => changeVisualMode("sidedeck")}
                className="grid size-7 place-items-center rounded-full text-white/60 hover:bg-white/10 hover:text-white transition-colors"
                title="Thanh bên (Sidedeck Panel)"
              >
                <PanelRight className="size-3.5" />
              </button>
              <button
                onClick={() => setConfigOpen(true)}
                className="grid size-7 place-items-center rounded-full text-white/40 hover:bg-white/10 hover:text-white transition-colors ml-1 border-l border-white/10 pl-1"
                title="Cài đặt kết nối"
              >
                <Settings className="size-3" />
              </button>
              <button
                onClick={() => changeVisualMode("hidden")}
                className="grid size-7 place-items-center rounded-full text-white/40 hover:bg-rose-500/20 hover:text-rose-300 transition-colors ml-1 border-l border-white/10 pl-1"
                title="Ẩn hoàn toàn (Chạy ngầm - Phím tắt: Escape hoặc Ctrl+Alt+Shift+\)"
              >
                <X className="size-3.5" />
              </button>
            </div>
          </header>

          {/* Vùng Trung Tâm: Duy nhất quả cầu Raphael Orb & Phụ Đề Stream */}
          <div className="relative z-10 flex flex-1 flex-col items-center justify-center my-auto pointer-events-auto">
            <div className="relative flex size-[420px] sm:size-[520px] items-center justify-center">
              <SageCoreCanvas
                state={sage.state}
                levels={sage.levels}
                showCircleBorder={false}
                className="size-full"
              />
              <div className="pointer-events-none absolute -top-2">
                <StateBadge state={sage.state} />
              </div>
            </div>

            {/* Khung phụ đề văn bản hiển thị câu trả lời dạng stream gối đầu */}
            {(latestAssistantEntry || latestUserEntry) && (
              <div className="mt-2 max-w-2xl px-6 py-3.5 rounded-2xl border border-white/12 bg-black/65 backdrop-blur-xl shadow-2xl text-center transition-all animate-in fade-in slide-in-from-bottom-2">
                {latestUserEntry && sage.state !== "speaking" && (
                  <p className="text-xs text-[color:var(--sage-text-dim)] mb-1">
                    <span className="text-[color:var(--sage-cyan)] font-medium">Rimuru: </span>
                    {latestUserEntry.text}
                  </p>
                )}
                {latestAssistantEntry && (
                  <p className="text-sm font-medium leading-relaxed text-[color:var(--sage-text)]">
                    <span className="text-[color:var(--sage-gold)] font-semibold tracking-wider">&lt;&lt; BÁO CÁO &gt;&gt; </span>
                    {latestAssistantEntry.text}
                    {latestAssistantEntry.streaming && (
                      <span className="inline-block w-1.5 h-3.5 ml-1 bg-[color:var(--sage-cyan)] animate-pulse" />
                    )}
                  </p>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* CHẾ ĐỘ 2: CORNER DOCKED MINI ORB (Chỉ còn quả cầu trôi nổi ở góc)        */}
      {/* Khớp chính xác với hình ảnh mod Great Sage HUD người dùng cung cấp         */}
      {/* ========================================================================= */}
      {visualMode === "docked" && (
        <div
          className={
            isElectron
              ? "size-full flex flex-col items-center justify-center p-2 z-50 group pointer-events-auto select-none transition-all duration-300 animate-in fade-in zoom-in-95"
              : "fixed bottom-8 right-8 z-50 group pointer-events-auto transition-all duration-500 animate-in fade-in zoom-in-95"
          }
        >
          {/* Quả cầu tròn với viền tím phát quang đặc trưng, co dãn hào quang theo âm lượng mic thực tế */}
          <div
            onClick={() => changeVisualMode("overlay")}
            className="relative flex size-28 cursor-pointer items-center justify-center rounded-full border-2 transition-all duration-300 hover:scale-105 active:scale-95"
            style={{
              borderColor: `${accent}ee`,
              background: `radial-gradient(circle at center, rgba(14, 20, 36, 0.75) 0%, rgba(4, 6, 12, 0.88) 100%)`,
              boxShadow: `0 0 ${25 + sage.levels.mic_level * 50}px ${accent}${Math.min(99, Math.round(40 + sage.levels.mic_level * 59))}, inset 0 0 20px ${accent}33`,
            }}
            title="Raphael Mini Orb · Nhấp để bung toàn màn hình hoặc gọi 'Raphael!'"
          >
            <SageCoreCanvas
              state={sage.state}
              levels={sage.levels}
              showCircleBorder={true}
              className="size-24"
            />

            {/* Đèn chỉ báo trạng thái ở góc dưới quả cầu */}
            <div
              className="absolute bottom-1 right-1 size-3 rounded-full border-2 border-[#060810]"
              style={{
                backgroundColor: sage.status === "connected" ? accent : "#f59e0b",
                boxShadow: `0 0 10px ${sage.status === "connected" ? accent : "#f59e0b"}`,
              }}
              title={
                sage.status === "connected"
                  ? `Trạng thái: ${sage.state} (Đang lắng nghe: 'Raphael')`
                  : "Đang kết nối WebSocket Core Pipeline..."
              }
            />

            {/* Các nút thao tác nhanh khi rê chuột vào Mini Orb */}
            <div className="absolute -top-2 -right-2 flex items-center gap-1.5 opacity-0 group-hover:opacity-100 transition-opacity">
              <button
                onClick={(e) => {
                  e.stopPropagation()
                  changeVisualMode("overlay")
                }}
                className="grid size-6 place-items-center rounded-full border border-white/20 bg-black/85 text-white/90 shadow-lg hover:bg-white/25 hover:text-white"
                title="Bung toàn màn hình (Overlay)"
              >
                <Maximize2 className="size-3" />
              </button>
              <button
                onClick={(e) => {
                  e.stopPropagation()
                  changeVisualMode("sidedeck")
                }}
                className="grid size-6 place-items-center rounded-full border border-white/20 bg-black/85 text-white/90 shadow-lg hover:bg-white/25 hover:text-white"
                title="Mở Sidedeck Panel"
              >
                <PanelRight className="size-3" />
              </button>
              <button
                onClick={(e) => {
                  e.stopPropagation()
                  changeVisualMode("hidden")
                }}
                className="grid size-6 place-items-center rounded-full border border-white/20 bg-black/85 text-white/90 shadow-lg hover:bg-rose-500/30 hover:text-rose-300"
                title="Ẩn hoàn toàn (Chạy ngầm)"
              >
                <X className="size-3" />
              </button>
            </div>
          </div>

          {/* Nhãn trạng thái trực quan bên dưới Mini Orb */}
          <div className="mt-1 flex items-center gap-1.5 rounded-full border border-white/10 bg-black/85 px-2.5 py-0.5 text-[9px] font-mono text-white/80 shadow-md">
            <span
              className={`size-1.5 rounded-full ${
                sage.status === "connected"
                  ? sage.levels.mic_level > 0.04
                    ? "bg-cyan-400 animate-ping"
                    : "bg-emerald-400"
                  : "bg-amber-400 animate-pulse"
              }`}
            />
            <span className="truncate max-w-[130px]">
              {sage.status === "connected"
                ? sage.levels.mic_level > 0.04
                  ? "🎙️ Đang nhận âm..."
                  : "Nghe: 'Raphael'"
                : "Kết nối Pipeline..."}
            </span>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* CHẾ ĐỘ 3: GEMINI SIDEDECK PANEL (Trượt ra từ cạnh phải, Orb ngự trên đầu)  */}
      {/* ========================================================================= */}
      {visualMode === "sidedeck" && (
        <div className={`fixed inset-0 z-50 flex ${isElectron ? "justify-stretch" : "justify-end"} bg-transparent animate-in fade-in`}>
          {/* Nền trong suốt bên trái chỉ hiện khi chạy trên trình duyệt web thông thường */}
          {!isElectron && (
            <div
              className="flex-1 cursor-pointer pointer-events-auto"
              onClick={() => changeVisualMode("docked")}
              title="Nhấp vào vùng trống để thu nhỏ về góc"
            />
          )}

          {/* Sidedeck Panel bên phải (Thiết kế Matte Architectural Minimalist, bo góc nhẹ 8px thanh thoát) */}
          <aside className="relative pointer-events-auto flex h-dvh w-full md:w-[440px] flex-col rounded-l-lg border-l border-zinc-800/80 bg-[#090a10] shadow-2xl transition-all animate-in slide-in-from-right">
            {/* Header Sidedeck với Quả cầu Raphael Orb ngự trị ngay phía trên */}
            <header className="flex items-center justify-between border-b border-zinc-800/70 px-4 py-3 bg-zinc-950/40">
              <div className="flex items-center gap-3">
                <div
                  className="relative size-12 cursor-pointer rounded-full border border-zinc-700/80 transition-transform hover:scale-105"
                  style={{
                    borderColor: `${accent}bb`,
                    background: `radial-gradient(circle at center, rgba(14, 20, 36, 0.8) 0%, rgba(4, 6, 12, 0.9) 100%)`,
                    boxShadow: `0 0 12px ${accent}33`,
                  }}
                  onClick={() => changeVisualMode("overlay")}
                  title="Nhấp để bung toàn màn hình"
                >
                  <SageCoreCanvas
                    state={sage.state}
                    levels={sage.levels}
                    showCircleBorder={true}
                    className="size-full"
                  />
                </div>
                <div>
                  <h2 className="text-xs font-semibold tracking-[0.2em] text-slate-200">
                    RAPHAEL SIDEDECK
                  </h2>
                  <div className="flex items-center gap-1.5 mt-0.5">
                    <StateBadge state={sage.state} />
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-1.5">
                <button
                  onClick={() => changeVisualMode("overlay")}
                  className="grid size-8 place-items-center rounded-md border border-zinc-800 text-slate-400 hover:bg-zinc-800 hover:text-white transition-colors"
                  title="Bung toàn màn hình"
                >
                  <Maximize2 className="size-3.5" />
                </button>
                <button
                  onClick={() => changeVisualMode("docked")}
                  className="grid size-8 place-items-center rounded-md border border-zinc-800 text-slate-400 hover:bg-zinc-800 hover:text-white transition-colors"
                  title="Thu nhỏ xuống góc (Chỉ còn quả cầu)"
                >
                  <Minimize2 className="size-3.5" />
                </button>
                <button
                  onClick={() => changeVisualMode("hidden")}
                  className="grid size-8 place-items-center rounded-md border border-zinc-800 text-slate-400 hover:bg-rose-500/20 hover:text-rose-300 transition-colors"
                  title="Ẩn hoàn toàn về chạy ngầm (Escape hoặc Ctrl+Alt+Shift+\)"
                >
                  <X className="size-3.5" />
                </button>
              </div>
            </header>

            {/* Nội dung Sidedeck: Lịch sử hội thoại, Khối Neo4j Graph & Cấu hình Stealth */}
            <div className="flex min-h-0 flex-1 flex-col gap-3 p-4 overflow-y-auto">
              <GraphQueryNote query={sage.graphQuery} />

              {/* Thẻ Cấu hình Khởi động & Hệ thống Chạy Ngầm (Minimalist Matte Style) */}
              <div className="rounded-md border border-zinc-800/80 bg-[#0d0f17] p-3 text-slate-300">
                <div className="flex items-center justify-between">
                  <div className="flex flex-col">
                    <span className="text-xs font-medium text-slate-200">Khởi động cùng Windows</span>
                    <span className="text-[10px] text-slate-400">Túc trực ngầm khi mở máy tính</span>
                  </div>
                  <button
                    type="button"
                    onClick={handleToggleStartup}
                    className={`relative inline-flex h-5 w-9 shrink-0 cursor-pointer items-center rounded-full transition-colors duration-200 ${
                      startupEnabled ? "bg-sky-500" : "bg-zinc-700"
                    }`}
                    title="Bật/Tắt tự động chạy ngầm khi khởi động Windows"
                  >
                    <span
                      className={`inline-block size-3.5 transform rounded-full bg-white shadow transition-transform duration-200 ${
                        startupEnabled ? "translate-x-4" : "translate-x-1"
                      }`}
                    />
                  </button>
                </div>

                {/* Cụm Micro-Actions Hệ thống */}
                <div className="mt-3 flex items-center justify-between border-t border-zinc-800/60 pt-2 text-[10px] font-mono text-slate-400">
                  <div className="flex items-center gap-1.5" title="Cổng kết nối WebSocket Audio Pipeline">
                    <Activity className="size-3 text-sky-400" />
                    <span>ws://127.0.0.1:8765</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => changeVisualMode("hidden")}
                      className="flex items-center gap-1 rounded px-1.5 py-0.5 hover:bg-zinc-800 hover:text-slate-200 transition-colors"
                      title="Ẩn về chế độ chạy ngầm (Escape hoặc Ctrl+Alt+Shift+\)"
                    >
                      <EyeOff className="size-3" />
                      <span>Ẩn ngầm</span>
                    </button>
                    <button
                      onClick={() => {
                        if (typeof window !== "undefined" && window.electronAPI?.stopAssistant) {
                          window.electronAPI.stopAssistant()
                        }
                      }}
                      className="flex items-center gap-1 rounded px-1.5 py-0.5 hover:bg-rose-950/40 hover:text-rose-300 transition-colors"
                      title="Dừng toàn bộ tiến trình Raphael"
                    >
                      <Power className="size-3" />
                      <span>Dừng</span>
                    </button>
                  </div>
                </div>
              </div>

              <ConversationPanel
                transcript={sage.transcript}
                className="flex min-h-[300px] flex-1 flex-col overflow-hidden rounded-md border border-zinc-800/80 bg-black/60"
              />
            </div>

            {/* Footer Sidedeck: Khung chat và điều khiển micro */}
            <footer className="border-t border-white/10 p-3 bg-black/60">
              <ControlPanel
                isListening={sage.isListening}
                outputMode={sage.outputMode}
                onStartListen={sage.startListen}
                onStopListen={sage.stopListen}
                onSendText={sage.sendText}
                onSetMode={sage.setOutputMode}
              />
            </footer>
          </aside>
        </div>
      )}

      {/* Modal Cài Đặt */}
      <ConfigDialog
        open={configOpen}
        url={sage.url}
        onClose={() => setConfigOpen(false)}
        onSave={sage.updateConfig}
      />
    </div>
  )
}

/* --------------------------- sub panels --------------------------- */

function GraphQueryNote({
  query,
}: {
  query: ReturnType<typeof useSageSocket>["graphQuery"]
}) {
  return (
    <NoteFrame title="Neo4j Cosmos Retrieval" jp="知識探索" icon={<Network className="size-4" />}>
      {query ? (
        <div className="space-y-2">
          <div className="flex items-center justify-between text-[11px] text-[color:var(--sage-text-dim)]">
            <span>Neo4j Cypher Traversal</span>
            <span className="rounded-full bg-white/5 px-2 py-0.5 font-mono text-[color:var(--sage-cyan)]">
              {query.hits ?? query.nodes?.length ?? 0} active stars
            </span>
          </div>
          <pre className="sage-scroll max-h-20 overflow-auto whitespace-pre-wrap break-words rounded-lg border border-white/8 bg-black/60 p-2 font-mono text-[10px] leading-relaxed text-[color:var(--sage-cyan-deep)]">
            {query.query ?? "MATCH (n)-[r]->(m) RETURN n, r, m"}
          </pre>
          {query.nodes && query.nodes.length > 0 && (
            <div className="flex flex-wrap gap-1">
              {query.nodes.map((n) => (
                <span
                  key={n.id}
                  className="rounded-md border border-[color:var(--sage-cyan-deep)]/40 bg-[color:var(--sage-cyan-deep)]/15 px-2 py-0.5 text-[10px] text-[color:var(--sage-cyan)] font-mono"
                >
                  ✦ {n.label ?? n.id}
                </span>
              ))}
            </div>
          )}
        </div>
      ) : (
        <p className="py-2 text-center text-[11px] text-[color:var(--sage-text-dim)]">
          Mạng lưới sao Neo4j đang ở trạng thái cân bằng. Sẽ bừng sáng khi truy vấn.
        </p>
      )}
    </NoteFrame>
  )
}
