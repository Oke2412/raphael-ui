/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.Util
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.network.chat.Component
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector4f
 */
package dev.dedek.greatsage.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dedek.greatsage.client.AssessMode;
import dev.dedek.greatsage.client.LightFx;
import dev.dedek.greatsage.network.AssessResultPayload;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector4f;

public final class AssessOverlay {
    private static final int BLUE = 7330047;
    private static final int BLUE_DEEP = 3054552;
    private static final int ALERT = 0xFF7A7A;
    private static final int VEIL = 465454;
    private static final double AURA_RANGE = 64.0;

    private AssessOverlay() {
    }

    public static void render(GuiGraphics graphics) {
        if (!AssessMode.active()) {
            if (AssessMode.glitching()) {
                AssessOverlay.glitch(graphics, graphics.guiWidth(), graphics.guiHeight());
            }
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        LightFx.ensureTextures();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float time = Util.getMillis() % 1000000L;
        Font font = minecraft.font;
        AssessOverlay.veil(graphics, width, height);
        AssessOverlay.auras(graphics, minecraft, width, height);
        graphics.flush();
        LightFx.beginAdditive();
        AssessOverlay.reticle(graphics, width, height, time);
        LightFx.endAdditive(graphics);
        graphics.flush();
        if (AssessMode.glitching()) {
            AssessOverlay.glitch(graphics, width, height);
        }
        AssessOverlay.readouts(graphics, font, width, height, time);
        AssessOverlay.lockedTarget(graphics, font, minecraft, width, height, time);
        AssessResultPayload result = AssessMode.result();
        if (result != null) {
            AssessOverlay.window(graphics, font, result, width, height);
        }
        RenderSystem.disableBlend();
    }

    private static void glitch(GuiGraphics graphics, int width, int height) {
        float strength = AssessMode.glitch();
        int bands = 14;
        int bandHeight = Math.max(2, height / bands);
        long seed = Util.getMillis() / 60L;
        for (int i = 0; i < bands; ++i) {
            int hash = (int)(seed * 31L + (long)i * 2654435761L & Integer.MAX_VALUE);
            if ((hash & 3) == 0) continue;
            int y = i * bandHeight;
            int shift = (int)((float)((hash >> 4) % 41 - 20) * strength);
            int alpha = Mth.clamp((int)Math.round(strength * (float)((hash >> 12) % 90 + 40)), (int)0, (int)255);
            graphics.fill(shift, y, width + shift, y + bandHeight, alpha << 24 | ((hash & 1) == 0 ? 7330047 : 0xFF7A7A));
        }
        int flash = Mth.clamp((int)Math.round(strength * strength * 200.0f), (int)0, (int)255);
        if (flash > 4) {
            int y = Math.round((1.0f - strength) * (float)height);
            graphics.fill(0, y, width, y + Math.max(2, bandHeight / 2), flash << 24 | 0xFFFFFF);
        }
    }

    private static void veil(GuiGraphics graphics, int width, int height) {
        int base = Mth.clamp((int)Math.round(40.8f), (int)0, (int)255);
        graphics.fill(0, 0, width, height, base << 24 | 0x71A2E);
        int bands = 7;
        int stepX = Math.max(1, width / (bands * 2));
        int stepY = Math.max(1, height / (bands * 2));
        int band = Mth.clamp((int)Math.round(14.025f), (int)0, (int)255);
        for (int i = 0; i < bands; ++i) {
            int l = i * stepX;
            int t = i * stepY;
            int color = band << 24 | 0x71A2E;
            graphics.fill(l, t, l + stepX, height - t, color);
            graphics.fill(width - l - stepX, t, width - l, height - t, color);
            graphics.fill(l + stepX, t, width - l - stepX, t + stepY, color);
            graphics.fill(l + stepX, height - t - stepY, width - l - stepX, height - t, color);
        }
        float sweep = (float)(Util.getMillis() % 3200L) / 3200.0f;
        int y = Math.round(sweep * (float)height);
        graphics.fill(0, y, width, y + 1, 678418687);
        for (int line = 0; line < height; line += 4) {
            graphics.fill(0, line, width, line + 1, 170826712);
        }
    }

    private static void auras(GuiGraphics graphics, Minecraft minecraft, int width, int height) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Matrix4f projection = minecraft.gameRenderer.getProjectionMatrix((double)((Integer)minecraft.options.fov().get()).intValue());
        Matrix4f view = new Matrix4f().rotation((Quaternionfc)new Quaternionf((Quaternionfc)camera.rotation()).conjugate());
        Vec3 eye = camera.getPosition();
        double mine = Math.max(1.0, AssessMode.playerEp());
        float[] screen = new float[3];
        graphics.flush();
        LightFx.beginAdditive();
        for (LivingEntity entity : minecraft.level.getEntitiesOfClass(LivingEntity.class, minecraft.player.getBoundingBox().inflate(64.0))) {
            double ep;
            if (entity == minecraft.player || !entity.isAlive() || (ep = AssessMode.epOf(entity.getId())) < 0.0 || !AssessOverlay.project(entity.position().add(0.0, (double)entity.getBbHeight() * 0.5, 0.0), eye, view, projection, width, height, screen)) continue;
            float ratio = (float)(Math.log10(1.0 + Math.max(0.0, ep)) / Math.log10(1.0 + mine));
            float size = Mth.clamp((float)(18.0f + ratio * 46.0f), (float)14.0f, (float)130.0f);
            float shrink = (float)(1.0 / (1.0 + (double)screen[2] * 0.05));
            int color = AssessMode.dangerous(ep) ? 0xFF7A7A : 7330047;
            LightFx.glow(graphics, screen[0], screen[1], (size *= Mth.clamp((float)shrink, (float)0.25f, (float)1.0f)) * 2.2f, color, 0.1f);
            LightFx.glow(graphics, screen[0], screen[1], size, color, 0.22f);
            LightFx.ring(graphics, screen[0], screen[1], size * 0.55f, color, 0.3f);
        }
        LightFx.endAdditive(graphics);
        graphics.flush();
    }

