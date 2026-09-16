/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.Util
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.phys.EntityHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package dev.dedek.greatsage.client.voice;

import dev.dedek.greatsage.GreatSageConfig;
import dev.dedek.greatsage.client.AssessMode;
import dev.dedek.greatsage.client.BattleSet;
import dev.dedek.greatsage.client.SageArchive;
import dev.dedek.greatsage.client.SageDialogue;
import dev.dedek.greatsage.client.SageMenuScreen;
import dev.dedek.greatsage.client.Waypoints;
import dev.dedek.greatsage.client.voice.VoiceGrammar;
import dev.dedek.greatsage.client.voice.VoiceSpeech;
import dev.dedek.greatsage.network.VoiceQueryPayload;
import dev.dedek.greatsage.network.VoiceReplyPayload;
import dev.dedek.greatsage.voice.NumberSpeech;
import dev.dedek.greatsage.voice.VoiceCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VoiceInput {
    private static final Logger LOG = LoggerFactory.getLogger((String)"greatsage/voice");
    private static final long SETTLE_MS = 1200L;
    private static final long THINK_TIMEOUT_MS = 5000L;
    private static final long QUIET_TAIL_MS = 4000L;
    private static State state = State.ASLEEP;
    private static long activeAt;
    private static final StringBuilder HEARD;
    private static long settleAt;
    private static long windowEndsAt;
    private static long stateSince;
    private static List<String> lastReply;
    private static Component lastSubtitle;
    private static boolean keyHeld;
    private static boolean warned;

    private VoiceInput() {
    }

    public static State state() {
        return state;
    }

    public static long stateSince() {
        return stateSince;
    }

    public static float window() {
        if (state != State.LISTENING) {
            return 0.0f;
        }
        long left = windowEndsAt - Util.getMillis();
        float total = (float)((Integer)GreatSageConfig.LISTEN_WINDOW_TICKS.get()).intValue() * 50.0f;
        return total <= 0.0f ? 0.0f : Math.max(0.0f, Math.min(1.0f, (float)left / total));
    }

    private static String mode() {
        return ((String)GreatSageConfig.VOICE_INPUT.get()).toLowerCase(Locale.ROOT);
    }

    public static boolean enabled() {
        return !"off".equals(VoiceInput.mode());
    }

    public static boolean busy() {
        if (!VoiceInput.enabled()) {
            return false;
        }
        return state != State.ASLEEP || Util.getMillis() - activeAt < 4000L;
    }

    public static void tick() {
        String fragment;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !VoiceInput.enabled()) {
            if (VoiceSpeech.listening()) {
                VoiceInput.stop();
            }
            return;
        }
        long now = Util.getMillis();
        if (state != State.ASLEEP) {
            activeAt = now;
        }
        if ("wake".equals(VoiceInput.mode()) && !VoiceSpeech.listening()) {
            VoiceSpeech.open(VoiceGrammar.fullJson(player));
        }
        while ((fragment = VoiceSpeech.poll()) != null) {
            VoiceInput.consume(fragment, player, now);
        }
        if (state == State.LISTENING) {
            boolean settled;
            boolean speaking;
            boolean bl = speaking = !VoiceSpeech.partial().isBlank();
            if (speaking) {
                settleAt = now + 1200L;
                windowEndsAt = Math.max(windowEndsAt, now + 1200L + 500L);
            }
            boolean bl2 = settled = HEARD.length() > 0 && !speaking && now >= settleAt;
            if (settled || now > windowEndsAt) {
                if (HEARD.length() > 0) {
                    VoiceInput.submit(HEARD.toString(), player);
                } else {
                    VoiceInput.sleep();
                }
            }
        }
        if (state == State.THINKING && now - stateSince > 5000L) {
            SageDialogue.reply(List.of("voice.hi", "reply.machine_error"), (Component)Component.translatable((String)"greatsage.voice.machine_error"));
            VoiceInput.sleep();
        }
    }

    private static void consume(String fragment, LocalPlayer player, long now) {
        String text = fragment.replace("[unk]", " ").trim();
        if (text.isEmpty()) {
            return;
        }
        if (state == State.ASLEEP) {
            if (!"wake".equals(VoiceInput.mode()) || !VoiceGrammar.isWake(text)) {
                return;
            }
            VoiceInput.wakeUp();
            String rest = VoiceGrammar.stripWake(text);
            if (!rest.isEmpty()) {
                VoiceInput.append(rest, now);
            }
            return;
        }
        if (state == State.LISTENING) {
            VoiceInput.append(VoiceGrammar.stripWake(text), now);
        }
    }

    private static void append(String text, long now) {
        if (text.isBlank()) {
            return;
        }
        if (HEARD.length() > 0) {
            HEARD.append(' ');
        }
        HEARD.append(text);
        settleAt = now + 1200L;
    }

    private static void wakeUp() {
        HEARD.setLength(0);
        state = State.LISTENING;
        stateSince = Util.getMillis();
        settleAt = stateSince + 1200L;
        windowEndsAt = stateSince + (long)((Integer)GreatSageConfig.LISTEN_WINDOW_TICKS.get()).intValue() * 50L;
        if (((Boolean)GreatSageConfig.VOICE_FEEDBACK.get()).booleanValue()) {
            SageDialogue.play("sfx.open");
        }
    }

    private static void sleep() {
        state = State.ASLEEP;
        stateSince = Util.getMillis();
        HEARD.setLength(0);
        if (!"wake".equals(VoiceInput.mode())) {
            VoiceSpeech.close();
        }
    }

    public static void stop() {
        keyHeld = false;
        VoiceInput.sleep();
    }

    public static void onKeyDown() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!"pushtotalk".equals(VoiceInput.mode().replace("_", "")) || minecraft.player == null) {
            return;
        }
        if (keyHeld) {
            return;
        }
        keyHeld = true;
        if (!VoiceInput.announce(minecraft)) {
            return;
        }
        if (VoiceSpeech.open(VoiceGrammar.fullJson(minecraft.player))) {
            HEARD.setLength(0);
            state = State.LISTENING;
            stateSince = Util.getMillis();
            settleAt = Long.MAX_VALUE;
            windowEndsAt = Long.MAX_VALUE;
        }
    }

    public static void onKeyUp() {
        if (!keyHeld) {
            return;
        }
        keyHeld = false;
        if (state == State.LISTENING) {
            long now = Util.getMillis();
            settleAt = now + 1200L;
            windowEndsAt = now + 1200L + 800L;
        }
    }

    private static boolean announce(Minecraft minecraft) {
        if (VoiceSpeech.available()) {
            return true;
        }
        if (!warned && minecraft.player != null) {
            warned = true;
            minecraft.player.displayClientMessage((Component)Component.translatable((String)("greatsage.voice.unavailable." + VoiceSpeech.status().name().toLowerCase(Locale.ROOT)), (Object[])new Object[]{VoiceSpeech.describe()}), false);
        }
        return false;
    }

    public static void submit(String text, @Nullable LocalPlayer player) {
        HEARD.setLength(0);
        if (VoiceGrammar.stripWake(text).isBlank()) {
            VoiceInput.speak("reply.taiki", "voice.koku");
            VoiceInput.sleep();
            return;
        }
        VoiceGrammar.Match match = VoiceGrammar.parse(text, player);
        LOG.info("[greatsage] entendu \"{}\" -> {}", (Object)text, match == null ? "aucune correspondance" : match.command().id() + (String)(match.argument().isEmpty() ? "" : " (" + match.argument() + ")"));
        if (match == null) {
            SageDialogue.reply(List.of("voice.hi", "reply.ninshiki_fuka", "reply.saido_onegai"), (Component)Component.translatable((String)"greatsage.voice.ninshiki_fuka"));
            VoiceInput.sleep();
            return;
        }
        if (match.command().scope() == VoiceCommand.Scope.LOCAL) {
            VoiceInput.local(match, player);
            VoiceInput.sleep();
            return;
        }
        state = State.THINKING;
        stateSince = Util.getMillis();
        PacketDistributor.sendToServer((CustomPacketPayload)new VoiceQueryPayload(match.command().id(), VoiceInput.payload(match), VoiceInput.lookedAt()), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    private static String payload(VoiceGrammar.Match match) {
        String id = match.command().id();
        if ("battle_mode".equals(id) || "battle_mode_off".equals(id)) {
            return String.join((CharSequence)",", BattleSet.all());
        }
        return match.argument();
    }

    private static int lookedAt() {
        HitResult hit = Minecraft.getInstance().crosshairPickEntity == null ? Minecraft.getInstance().hitResult : null;
        Entity picked = Minecraft.getInstance().crosshairPickEntity;
        if (picked != null) {
            return picked.getId();
        }
        if (hit instanceof EntityHitResult) {
            EntityHitResult entityHit = (EntityHitResult)hit;
            return entityHit.getEntity().getId();
        }
        return -1;
    }

    private static void local(VoiceGrammar.Match match, @Nullable LocalPlayer player) {
        String id;
        Minecraft minecraft = Minecraft.getInstance();
        switch (id = match.command().id()) {
            case "wake": 
            case "present": 
            case "standby": {
                VoiceInput.speak("reply.taiki", "voice.koku");
                break;
            }
            case "dismiss_request": 
            case "done": 
            case "louder": 
            case "speak_less": 
            case "speak_more": {
                VoiceInput.speak("reply.ryoukai", "voice.ryo");
                break;
            }
            case "sleep": {
                VoiceInput.speak("reply.taiki_kaijo", "voice.ryo");
                break;
            }
            case "resume": {
                VoiceInput.speak("reply.hatsugen_saikai", "voice.ryo");
                break;
            }
            case "watch": {
                VoiceInput.speak("reply.kanshi_kaishi", "voice.ryo");
                break;
            }
            case "stop_watch": {
                VoiceInput.speak("reply.kanshi_shuuryou", "voice.ryo");
                break;
            }
            case "silence": {
                VoiceInput.speak("reply.chinmoku", "voice.ryo");
                SageDialogue.silence();
                break;
            }
            case "thanks": 
            case "praise": {
                VoiceInput.speak("reply.fuyou", "voice.hi");
                break;
            }
            case "identify": {
                VoiceInput.speak("reply.watashi_wa_daikenja", "voice.koku");
                break;
            }
            case "alive": {
                VoiceInput.speak("reply.watashi_wa_skill", "voice.koku");
                break;
            }
            case "functions": {
                VoiceInput.speak("reply.kinou_ichiran", "voice.koku");
                break;
            }
            case "understood": {
                VoiceInput.speak("reply.ze", "voice.ze");
                break;
            }
            case "open_menu": 
            case "open_analyzing": 
            case "open_records": 
            case "open_observations": {
                VoiceInput.speak("reply.ryoukai", "voice.ryo");
                if (minecraft.screen != null) break;
                minecraft.setScreen((Screen)new SageMenuScreen());
                break;
            }
            case "close_menu": {
                VoiceInput.speak("reply.ryoukai", "voice.ryo");
                Screen screen = minecraft.screen;
                if (!(screen instanceof SageMenuScreen)) break;
                SageMenuScreen screen2 = (SageMenuScreen)screen;
                screen2.onClose();
                break;
            }
            case "assess_open": {
                VoiceInput.speak("reply.ryoukai", "voice.ryo");
                if (AssessMode.active()) break;
                AssessMode.toggle();
                break;
            }
            case "assess_close": {
                VoiceInput.speak("reply.ryoukai", "voice.ryo");
                if (!AssessMode.active()) break;
                AssessMode.toggle();
                break;
            }
            case "repeat": {
                if (lastReply.isEmpty()) {
                    VoiceInput.speak("reply.jouhou_nashi", "voice.hi");
                    break;
                }
                SageDialogue.reply(lastReply, lastSubtitle);
                break;
            }
            case "repeat_announce": {
                VoiceInput.speak("reply.ryoukai", "voice.ryo");
                break;
            }
            case "say_something": {
                VoiceInput.speak("reply.taiki", "voice.koku");
                break;
            }
            case "clip_count": {
                VoiceInput.speak("reply.onsei_suu", "voice.koku");
                break;
            }
            case "version": {
                VoiceInput.speak("reply.version", "voice.koku");
                break;
            }
            case "quiet_mode": 
            case "loud_mode": 
            case "tips_off": 
            case "tips_on": 
            case "mute_resistances": 
            case "unmute_all": 
            case "mute_skill": 
            case "unmute_skill": 
            case "species_forget": {
                VoiceInput.speak("reply.settei_henkou", "voice.ryo");
                break;
            }
            case "archive_count": {
                VoiceInput.speakValue("reply.kansatsu_suu", SageArchive.all().size(), "greatsage.voice.kansatsu_suu");
                break;
            }
            case "seen_before": 
            case "species_seen": {
                SageArchive.Sighting seen = VoiceInput.findSighting(match.argument());
                VoiceInput.speak(seen == null ? "reply.kiroku_nashi" : "reply.kiroku_ari", seen == null ? "voice.hi" : "voice.ze");
                break;
            }
            case "strongest_seen": {
                List<SageArchive.Sighting> all = SageArchive.all();
                if (all.isEmpty()) {
                    VoiceInput.speak("reply.mikansoku", "voice.hi");
                    break;
                }
                VoiceInput.say(List.of("voice.kai", "reply.saikyou_kiroku", "num.desu"), (Component)Component.translatable((String)"greatsage.voice.saikyou_kiroku", (Object[])new Object[]{all.get(0).name()}));
                break;
            }
            case "about_species": 
            case "species_power": {
                SageArchive.Sighting seen = VoiceInput.findSighting(match.argument());
                if (seen == null) {
                    VoiceInput.speak("reply.mikansoku", "voice.hi");
                    break;
                }
                VoiceInput.say(List.of("voice.kai", "reply.heikin_sonzaichi", "num.desu"), (Component)Component.translatable((String)"greatsage.voice.heikin_sonzaichi", (Object[])new Object[]{seen.name(), VoiceInput.written(seen.averageEp())}));
                break;
            }
            case "mark_place": {
                Waypoints.mark(minecraft);
                VoiceInput.speak("reply.kiroku_kanryou", "voice.ryo");
                break;
            }
            case "recall_place": {
                String where = Waypoints.recall();
                if (where == null) {
                    VoiceInput.speak("reply.gaitou_nashi", "voice.hi");
                    break;
                }
                VoiceInput.say(List.of("voice.kai", "reply.zahyou", "num.desu"), (Component)Component.translatable((String)"greatsage.voice.zahyou", (Object[])new Object[]{where}));
                break;
            }
            case "clear_mark": {
                Waypoints.clear();
                VoiceInput.speak("reply.shoukyo_kanryou", "voice.ryo");
                break;
            }
            case "death_point": {
                String where = Waypoints.death();
                if (where == null) {
                    VoiceInput.speak("reply.gaitou_nashi", "voice.hi");
                    break;
                }
                VoiceInput.say(List.of("voice.kai", "reply.shibou_chiten", "num.desu"), (Component)Component.translatable((String)"greatsage.voice.shibou_chiten", (Object[])new Object[]{where}));
                break;
            }
            case "scan_this": {
                if (!AssessMode.active()) {
                    AssessMode.toggle();
                }
                VoiceInput.speak("reply.ryoukai", "voice.ryo");
                break;
            }
            default: {
                VoiceInput.speak("reply.jouhou_nashi", "voice.hi");
            }
        }
    }

    private static void speakValue(String frame, double value, String key) {
        ArrayList<String> clips = new ArrayList<String>();
        clips.add("voice.kai");
        clips.add(frame);
        clips.addAll(NumberSpeech.of(value));
        clips.add("num.desu");
        VoiceInput.say(clips, (Component)Component.translatable((String)key, (Object[])new Object[]{NumberSpeech.written(value)}));
    }

    private static void say(List<String> clips, Component text) {
        lastReply = clips;
        lastSubtitle = text;
        SageDialogue.reply(clips, text);
    }

    private static String written(double value) {
        return NumberSpeech.written(value);
    }

    @Nullable
    private static SageArchive.Sighting findSighting(String wanted) {
        if (wanted.isBlank()) {
            return null;
        }
        String needle = wanted.toLowerCase(Locale.ROOT).trim();
        for (SageArchive.Sighting sighting : SageArchive.all()) {
            String label = sighting.name().toLowerCase(Locale.ROOT);
            if (!label.equals(needle) && !label.contains(needle)) continue;
            return sighting;
        }
        return null;
    }

    private static void speak(String reply, String marker) {
        List<String> clips = List.of(marker, reply);
        MutableComponent text = Component.translatable((String)("greatsage.voice." + reply.replace("reply.", "")));
        lastReply = clips;
        lastSubtitle = text;
        SageDialogue.reply(clips, (Component)text);
    }

    public static void onReply(VoiceReplyPayload payload) {
        state = State.ASLEEP;
        stateSince = Util.getMillis();
        VoiceSpeech.close();
        Object[] args = payload.args().toArray();
        MutableComponent text = payload.subtitle().isEmpty() ? Component.empty() : Component.translatable((String)payload.subtitle(), (Object[])args);
        lastReply = payload.clips();
        lastSubtitle = text;
        SageDialogue.reply(payload.clips(), (Component)text);
    }

    static {
        HEARD = new StringBuilder();
        stateSince = Util.getMillis();
        lastReply = List.of();
        lastSubtitle = Component.empty();
    }

    public static enum State {
        ASLEEP,
        LISTENING,
        THINKING;

    }
}

