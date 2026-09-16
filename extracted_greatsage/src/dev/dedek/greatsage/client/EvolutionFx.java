/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.util.Mth
 */
package dev.dedek.greatsage.client;

import dev.dedek.greatsage.client.LightFx;
import dev.dedek.greatsage.client.SageCore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

final class EvolutionFx {
    static final int GOLD = 16771496;
    private static final int ARC = 0xCFF3FF;
    private static final int SPEED_LINES = 112;
    private static final int RUNES = 24;
    private static final int CRACK_BRANCHES = 11;
    private static final int DEBRIS = 40;
    private static final float GROUND_Y = 0.74f;
    private static final float MIN_BUILD_MS = 1500.0f;
    private static final float[][] DENSE_VERTICES;
    private static final int[][] DENSE_EDGES;

    private EvolutionFx() {
    }

    private static Subdivision subdivide() {
        ArrayList<float[]> verts = new ArrayList<float[]>();
        HashMap<String, Integer> index = new HashMap<String, Integer>();
        LinkedHashSet<Long> edges = new LinkedHashSet<Long>();
        for (float[] v : SageCore.vertices()) {
            verts.add(new float[]{v[0], v[1], v[2]});
        }
        for (int[] face : SageCore.faces()) {
            int a = face[0];
            int b = face[1];
            int c = face[2];
            int ab = EvolutionFx.midpoint(verts, index, a, b);
            int bc = EvolutionFx.midpoint(verts, index, b, c);
            int ca = EvolutionFx.midpoint(verts, index, c, a);
            for (int[] t : new int[][]{{a, ab, ca}, {b, bc, ab}, {c, ca, bc}, {ab, bc, ca}}) {
                for (int i = 0; i < 3; ++i) {
                    int x = t[i];
                    int y = t[(i + 1) % 3];
                    edges.add((long)Math.min(x, y) << 32 | (long)Math.max(x, y));
                }
            }
        }
        int[][] nArray = new int[edges.size()][2];
        int i = 0;
        Iterator iterator = edges.iterator();
        while (iterator.hasNext()) {
            long packed = (Long)iterator.next();
            nArray[i][0] = (int)(packed >> 32);
            nArray[i][1] = (int)(packed & 0xFFFFFFFFL);
            ++i;
        }
        return new Subdivision((float[][])verts.toArray((T[])new float[0][]), nArray);
    }

    private static int midpoint(List<float[]> verts, Map<String, Integer> index, int a, int b) {
        String key = Math.min(a, b) + ":" + Math.max(a, b);
        Integer known = index.get(key);
        if (known != null) {
            return known;
        }
        float[] p = verts.get(a);
        float[] q = verts.get(b);
        float x = (p[0] + q[0]) * 0.5f;
        float y = (p[1] + q[1]) * 0.5f;
        float z = (p[2] + q[2]) * 0.5f;
        float len = Mth.sqrt((float)(x * x + y * y + z * z));
        verts.add(new float[]{x / len, y / len, z / len});
        index.put(key, verts.size() - 1);
        return verts.size() - 1;
    }

    static float render(GuiGraphics graphics, float width, float height, float time, float answerAt, float climaxAt, float alpha, float voice) {
        float cx = width / 2.0f;
        float groundY = height * 0.74f;
        float reach = Math.max(width, height);
        float since = time - answerAt;
        if (since < 0.0f) {
            return EvolutionFx.buildUp(graphics, cx, groundY, width, height, reach, time, answerAt, alpha, voice);
        }
        float build = Math.max(1500.0f, climaxAt - answerAt - 1150.0f);
        return EvolutionFx.release(graphics, cx, groundY, width, height, reach, since, build, EvolutionFx.yawAt(answerAt), EvolutionFx.pitchAt(answerAt), alpha, voice);
    }

    private static float yawAt(float time) {
        return time * 4.2E-4f;
    }

    private static float pitchAt(float time) {
        return 0.42f + Mth.sin((float)(time * 2.6E-4f)) * 0.2f;
    }

