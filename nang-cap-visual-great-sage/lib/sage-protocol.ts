// ------------------------------------------------------------------
// Great Sage <-> UI WebSocket protocol
// Default endpoint: ws://127.0.0.1:8765
// ------------------------------------------------------------------

export const DEFAULT_WS_URL = "ws://127.0.0.1:8765"

export type SageState =
  | "idle"
  | "listening"
  | "transcribing"
  | "thinking"
  | "speaking"

export type OutputMode = "voice_and_text" | "text_only" | "voice_only"

export type ConnectionStatus =
  | "connecting"
  | "connected"
  | "disconnected"
  | "error"
  | "demo"

export interface AudioLevels {
  mic_level: number
  speaker_level: number
  spectrum: number[]
}

// A node/edge snapshot for the Neo4j retrieval effect.
export interface GraphNode {
  id: string
  label?: string
}
export interface GraphRel {
  source: string
  target: string
}
export interface GraphQueryPayload {
  query?: string
  nodes?: GraphNode[]
  rels?: GraphRel[]
  hits?: number
}

// ------------------------------- Events (server -> UI) ---------------
export type ServerEvent =
  | { type: "STATE_CHANGE"; state: SageState }
  | { type: "WAKE_WORD_DETECTED"; keyword: string }
  | ({ type: "AUDIO_LEVELS" } & AudioLevels)
  | { type: "USER_TRANSCRIPT"; text: string }
  | { type: "ASSISTANT_STREAM"; token: string; done?: boolean }
  | { type: "ERROR"; message: string }
  | ({ type: "GRAPH_QUERY" } & GraphQueryPayload)

// ------------------------------- Commands (UI -> server) -------------
export type ClientCommand =
  | { type: "START_LISTEN" }
  | { type: "STOP_LISTEN" }
  | { type: "SEND_TEXT_PROMPT"; text: string }
  | { type: "SET_OUTPUT_MODE"; mode: OutputMode }
  | { type: "UPDATE_CONFIG"; url: string }

// Defensive parser — tolerant of minor shape differences from the pipeline.
export function parseServerEvent(raw: unknown): ServerEvent | null {
  if (typeof raw !== "object" || raw === null) return null
  const obj = raw as Record<string, unknown>
  const type = obj.type
  if (typeof type !== "string") return null

  switch (type) {
    case "STATE_CHANGE": {
      const state = (obj.state ?? obj.payload) as SageState
      return { type, state }
    }
    case "WAKE_WORD_DETECTED": {
      return { type, keyword: String(obj.keyword ?? "raphael") }
    }
    case "AUDIO_LEVELS":
      return {
        type,
        mic_level: Number(obj.mic_level ?? 0),
        speaker_level: Number(obj.speaker_level ?? 0),
        spectrum: Array.isArray(obj.spectrum)
          ? (obj.spectrum as number[]).map(Number)
          : [],
      }
    case "USER_TRANSCRIPT":
      return { type, text: String(obj.text ?? obj.payload ?? "") }
    case "ASSISTANT_STREAM":
      return {
        type,
        token: String(obj.token ?? obj.text ?? obj.payload ?? ""),
        done: Boolean(obj.done),
      }
    case "ERROR":
      return { type, message: String(obj.message ?? obj.payload ?? "Unknown error") }
    case "GRAPH_QUERY":
      return {
        type,
        query: obj.query ? String(obj.query) : undefined,
        nodes: Array.isArray(obj.nodes) ? (obj.nodes as GraphNode[]) : undefined,
        rels: Array.isArray(obj.rels) ? (obj.rels as GraphRel[]) : undefined,
        hits: obj.hits != null ? Number(obj.hits) : undefined,
      }
    default:
      return null
  }
}

export const STATE_META: Record<
  SageState,
  { label: string; jp: string; color: string; hint: string }
> = {
  idle: {
    label: "Idle",
    jp: "待機",
    color: "#dffcec",
    hint: "Great Sage đang chờ lệnh",
  },
  listening: {
    label: "Listening",
    jp: "傾聴",
    color: "#8fe9c0",
    hint: "Đang lắng nghe micro",
  },
  transcribing: {
    label: "Transcribing",
    jp: "解析",
    color: "#7fd0ff",
    hint: "Đang chuyển giọng nói thành văn bản",
  },
  thinking: {
    label: "Thinking",
    jp: "思考",
    color: "#b070ff",
    hint: "Truy xuất tri thức & suy luận",
  },
  speaking: {
    label: "Speaking",
    jp: "解答",
    color: "#ffe9a8",
    hint: "Raphael đang phúc đáp",
  },
}
