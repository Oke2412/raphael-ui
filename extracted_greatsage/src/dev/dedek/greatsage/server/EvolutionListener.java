/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.architectury.event.EventResult
 *  io.github.manasmods.manascore.race.api.ManasRace
 *  io.github.manasmods.manascore.race.api.ManasRaceInstance
 *  io.github.manasmods.manascore.race.api.RaceEvents
 *  javax.annotation.Nullable
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.LivingEntity
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package dev.dedek.greatsage.server;

import dev.architectury.event.EventResult;
import dev.dedek.greatsage.GreatSageGameRules;
import dev.dedek.greatsage.network.EvolutionPayload;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceEvents;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

public final class EvolutionListener {
    private EvolutionListener() {
    }

    public static void register() {
        RaceEvents.SET_RACE.register((oldRace, entity, newRace, flag, cancel, message) -> {
            if (!(entity instanceof ServerPlayer)) {
                return EventResult.pass();
            }
            ServerPlayer player = (ServerPlayer)entity;
            if (!GreatSageGameRules.allowed((LivingEntity)player)) {
                return EventResult.pass();
            }
            ResourceLocation from = EvolutionListener.idOf(oldRace);
            ResourceLocation to = EvolutionListener.idOf(newRace);
            if (from == null || to == null || from.equals((Object)to)) {
                return EventResult.pass();
            }
            if (!EvolutionListener.isEvolution(oldRace, player, newRace)) {
                return EventResult.pass();
            }
            PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new EvolutionPayload(from, (Component)oldRace.getRace().getName(), to, (Component)newRace.getRace().getName()), (CustomPacketPayload[])new CustomPacketPayload[0]);
            return EventResult.pass();
        });
    }

    private static boolean isEvolution(ManasRaceInstance oldRace, ServerPlayer player, ManasRaceInstance newRace) {
        ManasRace previous = oldRace.getRace();
        ManasRace next = newRace.getRace();
        if (previous == null || next == null) {
            return false;
        }
        try {
            return previous.getNextEvolutions(oldRace, (LivingEntity)player).contains(next);
        }
        catch (RuntimeException failure) {
            return true;
        }
    }

    @Nullable
    private static ResourceLocation idOf(@Nullable ManasRaceInstance instance) {
        if (instance == null) {
            return null;
        }
        ManasRace race = instance.getRace();
        return race == null ? null : race.getRegistryName();
    }
}