    private static float buildUp(GuiGraphics graphics, float cx, float groundY, float width, float height, float reach, float time, float answerAt, float alpha, float voice) {
        float t = Mth.clamp((float)(time / Math.max(1.0f, answerAt)), (float)0.0f, (float)1.0f);
        float draw = Mth.clamp((float)(t / 0.3f), (float)0.0f, (float)1.0f);
        float charge = t * t * t;
        float cy = height * 0.46f;
        float shell = height * 0.19f;
        EvolutionFx.magicCircle(graphics, cx, groundY, width * 0.46f, time, draw, alpha, 1.0f);
        EvolutionFx.drawnMotes(graphics, cx, cy, width, height, time, charge, alpha * (0.3f + draw * 0.7f));
        float glow = shell * (0.25f + charge * 2.4f);
        LightFx.glow(graphics, cx, cy, glow * 3.0f, glow * 3.0f, 16771496, alpha * charge * 0.55f);
        LightFx.glow(graphics, cx, cy, glow * 1.4f, glow * 1.4f, 16771496, alpha * (0.25f + charge));
        LightFx.glow(graphics, cx, cy, glow * 0.5f, glow * 0.5f, 0xFFFFFF, alpha * (0.5f + charge * 0.5f));
        LightFx.glow(graphics, cx, cy, reach * charge * 1.9f, 5.0f + charge * 26.0f, 0xCFF3FF, alpha * charge * 0.7f);
        LightFx.glow(graphics, cx, cy, 4.0f + charge * 16.0f, reach * charge * 0.85f, 0xCFF3FF, alpha * charge * 0.35f);
        float squeeze = 1.0f - charge;
        LightFx.ring(graphics, cx, cy, shell * (1.0f + squeeze * 2.6f), shell * (1.0f + squeeze * 2.6f), 0xCFF3FF, alpha * charge * 0.5f);
        float tremble = charge * 4.0f;
        float wobble = Mth.sin((float)(time / 22.0f)) * tremble;
        EvolutionFx.shellFrame(graphics, cx + wobble, cy + wobble * 0.4f, shell * (1.0f - charge * 0.1f), EvolutionFx.yawAt(time), EvolutionFx.pitchAt(time), alpha * (0.55f + charge * 0.45f), 2.4f + charge * 2.0f, 16771496, 1.0f);
        int leaks = Math.round(charge * 16.0f);
        for (int i = 0; i < leaks; ++i) {
            float ang = (float)i * 137.5f + time * 0.02f;
            float flicker = 0.55f + 0.45f * Mth.sin((float)(time * 0.02f + (float)i * 2.3f));
            LightFx.ray(graphics, cx, cy, shell * (1.6f + charge * 2.4f), 3.0f + charge * 5.0f, ang, 0xFFFFFF, alpha * charge * 0.55f * flicker);
        }
        EvolutionFx.speedLines(graphics, cx, cy, reach * (0.7f - 0.26f * charge), reach * 0.98f, time * 3.0E-5f, 16771496, alpha * charge * 0.3f);
        return 5.0f * charge * charge * Mth.sin((float)(time / 16.0f));
    }

    private static float release(GuiGraphics graphics, float cx, float groundY, float width, float height, float reach, float since, float build, float yaw, float pitch, float alpha, float voice) {
        float cy = height * 0.46f;
        float shell = height * 0.19f;
        EvolutionFx.overload(graphics, cx, cy, width, height, since, alpha);
        EvolutionFx.shockRings(graphics, cx, cy, reach, since, alpha);
        EvolutionFx.burstPlates(graphics, cx, cy, shell, since, yaw, pitch, alpha);
        EvolutionFx.nakedCore(graphics, cx, cy, shell, since, alpha, voice);
        EvolutionFx.crystallise(graphics, cx, cy, shell, since, build, alpha, voice);
        EvolutionFx.detonation(graphics, cx, cy, groundY, reach, since, alpha);
        EvolutionFx.cracks(graphics, cx, cy, reach, since, alpha);
        EvolutionFx.pillars(graphics, width, height, since, alpha);
        EvolutionFx.debris(graphics, cx, cy, width, height, since, alpha);
        EvolutionFx.speedLines(graphics, cx, cy, reach * 0.38f, reach * 1.05f, since * 4.0E-5f, 16771496, alpha * Math.max(0.0f, 0.34f - since / 3600.0f));
        EvolutionFx.magicCircle(graphics, cx, groundY, width * 0.46f, since * 3.0f, 1.0f, alpha, Mth.clamp((float)(1.0f - since / 2600.0f), (float)0.0f, (float)1.0f));
        float quake = 15.0f * (float)Math.exp((double)(-since) / 240.0) * Mth.sin((float)(since / 24.0f));
        float lock = since - (1150.0f + build);
        if (lock > 0.0f && lock < 700.0f) {
            quake += 9.0f * (float)Math.exp((double)(-lock) / 130.0) * Mth.sin((float)(lock / 19.0f));
        }
        return quake;
    }

