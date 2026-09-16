/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.DeltaTracker
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.LayeredDraw$Layer
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.util.Mth
 */
package dev.dedek.greatsage.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dedek.greatsage.CeremonyType;
import dev.dedek.greatsage.GreatSageConfig;
import dev.dedek.greatsage.client.Announcement;
import dev.dedek.greatsage.client.AnnouncementManager;
import dev.dedek.greatsage.client.CeremonyFx;
import dev.dedek.greatsage.client.CompactAnnouncement;
import dev.dedek.greatsage.client.EvolutionFx;
import dev.dedek.greatsage.client.LightFx;
import dev.dedek.greatsage.client.SageCore;
import dev.dedek.greatsage.client.Subtitles;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;

public final class AnnouncementOverlay
implements LayeredDraw.Layer {
    private static final float FADE_IN_MS = 240.0f;
    private static final float FADE_OUT_MS = 560.0f;
    private static final float BUILD_MS = 900.0f;
    private static final float TYPE_MS = 420.0f;
    private static final float CHAR_FADE_MS = 130.0f;
    private static final float SLAM_SOLO_SCALE = 4.0f;
    private static final float SLAM_GROUP_SCALE = 2.3f;
    private static final int TIER_LINE_HEIGHT = 14;
    private static final int SKILL_LINE_HEIGHT = 16;
    private static final int CEREMONY_LINE_HEIGHT = 32;
    private static final int GROUP_SPACING = 9;
    private static final float CEREMONY_SCALE = 1.9f;
    private static final float RACE_SCALE = 2.6f;
    private static final int VEIL_TINT = 200972;

    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        AnnouncementOverlay.draw(graphics, deltaTracker);
    }

    public static void renderWithHiddenHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (Minecraft.getInstance().options.hideGui && ((Boolean)GreatSageConfig.SHOW_WITH_HIDDEN_HUD.get()).booleanValue()) {
            AnnouncementOverlay.draw(graphics, deltaTracker);
        }
    }

    private static void draw(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!((Boolean)GreatSageConfig.ENABLE_OVERLAY.get()).booleanValue()) {
            return;
        }
        AnnouncementManager.update();
        Announcement announcement = AnnouncementManager.current();
        if (announcement == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LightFx.ensureTextures();
        float time = (float)AnnouncementManager.elapsedMs();
        float total = announcement.durationMs();
        float global = Math.min(Mth.clamp((float)(time / 240.0f), (float)0.0f, (float)1.0f), Mth.clamp((float)((total - time) / 560.0f), (float)0.0f, (float)1.0f));
        if (global <= 0.0f) {
            return;
        }
        if (((Boolean)GreatSageConfig.COMPACT_SKILLS.get()).booleanValue() && announcement.ceremony() == null && announcement.evolution() == null) {
            CompactAnnouncement.draw(graphics, announcement, time, total);
            return;
        }
        Font font = minecraft.font;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float centerX = (float)width / 2.0f;
        int[] pages = AnnouncementOverlay.paginate(announcement.lines(), height);
        int page = AnnouncementOverlay.currentPage(announcement.lines(), pages, time);
        ArrayList<Announcement.Line> lines = new ArrayList<Announcement.Line>();
        ArrayList<Integer> indices = new ArrayList<Integer>();
        for (int i = 0; i < announcement.lines().size(); ++i) {
            if (pages[i] != page) continue;
            lines.add(announcement.lines().get(i));
            indices.add(i);
        }
        if (lines.isEmpty()) {
            return;
        }
        float[] offsets = AnnouncementOverlay.layout(lines);
        float blockHeight = offsets[lines.size()];
        float topY = (float)height / 2.0f - blockHeight / 2.0f;
        float centerY = topY + blockHeight / 2.0f;
        CeremonyType ceremony = announcement.ceremony();
        int accent = ceremony != null ? CeremonyFx.accent(ceremony) : (announcement.evolution() != null ? 16771496 : AnnouncementOverlay.tierColor(announcement.topTier()));
        float coreRadius = Mth.clamp((float)Math.max(Math.max(72.0f, AnnouncementOverlay.widestLine(font, lines) * 0.62f), blockHeight * 0.85f), (float)72.0f, (float)((float)Math.min(width, height) * 0.44f));
        float voice = AnnouncementManager.amplitude();
        float build = Mth.clamp((float)(time / 900.0f), (float)0.0f, (float)1.0f);
        AnnouncementOverlay.drawVeil(graphics, width, height, global, ceremony == null ? 1.0f : CeremonyFx.darkenScale(ceremony));
        graphics.flush();
        LightFx.beginAdditive();
        Announcement.Evolution evolution = announcement.evolution();
        float quake = 0.0f;
        if (evolution != null && ceremony == null) {
            quake = EvolutionFx.render(graphics, width, height, time, announcement.answerAtMs(), AnnouncementOverlay.climaxAtMs(announcement), global, voice);
        } else {
            if (ceremony != null) {
                CeremonyFx.draw(graphics, ceremony, width, height, time, global);
            }
            SageCore.render(graphics, centerX, centerY, coreRadius, time, build, global, accent, voice);
        }
        AnnouncementOverlay.drawBloom(graphics, font, lines, offsets, centerX, topY, time, global);
        LightFx.endAdditive(graphics);
        boolean solo = announcement.skillCount() <= 1;
        PoseStack shake = graphics.pose();
        shake.pushPose();
        if (Math.abs(quake) > 0.05f) {
            shake.translate(quake, quake * 0.55f, 0.0f);
        }
        AnnouncementOverlay.applyShake(shake, lines, time);
        for (int i = 0; i < lines.size(); ++i) {
            Announcement.Line line = (Announcement.Line)lines.get(i);
            AnnouncementOverlay.drawLine(graphics, font, line, centerX, topY + offsets[i] + (float)AnnouncementOverlay.heightOf(line) / 2.0f, time, global, accent, solo);
        }
        shake.popPose();
        Subtitles.render(graphics, announcement, time, global);
        graphics.flush();
        LightFx.beginAdditive();
        AnnouncementOverlay.drawShockwaves(graphics, lines, centerX, centerY, time, global, accent);
        AnnouncementOverlay.drawTypeSparks(graphics, font, lines, offsets, centerX, topY, time, global);
        LightFx.endAdditive(graphics);
        RenderSystem.disableBlend();
    }

    private static void drawVeil(GuiGraphics graphics, int width, int height, float global, float scale) {
        float strength = Math.min(1.0f, ((Double)GreatSageConfig.DARKEN_STRENGTH.get()).floatValue() * scale);
        int base = AnnouncementOverlay.alpha(strength * global * 0.86f);
        if (base > 0) {
            graphics.fill(0, 0, width, height, base << 24 | 0x3110C);
        }
        int bands = 7;
        int stepX = Math.max(1, width / (bands * 2));
        int stepY = Math.max(1, height / (bands * 2));
        int band = AnnouncementOverlay.alpha(strength * global * 0.05f);
        if (band <= 0) {
            return;
        }
        for (int i = 0; i < bands; ++i) {
            int l = i * stepX;
            int t = i * stepY;
            int color = band << 24 | 0x3110C;
            graphics.fill(l, t, l + stepX, height - t, color);
            graphics.fill(width - l - stepX, t, width - l, height - t, color);
            graphics.fill(l + stepX, t, width - l - stepX, t + stepY, color);
            graphics.fill(l + stepX, height - t - stepY, width - l - stepX, height - t, color);
        }
    }

    private static void drawBloom(GuiGraphics graphics, Font font, List<Announcement.Line> lines, float[] offsets, float centerX, float topY, float time, float global) {
        for (int i = 0; i < lines.size(); ++i) {
            Announcement.Line line = lines.get(i);
            float progress = AnnouncementOverlay.typeProgress(line, time);
            if (progress <= 0.0f) continue;
            float y = topY + offsets[i] + (float)AnnouncementOverlay.heightOf(line) / 2.0f;
            float scale = line.type() == Announcement.LineType.CEREMONY ? 1.9f : 1.0f;
            LightFx.glow(graphics, centerX, y, (float)font.width((FormattedText)line.text()) * scale + 80.0f, (float)AnnouncementOverlay.heightOf(line) + 18.0f, 14679276, global * progress * 0.2f);
        }
    }

    private static void drawLine(GuiGraphics graphics, Font font, Announcement.Line line, float centerX, float centerY, float time, float global, int accent, boolean solo) {
        boolean newRace;
        float spacing;
        String text;
        float progress = AnnouncementOverlay.typeProgress(line, time);
        if (progress <= 0.0f) {
            return;
        }
        boolean ceremony = line.type() == Announcement.LineType.CEREMONY;
        boolean tier = line.type() == Announcement.LineType.TIER;
        String string = text = ceremony || tier ? line.text().getString().toUpperCase(Locale.ROOT) : line.text().getString();
        int color = tier ? AnnouncementOverlay.tierColor(line.tier()) : (ceremony ? accent : 14679276);
        float f = spacing = tier || ceremony ? 1.6f : 0.0f;
        if (tier) {
            AnnouncementOverlay.drawTyped(graphics, font, line, text, color, 1.0f, spacing, centerX, centerY, time, global);
            return;
        }
        if (line.type() == Announcement.LineType.RACE_FROM) {
            float fade = 1.0f - Mth.clamp((float)((time - (float)line.revealAtMs() - 900.0f) / 700.0f), (float)0.0f, (float)1.0f);
            int a2 = AnnouncementOverlay.alpha(progress * global * fade * 0.75f);
            if (a2 >= 8) {
                float w = (float)font.width(text) / 2.0f;
                int n = Math.round(centerX - w);
                Objects.requireNonNull(font);
                graphics.drawString(font, text, n, Math.round(centerY - 9.0f / 2.0f), a2 << 24 | 0x9FB3A8, true);
                graphics.fill(Math.round(centerX - w), Math.round(centerY), Math.round(centerX + w), Math.round(centerY) + 1, a2 << 24 | 0xFFE9A8);
            }
            return;
        }
        boolean bl = newRace = line.type() == Announcement.LineType.RACE_TO || line.type() == Announcement.LineType.ANALYSED;
        float base = newRace ? 2.6f : (ceremony ? 1.9f : 1.0f);
        float from = solo || newRace ? 4.0f : 2.3f;
        float eased = AnnouncementOverlay.easeOutExpo(progress);
        float scale = base * (from + (1.0f - from) * eased);
        float since = time - (float)line.revealAtMs();
        float squash = since >= 0.0f ? (float)Math.exp((double)(-since) / 105.0) * 0.13f : 0.0f;
        int a = AnnouncementOverlay.alpha(Mth.clamp((float)(progress * 1.6f), (float)0.0f, (float)1.0f) * global);
        if (a < 8) {
            return;
        }
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX, centerY, 0.0f);
        pose.scale(scale * (1.0f + squash), scale * (1.0f - squash), 1.0f);
        float half = (float)font.width(text) / 2.0f;
        Objects.requireNonNull(font);
        float y = (float)(-9) / 2.0f;
        float split = (1.0f - eased) * 6.0f;
        if (split >= 0.5f) {
            int ghost = AnnouncementOverlay.alpha(progress * global * 0.5f);
            graphics.drawString(font, text, Math.round(-half - split), Math.round(y), ghost << 24 | 0x66E0FF, false);
            graphics.drawString(font, text, Math.round(-half + split), Math.round(y), ghost << 24 | 0xFF5E7A, false);
        }
        graphics.drawString(font, text, Math.round(-half), Math.round(y), a << 24 | color, true);
        pose.popPose();
    }

    private static void drawTyped(GuiGraphics graphics, Font font, Announcement.Line line, String text, int color, float scale, float spacing, float centerX, float centerY, float time, float global) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX, centerY, 0.0f);
        pose.scale(scale, scale, 1.0f);
        float cursor = -AnnouncementOverlay.spacedWidth(font, text, spacing) / 2.0f;
        Objects.requireNonNull(font);
        float baseY = (float)(-9) / 2.0f;
        for (int i = 0; i < text.length(); ++i) {
            float in;
            int a;
            String glyph = String.valueOf(text.charAt(i));
            float since = time - AnnouncementOverlay.charTime(line, i, text.length());
            if (since >= 0.0f && (a = AnnouncementOverlay.alpha((in = Mth.clamp((float)(since / 130.0f), (float)0.0f, (float)1.0f)) * global)) >= 8) {
                pose.pushPose();
                pose.translate(cursor, baseY + (1.0f - in) * -2.5f, 0.0f);
                graphics.drawString(font, glyph, 0, 0, a << 24 | color, true);
                pose.popPose();
            }
            cursor += (float)font.width(glyph) + spacing;
        }
        pose.popPose();
    }

    private static void applyShake(PoseStack pose, List<Announcement.Line> lines, float time) {
        float latest = -1.0f;
        for (Announcement.Line line : lines) {
            if (line.type() == Announcement.LineType.TIER || !((float)line.revealAtMs() <= time) || !((float)line.revealAtMs() > latest)) continue;
            latest = line.revealAtMs();
        }
        if (latest < 0.0f) {
            return;
        }
        float since = time - latest;
        float amplitude = 4.2f * (float)Math.exp((double)(-since) / 90.0);
        if (amplitude < 0.05f) {
            return;
        }
        pose.translate(amplitude * Mth.sin((float)(since / 15.0f)), amplitude * 0.55f * Mth.cos((float)(since / 12.0f)), 0.0f);
    }

    private static void drawShockwaves(GuiGraphics graphics, List<Announcement.Line> lines, float centerX, float centerY, float time, float global, int accent) {
        for (Announcement.Line line : lines) {
            float since;
            if (line.type() == Announcement.LineType.TIER || (since = time - (float)line.revealAtMs()) < 0.0f || since > 520.0f) continue;
            float p = since / 520.0f;
            float radius = 30.0f + AnnouncementOverlay.easeOutExpo(p) * 240.0f;
            LightFx.ring(graphics, centerX, centerY, radius, radius * 0.55f, accent, global * (1.0f - p) * 0.45f);
            LightFx.beam(graphics, centerX, centerY, radius * 3.2f, 26.0f * (1.0f - p), 0.0f, 0xFFFFFF, global * (1.0f - p) * 0.3f);
        }
    }

    private static void drawTypeSparks(GuiGraphics graphics, Font font, List<Announcement.Line> lines, float[] offsets, float centerX, float topY, float time, float global) {
        for (int index = 0; index < lines.size(); ++index) {
            Announcement.Line line = lines.get(index);
            if (line.type() != Announcement.LineType.TIER || AnnouncementOverlay.typeProgress(line, time) <= 0.0f) continue;
            boolean ceremony = line.type() == Announcement.LineType.CEREMONY;
            boolean tier = line.type() == Announcement.LineType.TIER;
            String text = ceremony || tier ? line.text().getString().toUpperCase(Locale.ROOT) : line.text().getString();
            float scale = ceremony ? 1.9f : 1.0f;
            float spacing = tier || ceremony ? 1.6f : 0.0f;
            float y = topY + offsets[index] + (float)AnnouncementOverlay.heightOf(line) / 2.0f;
            float cursor = centerX - AnnouncementOverlay.spacedWidth(font, text, spacing) * scale / 2.0f;
            for (int i = 0; i < text.length(); ++i) {
                float slot = (float)font.width(String.valueOf(text.charAt(i))) * scale;
                float since = time - AnnouncementOverlay.charTime(line, i, text.length());
                if (since >= 0.0f && since < 200.0f) {
                    float decay = 1.0f - since / 200.0f;
                    LightFx.glow(graphics, cursor + slot / 2.0f, y, 6.0f + decay * 13.0f, 0xFFFFFF, global * decay * 0.6f);
                }
                cursor += slot + spacing * scale;
            }
        }
    }

    private static float typeProgress(Announcement.Line line, float time) {
        return Mth.clamp((float)((time - ((float)line.revealAtMs() - 420.0f)) / 420.0f), (float)0.0f, (float)1.0f);
    }

    private static float charTime(Announcement.Line line, int index, int length) {
        float share = length <= 1 ? 1.0f : ((float)index + 1.0f) / (float)length;
        return (float)line.revealAtMs() - 420.0f + 420.0f * share;
    }

    private static int[] paginate(List<Announcement.Line> lines, int screenHeight) {
        float budget = Math.max(60.0f, (float)screenHeight * 0.52f);
        int[] pages = new int[lines.size()];
        int page = 0;
        float used = 0.0f;
        for (int i = 0; i < lines.size(); ++i) {
            boolean orphanHeader;
            float h = AnnouncementOverlay.heightOf(lines.get(i)) + (i > 0 && lines.get(i).type() != Announcement.LineType.SKILL ? 9 : 0);
            boolean bl = orphanHeader = lines.get(i).type() != Announcement.LineType.SKILL && i + 1 < lines.size() && used + h + (float)AnnouncementOverlay.heightOf(lines.get(i + 1)) > budget;
            if (used > 0.0f && (used + h > budget || orphanHeader)) {
                used = 0.0f;
            }
            pages[i] = ++page;
            used += h;
        }
        return pages;
    }

    private static int currentPage(List<Announcement.Line> lines, int[] pages, float time) {
        int page = 0;
        for (int i = 0; i < lines.size(); ++i) {
            if (!(AnnouncementOverlay.typeProgress(lines.get(i), time) > 0.0f)) continue;
            page = pages[i];
        }
        return page;
    }

    private static float[] layout(List<Announcement.Line> lines) {
        float[] offsets = new float[lines.size() + 1];
        float cursor = 0.0f;
        for (int i = 0; i < lines.size(); ++i) {
            Announcement.Line line = lines.get(i);
            if (i > 0 && line.type() != Announcement.LineType.SKILL) {
                cursor += 9.0f;
            }
            offsets[i] = cursor;
            cursor += (float)AnnouncementOverlay.heightOf(line);
        }
        offsets[lines.size()] = cursor;
        return offsets;
    }

    private static int heightOf(Announcement.Line line) {
        return switch (line.type()) {
            default -> throw new MatchException(null, null);
            case Announcement.LineType.CEREMONY -> 32;
            case Announcement.LineType.RACE_FROM -> 20;
            case Announcement.LineType.RACE_TO, Announcement.LineType.ANALYSED -> 44;
            case Announcement.LineType.TIER -> 14;
            case Announcement.LineType.SKILL -> 16;
        };
    }

    private static float climaxAtMs(Announcement announcement) {
        for (Announcement.Line line : announcement.lines()) {
            if (line.type() != Announcement.LineType.RACE_TO) continue;
            return line.revealAtMs();
        }
        return (float)announcement.answerAtMs() + 3600.0f;
    }

    private static float widestLine(Font font, List<Announcement.Line> visible) {
        float widest = 0.0f;
        for (Announcement.Line line : visible) {
            float width = font.width((FormattedText)line.text());
            if (line.type() == Announcement.LineType.CEREMONY) {
                width *= 1.9f;
            } else if (line.type() == Announcement.LineType.RACE_TO || line.type() == Announcement.LineType.ANALYSED) {
                width *= 2.6f;
            }
            widest = Math.max(widest, width);
        }
        return widest;
    }

    private static float spacedWidth(Font font, String text, float spacing) {
        if (text.isEmpty()) {
            return 0.0f;
        }
        float width = -spacing;
        for (int i = 0; i < text.length(); ++i) {
            width += (float)font.width(String.valueOf(text.charAt(i))) + spacing;
        }
        return width;
    }

    private static float easeOutExpo(float t) {
        return t >= 1.0f ? 1.0f : 1.0f - (float)Math.pow(2.0, -10.0 * (double)t);
    }

    private static int alpha(float value) {
        return Mth.clamp((int)Math.round(value * 255.0f), (int)0, (int)255);
    }

    static int tierColor(String tier) {
        return switch (tier == null ? "" : tier) {
            case "ultimate" -> 16738890;
            case "unique" -> 16757575;
            case "extra" -> 16770442;
            case "intrinsic" -> 10219688;
            case "common" -> 14147304;
            case "resistance" -> 9427967;
            default -> 14679276;
        };
    }
}

