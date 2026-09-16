/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.util.Mth
 */
package dev.dedek.greatsage.client;

import dev.dedek.greatsage.client.LightFx;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

final class SageCore {
    static final int SAGE = 14679276;
    static final int SAGE_DEEP = 9431488;
    static final int WHITE = 0xFFFFFF;
    private static final float PHI = 1.618034f;
    private static final float[][] VERTICES = SageCore.normalize(new float[][]{{-1.0f, 1.618034f, 0.0f}, {1.0f, 1.618034f, 0.0f}, {-1.0f, -1.618034f, 0.0f}, {1.0f, -1.618034f, 0.0f}, {0.0f, -1.0f, 1.618034f}, {0.0f, 1.0f, 1.618034f}, {0.0f, -1.0f, -1.618034f}, {0.0f, 1.0f, -1.618034f}, {1.618034f, 0.0f, -1.0f}, {1.618034f, 0.0f, 1.0f}, {-1.618034f, 0.0f, -1.0f}, {-1.618034f, 0.0f, 1.0f}});
    private static final int[][] FACES = new int[][]{{0, 11, 5}, {0, 5, 1}, {0, 1, 7}, {0, 7, 10}, {0, 10, 11}, {1, 5, 9}, {5, 11, 4}, {11, 10, 2}, {10, 7, 6}, {7, 1, 8}, {3, 9, 4}, {3, 4, 2}, {3, 2, 6}, {3, 6, 8}, {3, 8, 9}, {4, 9, 5}, {2, 4, 11}, {6, 2, 10}, {8, 6, 7}, {9, 8, 1}};
    private static final int[][] EDGES = SageCore.buildEdges();

    private SageCore() {
    }

    static float[][] vertices() {
        return VERTICES;
    }

    static int[][] faces() {
        return FACES;
    }

    static int[][] edges() {
        return EDGES;
    }

