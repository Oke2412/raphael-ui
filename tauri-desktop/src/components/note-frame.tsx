"use client"

import type { ReactNode } from "react"
import { cn } from "@/lib/utils"

interface NoteFrameProps {
  title?: string
  jp?: string
  icon?: ReactNode
  right?: ReactNode
  children: ReactNode
  className?: string
  bodyClassName?: string
  accent?: string
}

/**
 * "Khung note" — a translucent HUD panel used across the console.
 * Renders a corner-bracketed frame with an optional titled header.
 */
export function NoteFrame({
  title,
  jp,
  icon,
  right,
  children,
  className,
  bodyClassName,
  accent = "#8fe9c0",
}: NoteFrameProps) {
  return (
    <section
      className={cn("sage-panel relative rounded-xl", className)}
      style={{ ["--accent" as string]: accent }}
    >
      {/* corner brackets */}
      <Corner className="left-0 top-0 border-l border-t" />
      <Corner className="right-0 top-0 border-r border-t" />
      <Corner className="bottom-0 left-0 border-b border-l" />
      <Corner className="bottom-0 right-0 border-b border-r" />

      {(title || right) && (
        <header className="flex items-center justify-between gap-2 border-b border-white/5 px-4 py-2.5">
          <div className="flex items-center gap-2">
            {icon && (
              <span className="grid size-6 place-items-center" style={{ color: accent }}>
                {icon}
              </span>
            )}
            <div className="leading-none">
              {title && (
                <h2 className="text-[11px] font-semibold uppercase tracking-[0.22em] text-[color:var(--sage-text)]">
                  {title}
                </h2>
              )}
              {jp && (
                <span className="text-[10px] tracking-[0.4em] text-[color:var(--sage-text-dim)]">
                  {jp}
                </span>
              )}
            </div>
          </div>
          {right}
        </header>
      )}
      <div className={cn("px-4 py-3", bodyClassName)}>{children}</div>
    </section>
  )
}

function Corner({ className }: { className?: string }) {
  return (
    <span
      className={cn(
        "pointer-events-none absolute size-3 border-[color:var(--accent)]/60",
        className,
      )}
      aria-hidden="true"
    />
  )
}
