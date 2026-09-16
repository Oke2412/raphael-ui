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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

final class SageMenuFx {
    private static final float STAGGER = 0.45f;
    private static final float HANDOVER = 0.72f;

    private SageMenuFx() {
    }

    static void shell(GuiGraphics graphics, float cx, float cy, float radius, float reach, float time, float assembled, float alpha) {
        if (alpha <= 0.004f || assembled <= 0.0f) {
            return;
        }
        float yaw = time * 3.4E-4f;
        float pitch = 0.42f + Mth.sin((float)(time * 2.1E-4f)) * 0.28f;
        float[][] v = SageCore.vertices();
        float[][] screen = new float[v.length][3];
        for (int i = 0; i < v.length; ++i) {
            SageCore.project(v[i], yaw, pitch, screen[i]);
        }
        SageMenuFx.intake(graphics, cx, cy, reach, time, assembled, alpha);
        int[][] edges = SageCore.edges();
        for (int i = 0; i < edges.length; ++i) {
            float wave = (float)i * 0.6180339f % 1.0f;
            float local = Mth.clamp((float)((assembled - wave * 0.45f) / 0.55f), (float)0.0f, (float)1.0f);
            if (local <= 0.0f) continue;
            float[] a = screen[edges[i][0]];
            float[] b = screen[edges[i][1]];
            float ax = cx + a[0] * radius;
            float ay = cy + a[1] * radius;
            float bx = cx + b[0] * radius;
            float by = cy + b[1] * radius;
            float depth = (a[2] + b[2]) * 0.5f;
            float weight = 0.3f + 0.7f * (depth + 1.0f) * 0.5f;
            float dx = (ax + bx) * 0.5f - cx;
            float dy = (ay + by) * 0.5f - cy;
            float d = Mth.sqrt((float)(dx * dx + dy * dy));
            float nx = d < 0.01f ? 0.0f : dx / d;
            float ny = d < 0.01f ? -1.0f : dy / d;
            int h = SageMenuFx.hash(i);
            boolean flip = (i & 1) == 0;
            for (int ghost = 2; ghost >= 0; --ghost) {
                float lag = local - (float)ghost * 0.055f;
                if (lag <= 0.0f) continue;
                float fade = ghost == 0 ? 1.0f : 0.26f / (float)ghost;
                SageMenuFx.fragment(graphics, ax, ay, bx, by, nx, ny, reach, h, flip, lag, 2.6f * weight, 14679276, alpha * weight * 0.62f * fade);
            }
            float land = Mth.clamp((float)((local - 0.72f) / 0.28f), (float)0.0f, (float)1.0f);
            if (!(land > 0.0f) || !(land < 1.0f)) continue;
            float snap = Mth.sin((float)(land * (float)Math.PI));
            LightFx.glow(graphics, (ax + bx) * 0.5f, (ay + by) * 0.5f, 7.0f + snap * 34.0f, 0xFFFFFF, alpha * snap * 0.55f);
        }
        for (float[] point : screen) {
            float weight = 0.35f + 0.65f * (point[2] + 1.0f) * 0.5f;
            LightFx.glow(graphics, cx + point[0] * radius, cy + point[1] * radius, 5.5f * weight, 0xFFFFFF, alpha * assembled * 0.55f * weight);
        }
    }

    static float coreFade(float assembled) {
        return Mth.clamp((float)((assembled - 0.72f) / 0.27999997f), (float)0.0f, (float)1.0f);
    }

    private static void fragment(GuiGraphics graphics, float ax, float ay, float bx, float by, float nx, float ny, float reach, int h, boolean flip, float local, float thickness, int color, float alpha) {
        float ry;
        float sin;
        float eased = SageMenuFx.easeOutBack(Mth.clamp((float)local, (float)0.0f, (float)1.0f));
        float away = (1.0f - eased) * reach * (0.55f + (float)(h & 0x3F) / 63.0f * 0.6f);
        float spin = (1.0f - eased) * (200.0f + (float)(h >> 6 & 0xFF)) * (flip ? 1.0f : -1.0f);
        float mx = (ax + bx) * 0.5f + nx * away;
        float my = (ay + by) * 0.5f + ny * away;
        float hx = (bx - ax) * 0.5f;
        float hy = (by - ay) * 0.5f;
        float rad = spin * ((float)Math.PI / 180);
        float cos = Mth.cos((float)rad);
        float rx = hx * cos - hy * (sin = Mth.sin((float)rad));
        float length = 2.0f * Mth.sqrt((float)(rx * rx + (ry = hx * sin + hy * cos) * ry));
        if (length < 0.5f) {
            return;
        }
        LightFx.beam(graphics, mx, my, length, thickness, (float)Math.toDegrees(Math.atan2(ry, rx)), color, alpha);
    }

    private static void intake(GuiGraphics graphics, float cx, float cy, float reach, float time, float assembled, float alpha) {
        float strength = Mth.sin((float)(Mth.clamp((float)assembled, (float)0.0f, (float)1.0f) * (float)Math.PI));
        if (strength <= 0.01f) {
            return;
        }
        for (int i = 0; i < 44; ++i) {
            int h = SageMenuFx.hash(i * 31 + 5);
            float angle = (float)i / 44.0f * 360.0f + (float)(h & 0xF) - 7.5f;
            float travel = (time * 9.0E-4f + (float)(h >> 8 & 0xFF) / 255.0f) % 1.0f;
            float distance = reach * (0.95f - travel * 0.62f);
            float rad = angle * ((float)Math.PI / 180);
            float len = reach * 0.1f * (0.4f + travel);
            LightFx.beam(graphics, cx + Mth.cos((float)rad) * distance, cy + Mth.sin((float)rad) * distance, len, 1.6f, angle, 9431488, alpha * strength * travel * 0.3f);
        }
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        float inv = t - 1.0f;
        return 1.0f + c3 * inv * inv * inv + c1 * inv * inv;
    }

    private static int hash(int seed) {
        int h = seed * -1640531527;
        h ^= h >>> 15;
        h *= -2048144789;
        h ^= h >>> 13;
        return h & Integer.MAX_VALUE;
    }
}

