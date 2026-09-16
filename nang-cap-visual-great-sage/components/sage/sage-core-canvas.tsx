"use client"

import { useEffect, useRef } from "react"
import { STATE_META, type AudioLevels, type SageState } from "@/lib/sage-protocol"

// Icosahedron golden-ratio vertices (from the original SageCore algorithm).
const PHI = 1.618034
const RAW = [
  [-1, PHI, 0], [1, PHI, 0], [-1, -PHI, 0], [1, -PHI, 0],
  [0, -1, PHI], [0, 1, PHI], [0, -1, -PHI], [0, 1, -PHI],
  [PHI, 0, -1], [PHI, 0, 1], [-PHI, 0, -1], [-PHI, 0, 1],
]
const VERTICES = RAW.map((p) => {
  const l = Math.hypot(p[0], p[1], p[2])
  return [p[0] / l, p[1] / l, p[2] / l] as [number, number, number]
})
const FACES = [
  [0, 11, 5], [0, 5, 1], [0, 1, 7], [0, 7, 10], [0, 10, 11],
  [1, 5, 9], [5, 11, 4], [11, 10, 2], [10, 7, 6], [7, 1, 8],
  [3, 9, 4], [3, 4, 2], [3, 2, 6], [3, 6, 8], [3, 8, 9],
  [4, 9, 5], [2, 4, 11], [6, 2, 10], [8, 6, 7], [9, 8, 1],
]
const EDGES: [number, number][] = (() => {
  const set = new Set<string>()
  const out: [number, number][] = []
  for (const f of FACES) {
    for (let i = 0; i < 3; i++) {
      const a = f[i]
      const b = f[(i + 1) % 3]
      const key = `${Math.min(a, b)}_${Math.max(a, b)}`
      if (!set.has(key)) {
        set.add(key)
        out.push([Math.min(a, b), Math.max(a, b)])
      }
    }
  }
  return out
})()

function project(
  s: [number, number, number],
  yaw: number,
  pitch: number,
): [number, number, number] {
  const cy = Math.cos(yaw)
  const sy = Math.sin(yaw)
  const x = s[0] * cy + s[2] * sy
  const z = -s[0] * sy + s[2] * cy
  const cp = Math.cos(pitch)
  const sp = Math.sin(pitch)
  const y = s[1] * cp - z * sp
  const outZ = s[1] * sp + z * cp
  return [x, y, outZ]
}

interface Props {
  state: SageState
  levels: AudioLevels
  className?: string
  showCircleBorder?: boolean
}

