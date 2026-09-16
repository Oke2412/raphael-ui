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
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AssessAuraPayload(double viewerEp, List<Integer> ids, List<Float> eps) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<AssessAuraPayload> TYPE = new CustomPacketPayload.Type(GreatSage.id("assess_aura"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssessAuraPayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.DOUBLE, AssessAuraPayload::viewerEp, (StreamCodec)ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), AssessAuraPayload::ids, (StreamCodec)ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list()), AssessAuraPayload::eps, AssessAuraPayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

