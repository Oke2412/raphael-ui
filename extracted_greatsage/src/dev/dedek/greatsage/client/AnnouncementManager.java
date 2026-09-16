/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.Util
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.resources.sounds.SimpleSoundInstance
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.util.Mth
 */
package dev.dedek.greatsage.client;

import dev.dedek.greatsage.CeremonyType;
import dev.dedek.greatsage.GreatSageConfig;
import dev.dedek.greatsage.GreatSageSounds;
import dev.dedek.greatsage.SoundManifest;
import dev.dedek.greatsage.client.Announcement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;

public final class AnnouncementManager {
    private static final Deque<Announcement> QUEUE = new ArrayDeque<Announcement>();
    private static final List<Announcement.Entry> PENDING = new ArrayList<Announcement.Entry>();
    private static long pendingDeadlineMs;
    @Nullable
    private static Announcement current;
    private static double elapsedMs;
    private static long lastUpdateMs;
    private static int nextStep;
    @Nullable
    private static CeremonyType openCeremony;
    @Nullable
    private static Announcement.Evolution openEvolution;
    @Nullable
    private static Announcement.Analysis openAnalysis;
    private static boolean openSkills;
    private static final List<Announcement.Entry> CEREMONY_SKILLS;
    private static long ceremonyDeadlineMs;

    private AnnouncementManager() {
    }

    public static void startCeremony(CeremonyType type) {
        Announcement merged;
        if (type.listsSkills() && openEvolution != null && current != null && current.evolution() != null && current.ceremony() == null && AnnouncementManager.prefixMatches(merged = Announcement.build(type, openEvolution, List.copyOf(CEREMONY_SKILLS), false))) {
            current = merged;
            openCeremony = type;
            ceremonyDeadlineMs = Util.getMillis() + AnnouncementManager.hold();
            return;
        }
        List<Announcement.Entry> seed = type.listsSkills() ? List.copyOf(PENDING) : List.of();
        PENDING.clear();
        Announcement announcement = Announcement.build(type, seed, !type.listsSkills());
        if (current != null && (current.ceremony() != null || current.evolution() != null)) {
            QUEUE.addLast(announcement);
            return;
        }
        current = announcement;
        elapsedMs = 0.0;
        nextStep = 0;
        CEREMONY_SKILLS.clear();
        openEvolution = null;
        if (type.listsSkills()) {
            CEREMONY_SKILLS.addAll(seed);
            openCeremony = type;
            ceremonyDeadlineMs = Util.getMillis() + AnnouncementManager.hold();
        } else {
            openCeremony = null;
        }
    }

    public static void startEvolution(Announcement.Evolution evolution) {
        Announcement merged;
        if (openCeremony != null && current != null && current.ceremony() != null && AnnouncementManager.prefixMatches(merged = Announcement.build(openCeremony, evolution, List.copyOf(CEREMONY_SKILLS), false))) {
            current = merged;
            openEvolution = evolution;
            ceremonyDeadlineMs = Util.getMillis() + AnnouncementManager.hold();
            return;
        }
        List<Announcement.Entry> seed = List.copyOf(PENDING);
        PENDING.clear();
        Announcement announcement = Announcement.evolution(evolution, seed, false);
        if (current != null && (current.ceremony() != null || current.evolution() != null)) {
            QUEUE.addLast(announcement);
            return;
        }
        current = announcement;
        elapsedMs = 0.0;
        nextStep = 0;
        CEREMONY_SKILLS.clear();
        CEREMONY_SKILLS.addAll(seed);
        openCeremony = null;
        openEvolution = evolution;
        ceremonyDeadlineMs = Util.getMillis() + AnnouncementManager.hold();
    }

    public static void startAnalysis(Announcement.Analysis analysis) {
        List<Announcement.Entry> seed = List.copyOf(PENDING);
        PENDING.clear();
        Announcement announcement = Announcement.analysis(analysis, seed, false);
        if (current != null) {
            QUEUE.addLast(announcement);
            return;
        }
        current = announcement;
        elapsedMs = 0.0;
        nextStep = 0;
        CEREMONY_SKILLS.clear();
        CEREMONY_SKILLS.addAll(seed);
        openCeremony = null;
        openEvolution = null;
        openAnalysis = analysis;
        ceremonyDeadlineMs = Util.getMillis() + AnnouncementManager.hold();
    }

    public static void submit(Announcement.Entry entry) {
        if (AnnouncementManager.somethingOpen() && AnnouncementManager.absorb(entry)) {
            return;
        }
        for (Announcement.Entry existing : PENDING) {
            if (!existing.skillId().equals((Object)entry.skillId())) continue;
            return;
        }
        PENDING.add(entry);
        pendingDeadlineMs = Util.getMillis() + (long)((Integer)GreatSageConfig.BATCH_WINDOW_TICKS.get()).intValue() * 50L;
    }

    private static boolean somethingOpen() {
        return openCeremony != null || openEvolution != null || openAnalysis != null || openSkills;
    }