    private static void reticle(GuiGraphics graphics, int width, int height, float time) {
        float cx = (float)width / 2.0f;
        float cy = (float)height / 2.0f;
        float r = (float)Math.min(width, height) * 0.115f;
        float scan = AssessMode.scan();
        LightFx.arc(graphics, cx, cy, r, time * 0.03f, 118.0f, 2.4f, 7330047, 0.55f);
        LightFx.arc(graphics, cx, cy, r, time * 0.03f + 180.0f, 118.0f, 2.4f, 7330047, 0.55f);
        LightFx.ring(graphics, cx, cy, r * 1.22f, 3054552, 0.2f);
        for (int i = 0; i < 4; ++i) {
            float angle = (float)i * 90.0f;
            LightFx.ray(graphics, cx + Mth.cos((float)(angle * ((float)Math.PI / 180))) * r * 0.34f, cy + Mth.sin((float)(angle * ((float)Math.PI / 180))) * r * 0.34f, r * 0.3f, 2.0f, angle, 7330047, 0.7f);
        }
        LightFx.glow(graphics, cx, cy, 4.0f, 0xFFFFFF, 0.8f);
        if (scan > 0.0f) {
            LightFx.arc(graphics, cx, cy, r * 1.42f, 0.0f, 359.0f, 6.5f, 3054552, 0.3f);
            LightFx.arc(graphics, cx, cy, r * 1.42f, -90.0f, 359.0f * scan, 6.5f, 0xFFFFFF, 0.95f);
            LightFx.arc(graphics, cx, cy, r * 1.42f, -90.0f, 359.0f * scan, 13.0f, 7330047, 0.3f);
        }
    }

    private static void readouts(GuiGraphics graphics, Font font, int width, int height, float time) {
        String state;
        int a = 190;
        AssessOverlay.line(graphics, font, 26, 24, Component.translatable((String)"greatsage.assess.mode").getString(), a, 7330047);
        AssessOverlay.line(graphics, font, 26, 34, Component.translatable((String)"greatsage.assess.ep", (Object[])new Object[]{AssessOverlay.format(AssessMode.playerEp())}).getString(), 150, 3054552);
        LivingEntity target = AssessMode.target();
        if (target == null) {
            state = "greatsage.assess.idle";
        } else if (AssessMode.result() != null) {
            state = "greatsage.assess.done";
        } else {
            state = switch (AssessMode.obstacle()) {
                case "hold" -> "greatsage.assess.hold";
                case "far" -> "greatsage.assess.far";
                default -> "greatsage.assess.scanning";
            };
        }
        String text = Component.translatable((String)state).getString();
        boolean warn = state.endsWith("hold") || state.endsWith("far");
        int color = warn ? 0xFF7A7A : 7330047;
        int alpha = warn ? Math.round(120.0f + 90.0f * Math.abs(Mth.sin((float)(time / 260.0f)))) : a;
        AssessOverlay.line(graphics, font, 26, height - 34, text, alpha, color);
    }