export function SageCoreCanvas({ state, levels, className, showCircleBorder = false }: Props) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const stateRef = useRef(state)
  const levelsRef = useRef(levels)
  const showBorderRef = useRef(showCircleBorder)
  showBorderRef.current = showCircleBorder
  // smoothed values
  const smooth = useRef({ voice: 0, color: STATE_META.idle.color, r: 0, g: 0, b: 0 })

  stateRef.current = state
  levelsRef.current = levels

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext("2d")
    if (!ctx) return

    let raf = 0
    let dpr = 1
    let W = 0
    let H = 0

    const resize = () => {
      const rect = canvas.getBoundingClientRect()
      dpr = Math.min(window.devicePixelRatio || 1, 2)
      W = Math.max(1, Math.round(rect.width))
      H = Math.max(1, Math.round(rect.height))
      canvas.width = W * dpr
      canvas.height = H * dpr
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    }
    resize()
    const ro = new ResizeObserver(resize)
    ro.observe(canvas)

    const hexToRgb = (hex: string) => {
      const h = hex.replace("#", "")
      return {
        r: parseInt(h.slice(0, 2), 16),
        g: parseInt(h.slice(2, 4), 16),
        b: parseInt(h.slice(4, 6), 16),
      }
    }
    // init smoothed color
    {
      const c = hexToRgb(STATE_META.idle.color)
      smooth.current.r = c.r
      smooth.current.g = c.g
      smooth.current.b = c.b
    }

    const rgba = (r: number, g: number, b: number, a: number) =>
      `rgba(${r | 0},${g | 0},${b | 0},${a})`

    const drawGlow = (
      x: number,
      y: number,
      radius: number,
      color: string,
      alpha: number,
    ) => {
      if (alpha <= 0.005 || radius <= 0.5) return
      ctx.save()
      ctx.globalCompositeOperation = "lighter"
      const grad = ctx.createRadialGradient(x, y, 0, x, y, radius)
      grad.addColorStop(0, color)
      grad.addColorStop(0.35, color)
      grad.addColorStop(1, "rgba(0,0,0,0)")
      ctx.globalAlpha = Math.min(1, alpha)
      ctx.fillStyle = grad
      ctx.beginPath()
      ctx.arc(x, y, radius, 0, Math.PI * 2)
      ctx.fill()
      ctx.restore()
    }

    const drawBeam = (
      x1: number,
      y1: number,
      x2: number,
      y2: number,
      width: number,
      color: string,
      alpha: number,
    ) => {
      if (alpha <= 0.005) return
      ctx.save()
      ctx.globalCompositeOperation = "lighter"
      ctx.globalAlpha = Math.min(1, alpha)
      ctx.strokeStyle = color
      ctx.lineWidth = width
      ctx.lineCap = "round"
      ctx.beginPath()
      ctx.moveTo(x1, y1)
      ctx.lineTo(x2, y2)
      ctx.stroke()
      ctx.restore()
    }

    const start = performance.now()

    const frame = () => {
      raf = requestAnimationFrame(frame)
      const time = performance.now() - start
      const st = stateRef.current
      const lv = levelsRef.current

      // target voice from mic/speaker energy
      const targetVoice = Math.min(
        1,
        Math.max(lv.mic_level, lv.speaker_level, st === "thinking" ? 0.35 : 0),
      )
      smooth.current.voice += (targetVoice - smooth.current.voice) * 0.12

      // target color from state
      const tc = hexToRgb(STATE_META[st].color)
      smooth.current.r += (tc.r - smooth.current.r) * 0.06
      smooth.current.g += (tc.g - smooth.current.g) * 0.06
      smooth.current.b += (tc.b - smooth.current.b) * 0.06
      const { r, g, b, voice } = smooth.current
      const baseColor = rgba(r, g, b, 1)
      const deepColor = "#8fe9c0"

      ctx.clearRect(0, 0, W, H)

      const cx = W / 2
      const cy = H / 2
      const baseR = Math.min(W, H) * 0.15
      const alpha = 0.95
      const breath = 1.0 + voice * 0.14 + Math.sin(time / 1400) * 0.015
      const scale = baseR * breath

      // --- 1. central star & rays -------------------------------------
      const pulse = 0.85 + 0.15 * Math.sin(time / 240) + voice * 0.65
      const starSize = baseR * 0.38 * pulse
      drawGlow(cx, cy, starSize * 1.8, baseColor, alpha * 0.35)
      drawGlow(cx, cy, starSize * 0.75, "#ffffff", alpha * (0.6 + voice * 0.4))
      for (let i = 0; i < 8; i++) {
        const angle = (i * 45 * Math.PI) / 180 + time * 0.0008
        const len = (i % 2 === 0 ? starSize * 3.2 : starSize * 1.7) * pulse
        drawBeam(
          cx,
          cy,
          cx + Math.cos(angle) * len,
          cy + Math.sin(angle) * len,
          3.0,
          "#ffffff",
          alpha * 0.4,
        )
      }

      // --- 2. radiating rays (fade smoothly before edge to prevent any clipping) ---
      const rays = 18
      for (let ri = 0; ri < rays; ri++) {
        const angle = (ri / rays) * Math.PI * 2 + time * 7.0e-5
        const cos = Math.cos(angle)
        const sin = Math.sin(angle)
        const drift = (time * 0.035) % 20
        const reach = baseR * (2.4 + voice * 0.5)
        for (let d = baseR * 0.75 + drift; d < reach; d += 20) {
          const fade = Math.max(0, 1 - d / reach)
          drawGlow(cx + cos * d, cy + sin * d, 4 + fade * 3, baseColor, alpha * fade * 0.45)
        }
      }

      // --- 2b. circular boundary ring (matching Great Sage mod HUD from user screenshot) ---
      if (showBorderRef.current) {
        ctx.save()
        ctx.strokeStyle = baseColor
        ctx.lineWidth = 1.8 + voice * 1.2
        ctx.globalAlpha = 0.85
        ctx.shadowColor = baseColor
        ctx.shadowBlur = 14
        ctx.beginPath()
        ctx.arc(cx, cy, baseR * 2.36, 0, Math.PI * 2)
        ctx.stroke()
        ctx.restore()
      }

      // --- 3. orbit rings ---------------------------------------------
      const beads = 54
      for (let ring = 0; ring < 3; ring++) {
        const tilt = 0.5 + ring * 0.75
        const spin = time * (4.2e-4 - ring * 1.1e-4) * (ring % 2 === 0 ? 1 : -1)
        const ringRadius = baseR * (1.02 + ring * 0.16) * breath
        const c = ring === 1 ? baseColor : deepColor
        for (let i = 0; i < beads; i++) {
          if (i % 3 === 2) continue
          const angle = (Math.PI * 2 * i) / beads + spin
          const pt = project([Math.cos(angle), 0, Math.sin(angle)], ring * 1.1, tilt)
          const weight = 0.28 + 0.72 * (pt[2] + 1) * 0.5
          drawGlow(
            cx + pt[0] * ringRadius,
            cy + pt[1] * ringRadius,
            5 * weight,
            c,
            alpha * 0.75 * weight,
          )
        }
      }

      // --- 4. icosahedron core frame ----------------------------------
      const yaw = time * 3.4e-4
      const pitch = 0.42 + Math.sin(time * 2.1e-4) * 0.28
      const screen = VERTICES.map((v) => project(v, yaw, pitch))
      for (const [ea, eb] of EDGES) {
        const a = screen[ea]
        const bb = screen[eb]
        const depth = (a[2] + bb[2]) * 0.5
        const weight = 0.3 + 0.7 * (depth + 1) * 0.5
        drawBeam(
          cx + a[0] * scale,
          cy + a[1] * scale,
          cx + bb[0] * scale,
          cy + bb[1] * scale,
          2.5 * weight,
          baseColor,
          alpha * 0.65 * weight,
        )
      }
      for (const v of screen) {
        const weight = 0.35 + 0.65 * (v[2] + 1) * 0.5
        drawGlow(cx + v[0] * scale, cy + v[1] * scale, 8 * weight, "#ffffff", alpha * 0.7 * weight)
      }

      // --- 5. floating motes ------------------------------------------
      for (let i = 0; i < 30; i++) {
        const angle = i * 2.399963 + time * 1.3e-4 * ((i % 3) + 1)
        const drift = (time / 4200 + i * 0.6180339) % 1
        const dist = baseR * (1.1 + drift * 1.9)
        const fade = Math.sin(drift * Math.PI)
        drawGlow(
          cx + Math.cos(angle) * dist,
          cy + Math.sin(angle) * dist * 0.72,
          4 + fade * 4,
          deepColor,
          alpha * fade * 0.6,
        )
      }
    }

    raf = requestAnimationFrame(frame)
    return () => {
      cancelAnimationFrame(raf)
      ro.disconnect()
    }
  }, [])

  return <canvas ref={canvasRef} className={className} aria-hidden="true" />
}
