/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.github.manasmods.manascore.skill.api.ManasSkill
 *  io.github.manasmods.manascore.skill.api.ManasSkillInstance
 *  io.github.manasmods.manascore.skill.api.SkillAPI
 *  io.github.manasmods.tensura.ability.skill.Skill
 *  io.github.manasmods.tensura.ability.skill.unique.PredatorSkill
 *  io.github.manasmods.tensura.menu.container.SpatialStorageContainer
 *  io.github.manasmods.tensura.storage.TensuraStorages
 *  io.github.manasmods.tensura.storage.chunk.ChunkStorage
 *  io.github.manasmods.tensura.storage.chunk.IChunk
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.tags.FluidTags
 *  net.minecraft.world.effect.MobEffects
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.monster.Enemy
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.chunk.LevelChunk
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerChangedDimensionEvent
 *  net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerLoggedOutEvent
 *  net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerRespawnEvent
 *  net.neoforged.neoforge.event.level.BlockEvent$BreakEvent
 *  net.neoforged.neoforge.event.tick.PlayerTickEvent$Post
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package dev.dedek.greatsage.server;

import dev.dedek.greatsage.GreatSageCommonConfig;
import dev.dedek.greatsage.GreatSageGameRules;
import dev.dedek.greatsage.GreatSageTip;
import dev.dedek.greatsage.network.TipPayload;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.ability.skill.unique.PredatorSkill;
import io.github.manasmods.tensura.menu.container.SpatialStorageContainer;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.chunk.ChunkStorage;
import io.github.manasmods.tensura.storage.chunk.IChunk;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="greatsage")
public final class TipWatcher {
    private static final int PERIOD = 20;
    private static final int ORE_REACH = 3;
    private static final Map<UUID, State> STATES = new HashMap<UUID, State>();
    private static final Map<UUID, Integer> LAST_ANY = new HashMap<UUID, Integer>();

    private TipWatcher() {
    }

    public static boolean fire(ServerPlayer player, GreatSageTip tip) {
        if (!((Boolean)GreatSageCommonConfig.ENABLE_TIPS.get()).booleanValue()) {
            return false;
        }
        if (!GreatSageGameRules.carries((LivingEntity)player)) {
            return false;
        }
        int now = player.tickCount;
        State state = STATES.computeIfAbsent(player.getUUID(), key -> new State());
        int quiet = (Integer)GreatSageCommonConfig.TIP_QUIET_TICKS.get();
        Integer any = LAST_ANY.get(player.getUUID());
        if (any != null && now - any < quiet) {
            return false;
        }
        Integer last = state.lastFired().get((Object)tip);
        if (last != null && now - last < tip.cooldownTicks()) {
            return false;
        }
        int count = state.counts().getOrDefault((Object)tip, 0);
        if (tip.maxTimes() > 0 && count >= tip.maxTimes()) {
            return false;
        }
        state.lastFired().put(tip, now);
        state.counts().put(tip, count + 1);
        LAST_ANY.put(player.getUUID(), now);
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new TipPayload(tip.id()), (CustomPacketPayload[])new CustomPacketPayload[0]);
        return true;
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
        LAST_ANY.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player2 = (ServerPlayer)player;
        BlockPos origin = event.getPos();
        for (BlockPos pos : BlockPos.betweenClosed((BlockPos)origin.offset(-3, -3, -3), (BlockPos)origin.offset(3, 3, 3))) {
            BlockState state = player2.level().getBlockState(pos);
            if (!state.is(BlockTags.DIAMOND_ORES) && !state.is(BlockTags.EMERALD_ORES) && !state.is(BlockTags.GOLD_ORES)) continue;
            if (!TipWatcher.fire(player2, GreatSageTip.KOUSEKI_URA)) break;
            return;
        }
        ItemStack held = player2.getMainHandItem();
        float current = held.getDestroySpeed(event.getState());
        for (ItemStack stack : player2.getInventory().items) {
            if (stack.isEmpty() || !(stack.getDestroySpeed(event.getState()) > current + 1.0f)) continue;
            TipWatcher.fire(player2, GreatSageTip.TEKISHITA_DOUGU);
            break;
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer) {
            ServerPlayer player2 = (ServerPlayer)player;
            TipWatcher.fire(player2, GreatSageTip.SAIKOUSEI);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer) {
            ServerPlayer player2 = (ServerPlayer)player;
            TipWatcher.fire(player2, GreatSageTip.MICHI_RYOUIKI);
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        ServerPlayer player;
        block5: {
            block4: {
                Player player2 = event.getEntity();
                if (!(player2 instanceof ServerPlayer)) break block4;
                player = (ServerPlayer)player2;
                if (player.tickCount % 20 == 0 && player.isAlive()) break block5;
            }
            return;
        }
        if (TipWatcher.survival(player) || TipWatcher.surroundings(player) || TipWatcher.gear(player) || TipWatcher.progress(player)) {
            return;
        }
        TipWatcher.ambience(player);
    }