    private static void overload(GuiGraphics graphics, float cx, float cy, float width, float height, float since, float alpha) {
        float p = Mth.clamp((float)(since / 460.0f), (float)0.0f, (float)1.0f);
        if (p >= 1.0f) {
            return;
        }
        float fade = 1.0f - p * p;
        LightFx.glow(graphics, cx, cy, width * 3.0f, height * 3.0f, 0xFFFFFF, alpha * fade * 0.95f);
        LightFx.glow(graphics, cx, cy, width * 3.4f, 30.0f * fade, 0xCFF3FF, alpha * fade * 0.8f);
    }

    private static void shockRings(GuiGraphics graphics, float cx, float cy, float reach, float since, float alpha) {
        int[] colors = new int[]{0xFFFFFF, 16771496, 0xCFF3FF};
        float[] speeds = new float[]{900.0f, 1300.0f, 1750.0f};
        float[] flat = new float[]{0.62f, 0.78f, 0.5f};
        for (int i = 0; i < 3; ++i) {
            float p = Mth.clamp((float)((since - (float)i * 70.0f) / speeds[i]), (float)0.0f, (float)1.0f);
            if (p <= 0.0f || p >= 1.0f) continue;
            float eased = 1.0f - (1.0f - p) * (1.0f - p) * (1.0f - p);
            float r = reach * 0.05f + eased * reach * 0.95f;
            LightFx.ring(graphics, cx, cy, r, r * flat[i], colors[i], alpha * (1.0f - p) * (1.0f - p) * 0.75f);
        }
    }

    private static void burstPlates(GuiGraphics graphics, float cx, float cy, float radius, float since, float yaw, float pitch, float alpha) {
        float p = Mth.clamp((float)((since - 80.0f) / 1100.0f), (float)0.0f, (float)1.0f);
        if (p <= 0.0f || p >= 1.0f) {
            return;
        }
        float[][] v = SageCore.vertices();
        int[][] faces = SageCore.faces();
        float[] a = new float[3];
        float[] b = new float[3];
        for (int f = 0; f < faces.length; ++f) {
            float ox = 0.0f;
            float oy = 0.0f;
            for (int k = 0; k < 3; ++k) {
                SageCore.project(v[faces[f][k]], yaw, pitch, a);
                ox += a[0] / 3.0f;
                oy += a[1] / 3.0f;
            }
            for (int ghost = 2; ghost >= 0; --ghost) {
                float gp = p - (float)ghost * 0.055f;
                if (gp <= 0.0f) continue;
                float fly = gp * gp;
                float push = 1.0f + fly * (9.0f + (float)(f % 4) * 2.4f);
                float spin = fly * (520.0f + (float)(f % 6) * 200.0f) * (f % 2 == 0 ? 1.0f : -1.0f);
                float px = cx + ox * radius * push;
                float py = cy + oy * radius * push;
                float tilt = Math.abs(Mth.cos((float)(spin * ((float)Math.PI / 180) * 0.5f)));
                float fade = (1.0f - gp) * (ghost == 0 ? 1.0f : 0.28f / (float)ghost);
                for (int k = 0; k < 3; ++k) {
                    SageCore.project(v[faces[f][k]], yaw, pitch, a);
                    SageCore.project(v[faces[f][(k + 1) % 3]], yaw, pitch, b);
                    float rad = spin * ((float)Math.PI / 180);
                    float cos = Mth.cos((float)rad);
                    float sin = Mth.sin((float)rad);
                    float mx = ((a[0] + b[0]) * 0.5f - ox) * radius;
                    float my = ((a[1] + b[1]) * 0.5f - oy) * radius;
                    float rx = mx * cos - my * sin;
                    float ry = mx * sin + my * cos;
                    float len = Mth.sqrt((float)(EvolutionFx.sq(b[0] - a[0]) + EvolutionFx.sq(b[1] - a[1]))) * radius;
                    float ang = (float)Math.toDegrees(Math.atan2(b[1] - a[1], b[0] - a[0])) + spin;
                    LightFx.beam(graphics, px + rx, py + ry, len * (1.0f - fly * 0.3f) * (0.35f + 0.65f * tilt), 4.2f, ang, 16771496, alpha * fade * 0.9f);
                }
                if (ghost != 0) continue;
                LightFx.glow(graphics, px, py, radius * 0.55f * (1.0f - gp), 0xFFFFFF, alpha * (1.0f - gp) * 0.45f);
            }
        }
    }

