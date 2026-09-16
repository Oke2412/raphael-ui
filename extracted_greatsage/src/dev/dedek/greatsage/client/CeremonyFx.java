/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.util.Mth
 */
package dev.dedek.greatsage.client;

import dev.dedek.greatsage.CeremonyType;
import dev.dedek.greatsage.client.LightFx;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

final class CeremonyFx {
    private static final int WHITE = 0xFFFFFF;
    private static final float GOLDEN_ANGLE = 2.399963f;

    private CeremonyFx() {
    }

    static int accent(CeremonyType type) {
        return switch (type) {
            default -> throw new MatchException(null, null);
            case CeremonyType.HERO_EGG -> 16771496;
            case CeremonyType.DEMON_LORD_SEED -> 11559423;
            case CeremonyType.HARVEST_FESTIVAL -> 16751164;
            case CeremonyType.TRUE_HERO -> 0xFFF4C4;
            case CeremonyType.TRUE_DEMON_LORD -> 0xFF4040;
        };
    }

    static float darkenScale(CeremonyType type) {
        return switch (type) {
            default -> throw new MatchException(null, null);
            case CeremonyType.HERO_EGG -> 0.75f;
            case CeremonyType.DEMON_LORD_SEED -> 1.25f;
            case CeremonyType.HARVEST_FESTIVAL -> 1.15f;
            case CeremonyType.TRUE_HERO -> 0.85f;
            case CeremonyType.TRUE_DEMON_LORD -> 1.3f;
        };
    }

    static void draw(GuiGraphics graphics, CeremonyType type, int width, int height, float time, float global) {
        int color = CeremonyFx.accent(type);
        float cx = (float)width / 2.0f;
        float cy = (float)height / 2.0f;
        switch (type) {
            case HERO_EGG: {
                CeremonyFx.heroEgg(graphics, width, height, cx, cy, time, global, color);
                break;
            }
            case DEMON_LORD_SEED: {
                CeremonyFx.demonLordSeed(graphics, width, height, cx, cy, time, global, color);
                break;
            }
            case HARVEST_FESTIVAL: {
                CeremonyFx.harvestFestival(graphics, width, height, cx, cy, time, global, color);
                break;
            }
            case TRUE_HERO: {
                CeremonyFx.radiant(graphics, width, height, cx, cy, time, global, color, true);
                break;
            }
            case TRUE_DEMON_LORD: {
                CeremonyFx.radiant(graphics, width, height, cx, cy, time, global, color, false);
            }
        }
    }

    private static void heroEgg(GuiGraphics graphics, int width, int height, float cx, float cy, float time, float global, int color) {
        float grow = CeremonyFx.ease(Mth.clamp((float)(time / 1400.0f), (float)0.0f, (float)1.0f));
        LightFx.beam(graphics, cx, cy, (float)height * 1.2f, 190.0f * grow, 90.0f, color, global * 0.16f);
        LightFx.beam(graphics, cx, cy, (float)height * 1.2f, 62.0f * grow, 90.0f, color, global * 0.22f);
        LightFx.beam(graphics, cx, cy, (float)height * 1.2f, 16.0f * grow, 90.0f, 0xFFFFFF, global * 0.2f);
        for (int i = 0; i < 44; ++i) {
            float phase = (time / 2600.0f + (float)i * 0.6180339f) % 1.0f;
            float x = cx + Mth.sin((float)((float)i * 2.399963f)) * (22.0f + (float)(i % 7) * 16.0f);
            float y = (float)height - phase * (float)height * 1.1f;
            LightFx.glow(graphics, x, y, 4.0f + (float)(i % 3) * 3.0f, color, global * (1.0f - phase) * 0.75f);
        }
        float radius = grow * (float)Math.min(width, height) * 0.42f;
        LightFx.ring(graphics, cx, cy, radius, radius * 0.72f, color, global * 0.4f);
    }

