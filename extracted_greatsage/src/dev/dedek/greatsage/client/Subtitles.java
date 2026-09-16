/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
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

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dedek.greatsage.CeremonyType;
import dev.dedek.greatsage.SoundManifest;
import dev.dedek.greatsage.client.Announcement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

final class Subtitles {
    private static final int MAX_SEGMENTS = 12;
    private static final float FADE_MS = 180.0f;
    private static final int BOTTOM_MARGIN = 46;
    private static final float SCALE = 0.85f;
    private static final float TEXT_OPACITY = 0.74f;
    private static final float BACKDROP_OPACITY = 0.26f;
    private static final int PADDING = 5;
    private static final float SILENCE_LEVEL = 0.12f;
    private static final int MIN_GAP_TICKS = 4;
    private static final int TEXT_COLOR = 12571849;

    private Subtitles() {
    }

    static void render(GuiGraphics graphics, Announcement announcement, float time, float global) {
        CeremonyType ceremony = announcement.ceremony();
        if (ceremony == null) {
            if (announcement.evolution() != null) {
                Subtitles.renderEvolution(graphics, announcement, time, global);
            } else if (announcement.analysis() != null) {
                Subtitles.renderClip(graphics, announcement, "voice.kaiseki_kanryou", "greatsage.voice.kaiseki_kanryou", time, global);
            }
            return;
        }
        int spoken = SoundManifest.durationMs(ceremony.soundKey());
        if (spoken <= 0) {
            return;
        }
        List<String> segments = Subtitles.segments(ceremony);
        if (segments.isEmpty()) {
            return;
        }
        float[] bounds = Subtitles.boundaries(ceremony, segments.size(), spoken);
        String current = null;
        float localTime = 0.0f;
        float span = 0.0f;
        for (int i = 0; i < segments.size(); ++i) {
            if (!(time >= bounds[i]) || !(time < bounds[i + 1])) continue;
            current = segments.get(i);
            localTime = time - bounds[i];
            span = bounds[i + 1] - bounds[i];
            break;
        }
        if (current == null) {
            return;
        }
        Subtitles.paint(graphics, current, localTime, span, global);
    }

    private static void paint(GuiGraphics graphics, String current, float localTime, float span, float global) {
        int textAlpha;
        float fade = Math.min(Mth.clamp((float)(localTime / 180.0f), (float)0.0f, (float)1.0f), Mth.clamp((float)((span - localTime) / 180.0f), (float)0.0f, (float)1.0f));
        int a = Mth.clamp((int)Math.round(fade * global * 255.0f), (int)0, (int)255);
        if (a < 8) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int wrapWidth = Math.round((float)Math.min(300, width - 60) / 0.85f);
        List wrapped = font.split((FormattedText)Component.literal((String)current), wrapWidth);
        Objects.requireNonNull(font);
        int lineHeight = 9 + 1;
        int blockHeight = wrapped.size() * lineHeight;
        int widest = 0;
        for (FormattedCharSequence line : wrapped) {
            widest = Math.max(widest, font.width(line));
        }
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate((float)width / 2.0f, (float)(height - 46) - (float)blockHeight * 0.85f, 0.0f);
        pose.scale(0.85f, 0.85f, 1.0f);
        int backdrop = Math.round((float)a * 0.26f);
        if (backdrop >= 6) {
            graphics.fill(-widest / 2 - 5, -5, widest / 2 + 5, blockHeight + 5 - 1, backdrop << 24);
        }
        if ((textAlpha = Math.round((float)a * 0.74f)) >= 8) {
            for (int i = 0; i < wrapped.size(); ++i) {
                FormattedCharSequence line = (FormattedCharSequence)wrapped.get(i);
                graphics.drawString(font, line, -font.width(line) / 2, i * lineHeight, textAlpha << 24 | 0xBFD4C9, true);
            }
        }
        pose.popPose();
    }

