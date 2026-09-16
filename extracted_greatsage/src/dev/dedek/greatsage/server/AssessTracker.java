/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.github.manasmods.tensura.util.EnergyHelper
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.effect.MobEffects
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerLoggedOutEvent
 *  net.neoforged.neoforge.event.tick.PlayerTickEvent$Post
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package dev.dedek.greatsage.server;

import dev.dedek.greatsage.GreatSageCommonConfig;
import dev.dedek.greatsage.network.AssessAuraPayload;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="greatsage")
public final class AssessTracker {
    private static final int PERIOD_TICKS = 10;
    private static final double RANGE = 64.0;
    private static final int MAX_AURAS = 48;
    private static final Set<UUID> WATCHING = new HashSet<UUID>();

    private AssessTracker() {
    }

    public static void setActive(ServerPlayer player, boolean active) {
        if (active) {
            WATCHING.add(player.getUUID());
            AssessTracker.push(player);
        } else {
            WATCHING.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        ServerPlayer player;
        Player player2 = event.getEntity();
        if (!(player2 instanceof ServerPlayer) || !WATCHING.contains((player = (ServerPlayer)player2).getUUID()) || player.tickCount % 10 != 0) {
            return;
        }
        AssessTracker.push(player);
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        WATCHING.remove(event.getEntity().getUUID());
    }

    private static void push(ServerPlayer player) {
        double viewerEp = EnergyHelper.getMaxEP((LivingEntity)player);
        List around = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(64.0), entity -> entity != player && entity.isAlive());
        ArrayList<Integer> ids = new ArrayList<Integer>();
        ArrayList<Float> eps = new ArrayList<Float>();
        double strongest = 0.0;
        around.sort((a, b) -> Double.compare(EnergyHelper.getMaxEP((LivingEntity)b), EnergyHelper.getMaxEP((LivingEntity)a)));
        for (LivingEntity entity2 : around) {
            double ep = EnergyHelper.getMaxEP((LivingEntity)entity2);
            strongest = Math.max(strongest, ep);
            if (ids.size() >= 48) continue;
            ids.add(entity2.getId());
            eps.add(Float.valueOf((float)ep));
        }
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new AssessAuraPayload(viewerEp, List.copyOf(ids), List.copyOf(eps)), (CustomPacketPayload[])new CustomPacketPayload[0]);
        AssessTracker.nausea(player, viewerEp, strongest);
    }

    private static void nausea(ServerPlayer player, double viewerEp, double strongest) {
        double ratio = (Double)GreatSageCommonConfig.ASSESS_NAUSEA_RATIO.get();
        if (viewerEp <= 0.0 || strongest < viewerEp * ratio) {
            return;
        }
        int amplifier = strongest > viewerEp * ratio * 4.0 ? 3 : 2;
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, amplifier, true, false, false));
    }
}

