/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvent
 *  net.neoforged.bus.api.IEventBus
 *  net.neoforged.neoforge.registries.DeferredHolder
 *  net.neoforged.neoforge.registries.DeferredRegister
 */
package dev.dedek.greatsage;

import dev.dedek.greatsage.GreatSage;
import dev.dedek.greatsage.SoundManifest;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GreatSageSounds {
    private static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create((ResourceKey)Registries.SOUND_EVENT, (String)"greatsage");
    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> BY_KEY = new HashMap<String, DeferredHolder<SoundEvent, SoundEvent>>();

    private GreatSageSounds() {
    }

    public static void register(IEventBus modBus) {
        REGISTRY.register(modBus);
    }

    @Nullable
    public static SoundEvent get(String key) {
        DeferredHolder<SoundEvent, SoundEvent> holder = BY_KEY.get(key);
        return holder == null ? null : (SoundEvent)holder.value();
    }

    static {
        for (String key : SoundManifest.keys()) {
            BY_KEY.put(key, (DeferredHolder<SoundEvent, SoundEvent>)REGISTRY.register(key, () -> SoundEvent.createVariableRangeEvent((ResourceLocation)GreatSage.id(key))));
        }
    }
}

