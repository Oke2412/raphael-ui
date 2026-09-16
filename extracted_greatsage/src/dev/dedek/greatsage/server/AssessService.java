/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.github.manasmods.manascore.race.api.ManasRaceInstance
 *  io.github.manasmods.manascore.race.api.RaceAPI
 *  io.github.manasmods.manascore.skill.api.ManasSkill
 *  io.github.manasmods.manascore.skill.api.SkillAPI
 *  io.github.manasmods.tensura.ability.skill.Skill
 *  io.github.manasmods.tensura.util.EnergyHelper
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.effect.MobEffects
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package dev.dedek.greatsage.server;

import dev.dedek.greatsage.GreatSageCommonConfig;
import dev.dedek.greatsage.network.AssessResultPayload;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.lang.invoke.CallSite;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AssessService {
    private static final int MAX_SKILLS = 40;
    private static final List<String> RANK = List.of("ultimate", "unique", "extra", "intrinsic", "common", "resistance");

    private AssessService() {
    }

    public static void assess(ServerPlayer player, int entityId) {
        Entity entity = player.level().getEntity(entityId);
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        LivingEntity target = (LivingEntity)entity;
        double reach = (Double)GreatSageCommonConfig.ASSESS_RANGE.get();
        if (player.distanceToSqr((Entity)target) > reach * reach) {
            return;
        }
        double playerEp = EnergyHelper.getMaxEP((LivingEntity)player);
        double targetEp = EnergyHelper.getMaxEP((LivingEntity)target);
        double ceiling = (Double)GreatSageCommonConfig.ASSESS_EP_RATIO.get();
        if (playerEp > 0.0 && targetEp > playerEp * ceiling) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 300, 3, true, false, false));
            PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new AssessResultPayload("", target.getName().getString(), "", -1.0, 0.0f, List.of()), (CustomPacketPayload[])new CustomPacketPayload[0]);
            return;
        }
        String race = RaceAPI.getRaceFrom((LivingEntity)target).getRace().map(ManasRaceInstance::getRace).map(value -> value.getName().getString()).orElse("");
        ArrayList<Skill> found = new ArrayList<Skill>();
        for (Object instance : SkillAPI.getSkillsFrom((LivingEntity)target).getLearnedSkills()) {
            Skill skill;
            ManasSkill manasSkill = instance.getSkill();
            if (!(manasSkill instanceof Skill) || (skill = (Skill)manasSkill).getType() == null || "resistance".equalsIgnoreCase(skill.getType().getSerializedName()) && !instance.isMastered(target)) continue;
            found.add(skill);
        }
        found.sort((a, b) -> Integer.compare(AssessService.rankOf(a), AssessService.rankOf(b)));
        ArrayList<CallSite> skills = new ArrayList<CallSite>();
        for (Skill skill : found) {
            skills.add((CallSite)((Object)(skill.getType().getSerializedName().toLowerCase(Locale.ROOT) + "|" + skill.getName().getString())));
            if (skills.size() < 40) continue;
            break;
        }
        String type = BuiltInRegistries.ENTITY_TYPE.getKey((Object)target.getType()).toString();
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new AssessResultPayload(type, target.getName().getString(), race, targetEp, target.getMaxHealth(), List.copyOf(skills)), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    private static int rankOf(Skill skill) {
        int index = RANK.indexOf(skill.getType().getSerializedName().toLowerCase(Locale.ROOT));
        return index < 0 ? RANK.size() : index;
    }
}

