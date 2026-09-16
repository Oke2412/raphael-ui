/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.github.manasmods.manascore.skill.api.ManasSkillInstance
 *  io.github.manasmods.manascore.skill.api.SkillAPI
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.neoforge.event.entity.living.LivingDeathEvent
 *  net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerLoggedOutEvent
 *  net.neoforged.neoforge.event.tick.PlayerTickEvent$Post
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package dev.dedek.greatsage.server;

import dev.dedek.greatsage.network.VoiceReplyPayload;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="greatsage")
public final class BattleMode {
    private static final int PERIOD = 10;
    private static final int RECAST_FLOOR = 100;
    private static final Map<UUID, Guard> ACTIVE = new HashMap<UUID, Guard>();

    private BattleMode() {
    }

    public static int set(ServerPlayer player, boolean on, String raw) {
        if (!on) {
            Guard guard = ACTIVE.remove(player.getUUID());
            if (guard == null) {
                return 0;
            }
            int dropped = 0;
            for (ResourceLocation id : guard.skills()) {
                ManasSkillInstance instance = BattleMode.instance(player, id);
                if (instance == null || !instance.isToggled() || !BattleMode.toggle(player, instance, false)) continue;
                ++dropped;
            }
            return dropped;
        }
        ArrayList<ResourceLocation> wanted = new ArrayList<ResourceLocation>();
        for (String entry : raw.split(",")) {
            ResourceLocation id = ResourceLocation.tryParse((String)entry.trim());
            if (id == null || BattleMode.instance(player, id) == null || wanted.contains(id)) continue;
            wanted.add(id);
        }
        if (wanted.isEmpty()) {
            ACTIVE.remove(player.getUUID());
            return 0;
        }
        Guard guard = new Guard(wanted);
        ACTIVE.put(player.getUUID(), guard);
        return BattleMode.raise(player, guard);
    }

    public static boolean active(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    private static int raise(ServerPlayer player, Guard guard) {
        int held = 0;
        for (ResourceLocation id : guard.skills()) {
            ManasSkillInstance instance = BattleMode.instance(player, id);
            if (instance == null) continue;
            if (instance.canBeToggled((LivingEntity)player)) {
                if (!instance.isToggled() && !BattleMode.toggle(player, instance, true)) continue;
                ++held;
                continue;
            }
            if (guard.suspended().contains(id)) continue;
            Integer last = guard.lastCast().get(id);
            if (last != null && player.tickCount - last < 100) {
                ++held;
                continue;
            }
            if (!BattleMode.cast(player, instance)) continue;
            guard.lastCast().put(id, player.tickCount);
            ++held;
            if (instance.getCoolDown(0) > 0) continue;
            guard.suspended().add(id);
            String label = instance.getSkill() == null ? id.getPath() : instance.getSkill().getName().getString();
            player.displayClientMessage((Component)Component.translatable((String)"greatsage.battle.suspended", (Object[])new Object[]{label}), false);
            PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new VoiceReplyPayload(List.of("voice.hi", "reply.iji_fuka"), "greatsage.battle.suspended", List.of(label)), (CustomPacketPayload[])new CustomPacketPayload[0]);
        }
        return held;
    }

    private static boolean cast(ServerPlayer player, ManasSkillInstance instance) {
        try {
            if (instance.onCoolDown(0) || !instance.canActivateSkill((LivingEntity)player, 0)) {
                return false;
            }
            instance.onPressed((LivingEntity)player, 0, 0);
            instance.onRelease((LivingEntity)player, 0, 0, 0);
            SkillAPI.getSkillsFrom((LivingEntity)player).updateSkill(instance, true);
            return true;
        }
        catch (Throwable failure) {
            return false;
        }
    }

    private static boolean toggle(ServerPlayer player, ManasSkillInstance instance, boolean on) {
        try {
            if (!instance.canBeToggled((LivingEntity)player) || instance.isToggled() == on) {
                return false;
            }
            instance.setToggled(on);
            if (on) {
                instance.onToggleOn((LivingEntity)player);
            } else {
                instance.onToggleOff((LivingEntity)player);
            }
            SkillAPI.getSkillsFrom((LivingEntity)player).updateSkill(instance, true);
            return true;
        }
        catch (Throwable failure) {
            return false;
        }
    }

    private static ManasSkillInstance instance(ServerPlayer player, ResourceLocation id) {
        return SkillAPI.getSkillsFrom((LivingEntity)player).getSkill(id).orElse(null);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        ServerPlayer player;
        block5: {
            block4: {
                Player player2 = event.getEntity();
                if (!(player2 instanceof ServerPlayer)) break block4;
                player = (ServerPlayer)player2;
                if (player.tickCount % 10 == 0 && player.isAlive()) break block5;
            }
            return;
        }
        Guard guard = ACTIVE.get(player.getUUID());
        if (guard != null) {
            BattleMode.raise(player, guard);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer)livingEntity;
            ACTIVE.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
    }

    private record Guard(List<ResourceLocation> skills, Set<ResourceLocation> suspended, Map<ResourceLocation, Integer> lastCast) {
        Guard(List<ResourceLocation> skills) {
            this(skills, new HashSet<ResourceLocation>(), new HashMap<ResourceLocation, Integer>());
        }
    }
}

