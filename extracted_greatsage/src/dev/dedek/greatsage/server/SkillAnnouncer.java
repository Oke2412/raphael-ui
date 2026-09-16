/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.github.manasmods.manascore.skill.api.ManasSkill
 *  io.github.manasmods.tensura.ability.skill.Skill
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.LivingEntity
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package dev.dedek.greatsage.server;

import dev.dedek.greatsage.GreatSageCommonConfig;
import dev.dedek.greatsage.GreatSageGameRules;
import dev.dedek.greatsage.network.SkillAnnouncePayload;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SkillAnnouncer {
    private SkillAnnouncer() {
    }

    public static boolean send(ServerPlayer player, ManasSkill skill) {
        return SkillAnnouncer.send(player, skill, true);
    }

    public static boolean send(ServerPlayer player, ManasSkill skill, boolean respectGrace) {
        Skill tensuraSkill;
        if (respectGrace && player.tickCount < (Integer)GreatSageCommonConfig.JOIN_GRACE_TICKS.get()) {
            return false;
        }
        if (!GreatSageGameRules.allowed((LivingEntity)player)) {
            return false;
        }
        if (!(skill instanceof Skill) || (tensuraSkill = (Skill)skill).getType() == null) {
            return false;
        }
        ResourceLocation id = tensuraSkill.getRegistryName();
        if (id == null) {
            return false;
        }
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new SkillAnnouncePayload(id, tensuraSkill.getType().getSerializedName(), (Component)tensuraSkill.getName()), (CustomPacketPayload[])new CustomPacketPayload[0]);
        return true;
    }
}

