/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.neoforged.neoforge.common.ModConfigSpec
 *  net.neoforged.neoforge.common.ModConfigSpec$BooleanValue
 *  net.neoforged.neoforge.common.ModConfigSpec$Builder
 *  net.neoforged.neoforge.common.ModConfigSpec$ConfigValue
 *  net.neoforged.neoforge.common.ModConfigSpec$DoubleValue
 *  net.neoforged.neoforge.common.ModConfigSpec$IntValue
 */
package dev.dedek.greatsage;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class GreatSageConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> VOICE_INPUT;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WAKE_WORDS;
    public static final ModConfigSpec.IntValue LISTEN_WINDOW_TICKS;
    public static final ModConfigSpec.BooleanValue VOICE_FEEDBACK;
    public static final ModConfigSpec.BooleanValue VOICE_AUTO_DOWNLOAD;
    public static final ModConfigSpec.DoubleValue VOICE_CONFIDENCE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BATTLE_SKILLS;
    public static final ModConfigSpec.BooleanValue ENABLE_VOICE;
    public static final ModConfigSpec.BooleanValue VOICE_WITHOUT_NAME_CLIP;
    public static final ModConfigSpec.BooleanValue ENABLE_SFX;
    public static final ModConfigSpec.BooleanValue ENABLE_OVERLAY;
    public static final ModConfigSpec.BooleanValue SHOW_WITH_HIDDEN_HUD;
    public static final ModConfigSpec.BooleanValue COMPACT_SKILLS;
    public static final ModConfigSpec.DoubleValue VOLUME;
    public static final ModConfigSpec.DoubleValue ASSESS_WARN_RATIO;
    public static final ModConfigSpec.DoubleValue DARKEN_STRENGTH;
    public static final ModConfigSpec.IntValue BATCH_WINDOW_TICKS;
    public static final ModConfigSpec.IntValue MAX_QUEUED;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MENU_SKILLS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> TIER_ORDER;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ANNOUNCED_TIERS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> SKILL_BLACKLIST;
    public static final ModConfigSpec.IntValue CEREMONY_HOLD_TICKS;

    private GreatSageConfig() {
    }

    public static boolean tierAnnounced(String tier) {
        if (tier == null || tier.isEmpty()) {
            return true;
        }
        String needle = tier.toLowerCase(Locale.ROOT);
        for (String allowed : (List)ANNOUNCED_TIERS.get()) {
            if (!allowed.trim().equalsIgnoreCase(needle)) continue;
            return true;
        }
        return false;
    }

    public static boolean blacklisted(String skillId) {
        for (String entry : (List)SKILL_BLACKLIST.get()) {
            if (!entry.trim().equalsIgnoreCase(skillId)) continue;
            return true;
        }
        return false;
    }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Great Sage client settings. Sounds, overlay, menu.").push("general");
        ENABLE_VOICE = builder.comment("Play the voice clips. Turn this off if you only want the visuals.").define("enableVoice", true);
        VOICE_WITHOUT_NAME_CLIP = builder.comment(new String[]{"Say the sentence (Koku ... no kakutoku ni seiko shimashita) even when none", "of the skills in the announce has a recorded name clip. Missing names just", "leave a gap, the rest is still spoken.", "false: an announce with no name clip at all stays completely silent."}).define("voiceWithoutNameClip", true);
        ENABLE_OVERLAY = builder.comment("Show the on screen animation. The voice still plays if you turn this off.").define("enableOverlay", true);
        SHOW_WITH_HIDDEN_HUD = builder.comment(new String[]{"Keep the announce visible when the HUD is hidden with F1.", "Useful to record a video without the hotbar and the health bar.", "false: F1 hides it like the rest of the interface."}).define("showWithHiddenHud", false);
        ENABLE_SFX = builder.comment(new String[]{"Play the sound effects: Great Sage showing up at the start of every", "announce, and the impact on each skill name."}).define("enableSfx", true);
        COMPACT_SKILLS = builder.comment(new String[]{"Quiet announce for skills. Instead of taking the whole screen, Great Sage", "slides in from the bottom right corner and speaks in a small bubble.", "Nothing gets darkened and you can keep playing. The voice does not change.", "Ceremonies (Demon Lord Seed, awakenings...) and evolutions always keep", "the full thing, whatever you put here."}).define("compactSkillAnnounce", false);
        VOLUME = builder.comment("Volume of the voice clips. Multiplied by the game Master volume.").defineInRange("volume", 1.0, 0.0, 2.0);
        ASSESS_WARN_RATIO = builder.comment(new String[]{"EP ratio where a target gets flagged as dangerous in assess mode.", "Its aura turns red and Great Sage tells you not to fight it.", "5 means five times your own EP."}).defineInRange("assessWarnRatio", 5.0, 1.0, 1000.0);
        VOICE_INPUT = builder.comment(new String[]{"How you talk to Great Sage. Off by default, nobody opens your mic", "without asking.", "off        : nothing listens, ever.", "wake       : the mic stays open but only listens for your wake words.", "             Say one and Great Sage shows up and waits for the request.", "pushToTalk : the mic only opens while you hold the key, and you say the", "             whole thing in one go.", "You also need the recognition library in .minecraft/greatsage-voice/,", "it is not shipped with the mod. Everything runs offline, nothing is sent", "anywhere."}).define("voiceInput", (Object)"off");
        WAKE_WORDS = builder.comment(new String[]{"What wakes him up in wake mode. Keep them short and distinctive,", "a one syllable word will trigger on everything you say.", "They have to be real English words: the model only knows English, and", "anything it does not know gets silently dropped. That is why 'dai kenja'", "is not in here, 'kenja' does not exist for it."}).defineList("wakeWords", List.of("great sage", "wise one", "sage"), entry -> entry instanceof String);
        LISTEN_WINDOW_TICKS = builder.comment(new String[]{"How long you get to formulate after the wake word. 120 = 6 seconds.", "Too short and you get cut off mid sentence, too long and he stands", "there listening to your whole conversation."}).defineInRange("listenWindowTicks", 120, 40, 400);
        VOICE_FEEDBACK = builder.comment(new String[]{"Play the little sound when he starts listening.", "false: he shows up silently."}).define("voiceFeedback", true);
        VOICE_AUTO_DOWNLOAD = builder.comment(new String[]{"Fetch the recognition library and the English model on your behalf,", "into .minecraft/greatsage-voice/. About 65 MB, once.", "It only ever runs when voiceInput is not 'off'. If you never speak to", "him, nothing is downloaded, ever.", "The two jars are checked against a known fingerprint before anything", "loads them, because a native library runs with the same rights as the", "game and I am not asking you to trust the network for that.", "false: put the files there yourself, the mod will use them."}).define("voiceAutoDownload", true);
        VOICE_CONFIDENCE = builder.comment(new String[]{"How sure the recognition has to be before Great Sage acts on it.", "A badly heard sentence is worse than no sentence: it turns into a", "random command. Raise this if he keeps doing things you did not ask", "for, lower it if he says he did not understand when you were clear.", "0 accepts everything."}).defineInRange("voiceConfidence", 0.55, 0.0, 1.0);
        BATTLE_SKILLS = builder.comment(new String[]{"Skills Great Sage raises when you ask for battle mode.", "You do not need to edit this by hand: the Battle Mode page in the", "menu (key G) does it for you, and writes here.", "Toggle skills are held on for as long as battle mode lasts. Skills", "that are not toggles get recast as soon as their cooldown ends."}).defineList("battleSkills", List.of(), entry -> entry instanceof String);
        DARKEN_STRENGTH = builder.comment("How dark the screen gets during an announce. 0 = no darkening at all.").defineInRange("darkenStrength", 0.72, 0.0, 1.0);
        BATCH_WINDOW_TICKS = builder.comment(new String[]{"Grouping window in ticks. Every skill unlocked during that window ends up", "in one single announce, and each new skill pushes the window further.", "Too short and one evolution gets split into several announces.", "Too long and the announce takes ages to start."}).defineInRange("batchWindowTicks", 10, 1, 100);
        MAX_QUEUED = builder.comment("How many announces can wait in line.").defineInRange("maxQueued", 4, 1, 32);
        MENU_SKILLS = builder.comment(new String[]{"Skills that give access to the Great Sage menu. Without one of them the", "keybind does nothing. Great Sage evolves into Raphael, so add the evolved", "form here if you want to keep the menu after evolving."}).defineList("menuSkills", List.of("tensura:great_sage"), () -> "tensura:great_sage", o -> {
            String s;
            return o instanceof String && !(s = (String)o).isBlank();
        });
        TIER_ORDER = builder.comment(new String[]{"Order the categories are spoken in inside one announce.", "Anything not listed here gets spoken at the end."}).defineList("tierOrder", Arrays.asList("extra", "unique", "ultimate", "intrinsic", "common", "resistance"), () -> "unique", o -> {
            String s;
            return o instanceof String && !(s = (String)o).isBlank();
        });
        ANNOUNCED_TIERS = builder.comment(new String[]{"Skill types that trigger an announce.", "Possible values: resistance, intrinsic, common, extra, unique, ultimate"}).defineList("announcedTiers", Arrays.asList("resistance", "intrinsic", "common", "extra", "unique", "ultimate"), () -> "unique", o -> {
            String s;
            return o instanceof String && !(s = (String)o).isBlank();
        });
        CEREMONY_HOLD_TICKS = builder.comment(new String[]{"A ceremony that lists skills stays open as long as new ones keep coming.", "This is how long it waits in silence after the last one before saying the", "closing line. Also used for normal announces, so Predator giving skills in", "waves does not trigger ten announces in a row. 40 = 2 seconds."}).defineInRange("ceremonyHoldTicks", 40, 5, 400);
        SKILL_BLACKLIST = builder.comment(new String[]{"Skill ids that never get announced, for example \"tensura:self_regeneration\".", "They still show up in the Records page, this only silences the announce."}).defineList("skillBlacklist", List.of(), () -> "tensura:example", o -> {
            String s;
            return o instanceof String && !(s = (String)o).isBlank();
        });
        builder.pop();
        SPEC = builder.build();
    }
}