    private static void nakedCore(GuiGraphics graphics, float cx, float cy, float radius, float since, float alpha, float voice) {
        float p = Mth.clamp((float)((since - 180.0f) / 900.0f), (float)0.0f, (float)1.0f);
        if (p <= 0.0f) {
            return;
        }
        float calm = Mth.clamp((float)((since - 1100.0f) / 900.0f), (float)0.0f, (float)1.0f);
        float beat = 1.0f + 0.3f * Mth.sin((float)(since / 62.0f)) * (1.0f - calm);
        float size = radius * (2.1f - 0.9f * calm) * beat * (1.0f + voice * 0.2f);
        float a = alpha * (1.0f - calm * 0.45f);
        LightFx.glow(graphics, cx, cy, size * 2.6f, size * 2.6f, 16771496, a * 0.35f);
        LightFx.glow(graphics, cx, cy, size, size, 16771496, a * 0.75f);
        LightFx.glow(graphics, cx, cy, size * 0.36f, size * 0.36f, 0xFFFFFF, a);
        LightFx.ring(graphics, cx, cy, size * 0.78f, size * 0.78f, 0xCFF3FF, a * 0.55f * (1.0f - calm * 0.6f));
        LightFx.ring(graphics, cx, cy, size * (1.15f + 0.1f * Mth.sin((float)(since / 130.0f))), size * 0.42f, 0xCFF3FF, a * 0.3f * (1.0f - calm));
        for (int i = 0; i < 16; ++i) {
            LightFx.ray(graphics, cx, cy, size * (2.6f + (float)(i % 3)), 4.0f * beat, (float)i * 22.5f + since * 0.004f, 0xFFFFFF, a * 0.4f * (1.0f - calm));
        }
    }

    private static void crystallise(GuiGraphics graphics, float cx, float cy, float radius, float since, float build, float alpha, float voice) {
        int i;
        float p = Mth.clamp((float)((since - 1150.0f) / build), (float)0.0f, (float)1.0f);
        if (p <= 0.0f) {
            return;
        }
        float eased = 1.0f - (1.0f - p) * (1.0f - p) * (1.0f - p);
        float scale = radius * 1.5f * (1.0f + voice * 0.1f);
        float yaw = since * 3.2E-4f;
        float pitch = 0.42f + Mth.sin((float)(since * 2.0E-4f)) * 0.22f;
        float[][] screen = new float[DENSE_VERTICES.length][3];
        for (i = 0; i < DENSE_VERTICES.length; ++i) {
            SageCore.project(DENSE_VERTICES[i], yaw, pitch, screen[i]);
        }
        for (i = 0; i < DENSE_EDGES.length; ++i) {
            float[] a = screen[DENSE_EDGES[i][0]];
            float[] b = screen[DENSE_EDGES[i][1]];
            float depth = (a[2] + b[2]) * 0.5f;
            float wave = (float)i * 0.6180339f % 1.0f * 0.55f;
            float local = Mth.clamp((float)((eased - wave) / 0.35f), (float)0.0f, (float)1.0f);
            if (local <= 0.0f) continue;
            float grow = local * scale;
            float ax = cx + a[0] * grow;
            float ay = cy + a[1] * grow;
            float bx = cx + b[0] * grow;
            float by = cy + b[1] * grow;
            float len = Mth.sqrt((float)(EvolutionFx.sq(bx - ax) + EvolutionFx.sq(by - ay)));
            if (len < 0.5f) continue;
            float weight = 0.3f + 0.7f * (depth + 1.0f) * 0.5f;
            float fresh = 1.0f - Mth.clamp((float)((local - 0.6f) / 0.4f), (float)0.0f, (float)1.0f);
            LightFx.beam(graphics, (ax + bx) * 0.5f, (ay + by) * 0.5f, len, (2.2f + fresh * 3.0f) * weight, (float)Math.toDegrees(Math.atan2(by - ay, bx - ax)), fresh > 0.5f ? 0xFFFFFF : 14679276, alpha * (0.5f + fresh * 0.5f) * weight);
            if (!(fresh > 0.85f)) continue;
            LightFx.glow(graphics, bx, by, 9.0f, 0xFFFFFF, alpha * fresh * 0.7f);
        }
        for (float[] vert : screen) {
            float weight = 0.35f + 0.65f * (vert[2] + 1.0f) * 0.5f;
            LightFx.glow(graphics, cx + vert[0] * scale * eased, cy + vert[1] * scale * eased, 4.5f * weight, 0xFFFFFF, alpha * eased * 0.5f * weight);
        }
        float lock = since - (1150.0f + build);
        if (lock >= 0.0f && lock < 900.0f) {
            float k = lock / 900.0f;
            float snap = (1.0f - k) * (1.0f - k);
            LightFx.glow(graphics, cx, cy, scale * (2.4f + k * 3.0f), scale * (2.4f + k * 3.0f), 0xFFFFFF, alpha * snap * 0.55f);
            float r = scale * (1.0f + k * 3.2f);
            LightFx.ring(graphics, cx, cy, r, r, 14679276, alpha * snap * 0.8f);
        }
        EvolutionFx.orbitRings(graphics, cx, cy, scale, since, build, alpha);
    }