    private static boolean absorb(Announcement.Entry entry) {
        if (current == null || openAnalysis == null && openEvolution == null && !openSkills && current.ceremony() != openCeremony || openEvolution != null && current.evolution() == null || openAnalysis != null && current.analysis() == null || openSkills && (current.ceremony() != null || current.evolution() != null || current.analysis() != null)) {
            return false;
        }
        for (Announcement.Entry existing : CEREMONY_SKILLS) {
            if (!existing.skillId().equals((Object)entry.skillId())) continue;
            return true;
        }
        ArrayList<Announcement.Entry> candidate = new ArrayList<Announcement.Entry>(CEREMONY_SKILLS);
        candidate.add(entry);
        Announcement rebuilt = Announcement.build(openCeremony, openEvolution, openAnalysis, candidate, false);
        if (!AnnouncementManager.prefixMatches(rebuilt)) {
            return false;
        }
        CEREMONY_SKILLS.clear();
        CEREMONY_SKILLS.addAll(candidate);
        current = rebuilt;
        ceremonyDeadlineMs = Util.getMillis() + AnnouncementManager.hold();
        return true;
    }

    private static boolean prefixMatches(Announcement rebuilt) {
        if (current == null || rebuilt.steps().size() < nextStep) {
            return false;
        }
        for (int i = 0; i < nextStep; ++i) {
            if (current.steps().get(i).equals(rebuilt.steps().get(i))) continue;
            return false;
        }
        return true;
    }

    private static void closeCeremony() {
        if (!AnnouncementManager.somethingOpen()) {
            return;
        }
        Announcement closed = Announcement.build(openCeremony, openEvolution, openAnalysis, List.copyOf(CEREMONY_SKILLS), true);
        if (AnnouncementManager.prefixMatches(closed)) {
            current = closed;
        }
        openCeremony = null;
        openEvolution = null;
        openAnalysis = null;
        openSkills = false;
    }

    private static long hold() {
        return (long)((Integer)GreatSageConfig.CEREMONY_HOLD_TICKS.get()).intValue() * 50L;
    }

    public static void clear() {
        QUEUE.clear();
        PENDING.clear();
        CEREMONY_SKILLS.clear();
        openCeremony = null;
        openEvolution = null;
        openAnalysis = null;
        openSkills = false;
        current = null;
        elapsedMs = 0.0;
        nextStep = 0;
    }

    public static void update() {
        Minecraft minecraft = Minecraft.getInstance();
        long now = Util.getMillis();
        double delta = lastUpdateMs == 0L ? 0.0 : (double)(now - lastUpdateMs);
        lastUpdateMs = now;
        if (minecraft.isPaused() || minecraft.player == null) {
            return;
        }
        if (AnnouncementManager.somethingOpen() && now >= ceremonyDeadlineMs) {
            AnnouncementManager.closeCeremony();
        }
        if (!PENDING.isEmpty() && now >= pendingDeadlineMs) {
            if (QUEUE.size() < (Integer)GreatSageConfig.MAX_QUEUED.get()) {
                QUEUE.addLast(Announcement.build(null, null, null, List.copyOf(PENDING), false));
            }
            PENDING.clear();
        }
        if (current == null) {
            if (QUEUE.isEmpty()) {
                return;
            }
            current = QUEUE.pollFirst();
            elapsedMs = 0.0;
            nextStep = 0;
            delta = 0.0;
            if (!current.closed() && current.ceremony() == null && current.evolution() == null && current.analysis() == null) {
                CEREMONY_SKILLS.clear();
                CEREMONY_SKILLS.addAll(current.entries());
                openSkills = true;
                ceremonyDeadlineMs = now + AnnouncementManager.hold();
            }
        }
        elapsedMs += delta;
        while (nextStep < current.steps().size() && (double)current.steps().get(nextStep).atMs() <= elapsedMs) {
            AnnouncementManager.play(current.steps().get(nextStep).soundKey());
            ++nextStep;
        }
        if (elapsedMs >= (double)current.durationMs()) {
            if (AnnouncementManager.somethingOpen()) {
                AnnouncementManager.closeCeremony();
            }
            current = null;
            elapsedMs = 0.0;
            nextStep = 0;
        }
    }

    private static void play(String key) {
        SoundEvent sound = GreatSageSounds.get(key);
        if (sound == null) {
            return;
        }
        float volume = ((Double)GreatSageConfig.VOLUME.get()).floatValue();
        if (volume <= 0.0f) {
            return;
        }
        Minecraft.getInstance().getSoundManager().play((SoundInstance)SimpleSoundInstance.forUI((SoundEvent)sound, (float)1.0f, (float)volume));
    }

    public static float amplitude() {
        Announcement announcement = current;
        if (announcement == null) {
            return 0.0f;
        }
        float loudest = 0.0f;
        for (Announcement.Step step : announcement.steps()) {
            if ((double)step.atMs() > elapsedMs) break;
            double into = elapsedMs - (double)step.atMs();
            if (into >= (double)SoundManifest.durationMs(step.soundKey())) continue;
            float position = (float)(into / 50.0);
            int tick = (int)position;
            float blend = position - (float)tick;
            float value = Mth.lerp((float)blend, (float)SoundManifest.amplitudeAt(step.soundKey(), tick), (float)SoundManifest.amplitudeAt(step.soundKey(), tick + 1));
            loudest = Math.max(loudest, value);
        }
        return loudest;
    }

    @Nullable
    public static Announcement current() {
        return current;
    }

    public static double elapsedMs() {
        return elapsedMs;
    }

    static {
        CEREMONY_SKILLS = new ArrayList<Announcement.Entry>();
    }
}

