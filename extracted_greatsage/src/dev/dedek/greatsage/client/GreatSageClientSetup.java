/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.neoforged.bus.api.IEventBus
 *  net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
 */
package dev.dedek.greatsage.client;

import dev.dedek.greatsage.client.voice.VoiceInstall;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public final class GreatSageClientSetup {
    private GreatSageClientSetup() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(VoiceInstall::ensure));
    }
}

