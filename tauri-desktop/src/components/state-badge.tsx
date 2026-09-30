"use client"

import { STATE_META, type ConnectionStatus, type SageState } from "@/lib/sage-protocol"

const STATES: SageState[] = ["idle", "listening", "transcribing", "thinking", "speaking"]

export function StateBadge({ state }: { state: SageState }) {
  const meta = STATE_META[state]
  return (
    <div className="flex items-center gap-3">
      <span className="relative grid size-3 place-items-center">
        <span
          className="absolute inset-0 rounded-full sage-pulse-ring"
          style={{ background: meta.color }}
        />
        <span className="size-2 rounded-full" style={{ background: meta.color }} />
      </span>
      <div className="leading-tight">
        <div className="flex items-baseline gap-2">
          <span
            className="text-sm font-semibold uppercase tracking-[0.18em]"
            style={{ color: meta.color }}
          >
            {meta.label}
          </span>
          <span className="text-[11px] tracking-[0.3em] text-[color:var(--sage-text-dim)]">
            {meta.jp}
          </span>
        </div>
        <p className="text-[11px] text-[color:var(--sage-text-dim)]">{meta.hint}</p>
      </div>
    </div>
  )
}

export function StateRail({ state }: { state: SageState }) {
  return (
    <div className="flex flex-col gap-1.5">
      {STATES.map((s) => {
        const active = s === state
        const meta = STATE_META[s]
        return (
          <div key={s} className="flex items-center gap-2.5">
            <span
              className="h-6 w-[3px] rounded-full transition-all"
              style={{
                background: active ? meta.color : "rgba(147,164,189,0.22)",
                boxShadow: active ? `0 0 10px ${meta.color}` : "none",
              }}
            />
            <span
              className="text-[11px] uppercase tracking-[0.16em] transition-colors"
              style={{ color: active ? meta.color : "var(--sage-text-dim)" }}
            >
              {meta.label}
            </span>
          </div>
        )
      })}
    </div>
  )
}

const STATUS_META: Record<ConnectionStatus, { label: string; color: string }> = {
  connecting: { label: "Đang kết nối…", color: "#ffe9a8" },
  connected: { label: "Pipeline online", color: "#8fe9c0" },
  disconnected: { label: "Đã ngắt", color: "#93a4bd" },
  error: { label: "Lỗi kết nối", color: "#ff5050" },
  demo: { label: "Chế độ Demo", color: "#b070ff" },
}

export function ConnectionPill({ status }: { status: ConnectionStatus }) {
  const meta = STATUS_META[status]
  return (
    <span
      className="inline-flex items-center gap-2 rounded-full border px-3 py-1 text-[11px] font-medium tracking-wide"
      style={{
        borderColor: `${meta.color}55`,
        color: meta.color,
        background: `${meta.color}12`,
      }}
    >
      <span
        className="size-1.5 rounded-full"
        style={{ background: meta.color, boxShadow: `0 0 8px ${meta.color}` }}
      />
      {meta.label}
    </span>
  )
}
