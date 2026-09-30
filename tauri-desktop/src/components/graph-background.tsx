"use client"

import { useEffect, useRef } from "react"

interface Node {
  x: number
  y: number
  vx: number
  vy: number
  glow: number
  seed: number
}
interface Edge {
  a: number
  b: number
  charge: number // 0..1 travelling pulse energy
}

interface Props {
  /** increments each time a GRAPH_QUERY arrives from the pipeline */
  pulse: number
  /** current state color for tinting the retrieval sweep */
  accent: string
  /** Whether the star map is visible (true in fullscreen, false in docked/sidedeck) */
  visible?: boolean
  className?: string
}

export function GraphBackground({
  pulse,
  accent,
  visible = true,
  className,
}: Props) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const pulseRef = useRef(pulse)
  const accentRef = useRef(accent)
  const visibleRef = useRef(visible)
  const nodesRef = useRef<Node[]>([])
  const edgesRef = useRef<Edge[]>([])
  const lastPulse = useRef(pulse)
  const flashTimer = useRef(0)

  accentRef.current = accent
  pulseRef.current = pulse
  visibleRef.current = visible

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext("2d")
    if (!ctx) return

    let raf = 0
    let dpr = 1
    let W = 0
    let H = 0

    const NODE_COUNT = 48

    const build = () => {
      const nodes: Node[] = []
      for (let i = 0; i < NODE_COUNT; i++) {
        nodes.push({
          x: Math.random() * W,
          y: Math.random() * H,
          vx: (Math.random() - 0.5) * 0.12,
          vy: (Math.random() - 0.5) * 0.12,
          glow: 0,
          seed: Math.random() * 1000,
        })
      }
      // connect each node to its 2 nearest neighbours
      const edges: Edge[] = []
      const seen = new Set<string>()
      for (let i = 0; i < nodes.length; i++) {
        const dists = nodes
          .map((n, j) => ({ j, d: (n.x - nodes[i].x) ** 2 + (n.y - nodes[i].y) ** 2 }))
          .filter((o) => o.j !== i)
          .sort((a, b) => a.d - b.d)
          .slice(0, 2)
        for (const { j } of dists) {
          const key = `${Math.min(i, j)}_${Math.max(i, j)}`
          if (!seen.has(key)) {
            seen.add(key)
            edges.push({ a: Math.min(i, j), b: Math.max(i, j), charge: 0 })
          }
        }
      }
      nodesRef.current = nodes
      edgesRef.current = edges
    }

    const resize = () => {
      const rect = canvas.getBoundingClientRect()
      dpr = Math.min(window.devicePixelRatio || 1, 2)
      W = Math.max(1, Math.round(rect.width))
      H = Math.max(1, Math.round(rect.height))
      canvas.width = W * dpr
      canvas.height = H * dpr
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
      build()
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

    // trigger a retrieval sweep: light up a connected cluster
    const triggerRetrieval = () => {
      const nodes = nodesRef.current
      const edges = edgesRef.current
      if (!nodes.length) return
      flashTimer.current = 75 // Flash for 75 frames if hidden
      const startIdx = Math.floor(Math.random() * nodes.length)
      const queue = [startIdx]
      const visited = new Set<number>([startIdx])
      let count = 0
      while (queue.length && count < 10) {
        const cur = queue.shift() as number
        nodes[cur].glow = 1
        count++
        for (const e of edges) {
          let next = -1
          if (e.a === cur && !visited.has(e.b)) next = e.b
          else if (e.b === cur && !visited.has(e.a)) next = e.a
          if (next >= 0 && Math.random() > 0.35) {
            visited.add(next)
            queue.push(next)
            e.charge = 1
          }
        }
      }
    }

    const frame = () => {
      raf = requestAnimationFrame(frame)

      if (pulseRef.current !== lastPulse.current) {
        lastPulse.current = pulseRef.current
        triggerRetrieval()
      }

      // KHI Ở CHẾ ĐỘ MINI ORB HOẶC SIDEDECK: BẢN ĐỒ SAO ẨN 100% ĐỂ GIỮ MÀN HÌNH TRONG SUỐT
      const isVisible = visibleRef.current || flashTimer.current > 0
      if (!isVisible) {
        ctx.clearRect(0, 0, W, H)
        return
      }

      if (flashTimer.current > 0) {
        flashTimer.current--
      }

      const nodes = nodesRef.current
      const edges = edgesRef.current
      const { r, g, b } = hexToRgb(accentRef.current || "#cbcfd6")
      const t = performance.now() / 1000

      ctx.clearRect(0, 0, W, H)

      // drift nodes
      for (const n of nodes) {
        n.x += n.vx
        n.y += n.vy
        if (n.x < 0 || n.x > W) n.vx *= -1
        if (n.y < 0 || n.y > H) n.vy *= -1
        n.glow *= 0.965
      }

      // edges
      ctx.lineCap = "round"
      const baseAlphaMultiplier = visibleRef.current ? 1.0 : Math.min(1, flashTimer.current / 40)

      for (const e of edges) {
        const na = nodes[e.a]
        const nb = nodes[e.b]
        const dist = Math.hypot(na.x - nb.x, na.y - nb.y)
        if (dist > Math.max(W, H) * 0.28) continue
        const base = 0.025 * baseAlphaMultiplier
        const litA = Math.max(na.glow, nb.glow)
        ctx.strokeStyle = `rgba(${r},${g},${b},${(base + litA * 0.45 + e.charge * 0.5) * baseAlphaMultiplier})`
        ctx.lineWidth = 0.5 + e.charge * 1.5
        ctx.beginPath()
        ctx.moveTo(na.x, na.y)
        ctx.lineTo(nb.x, nb.y)
        ctx.stroke()

        // travelling pulse along the edge
        if (e.charge > 0.02) {
          const p = 1 - e.charge
          const px = na.x + (nb.x - na.x) * p
          const py = na.y + (nb.y - na.y) * p
          ctx.save()
          ctx.globalCompositeOperation = "lighter"
          const grad = ctx.createRadialGradient(px, py, 0, px, py, 10)
          grad.addColorStop(0, `rgba(${r},${g},${b},${e.charge * baseAlphaMultiplier})`)
          grad.addColorStop(1, "rgba(0,0,0,0)")
          ctx.fillStyle = grad
          ctx.beginPath()
          ctx.arc(px, py, 10, 0, Math.PI * 2)
          ctx.fill()
          ctx.restore()
          e.charge *= 0.955
        } else {
          e.charge = 0
        }
      }

      // nodes (dormant cosmic stars that ignite on Neo4j query)
      for (const n of nodes) {
        const twinkle = (0.12 + 0.08 * Math.sin(t * 1.2 + n.seed)) * baseAlphaMultiplier
        const a = (twinkle + n.glow * 0.85) * baseAlphaMultiplier
        const size = 1.2 + n.glow * 4.0
        ctx.save()
        ctx.globalCompositeOperation = "lighter"
        const grad = ctx.createRadialGradient(n.x, n.y, 0, n.x, n.y, size * 2.5)
        grad.addColorStop(0, `rgba(${r},${g},${b},${a})`)
        grad.addColorStop(1, "rgba(0,0,0,0)")
        ctx.fillStyle = grad
        ctx.beginPath()
        ctx.arc(n.x, n.y, size * 2.5, 0, Math.PI * 2)
        ctx.fill()
        ctx.fillStyle = `rgba(255,255,255,${Math.min(1, (a + 0.1) * baseAlphaMultiplier)})`
        ctx.beginPath()
        ctx.arc(n.x, n.y, size * 0.5, 0, Math.PI * 2)
        ctx.fill()
        ctx.restore()
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
      className={`${className ?? ""} ${
        visible ? "opacity-75" : "opacity-0"
      } transition-opacity duration-500`}
      aria-hidden="true"
    />
  )
}
