"use client"

import { useState, useEffect, useCallback, useRef } from "react"
import {
  Maximize2,
  Minimize2,
  PanelRight,
  Settings,
  X,
  Activity,
  Sliders,
  Sparkles,
  Layers,
  Terminal,
  ChevronDown,
  ChevronUp,
  Power,
  RefreshCw,
  MessageSquare,
  GripHorizontal,
} from "lucide-react"
import { invoke } from "@tauri-apps/api/core"
import { listen } from "@tauri-apps/api/event"
import { STATE_META } from "@/lib/sage-protocol"
import { useSageSocket } from "@/lib/use-sage-socket"
import { SageCoreCanvas } from "./sage-core-canvas"
import { GraphBackground } from "./graph-background"
import { StateBadge } from "./state-badge"
import { ConversationPanel } from "./conversation-panel"
import { ControlPanel } from "./control-panel"
import { ConfigDialog } from "./config-dialog"

export type VisualMode = "overlay" | "docked" | "sidedeck" | "hidden"

function playHapticTone(type: "click" | "transition" | "wake" = "click") {
  if (typeof window === "undefined") return
  try {
    const AudioCtx =
      window.AudioContext ||
      (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext
    const ctx = new AudioCtx()
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    const now = ctx.currentTime

    if (type === "click") {
      osc.type = "sine"
      osc.frequency.setValueAtTime(320, now)
      osc.frequency.exponentialRampToValueAtTime(140, now + 0.05)
      gain.gain.setValueAtTime(0.06, now)
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.05)
      osc.connect(gain)
      gain.connect(ctx.destination)
      osc.start(now)
      osc.stop(now + 0.05)
    } else if (type === "transition") {
      osc.type = "triangle"
      osc.frequency.setValueAtTime(180, now)
      osc.frequency.exponentialRampToValueAtTime(360, now + 0.12)
      gain.gain.setValueAtTime(0.05, now)
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.12)
      osc.connect(gain)
      gain.connect(ctx.destination)
      osc.start(now)
      osc.stop(now + 0.12)
    } else if (type === "wake") {
      osc.type = "sine"
      osc.frequency.setValueAtTime(587.33, now) // D5
      osc.frequency.exponentialRampToValueAtTime(880, now + 0.14) // A5
      gain.gain.setValueAtTime(0.15, now)
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.45)
      osc.connect(gain)
      gain.connect(ctx.destination)
      osc.start(now)
      osc.stop(now + 0.45)
    }
  } catch (_) {}
}

