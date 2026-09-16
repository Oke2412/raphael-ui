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

public record AssessRequestPayload(int entityId) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<AssessRequestPayload> TYPE = new CustomPacketPayload.Type(GreatSage.id("assess_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssessRequestPayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.VAR_INT, AssessRequestPayload::entityId, AssessRequestPayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