    private static void lockedTarget(GuiGraphics graphics, Font font, Minecraft minecraft, int width, int height, float time) {
        LivingEntity target = AssessMode.target();
        if (target == null) {
            return;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Matrix4f projection = minecraft.gameRenderer.getProjectionMatrix((double)((Integer)minecraft.options.fov().get()).intValue());
        Matrix4f view = new Matrix4f().rotation((Quaternionfc)new Quaternionf((Quaternionfc)camera.rotation()).conjugate());
        Vec3 eye = camera.getPosition();
        float[] feet = new float[3];
        float[] head = new float[3];
        if (!AssessOverlay.project(target.position(), eye, view, projection, width, height, feet) || !AssessOverlay.project(target.position().add(0.0, (double)target.getBbHeight(), 0.0), eye, view, projection, width, height, head)) {
            return;
        }
        float cx = (feet[0] + head[0]) * 0.5f;
        float cy = (feet[1] + head[1]) * 0.5f;
        float halfHeight = Math.max(9.0f, Math.abs(feet[1] - head[1]) * 0.6f);
        float halfWidth = Mth.clamp((float)(halfHeight * (target.getBbWidth() / Math.max(0.3f, target.getBbHeight()))), (float)(halfHeight * 0.35f), (float)(halfHeight * 1.6f));
        float scan = AssessMode.scan();
        boolean done = AssessMode.result() != null;
        int color = done ? 0xFFFFFF : 7330047;
        float grip = 1.0f + (1.0f - scan) * 0.35f;
        float arm = Mth.clamp((float)(halfHeight * 0.42f), (float)5.0f, (float)14.0f);
        graphics.flush();
        LightFx.beginAdditive();
        for (int i = 0; i < 4; ++i) {
            float sx = (i & 1) == 0 ? -1.0f : 1.0f;
            float sy = (i & 2) == 0 ? -1.0f : 1.0f;
            float x = cx + sx * halfWidth * grip;
            float y = cy + sy * halfHeight * grip;
            LightFx.beam(graphics, x - sx * arm / 2.0f, y, arm, 2.0f, 0.0f, color, 0.85f);
            LightFx.beam(graphics, x, y - sy * arm / 2.0f, arm, 2.0f, 90.0f, color, 0.85f);
        }
        if (!done && scan > 0.0f) {
            float sweep = time % 1100.0f / 1100.0f;
            float y = cy - halfHeight * grip + sweep * halfHeight * 2.0f * grip;
            LightFx.beam(graphics, cx, y, halfWidth * 2.0f * grip, 1.6f, 0.0f, 7330047, 0.55f);
            LightFx.glow(graphics, cx, y, halfWidth * 2.4f, 8.0f, 7330047, 0.14f);
        }
        LightFx.endAdditive(graphics);
        graphics.flush();
        String name = target.getName().getString();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(cx, cy - halfHeight * grip - 11.0f, 0.0f);
        pose.scale(0.75f, 0.75f, 1.0f);
        graphics.drawString(font, name, -font.width(name) / 2, 0, 0xDC000000 | color, true);
        pose.popPose();
    }

    private static void window(GuiGraphics graphics, Font font, AssessResultPayload result, int width, int height) {
        float appear = Mth.clamp((float)((float)(Util.getMillis() - AssessMode.resultAt()) / 320.0f), (float)0.0f, (float)1.0f);
        int a = Mth.clamp((int)Math.round(appear * 255.0f), (int)0, (int)255);
        if (a < 8) {
            return;
        }
        int panelWidth = Math.min(168, width / 3);
        int left = width - panelWidth - 18;
        int top = Math.round((float)height * 0.16f);
        int rows = result.failed() ? 2 : 4 + Math.min(5, result.skills().size()) + (result.skills().isEmpty() ? 0 : 1);
        int panelHeight = 22 + rows * 11;
        graphics.fill(left, top, left + panelWidth, top + panelHeight, Math.round((float)a * 0.82f) << 24 | 0x71A2E);
        int edge = a << 24 | (result.failed() ? 0xFF7A7A : 7330047);
        graphics.fill(left, top, left + panelWidth, top + 1, edge);
        graphics.fill(left, top + panelHeight - 1, left + panelWidth, top + panelHeight, edge);
        graphics.fill(left, top, left + 1, top + panelHeight, edge);
        graphics.fill(left + panelWidth - 1, top, left + panelWidth, top + panelHeight, edge);
        graphics.drawString(font, result.name(), left + 8, top + 8, a << 24 | 0xFFFFFF, true);
        int y = top + 21;
        if (result.failed()) {
            AssessOverlay.row(graphics, font, left, y, a, Component.translatable((String)"greatsage.assess.unreadable").getString(), "", 0xFF7A7A);
            return;
        }
        if (!result.race().isEmpty()) {
            y = AssessOverlay.row(graphics, font, left, y, a, Component.translatable((String)"greatsage.assess.race").getString(), result.race(), 7330047);
        }
        y = AssessOverlay.row(graphics, font, left, y, a, Component.translatable((String)"greatsage.assess.epLabel").getString(), AssessOverlay.format(result.ep()), AssessMode.dangerous(result.ep()) ? 0xFF7A7A : 7330047);
        y = AssessOverlay.row(graphics, font, left, y, a, Component.translatable((String)"greatsage.assess.health").getString(), String.valueOf(Math.round(result.maxHealth())), 3054552);
        if (!result.skills().isEmpty()) {
            y = AssessOverlay.row(graphics, font, left, y, a, Component.translatable((String)"greatsage.assess.notable").getString(), "", 3054552);
            int shown = 0;
            for (String skill : result.skills()) {
                if (shown++ >= 5) break;
                int split = skill.indexOf(124);
                y = AssessOverlay.row(graphics, font, left, y, a, "\u00b7", split < 0 ? skill : skill.substring(split + 1), 12573160);
            }
        }
    }

    private static int row(GuiGraphics graphics, Font font, int left, int y, int a, String label, String value, int color) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate((float)left + 8.0f, (float)y, 0.0f);
        pose.scale(0.75f, 0.75f, 1.0f);
        graphics.drawString(font, label, 0, 0, Math.round((float)a * 0.55f) << 24 | 0x2E9BD8, false);
        if (!value.isEmpty()) {
            graphics.drawString(font, value, font.width(label) + 6, 0, a << 24 | color, false);
        }
        pose.popPose();
        return y + 11;
    }

