/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.github.manasmods.manascore.skill.api.ManasSkill
 *  io.github.manasmods.manascore.skill.api.ManasSkillInstance
 *  io.github.manasmods.manascore.skill.api.SkillAPI
 *  io.github.manasmods.tensura.ability.skill.Skill
 *  io.github.manasmods.tensura.ability.subclass.ISpatialStorage
 *  io.github.manasmods.tensura.menu.container.SpatialStorageContainer
 *  javax.annotation.Nullable
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.item.ItemStack
 */
package dev.dedek.greatsage.client.voice;

import dev.dedek.greatsage.GreatSageConfig;
import dev.dedek.greatsage.client.SageArchive;
import dev.dedek.greatsage.voice.VoiceCommand;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.ability.subclass.ISpatialStorage;
import io.github.manasmods.tensura.menu.container.SpatialStorageContainer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class VoiceGrammar {
    private static final Map<String, Double> WEIGHTS = new HashMap<String, Double>();

    private VoiceGrammar() {
    }

    public static String wakeJson() {
        ArrayList<String> words = new ArrayList<String>(VoiceGrammar.wakeWords());
        return VoiceGrammar.json(words);
    }

    public static String fullJson(@Nullable LocalPlayer player) {
        LinkedHashSet<String> phrases = new LinkedHashSet<String>(VoiceGrammar.wakeWords());
        List<String> skills = VoiceGrammar.skillNames(player);
        List<String> species = VoiceGrammar.speciesNames();
        List<String> items = VoiceGrammar.itemNames(player);
        for (VoiceCommand command : VoiceCommand.all()) {
            for (String phrase : command.phrases()) {
                switch (command.slot()) {
                    case NONE: {
                        phrases.add(phrase);
                        break;
                    }
                    case SKILL: {
                        VoiceGrammar.expand(phrases, phrase, skills);
                        break;
                    }
                    case SPECIES: {
                        VoiceGrammar.expand(phrases, phrase, species);
                        break;
                    }
                    case ITEM: {
                        VoiceGrammar.expand(phrases, phrase, items);
                    }
                }
            }
        }
        return VoiceGrammar.json(new ArrayList<String>(phrases));
    }

    private static void expand(Set<String> into, String phrase, List<String> values) {
        if (values.isEmpty()) {
            into.add(phrase);
            return;
        }
        for (String value : values) {
            into.add(phrase + " " + value);
        }
    }

    private static String json(List<String> phrases) {
        StringBuilder out = new StringBuilder("[");
        for (String phrase : phrases) {
            if (out.length() > 1) {
                out.append(',');
            }
            out.append('\"').append(VoiceGrammar.clean(phrase)).append('\"');
        }
        if (out.length() > 1) {
            out.append(',');
        }
        return out.append("\"[unk]\"]").toString();
    }

    private static String clean(String raw) {
        return raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    private static List<String> wakeWords() {
        ArrayList<String> out = new ArrayList<String>();
        for (String word : (List)GreatSageConfig.WAKE_WORDS.get()) {
            String cleaned = VoiceGrammar.clean(word);
            if (cleaned.isEmpty()) continue;
            out.add(cleaned);
        }
        return out;
    }

    private static List<String> skillNames(@Nullable LocalPlayer player) {
        ArrayList<String> out = new ArrayList<String>();
        if (player == null) {
            return out;
        }
        for (ManasSkillInstance instance : SkillAPI.getSkillsFrom((LivingEntity)player).getLearnedSkills()) {
            Skill skill;
            String name;
            ManasSkill manasSkill = instance.getSkill();
            if (!(manasSkill instanceof Skill) || (name = VoiceGrammar.clean((skill = (Skill)manasSkill).getName().getString())).isEmpty() || out.contains(name)) continue;
            out.add(name);
        }
        return out;
    }

    private static List<String> speciesNames() {
        ArrayList<String> out = new ArrayList<String>();
        for (SageArchive.Sighting sighting : SageArchive.all()) {
            String name = VoiceGrammar.clean(sighting.name());
            if (name.isEmpty() || out.contains(name)) continue;
            out.add(name);
        }
        return out;
    }

    private static List<String> itemNames(@Nullable LocalPlayer player) {
        ArrayList<String> out = new ArrayList<String>();
        if (player == null) {
            return out;
        }
        for (ItemStack stack : player.getInventory().items) {
            VoiceGrammar.add(out, stack);
        }
        for (ManasSkillInstance instance : SkillAPI.getSkillsFrom((LivingEntity)player).getLearnedSkills()) {
            ManasSkill manasSkill = instance.getSkill();
            if (!(manasSkill instanceof ISpatialStorage)) continue;
            ISpatialStorage holder = (ISpatialStorage)manasSkill;
            try {
                SpatialStorageContainer container = holder.getSpatialStorage(instance, (HolderLookup.Provider)player.registryAccess());
                if (container == null) continue;
                for (int slot = 0; slot < container.getContainerSize(); ++slot) {
                    VoiceGrammar.add(out, container.getItem(slot));
                }
            }
            catch (RuntimeException runtimeException) {
            }
        }
        return out;
    }

    private static void add(List<String> out, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        String name = VoiceGrammar.clean(stack.getHoverName().getString());
        if (!name.isEmpty() && !out.contains(name)) {
            out.add(name);
        }
    }

    public static boolean isWake(String heard) {
        String text = VoiceGrammar.clean(heard);
        for (String word : VoiceGrammar.wakeWords()) {
            if (!text.equals(word) && !text.startsWith(word + " ")) continue;
            return true;
        }
        return false;
    }

    public static String stripWake(String heard) {
        String text = VoiceGrammar.clean(heard);
        boolean again = true;
        block0: while (again) {
            again = false;
            for (String word : VoiceGrammar.wakeWords()) {
                if (text.equals(word)) {
                    return "";
                }
                if (!text.startsWith(word + " ")) continue;
                text = text.substring(word.length() + 1).trim();
                again = true;
                continue block0;
            }
        }
        return text;
    }

    @Nullable
    public static Match parse(String heard, @Nullable LocalPlayer player) {
        List<String> words = VoiceGrammar.tokens(VoiceGrammar.stripWake(heard));
        if (words.isEmpty()) {
            return null;
        }
        List<String> skills = VoiceGrammar.skillNames(player);
        List<String> species = VoiceGrammar.speciesNames();
        List<String> items = VoiceGrammar.itemNames(player);
        Match best = null;
        double bestScore = 0.0;
        for (VoiceCommand command : VoiceCommand.all()) {
            if ("wake".equals(command.id())) continue;
            for (String phrase : command.phrases()) {
                double score;
                List<String> pattern = VoiceGrammar.tokens(VoiceGrammar.clean(phrase));
                if (pattern.isEmpty()) continue;
                Fit fit = VoiceGrammar.fit(pattern, words);
                double coverage = fit.total <= 0.0 ? 0.0 : fit.weight / fit.total;
                if (coverage < 0.62 || fit.matched < 1) continue;
                String argument = "";
                double bonus = 0.0;
                double ignored = 0.0;
                for (String spare : VoiceGrammar.tokens(fit.leftover)) {
                    ignored += VoiceGrammar.weight(spare);
                }
                if (command.slot() != VoiceCommand.Slot.NONE) {
                    List<String> pool = switch (command.slot()) {
                        case VoiceCommand.Slot.SKILL -> skills;
                        case VoiceCommand.Slot.SPECIES -> species;
                        case VoiceCommand.Slot.ITEM -> items;
                        default -> List.of();
                    };
                    String resolved = VoiceGrammar.closest(fit.leftover, pool);
                    if (resolved == null) continue;
                    argument = resolved;
                    bonus = 4.0;
                } else if (!fit.leftover.isBlank()) {
                    bonus = -1.6 * ignored;
                }
                if (!((score = fit.weight * 2.0 + coverage * 3.0 + bonus) > bestScore) || !(score > 2.0)) continue;
                bestScore = score;
                best = new Match(command, argument);
            }
        }
        return best;
    }

    private static synchronized double weight(String word) {
        if (WEIGHTS.isEmpty()) {
            HashMap<String, Integer> seen = new HashMap<String, Integer>();
            int total = 0;
            for (VoiceCommand voiceCommand : VoiceCommand.all()) {
                for (String phrase : voiceCommand.phrases()) {
                    ++total;
                    for (String token : new LinkedHashSet<String>(VoiceGrammar.tokens(VoiceGrammar.clean(phrase)))) {
                        seen.merge(token, 1, Integer::sum);
                    }
                }
            }
            for (Map.Entry entry : seen.entrySet()) {
                WEIGHTS.put((String)entry.getKey(), Math.log(1.0 + (double)total / (double)((Integer)entry.getValue()).intValue()));
            }
            WEIGHTS.put("", 0.0);
        }
        return WEIGHTS.getOrDefault(word, 3.0);
    }

    private static Fit fit(List<String> pattern, List<String> words) {
        boolean[] used = new boolean[words.size()];
        int matched = 0;
        double weight = 0.0;
        double total = 0.0;
        int cursor = 0;
        block0: for (String want : pattern) {
            total += VoiceGrammar.weight(want);
            for (int i = cursor; i < words.size(); ++i) {
                if (used[i] || !VoiceGrammar.similar(want, words.get(i))) continue;
                used[i] = true;
                ++matched;
                weight += VoiceGrammar.weight(want);
                cursor = i + 1;
                continue block0;
            }
        }
        StringBuilder leftover = new StringBuilder();
        for (int i = 0; i < words.size(); ++i) {
            if (used[i]) continue;
            if (leftover.length() > 0) {
                leftover.append(' ');
            }
            leftover.append(words.get(i));
        }
        return new Fit(matched, weight, total, leftover.toString());
    }

    private static boolean similar(String a, String b) {
        if (a.equals(b)) {
            return true;
        }
        int shortest = Math.min(a.length(), b.length());
        if (shortest <= 3) {
            return false;
        }
        if (a.startsWith(b) || b.startsWith(a)) {
            return Math.abs(a.length() - b.length()) <= 2;
        }
        int tolerance = shortest <= 5 ? 1 : 2;
        return VoiceGrammar.distance(a, b, tolerance) <= tolerance;
    }

    private static int distance(String a, String b, int limit) {
        if (Math.abs(a.length() - b.length()) > limit) {
            return limit + 1;
        }
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); ++j) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); ++i) {
            current[0] = i;
            int rowBest = current[0];
            for (int j = 1; j <= b.length(); ++j) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
                rowBest = Math.min(rowBest, current[j]);
            }
            if (rowBest > limit) {
                return limit + 1;
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }

    private static List<String> tokens(String text) {
        ArrayList<String> out = new ArrayList<String>();
        for (String word : VoiceGrammar.clean(text).split(" ")) {
            if (word.isBlank()) continue;
            out.add(word);
        }
        return out;
    }

    @Nullable
    private static String closest(String spoken, List<String> pool) {
        List<String> said = VoiceGrammar.tokens(spoken);
        if (said.isEmpty()) {
            return null;
        }
        String best = null;
        double bestScore = 0.0;
        for (String value : pool) {
            double score;
            List<String> candidate = VoiceGrammar.tokens(value);
            if (candidate.isEmpty()) continue;
            int shared = 0;
            block1: for (String word : candidate) {
                for (String heard : said) {
                    if (!VoiceGrammar.similar(word, heard)) continue;
                    ++shared;
                    continue block1;
                }
            }
            if (shared == 0 || !((score = (double)shared / (double)candidate.size() + (double)shared / (double)said.size()) > bestScore)) continue;
            bestScore = score;
            best = value;
        }
        return bestScore > 1.05 ? best : null;
    }

    private record Fit(int matched, double weight, double total, String leftover) {
    }

    public record Match(VoiceCommand command, String argument) {
    }
}

