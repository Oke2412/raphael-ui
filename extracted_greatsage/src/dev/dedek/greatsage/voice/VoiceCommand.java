/*
 * Decompiled with CFR 0.152.
 */
package dev.dedek.greatsage.voice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class VoiceCommand {
    private final String id;
    private final Scope scope;
    private final Slot slot;
    private final List<String> phrases;
    private static final Map<String, VoiceCommand> BY_ID = new LinkedHashMap<String, VoiceCommand>();
    private static final List<VoiceCommand> ALL = new ArrayList<VoiceCommand>();

    private VoiceCommand(String id, Scope scope, Slot slot, List<String> phrases) {
        this.id = id;
        this.scope = scope;
        this.slot = slot;
        this.phrases = phrases;
    }

    public String id() {
        return this.id;
    }

    public Scope scope() {
        return this.scope;
    }

    public Slot slot() {
        return this.slot;
    }

    public List<String> phrases() {
        return this.phrases;
    }

    private static void add(String id, Scope scope, Slot slot, String ... phrases) {
        VoiceCommand command = new VoiceCommand(id, scope, slot, List.of(phrases));
        if (BY_ID.put(id, command) != null) {
            throw new IllegalStateException("commande vocale en double : " + id);
        }
        ALL.add(command);
    }

    private static void local(String id, String ... phrases) {
        VoiceCommand.add(id, Scope.LOCAL, Slot.NONE, phrases);
    }

    private static void ask(String id, String ... phrases) {
        VoiceCommand.add(id, Scope.SERVER, Slot.NONE, phrases);
    }

    public static List<VoiceCommand> all() {
        return List.copyOf(ALL);
    }

    public static VoiceCommand byId(String id) {
        return BY_ID.get(id.toLowerCase(Locale.ROOT));
    }

    public static int count() {
        return ALL.size();
    }

    static {
        VoiceCommand.local("wake", "great sage", "wise one", "sage");
        VoiceCommand.local("present", "are you there", "can you hear me");
        VoiceCommand.local("standby", "stand by", "wait");
        VoiceCommand.local("dismiss_request", "never mind", "forget it", "cancel that");
        VoiceCommand.local("repeat", "repeat that", "say that again", "what did you say");
        VoiceCommand.local("louder", "louder", "speak up");
        VoiceCommand.local("silence", "be quiet", "silence", "stop talking");
        VoiceCommand.local("resume", "resume", "you may speak");
        VoiceCommand.local("sleep", "stop listening", "go to sleep");
        VoiceCommand.local("functions", "what can you do", "list your functions", "help");
        VoiceCommand.local("thanks", "thank you", "thanks");
        VoiceCommand.local("praise", "good work", "well done");
        VoiceCommand.local("alive", "are you alive", "are you conscious");
        VoiceCommand.local("identify", "who are you", "identify yourself");
        VoiceCommand.ask("my_ep", "what is my existence value", "what is my ep", "how much ep do i have", "what is my own existence value", "tell me my existence value");
        VoiceCommand.ask("my_power", "how strong am i", "what is my power");
        VoiceCommand.ask("my_magicules", "what is my magic", "how much magic do i have", "what is my mana", "what is my mp");
        VoiceCommand.ask("magicule_check", "am i low on magic", "do i have enough magic", "am i low on mana");
        VoiceCommand.ask("my_health", "what is my health", "how much health do i have", "how hurt am i");
        VoiceCommand.ask("injured", "am i injured", "am i hurt");
        VoiceCommand.ask("my_race", "what is my race", "what species am i");
        VoiceCommand.ask("can_evolve", "can i evolve", "am i ready to evolve");
        VoiceCommand.ask("evolution_needs", "what do i need to evolve", "what are my evolution requirements");
        VoiceCommand.ask("next_evolution", "what is my next evolution", "what will i become");
        VoiceCommand.ask("evolution_gap", "how close am i to evolving", "how much more do i need");
        VoiceCommand.ask("is_demon_lord", "am i a demon lord", "have i awakened");
        VoiceCommand.ask("is_hero", "am i a hero", "am i a true hero");
        VoiceCommand.ask("skill_count", "how many skills do i have", "count my skills");
        VoiceCommand.ask("mastered_count", "how many skills have i mastered", "count my mastered skills");
        VoiceCommand.ask("strongest_skill", "what is my strongest skill", "what is my best skill");
        VoiceCommand.ask("rarest_skill", "what is my rarest skill", "what is my most unique skill");
        VoiceCommand.ask("weakest_skill", "what is my weakest skill", "what is my worst skill", "what is my least useful skill");
        VoiceCommand.ask("hungry", "am i hungry", "what is my food level");
        VoiceCommand.ask("poisoned", "am i poisoned", "do i have a status effect");
        VoiceCommand.ask("effects", "what effects am i under", "list my effects");
        VoiceCommand.ask("burning", "am i on fire");
        VoiceCommand.ask("air", "how much air do i have", "am i drowning");
        VoiceCommand.ask("holding", "what am i holding", "what is in my hand");
        VoiceCommand.ask("tool_breaking", "is my tool about to break", "check my equipment");
        VoiceCommand.ask("armour", "what is my armour", "how protected am i");
        VoiceCommand.ask("attack", "what is my attack power", "how much damage do i do");
        VoiceCommand.ask("speed", "how fast am i", "what is my speed");
        VoiceCommand.ask("world_age", "how long have i lived", "how old is this world");
        VoiceCommand.ask("deaths", "how many times have i died", "count my deaths");
        VoiceCommand.ask("in_danger", "am i in danger", "assess my situation");
        VoiceCommand.ask("list_skills", "list my skills", "what skills do i have");
        VoiceCommand.ask("list_unique", "list my unique skills");
        VoiceCommand.ask("list_ultimate", "list my ultimate skills");
        VoiceCommand.ask("list_resistance", "list my resistance", "list my resistant skills");
        VoiceCommand.ask("list_intrinsic", "list my intrinsic skills");
        VoiceCommand.ask("list_extra", "list my extra skills");
        VoiceCommand.ask("list_common", "list my common skills");
        VoiceCommand.add("have_skill", Scope.SERVER, Slot.SKILL, "do i have", "can i use");
        VoiceCommand.add("what_skill", Scope.SERVER, Slot.SKILL, "what is", "tell me about");
        VoiceCommand.add("skill_effect", Scope.SERVER, Slot.SKILL, "what does", "explain");
        VoiceCommand.add("skill_mastery", Scope.SERVER, Slot.SKILL, "what is my mastery of", "how well do i know");
        VoiceCommand.add("skill_mastered", Scope.SERVER, Slot.SKILL, "have i mastered", "is complete");
        VoiceCommand.add("analyse", Scope.SERVER, Slot.SKILL, "analyse", "analyze", "start analysing", "break down");
        VoiceCommand.add("toggle_skill", Scope.SERVER, Slot.SKILL, "toggle", "switch");
        VoiceCommand.add("skill_on", Scope.SERVER, Slot.SKILL, "turn on", "enable", "activate");
        VoiceCommand.add("skill_use", Scope.SERVER, Slot.SKILL, "use", "cast", "trigger", "fire", "unleash");
        VoiceCommand.add("skill_off", Scope.SERVER, Slot.SKILL, "turn off", "disable", "deactivate");
        VoiceCommand.add("skill_state", Scope.SERVER, Slot.SKILL, "is on", "is active");
        VoiceCommand.ask("battle_mode", "enable battle mode", "battle mode", "combat mode");
        VoiceCommand.ask("battle_mode_off", "disable battle mode", "stand down mode");
        VoiceCommand.ask("toggle_all_off", "turn everything off", "disable all skills", "disable everything", "turn it all off");
        VoiceCommand.ask("analysis_stop", "stop the analysis", "cancel the analysis", "abort");
        VoiceCommand.ask("analysis_progress", "how is the analysis going", "analysis progress", "how long left");
        VoiceCommand.ask("analysis_target", "what are you analysing", "what is being analysed");
        VoiceCommand.ask("analyse_weakest", "analyse my weakest skill", "analyse something useful");
        VoiceCommand.ask("analysis_advice", "what should i analyse", "recommend an analysis");
        VoiceCommand.add("skill_cost", Scope.SERVER, Slot.SKILL, "how much does cost", "what is the cost of", "what does it cost to learn");
        VoiceCommand.add("can_learn", Scope.SERVER, Slot.SKILL, "can i learn", "am i able to learn");
        VoiceCommand.ask("missing_skills", "what skills am i missing", "what don't i have");
        VoiceCommand.ask("closest_mastery", "what skill is closest to mastery");
        VoiceCommand.ask("unused_skills", "which of my skills are unused");
        VoiceCommand.ask("total_skills", "how many skills exist", "how many skills are there in total");
        VoiceCommand.add("skill_source", Scope.SERVER, Slot.SKILL, "which mod added", "where does come from");
        VoiceCommand.local("open_analyzing", "open my skill list", "show me my skills");
        VoiceCommand.local("open_records", "open the records", "show me all skills");
        VoiceCommand.local("open_observations", "open the observations", "show me what i have scanned");
        VoiceCommand.local("open_menu", "open the menu", "show yourself");
        VoiceCommand.local("close_menu", "close the menu", "dismiss");
        VoiceCommand.ask("race_skills", "what skills does my race grant");
        VoiceCommand.ask("last_learned", "what did i just learn", "what was that skill");
        VoiceCommand.local("repeat_announce", "announce my skills again", "repeat the last announcement");
        VoiceCommand.local("mute_resistances", "be quiet about resistance", "stop announcing resistance");
        VoiceCommand.local("unmute_all", "announce everything", "stop filtering");
        VoiceCommand.local("assess_open", "assess", "open assess mode", "analyse mode");
        VoiceCommand.local("assess_close", "close assess", "exit assess mode");
        VoiceCommand.ask("what_is_this", "what is this", "what am i looking at");
        VoiceCommand.local("scan_this", "scan this", "analyse this creature", "read it");
        VoiceCommand.ask("target_power", "how strong is it", "what is its existence value", "what is the target existence value", "what is the target ep", "what is its ep", "how strong is the target", "what is the target power");
        VoiceCommand.ask("can_i_win", "can i beat it", "should i fight it");
        VoiceCommand.ask("target_skills", "what are its skills", "what can it do", "what are the target skills", "list its skills");
        VoiceCommand.ask("target_weakness", "what are its weaknesses", "how do i kill it");
        VoiceCommand.ask("target_health", "what is its health", "how hurt is it", "what is the target health", "how much health does it have");
        VoiceCommand.ask("target_hostile", "is it hostile", "will it attack me");
        VoiceCommand.ask("target_count", "how many are there", "count them");
        VoiceCommand.ask("strongest_nearby", "what is the strongest thing nearby", "what is the biggest threat");
        VoiceCommand.ask("behind_me", "is anything behind me", "check my back");
        VoiceCommand.ask("followed", "is anything following me", "am i being tracked");
        VoiceCommand.ask("target_distance", "how far is it", "what is the distance");
        VoiceCommand.ask("target_race", "what is its race", "what species is it", "what is the target race", "what is the target species");
        VoiceCommand.ask("can_predate", "can i eat it", "can i predate it");
        VoiceCommand.ask("predate_gain", "what would i gain", "what skills would i get");
        VoiceCommand.local("seen_before", "have i seen this before", "is it in my records");
        VoiceCommand.local("archive_count", "how many have i scanned", "how big is my archive");
        VoiceCommand.local("strongest_seen", "what is the strongest thing i have seen");
        VoiceCommand.add("about_species", Scope.LOCAL, Slot.SPECIES, "tell me about", "what is a");
        VoiceCommand.add("species_power", Scope.LOCAL, Slot.SPECIES, "how strong is a");
        VoiceCommand.add("species_seen", Scope.LOCAL, Slot.SPECIES, "have i scanned a");
        VoiceCommand.add("species_forget", Scope.LOCAL, Slot.SPECIES, "forget", "delete that record");
        VoiceCommand.ask("compare_target", "is it stronger than me", "compare it to me");
        VoiceCommand.ask("survival_time", "how long would it take to kill me");
        VoiceCommand.ask("target_name", "what is its name", "does it have a name");
        VoiceCommand.ask("find_ore", "is there ore nearby", "find ore", "any ore around");
        VoiceCommand.ask("find_diamond", "find diamonds", "any diamonds nearby");
        VoiceCommand.ask("find_iron", "find iron");
        VoiceCommand.ask("find_gold", "find gold");
        VoiceCommand.ask("find_magisteel", "find magic ore", "find rare ore");
        VoiceCommand.ask("find_cave", "is there a cave below", "any caves nearby");
        VoiceCommand.ask("find_lava", "is there lava nearby", "any lava around");
        VoiceCommand.ask("find_water", "is there water nearby");
        VoiceCommand.ask("area_danger", "is this area dangerous", "assess this place");
        VoiceCommand.ask("magicule_density", "how dense is the magic here", "what is the magic density", "how much mana is here");
        VoiceCommand.ask("good_build_spot", "is this a good place to build");
        VoiceCommand.ask("coordinates", "where am i", "what are my coordinates");
        VoiceCommand.ask("dimension", "what dimension is this", "what world am i in");
        VoiceCommand.ask("biome", "what biome is this");
        VoiceCommand.ask("time", "what time is it", "is it night");
        VoiceCommand.ask("until_dawn", "how long until dawn", "when does the sun rise");
        VoiceCommand.ask("until_night", "how long until night");
        VoiceCommand.ask("weather", "what is the weather", "is it raining");
        VoiceCommand.ask("will_storm", "will it storm");
        VoiceCommand.ask("altitude", "how deep am i", "what is my altitude");
        VoiceCommand.ask("facing", "which way is north", "what direction am i facing");
        VoiceCommand.ask("spawn_distance", "how far is my spawn", "where is my bed");
        VoiceCommand.local("death_point", "where did i die", "find my death point");
        VoiceCommand.local("mark_place", "remember this place", "mark this spot");
        VoiceCommand.local("recall_place", "where is the place i marked", "take me back");
        VoiceCommand.local("clear_mark", "forget that place", "clear the mark");
        VoiceCommand.ask("safe_to_sleep", "is it safe to sleep", "can i sleep here");
        VoiceCommand.ask("find_village", "is there a village nearby");
        VoiceCommand.ask("find_labyrinth", "is there a labyrinth nearby");
        VoiceCommand.ask("light_level", "how much light is here", "is it dark");
        VoiceCommand.ask("under_sky", "am i under the sky", "can i see the sun");
        VoiceCommand.ask("drop_below", "is the ground safe below me", "is there a drop");
        VoiceCommand.ask("stomach_list", "what is in my stomach", "list my predator storage");
        VoiceCommand.ask("stomach_fill", "how full is my stomach", "how much space is left");
        VoiceCommand.add("stomach_has", Scope.SERVER, Slot.ITEM, "do i have in my stomach");
        VoiceCommand.add("stomach_take", Scope.SERVER, Slot.ITEM, "take out", "retrieve", "give me");
        VoiceCommand.ask("stomach_store", "store this", "eat this", "swallow it");
        VoiceCommand.add("item_count", Scope.SERVER, Slot.ITEM, "how many do i have", "count my");
        VoiceCommand.ask("have_food", "do i have food", "am i carrying food");
        VoiceCommand.ask("best_weapon", "what is my best weapon");
        VoiceCommand.ask("best_pickaxe", "what is my best pickaxe");
        VoiceCommand.ask("better_tool", "do i have a better tool", "should i switch tools");
        VoiceCommand.ask("equip_best", "equip my best weapon", "switch to my best weapon");
        VoiceCommand.ask("repair", "repair my tool");
        VoiceCommand.ask("free_slots", "how many empty slots do i have", "is my inventory full");
        VoiceCommand.ask("drop_item", "drop this", "throw it away");
        VoiceCommand.ask("identify_item", "what is this item", "identify this");
        VoiceCommand.ask("item_value", "is this item valuable", "is it worth keeping");
        VoiceCommand.add("can_craft", Scope.SERVER, Slot.ITEM, "can i craft", "do i have the materials for");
        VoiceCommand.add("craft_needs", Scope.SERVER, Slot.ITEM, "what do i need for", "what materials are missing for");
        VoiceCommand.add("craft", Scope.SERVER, Slot.ITEM, "craft", "make");
        VoiceCommand.ask("durability", "how much durability is left", "check my tool");
        VoiceCommand.ask("food_advice", "what should i eat", "recommend food");
        VoiceCommand.ask("eat", "eat something", "feed me");
        VoiceCommand.add("sort_inventory", Scope.SERVER, Slot.NONE, "sort my inventory", "tidy my inventory", "arrange my inventory");
        VoiceCommand.add("sort_by_name", Scope.SERVER, Slot.NONE, "sort my inventory by name", "sort by name", "arrange by name");
        VoiceCommand.add("sort_by_count", Scope.SERVER, Slot.NONE, "sort my inventory by quantity", "sort by quantity", "sort by amount", "sort by count");
        VoiceCommand.add("sort_by_type", Scope.SERVER, Slot.NONE, "sort my inventory by type", "sort by type", "sort by category", "group by type");
        VoiceCommand.ask("best_item", "what is my most valuable item");
        VoiceCommand.ask("lost_items", "did i lose anything", "what did i drop");
        VoiceCommand.ask("stomach_left", "how much can my stomach still hold");
        VoiceCommand.ask("should_fight", "should i fight", "is this winnable");
        VoiceCommand.ask("should_run", "should i run", "should i retreat");
        VoiceCommand.ask("survival_odds", "what are my chances", "what is my survival probability");
        VoiceCommand.ask("skill_advice", "what skill should i use", "recommend a skill");
        VoiceCommand.ask("incoming_damage", "how much damage will it do");
        VoiceCommand.ask("one_shot", "can it kill me in one hit");
        VoiceCommand.ask("winning", "am i winning", "how is the fight going");
        VoiceCommand.ask("enemy_health", "how hurt is my enemy");
        VoiceCommand.ask("about_to_die", "is it about to die");
        VoiceCommand.local("watch", "warn me if anything approaches", "keep watch");
        VoiceCommand.local("stop_watch", "stop watching", "stand down");
        VoiceCommand.ask("what_killed_me", "what killed me", "what was that");
        VoiceCommand.ask("target_awakened", "is that thing a demon lord", "is it awakened");
        VoiceCommand.ask("enemy_count", "how many enemies are around", "count the threats");
        VoiceCommand.ask("pick_target", "which one is strongest", "pick a target");
        VoiceCommand.ask("protect", "protect me", "shield me");
        VoiceCommand.ask("heal", "heal me", "restore my health");
        VoiceCommand.ask("fall_safe", "can i survive the fall", "is this drop safe");
        VoiceCommand.ask("overleveled", "is this too easy", "am i too strong for this");
        VoiceCommand.ask("evolve_first", "should i evolve first");
        VoiceCommand.local("quiet_mode", "be discreet", "use quiet mode", "small announcements");
        VoiceCommand.local("loud_mode", "be loud", "full announcements", "normal mode");
        VoiceCommand.local("tips_off", "stop commenting", "disable your remarks");
        VoiceCommand.local("tips_on", "start commenting", "enable your remarks");
        VoiceCommand.local("speak_less", "speak less often", "be more patient");
        VoiceCommand.local("speak_more", "speak more often");
        VoiceCommand.add("mute_skill", Scope.LOCAL, Slot.SKILL, "ignore", "never announce");
        VoiceCommand.add("unmute_skill", Scope.LOCAL, Slot.SKILL, "announce again", "stop ignoring");
        VoiceCommand.local("clip_count", "how many voice lines do you have");
        VoiceCommand.local("version", "what version are you");
        VoiceCommand.local("understood", "did you understand");
        VoiceCommand.local("say_something", "say something", "talk to me");
        VoiceCommand.ask("advice", "tell me something useful", "give me advice");
        VoiceCommand.local("done", "that is all", "we are done", "dismissed");
    }

    public static enum Scope {
        LOCAL,
        SERVER;

    }

    public static enum Slot {
        NONE,
        SKILL,
        SPECIES,
        ITEM;

    }
}

