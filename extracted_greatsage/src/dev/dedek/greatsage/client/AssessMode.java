/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.Int2DoubleMap
 *  it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap
 *  javax.annotation.Nullable
 *  net.minecraft.Util
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.HitResult
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package dev.dedek.greatsage.client;

import dev.dedek.greatsage.GreatSageCommonConfig;
import dev.dedek.greatsage.GreatSageConfig;
import dev.dedek.greatsage.client.SageArchive;
import dev.dedek.greatsage.client.SageDialogue;
import dev.dedek.greatsage.network.AssessAuraPayload;
import dev.dedek.greatsage.network.AssessModePayload;
import dev.dedek.greatsage.network.AssessRequestPayload;
import dev.dedek.greatsage.network.AssessResultPayload;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AssessMode {
    private static final double SCAN_DISTANCE = 32.0;
    private static final double MAX_DISTANCE = 64.0;
    private static final double AIM_REACH = 64.0;
    private static final float SCAN_PER_TICK = 0.014f;
    private static final float DECAY_PER_TICK = 0.035f;
    private static boolean active;
    private static boolean hudWasHidden;
    @Nullable
    private static LivingEntity target;
    private static float scan;
    private static boolean holding;
    private static boolean awaitingResult;
    @Nullable
    private static AssessResultPayload result;
    private static long resultAt;
    private static String obstacle;
    private static final long GLITCH_MS = 900L;
    private static long glitchUntil;
    private static double viewerEp;
    private static final Int2DoubleMap AURA;

    private AssessMode() {
    }

    public static boolean active() {
        return active;
    }

    public static void toggle() {
        if (active) {
            AssessMode.close();
        } else {
            AssessMode.open();
        }
    }

    private static void open() {
        active = true;
        AssessMode.clearTarget();
        Minecraft minecraft = Minecraft.getInstance();
        hudWasHidden = minecraft.options.hideGui;
        minecraft.options.hideGui = true;
        PacketDistributor.sendToServer((CustomPacketPayload)new AssessModePayload(true), (CustomPacketPayload[])new CustomPacketPayload[0]);
        SageDialogue.say("voice.kaiseki_kantei");
    }

    public static boolean glitching() {
        return Util.getMillis() < glitchUntil;
    }

    public static float glitch() {
        long left = glitchUntil - Util.getMillis();
        return left <= 0L ? 0.0f : (float)left / 900.0f;
    }

    public static void close() {
        AssessMode.close(false);
    }

    private static void close(boolean brutal) {
        if (!active) {
            return;
        }
        active = false;
        AssessMode.clearTarget();
        Minecraft.getInstance().options.hideGui = hudWasHidden;
        PacketDistributor.sendToServer((CustomPacketPayload)new AssessModePayload(false), (CustomPacketPayload[])new CustomPacketPayload[0]);
        if (!brutal) {
            SageDialogue.say("voice.kaiseki_shuuryou");
        }
    }

    public static void reset() {
        if (active) {
            Minecraft.getInstance().options.hideGui = hudWasHidden;
        }
        active = false;
        AssessMode.clearTarget();
        AURA.clear();
        viewerEp = 0.0;
    }

    private static void clearTarget() {
        target = null;
        scan = 0.0f;
        holding = false;
        awaitingResult = false;
        result = null;
        obstacle = "";
    }

    public static void press() {
        if (!active) {
            return;
        }
        holding = true;
        if (target != null && target.isAlive() && result == null && !awaitingResult) {
            return;
        }
        LivingEntity aimed = AssessMode.aimedAt();
        if (aimed == null) {
            return;
        }
        AssessMode.lock(aimed);
    }

    private static void lock(LivingEntity aimed) {
        target = aimed;
        scan = 0.0f;
        result = null;
        awaitingResult = false;
        obstacle = "";
        double ep = AssessMode.epOf(aimed.getId());
        double mine = AssessMode.playerEp();
        if (ep >= 0.0 && mine > 0.0 && ep > mine * (Double)GreatSageCommonConfig.ASSESS_EP_RATIO.get()) {
            awaitingResult = true;
            PacketDistributor.sendToServer((CustomPacketPayload)new AssessRequestPayload(aimed.getId()), (CustomPacketPayload[])new CustomPacketPayload[0]);
            return;
        }
        SageDialogue.say("voice.taishou_kakunin");
    }

    public static void release() {
        holding = false;
    }

    @Nullable
    private static LivingEntity aimedAt() {
        LivingEntity living;
        Minecraft minecraft = Minecraft.getInstance();
        Entity camera = minecraft.getCameraEntity();
        if (camera == null) {
            return null;
        }
        HitResult hit = camera.pick(64.0, 0.0f, false);
        Entity found = null;
        double best = Double.MAX_VALUE;
        double limit = hit == null ? 4096.0 : hit.getLocation().distanceToSqr(camera.getEyePosition());
        Vec3 look = camera.getViewVector(1.0f);
        Vec3 eye = camera.getEyePosition();
        Vec3 reach = eye.add(look.scale(64.0));
        AABB box = camera.getBoundingBox().expandTowards(look.scale(64.0)).inflate(1.0);
        for (Entity entity : minecraft.level.getEntities(camera, box, candidate -> candidate instanceof LivingEntity && candidate.isPickable())) {
            double distance;
            Optional clip = entity.getBoundingBox().inflate(0.35).clip(eye, reach);
            if (clip.isEmpty() || !((distance = eye.distanceToSqr((Vec3)clip.get())) < best) || !(distance <= limit)) continue;
            best = distance;
            found = entity;
        }
        return found instanceof LivingEntity ? (living = (LivingEntity)found) : null;
    }

    public static void tick() {
        if (!active) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            AssessMode.reset();
            return;
        }
        if (target == null) {
            return;
        }
        if (!target.isAlive() || (double)player.distanceTo((Entity)target) > 64.0) {
            AssessMode.clearTarget();
            return;
        }
        if (result != null) {
            return;
        }
        double distance = player.distanceTo((Entity)target);
        obstacle = !holding ? "hold" : (distance > 32.0 ? "far" : "");
        if (obstacle.isEmpty()) {
            if (scan <= 0.0f) {
                SageDialogue.say("voice.kaiseki_chuu");
            }
            if ((scan = Math.min(1.0f, scan + 0.014f)) >= 1.0f && !awaitingResult) {
                awaitingResult = true;
                PacketDistributor.sendToServer((CustomPacketPayload)new AssessRequestPayload(target.getId()), (CustomPacketPayload[])new CustomPacketPayload[0]);
            }
        } else if (!awaitingResult) {
            scan = Math.max(0.0f, scan - 0.035f);
        }
    }

    public static void accept(AssessResultPayload payload) {
        awaitingResult = false;
        if (payload.failed()) {
            SageDialogue.say("voice.kaiseki_shippai");
            glitchUntil = Util.getMillis() + 900L;
            AssessMode.close(true);
            return;
        }
        result = payload;
        resultAt = Util.getMillis();
        SageArchive.record(payload);
        SageDialogue.say("voice.taishou_kanryou");
        if (AssessMode.dangerous(payload.ep())) {
            SageDialogue.queue("voice.seizon_kakuritsu");
        }
    }

    public static boolean dangerous(double targetEp) {
        double mine = AssessMode.playerEp();
        return mine > 0.0 && targetEp > mine * (Double)GreatSageConfig.ASSESS_WARN_RATIO.get();
    }

    public static void accept(AssessAuraPayload payload) {
        viewerEp = payload.viewerEp();
        AURA.clear();
        for (int i = 0; i < payload.ids().size() && i < payload.eps().size(); ++i) {
            AURA.put(payload.ids().get(i).intValue(), payload.eps().get(i).doubleValue());
        }
    }

    public static double playerEp() {
        return viewerEp;
    }

    public static double epOf(int entityId) {
        return AURA.containsKey(entityId) ? AURA.get(entityId) : -1.0;
    }

    @Nullable
    public static LivingEntity target() {
        return target;
    }

    public static float scan() {
        return scan;
    }

    public static boolean holding() {
        return holding;
    }

    public static String obstacle() {
        return obstacle;
    }

    @Nullable
    public static AssessResultPayload result() {
        return result;
    }

    public static long resultAt() {
        return resultAt;
    }

    public static double scanDistance() {
        return 32.0;
    }

    static {
        obstacle = "";
        AURA = new Int2DoubleOpenHashMap();
    }
}

