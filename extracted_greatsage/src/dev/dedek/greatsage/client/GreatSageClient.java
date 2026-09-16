/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.LayeredDraw$Layer
 *  net.minecraft.world.entity.EntityType
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent$LoggingOut
 *  net.neoforged.neoforge.client.event.ClientTickEvent$Post
 *  net.neoforged.neoforge.client.event.EntityRenderersEvent$RegisterRenderers
 *  net.neoforged.neoforge.client.event.RegisterGuiLayersEvent
 *  net.neoforged.neoforge.client.event.ScreenEvent$Render$Post
 *  net.neoforged.neoforge.client.gui.VanillaGuiLayers
 *  net.neoforged.neoforge.event.tick.PlayerTickEvent$Post
 */
package dev.dedek.greatsage.client;

import dev.dedek.greatsage.GreatSage;
import dev.dedek.greatsage.client.AnalysisClient;
import dev.dedek.greatsage.client.AnnouncementManager;
import dev.dedek.greatsage.client.AnnouncementOverlay;
import dev.dedek.greatsage.client.AssessMode;
import dev.dedek.greatsage.client.SageArchive;
import dev.dedek.greatsage.client.SageDialogue;
import dev.dedek.greatsage.client.SageTips;
import dev.dedek.greatsage.client.VenuzdonoaRenderer;
import dev.dedek.greatsage.client.VoiceInstallOverlay;
import dev.dedek.greatsage.client.Waypoints;
import dev.dedek.greatsage.client.voice.VoiceInput;
import dev.dedek.greatsage.client.voice.VoiceInstall;
import dev.dedek.greatsage.client.voice.VoiceSpeech;
import dev.dedek.greatsage.content.GreatSageContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid="greatsage", value={Dist.CLIENT})
public final class GreatSageClient {
    private GreatSageClient() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer((EntityType)GreatSageContent.VENUZDONOA.get(), VenuzdonoaRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CHAT, GreatSage.id("skill_announce"), (LayeredDraw.Layer)new AnnouncementOverlay());
        event.registerAbove(VanillaGuiLayers.CHAT, GreatSage.id("voice_install"), (LayeredDraw.Layer)new VoiceInstallOverlay());
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            return;
        }
        VoiceInstallOverlay.draw(event.getGuiGraphics(), minecraft, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            AnnouncementManager.update();
            VoiceInput.tick();
            Waypoints.tick(Minecraft.getInstance());
            SageDialogue.update();
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        VoiceInstall.ensure();
        VoiceInstall.drain(Minecraft.getInstance());
        if (VoiceInput.enabled()) {
            VoiceSpeech.status();
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        AnnouncementManager.clear();
        AnalysisClient.clear();
        AssessMode.reset();
        SageArchive.unload();
        VoiceInput.stop();
        SageTips.clear();
        SageDialogue.clear();
    }
}

