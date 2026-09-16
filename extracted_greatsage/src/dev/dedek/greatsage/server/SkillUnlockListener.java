/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.architectury.event.EventResult
 *  io.github.manasmods.manascore.skill.api.ManasSkill
 *  io.github.manasmods.manascore.skill.api.ManasSkillInstance
 *  io.github.manasmods.manascore.skill.api.SkillEvents
 *  io.github.manasmods.tensura.ability.TensuraSkill
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.LivingEntity
 */
package dev.dedek.greatsage.server;

import dev.architectury.event.EventResult;
import dev.dedek.greatsage.server.SkillAnnouncer;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillEvents;
import io.github.manasmods.tensura.ability.TensuraSkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class SkillUnlockListener {
    private SkillUnlockListener() {
    }

    public static void register() {
        SkillEvents.UNLOCK_SKILL.register((instance, entity, message) -> {
            ServerPlayer player;
            if (entity instanceof ServerPlayer && SkillUnlockListener.acquired(instance, player = (ServerPlayer)entity)) {
                SkillAnnouncer.send(player, instance.getSkill());
            }
            return EventResult.pass();
        });
    }

    private static boolean acquired(ManasSkillInstance instance, ServerPlayer player) {
        ManasSkill manasSkill = instance.getSkill();
        if (!(manasSkill instanceof TensuraSkill)) {
            return true;
        }
        TensuraSkill skill = (TensuraSkill)manasSkill;
        try {
            int threshold = skill.getAcquirementMastery((LivingEntity)player);
            return threshold <= 0 || instance.getMastery() != (double)threshold;
        }
        catch (RuntimeException failure) {
            return true;
        }
    }
}