    private static float[][] normalize(float[][] points) {
        for (float[] p : points) {
            float len = Mth.sqrt((float)(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]));
            p[0] = p[0] / len;
            p[1] = p[1] / len;
            p[2] = p[2] / len;
        }
        return points;
    }

    private static int[][] buildEdges() {
        boolean[][] seen = new boolean[VERTICES.length][VERTICES.length];
        int[][] edges = new int[30][2];
        int count = 0;
        for (int[] face : FACES) {
            for (int i = 0; i < 3; ++i) {
                int hi;
                int a = face[i];
                int b = face[(i + 1) % 3];
                int lo = Math.min(a, b);
                if (seen[lo][hi = Math.max(a, b)]) continue;
                seen[lo][hi] = true;
                edges[count][0] = lo;
                edges[count][1] = hi;
                ++count;
            }
        }
        return edges;
    }

    static void project(float[] source, float yaw, float pitch, float[] out) {
        float cosY = Mth.cos((float)yaw);
        float sinY = Mth.sin((float)yaw);
        float x = source[0] * cosY + source[2] * sinY;
        float z = -source[0] * sinY + source[2] * cosY;
        float cosP = Mth.cos((float)pitch);
        float sinP = Mth.sin((float)pitch);
        float y = source[1] * cosP - z * sinP;
        out[2] = source[1] * sinP + z * cosP;
        out[0] = x;
        out[1] = y;
    }

    static void render(GuiGraphics graphics, float centerX, float centerY, float radius, float time, float build, float alpha, int accent, float voice) {
        if (alpha <= 0.004f) {
            return;
        }
        float eased = SageCore.ease(build);
        float breath = 1.0f + voice * 0.14f;
        float scale = radius * (0.15f + 0.85f * eased) * breath;
        float lit = alpha * (0.72f + voice * 0.55f);
        SageCore.drawRays(graphics, centerX, centerY, radius * breath, time, eased, lit, voice);
        SageCore.drawOrbits(graphics, centerX, centerY, radius * breath, time, eased, lit, accent);
        SageCore.drawFrame(graphics, centerX, centerY, scale, time, eased, lit);
        SageCore.drawMotes(graphics, centerX, centerY, radius, time, alpha);
        SageCore.drawStar(graphics, centerX, centerY, radius, time, eased, alpha, voice);
    }

    /*
     * WARNING - void declaration
     */
    private static void drawFrame(GuiGraphics graphics, float centerX, float centerY, float scale, float time, float build, float alpha) {
        void var10_11;
        float yaw = time * 3.4E-4f;
        float pitch = 0.42f + Mth.sin((float)(time * 2.1E-4f)) * 0.28f;
        float[][] screen = new float[VERTICES.length][3];
        boolean bl = false;
        while (var10_11 < VERTICES.length) {
            SageCore.project(VERTICES[var10_11], yaw, pitch, screen[var10_11]);
            ++var10_11;
        }
        for (int[] edge : EDGES) {
            float[] b = screen[edge[1]];
            float bx = centerX + b[0] * scale;
            float[] a = screen[edge[0]];
            float ax = centerX + a[0] * scale;
            float dx = bx - ax;
            float by = centerY + b[1] * scale;
            float ay = centerY + a[1] * scale;
            float dy = by - ay;
            float length = Mth.sqrt((float)(dx * dx + dy * dy));
            if (length < 0.5f) continue;
            float depth = (a[2] + b[2]) * 0.5f;
            float weight = 0.3f + 0.7f * (depth + 1.0f) * 0.5f;
            float angle = (float)Math.toDegrees(Math.atan2(dy, dx));
            LightFx.beam(graphics, (ax + bx) * 0.5f, (ay + by) * 0.5f, length, 2.6f * weight, angle, 14679276, alpha * build * 0.62f * weight);
        }
        for (float[] v : screen) {
            float weight = 0.35f + 0.65f * (v[2] + 1.0f) * 0.5f;
            LightFx.glow(graphics, centerX + v[0] * scale, centerY + v[1] * scale, 5.5f * weight, 0xFFFFFF, alpha * build * 0.55f * weight);
        }
    }

    private static void drawOrbits(GuiGraphics graphics, float centerX, float centerY, float radius, float time, float build, float alpha, int accent) {
        int beads = 54;
        float[] point = new float[3];
        float[] source = new float[3];
        for (int ring = 0; ring < 3; ++ring) {
            float tilt = 0.5f + (float)ring * 0.75f;
            float spin = time * (4.2E-4f - (float)ring * 1.1E-4f) * (ring % 2 == 0 ? 1.0f : -1.0f);
            float ringRadius = radius * (1.02f + (float)ring * 0.16f) * build;
            int color = ring == 1 ? accent : 14679276;
            for (int i = 0; i < beads; ++i) {
                if (i % 3 == 2) continue;
                float angle = (float)(Math.PI * 2 * (double)i / (double)beads) + spin;
                source[0] = Mth.cos((float)angle);
                source[1] = 0.0f;
                source[2] = Mth.sin((float)angle);
                SageCore.project(source, (float)ring * 1.1f, tilt, point);
                float weight = 0.28f + 0.72f * (point[2] + 1.0f) * 0.5f;
                LightFx.glow(graphics, centerX + point[0] * ringRadius, centerY + point[1] * ringRadius, 4.2f * weight, color, alpha * build * 0.7f * weight);
            }
        }
    }

    private static void drawRays(GuiGraphics graphics, float centerX, float centerY, float radius, float time, float build, float alpha, float voice) {
        int rays = 18;
        for (int r = 0; r < rays; ++r) {
            float angle = (float)(Math.PI * 2 * (double)r / (double)rays) + time * 7.0E-5f;
            float cos = Mth.cos((float)angle);
            float sin = Mth.sin((float)angle);
            float drift = time * 0.035f % 22.0f;
            float reach = radius * (3.2f + voice * 0.9f);
            for (float d = radius * 0.75f + drift; d < reach; d += 22.0f) {
                float fade = 1.0f - d / reach;
                LightFx.glow(graphics, centerX + cos * d * build, centerY + sin * d * build, 3.4f + fade * 2.0f + voice * 2.0f, 14679276, alpha * build * fade * 0.5f);
            }
        }
    }

    private static void drawMotes(GuiGraphics graphics, float centerX, float centerY, float radius, float time, float alpha) {
        for (int i = 0; i < 30; ++i) {
            float angle = (float)i * 2.399963f + time * 1.3E-4f * (float)(i % 3 + 1);
            float drift = (time / 4200.0f + (float)i * 0.6180339f) % 1.0f;
            float distance = radius * (1.1f + drift * 1.9f);
            float fade = Mth.sin((float)(drift * (float)Math.PI));
            LightFx.glow(graphics, centerX + Mth.cos((float)angle) * distance, centerY + Mth.sin((float)angle) * distance * 0.72f, 3.0f + fade * 3.0f, 9431488, alpha * fade * 0.55f);
        }
    }

    private static void drawStar(GuiGraphics graphics, float centerX, float centerY, float radius, float time, float build, float alpha, float voice) {
        float pulse = 0.85f + 0.15f * Mth.sin((float)(time / 240.0f)) + voice * 0.75f;
        float size = radius * 0.42f * build * pulse;
        LightFx.glow(graphics, centerX, centerY, size * 2.0f, size * 2.0f, 14679276, alpha * 0.3f);
        LightFx.glow(graphics, centerX, centerY, size * 0.7f, size * 0.7f, 0xFFFFFF, alpha * (0.55f + voice * 0.45f));
        for (int i = 0; i < 8; ++i) {
            float angle = (float)i * 45.0f + time * 0.002f;
            float length = (i % 2 == 0 ? size * 3.4f : size * 1.7f) * pulse;
            LightFx.ray(graphics, centerX, centerY, length, 3.2f, angle, 0xFFFFFF, alpha * build * 0.5f);
        }
    }

    private static float ease(float t) {
        float inv = 1.0f - t;
        return 1.0f - inv * inv * inv;
    }
}

