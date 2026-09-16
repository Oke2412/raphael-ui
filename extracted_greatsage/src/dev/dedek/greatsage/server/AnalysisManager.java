/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.architectury.event.EventResult
 *  io.github.manasmods.manascore.skill.api.ManasSkill
 *  io.github.manasmods.manascore.skill.api.ManasSkillInstance
 *  io.github.manasmods.manascore.skill.api.SkillAPI
 *  io.github.manasmods.manascore.skill.api.SkillEvents
 *  io.github.manasmods.tensura.ability.skill.Skill
 *  io.github.manasmods.tensura.storage.TensuraStorages
 *  io.github.manasmods.tensura.storage.ep.IExistence
 *  javax.annotation.Nullable
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.LivingEntity
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package dev.dedek.greatsage.server;

import dev.architectury.event.EventResult;
import dev.dedek.greatsage.GreatSageCommonConfig;
import dev.dedek.greatsage.network.AnalysisStatePayload;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.SkillEvents;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AnalysisManager {
    private static final Logger LOGGER = LoggerFactory.getLogger((String)"greatsage");
    private static final String ROOT = "greatsage_analysis";
    private static final String SKILL = "skill";
    private static final String LEFT = "left";
    private static final String TOTAL = "total";

    private AnalysisManager() {
    }

    public static void register() {
        SkillEvents.ACTIVATE_SKILL.register((instance, entity, holdTicks, keyNumber) -> {
            if (entity instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer)entity;
                ManasSkillInstance skill = (ManasSkillInstance)instance.get();
                if (skill != null && skill.getSkillId() != null && skill.getSkillId().equals((Object)AnalysisManager.analysedSkill(player))) {
                    return EventResult.interruptFalse();
                }
            }
            return EventResult.pass();
        });
    }

    private static CompoundTag root(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound("PlayerPersisted");
        if (!player.getPersistentData().contains("PlayerPersisted")) {
            player.getPersistentData().put("PlayerPersisted", (Tag)persisted);
        }
        CompoundTag root = persisted.getCompound(ROOT);
        if (!persisted.contains(ROOT)) {
            persisted.put(ROOT, (Tag)root);
        }
        return root;
    }

    public static double progress(ServerPlayer player) {
        CompoundTag root = AnalysisManager.root(player);
        int left = root.getInt(LEFT);
        int total = root.getInt(TOTAL);
        if (left <= 0 || total <= 0) {
            return 0.0;
        }
        return 1.0 - (double)left / (double)total;
    }

    @Nullable
    public static ResourceLocation analysedSkill(ServerPlayer player) {
        CompoundTag root = AnalysisManager.root(player);
        if (root.getInt(LEFT) <= 0) {
            return null;
        }
        String id = root.getString(SKILL);
        return id.isEmpty() ? null : ResourceLocation.tryParse((String)id);
    }

    public static void start(ServerPlayer player, String rawId) {
        Skill skill;
        ManasSkill manasSkill;
        if (AnalysisManager.analysedSkill(player) != null) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse((String)rawId.trim());
        if (id == null) {
            return;
        }
        ManasSkillInstance instance = SkillAPI.getSkillsFrom((LivingEntity)player).getSkill(id).orElse(null);
        if (instance == null || instance.isMastered((LivingEntity)player) || !((manasSkill = instance.getSkill()) instanceof Skill) || (skill = (Skill)manasSkill).getType() == null) {
            return;
        }
        if ("resistance".equalsIgnoreCase(skill.getType().getSerializedName())) {
            player.displayClientMessage((Component)Component.translatable((String)"greatsage.analysis.noResistance"), true);
            return;
        }
        if (instance.isTemporarySkill() || instance.shouldRemove()) {
            player.displayClientMessage((Component)Component.translatable((String)"greatsage.analysis.temporary"), true);
            return;
        }
        double cost = skill.getAcquiringMagiculeCost(instance);
        IExistence existence = TensuraStorages.getExistenceFrom((LivingEntity)player);
        if (cost > 0.0) {
            if (existence.getMagicule() < cost) {
                player.displayClientMessage((Component)Component.translatable((String)"greatsage.analysis.noMagicule", (Object[])new Object[]{Math.round(cost)}), true);
                return;
            }
            existence.setMagicule(existence.getMagicule() - cost);
        }
        int ticks = AnalysisManager.duration(instance);
        CompoundTag root = AnalysisManager.root(player);
        root.putString(SKILL, id.toString());
        root.putInt(LEFT, ticks);
        root.putInt(TOTAL, ticks);
        AnalysisManager.sync(player, "voice.kaiseki_kaishi");
    }

    public static void cancel(ServerPlayer player) {
        if (AnalysisManager.analysedSkill(player) == null) {
            return;
        }
        AnalysisManager.clear(player);
        AnalysisManager.sync(player, "voice.kaiseki_chuudan");
    }

    private static int duration(ManasSkillInstance instance) {
        Skill skill;
        int base = (Integer)GreatSageCommonConfig.ANALYSIS_TICKS.get();
        Object object = instance.getSkill();
        if (!(object instanceof Skill) || (skill = (Skill)object).getType() == null) {
            return base;
        }
        return base * (switch (skill.getType().getSerializedName().toLowerCase(Locale.ROOT)) {
            case "ultimate" -> 8;
            case "unique" -> 5;
            case "extra" -> 3;
            case "intrinsic", "common" -> 2;
            default -> 1;
        });
    }

    public static void tick(ServerPlayer player) {
        ManasSkillInstance instance;
        CompoundTag root = AnalysisManager.root(player);
        int left = root.getInt(LEFT);
        if (left <= 0) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse((String)root.getString(SKILL));
        ManasSkillInstance manasSkillInstance = instance = id == null ? null : (ManasSkillInstance)SkillAPI.getSkillsFrom((LivingEntity)player).getSkill(id).orElse(null);
        if (instance == null) {
            AnalysisManager.clear(player);
            AnalysisManager.sync(player, "voice.kaiseki_chuudan");
            return;
        }
        if (instance.isTemporarySkill() || instance.shouldRemove()) {
            AnalysisManager.clear(player);
            player.displayClientMessage((Component)Component.translatable((String)"greatsage.analysis.temporary"), true);
            AnalysisManager.sync(player, "voice.kaiseki_chuudan");
            return;
        }
        root.putInt(LEFT, --left);
        if (left <= 0) {
            instance.setMastery((double)instance.getMaxMastery());
            try {
                instance.onSkillMastered((LivingEntity)player);
            }
            catch (RuntimeException failure) {
                LOGGER.warn("[greatsage] onSkillMastered a echoue pour {} : {}", (Object)instance.getSkillId(), (Object)failure.toString());
            }
            SkillAPI.getSkillsFrom((LivingEntity)player).updateSkill(instance, true);
            AnalysisManager.clear(player);
            AnalysisManager.sync(player, "voice.kaiseki_kanryou", id.toString());
            return;
        }
        if (left % 20 == 0) {
            AnalysisManager.sync(player, "");
        }
    }

    private static void clear(ServerPlayer player) {
        CompoundTag root = AnalysisManager.root(player);
        root.putString(SKILL, "");
        root.putInt(LEFT, 0);
        root.putInt(TOTAL, 0);
    }

    public static void sync(ServerPlayer player, String voiceKey) {
        AnalysisManager.sync(player, voiceKey, "");
    }

    public static void sync(ServerPlayer player, String voiceKey, String completed) {
        CompoundTag root = AnalysisManager.root(player);
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new AnalysisStatePayload(root.getString(SKILL), root.getInt(LEFT), root.getInt(TOTAL), voiceKey, completed), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }
}

