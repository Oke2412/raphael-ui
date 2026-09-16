/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.resources.language.I18n
 *  net.minecraft.util.Mth
 */
package dev.dedek.greatsage.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dedek.greatsage.client.Announcement;
import dev.dedek.greatsage.client.AnnouncementManager;
import dev.dedek.greatsage.client.AnnouncementOverlay;
import dev.dedek.greatsage.client.LightFx;
import dev.dedek.greatsage.client.SageCore;
import dev.dedek.greatsage.client.Subtitles;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.Mth;

final class CompactAnnouncement {
    private static final float SLIDE_MS = 520.0f;
    private static final float MARGIN = 14.0f;
    private static final float CORE = 22.0f;
    private static final int VISIBLE_LINES = 4;
    private static final float SCALE = 0.8f;
    private static final int PADDING = 6;
    private static final int FOLD = 172;
    private static final String ANALYSIS_CLIP = "voice.kaiseki_kanryou";
    private static final String ANALYSIS_TEXT = "greatsage.voice.kaiseki_kanryou";

    private CompactAnnouncement() {
    }

    private static List<String> fold(Font font, String sentence) {
        ArrayList<String> out = new ArrayList<String>();
        StringBuilder line = new StringBuilder();
        for (String word : sentence.split(" ")) {
            String probe;
            String string = probe = line.isEmpty() ? word : String.valueOf(line) + " " + word;
            if (font.width(probe) > 172 && !line.isEmpty()) {
                out.add(line.toString());
                line = new StringBuilder(word);
                continue;
            }
            line = new StringBuilder(probe);
        }
        if (!line.isEmpty()) {
            out.add(line.toString());
        }
        return out;
    }

    static void draw(GuiGraphics graphics, Announcement announcement, float time, float total) {
        float out;
        float in = Mth.clamp((float)(time / 520.0f), (float)0.0f, (float)1.0f);
        float slide = CompactAnnouncement.ease(Math.min(in, out = Mth.clamp((float)((total - time) / 520.0f), (float)0.0f, (float)1.0f)));
        if (slide <= 0.005f) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float restX = (float)width - 14.0f - 22.0f;
        float restY = (float)height - 14.0f - 22.0f;
        float cx = Mth.lerp((float)slide, (float)(restX + 74.8f), (float)restX);
        float cy = Mth.lerp((float)slide, (float)(restY + 74.8f), (float)restY);
        float voice = AnnouncementManager.amplitude();
        CompactAnnouncement.bubble(graphics, font, announcement, cx, cy, time, slide);
        graphics.flush();
        LightFx.beginAdditive();
        SageCore.render(graphics, cx, cy, 22.0f, time, 1.0f, slide, AnnouncementOverlay.tierColor(announcement.topTier()), voice);
        LightFx.endAdditive(graphics);
        graphics.flush();
        RenderSystem.disableBlend();
    }

    private static void bubble(GuiGraphics graphics, Font font, Announcement announcement, float cx, float cy, float time, float slide) {
        Announcement.Step step;
        ArrayList<Announcement.Line> shown = new ArrayList<Announcement.Line>();
        for (Announcement.Line line : announcement.lines()) {
            if (!(time >= (float)line.revealAtMs())) continue;
            shown.add(line);
        }
        if (shown.isEmpty()) {
            return;
        }
        while (shown.size() > 4) {
            shown.remove(0);
        }
        Objects.requireNonNull(font);
        int lineHeight = 9 + 2;
        int widest = 0;
        ArrayList<Row> rows = new ArrayList<Row>();
        for (Announcement.Line line : shown) {
            boolean tier = line.type() == Announcement.LineType.TIER;
            String text = tier ? line.text().getString().toUpperCase(Locale.ROOT) : line.text().getString();
            float since = time - (float)line.revealAtMs();
            float fresh = 1.0f - Mth.clamp((float)(since / 400.0f), (float)0.0f, (float)1.0f);
            int color = tier ? AnnouncementOverlay.tierColor(line.tier()) : (fresh > 0.5f ? 0xFFFFFF : 14679276);
            rows.add(new Row(text, color, tier ? 0.75f : 0.95f));
            widest = Math.max(widest, font.width(text));
        }
        if (announcement.analysis() != null && (step = Subtitles.stepOf(announcement, ANALYSIS_CLIP)) != null && time >= (float)step.atMs() && I18n.exists((String)ANALYSIS_TEXT)) {
            for (String part : CompactAnnouncement.fold(font, I18n.get((String)ANALYSIS_TEXT, (Object[])new Object[0]))) {
                rows.add(new Row(part, 14679276, 0.82f));
                widest = Math.max(widest, font.width(part));
            }
        }
        int boxWidth = widest + 12;
        int n = rows.size() * lineHeight + 12 - 2;
        float right = cx - 25.3f;
        float top = cy - (float)n * 0.8f / 2.0f;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(right - (float)boxWidth * 0.8f, top, 0.0f);
        pose.scale(0.8f, 0.8f, 1.0f);
        int a = Mth.clamp((int)Math.round(slide * 255.0f), (int)0, (int)255);
        graphics.fill(0, 0, boxWidth, n, Math.round((float)a * 0.62f) << 24 | 0x4140E);
        int edge = Math.round((float)a * 0.45f) << 24 | 0x8FE9C0;
        graphics.fill(boxWidth - 1, 0, boxWidth, n, edge);
        for (int i = 0; i < rows.size(); ++i) {
            Row row = (Row)rows.get(i);
            int alpha = Math.round((float)a * row.alpha());
            graphics.drawString(font, row.text(), 6, 6 + i * lineHeight, alpha << 24 | row.color(), true);
        }
        pose.popPose();
    }

    private static float ease(float t) {
        float inv = 1.0f - Mth.clamp((float)t, (float)0.0f, (float)1.0f);
        return 1.0f - inv * inv * inv;
    }

    private record Row(String text, int color, float alpha) {
    }
}

