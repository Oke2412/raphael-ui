/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.bus.api.IEventBus
 *  net.neoforged.fml.ModContainer
 *  net.neoforged.fml.common.Mod
 *  net.neoforged.fml.config.IConfigSpec
 *  net.neoforged.fml.config.ModConfig$Type
 *  net.neoforged.fml.loading.FMLEnvironment
 */
package dev.dedek.greatsage;

import dev.dedek.greatsage.GreatSageCommonConfig;
import dev.dedek.greatsage.GreatSageConfig;
import dev.dedek.greatsage.GreatSageGameRules;
import dev.dedek.greatsage.GreatSageSounds;
import dev.dedek.greatsage.ability.GreatSageSkills;
import dev.dedek.greatsage.client.GreatSageClientSetup;
import dev.dedek.greatsage.content.GreatSageContent;
import dev.dedek.greatsage.network.GreatSageNetwork;
import dev.dedek.greatsage.server.AnalysisManager;
import dev.dedek.greatsage.server.EvolutionListener;
import dev.dedek.greatsage.server.SkillUnlockListener;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(value="greatsage")
public class GreatSage {
    public static final String MOD_ID = "greatsage";

    public GreatSage(IEventBus modBus, ModContainer container) {
        GreatSageSounds.register(modBus);
        GreatSageContent.register(modBus);
        GreatSageSkills.init();
        GreatSageGameRules.init();
        modBus.addListener(GreatSageNetwork::onRegisterPayloads);
        container.registerConfig(ModConfig.Type.CLIENT, (IConfigSpec)GreatSageConfig.SPEC);
        container.registerConfig(ModConfig.Type.COMMON, (IConfigSpec)GreatSageCommonConfig.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            GreatSageClientSetup.register(modBus);
        }
        SkillUnlockListener.register();
        EvolutionListener.register();
        AnalysisManager.register();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath((String)MOD_ID, (String)path);
    }
}

