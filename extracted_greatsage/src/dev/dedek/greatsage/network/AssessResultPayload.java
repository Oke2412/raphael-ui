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

public record AssessResultPayload(String entityType, String name, String race, double ep, float maxHealth, List<String> skills) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<AssessResultPayload> TYPE = new CustomPacketPayload.Type(GreatSage.id("assess_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssessResultPayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.STRING_UTF8, AssessResultPayload::entityType, (StreamCodec)ByteBufCodecs.STRING_UTF8, AssessResultPayload::name, (StreamCodec)ByteBufCodecs.STRING_UTF8, AssessResultPayload::race, (StreamCodec)ByteBufCodecs.DOUBLE, AssessResultPayload::ep, (StreamCodec)ByteBufCodecs.FLOAT, AssessResultPayload::maxHealth, (StreamCodec)ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), AssessResultPayload::skills, AssessResultPayload::new);

    public boolean failed() {
        return this.ep < 0.0;
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