    private static float[] boundaries(CeremonyType ceremony, int count, int spoken) {
        float[] bounds = new float[count + 1];
        bounds[0] = 0.0f;
        bounds[count] = spoken;
        if (count <= 1) {
            return bounds;
        }
        List<int[]> gaps = Subtitles.silentGaps(ceremony.soundKey());
        if (gaps.size() >= count - 1) {
            gaps.sort((a, b) -> Integer.compare(b[1] - b[0], a[1] - a[0]));
            ArrayList<int[]> chosen = new ArrayList<int[]>(gaps.subList(0, count - 1));
            chosen.sort((a, b) -> Integer.compare(a[0], b[0]));
            for (int i = 0; i < chosen.size(); ++i) {
                int[] gap = (int[])chosen.get(i);
                bounds[i + 1] = (float)(gap[0] + gap[1]) * 0.5f * 50.0f;
            }
            return bounds;
        }
        return Subtitles.proportional(ceremony, count, spoken, bounds);
    }

    private static List<int[]> silentGaps(String soundKey) {
        ArrayList<int[]> gaps = new ArrayList<int[]>();
        int length = SoundManifest.durationMs(soundKey) / 50;
        int start = -1;
        for (int tick = 2; tick < length - 2; ++tick) {
            boolean quiet;
            boolean bl = quiet = SoundManifest.amplitudeAt(soundKey, tick) < 0.12f;
            if (quiet && start < 0) {
                start = tick;
                continue;
            }
            if (quiet || start < 0) continue;
            if (tick - start >= 4) {
                gaps.add(new int[]{start, tick});
            }
            start = -1;
        }
        return gaps;
    }

    private static float[] proportional(CeremonyType ceremony, int count, int spoken, float[] bounds) {
        List<String> segments = Subtitles.segments(ceremony);
        int totalChars = 0;
        for (String segment : segments) {
            totalChars += Math.max(1, segment.length());
        }
        float cursor = 0.0f;
        for (int i = 0; i < count - 1; ++i) {
            bounds[i + 1] = cursor += (float)spoken * ((float)Math.max(1, segments.get(i).length()) / (float)totalChars);
        }
        return bounds;
    }

    private static void renderEvolution(GuiGraphics graphics, Announcement announcement, float time, float global) {
        String[] keys = new String[]{"voice.koku", "voice.shinka_jouken", "voice.shounin", "voice.e_no_shinka"};
        for (int i = 0; i < keys.length; ++i) {
            Announcement.Step step = Subtitles.stepOf(announcement, keys[i]);
            if (step == null) continue;
            int length = SoundManifest.durationMs(keys[i]);
            if (!(time >= (float)step.atMs()) || !(time < (float)(step.atMs() + length))) continue;
            String key = "greatsage.subtitle.evolution." + i;
            if (I18n.exists((String)key)) {
                Subtitles.paint(graphics, I18n.get((String)key, (Object[])new Object[0]), time - (float)step.atMs(), length, global);
            }
            return;
        }
    }

    private static void renderClip(GuiGraphics graphics, Announcement announcement, String soundKey, String textKey, float time, float global) {
        Announcement.Step step = Subtitles.stepOf(announcement, soundKey);
        if (step == null || !I18n.exists((String)textKey)) {
            return;
        }
        int length = SoundManifest.durationMs(soundKey);
        if (time >= (float)step.atMs() && time < (float)(step.atMs() + length)) {
            Subtitles.paint(graphics, I18n.get((String)textKey, (Object[])new Object[0]), time - (float)step.atMs(), length, global);
        }
    }

    static Announcement.Step stepOf(Announcement announcement, String soundKey) {
        for (Announcement.Step step : announcement.steps()) {
            if (!step.soundKey().equals(soundKey)) continue;
            return step;
        }
        return null;
    }

    private static List<String> segments(CeremonyType ceremony) {
        String key;
        ArrayList<String> out = new ArrayList<String>();
        for (int i = 0; i < 12 && I18n.exists((String)(key = "greatsage.subtitle." + ceremony.id() + "." + i)); ++i) {
            out.add(I18n.get((String)key, (Object[])new Object[0]));
        }
        return out;
    }
}