    private static void line(GuiGraphics graphics, Font font, int x, int y, String text, int alpha, int color) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate((float)x, (float)y, 0.0f);
        pose.scale(0.75f, 0.75f, 1.0f);
        graphics.drawString(font, text, 0, 0, Mth.clamp((int)alpha, (int)0, (int)255) << 24 | color, true);
        pose.popPose();
    }

    private static boolean project(Vec3 world, Vec3 eye, Matrix4f view, Matrix4f projection, int width, int height, float[] out) {
        Vector4f point = new Vector4f((float)(world.x - eye.x), (float)(world.y - eye.y), (float)(world.z - eye.z), 1.0f);
        view.transform(point);
        float depth = -point.z;
        projection.transform(point);
        if (point.w <= 1.0E-4f || depth <= 0.0f) {
            return false;
        }
        out[0] = (point.x / point.w * 0.5f + 0.5f) * (float)width;
        out[1] = (1.0f - (point.y / point.w * 0.5f + 0.5f)) * (float)height;
        out[2] = depth;
        return true;
    }

    private static String format(double value) {
        if (value >= 1000000.0) {
            return String.format(Locale.ROOT, "%.1fM", value / 1000000.0);
        }
        if (value >= 1000.0) {
            return String.format(Locale.ROOT, "%.1fk", value / 1000.0);
        }
        return String.valueOf(Math.round(value));
    }
}

