/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.architectury.registry.registries.RegistrySupplier
 *  io.github.manasmods.manascore.skill.api.ManasSkill
 *  io.github.manasmods.manascore.skill.impl.SkillRegistry
 *  net.minecraft.resources.ResourceLocation
 */
package dev.dedek.greatsage.ability;

import dev.architectury.registry.registries.RegistrySupplier;
import dev.dedek.greatsage.ability.VenuzdonoaSkill;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.impl.SkillRegistry;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;

public final class GreatSageSkills {
    public static final RegistrySupplier<ManasSkill> VENUZDONOA = GreatSageSkills.register("venuzdonoa", VenuzdonoaSkill::new);

    private GreatSageSkills() {
    }

    private static <E extends ManasSkill> RegistrySupplier<E> register(String name, Supplier<E> supplier) {
        return SkillRegistry.SKILLS.register(ResourceLocation.fromNamespaceAndPath((String)"greatsage", (String)name), supplier);
    }

    public static void init() {
    }
}

