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

import dev.dedek.greatsage.CeremonyType;
import java.util.Arrays;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class GreatSageCommonConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue JOIN_GRACE_TICKS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ANNOUNCED_CEREMONIES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> SHIELDED_CEREMONIES;
    public static final ModConfigSpec.IntValue CEREMONY_SHIELD_TICKS;
    public static final ModConfigSpec.IntValue ANALYSIS_TICKS;
    public static final ModConfigSpec.DoubleValue ASSESS_RANGE;
    public static final ModConfigSpec.DoubleValue ASSESS_EP_RATIO;
    public static final ModConfigSpec.DoubleValue ASSESS_NAUSEA_RATIO;
    public static final ModConfigSpec.BooleanValue ENABLE_TIPS;
    public static final ModConfigSpec.IntValue TIP_QUIET_TICKS;

    private GreatSageCommonConfig() {
    }

    public static boolean ceremonyAnnounced(CeremonyType type) {
        for (String entry : (List)ANNOUNCED_CEREMONIES.get()) {
            if (!CeremonyType.matches(entry, type)) continue;
            return true;
        }
        return false;
    }

    public static boolean ceremonyShields(CeremonyType type) {
        for (String entry : (List)SHIELDED_CEREMONIES.get()) {
            if (!CeremonyType.matches(entry, type)) continue;
            return true;
        }
        return false;
    }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("general");
        JOIN_GRACE_TICKS = builder.comment(new String[]{"Silence delay in ticks after a player joins the world.", "Tensura hands out the whole racial kit at that moment: resistances,", "intrinsics... Without this delay every login and every respawn would", "trigger an endless announce listing stuff you already had.", "100 = 5 seconds. Put 0 if you really want to hear everything on login."}).defineInRange("joinGraceTicks", 100, 0, 600);
        ANNOUNCED_CEREMONIES = builder.comment(new String[]{"Progression steps that get their own announce.", "Possible values: hero_egg, demon_lord_seed, harvest_festival, true_hero,", "true_demon_lord"}).defineList("announcedCeremonies", Arrays.asList("hero_egg", "demon_lord_seed", "harvest_festival", "true_hero", "true_demon_lord"), () -> "true_hero", o -> {
            String s;
            return o instanceof String && !(s = (String)o).isBlank();
        });
        SHIELDED_CEREMONIES = builder.comment(new String[]{"Ceremonies where the player cannot take damage. Awakening animations are", "long and you cannot move, so dying in the middle of one would be rough."}).defineList("shieldedCeremonies", Arrays.asList("true_hero", "true_demon_lord"), () -> "true_hero", o -> {
            String s;
            return o instanceof String && !(s = (String)o).isBlank();
        });
        CEREMONY_SHIELD_TICKS = builder.comment("How long that protection lasts, in ticks. 400 = 20 seconds.").defineInRange("ceremonyShieldTicks", 400, 20, 2400);
        ANALYSIS_TICKS = builder.comment(new String[]{"Base duration of one analysis, in ticks. 1200 = one minute.", "The real duration is multiplied by rarity: x2 common and intrinsic,", "x3 extra, x5 unique, x8 ultimate."}).defineInRange("analysisTicks", 1200, 20, 72000);
        ASSESS_RANGE = builder.comment("Max distance in blocks at which a target can be read in assess mode.").defineInRange("assessRange", 48.0, 4.0, 128.0);
        ASSESS_EP_RATIO = builder.comment(new String[]{"Above this EP ratio the target is beyond what Great Sage can read and the", "analysis fails. This is the limit that makes assess mode mean something,", "not everything is readable. 10 means ten times your own EP."}).defineInRange("assessEpRatio", 10.0, 1.0, 10000.0);
        ASSESS_NAUSEA_RATIO = builder.comment(new String[]{"EP ratio where being close to a creature makes you sick in assess mode.", "This is the real game nausea effect, level 3, level 4 if the gap is huge."}).defineInRange("assessNauseaRatio", 8.0, 1.0, 10000.0);
        ENABLE_TIPS = builder.comment(new String[]{"Great Sage comments on what you are doing: ore hidden behind a block,", "incoming danger, tool about to break...", "You need the Great Sage skill for these, always. Skill announces and", "ceremonies are the only things that work without it.", "false: it only speaks for skills and ceremonies."}).define("enableTips", true);
        TIP_QUIET_TICKS = builder.comment(new String[]{"Minimum silence between two comments, whatever they are. 600 = 30 seconds.", "This is the setting that decides if Great Sage is a companion or an", "annoying parrot. Each comment also has its own cooldown on top of this."}).defineInRange("tipQuietTicks", 600, 20, 24000);
        builder.pop();
        SPEC = builder.build();
    }
}

