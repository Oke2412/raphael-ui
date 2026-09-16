/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.DeltaTracker
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.LayeredDraw$Layer
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.util.Mth
 */
package dev.dedek.greatsage.client;

import dev.dedek.greatsage.client.voice.VoiceInstall;
import java.util.Objects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

public final class VoiceInstallOverlay
implements LayeredDraw.Layer {
    private static final int WIDTH = 132;
    private static final int HEIGHT = 3;
    private static final int MARGIN = 8;
    private static final long LINGER_MS = 6000L;
    private static final int SAGE = 14679276;
    private static final int SAGE_DEEP = 9431488;
    private static final int RAIL = 3038284;
    private static final int BACKDROP = 200972;
    private static final int ALERT = 16738890;
    private static long finishedAt;

    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        VoiceInstallOverlay.draw(graphics, minecraft, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
    }

    public static void draw(GuiGraphics graphics, Minecraft minecraft, int screenWidth, int screenHeight) {
        boolean failed;
        VoiceInstall.Phase phase = VoiceInstall.phase();
        if (phase == VoiceInstall.Phase.IDLE) {
            return;
        }
        boolean bl = failed = phase == VoiceInstall.Phase.FAILED;
        if (phase == VoiceInstall.Phase.DONE) {
            if (finishedAt == 0L) {
                finishedAt = System.currentTimeMillis();
            }
            if (System.currentTimeMillis() - finishedAt > 6000L) {
                return;
            }
        }
        Font font = minecraft.font;
        int percent = Mth.clamp((int)VoiceInstall.percent(), (int)0, (int)100);
        MutableComponent label = failed ? Component.translatable((String)"greatsage.voice.install.bar.failed") : (phase == VoiceInstall.Phase.DONE ? Component.translatable((String)"greatsage.voice.install.bar.done") : Component.translatable((String)"greatsage.voice.install.bar", (Object[])new Object[]{percent}));
        int textWidth = font.width((FormattedText)label);
        int boxWidth = Math.max(132, textWidth) + 12;
        Objects.requireNonNull(font);
        int boxHeight = 9 + 3 + 13;
        int left = screenWidth - boxWidth - 8;
        int top = screenHeight - boxHeight - 8;
        graphics.fill(left, top, left + boxWidth, top + boxHeight, -1157426932);
        graphics.fill(left, top, left + boxWidth, top + 1, 1720707520);
        graphics.drawString(font, (Component)label, left + 6, top + 5, 0xFF000000 | (failed ? 16738890 : 14679276), false);
        int barLeft = left + 6;
        Objects.requireNonNull(font);
        int barTop = top + 9 + 8;
        int barWidth = boxWidth - 12;
        graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + 3, -13738932);
        if (!failed) {
            int filled;
            int n = filled = phase == VoiceInstall.Phase.DONE ? barWidth : Math.round((float)(barWidth * percent) / 100.0f);
            if (filled > 0) {
                graphics.fill(barLeft, barTop, barLeft + filled, barTop + 3, -7345728);
                graphics.fill(barLeft + Math.max(0, filled - 2), barTop, barLeft + filled, barTop + 3, -2097940);
            }
        } else {
            graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + 3, -38326);
        }
    }
}

