/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.github.manasmods.manascore.skill.api.ManasSkillInstance
 *  io.github.manasmods.tensura.ability.skill.Skill
 *  io.github.manasmods.tensura.ability.skill.Skill$SkillType
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 */
package dev.dedek.greatsage.ability;

import dev.dedek.greatsage.GreatSage;
import dev.dedek.greatsage.content.GreatSageContent;
import dev.dedek.greatsage.content.VenuzdonoaEntity;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class VenuzdonoaSkill
extends Skill {
    private static final double SUMMON_DISTANCE = 2.6;
    private static final int GROUND_SEARCH = 8;
    private static final int COOLDOWN_TICKS = 200;

    public VenuzdonoaSkill() {
        super(Skill.SkillType.UNIQUE);
    }

    @Nullable
    public ResourceLocation getSkillIcon() {
        return GreatSage.id("textures/skill/venuzdonoa.png");
    }

    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        Level level = entity.level();
        if (level.isClientSide()) {
            return;
        }
        Vec3 forward = entity.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
        if (forward.lengthSqr() < 1.0E-4) {
            forward = new Vec3(0.0, 0.0, 1.0);
        }
        Vec3 target = entity.position().add(forward.scale(2.6));
        VenuzdonoaEntity sword = new VenuzdonoaEntity((EntityType<? extends VenuzdonoaEntity>)((EntityType)GreatSageContent.VENUZDONOA.get()), level);
        sword.moveTo(target.x, VenuzdonoaSkill.groundBelow(level, target, entity.getY()), target.z, entity.getYRot() + 90.0f, 0.0f);
        level.addFreshEntity((Entity)sword);
        instance.setCoolDown(200, mode);
        instance.markDirty();
    }

    private static double groundBelow(Level level, Vec3 target, double fallback) {
        BlockPos probe = BlockPos.containing((double)target.x, (double)(fallback + 1.0), (double)target.z);
        for (int i = 0; i < 8; ++i) {
            if (!level.getBlockState(probe).isAir()) {
                return (double)probe.getY() + 1.0;
            }
            probe = probe.below();
        }
        return fallback;
    }
}

