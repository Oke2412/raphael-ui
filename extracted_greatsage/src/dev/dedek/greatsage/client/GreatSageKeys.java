/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.InputConstants$Type
 *  io.github.manasmods.manascore.skill.api.SkillAPI
 *  io.github.manasmods.manascore.skill.impl.SkillStorage
 *  net.minecraft.client.KeyMapping
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.LivingEntity
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.neoforge.client.event.ClientTickEvent$Post
 *  net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
 */
package dev.dedek.greatsage.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.dedek.greatsage.GreatSageConfig;
import dev.dedek.greatsage.GreatSageGameRules;
import dev.dedek.greatsage.client.AnnouncementManager;
import dev.dedek.greatsage.client.AssessMode;
import dev.dedek.greatsage.client.SageDialogue;
import dev.dedek.greatsage.client.SageMenuScreen;
import dev.dedek.greatsage.client.SageTips;
import dev.dedek.greatsage.client.voice.VoiceInput;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.impl.SkillStorage;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid="greatsage", value={Dist.CLIENT})
public final class GreatSageKeys {
    static final KeyMapping OPEN_MENU = new KeyMapping("key.greatsage.menu", InputConstants.Type.KEYSYM, 71, "key.categories.greatsage");
    static final KeyMapping ASSESS = new KeyMapping("key.greatsage.assess", InputConstants.Type.KEYSYM, 86, "key.categories.greatsage");
    static final KeyMapping SCAN = new KeyMapping("key.greatsage.scan", InputConstants.Type.KEYSYM, 66, "key.categories.greatsage");
    private static boolean scanHeld;
    static final KeyMapping VOICE;
    static final KeyMapping PANIC;
    private static boolean conflictWarned;

    private GreatSageKeys() {
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(PANIC);
        event.register(VOICE);
        event.register(OPEN_MENU);
        event.register(ASSESS);
        event.register(SCAN);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        while (ASSESS.consumeClick()) {
            if (minecraft.screen != null || !GreatSageKeys.hasSage(player)) continue;
            AssessMode.toggle();
        }
        if (AssessMode.active()) {
            boolean down;
            boolean bl = down = SCAN.isDown() && minecraft.screen == null;
            if (down && !scanHeld) {
                AssessMode.press();
            } else if (!down && scanHeld) {
                AssessMode.release();
            }
            scanHeld = down;
            AssessMode.tick();
        } else if (scanHeld) {
            scanHeld = false;
        }
        if (GreatSageKeys.hasSage(player)) {
            if (VOICE.isDown()) {
                VoiceInput.onKeyDown();
            } else {
                VoiceInput.onKeyUp();
            }
        }
        while (PANIC.consumeClick()) {
            AnnouncementManager.clear();
            SageDialogue.clear();
            SageTips.clear();
            VoiceInput.stop();
        }
        while (VOICE.consumeClick()) {
        }
        while (OPEN_MENU.consumeClick()) {
            if (minecraft.screen != null) continue;
            GreatSageKeys.warnIfConflicting(minecraft);
            if (GreatSageKeys.hasSage(player)) {
                minecraft.setScreen((Screen)new SageMenuScreen());
                continue;
            }
            player.displayClientMessage((Component)Component.translatable((String)"greatsage.menu.denied"), true);
        }
    }

    private static void warnIfConflicting(Minecraft minecraft) {
        if (conflictWarned || minecraft.player == null) {
            return;
        }
        for (KeyMapping other : minecraft.options.keyMappings) {
            if (other == OPEN_MENU || other.isUnbound() || !other.same(OPEN_MENU)) continue;
            conflictWarned = true;
            minecraft.player.displayClientMessage((Component)Component.translatable((String)"greatsage.key.conflict", (Object[])new Object[]{OPEN_MENU.getTranslatedKeyMessage(), Component.translatable((String)other.getName())}), false);
            return;
        }
    }

    private static boolean hasSage(LocalPlayer player) {
        if (!player.level().getGameRules().getBoolean(GreatSageGameRules.NEED_GREAT_SAGE)) {
            return true;
        }
        SkillStorage storage = SkillAPI.getSkillsFrom((LivingEntity)player);
        for (String id : (List)GreatSageConfig.MENU_SKILLS.get()) {
            ResourceLocation key = ResourceLocation.tryParse((String)id.trim());
            if (key == null || !storage.getSkill(key).isPresent()) continue;
            return true;
        }
        return false;
    }

    static {
        VOICE = new KeyMapping("key.greatsage.voice", InputConstants.Type.KEYSYM, -1, "key.categories.greatsage");
        PANIC = new KeyMapping("key.greatsage.panic", InputConstants.Type.KEYSYM, 261, "key.categories.greatsage");
    }
}

