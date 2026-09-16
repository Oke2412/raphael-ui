/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.PoseStack
 *  javax.annotation.Nullable
 *  net.minecraft.Util
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.resources.language.I18n
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.util.FormattedCharSequence
 *  net.minecraft.util.Mth
 */
package dev.dedek.greatsage.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dedek.greatsage.GreatSageConfig;
import dev.dedek.greatsage.GreatSageTip;
import dev.dedek.greatsage.SoundManifest;
import dev.dedek.greatsage.client.AnnouncementManager;
import dev.dedek.greatsage.client.AssessMode;
import dev.dedek.greatsage.client.LightFx;
import dev.dedek.greatsage.client.SageCore;
import dev.dedek.greatsage.client.SageDialogue;
import dev.dedek.greatsage.client.voice.VoiceInput;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public final class SageTips {
    private static final float SLIDE_MS = 480.0f;
    private static final float LINGER_MS = 2600.0f;
    private static final float MARGIN = 14.0f;
    private static final float CORE = 20.0f;
    private static final int WIDTH = 150;
    private static final int PADDING = 6;
    private static final float SCALE = 0.8f;
    @Nullable
    private static GreatSageTip current;
    private static long startedAt;
    private static float lengthMs;

    private SageTips() {
    }

    public static void accept(String id) {
        GreatSageTip tip = GreatSageTip.byId(id);
        if (tip == null) {
            return;
        }
        if (VoiceInput.busy() || SageDialogue.active()) {
            return;
        }
        current = tip;
        startedAt = Util.getMillis();
        lengthMs = Math.max(1400.0f, (float)SoundManifest.durationMs(tip.soundKey()));
        SageDialogue.say(tip.soundKey());
    }

    public static void clear() {
        current = null;
    }

    private static boolean active() {
        return current != null && (float)(Util.getMillis() - startedAt) < lengthMs + 2600.0f;
    }

    public static void render(GuiGraphics graphics) {
        if (!SageTips.active() || !((Boolean)GreatSageConfig.ENABLE_OVERLAY.get()).booleanValue()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (AssessMode.active()) {
            return;
        }
        LightFx.ensureTextures();
        float since = Util.getMillis() - startedAt;
        float in = Mth.clamp((float)(since / 480.0f), (float)0.0f, (float)1.0f);
        float out = Mth.clamp((float)((lengthMs + 2600.0f - since) / 480.0f), (float)0.0f, (float)1.0f);
        float slide = SageTips.ease(Math.min(in, out));
        if (slide <= 0.005f) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float restX = (float)width - 14.0f - 20.0f;
        float restY = (float)height - 14.0f - 20.0f;
        float cx = Mth.lerp((float)slide, (float)(restX + 68.0f), (float)restX);
        float cy = Mth.lerp((float)slide, (float)(restY + 68.0f), (float)restY);
        SageTips.bubble(graphics, minecraft.font, cx, cy, since, slide);
        graphics.flush();
        LightFx.beginAdditive();
        SageCore.render(graphics, cx, cy, 20.0f, since, 1.0f, slide, 9431488, AnnouncementManager.amplitude());
        LightFx.endAdditive(graphics);
        graphics.flush();
        RenderSystem.disableBlend();
    }

    private static void bubble(GuiGraphics graphics, Font font, float cx, float cy, float since, float slide) {
        String key = current.translationKey();
        if (!I18n.exists((String)key)) {
            return;
        }
        String full = I18n.get((String)key, (Object[])new Object[0]);
        int shown = Mth.clamp((int)Math.round((float)full.length() * (since / lengthMs)), (int)1, (int)full.length());
        List lines = font.split((FormattedText)Component.literal((String)full.substring(0, shown)), 150);
        Objects.requireNonNull(font);
        int lineHeight = 9 + 2;
        int boxHeight = Math.max(lineHeight, lines.size() * lineHeight) + 12;
        int widest = 0;
        for (FormattedCharSequence line : lines) {
            widest = Math.max(widest, font.width(line));
        }
        int boxWidth = Math.max(70, widest) + 12;
        float right = cx - 23.0f;
        float top = cy - (float)boxHeight * 0.8f / 2.0f;
        int a = Mth.clamp((int)Math.round(slide * 255.0f), (int)0, (int)255);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(right - (float)boxWidth * 0.8f, top, 0.0f);
        pose.scale(0.8f, 0.8f, 1.0f);
        graphics.fill(0, 0, boxWidth, boxHeight, Math.round((float)a * 0.66f) << 24 | 0x4140E);
        int edge = Math.round((float)a * 0.45f) << 24 | 0x8FE9C0;
        graphics.fill(boxWidth - 1, 0, boxWidth, boxHeight, edge);
        for (int i = 0; i < lines.size(); ++i) {
            graphics.drawString(font, (FormattedCharSequence)lines.get(i), 6, 6 + i * lineHeight, Math.round((float)a * 0.95f) << 24 | 0xDFFCEC, true);
        }
        pose.popPose();
    }

    private static float ease(float t) {
        float inv = 1.0f - Mth.clamp((float)t, (float)0.0f, (float)1.0f);
        return 1.0f - inv * inv * inv;
    }
}

