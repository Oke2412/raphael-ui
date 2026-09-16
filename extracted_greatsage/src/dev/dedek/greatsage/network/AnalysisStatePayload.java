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

public record AnalysisStatePayload(String skillId, int ticksLeft, int ticksTotal, String voiceKey, String completed) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<AnalysisStatePayload> TYPE = new CustomPacketPayload.Type(GreatSage.id("analysis_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AnalysisStatePayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.STRING_UTF8, AnalysisStatePayload::skillId, (StreamCodec)ByteBufCodecs.VAR_INT, AnalysisStatePayload::ticksLeft, (StreamCodec)ByteBufCodecs.VAR_INT, AnalysisStatePayload::ticksTotal, (StreamCodec)ByteBufCodecs.STRING_UTF8, AnalysisStatePayload::voiceKey, (StreamCodec)ByteBufCodecs.STRING_UTF8, AnalysisStatePayload::completed, AnalysisStatePayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