export function SageConsole() {
  const sage = useSageSocket()
  // Mặc định khởi chạy ngầm 100% Stealth (hidden) trong Tauri, hoặc overlay trên web
  const [visualMode, setVisualMode] = useState<VisualMode>("hidden")
  const [configOpen, setConfigOpen] = useState(false)
  const isTauri = typeof window !== "undefined" && Boolean((window as unknown as { __TAURI_INTERNALS__?: unknown }).__TAURI_INTERNALS__)
  const [showInspector, setShowInspector] = useState(false)
  const [showSettingsDrawer, setShowSettingsDrawer] = useState(false)
  const [missionInput, setMissionInput] = useState("")
  const [startupEnabled, setStartupEnabled] = useState(false)
  const handleToggleStartup = () => {
    setStartupEnabled((prev) => !prev)
  }

  const accent = STATE_META[sage.state]?.color ?? "#cbcfd6"

  // Vị trí và logic kéo thả mini orb tự do trên màn hình
  const [dockedCoord, setDockedCoord] = useState<{ x: number; y: number } | null>(null)
  const isDraggingOrb = useRef(false)
  const dragStartOrb = useRef({ x: 0, y: 0, startPos: { x: 0, y: 0 } })
  const hasMovedOrb = useRef(false)

  const handleOrbPointerDown = (e: React.PointerEvent) => {
    if (e.button !== 0) return
    isDraggingOrb.current = true
    hasMovedOrb.current = false
    const currentLeft = dockedCoord?.x ?? (typeof window !== "undefined" ? window.innerWidth - 90 : 0)
    const currentTop = dockedCoord?.y ?? (typeof window !== "undefined" ? window.innerHeight - 90 : 0)
    dragStartOrb.current = {
      x: e.screenX,
      y: e.screenY,
      startPos: { x: currentLeft, y: currentTop },
    }
    try {
      ;(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId)
    } catch {}
  }

  const handleOrbPointerMove = (e: React.PointerEvent) => {
    if (!isDraggingOrb.current) return
    const dx = e.screenX - dragStartOrb.current.x
    const dy = e.screenY - dragStartOrb.current.y
    if (Math.hypot(dx, dy) > 4) {
      hasMovedOrb.current = true
    }
    if (hasMovedOrb.current) {
      if (isTauri) {
        invoke("move_window_by", { dx: Math.round(dx), dy: Math.round(dy) })
        dragStartOrb.current.x = e.screenX
        dragStartOrb.current.y = e.screenY
      } else {
        const newX = Math.max(10, Math.min(window.innerWidth - 85, dragStartOrb.current.startPos.x + dx))
        const newY = Math.max(10, Math.min(window.innerHeight - 85, dragStartOrb.current.startPos.y + dy))
        setDockedCoord({ x: newX, y: newY })
      }
    }
  }

  const handleOrbPointerUp = (e: React.PointerEvent) => {
    if (!isDraggingOrb.current) return
    isDraggingOrb.current = false
    try {
      ;(e.currentTarget as HTMLElement).releasePointerCapture(e.pointerId)
    } catch {}
    if (!hasMovedOrb.current) {
      changeVisualMode("sidedeck")
    }
  }

  const isDraggingSidedeck = useRef(false)
  const dragStartSidedeck = useRef({ x: 0, y: 0 })
  const hasMovedSidedeck = useRef(false)

  const handleSidedeckPointerDown = (e: React.PointerEvent) => {
    if (e.button !== 0) return
    if ((e.target as HTMLElement).closest("button, input, textarea, a, [role='button']")) {
      return
    }
    isDraggingSidedeck.current = true
    hasMovedSidedeck.current = false
    dragStartSidedeck.current = { x: e.screenX, y: e.screenY }
    try {
      ;(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId)
    } catch {}
  }

  const handleSidedeckPointerMove = (e: React.PointerEvent) => {
    if (!isDraggingSidedeck.current) return
    const dx = e.screenX - dragStartSidedeck.current.x
    const dy = e.screenY - dragStartSidedeck.current.y
    if (Math.hypot(dx, dy) > 2) {
      hasMovedSidedeck.current = true
    }
    if (hasMovedSidedeck.current) {
      if (isTauri) {
        invoke("move_window_by", { dx: Math.round(dx), dy: Math.round(dy) })
        dragStartSidedeck.current.x = e.screenX
        dragStartSidedeck.current.y = e.screenY
      }
    }
  }

  const handleSidedeckPointerUp = (e: React.PointerEvent) => {
    if (!isDraggingSidedeck.current) return
    isDraggingSidedeck.current = false
    try {
      ;(e.currentTarget as HTMLElement).releasePointerCapture(e.pointerId)
    } catch {}
  }

  useEffect(() => {
    if (isTauri) {
      let unlisten: (() => void) | null = null
      listen<string>("set-mode-from-backend", (event) => {
        setVisualMode(event.payload as VisualMode)
      }).then((fn) => {
        unlisten = fn
      })
      return () => {
        if (unlisten) unlisten()
      }
    } else {
      // Khi mở trên trình duyệt thường (test web), hiển thị Toàn Cảnh
      setVisualMode("overlay")
    }
  }, [isTauri])

  const changeVisualMode = useCallback(
    (mode: VisualMode) => {
      playHapticTone("transition")
      setVisualMode(mode)
      if (isTauri) {
        invoke("set_visual_mode", { mode })
      }
    },
    [isTauri],
  )

  // Tự động thức dậy mở Overlay toàn màn hình khi wake word được phát hiện
  useEffect(() => {
    if (sage.wakeCount > 0 && visualMode !== "overlay") {
      playHapticTone("wake")
      changeVisualMode("overlay")
    }
  }, [sage.wakeCount, visualMode, changeVisualMode])

  useEffect(() => {
    // Chỉ tự động thức dậy khi micro thực sự bắt đầu lắng nghe từ backend Python thật (không thức dậy ở demo mode)
    if (!sage.demo && sage.status === "connected" && sage.state === "listening" && visualMode === "hidden") {
      playHapticTone("wake")
      changeVisualMode("overlay")
    }
  }, [sage.demo, sage.status, sage.state, visualMode, changeVisualMode])

  // Lắng nghe phím tắt toàn cục bên trong web: Escape (ẩn ngầm 100%) và Ctrl+Space (chuyển mode)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.code === "Space") {
        e.preventDefault()
        if (visualMode === "overlay") changeVisualMode("docked")
        else if (visualMode === "docked") changeVisualMode("sidedeck")
        else changeVisualMode("overlay")
      }
      if (e.key === "Escape") {
        e.preventDefault()
        changeVisualMode("hidden")
      }
    }
    window.addEventListener("keydown", handleKeyDown)
    return () => window.removeEventListener("keydown", handleKeyDown)
  }, [visualMode, changeVisualMode])

  // Bắt đầu phiên làm việc từ Mission Input
  const handleStartMission = (targetMode: VisualMode = "docked") => {
    playHapticTone("click")
    const text = missionInput.trim() || "Bắt đầu phiên làm việc cùng Raphael"
    sage.sendText(text)
    setMissionInput("")
    changeVisualMode(targetMode)
  }

  // Lấy câu nói/stream mới nhất hiển thị phụ đề nổi
  const latestAssistantEntry = [...sage.transcript].reverse().find((t) => t.role === "assistant")
  const latestUserEntry = [...sage.transcript].reverse().find((t) => t.role === "user")

  if (visualMode === "hidden") {
    return <div className="sage-root pointer-events-none opacity-0 select-none" />
  }

  return (
    <div
      className={
        visualMode === "overlay"
          ? "sage-root fixed inset-0 size-full overflow-hidden select-none bg-transparent pointer-events-auto"
          : "sage-root fixed inset-0 size-full overflow-hidden select-none bg-transparent pointer-events-none"
      }
    >
      {/* ========================================================================= */}
      {/* VŨ TRỤ SAO TRI THỨC NEO4J GRAPH: CHỈ HIỆN KHI Ở TOÀN CẢNH (OVERLAY)       */}
      {/* KHI Ở SIDEDECK HOẶC DOCKED MINI ORB: ẨN 100% ĐỂ GIỮ MÀN HÌNH TRONG SUỐT    */}
      {/* ========================================================================= */}
      {visualMode === "overlay" && (
        <GraphBackground
          pulse={sage.graphPulse}
          accent={accent}
          visible={true}
          className="pointer-events-none fixed inset-0 z-10 size-full"
        />
      )}

      {/* ========================================================================= */}
      {/* MICRO GLASS ICON PILL: THANH ĐIỀU KHIỂN SIÊU MỎNG, XUYÊN THẤU             */}
      {/* CHỈ HIỆN Ở TOÀN CẢNH, TỰ ĐỘNG BIẾN MẤT KHI THU VỀ DOCKED HOẶC SIDEDECK   */}
      {/* ========================================================================= */}
      {visualMode === "overlay" && (
        <nav
          className="fixed top-3 left-1/2 -translate-x-1/2 z-50 flex items-center gap-1 px-2 py-1 rounded-[8px] border border-white/[0.08] bg-[rgba(16,18,22,0.35)] hover:bg-[rgba(16,18,22,0.65)] backdrop-blur-md shadow-md morph-transition pointer-events-auto"
        >
          {/* Nút 1: Toàn Cảnh (Active) */}
          <button
            onClick={() => changeVisualMode("overlay")}
            className="btn-hover-glow p-1.5 rounded-[5px] text-white bg-white/[0.1] border border-white/[0.12]"
            title="Toàn Cảnh Mission Control (Ctrl+Space)"
          >
            <Maximize2 className="size-3.5" />
          </button>

          {/* Nút 2: Mini Orb Góc */}
          <button
            onClick={() => changeVisualMode("docked")}
            className="btn-hover-glow p-1.5 rounded-[5px] text-[#8a919e] hover:text-white"
            title="Thu về Quả Cầu Mini Orb ở góc màn hình"
          >
            <Minimize2 className="size-3.5" />
          </button>

          {/* Nút 3: Sidedeck Panel */}
          <button
            onClick={() => changeVisualMode("sidedeck")}
            className="btn-hover-glow p-1.5 rounded-[5px] text-[#8a919e] hover:text-white"
            title="Mở Sidedeck Panel Chat"
          >
            <PanelRight className="size-3.5" />
          </button>

          <div className="h-3 w-px bg-white/10 mx-0.5" />

          {/* Nút Cài đặt */}
          <button
            onClick={() => setConfigOpen(true)}
            className="btn-hover-glow p-1.5 rounded-[5px] text-[#8a919e] hover:text-white"
            title="Cài đặt kết nối WebSocket / Tailscale"
          >
            <Settings className="size-3.5" />
          </button>

          {/* Nút Ẩn Hoàn Toàn về Chạy Ngầm */}
          <button
            onClick={() => changeVisualMode("hidden")}
            className="btn-hover-glow p-1.5 rounded-[5px] text-[#8a919e] hover:text-rose-300 hover:bg-rose-500/20"
            title="Ẩn về chế độ chạy ngầm (Escape hoặc Ctrl+Alt+Shift+\)"
          >
            <X className="size-3.5" />
          </button>
        </nav>
      )}

      {/* ========================================================================= */}
      {/* CHẾ ĐỘ 1: FULLSCREEN MISSION CONTROL (OVERLAY)                            */}
      {/* ========================================================================= */}
      {visualMode === "overlay" && (
        <div className="fixed inset-0 z-30 flex items-center justify-center pointer-events-auto">
          <main className="w-full max-w-5xl h-full flex flex-col justify-between pt-8 pb-16 px-6 pointer-events-auto morph-transition">
          {/* Top HUD: Neo4j nodes count + Status */}
          <div
            data-interactive="true"
            className="flex items-center justify-between text-xs pt-4 pointer-events-auto"
          >
            <div className="flex items-center gap-2">
              <div className="px-2.5 py-1 rounded-[6px] border border-white/[0.06] bg-[rgba(20,22,27,0.45)] backdrop-blur-md flex items-center gap-2 text-[#cbcfd6]">
                <span className="inline-block size-1.5 rounded-[2px] bg-emerald-400" />
                <span className="font-mono text-[11px]">
                  Neo4j: {sage.graphQuery?.hits ?? 1420} Nodes
                </span>
              </div>
              <div className="px-2.5 py-1 rounded-[6px] border border-white/[0.06] bg-[rgba(20,22,27,0.45)] backdrop-blur-md hidden sm:flex items-center gap-1.5 text-[#8a919e] font-mono text-[11px]">
                <span>Knowledge Cosmos</span>
              </div>
            </div>

            <div className="text-[11px] font-mono text-[#8a919e] flex items-center gap-2 bg-[rgba(20,22,27,0.45)] px-2.5 py-1 rounded-[6px] border border-white/[0.06] backdrop-blur-md">
              <StateBadge state={sage.state} />
            </div>
          </div>

          {/* VÙNG TRUNG TÂM: QUẢ CẦU RAPHAEL ORB 3D + PHỤ ĐỀ */}
          <div className="flex flex-col items-center justify-center my-auto text-center relative pointer-events-auto">
            {/* QUẢ CẦU RAPHAEL ORB */}
            <div
              data-interactive="true"
              onClick={() => sage.sendText("Báo cáo trạng thái trợ lý Raphael")}
              className="relative flex items-center justify-center cursor-pointer group"
              title="Nhấn vào quả cầu để kích hoạt giọng nói & xem rung phản hồi"
            >
              {/* Hào quang nhịp thở titan xám khói */}
              <div className="absolute size-72 rounded-full bg-[#383d47]/20 blur-3xl ambient-breath pointer-events-none" />
              <div className="absolute size-52 rounded-full border border-[rgba(200,205,215,0.1)] bg-[rgba(25,28,34,0.12)] backdrop-blur-sm ambient-breath pointer-events-none" />

              {/* Canvas 3D Orb Audio-Reactive */}
              <div className="relative size-64 sm:size-72 flex items-center justify-center">
                <SageCoreCanvas
                  state={sage.state}
                  levels={sage.levels}
                  sizeType="large"
                  showCircleBorder={false}
                  className="size-full"
                />
              </div>
            </div>

            <h1 className="text-2xl sm:text-3xl font-semibold text-[#f3f4f6] tracking-tight mt-1 mb-1 font-sans">
              Raphael Runic Core
            </h1>
            <p className="text-xs sm:text-sm text-[#8a919e] max-w-lg mx-auto font-sans leading-relaxed">
              Quả cầu cộng hưởng âm thanh: hoàn toàn tĩnh lặng khi im lặng, chỉ rung khi có giọng nói phát ra.
            </p>

            {/* Quick Action Chips (Bo góc 6px) */}
            <div className="flex flex-wrap items-center justify-center gap-2 mt-4">
              <button
                onClick={() => sage.sendText("Thử giọng nói và kiểm tra phản hồi âm thanh của Orb")}
                className="btn-hover-glow px-3 py-1.5 rounded-[6px] border border-[rgba(200,205,215,0.18)] bg-[rgba(32,36,44,0.7)] text-white text-xs font-medium transition-all shadow-sm flex items-center gap-1.5"
              >
                <span>🔊</span> Thử giọng nói & rung
              </button>
              <button
                onClick={() => sage.sendText("Nâng cấp kiến trúc Neo4j GraphRAG")}
                className="btn-hover-glow px-3 py-1.5 rounded-[6px] border border-[rgba(200,205,215,0.14)] bg-[rgba(24,27,33,0.6)] text-[#cbcfd6] text-xs font-medium transition-all shadow-sm flex items-center gap-1.5"
              >
                <span>⚡</span> Neo4j GraphRAG
              </button>
              <button
                onClick={() => sage.sendText("Phân tích mã nguồn main.py")}
                className="btn-hover-glow px-3 py-1.5 rounded-[6px] border border-[rgba(200,205,215,0.14)] bg-[rgba(24,27,33,0.6)] text-[#cbcfd6] text-xs font-medium transition-all shadow-sm flex items-center gap-1.5"
              >
                <span>📂</span> Phân tích main.py
              </button>
            </div>

            {/* Khung phụ đề văn bản hiển thị câu trả lời dạng stream gối đầu */}
            {(latestAssistantEntry || latestUserEntry) && (
              <div
                data-interactive="true"
                className="mt-4 max-w-2xl px-5 py-3 rounded-[10px] border border-white/[0.12] bg-[#121419]/85 backdrop-blur-xl shadow-2xl text-center transition-all animate-in fade-in slide-in-from-bottom-2"
              >
                {latestUserEntry && sage.state !== "speaking" && (
                  <p className="text-xs text-[#8a919e] mb-1 font-sans">
                    <span className="text-[#cbcfd6] font-medium font-mono">Rimuru: </span>
                    {latestUserEntry.text}
                  </p>
                )}
                {latestAssistantEntry && (
                  <p className="text-xs sm:text-sm font-medium leading-relaxed text-[#f3f4f6] font-sans">
                    <span className="text-[#e2e4e8] font-mono font-semibold tracking-wider">
                      &lt;&lt; BÁO CÁO &gt;&gt;{" "}
                    </span>
                    {latestAssistantEntry.text}
                    {latestAssistantEntry.streaming && (
                      <span className="inline-block w-1.5 h-3.5 ml-1 bg-sky-300 animate-pulse" />
                    )}
                  </p>
                )}
              </div>
            )}
          </div>

          {/* Thanh nhập mục tiêu phiên làm việc & Nút thu về Mini Orb (Bo góc 10px) */}
          <div data-interactive="true" className="w-full max-w-2xl mx-auto pointer-events-auto">
            <div className="relative flex items-center rounded-[10px] border border-[rgba(200,205,215,0.18)] bg-[rgba(18,20,25,0.75)] backdrop-blur-xl p-1.5 shadow-2xl">
              <span className="pl-3 pr-2 text-[#8a919e]">
                <Sparkles className="size-4" />
              </span>
              <input
                type="text"
                value={missionInput}
                onChange={(e) => setMissionInput(e.target.value)}
                placeholder="Nhập mục tiêu công việc hôm nay..."
                className="flex-1 bg-transparent border-none text-[#f3f4f6] placeholder-[#5a6270] text-sm focus:outline-none px-2 font-sans"
                onKeyDown={(e) => {
                  if (e.key === "Enter") handleStartMission("docked")
                }}
              />

              {/* NÚT 1: THU VỀ QUẢ CẦU MINI ORB GÓC MÀN HÌNH */}
              <button
                onClick={() => handleStartMission("docked")}
                className="btn-hover-glow flex items-center gap-1.5 px-3 py-2 rounded-[6px] bg-[#383d47] hover:bg-[#4e5461] text-white font-medium text-xs tracking-wide shadow-md transition-all mr-1.5"
              >
                <span>🔵 Thu về Mini Orb</span>
              </button>

              {/* NÚT 2: MỞ THẲNG SIDEDECK CHAT */}
              <button
                onClick={() => handleStartMission("sidedeck")}
                className="btn-hover-glow flex items-center gap-1.5 px-3 py-2 rounded-[6px] bg-[#252830] hover:bg-[#383d47] text-[#cbcfd6] border border-white/[0.08] font-medium text-xs transition-all"
              >
                <span>📑 Sidedeck</span>
              </button>
            </div>
            <p className="text-center text-[10px] text-[#69707e] mt-2 font-mono">
              Bấm 'Thu về Mini Orb' để quả cầu bay xuống góc màn hình, thanh điều khiển trên cùng tự động biến mất
            </p>
          </div>
        </main>
      </div>
    )}

      {/* ========================================================================= */}
      {/* CHẾ ĐỘ 2: CORNER DOCKED MINI ORB (Quả cầu Mini Orb trôi nổi góc màn hình)   */}
      {/* ========================================================================= */}
      {visualMode === "docked" && (
        <div
          style={
            !isTauri && dockedCoord
              ? { left: `${dockedCoord.x}px`, top: `${dockedCoord.y}px`, right: "auto", bottom: "auto" }
              : undefined
          }
          className={
            isTauri
              ? "size-full flex flex-col items-center justify-center p-2 z-50 group pointer-events-auto select-none transition-all duration-300 animate-in fade-in"
              : "fixed bottom-6 right-6 z-40 flex flex-col items-center group pointer-events-auto select-none"
          }
        >
          {/* Quả cầu Mini Orb 3D sống động (~68px) - KÉO THẢ TỰ DO KHẮP MÀN HÌNH */}
          <div
            onPointerDown={handleOrbPointerDown}
            onPointerMove={handleOrbPointerMove}
            onPointerUp={handleOrbPointerUp}
            onPointerCancel={handleOrbPointerUp}
            className="relative size-[68px] rounded-full flex items-center justify-center p-1 float-orb cursor-grab active:cursor-grabbing touch-none select-none"
            title="Raphael Mini Orb · Kéo để di chuyển vị trí, nhấp để mở Sidedeck"
          >
            {/* Vòng hào quang titan êm dịu, không tràn viền gây bóng hộp vuông */}
            <div className="absolute inset-0 rounded-full bg-[#383d47]/20 pointer-events-none group-hover:bg-[#4e5461]/35 transition-all" />
            <div className="absolute inset-0 rounded-full border border-[rgba(200,205,215,0.25)] bg-[#12141a]/95" />

            {/* Canvas vẽ 3D Mini Orb */}
            <SageCoreCanvas
              state={sage.state}
              levels={sage.levels}
              sizeType="mini"
              showCircleBorder={true}
              className="relative z-10 size-full pointer-events-none"
            />

            {/* Đèn chỉ báo trạng thái ở góc dưới quả cầu */}
            <div
              className="absolute bottom-1 right-1 size-2.5 rounded-full border border-[#0c0d10]"
              style={{
                backgroundColor: sage.status === "connected" ? accent : "#f59e0b",
                boxShadow: `0 0 8px ${sage.status === "connected" ? accent : "#f59e0b"}`,
              }}
            />
          </div>

          {/* Micro Navigation Bar nổi ngay trên Mini Orb khi hover - GỌN GÀNG, KHÔNG GÂY BÓNG HỘP */}
          <div className="mt-1 flex items-center gap-1 px-1.5 py-1 rounded-[6px] border border-[rgba(200,205,215,0.2)] bg-[#12141a]/95 opacity-0 group-hover:opacity-100 transition-opacity">
            <button
              onClick={(e) => {
                e.stopPropagation()
                changeVisualMode("sidedeck")
              }}
              className="btn-hover-glow p-1.5 rounded-[4px] text-[#cbcfd6] hover:text-white"
              title="Mở Sidedeck Chat"
            >
              <MessageSquare className="size-3.5" />
            </button>
            <div className="h-2.5 w-px bg-white/10" />
            <button
              onClick={(e) => {
                e.stopPropagation()
                changeVisualMode("overlay")
              }}
              className="btn-hover-glow p-1.5 rounded-[4px] text-[#cbcfd6] hover:text-white"
              title="Toàn Cảnh (Mission Control)"
            >
              <Maximize2 className="size-3.5" />
            </button>
            <div className="h-2.5 w-px bg-white/10" />
            <button
              onClick={(e) => {
                e.stopPropagation()
                changeVisualMode("hidden")
              }}
              className="btn-hover-glow p-1.5 rounded-[4px] text-rose-300 hover:bg-rose-500/20"
              title="Ẩn ngầm (Escape)"
            >
              <X className="size-3.5" />
            </button>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* CHẾ ĐỘ 3: AMBIENT SIDEDECK DOCK (BO GÓC 10px SẮC GỌN)                       */}
      {/* ========================================================================= */}
      {visualMode === "sidedeck" && (
        <aside
          className={
            isTauri
              ? "size-full flex flex-col justify-end p-2 sm:p-3 pointer-events-auto select-none transition-all duration-300 animate-in fade-in"
              : "fixed right-4 bottom-4 sm:bottom-6 z-40 w-full max-w-[340px] md:max-w-[360px] pointer-events-auto flex flex-col items-end morph-transition animate-in fade-in slide-in-from-bottom-4"
          }
        >
          {/* FLYOUT INSPECTOR: NEO4J GRAPH METRICS (PROGRESSIVE DISCLOSURE - BO GÓC 10px) */}
          {showInspector && (
            <div className="w-full mb-2 rounded-[10px] border border-[rgba(200,205,215,0.18)] bg-[rgba(20,22,27,0.96)] backdrop-blur-2xl p-3 shadow-[0_12px_40px_rgba(0,0,0,0.6)] morph-transition text-xs">
              <div className="flex items-center justify-between border-b border-white/[0.06] pb-2 mb-2">
                <div className="flex items-center gap-2">
                  <span className="inline-block size-2 rounded-[3px] bg-[#adb3be]" />
                  <span className="font-semibold text-[#cbcfd6]">Neo4j Graph Inspector</span>
                </div>
                <button
                  onClick={() => setShowInspector(false)}
                  className="text-[#8a919e] hover:text-white p-1"
                >
                  <X className="size-3.5" />
                </button>
              </div>
              <div className="space-y-1.5 font-mono text-[11px] text-[#8a919e]">
                <div className="flex justify-between">
                  <span>Graph Traversal:</span>
                  <span className="text-[#cbcfd6]">
                    {sage.graphQuery?.hits ?? 4} nodes active
                  </span>
                </div>
                <div className="flex justify-between">
                  <span>Cypher Query:</span>
                  <span className="text-neutral-200 truncate max-w-[180px]">
                    {sage.graphQuery?.query ?? "MATCH (n)-[r]->(m) RETURN n"}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span>WebSocket Pipeline:</span>
                  <span className="text-emerald-400">
                    {sage.status === "connected" ? "127.0.0.1:8765" : "Demo Mode"}
                  </span>
                </div>
              </div>
            </div>
          )}

          {/* DRAWER CÀI ĐẶT & CHẨN ĐOÁN HỆ THỐNG (BO GÓC 10px) */}
          {showSettingsDrawer && (
            <div className="w-full mb-2 rounded-[10px] border border-[rgba(200,205,215,0.18)] bg-[rgba(20,22,27,0.96)] backdrop-blur-2xl p-3 shadow-[0_12px_40px_rgba(0,0,0,0.6)] morph-transition text-xs">
              <div className="flex items-center justify-between border-b border-white/[0.06] pb-2 mb-2.5">
                <div className="flex items-center gap-2">
                  <Sliders className="size-3.5 text-[#adb3be]" />
                  <span className="font-semibold text-[#cbcfd6]">Điều Khiển & Cài Đặt</span>
                </div>
                <button
                  onClick={() => setShowSettingsDrawer(false)}
                  className="text-[#8a919e] hover:text-white p-1"
                >
                  <X className="size-3.5" />
                </button>
              </div>

              {/* Toggle Windows Startup */}
              <div className="flex items-center justify-between py-1.5">
                <div className="flex flex-col">
                  <span className="text-xs text-[#cbcfd6]">Khởi động cùng Windows</span>
                  <span className="text-[10px] text-[#69707e]">Túc trực ngầm khi mở máy</span>
                </div>
                <button
                  type="button"
                  onClick={handleToggleStartup}
                  className={`relative inline-flex h-5 w-9 shrink-0 cursor-pointer items-center rounded-full transition-colors duration-200 ${
                    startupEnabled ? "bg-[#383d47]" : "bg-neutral-800"
                  }`}
                >
                  <span
                    className={`inline-block size-3.5 transform rounded-full bg-white shadow transition-transform duration-200 ${
                      startupEnabled ? "translate-x-4" : "translate-x-1"
                    }`}
                  />
                </button>
              </div>

              {/* Dừng hoặc Cấu hình Tailscale */}
              <div className="flex items-center justify-between border-t border-white/[0.06] pt-2.5 mt-2">
                <button
                  onClick={() => setConfigOpen(true)}
                  className="btn-hover-glow flex items-center gap-1.5 px-2 py-1 rounded-[5px] border border-white/[0.08] text-[11px] text-[#cbcfd6]"
                >
                  <Settings className="size-3" />
                  <span>Tailscale IP</span>
                </button>
                <button
                  onClick={() => {
                    if (isTauri) {
                      invoke("stop_assistant").catch(console.error)
                    }
                  }}
                  className="btn-hover-glow flex items-center gap-1.5 px-2 py-1 rounded-[5px] bg-rose-950/40 border border-rose-500/20 text-[11px] text-rose-300 hover:bg-rose-900/50"
                >
                  <Power className="size-3" />
                  <span>Dừng Raphael</span>
                </button>
              </div>
            </div>
          )}

          {/* MAIN COMPACT DECK CONTAINER (BO GÓC 10px) */}
          <div className="w-full rounded-[10px] border border-[rgba(200,205,215,0.16)] bg-[rgba(20,22,27,0.88)] backdrop-blur-2xl shadow-[0_16px_50px_rgba(0,0,0,0.55)] p-3 flex flex-col gap-2.5">
            {/* Compact Header với Mini Orb Đang Xoay & Grip kéo di chuyển */}
            <div
              onPointerDown={handleSidedeckPointerDown}
              onPointerMove={handleSidedeckPointerMove}
              onPointerUp={handleSidedeckPointerUp}
              onPointerCancel={handleSidedeckPointerUp}
              className="flex items-center justify-between cursor-grab active:cursor-grabbing select-none"
              title="Kéo thanh tiêu đề để di chuyển Sidedeck"
            >
              <div className="flex items-center gap-2">
                {/* MINI RAPHAEL ORB TRÊN HEADER */}
                <div
                  className="size-7 rounded-[6px] overflow-hidden border border-[rgba(200,205,215,0.25)] bg-black/40 flex items-center justify-center cursor-pointer"
                  onClick={() => changeVisualMode("overlay")}
                  title="Nhấn để bung Toàn Cảnh Mission Control"
                >
                  <SageCoreCanvas
                    state={sage.state}
                    levels={sage.levels}
                    sizeType="header"
                    className="size-full"
                  />
                </div>

                <button
                  onClick={() => setShowInspector(!showInspector)}
                  className="btn-hover-glow flex items-center gap-1.5 px-2 py-1 rounded-[6px] bg-white/[0.04] border border-white/[0.06]"
                  title="Bật/Tắt xem thông số đồ thị Neo4j"
                >
                  <span className="inline-block size-1.5 rounded-full bg-emerald-400 animate-pulse" />
                  <span className="text-[11px] font-mono font-medium text-[#cbcfd6]">
                    {sage.status === "connected" ? "14ms" : "Demo"}
                  </span>
                  <span className="text-[8px] text-[#69707e]">
                    {showInspector ? "▴" : "▾"}
                  </span>
                </button>
              </div>

              {/* GRIP DI CHUYỂN Ở GIỮA */}
              <div
                className="flex-1 h-6 flex items-center justify-center opacity-40 hover:opacity-100 transition-opacity cursor-grab active:cursor-grabbing px-1"
                title="Kéo để di chuyển Sidedeck trên màn hình"
              >
                <GripHorizontal className="size-4 text-[#8a919e]" />
              </div>

              {/* CÁC NÚT ĐIỀU HƯỚNG TÍCH HỢP TRONG SIDEDECK */}
              <div className="flex items-center gap-1">
                <button
                  onClick={() => setShowSettingsDrawer(!showSettingsDrawer)}
                  className="btn-hover-glow p-1 rounded-[6px] text-[#8a919e] hover:text-white"
                  title="Mở bảng điều khiển hệ thống"
                >
                  <Sliders className="size-3.5" />
                </button>
                <button
                  onClick={() => changeVisualMode("overlay")}
                  className="btn-hover-glow p-1 rounded-[6px] text-[#8a919e] hover:text-white"
                  title="Mở rộng Toàn Màn Hình (Overlay)"
                >
                  <Maximize2 className="size-3.5" />
                </button>
                <button
                  onClick={() => changeVisualMode("docked")}
                  className="btn-hover-glow p-1 rounded-[6px] text-[#8a919e] hover:text-white"
                  title="Thu nhỏ về Mini Orb góc màn hình"
                >
                  <Minimize2 className="size-3.5" />
                </button>
                <button
                  onClick={() => changeVisualMode("hidden")}
                  className="btn-hover-glow p-1 rounded-[6px] text-[#8a919e] hover:text-rose-300"
                  title="Ẩn ngầm (Escape)"
                >
                  <X className="size-3.5" />
                </button>
              </div>
            </div>

            {/* Lịch sử hội thoại (ConversationPanel) */}
            <ConversationPanel
              transcript={sage.transcript}
              className="max-h-[260px] min-h-[140px]"
            />

            {/* Khung nhập liệu & Điều khiển âm thanh (ControlPanel) */}
            <ControlPanel
              isListening={sage.isListening}
              outputMode={sage.outputMode}
              onStartListen={sage.startListen}
              onStopListen={sage.stopListen}
              onSendText={sage.sendText}
              onSetMode={sage.setOutputMode}
            />
          </div>
        </aside>
      )}

      {/* Modal Cài Đặt Tailscale */}
      <ConfigDialog
        open={configOpen}
        url={sage.url}
        onClose={() => setConfigOpen(false)}
        onSave={sage.updateConfig}
      />
    </div>
  )
}