    private static void orbitRings(GuiGraphics graphics, float cx, float cy, float scale, float since, float build, float alpha) {
        float p = Mth.clamp((float)((since - (1150.0f + build)) / 800.0f), (float)0.0f, (float)1.0f);
        if (p <= 0.0f) {
            return;
        }
        for (int r = 0; r < 3; ++r) {
            float radius = scale * (1.22f + (float)r * 0.2f) * (1.6f - 0.6f * p);
            float flat = 0.24f + (float)r * 0.22f;
            float spin = since * 6.0E-4f * (r % 2 == 0 ? 1.0f : -1.0f) + (float)r * 1.1f;
            int dots = 34;
            for (int i = 0; i < dots; ++i) {
                if ((i + r) % 3 == 0) continue;
                float ang = (float)i / (float)dots * ((float)Math.PI * 2) + spin;
                LightFx.glow(graphics, cx + Mth.cos((float)ang) * radius, cy + Mth.sin((float)ang) * radius * flat, 3.4f, 14679276, alpha * p * 0.5f);
            }
        }
    }

    /*
     * WARNING - void declaration
     */
    private static void shellFrame(GuiGraphics graphics, float cx, float cy, float radius, float yaw, float pitch, float alpha, float thickness, int rgb, float vertexGlow) {
        void var12_13;
        float[][] v = SageCore.vertices();
        float[][] screen = new float[v.length][3];
        boolean bl = false;
        while (var12_13 < v.length) {
            SageCore.project(v[var12_13], yaw, pitch, screen[var12_13]);
            ++var12_13;
        }
        for (int[] e : SageCore.edges()) {
            float[] a = screen[e[0]];
            float[] b = screen[e[1]];
            float ax = cx + a[0] * radius;
            float ay = cy + a[1] * radius;
            float bx = cx + b[0] * radius;
            float by = cy + b[1] * radius;
            float weight = 0.32f + 0.68f * ((a[2] + b[2]) * 0.5f + 1.0f) * 0.5f;
            float len = Mth.sqrt((float)(EvolutionFx.sq(bx - ax) + EvolutionFx.sq(by - ay)));
            LightFx.beam(graphics, (ax + bx) * 0.5f, (ay + by) * 0.5f, len, thickness * weight, (float)Math.toDegrees(Math.atan2(by - ay, bx - ax)), rgb, alpha * weight);
        }
        if (vertexGlow > 0.0f) {
            for (float[] pt : screen) {
                float weight = 0.35f + 0.65f * (pt[2] + 1.0f) * 0.5f;
                LightFx.glow(graphics, cx + pt[0] * radius, cy + pt[1] * radius, 6.5f * weight, 0xFFFFFF, alpha * vertexGlow * 0.6f * weight);
            }
        }
    }

