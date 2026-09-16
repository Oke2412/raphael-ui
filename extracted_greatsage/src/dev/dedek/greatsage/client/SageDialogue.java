/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  javax.annotation.Nullable
 *  net.minecraft.Util
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.resources.language.I18n
 *  net.minecraft.client.resources.sounds.SimpleSoundInstance
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.util.FormattedCharSequence
 *  net.minecraft.util.Mth
 */
package dev.dedek.greatsage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dedek.greatsage.GreatSageConfig;
import dev.dedek.greatsage.GreatSageSounds;
import dev.dedek.greatsage.SoundManifest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public final class SageDialogue {
    private static final float FADE_MS = 220.0f;
    private static final float LINGER_MS = 1500.0f;
    private static final int WIDTH = 150;
    private static final int PADDING = 6;
    private static final float SCALE = 0.8f;
    private static List<String> queue = List.of();
    private static float[] starts = new float[0];
    private static int played;
    private static final List<SimpleSoundInstance> PLAYING;
    private static long startedAt;
    private static float totalMs;
    private static String textKey;
    private static String queued;
    private static final Map<String, String> MARKER;
    @Nullable
    private static Component override;

    private SageDialogue() {
    }

    static void greet() {
        String key = SageDialogue.resolve("voice.ohayo", "voice.kidou");
        if (key != null) {
            SageDialogue.speak(List.of(key), key);
        }
    }

    static void queue(String key) {
        if (!SageDialogue.active()) {
            SageDialogue.say(key);
            return;
        }
        queued = key;
    }

    static void say(String key) {
        override = null;
        ArrayList<String> parts = new ArrayList<String>();
        String marker = MARKER.get(key);
        if (marker != null && SoundManifest.has(marker)) {
            parts.add(marker);
        }
        parts.add(key);
        SageDialogue.speak(parts, key);
    }

    public static void reply(List<String> clips, Component subtitle) {
        override = subtitle;
        SageDialogue.speak(clips, clips.isEmpty() ? "voice.koku" : clips.get(clips.size() - 1));
    }

    private static void speak(List<String> parts, String key) {
        SageDialogue.silence();
        queue = List.copyOf(parts);
        starts = new float[queue.size()];
        float cursor = 0.0f;
        for (int i = 0; i < queue.size(); ++i) {
            SageDialogue.starts[i] = cursor;
            cursor += (float)Math.max(300, SoundManifest.durationMs(queue.get(i))) + 40.0f;
        }
        totalMs = Math.max(1200.0f, cursor);
        startedAt = Util.getMillis();
        played = 0;
        textKey = key;
        SageDialogue.update();
    }

    static void update() {
        if (played >= queue.size()) {
            if (!queued.isEmpty() && !SageDialogue.active()) {
                String next = queued;
                queued = "";
                SageDialogue.say(next);
            }
            return;
        }
        float since = Util.getMillis() - startedAt;
        while (played < queue.size() && since >= starts[played]) {
            SageDialogue.speakPart(queue.get(played));
            ++played;
        }
    }

    public static void clear() {
        SageDialogue.silence();
        queue = List.of();
        played = 0;
        textKey = "";
        queued = "";
        override = null;
    }

    public static void silence() {
        Minecraft minecraft = Minecraft.getInstance();
        for (SimpleSoundInstance instance : PLAYING) {
            minecraft.getSoundManager().stop((SoundInstance)instance);
        }
        PLAYING.clear();
        played = queue.size();
    }

    private static void speakPart(String key) {
        SoundEvent sound = GreatSageSounds.get(key);
        float volume = ((Double)GreatSageConfig.VOLUME.get()).floatValue();
        if (sound == null || volume <= 0.0f) {
            return;
        }
        SimpleSoundInstance instance = SimpleSoundInstance.forUI((SoundEvent)sound, (float)1.0f, (float)volume);
        PLAYING.add(instance);
        Minecraft.getInstance().getSoundManager().play((SoundInstance)instance);
    }

    public static void play(String key) {
        SoundEvent sound = GreatSageSounds.get(key);
        float volume = ((Double)GreatSageConfig.VOLUME.get()).floatValue();
        if (sound == null || volume <= 0.0f) {
            return;
        }
        Minecraft.getInstance().getSoundManager().play((SoundInstance)SimpleSoundInstance.forUI((SoundEvent)sound, (float)1.0f, (float)volume));
    }

    private static String resolve(String ... keys) {
        for (String key : keys) {
            if (!SoundManifest.has(key)) continue;
            return key;
        }
        return keys.length == 0 ? null : keys[0];
    }

    static boolean active() {
        return !textKey.isEmpty() && (float)(Util.getMillis() - startedAt) < totalMs + 1500.0f;
    }

    static void render(GuiGraphics graphics, float left, float top, float global) {
        String full;
        SageDialogue.update();
        if (!SageDialogue.active()) {
            return;
        }
        String key = "greatsage.voice." + textKey.replace("voice.", "");
        if (override == null && !I18n.exists((String)key)) {
            return;
        }
        float since = Util.getMillis() - startedAt;
        float fade = Math.min(Mth.clamp((float)(since / 220.0f), (float)0.0f, (float)1.0f), Mth.clamp((float)((totalMs + 1500.0f - since) / 440.0f), (float)0.0f, (float)1.0f));
        int a = Mth.clamp((int)Math.round(fade * global * 255.0f), (int)0, (int)255);
        if (a < 8) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        String string = full = override != null ? override.getString() : I18n.get((String)key, (Object[])new Object[0]);
        if (full.isEmpty()) {
            return;
        }
        int shown = Mth.clamp((int)Math.round((float)full.length() * (since / totalMs)), (int)1, (int)full.length());
        List lines = font.split((FormattedText)Component.literal((String)full.substring(0, shown)), 150);
        Objects.requireNonNull(font);
        int lineHeight = 9 + 2;
        int boxHeight = Math.max(lineHeight, lines.size() * lineHeight) + 12;
        int widest = 0;
        for (FormattedCharSequence line : lines) {
            widest = Math.max(widest, font.width(line));
        }
        int boxWidth = Math.max(70, widest) + 12;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(left, top, 0.0f);
        pose.scale(0.8f, 0.8f, 1.0f);
        graphics.fill(0, 0, boxWidth, boxHeight, Math.round((float)a * 0.72f) << 24 | 0x4140E);
        int edge = Math.round((float)a * 0.55f) << 24 | 0x8FE9C0;
        graphics.fill(0, 0, boxWidth, 1, edge);
        graphics.fill(0, boxHeight - 1, boxWidth, boxHeight, edge);
        graphics.fill(0, 0, 1, boxHeight, edge);
        graphics.fill(boxWidth - 1, 0, boxWidth, boxHeight, edge);
        for (int i = 0; i < 3; ++i) {
            graphics.fill(10 + i, -3 + i, 10 + i + (3 - i) * 2, -2 + i, edge);
        }
        int textAlpha = Math.round((float)a * 0.94f);
        for (int i = 0; i < lines.size(); ++i) {
            graphics.drawString(font, (FormattedCharSequence)lines.get(i), 6, 6 + i * lineHeight, textAlpha << 24 | 0xDFFCEC, false);
        }
        pose.popPose();
    }

    static {
        PLAYING = new ArrayList<SimpleSoundInstance>();
        textKey = "";
        queued = "";
        MARKER = Map.of("voice.kaiseki_chuudan", "voice.koku", "voice.mishutoku", "voice.hi", "voice.kaiseki_kantei", "voice.ryo", "voice.kaiseki_shuuryou", "voice.ryo", "voice.taishou_kakunin", "voice.koku", "voice.taishou_kanryou", "voice.koku", "voice.seizon_kakuritsu", "voice.koku", "voice.kaiseki_shippai", "voice.hi");
    }
}

