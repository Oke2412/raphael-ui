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

public record VoiceReplyPayload(List<String> clips, String subtitle, List<String> args) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<VoiceReplyPayload> TYPE = new CustomPacketPayload.Type(GreatSage.id("voice_reply"));
    private static final StreamCodec<RegistryFriendlyByteBuf, List<String>> STRINGS = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).cast();
    public static final StreamCodec<RegistryFriendlyByteBuf, VoiceReplyPayload> STREAM_CODEC = StreamCodec.composite(STRINGS, VoiceReplyPayload::clips, (StreamCodec)ByteBufCodecs.STRING_UTF8, VoiceReplyPayload::subtitle, STRINGS, VoiceReplyPayload::args, VoiceReplyPayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

