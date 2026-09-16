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

public record VoiceQueryPayload(String command, String argument, int targetId) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<VoiceQueryPayload> TYPE = new CustomPacketPayload.Type(GreatSage.id("voice_query"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VoiceQueryPayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.STRING_UTF8, VoiceQueryPayload::command, (StreamCodec)ByteBufCodecs.STRING_UTF8, VoiceQueryPayload::argument, (StreamCodec)ByteBufCodecs.VAR_INT, VoiceQueryPayload::targetId, VoiceQueryPayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

