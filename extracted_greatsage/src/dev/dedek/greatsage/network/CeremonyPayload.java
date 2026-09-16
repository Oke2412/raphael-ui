/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 */
package dev.dedek.greatsage.network;

import dev.dedek.greatsage.GreatSage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CeremonyPayload(String ceremonyId) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<CeremonyPayload> TYPE = new CustomPacketPayload.Type(GreatSage.id("ceremony"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CeremonyPayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.STRING_UTF8, CeremonyPayload::ceremonyId, CeremonyPayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

