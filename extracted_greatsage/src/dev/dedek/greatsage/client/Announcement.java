/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 */
package dev.dedek.greatsage.client;

import dev.dedek.greatsage.CeremonyType;
import dev.dedek.greatsage.GreatSageConfig;
import dev.dedek.greatsage.SoundManifest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class Announcement {
    private static final int GAP_MS = 30;
    private static final int SILENT_NAME_MS = 620;
    private static final int SILENT_TIER_MS = 520;
    private static final int SILENT_CEREMONY_MS = 1600;
    private static final int TAIL_MS = 550;
    private static final int MIN_DURATION_MS = 1700;
    private static final int WORLD_ANSWER_WAIT_MS = 2600;
    @Nullable
    private final CeremonyType ceremony;
    @Nullable
    private final Evolution evolution;
    @Nullable
    private final Analysis analysis;
    private final int answerAtMs;
    private final List<Entry> entries;
    private final List<Line> lines;
    private final List<Step> steps;
    private final int durationMs;
    private final int skillCount;
    private final String topTier;
    private final boolean closed;

    private Announcement(@Nullable CeremonyType ceremony, @Nullable Evolution evolution, @Nullable Analysis analysis, List<Entry> entries, int answerAtMs, List<Line> lines, List<Step> steps, int durationMs, int skillCount, String topTier, boolean closed) {
        this.entries = entries;
        this.ceremony = ceremony;
        this.evolution = evolution;
        this.analysis = analysis;
        this.answerAtMs = answerAtMs;
        this.lines = lines;
        this.steps = steps;
        this.durationMs = durationMs;
        this.skillCount = skillCount;
        this.topTier = topTier;
        this.closed = closed;
    }

    public static Announcement skills(List<Entry> entries) {
        return Announcement.build(null, entries, true);
    }

    public static Announcement evolution(Evolution evo, List<Entry> entries, boolean closed) {
        return Announcement.build(null, evo, null, entries, closed);
    }

    public static Announcement analysis(Analysis analysis, List<Entry> entries, boolean closed) {
        return Announcement.build(null, null, analysis, entries, closed);
    }

    public static Announcement build(@Nullable CeremonyType ceremony, List<Entry> entries, boolean closed) {
        return Announcement.build(ceremony, null, null, entries, closed);
    }

    public static Announcement build(@Nullable CeremonyType ceremony, @Nullable Evolution evolution, List<Entry> entries, boolean closed) {
        return Announcement.build(ceremony, evolution, null, entries, closed);
    }

    public static Announcement build(@Nullable CeremonyType ceremony, @Nullable Evolution evolution, @Nullable Analysis analysis, List<Entry> entries, boolean closed) {
        Map<String, List<Entry>> groups = Announcement.group(entries);
        boolean voiced = (Boolean)GreatSageConfig.ENABLE_VOICE.get() != false && (ceremony != null || analysis != null || (Boolean)GreatSageConfig.VOICE_WITHOUT_NAME_CLIP.get() != false || entries.stream().anyMatch(entry -> SoundManifest.has(Announcement.nameKey(entry))));
        ArrayList<Line> lines = new ArrayList<Line>();
        ArrayList<Step> steps = new ArrayList<Step>();
        int cursor = 0;
        if (((Boolean)GreatSageConfig.ENABLE_SFX.get()).booleanValue()) {
            cursor = Announcement.emit(steps, "sfx.open", cursor, 0, true);
        }
        int answerAt = 0;
        if (ceremony != null) {
            lines.add(new Line(LineType.CEREMONY, (Component)Component.translatable((String)ceremony.translationKey()), "", cursor));
            cursor = Announcement.emit(steps, ceremony.soundKey(), cursor, 1600, voiced);
            if (evolution != null) {
                cursor = Announcement.emit(steps, "voice.tsuzukete", cursor, 0, voiced);
                lines.add(new Line(LineType.RACE_FROM, evolution.fromName(), "", cursor));
                cursor = Announcement.emit(steps, Announcement.raceKey(evolution.fromId()), cursor, 620, voiced);
                cursor = Announcement.emit(steps, "voice.yori", cursor, 0, voiced);
                lines.add(new Line(LineType.RACE_TO, evolution.toName(), "", cursor));
                cursor = Announcement.emit(steps, Announcement.raceKey(evolution.toId()), cursor, 620, voiced);
                cursor = Announcement.emit(steps, "voice.e_no_shinka", cursor, 0, voiced);
            }
        } else if (evolution != null) {
            if (voiced) {
                cursor = Announcement.emit(steps, "voice.koku", cursor, 0, true);
                cursor = Announcement.emit(steps, "voice.shinka_jouken", cursor, 1600, true);
            }
            answerAt = cursor += 2600;
            cursor = Announcement.emit(steps, "voice.shounin", cursor, 700, voiced);
            lines.add(new Line(LineType.RACE_FROM, evolution.fromName(), "", cursor));
            cursor = Announcement.emit(steps, Announcement.raceKey(evolution.fromId()), cursor, 620, voiced);
            cursor = Announcement.emit(steps, "voice.yori", cursor, 0, voiced);
            lines.add(new Line(LineType.RACE_TO, evolution.toName(), "", cursor));
            cursor = Announcement.emit(steps, Announcement.raceKey(evolution.toId()), cursor, 620, voiced);
            cursor = Announcement.emit(steps, "voice.e_no_shinka", cursor, 0, voiced);
        } else if (analysis != null) {
            if (voiced) {
                cursor = Announcement.emit(steps, "voice.koku", cursor, 0, true);
            }
            lines.add(new Line(LineType.ANALYSED, analysis.skillName(), "", cursor));
            cursor = Announcement.emit(steps, Announcement.nameKey(analysis.skillId()), cursor, 620, voiced);
            cursor = Announcement.emit(steps, "voice.kaiseki_kanryou", cursor, 0, voiced);
        } else if (voiced) {
            cursor = Announcement.emit(steps, "voice.koku", cursor, 0, true);
        }
        boolean needsConnector = ceremony != null || evolution != null || analysis != null;
        for (Map.Entry<String, List<Entry>> group : groups.entrySet()) {
            String tier = group.getKey();
            if (needsConnector) {
                cursor = Announcement.emit(steps, "voice.tsuzukete", cursor, 0, voiced);
            }
            needsConnector = true;
            lines.add(new Line(LineType.TIER, (Component)Component.translatable((String)("greatsage.tier." + tier)), tier, cursor));
            cursor = Announcement.emit(steps, "tier." + tier, cursor, 520, voiced);
            for (Entry entry2 : group.getValue()) {
                lines.add(new Line(LineType.SKILL, entry2.displayName(), tier, cursor));
                cursor = Announcement.emit(steps, Announcement.nameKey(entry2), cursor, 620, voiced);
            }
        }
        if (closed && voiced && !entries.isEmpty()) {
            cursor = Announcement.emit(steps, "voice.no_kakutoku", cursor, 0, true);
            cursor = Announcement.emit(steps, "voice.seiko_shimashita", cursor, 0, true);
        }
        Announcement.addSoundEffects(lines, steps, ceremony);
        int duration = cursor + 550 + (closed ? 0 : (Integer)GreatSageConfig.CEREMONY_HOLD_TICKS.get() * 50);
        String topTier = groups.isEmpty() ? "" : groups.keySet().iterator().next();
        return new Announcement(ceremony, evolution, analysis, List.copyOf(entries), answerAt, List.copyOf(lines), List.copyOf(steps), Math.max(1700, duration), entries.size(), topTier, closed);
    }

    private static void addSoundEffects(List<Line> lines, List<Step> steps, @Nullable CeremonyType ceremony) {
        if (!((Boolean)GreatSageConfig.ENABLE_SFX.get()).booleanValue() || !SoundManifest.has("sfx.skill")) {
            return;
        }
        for (Line line : lines) {
            if (line.type() != LineType.SKILL && line.type() != LineType.ANALYSED) continue;
            steps.add(new Step("sfx.skill", line.revealAtMs()));
        }
        steps.sort(Comparator.comparingInt(Step::atMs));
    }

    private static Map<String, List<Entry>> group(List<Entry> entries) {
        LinkedHashMap<String, List> byTier = new LinkedHashMap<String, List>();
        for (Entry entry : entries) {
            byTier.computeIfAbsent(Announcement.tierOf(entry), key -> new ArrayList()).add(entry);
        }
        LinkedHashMap<String, List<Entry>> ordered = new LinkedHashMap<String, List<Entry>>();
        for (String tier : (List)GreatSageConfig.TIER_ORDER.get()) {
            String key2 = tier.trim().toLowerCase(Locale.ROOT);
            List group = (List)byTier.remove(key2);
            if (group == null) continue;
            ordered.put(key2, group);
        }
        ordered.putAll(byTier);
        return ordered;
    }

    private static String tierOf(Entry entry) {
        String tier = entry.tier();
        return tier == null || tier.isBlank() ? "unknown" : tier.toLowerCase(Locale.ROOT);
    }

    private static String raceKey(ResourceLocation id) {
        String qualified = "race." + id.getNamespace() + "." + id.getPath();
        return SoundManifest.has(qualified) ? qualified : "race." + id.getPath();
    }

    private static String nameKey(ResourceLocation id) {
        String qualified = "skill." + id.getNamespace() + "." + id.getPath();
        return SoundManifest.has(qualified) ? qualified : "skill." + id.getPath();
    }

    private static String nameKey(Entry entry) {
        String qualified = "skill." + entry.skillId().getNamespace() + "." + entry.skillId().getPath();
        return SoundManifest.has(qualified) ? qualified : "skill." + entry.skillId().getPath();
    }

    private static int emit(List<Step> steps, String key, int cursor, int silentFallbackMs, boolean voiced) {
        int length;
        int n = length = voiced ? SoundManifest.durationMs(key) : 0;
        if (length <= 0) {
            return cursor + silentFallbackMs;
        }
        steps.add(new Step(key, cursor));
        return cursor + length + 30;
    }

    @Nullable
    public CeremonyType ceremony() {
        return this.ceremony;
    }

    @Nullable
    public Evolution evolution() {
        return this.evolution;
    }

    @Nullable
    public Analysis analysis() {
        return this.analysis;
    }

    public int answerAtMs() {
        return this.answerAtMs;
    }

    public List<Entry> entries() {
        return this.entries;
    }

    public List<Line> lines() {
        return this.lines;
    }

    public List<Step> steps() {
        return this.steps;
    }

    public int durationMs() {
        return this.durationMs;
    }

    public int skillCount() {
        return this.skillCount;
    }

    public String topTier() {
        return this.topTier;
    }

    public boolean closed() {
        return this.closed;
    }

    public record Evolution(ResourceLocation fromId, Component fromName, ResourceLocation toId, Component toName) {
    }

    public record Analysis(ResourceLocation skillId, Component skillName) {
    }

    public record Line(LineType type, Component text, String tier, int revealAtMs) {
    }

    public static enum LineType {
        CEREMONY,
        RACE_FROM,
        RACE_TO,
        ANALYSED,
        TIER,
        SKILL;

    }

    public record Entry(ResourceLocation skillId, String tier, Component displayName) {
    }

    public record Step(String soundKey, int atMs) {
    }
}

