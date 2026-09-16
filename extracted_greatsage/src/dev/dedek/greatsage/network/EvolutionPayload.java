/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.ComponentSerialization
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 */
package dev.dedek.greatsage.network;

import dev.dedek.greatsage.GreatSage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EvolutionPayload(ResourceLocation fromId, Component fromName, ResourceLocation toId, Component toName) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<EvolutionPayload> TYPE = new CustomPacketPayload.Type(GreatSage.id("evolution"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EvolutionPayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)ResourceLocation.STREAM_CODEC, EvolutionPayload::fromId, (StreamCodec)ComponentSerialization.STREAM_CODEC, EvolutionPayload::fromName, (StreamCodec)ResourceLocation.STREAM_CODEC, EvolutionPayload::toId, (StreamCodec)ComponentSerialization.STREAM_CODEC, EvolutionPayload::toName, EvolutionPayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