    private static void magicCircle(GuiGraphics graphics, float cx, float groundY, float radius, float time, float draw, float alpha, float fade) {
        float a = alpha * fade * draw;
        if (a <= 0.01f) {
            return;
        }
        float rx = radius;
        float ry = radius * 0.3f;
        LightFx.ring(graphics, cx, groundY, rx * draw, ry * draw, 16771496, a * 0.55f);
        LightFx.ring(graphics, cx, groundY, rx * 0.74f * draw, ry * 0.74f * draw, 16771496, a * 0.4f);
        LightFx.ring(graphics, cx, groundY, rx * 0.4f * draw, ry * 0.4f * draw, 16771496, a * 0.3f);
        LightFx.glow(graphics, cx, groundY, rx * 2.0f, ry * 3.2f, 16771496, a * 0.14f);
        for (int i = 0; i < 24; ++i) {
            float share = ((float)i + 1.0f) / 24.0f;
            if (draw < share * 0.9f) continue;
            float ang = (float)i / 24.0f * ((float)Math.PI * 2) + time * 2.2E-4f;
            float px = cx + Mth.cos((float)ang) * rx * 0.87f;
            float py = groundY + Mth.sin((float)ang) * ry * 0.87f;
            float lit = Mth.clamp((float)((draw - share * 0.9f) * 12.0f), (float)0.0f, (float)1.0f);
            LightFx.beam(graphics, px, py, 13.0f, 2.6f, ang * 57.295776f + 90.0f, 16771496, a * lit * 0.9f);
            LightFx.glow(graphics, px, py, 9.0f, 0xFFFFFF, a * lit * 0.5f);
        }
    }

    private static void drawnMotes(GuiGraphics graphics, float cx, float cy, float width, float height, float time, float charge, float alpha) {
        for (int i = 0; i < 52; ++i) {
            int h = EvolutionFx.hash(i);
            float lane = (float)(h & 0x3FF) / 1023.0f * width;
            float speed = 1.6E-4f + (float)(h >> 10 & 0x3F) / 63.0f * 3.0E-4f;
            float travel = (time * speed + (float)(h >> 16 & 0xFF) / 255.0f) % 1.0f;
            float riseY = height * (1.02f - travel * 1.05f);
            float pull = charge * charge;
            float x = Mth.lerp((float)pull, (float)lane, (float)cx);
            float y = Mth.lerp((float)pull, (float)riseY, (float)cy);
            float size = 2.0f + (float)(h >> 6 & 7) * 0.6f + pull * 3.0f;
            float edge = Math.min(1.0f, Math.min(travel, 1.0f - travel) * 6.0f);
            LightFx.glow(graphics, x, y, size, 16771496, alpha * edge * (0.35f + pull * 0.5f));
        }
    }

    private static void speedLines(GuiGraphics graphics, float cx, float cy, float inner, float outer, float spin, int rgb, float alpha) {
        if (alpha <= 0.005f || outer <= inner) {
            return;
        }
        for (int i = 0; i < 112; ++i) {
            int h = EvolutionFx.hash(i);
            float ang = (float)i / 112.0f * 360.0f + (float)(h & 0xF) - 7.5f + spin;
            float jitter = (float)(h >> 8 & 0x3F) / 63.0f;
            float start = inner * (0.82f + jitter * 0.34f);
            float len = outer - start;
            float rad = ang * ((float)Math.PI / 180);
            LightFx.beam(graphics, cx + Mth.cos((float)rad) * (start + len / 2.0f), cy + Mth.sin((float)rad) * (start + len / 2.0f), len, 1.4f + jitter * 3.4f, ang, rgb, alpha * (0.4f + jitter * 0.6f));
        }
    }

    private static void detonation(GuiGraphics graphics, float cx, float cy, float groundY, float reach, float since, float alpha) {
        float p = Mth.clamp((float)(since / 1600.0f), (float)0.0f, (float)1.0f);
        if (p >= 1.0f) {
            return;
        }
        float eased = 1.0f - (1.0f - p) * (1.0f - p) * (1.0f - p);
        float r = reach * eased * 0.85f;
        LightFx.ring(graphics, cx, groundY, r, r * 0.26f, 16771496, alpha * (1.0f - p) * 0.55f);
        LightFx.ring(graphics, cx, groundY, r * 0.62f, r * 0.16f, 0xFFFFFF, alpha * (1.0f - p) * 0.35f);
        float column = 1.0f - Mth.clamp((float)(since / 620.0f), (float)0.0f, (float)1.0f);
        if (column > 0.0f) {
            LightFx.glow(graphics, cx, (cy + groundY) * 0.5f, reach * 0.1f * column, Math.abs(groundY - cy) * 2.6f, 0xFFFFFF, alpha * column * 0.5f);
        }
    }

