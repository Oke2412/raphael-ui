"use client"

import { useCallback, useEffect, useRef, useState } from "react"
import {
  DEFAULT_WS_URL,
  parseServerEvent,
  type AudioLevels,
  type ClientCommand,
  type ConnectionStatus,
  type GraphQueryPayload,
  type OutputMode,
  type SageState,
  type ServerEvent,
} from "./sage-protocol"

export interface TranscriptEntry {
  id: string
  role: "user" | "assistant"
  text: string
  streaming?: boolean
  ts: number
}

export interface SageLogEntry {
  id: string
  kind: "info" | "error" | "graph"
  text: string
  ts: number
}

const SILENT_LEVELS: AudioLevels = {
  mic_level: 0,
  speaker_level: 0,
  spectrum: new Array(32).fill(0),
}

export interface UseSageSocket {
  status: ConnectionStatus
  state: SageState
  levels: AudioLevels
  transcript: TranscriptEntry[]
  logs: SageLogEntry[]
  graphQuery: GraphQueryPayload | null
  graphPulse: number
  wakeCount: number
  outputMode: OutputMode
  url: string
  demo: boolean
  isListening: boolean
  connect: () => void
  disconnect: () => void
  toggleDemo: (on: boolean) => void
  startListen: () => void
  stopListen: () => void
  sendText: (text: string) => void
  setOutputMode: (mode: OutputMode) => void
  updateConfig: (url: string) => void
}

