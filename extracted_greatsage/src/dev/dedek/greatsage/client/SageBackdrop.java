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

final class SageBackdrop {
    private static final int DEEP = 267278;
    private static final int NEBULA = 3046747;
    private static final int MOTES = 46;
    private static final int CLOUDS = 9;

    private SageBackdrop() {
    }

    static void render(GuiGraphics graphics, int width, int height, float time, float alpha) {
        int a = Mth.clamp((int)Math.round(alpha * 255.0f), (int)0, (int)255);
        if (a <= 0) {
            return;
        }
        graphics.fill(0, 0, width, height, a << 24 | 0x4140E);
        graphics.flush();
        LightFx.beginAdditive();
        SageBackdrop.clouds(graphics, width, height, time, alpha);
        SageBackdrop.motes(graphics, width, height, time, alpha);
        LightFx.endAdditive(graphics);
        graphics.flush();
        int bands = 8;
        int stepX = Math.max(1, width / (bands * 2));
        int stepY = Math.max(1, height / (bands * 2));
        int band = Mth.clamp((int)Math.round(alpha * 0.055f * 255.0f), (int)0, (int)255);
        if (band <= 0) {
            return;
        }
        for (int i = 0; i < bands; ++i) {
            int l = i * stepX;
            int t = i * stepY;
            int color = band << 24 | 0x4140E;
            graphics.fill(l, t, l + stepX, height - t, color);
            graphics.fill(width - l - stepX, t, width - l, height - t, color);
            graphics.fill(l + stepX, t, width - l - stepX, t + stepY, color);
            graphics.fill(l + stepX, height - t - stepY, width - l - stepX, height - t, color);
        }
    }

    private static void clouds(GuiGraphics graphics, int width, int height, float time, float alpha) {
        for (int i = 0; i < 9; ++i) {
            int h = SageBackdrop.hash(i * 17 + 3);
            float speed = 1.2E-5f + (float)(h >> 4 & 0xF) / 15.0f * 2.2E-5f;
            float x = ((float)(h & 0x3FF) / 1023.0f + time * speed) % 1.2f - 0.1f;
            float y = (float)(h >> 10 & 0x3FF) / 1023.0f;
            float w = (float)width * (0.3f + (float)(h >> 20 & 7) / 7.0f * 0.45f);
            float tall = (float)height * (0.22f + (float)(h >> 23 & 7) / 7.0f * 0.35f);
            float breath = 0.72f + 0.28f * Mth.sin((float)(time * 1.3E-4f + (float)i));
            LightFx.glow(graphics, x * (float)width, y * (float)height, w * breath, tall * breath, 3046747, alpha * 0.085f);
        }
    }

    private static void motes(GuiGraphics graphics, int width, int height, float time, float alpha) {
        for (int i = 0; i < 46; ++i) {
            int h = SageBackdrop.hash(i * 29 + 11);
            float speed = 3.0E-5f + (float)(h >> 6 & 0x1F) / 31.0f * 7.5E-5f;
            float x = (float)(h & 0x3FF) / 1023.0f * (float)width;
            float y = (1.05f - ((float)(h >> 11 & 0x3FF) / 1023.0f + time * speed) % 1.0f) * (float)height;
            float size = 2.0f + (float)(h >> 21 & 7) * 0.85f;
            float twinkle = 0.55f + 0.45f * Mth.sin((float)(time * 0.0016f + (float)i * 1.7f));
            LightFx.glow(graphics, x, y, size, 9431488, alpha * twinkle * 0.42f);
        }
    }

    private static int hash(int seed) {
        int h = seed * -1640531527;
        h ^= h >>> 15;
        h *= -2048144789;
        h ^= h >>> 13;
        return h & Integer.MAX_VALUE;
    }
}

