/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.github.manasmods.tensura.storage.TensuraStorages
 *  io.github.manasmods.tensura.storage.ep.IExistence
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.tags.DamageTypeTags
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent
 *  net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerLoggedOutEvent
 *  net.neoforged.neoforge.event.tick.PlayerTickEvent$Post
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package dev.dedek.greatsage.server;

import dev.dedek.greatsage.CeremonyType;
import dev.dedek.greatsage.GreatSageCommonConfig;
import dev.dedek.greatsage.GreatSageGameRules;
import dev.dedek.greatsage.network.CeremonyPayload;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="greatsage")
public final class AwakeningWatcher {
    private static final int POLL_INTERVAL_TICKS = 5;
    private static final int FLAG_HERO_EGG = 1;
    private static final int FLAG_DEMON_LORD_SEED = 2;
    private static final int FLAG_TRUE_HERO = 4;
    private static final int FLAG_TRUE_DEMON_LORD = 8;
    private static final int FLAG_HARVEST = 16;
    private static final Map<UUID, Integer> SNAPSHOTS = new HashMap<UUID, Integer>();
    private static final Map<UUID, Long> SHIELDED = new HashMap<UUID, Long>();

    private AwakeningWatcher() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Integer previous;
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player2 = (ServerPlayer)player;
        AwakeningWatcher.expireShield(player2);
        if (player2.tickCount % 5 != 0) {
            return;
        }
        IExistence existence = TensuraStorages.getExistenceFrom((LivingEntity)player2);
        if (existence == null) {
            return;
        }
        int flags = 0;
        if (existence.isHeroEgg()) {
            flags |= 1;
        }
        if (existence.isDemonLordSeed()) {
            flags |= 2;
        }
        if (existence.isTrueHero()) {
            flags |= 4;
        }
        if (existence.isTrueDemonLord()) {
            flags |= 8;
        }
        if (existence.getHarvestTick() > 0) {
            flags |= 0x10;
        }
        if ((previous = SNAPSHOTS.put(player2.getUUID(), flags)) == null) {
            return;
        }
        int gained = flags & ~previous.intValue();
        if (gained == 0) {
            return;
        }
        if ((gained & 8) != 0) {
            AwakeningWatcher.fire(player2, CeremonyType.TRUE_DEMON_LORD);
        } else if ((gained & 4) != 0) {
            AwakeningWatcher.fire(player2, CeremonyType.TRUE_HERO);
        } else if ((gained & 0x10) != 0) {
            AwakeningWatcher.fire(player2, CeremonyType.HARVEST_FESTIVAL);
        } else if ((gained & 2) != 0) {
            AwakeningWatcher.fire(player2, CeremonyType.DEMON_LORD_SEED);
        } else if ((gained & 1) != 0) {
            AwakeningWatcher.fire(player2, CeremonyType.HERO_EGG);
        }
    }

    private static void fire(ServerPlayer player, CeremonyType type) {
        if (!GreatSageCommonConfig.ceremonyAnnounced(type) || !GreatSageGameRules.allowed((LivingEntity)player)) {
            return;
        }
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new CeremonyPayload(type.id()), (CustomPacketPayload[])new CustomPacketPayload[0]);
        if (GreatSageCommonConfig.ceremonyShields(type)) {
            SHIELDED.put(player.getUUID(), player.level().getGameTime() + (long)((Integer)GreatSageCommonConfig.CEREMONY_SHIELD_TICKS.get()).intValue());
        }
    }

    private static void expireShield(ServerPlayer player) {
        Long until = SHIELDED.get(player.getUUID());
        if (until != null && player.level().getGameTime() >= until) {
            SHIELDED.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player = (ServerPlayer)livingEntity;
        Long until = SHIELDED.get(player.getUUID());
        if (until == null || player.level().getGameTime() >= until) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        SNAPSHOTS.remove(id);
        SHIELDED.remove(id);
    }
}

