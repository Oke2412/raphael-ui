"use client"

import { useEffect, useState } from "react"
import { Network, X } from "lucide-react"

export function ConfigDialog({
  open,
  url,
  onClose,
  onSave,
}: {
  open: boolean
  url: string
  onClose: () => void
  onSave: (url: string) => void
}) {
  const [value, setValue] = useState(url)

  useEffect(() => {
    if (open) setValue(url)
  }, [open, url])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose()
    }
    if (open) window.addEventListener("keydown", onKey)
    return () => window.removeEventListener("keydown", onKey)
  }, [open, onClose])

  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-50 grid place-items-center bg-black/60 p-4 backdrop-blur-sm"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-label="Cấu hình kết nối"
    >
      <div
        className="sage-panel sage-rise w-full max-w-md rounded-2xl p-5"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Network className="size-4 text-[color:var(--sage-cyan-deep)]" />
            <h2 className="text-sm font-semibold uppercase tracking-[0.2em]">
              Cấu hình Tailscale
            </h2>
          </div>
          <button
            onClick={onClose}
            className="grid size-7 place-items-center rounded-md text-[color:var(--sage-text-dim)] transition-colors hover:bg-white/5 hover:text-white"
            aria-label="Đóng"
          >
            <X className="size-4" />
          </button>
        </div>

        <label className="mb-1.5 block text-[11px] uppercase tracking-[0.2em] text-[color:var(--sage-text-dim)]">
          WebSocket URL (server từ xa)
        </label>
        <input
          value={value}
          onChange={(e) => setValue(e.target.value)}
          placeholder="ws://127.0.0.1:8765"
          spellCheck={false}
          className="w-full rounded-lg border border-white/10 bg-black/40 px-3 py-2.5 font-mono text-sm text-[color:var(--sage-text)] outline-none transition-colors focus:border-[color:var(--sage-cyan-deep)]"
        />
        <p className="mt-2 text-[11px] leading-relaxed text-[color:var(--sage-text-dim)]">
          Nhập địa chỉ Tailscale của pipeline, ví dụ{" "}
          <span className="font-mono text-[color:var(--sage-cyan-deep)]">
            ws://100.x.y.z:8765
          </span>
          . Lệnh <span className="font-mono">UPDATE_CONFIG</span> sẽ được gửi và kết nối lại.
        </p>

        <div className="mt-5 flex justify-end gap-2">
          <button
            onClick={onClose}
            className="rounded-lg border border-white/10 px-4 py-2 text-sm text-[color:var(--sage-text-dim)] transition-colors hover:bg-white/5"
          >
            Huỷ
          </button>
          <button
            onClick={() => {
              onSave(value.trim() || url)
              onClose()
            }}
            className="rounded-lg px-4 py-2 text-sm font-semibold text-black transition-transform active:translate-y-px"
            style={{ background: "var(--sage-cyan-deep)" }}
          >
            Lưu & kết nối lại
          </button>
        </div>
      </div>
    </div>
  )
}
