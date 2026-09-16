/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.github.manasmods.manascore.skill.api.SkillAPI
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.GameRules
 *  net.minecraft.world.level.GameRules$BooleanValue
 *  net.minecraft.world.level.GameRules$Category
 *  net.minecraft.world.level.GameRules$Key
 *  net.minecraft.world.level.GameRules$Type
 */
package dev.dedek.greatsage;

import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;

public final class GreatSageGameRules {
    public static final ResourceLocation GREAT_SAGE = ResourceLocation.fromNamespaceAndPath((String)"tensura", (String)"great_sage");
    public static final GameRules.Key<GameRules.BooleanValue> NEED_GREAT_SAGE = GameRules.register((String)"needGreatSage", (GameRules.Category)GameRules.Category.PLAYER, (GameRules.Type)GameRules.BooleanValue.create((boolean)false));

    private GreatSageGameRules() {
    }

    public static void init() {
    }

    public static boolean allowed(LivingEntity entity) {
        if (!entity.level().getGameRules().getBoolean(NEED_GREAT_SAGE)) {
            return true;
        }
        return SkillAPI.getSkillsFrom((LivingEntity)entity).getSkill(GREAT_SAGE).isPresent();
    }

    public static boolean carries(LivingEntity entity) {
        return SkillAPI.getSkillsFrom((LivingEntity)entity).getSkill(GREAT_SAGE).isPresent();
    }
}

