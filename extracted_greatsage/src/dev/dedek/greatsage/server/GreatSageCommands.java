/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.suggestion.Suggestions
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 *  dev.architectury.registry.registries.Registrar
 *  io.github.manasmods.manascore.race.api.ManasRace
 *  io.github.manasmods.manascore.race.api.RaceAPI
 *  io.github.manasmods.manascore.skill.api.ManasSkill
 *  io.github.manasmods.manascore.skill.api.SkillAPI
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.commands.SharedSuggestionProvider
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.neoforge.event.RegisterCommandsEvent
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package dev.dedek.greatsage.server;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.architectury.registry.registries.Registrar;
import dev.dedek.greatsage.CeremonyType;
import dev.dedek.greatsage.network.CeremonyPayload;
import dev.dedek.greatsage.network.EvolutionPayload;
import dev.dedek.greatsage.server.SkillAnnouncer;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="greatsage")
public final class GreatSageCommands {
    private static final String[] DEMO_SKILLS = new String[]{"magic_sense", "universal_perception", "great_sage", "predator", "dragon_mode", "drain", "self_regeneration"};

    private GreatSageCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"greatsage").then(Commands.literal((String)"ceremony").then(Commands.argument((String)"type", (ArgumentType)StringArgumentType.word()).suggests((context, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(CeremonyType.values()).map(CeremonyType::id), (SuggestionsBuilder)builder)).executes(context -> GreatSageCommands.ceremony(((CommandSourceStack)context.getSource()).getPlayerOrException(), StringArgumentType.getString((CommandContext)context, (String)"type"), (CommandSourceStack)context.getSource()))))).then(Commands.literal((String)"evolve").then(Commands.argument((String)"from", (ArgumentType)StringArgumentType.word()).then(Commands.argument((String)"to", (ArgumentType)StringArgumentType.word()).executes(context -> GreatSageCommands.previewEvolution(((CommandSourceStack)context.getSource()).getPlayerOrException(), StringArgumentType.getString((CommandContext)context, (String)"from"), StringArgumentType.getString((CommandContext)context, (String)"to"), (CommandSourceStack)context.getSource())))))).then(Commands.literal((String)"demo").executes(context -> GreatSageCommands.demo(((CommandSourceStack)context.getSource()).getPlayerOrException())))).then(Commands.literal((String)"preview").then(Commands.argument((String)"skills", (ArgumentType)StringArgumentType.greedyString()).suggests((context, builder) -> GreatSageCommands.suggestSkills(builder)).executes(context -> GreatSageCommands.preview(((CommandSourceStack)context.getSource()).getPlayerOrException(), StringArgumentType.getString((CommandContext)context, (String)"skills"), (CommandSourceStack)context.getSource())))));
    }

    private static CompletableFuture<Suggestions> suggestSkills(SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        int lastSpace = remaining.lastIndexOf(32);
        SuggestionsBuilder target = lastSpace < 0 ? builder : builder.createOffset(builder.getStart() + lastSpace + 1);
        return SharedSuggestionProvider.suggestResource((Iterable)SkillAPI.getSkillRegistry().getIds(), (SuggestionsBuilder)target);
    }

    private static int demo(ServerPlayer player) {
        int sent = 0;
        for (String path : DEMO_SKILLS) {
            ManasSkill skill = (ManasSkill)SkillAPI.getSkillRegistry().get(ResourceLocation.fromNamespaceAndPath((String)"tensura", (String)path));
            if (skill == null || !SkillAnnouncer.send(player, skill, false)) continue;
            ++sent;
        }
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new CeremonyPayload(CeremonyType.TRUE_DEMON_LORD.id()), (CustomPacketPayload[])new CustomPacketPayload[0]);
        return sent;
    }

    private static int previewEvolution(ServerPlayer player, String from, String to, CommandSourceStack source) {
        ResourceLocation a = ResourceLocation.fromNamespaceAndPath((String)"tensura", (String)from);
        ResourceLocation b = ResourceLocation.fromNamespaceAndPath((String)"tensura", (String)to);
        Registrar registry = RaceAPI.getRaceRegistry();
        ManasRace ra = (ManasRace)registry.get(a);
        ManasRace rb = (ManasRace)registry.get(b);
        if (ra == null || rb == null) {
            source.sendFailure((Component)Component.literal((String)("Espece inconnue : " + (ra == null ? from : to))));
            return 0;
        }
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new EvolutionPayload(a, (Component)ra.getName(), b, (Component)rb.getName()), (CustomPacketPayload[])new CustomPacketPayload[0]);
        return 1;
    }

    private static int ceremony(ServerPlayer player, String raw, CommandSourceStack source) {
        CeremonyType type = CeremonyType.byId(raw);
        if (type == null) {
            source.sendFailure((Component)Component.literal((String)("Ceremonie inconnue : " + raw)));
            return 0;
        }
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new CeremonyPayload(type.id()), (CustomPacketPayload[])new CustomPacketPayload[0]);
        return 1;
    }

    private static int preview(ServerPlayer player, String raw, CommandSourceStack source) {
        ArrayList<String> unknown = new ArrayList<String>();
        int sent = 0;
        for (String token : raw.trim().split("\\s+")) {
            ManasSkill skill;
            if (token.isEmpty()) continue;
            ResourceLocation id = ResourceLocation.tryParse((String)(token.contains(":") ? token : "tensura:" + token));
            ManasSkill manasSkill = skill = id == null ? null : (ManasSkill)SkillAPI.getSkillRegistry().get(id);
            if (skill == null) {
                unknown.add(token);
                continue;
            }
            if (!SkillAnnouncer.send(player, skill, false)) continue;
            ++sent;
        }
        if (!unknown.isEmpty()) {
            source.sendFailure((Component)Component.literal((String)("Skill inconnu : " + String.join((CharSequence)", ", unknown))));
        }
        return sent;
    }
}

