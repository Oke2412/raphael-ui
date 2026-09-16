/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.github.manasmods.manascore.skill.api.ManasSkill
 *  io.github.manasmods.manascore.skill.api.SkillAPI
 *  io.github.manasmods.tensura.ability.skill.Skill
 *  javax.annotation.Nullable
 *  net.minecraft.Util
 *  net.minecraft.client.Minecraft
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.LivingEntity
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package dev.dedek.greatsage.client;

import dev.dedek.greatsage.client.Announcement;
import dev.dedek.greatsage.client.AnnouncementManager;
import dev.dedek.greatsage.client.SageDialogue;
import dev.dedek.greatsage.client.SageMenuScreen;
import dev.dedek.greatsage.network.AnalysisStatePayload;
import dev.dedek.greatsage.network.AnalyzeRequestPayload;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.skill.Skill;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AnalysisClient {
    private static final float MAX_DRIFT_TICKS = 24.0f;
    @Nullable
    private static ResourceLocation skill;
    private static int ticksLeft;
    private static int ticksTotal;
    private static long receivedAt;

    private AnalysisClient() {
    }

    public static void accept(AnalysisStatePayload payload) {
        ResourceLocation done;
        skill = payload.skillId().isEmpty() ? null : ResourceLocation.tryParse((String)payload.skillId());
        ticksLeft = payload.ticksLeft();
        ticksTotal = payload.ticksTotal();
        receivedAt = Util.getMillis();
        if (!payload.completed().isEmpty() && (done = ResourceLocation.tryParse((String)payload.completed())) != null && !(Minecraft.getInstance().screen instanceof SageMenuScreen)) {
            AnnouncementManager.startAnalysis(new Announcement.Analysis(done, AnalysisClient.nameOf(done)));
            return;
        }
        if (!payload.voiceKey().isEmpty()) {
            SageDialogue.say(payload.voiceKey());
        }
    }

    private static Component nameOf(ResourceLocation id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return Component.literal((String)id.getPath());
        }
        return (Component)SkillAPI.getSkillsFrom((LivingEntity)minecraft.player).getSkill(id).map(instance -> {
            MutableComponent mutableComponent;
            ManasSkill patt0$temp = instance.getSkill();
            if (patt0$temp instanceof Skill) {
                Skill skill = (Skill)patt0$temp;
                mutableComponent = skill.getName();
            } else {
                mutableComponent = Component.literal((String)id.getPath());
            }
            return mutableComponent;
        }).orElse(Component.literal((String)id.getPath()));
    }

    public static void clear() {
        skill = null;
        ticksLeft = 0;
        ticksTotal = 0;
    }

    @Nullable
    public static ResourceLocation skill() {
        return skill;
    }

    public static boolean running() {
        return skill != null && ticksLeft > 0;
    }

    public static float progress() {
        if (ticksTotal <= 0) {
            return 0.0f;
        }
        float left = Math.max(0.0f, (float)ticksLeft - AnalysisClient.drift());
        return Math.min(1.0f, 1.0f - left / (float)ticksTotal);
    }

    public static int secondsLeft() {
        return Math.max(0, Math.round(((float)ticksLeft - AnalysisClient.drift()) / 20.0f));
    }

    private static float drift() {
        return Math.min(24.0f, (float)(Util.getMillis() - receivedAt) / 50.0f);
    }

    public static boolean stalled() {
        return AnalysisClient.running() && (float)(Util.getMillis() - receivedAt) > 1200.0f;
    }

    public static void request(ResourceLocation id) {
        PacketDistributor.sendToServer((CustomPacketPayload)new AnalyzeRequestPayload(id.toString(), false), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    public static void requestCancel() {
        PacketDistributor.sendToServer((CustomPacketPayload)new AnalyzeRequestPayload("", true), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }
}