    private static void cracks(GuiGraphics graphics, float cx, float cy, float reach, float since, float alpha) {
        float p = Mth.clamp((float)((since - 60.0f) / 1400.0f), (float)0.0f, (float)1.0f);
        if (p <= 0.0f) {
            return;
        }
        float fade = 1.0f - Mth.clamp((float)((since - 900.0f) / 900.0f), (float)0.0f, (float)1.0f);
        if (fade <= 0.01f) {
            return;
        }
        float eased = 1.0f - (1.0f - p) * (1.0f - p);
        for (int b = 0; b < 11; ++b) {
            float len;
            int h = EvolutionFx.hash(b * 13 + 1);
            float ang = (float)b / 11.0f * 360.0f + (float)(h & 0x1F) - 16.0f;
            float x = cx;
            float y = cy;
            float remaining = reach * 1.05f * eased;
            int steps = 6;
            for (int s = 0; s < steps && remaining > 4.0f; remaining -= len, ++s) {
                int sh = EvolutionFx.hash(b * 71 + s);
                len = Math.min(remaining, reach * (0.1f + (float)(sh >> 5 & 0xF) / 15.0f * 0.14f));
                float rad = (ang += ((float)(sh & 0x1F) - 15.5f) * 1.5f) * ((float)Math.PI / 180);
                float nx = x + Mth.cos((float)rad) * len;
                float ny = y + Mth.sin((float)rad) * len;
                float thin = 3.6f * (1.0f - (float)s / (float)steps) + 0.8f;
                LightFx.beam(graphics, (x + nx) * 0.5f, (y + ny) * 0.5f, len, thin, ang, s == 0 ? 0xFFFFFF : 16771496, alpha * fade * 0.75f);
                x = nx;
                y = ny;
            }
        }
    }

    private static void pillars(GuiGraphics graphics, float width, float height, float since, float alpha) {
        for (int i = 0; i < 13; ++i) {
            int h = EvolutionFx.hash(i * 29 + 7);
            float delay = 120.0f + (float)(h & 0x1FF);
            float p = Mth.clamp((float)((since - delay) / 900.0f), (float)0.0f, (float)1.0f);
            if (p <= 0.0f || p >= 1.0f) continue;
            float x = (float)(h >> 9 & 0x3FF) / 1023.0f * width;
            float rise = 1.0f - (1.0f - p) * (1.0f - p);
            float tall = height * (0.35f + (float)(h >> 19 & 0xF) / 15.0f * 0.75f) * rise;
            float w = 6.0f + (float)(h >> 5 & 7) * 2.5f;
            float fade = 1.0f - p;
            LightFx.glow(graphics, x, height * 0.74f - tall / 2.0f, w, tall, 16771496, alpha * fade * 0.55f);
            LightFx.glow(graphics, x, height * 0.74f, w * 4.0f, w * 2.0f, 0xFFFFFF, alpha * fade * 0.4f);
        }
    }

    private static void debris(GuiGraphics graphics, float cx, float cy, float width, float height, float since, float alpha) {
        float p = Mth.clamp((float)((since - 380.0f) / 2800.0f), (float)0.0f, (float)1.0f);
        if (p <= 0.0f || p >= 1.0f) {
            return;
        }
        float fade = (1.0f - p) * (1.0f - p);
        for (int i = 0; i < 40; ++i) {
            int h = EvolutionFx.hash(i * 37 + 11);
            float ang = (float)(h & 0x3FF) / 1023.0f * ((float)Math.PI * 2);
            float speed = 0.3f + (float)(h >> 10 & 0x3F) / 63.0f;
            float dist = p * Math.max(width, height) * 0.55f * speed;
            float x = cx + Mth.cos((float)ang) * dist;
            float y = cy + Mth.sin((float)ang) * dist * 0.7f + p * p * height * 0.3f;
            float size = 2.0f + (float)(h >> 16 & 7) * 0.8f;
            float tumble = ang * 57.295776f + since * 0.05f * speed;
            LightFx.beam(graphics, x, y, size * 3.4f, size * 0.7f, tumble, 16771496, alpha * fade * 0.7f);
        }
    }

    private static int hash(int seed) {
        int h = seed * -1640531527;
        h ^= h >>> 15;
        h *= -2048144789;
        h ^= h >>> 13;
        return h & Integer.MAX_VALUE;
    }

    private static float sq(float x) {
        return x * x;
    }

    static {
        Subdivision s = EvolutionFx.subdivide();
        DENSE_VERTICES = s.vertices;
        DENSE_EDGES = s.edges;
    }

    private record Subdivision(float[][] vertices, int[][] edges) {
    }
}