export function useSageSocket(initialUrl: string = DEFAULT_WS_URL): UseSageSocket {
  const [status, setStatus] = useState<ConnectionStatus>("connecting")
  const [state, setState] = useState<SageState>("idle")
  const [levels, setLevels] = useState<AudioLevels>(SILENT_LEVELS)
  const [transcript, setTranscript] = useState<TranscriptEntry[]>([])
  const [logs, setLogs] = useState<SageLogEntry[]>([])
  const [graphQuery, setGraphQuery] = useState<GraphQueryPayload | null>(null)
  const [graphPulse, setGraphPulse] = useState(0)
  const [wakeCount, setWakeCount] = useState(0)
  const [outputMode, setOutputModeState] = useState<OutputMode>("voice_and_text")
  const [url, setUrl] = useState(initialUrl)
  const [demo, setDemo] = useState(false)
  const [isListening, setIsListening] = useState(false)

  const wsRef = useRef<WebSocket | null>(null)
  const reconnectRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const demoRef = useRef(false)

  const pushLog = useCallback((kind: SageLogEntry["kind"], text: string) => {
    setLogs((prev) =>
      [...prev, { id: crypto.randomUUID(), kind, text, ts: Date.now() }].slice(-60),
    )
  }, [])

  // ---- apply an inbound event to local state ----
  const applyEvent = useCallback(
    (evt: ServerEvent) => {
      switch (evt.type) {
        case "STATE_CHANGE":
          setState(evt.state)
          if (evt.state !== "listening") setIsListening(false)
          break
        case "WAKE_WORD_DETECTED":
          pushLog("info", `Đánh thức bởi từ khóa: "${evt.keyword}"`)
          setWakeCount((c) => c + 1)
          break
        case "AUDIO_LEVELS":
          setLevels({
            mic_level: evt.mic_level,
            speaker_level: evt.speaker_level,
            spectrum: evt.spectrum.length ? evt.spectrum : SILENT_LEVELS.spectrum,
          })
          break
        case "USER_TRANSCRIPT":
          setTranscript((prev) => [
            ...prev,
            { id: crypto.randomUUID(), role: "user", text: evt.text, ts: Date.now() },
          ])
          break
        case "ASSISTANT_STREAM":
          setTranscript((prev) => {
            const last = prev[prev.length - 1]
            if (last && last.role === "assistant" && last.streaming) {
              const updated = [...prev]
              updated[updated.length - 1] = {
                ...last,
                text: last.text + evt.token,
                streaming: !evt.done,
              }
              return updated
            }
            return [
              ...prev,
              {
                id: crypto.randomUUID(),
                role: "assistant",
                text: evt.token,
                streaming: !evt.done,
                ts: Date.now(),
              },
            ]
          })
          break
        case "GRAPH_QUERY":
          setGraphQuery(evt)
          setGraphPulse((p) => p + 1)
          pushLog(
            "graph",
            `Neo4j · ${evt.hits ?? evt.nodes?.length ?? 0} node · ${
              evt.query ?? "MATCH (n) RETURN n"
            }`,
          )
          break
        case "ERROR":
          pushLog("error", evt.message)
          break
      }
    },
    [pushLog],
  )

  // ---- demo simulation (keeps the console alive without a backend) ----
  const demoTimers = useRef<ReturnType<typeof setTimeout>[]>([])
  const demoRaf = useRef<number | null>(null)

  const stopDemo = useCallback(() => {
    demoRef.current = false
    demoTimers.current.forEach(clearTimeout)
    demoTimers.current = []
    if (demoRaf.current) cancelAnimationFrame(demoRaf.current)
    demoRaf.current = null
  }, [])

  const runDemo = useCallback(() => {
    if (demoRef.current) return // already running — avoid a second loop
    demoRef.current = true
    setStatus("demo")
    setState("idle")

    const later = (ms: number, fn: () => void) => {
      const t = setTimeout(fn, ms)
      demoTimers.current.push(t)
    }

    const SCRIPT: { user: string; graph: GraphQueryPayload; answer: string }[] = [
      {
        user: "Great Sage, phân tích cấu trúc tri thức của skill 'Predator'.",
        graph: {
          query: "MATCH (s:Skill {name:'Predator'})-[:EVOLVES_TO*]->(x) RETURN x",
          nodes: [
            { id: "predator", label: "Predator" },
            { id: "gluttony", label: "Gluttony" },
            { id: "beelzebub", label: "Beelzebub" },
            { id: "food", label: "Food Chain" },
          ],
          rels: [
            { source: "predator", target: "gluttony" },
            { source: "gluttony", target: "beelzebub" },
            { source: "predator", target: "food" },
          ],
          hits: 4,
        },
        answer:
          "Báo cáo. Skill «Predator» tiến hóa thành «Gluttony», rồi hợp nhất thành Ultimate Skill «Beelzebub». Chuỗi Food Chain cho phép hấp thu, phân tích và mô phỏng năng lực đối tượng đã nuốt.",
      },
      {
        user: "Tính xác suất chiến thắng nếu đối đầu Charybdis.",
        graph: {
          query: "MATCH (e:Enemy {name:'Charybdis'})-[:WEAK_TO]->(w) RETURN w",
          nodes: [
            { id: "charybdis", label: "Charybdis" },
            { id: "water", label: "Water Blade" },
            { id: "gravity", label: "Gravity" },
            { id: "veldora", label: "Veldora Aura" },
          ],
          rels: [
            { source: "charybdis", target: "water" },
            { source: "charybdis", target: "gravity" },
            { source: "veldora", target: "charybdis" },
          ],
          hits: 4,
        },
        answer:
          "Đề xuất. Xác suất thắng ước tính 91.4%. Charybdis phản ứng với dư âm của Veldora — khuyến nghị triển khai «Gravity Manipulation» kết hợp Water Blade để vô hiệu tái sinh.",
      },
    ]

    let idx = 0
    const cycle = () => {
      if (!demoRef.current) return
      const beat = SCRIPT[idx % SCRIPT.length]
      idx++

      setState("listening")
      setIsListening(true)
      later(2200, () => {
        if (!demoRef.current) return
        setIsListening(false)
        setState("transcribing")
      })
      later(3200, () => {
        if (!demoRef.current) return
        applyEvent({ type: "USER_TRANSCRIPT", text: beat.user })
        setState("thinking")
        applyEvent({ type: "GRAPH_QUERY", ...beat.graph })
      })
      later(5200, () => {
        if (!demoRef.current) return
        // TEXT FIRST — stream the answer while still reasoning.
        const tokens = beat.answer.match(/\S+\s*/g) ?? [beat.answer]
        tokens.forEach((tok, i) => {
          later(i * 85, () => {
            if (!demoRef.current) return
            applyEvent({
              type: "ASSISTANT_STREAM",
              token: tok,
              done: i === tokens.length - 1,
            })
          })
        })
        // VOICE AFTER — once the text is fully shown, play the spoken reply.
        later(tokens.length * 85 + 220, () => {
          if (!demoRef.current) return
          setState("speaking")
        })
        later(tokens.length * 85 + 2400, () => {
          if (!demoRef.current) return
          setState("idle")
          later(2600, cycle)
        })
      })
    }
    later(900, cycle)

    // synthetic audio spectrum
    const animate = () => {
      if (!demoRef.current) return
      const t = performance.now() / 1000
      setState((s) => {
        const active = s === "listening" || s === "speaking" || s === "transcribing"
        const base = active ? 0.55 : 0.08
        const spectrum = Array.from({ length: 32 }, (_, i) => {
          const wobble =
            Math.sin(t * 6 + i * 0.5) * 0.5 +
            Math.sin(t * 2.3 + i * 0.9) * 0.5
          return Math.max(0, base + wobble * (active ? 0.45 : 0.05))
        })
        setLevels({
          mic_level: s === "listening" ? base + Math.sin(t * 9) * 0.3 : 0.05,
          speaker_level: s === "speaking" ? base + Math.sin(t * 7) * 0.3 : 0.05,
          spectrum,
        })
        return s
      })
      demoRaf.current = requestAnimationFrame(animate)
    }
    demoRaf.current = requestAnimationFrame(animate)
  }, [applyEvent])

  // ---- live websocket connection ----
  const cleanupSocket = useCallback(() => {
    if (reconnectRef.current) {
      clearTimeout(reconnectRef.current)
      reconnectRef.current = null
    }
    if (wsRef.current) {
      const ws = wsRef.current
      // Detach the current socket first so its async onclose is treated as
      // stale (identity guard below) and does NOT trigger the demo fallback.
      wsRef.current = null
      try {
        ws.close()
      } catch {
        /* noop */
      }
    }
  }, [])

  const connectTo = useCallback(
    (target: string) => {
      stopDemo()
      cleanupSocket()
      setDemo(false)
      setStatus("connecting")

      let ws: WebSocket
      try {
        ws = new WebSocket(target)
      } catch {
        setStatus("error")
        pushLog("error", `URL không hợp lệ: ${target}`)
        return
      }
      wsRef.current = ws

      const isCurrent = () => wsRef.current === ws

      const failTimer = setTimeout(() => {
        if (isCurrent() && ws.readyState !== WebSocket.OPEN) {
          pushLog("info", "Không thấy pipeline — chuyển sang chế độ Demo.")
          try {
            ws.close()
          } catch {
            /* noop */
          }
        }
      }, 3500)

      ws.onopen = () => {
        if (!isCurrent()) return
        clearTimeout(failTimer)
        setStatus("connected")
        setDemo(false)
        pushLog("info", `Đã kết nối pipeline: ${target}`)
      }
      ws.onmessage = (e) => {
        if (!isCurrent()) return
        try {
          const evt = parseServerEvent(JSON.parse(e.data))
          if (evt) applyEvent(evt)
        } catch {
          /* ignore malformed frame */
        }
      }
      ws.onerror = () => {
        if (!isCurrent()) return
        pushLog("error", "Lỗi kết nối / mất kết nối Tailscale.")
      }
      ws.onclose = () => {
        clearTimeout(failTimer)
        // A stale socket (superseded by reconnect/unmount) must be ignored.
        if (!isCurrent()) return
        wsRef.current = null
        // Fall back to demo so the visuals stay alive.
        setDemo(true)
        runDemo()
      }
    },
    [applyEvent, cleanupSocket, pushLog, runDemo, stopDemo],
  )

  // initial connect on mount
  useEffect(() => {
    connectTo(initialUrl)
    return () => {
      stopDemo()
      cleanupSocket()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // ---- command sender ----
  const send = useCallback(
    (cmd: ClientCommand) => {
      const ws = wsRef.current
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify(cmd))
      }
    },
    [],
  )

  const startListen = useCallback(() => {
    setIsListening(true)
    setState("listening")
    send({ type: "START_LISTEN" })
    pushLog("info", "START_LISTEN")
  }, [send, pushLog])

  const stopListen = useCallback(() => {
    setIsListening(false)
    send({ type: "STOP_LISTEN" })
    pushLog("info", "STOP_LISTEN")
    if (!demoRef.current && status === "connected") setState("transcribing")
  }, [send, pushLog, status])

  const sendText = useCallback(
    (text: string) => {
      const clean = text.trim()
      if (!clean) return
      setTranscript((prev) => [
        ...prev,
        { id: crypto.randomUUID(), role: "user", text: clean, ts: Date.now() },
      ])
      send({ type: "SEND_TEXT_PROMPT", text: clean })
      pushLog("info", `SEND_TEXT_PROMPT: ${clean}`)
      if (demoRef.current) {
        // echo a small acknowledgement in demo mode
        setState("thinking")
        setTimeout(() => {
          applyEvent({
            type: "GRAPH_QUERY",
            query: `CALL db.index.fulltext.queryNodes('kb', '${clean.slice(0, 24)}')`,
            hits: 3,
          })
        }, 400)
        setTimeout(() => {
          setState("speaking")
          const answer = `Đã tiếp nhận truy vấn. Great Sage đang tổng hợp tri thức liên quan tới "${clean}" từ đồ thị Neo4j.`
          const toks = answer.match(/\S+\s*/g) ?? [answer]
          toks.forEach((t, i) =>
            setTimeout(
              () =>
                applyEvent({
                  type: "ASSISTANT_STREAM",
                  token: t,
                  done: i === toks.length - 1,
                }),
              900 + i * 70,
            ),
          )
          setTimeout(() => setState("idle"), 900 + toks.length * 70 + 800)
        }, 1400)
      }
    },
    [send, pushLog, applyEvent],
  )

  const setOutputMode = useCallback(
    (mode: OutputMode) => {
      setOutputModeState(mode)
      send({ type: "SET_OUTPUT_MODE", mode })
      pushLog("info", `SET_OUTPUT_MODE: ${mode}`)
    },
    [send, pushLog],
  )

  const updateConfig = useCallback(
    (nextUrl: string) => {
      setUrl(nextUrl)
      send({ type: "UPDATE_CONFIG", url: nextUrl })
      pushLog("info", `UPDATE_CONFIG: ${nextUrl}`)
      connectTo(nextUrl)
    },
    [send, pushLog, connectTo],
  )

  const connect = useCallback(() => connectTo(url), [connectTo, url])
  const disconnect = useCallback(() => {
    stopDemo()
    cleanupSocket()
    setStatus("disconnected")
  }, [stopDemo, cleanupSocket])

  const toggleDemo = useCallback(
    (on: boolean) => {
      if (on) {
        cleanupSocket()
        setDemo(true)
        runDemo()
      } else {
        stopDemo()
        connectTo(url)
      }
    },
    [cleanupSocket, runDemo, stopDemo, connectTo, url],
  )

  return {
    status,
    state,
    levels,
    transcript,
    logs,
    graphQuery,
    graphPulse,
    wakeCount,
    outputMode,
    url,
    demo,
    isListening,
    connect,
    disconnect,
    toggleDemo,
    startListen,
    stopListen,
    sendText,
    setOutputMode,
    updateConfig,
  }
}
