/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.core.BlockPos
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent$LoggingOut
 */
package dev.dedek.greatsage.client;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid="greatsage", value={Dist.CLIENT})
public final class Waypoints {
    @Nullable
    private static BlockPos mark;
    @Nullable
    private static BlockPos death;
    @Nullable
    private static BlockPos lastAlive;

    private Waypoints() {
    }

    public static void mark(Minecraft minecraft) {
        if (minecraft.player != null) {
            mark = minecraft.player.blockPosition();
        }
    }

    @Nullable
    public static String recall() {
        return Waypoints.format(mark);
    }

    @Nullable
    public static String death() {
        return Waypoints.format(death);
    }

    public static void clear() {
        mark = null;
    }

    @Nullable
    private static String format(@Nullable BlockPos pos) {
        return pos == null ? null : pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }
        if (minecraft.player.isAlive() && minecraft.player.getHealth() > 0.0f) {
            lastAlive = minecraft.player.blockPosition();
        } else if (lastAlive != null) {
            death = lastAlive;
            lastAlive = null;
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        mark = null;
        death = null;
        lastAlive = null;
    }
}