    private static void demonLordSeed(GuiGraphics graphics, int width, int height, float cx, float cy, float time, float global, int color) {
        int i;
        float pulse = 0.5f + 0.5f * Mth.sin((float)(time / 320.0f));
        float radius = (float)Math.min(width, height) * 0.17f * (0.85f + pulse * 0.15f);
        LightFx.glow(graphics, cx, cy, radius * 2.6f, radius * 2.2f, color, global * (0.18f + pulse * 0.12f));
        for (i = 0; i < 3; ++i) {
            LightFx.ring(graphics, cx, cy, radius * (1.0f - (float)i * 0.2f), radius * (0.85f - (float)i * 0.17f), color, global * (0.34f - (float)i * 0.08f));
        }
        for (i = 0; i < 38; ++i) {
            float phase = (time / 1700.0f + (float)i * 0.6180339f) % 1.0f;
            float x = i * 97 % Math.max(1, width);
            float y = phase * (float)height;
            float fade = 1.0f - Math.abs(phase - 0.5f) * 2.0f;
            LightFx.beam(graphics, x, y, 16.0f + (float)(i % 4) * 8.0f, 3.0f, 90.0f, color, global * fade * 0.6f);
        }
        float close = CeremonyFx.ease(Mth.clamp((float)(time / 1600.0f), (float)0.0f, (float)1.0f));
        float crown = (float)Math.min(width, height) * (0.46f - 0.1f * close);
        for (int seg = 0; seg < 6; ++seg) {
            LightFx.arc(graphics, cx, cy, crown, (float)seg * 60.0f + time * 0.01f, 26.0f, 3.0f, color, global * 0.6f);
        }
    }

    private static void harvestFestival(GuiGraphics graphics, int width, int height, float cx, float cy, float time, float global, int color) {
        for (int i = 0; i < 70; ++i) {
            float phase = (time / 3200.0f + (float)i * 0.6180339f) % 1.0f;
            float angle = (float)i * 2.399963f + time * 8.0E-5f;
            float distance = (1.0f - CeremonyFx.ease(phase)) * (float)Math.max(width, height) * 0.62f;
            LightFx.glow(graphics, cx + Mth.cos((float)angle) * distance, cy + Mth.sin((float)angle) * distance * 0.72f, 3.0f + (1.0f - phase) * 6.0f, color, global * phase * 0.8f);
        }
        float base = (float)Math.min(width, height) * 0.3f;
        float grow = CeremonyFx.ease(Mth.clamp((float)(time / 2200.0f), (float)0.0f, (float)1.0f));
        for (int i = 0; i < 3; ++i) {
            float r = base * grow * (1.0f + (float)i * 0.42f);
            LightFx.ring(graphics, cx, cy, r, r * 0.7f, color, global * (0.36f - (float)i * 0.09f));
        }
        LightFx.glow(graphics, cx, cy, (float)width * 1.4f, (float)height * 1.1f, color, global * 0.1f * (0.5f + 0.5f * Mth.sin((float)(time / 900.0f))));
    }

    private static void radiant(GuiGraphics graphics, int width, int height, float cx, float cy, float time, float global, int color, boolean bright) {
        float burst = CeremonyFx.ease(Mth.clamp((float)(time / 900.0f), (float)0.0f, (float)1.0f));
        float reach = (float)Math.max(width, height) * 0.8f;
        for (int pass = 0; pass < 2; ++pass) {
            int rays = pass == 0 ? 16 : 24;
            float spin = time * (pass == 0 ? 0.009f : -0.006f);
            float thickness = pass == 0 ? 13.0f : 6.0f;
            float strength = pass == 0 ? 0.42f : 0.24f;
            for (int i = 0; i < rays; ++i) {
                float angle = spin + 360.0f * (float)i / (float)rays;
                LightFx.ray(graphics, cx, cy, reach * burst, thickness, angle, color, global * strength);
            }
        }
        float wave = Mth.clamp((float)(time / 1500.0f), (float)0.0f, (float)1.0f);
        if (wave < 1.0f) {
            float r = CeremonyFx.ease(wave) * reach;
            LightFx.ring(graphics, cx, cy, r, r * 0.72f, 0xFFFFFF, global * (1.0f - wave) * 0.55f);
        }
        float flash = 1.0f - Mth.clamp((float)(time / 620.0f), (float)0.0f, (float)1.0f);
        LightFx.glow(graphics, cx, cy, (float)width * 1.8f, (float)height * 1.5f, bright ? 0xFFFFFF : color, global * flash * (bright ? 0.62f : 0.48f));
    }

    private static float ease(float t) {
        float inv = 1.0f - t;
        return 1.0f - inv * inv * inv;
    }
}

