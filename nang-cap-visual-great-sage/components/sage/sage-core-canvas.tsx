"use client"

import { useEffect, useRef } from "react"
import { STATE_META, type AudioLevels, type SageState } from "@/lib/sage-protocol"

// Icosahedron golden-ratio vertices
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
  sizeType?: "large" | "mini" | "header"
  onClick?: () => void
}

export function SageCoreCanvas({
  state,
  levels,
  className,
  showCircleBorder = false,
  sizeType,
  onClick,
}: Props) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const stateRef = useRef(state)
  const levelsRef = useRef(levels)
  const showBorderRef = useRef(showCircleBorder)
  const sizeTypeRef = useRef(sizeType)
  const audioEnergyRef = useRef(0)

  stateRef.current = state
  levelsRef.current = levels
  showBorderRef.current = showCircleBorder
  sizeTypeRef.current = sizeType

  // smoothed values & continuous rotation angle integration
  const smooth = useRef({
    voice: 0,
    r: 200,
    g: 205,
    b: 215,
  })
  const yawAngleRef = useRef(0)
  const yawSpeedRef = useRef(0.32)
  const peakSpeedRef = useRef(0.32)
  const decelTimerRef = useRef(0)
  const isDeceleratingRef = useRef(false)
  const ringAnglesRef = useRef([0, 0, 0])
  const ringSpeedsRef = useRef([0.35, -0.26, 0.18])
  const lastTimeRef = useRef(0)

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

    const drawGlowDot = (
      ctx: CanvasRenderingContext2D,
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

    const drawBeamLine = (
      ctx: CanvasRenderingContext2D,
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
    lastTimeRef.current = performance.now()

    const frame = () => {
      raf = requestAnimationFrame(frame)
      const now = performance.now()
      const time = now - start
      const dt =
        lastTimeRef.current === 0
          ? 0.016
          : Math.min(Math.max((now - lastTimeRef.current) / 1000, 0.001), 0.05)
      lastTimeRef.current = now

      const st = stateRef.current
      const lv = levelsRef.current

      // TÍNH TOÁN NĂNG LƯỢNG ÂM THANH XUẤT (AUDIO-REACTIVE RESONANCE)
      // CHỈ RUNG KHI CÓ TIẾNG NÓI THỰC TẾ (LỌC BỎ HOÀN TOÀN TẠP ÂM VÀ TIẾNG ỒN NỀN)
      const rawVoice = Math.max(
        lv.speaker_level > 0.02 ? lv.speaker_level : 0,
        st === "speaking" && lv.speaker_level > 0 ? lv.speaker_level : 0,
        st === "listening" && lv.mic_level > 0.04 ? lv.mic_level : 0,
      )

      // Trích xuất năng lượng âm thanh từ phổ tần số giọng nói (vocal bands 1-16)
      let spectrumVocalEnergy = 0
      if (lv.spectrum && lv.spectrum.length > 0 && rawVoice > 0.02) {
        let sum = 0
        const maxBin = Math.min(16, lv.spectrum.length)
        for (let i = 1; i < maxBin; i++) {
          sum += lv.spectrum[i] || 0
        }
        spectrumVocalEnergy = sum / (maxBin - 1)
      }

      // Ngưỡng kích hoạt: strictly 0 khi không có tiếng nói
      const targetEnergy =
        rawVoice > 0.02
          ? Math.min(1.0, Math.max(rawVoice * 1.5, spectrumVocalEnergy * 1.4))
          : 0

      // Fast attack (phản hồi tức thì theo âm tiết phát ra), smooth decay (tắt êm ái khi dứt lời)
      if (targetEnergy > audioEnergyRef.current) {
        audioEnergyRef.current += (targetEnergy - audioEnergyRef.current) * 0.35
      } else {
        audioEnergyRef.current += (targetEnergy - audioEnergyRef.current) * 0.1
      }
      if (audioEnergyRef.current < 0.012) {
        audioEnergyRef.current = 0
      }
      const audioEnergy = audioEnergyRef.current

      // TỐC ĐỘ XOAY & GIẢM TỐC THEO ĐƯỜNG CONG COSINE S-CURVE
      // Tốc độ nền cơ sở chuẩn: 0.32 rad/s (~19.5s một vòng quay hoàn chỉnh khi idle)
      const baseSpeed = 0.32
      const maxBoost = 2.4 // Tăng tốc bốc mạnh mẽ (lên tới ~2.72 rad/s, ~156 deg/s, hoàn thành 1 vòng chỉ trong ~2.3s)

      if (audioEnergy > 0.02) {
        // ĐANG NÓI: TĂNG TỐC SIÊU NHANH (HYPER-RESPONSIVE FAST ATTACK ~50ms)
        const targetSpeed = baseSpeed + audioEnergy * maxBoost
        yawSpeedRef.current += (targetSpeed - yawSpeedRef.current) * Math.min(1.0, 22.0 * dt)
        peakSpeedRef.current = Math.max(peakSpeedRef.current, yawSpeedRef.current)
        decelTimerRef.current = 0
        isDeceleratingRef.current = false
      } else {
        // DỨT LỜI NÓI: GIẢM TỐC ĐỘ THEO ĐƯỜNG CONG COSINE S-CURVE ÊM ÁI
        if (!isDeceleratingRef.current && yawSpeedRef.current > baseSpeed + 0.005) {
          isDeceleratingRef.current = true
          decelTimerRef.current = 0
          peakSpeedRef.current = yawSpeedRef.current
        }

        if (isDeceleratingRef.current) {
          const DECEL_DURATION = 1.4 // 1.4s giảm tốc êm như lụa
          decelTimerRef.current += dt
          const progress = Math.min(1.0, decelTimerRef.current / DECEL_DURATION)
          // Đường cong Cosine S-Curve: (1 + cos(progress * PI)) / 2
          const cosineWeight = (1.0 + Math.cos(progress * Math.PI)) / 2.0
          yawSpeedRef.current = baseSpeed + (peakSpeedRef.current - baseSpeed) * cosineWeight

          if (progress >= 1.0) {
            isDeceleratingRef.current = false
            yawSpeedRef.current = baseSpeed
            peakSpeedRef.current = baseSpeed
            decelTimerRef.current = 0
          }
        } else {
          yawSpeedRef.current += (baseSpeed - yawSpeedRef.current) * Math.min(1.0, 4.0 * dt)
        }
      }

      yawAngleRef.current += yawSpeedRef.current * dt
      const yaw = yawAngleRef.current

      // Góc nghiêng pitch nhịp nhàng theo cosine của góc xoay
      const pitch = 0.38 + Math.cos(yaw * 0.6) * 0.16

      // Target color theo trạng thái hệ thống
      const stateHex = STATE_META[st]?.color ?? "#cbcfd6"
      const tc = hexToRgb(stateHex)
      smooth.current.r += (tc.r - smooth.current.r) * 0.04
      smooth.current.g += (tc.g - smooth.current.g) * 0.04
      smooth.current.b += (tc.b - smooth.current.b) * 0.04
      const { r, g, b } = smooth.current
      const baseColor = `rgba(${r | 0},${g | 0},${b | 0}, 0.95)`
      const deepColor = `rgba(${Math.round(r * 0.65)},${Math.round(g * 0.68)},${Math.round(b * 0.72)}, 0.85)`

      ctx.clearRect(0, 0, W, H)

      const cx = W / 2
      const cy = H / 2

      // Xác định cỡ hiển thị: large (fullscreen), mini (góc 68px), header (sidedeck 28px)
      const effectiveSize =
        sizeTypeRef.current ||
        (Math.min(W, H) <= 50 ? "header" : Math.min(W, H) <= 120 ? "mini" : "large")
      const isHeader = effectiveSize === "header"
      const isMini = effectiveSize === "mini"

      const baseR = Math.min(W, H) * (isHeader ? 0.38 : isMini ? 0.36 : 0.32)
      const alpha = 0.88

      // RUNG ĐỘNG VẬT LÝ ÂM HỌC (ACOUSTIC PHYSICAL VIBRATION TREMOR)
      // Khi nói: rung cơ học mạnh mẽ theo biên độ và phổ âm thanh. Khi im lặng: tuyệt đối tĩnh tại 0px.
      const shakeAmp = audioEnergy > 0.02 ? (isMini ? 1.8 : isHeader ? 0.9 : 3.2) * audioEnergy : 0
      const shakeX = shakeAmp * (Math.sin(time * 0.075) * 0.65 + Math.cos(time * 0.13) * 0.35)
      const shakeY = shakeAmp * (Math.cos(time * 0.082) * 0.65 + Math.sin(time * 0.14) * 0.35)
      const orbCx = cx + shakeX
      const orbCy = cy + shakeY

      // ĐỘ CO DÃN & NHỊP THỞ: Nhịp thở tĩnh lặng nhẹ nhàng khi idle, bùng nở theo tiếng nói
      const baseBreath = 1.0 + Math.sin(time / 2200) * 0.015
      const breath = baseBreath + audioEnergy * 0.24 + (audioEnergy > 0.02 ? Math.sin(time * 0.05) * 0.04 * audioEnergy : 0)
      const scale = baseR * breath

      // 1. Ngôi sao trung tâm bùng sáng theo nhịp nói
      const pulse = 0.95 + 0.05 * Math.sin(time / 1800) + audioEnergy * 0.45
      const starSize = baseR * 0.32 * pulse
      drawGlowDot(ctx, orbCx, orbCy, starSize * 1.5, baseColor, alpha * 0.3)
      drawGlowDot(ctx, orbCx, orbCy, starSize * 0.65, "#ffffff", alpha * (0.65 + audioEnergy * 0.35))

      // 8 tia hào quang Runic xoay đồng bộ theo cosine mượt mà
      if (!isHeader) {
        for (let i = 0; i < 8; i++) {
          const angle = (i * 45 * Math.PI) / 180 + yaw * 0.35
          const freqVal = lv.spectrum && lv.spectrum[i * 2] !== undefined ? lv.spectrum[i * 2] : 1.0
          const jitter =
            audioEnergy > 0.02
              ? Math.sin(time * 0.06 + i * 1.8) * 5.5 * audioEnergy * (0.4 + freqVal * 0.6)
              : 0
          const len = (i % 2 === 0 ? starSize * (isMini ? 1.9 : 2.3) : starSize * 1.35) * pulse + jitter
          drawBeamLine(
            ctx,
            orbCx,
            orbCy,
            orbCx + Math.cos(angle) * len,
            orbCy + Math.sin(angle) * len,
            isMini ? 1.4 : 1.8,
            "#ffffff",
            alpha * (0.28 + audioEnergy * 0.3),
          )
        }
      }

      // 2. Vòng tròn ma thuật Runic viền ngoài (nở rộng khi âm lượng cao)
      if (showBorderRef.current || !isHeader) {
        ctx.save()
        ctx.strokeStyle = baseColor
        ctx.lineWidth = (1.0 + audioEnergy * 1.6) * (isMini ? 0.9 : 1.1)
        ctx.globalAlpha = isMini ? 0.6 : 0.45 + audioEnergy * 0.3
        ctx.beginPath()
        ctx.arc(
          orbCx,
          orbCy,
          baseR * (isHeader ? 0.95 : isMini ? 1.08 : 1.15) * (1 + audioEnergy * 0.08),
          0,
          Math.PI * 2,
        )
        ctx.stroke()
        ctx.restore()
      }

      // 3. Vòng quỹ đạo hạt 3D (tăng tốc đồng bộ và giảm tốc theo cosine)
      const ringsCount = isHeader ? 2 : 3
      const beads = isHeader ? 24 : isMini ? 32 : 48
      const baseRingRates = [0.35, -0.26, 0.18]
      const speedMultiplier = yawSpeedRef.current / baseSpeed

      for (let ring = 0; ring < ringsCount; ring++) {
        const tilt = 0.5 + ring * 0.75
        ringSpeedsRef.current[ring] = baseRingRates[ring] * speedMultiplier
        ringAnglesRef.current[ring] += ringSpeedsRef.current[ring] * dt
        const spin = ringAnglesRef.current[ring]

        const ringRadius = baseR * (0.75 + ring * 0.18) * breath
        const c = ring === 1 ? baseColor : deepColor

        for (let i = 0; i < beads; i++) {
          if (i % 3 === 2) continue
          const angle = (Math.PI * 2 * i) / beads + spin
          const pt = project([Math.cos(angle), 0, Math.sin(angle)], ring * 1.1, tilt)
          const weight = 0.28 + 0.72 * (pt[2] + 1) * 0.5
          drawGlowDot(
            ctx,
            orbCx + pt[0] * ringRadius,
            orbCy + pt[1] * ringRadius,
            (isHeader ? 2.2 : isMini ? 2.8 : 3.5) * weight * (1 + audioEnergy * 0.3),
            c,
            alpha * 0.6 * weight,
          )
        }
      }

      // 4. Khung Đa Diện 20 Mặt Xoay 3D (3D Icosahedron Core Rung Giật Cơ Học)
      // Biến dạng các đỉnh icosahedron theo sóng âm (Resonance vibration)
      const screen = VERTICES.map((v, idx) => {
        let vJitter = 0
        if (audioEnergy > 0.02) {
          const band = lv.spectrum && lv.spectrum[idx * 2] !== undefined ? lv.spectrum[idx * 2] : 1.0
          vJitter = audioEnergy * 0.20 * Math.sin(time * 0.065 + idx * 1.85) * (0.4 + band * 0.6)
        }
        const perturbed: [number, number, number] = [
          v[0] * (1 + vJitter),
          v[1] * (1 + vJitter),
          v[2] * (1 + vJitter),
        ]
        return project(perturbed, yaw, pitch)
      })
      const icosaScale = scale * (isHeader ? 0.55 : isMini ? 0.6 : 0.65)

      for (const [ea, eb] of EDGES) {
        const a = screen[ea]
        const bb = screen[eb]
        const depth = (a[2] + bb[2]) * 0.5
        const weight = 0.3 + 0.7 * (depth + 1) * 0.5
        drawBeamLine(
          ctx,
          orbCx + a[0] * icosaScale,
          orbCy + a[1] * icosaScale,
          orbCx + bb[0] * icosaScale,
          orbCy + bb[1] * icosaScale,
          (isHeader ? 1.4 : isMini ? 1.6 : 2.0) * weight,
          baseColor,
          alpha * (0.55 + audioEnergy * 0.3) * weight,
        )
      }
      for (const v of screen) {
        const weight = 0.35 + 0.65 * (v[2] + 1) * 0.5
        drawGlowDot(
          ctx,
          orbCx + v[0] * icosaScale,
          orbCy + v[1] * icosaScale,
          (isHeader ? 3.0 : isMini ? 4.2 : 5.5) * weight * (1 + audioEnergy * 0.4),
          "#ffffff",
          alpha * (0.65 + audioEnergy * 0.3) * weight,
        )
      }

      // 5. Bụi ma pháp trôi nổi (chỉ vẽ trên màn hình lớn để tối ưu hiệu năng)
      if (effectiveSize === "large") {
        for (let i = 0; i < 24; i++) {
          const angle = i * 2.399963 + time * 1.3e-4 * ((i % 3) + 1)
          const drift = (time / 4200 + i * 0.6180339) % 1
          const dist = baseR * (0.8 + drift * 1.4) * (1 + audioEnergy * 0.2)
          const fade = Math.sin(drift * Math.PI)
          drawGlowDot(
            ctx,
            orbCx + Math.cos(angle) * dist,
            orbCy + Math.sin(angle) * dist * 0.75,
            2.5 + fade * 2.5,
            deepColor,
            alpha * fade * 0.45,
          )
        }
      }
    }

    raf = requestAnimationFrame(frame)
    return () => {
      cancelAnimationFrame(raf)
      ro.disconnect()
    }
  }, [])

  return (
    <canvas
      ref={canvasRef}
      className={className}
      onClick={onClick}
      aria-hidden="true"
    />
  )
}
