/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.ComponentSerialization
 *  net.minecraft.network.codec.ByteBufCodecs
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
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SkillAnnouncePayload(ResourceLocation skillId, String tier, Component displayName) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<SkillAnnouncePayload> TYPE = new CustomPacketPayload.Type(GreatSage.id("skill_announce"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkillAnnouncePayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)ResourceLocation.STREAM_CODEC, SkillAnnouncePayload::skillId, (StreamCodec)ByteBufCodecs.STRING_UTF8, SkillAnnouncePayload::tier, (StreamCodec)ComponentSerialization.STREAM_CODEC, SkillAnnouncePayload::displayName, SkillAnnouncePayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

