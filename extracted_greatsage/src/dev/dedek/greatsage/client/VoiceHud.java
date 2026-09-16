/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.Util
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.util.Mth
 */
package dev.dedek.greatsage.client;

import dev.dedek.greatsage.client.LightFx;
import dev.dedek.greatsage.client.SageCore;
import dev.dedek.greatsage.client.SageDialogue;
import dev.dedek.greatsage.client.voice.VoiceInput;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class VoiceHud {
    private static final float FADE_MS = 260.0f;

    private VoiceHud() {
    }

    public static void render(GuiGraphics graphics) {
        float alpha;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        VoiceInput.State state = VoiceInput.state();
        boolean speaking = SageDialogue.active();
        if (state == VoiceInput.State.ASLEEP && !speaking) {
            return;
        }
        float since = Util.getMillis() - VoiceInput.stateSince();
        float f = alpha = state == VoiceInput.State.ASLEEP ? 1.0f : Mth.clamp((float)(since / 260.0f), (float)0.0f, (float)1.0f);
        if (alpha < 0.02f) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float min = Math.min(width, height);
        float radius = min * 0.055f;
        float cx = radius + 18.0f;
        float cy = (float)height - radius - 18.0f;
        float time = Util.getMillis() % 1000000L;
        LightFx.ensureTextures();
        graphics.flush();
        LightFx.beginAdditive();
        float glow = state == VoiceInput.State.LISTENING ? 1.0f : 0.72f;
        SageCore.render(graphics, cx, cy, radius, time, 1.0f, alpha * glow, 9431488, 0.0f);
        if (state == VoiceInput.State.LISTENING) {
            float left = VoiceInput.window();
            if (left > 0.0f) {
                LightFx.arc(graphics, cx, cy, radius * 1.5f, -90.0f, 359.9f * left, 2.6f, 0xFFFFFF, alpha * 0.9f);
            }
            float pulse = time % 1300.0f / 1300.0f;
            LightFx.arc(graphics, cx, cy, radius * (1.5f + pulse * 0.9f), 0.0f, 359.9f, 1.8f, 14679276, alpha * 0.35f * (1.0f - pulse));
        } else if (state == VoiceInput.State.THINKING) {
            float sweep = time % 900.0f / 900.0f * 360.0f;
            LightFx.arc(graphics, cx, cy, radius * 1.5f, sweep, 90.0f, 2.6f, 0xFFFFFF, alpha * 0.85f);
        }
        LightFx.endAdditive(graphics);
        graphics.flush();
        SageDialogue.render(graphics, cx + radius * 1.7f, cy - radius * 1.6f, alpha);
    }
}