    private static boolean survival(ServerPlayer player) {
        if (player.isOnFire() && TipWatcher.fire(player, GreatSageTip.NENSHOU_KAKUNIN)) {
            return true;
        }
        if (player.getAirSupply() < 60 && TipWatcher.fire(player, GreatSageTip.SANSO_KINSHOU)) {
            return true;
        }
        if (player.getHealth() < player.getMaxHealth() * 0.3f && TipWatcher.fire(player, GreatSageTip.SEIMEI_TEIKA)) {
            return true;
        }
        if ((player.hasEffect(MobEffects.POISON) || player.hasEffect(MobEffects.WITHER)) && TipWatcher.fire(player, GreatSageTip.DOKUSO_JUNKAN)) {
            return true;
        }
        if (player.fallDistance > 8.0f && !player.onGround() && TipWatcher.fire(player, GreatSageTip.RAKKA_KIKEN)) {
            return true;
        }
        int hostiles = 0;
        LivingEntity behind = null;
        Vec3 look = player.getLookAngle();
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(12.0), candidate -> candidate instanceof Enemy)) {
            Vec3 toward;
            ++hostiles;
            if (behind != null || !(player.distanceTo((Entity)entity) < 6.0f) || !((toward = entity.position().subtract(player.position()).normalize()).dot(look) < -0.4)) continue;
            behind = entity;
        }
        if (behind != null && TipWatcher.fire(player, GreatSageTip.HAIGO_SEKKIN)) {
            return true;
        }
        return hostiles >= 4 && TipWatcher.fire(player, GreatSageTip.TEKISEI_FUKUSUU);
    }

    private static boolean surroundings(ServerPlayer player) {
        ChunkStorage chunk;
        BlockState state;
        BlockPos at = player.blockPosition();
        boolean lava = false;
        boolean water = false;
        for (BlockPos pos : BlockPos.betweenClosed((BlockPos)at.offset(-3, -3, -3), (BlockPos)at.offset(3, 3, 3))) {
            state = player.level().getBlockState(pos);
            if (state.getFluidState().isEmpty()) continue;
            if (state.getFluidState().is(FluidTags.LAVA)) {
                lava = true;
                continue;
            }
            if (!state.getFluidState().is(FluidTags.WATER)) continue;
            water = true;
        }
        if (lava && TipWatcher.fire(player, GreatSageTip.YOUGAN_KINSETSU)) {
            return true;
        }
        if (water && !player.isInWater() && at.getY() < 50 && TipWatcher.fire(player, GreatSageTip.SUIGEN_HAIGO)) {
            return true;
        }
        if (at.getY() < 60 && player.level().getBlockState(at.below(3)).isAir() && !player.level().getBlockState(at.below(1)).isAir() && TipWatcher.fire(player, GreatSageTip.KUUDOU_CHOKKA)) {
            return true;
        }
        if (at.getY() < 40) {
            for (BlockPos pos : BlockPos.betweenClosed((BlockPos)at.offset(-6, -4, -6), (BlockPos)at.offset(6, 4, 6))) {
                state = player.level().getBlockState(pos);
                if (!state.is(BlockTags.DIAMOND_ORES) && !state.is(BlockTags.EMERALD_ORES)) continue;
                if (!TipWatcher.fire(player, GreatSageTip.KOUMYAKU_KENCHI)) break;
                return true;
            }
        }
        if (player.level().getMaxLocalRawBrightness(at) <= 7 && TipWatcher.fire(player, GreatSageTip.KOURYOU_FUSOKU)) {
            return true;
        }
        long day = player.level().getDayTime() % 24000L;
        if (day > 11000L && day < 12400L && TipWatcher.fire(player, GreatSageTip.NICHIBOTSU)) {
            return true;
        }
        if (player.level().isRaining() && TipWatcher.fire(player, GreatSageTip.KISHOU_KYUUHEN)) {
            return true;
        }
        ChunkStorage chunkStorage = TensuraStorages.getChunkFrom((LevelChunk)player.level().getChunkAt(at));
        return chunkStorage instanceof IChunk && (chunk = chunkStorage).getMaxMagicule() > 0.0 && chunk.getMagicule() > chunk.getMaxMagicule() * 0.85 && TipWatcher.fire(player, GreatSageTip.MASO_NOUDO);
    }

    private static boolean gear(ServerPlayer player) {
        boolean full;
        ItemStack held = player.getMainHandItem();
        if (held.isDamageableItem() && (double)held.getDamageValue() > (double)held.getMaxDamage() * 0.9 && TipWatcher.fire(player, GreatSageTip.DOUGU_HASON)) {
            return true;
        }
        boolean bl = full = player.getInventory().getFreeSlot() < 0;
        if (full && TipWatcher.fire(player, GreatSageTip.SHUUNOU_JOUGEN)) {
            return true;
        }
        return PredatorHelper.nearlyFull(player) && TipWatcher.fire(player, GreatSageTip.IBUKURO_WAZUKA);
    }

    private static boolean progress(ServerPlayer player) {
        for (ManasSkillInstance instance : SkillAPI.getSkillsFrom((LivingEntity)player).getLearnedSkills()) {
            int max;
            if (!(instance.getSkill() instanceof Skill) || instance.isMastered((LivingEntity)player) || (max = instance.getMaxMastery()) <= 0 || !(instance.getMastery() > (double)max * 0.9) || !TipWatcher.fire(player, GreatSageTip.SHUUJUKU_JOUGEN)) continue;
            return true;
        }
        for (ManasSkillInstance instance : SkillAPI.getSkillsFrom((LivingEntity)player).getLearnedSkills()) {
            if (!(instance.getSkill() instanceof Skill) || instance.isMastered((LivingEntity)player)) continue;
            return TipWatcher.fire(player, GreatSageTip.MIKAISEKI_SONZAI);
        }
        return false;
    }

    private static void ambience(ServerPlayer player) {
        long day = player.level().getDayTime() % 24000L;
        if (day > 13000L && day < 23000L && TipWatcher.fire(player, GreatSageTip.KYUUSOKU_SUISHOU)) {
            return;
        }
        if (player.tickCount > 108000 && TipWatcher.fire(player, GreatSageTip.RENZOKU_KATSUDOU)) {
            return;
        }
        if (player.getRandom().nextInt(600) == 0) {
            TipWatcher.fire(player, player.getRandom().nextBoolean() ? GreatSageTip.HOUKOKU_NASHI : GreatSageTip.HANDAN_SHIJI);
        }
    }

    public static void onDeath(ServerPlayer player) {
        TipWatcher.fire(player, GreatSageTip.KATSUDOU_TEISHI);
    }

    private record State(Map<GreatSageTip, Integer> lastFired, Map<GreatSageTip, Integer> counts) {
        State() {
            this(new EnumMap<GreatSageTip, Integer>(GreatSageTip.class), new EnumMap<GreatSageTip, Integer>(GreatSageTip.class));
        }
    }

    private static final class PredatorHelper {
        private PredatorHelper() {
        }

        static boolean nearlyFull(ServerPlayer player) {
            for (ManasSkillInstance instance : SkillAPI.getSkillsFrom((LivingEntity)player).getLearnedSkills()) {
                PredatorSkill predator;
                SpatialStorageContainer container;
                ManasSkill manasSkill = instance.getSkill();
                if (!(manasSkill instanceof PredatorSkill) || (container = (predator = (PredatorSkill)manasSkill).getSpatialStorage(instance, (HolderLookup.Provider)player.registryAccess())) == null) continue;
                int free = 0;
                for (int slot = 0; slot < container.getContainerSize(); ++slot) {
                    if (!container.getItem(slot).isEmpty()) continue;
                    ++free;
                }
                return free <= Math.max(1, container.getContainerSize() / 10);
            }
            return false;
        }
    }
}

