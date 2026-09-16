/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager$DestFactor
 *  com.mojang.blaze3d.platform.GlStateManager$SourceFactor
 *  com.mojang.blaze3d.platform.NativeImage
 *  com.mojang.blaze3d.platform.NativeImage$Format
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.renderer.texture.AbstractTexture
 *  net.minecraft.client.renderer.texture.DynamicTexture
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 */
package dev.dedek.greatsage.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dedek.greatsage.GreatSage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

final class LightFx {
    private static final ResourceLocation GLOW = GreatSage.id("dynamic/glow");
    private static final ResourceLocation BEAM = GreatSage.id("dynamic/beam");
    private static final ResourceLocation RING = GreatSage.id("dynamic/ring");
    private static final int SIZE = 96;
    private static boolean ready;

    private LightFx() {
    }

    static void ensureTextures() {
        if (ready) {
            return;
        }
        ready = true;
        LightFx.register(GLOW, LightFx::glowPixel);
        LightFx.register(BEAM, LightFx::beamPixel);
        LightFx.register(RING, LightFx::ringPixel);
    }

    private static void register(ResourceLocation id, Falloff falloff) {
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, 96, 96, false);
        for (int y = 0; y < 96; ++y) {
            for (int x = 0; x < 96; ++x) {
                float u = ((float)x + 0.5f) / 96.0f;
                float v = ((float)y + 0.5f) / 96.0f;
                int a = Mth.clamp((int)Math.round(falloff.at(u, v) * 255.0f), (int)0, (int)255);
                image.setPixelRGBA(x, y, a << 24 | 0xFFFFFF);
            }
        }
        Minecraft.getInstance().getTextureManager().register(id, (AbstractTexture)new DynamicTexture(image));
    }

    private static float glowPixel(float u, float v) {
        float d = Mth.sqrt((float)(LightFx.sq(u - 0.5f) + LightFx.sq(v - 0.5f))) * 2.0f;
        if (d >= 1.0f) {
            return 0.0f;
        }
        float f = 1.0f - d;
        return f * f * f * (0.55f + 0.45f * f);
    }

    private static float beamPixel(float u, float v) {
        float across = 1.0f - Math.min(1.0f, Math.abs(v - 0.5f) * 2.0f);
        float along = 1.0f - Math.min(1.0f, Math.abs(u - 0.5f) * 2.0f);
        return across * across * Mth.sqrt((float)along);
    }

    private static float ringPixel(float u, float v) {
        float d = Mth.sqrt((float)(LightFx.sq(u - 0.5f) + LightFx.sq(v - 0.5f))) * 2.0f;
        float edge = 1.0f - Math.min(1.0f, Math.abs(d - 0.86f) / 0.14f);
        return edge * edge * edge;
    }

    private static float sq(float x) {
        return x * x;
    }

    static void beginAdditive() {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((GlStateManager.SourceFactor)GlStateManager.SourceFactor.SRC_ALPHA, (GlStateManager.DestFactor)GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask((boolean)false);
    }

    static void endAdditive(GuiGraphics graphics) {
        graphics.flush();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }

    private static void tint(int rgb, float alpha) {
        RenderSystem.setShaderColor((float)((float)(rgb >> 16 & 0xFF) / 255.0f), (float)((float)(rgb >> 8 & 0xFF) / 255.0f), (float)((float)(rgb & 0xFF) / 255.0f), (float)Mth.clamp((float)alpha, (float)0.0f, (float)1.0f));
    }

    private static void sprite(GuiGraphics graphics, ResourceLocation texture, float centerX, float centerY, float width, float height, float rotationDeg, int rgb, float alpha) {
        if (alpha <= 0.004f || width < 0.4f || height < 0.4f) {
            return;
        }
        LightFx.tint(rgb, alpha);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX, centerY, 0.0f);
        if (rotationDeg != 0.0f) {
            pose.mulPose(Axis.ZP.rotationDegrees(rotationDeg));
        }
        pose.scale(width / 96.0f, height / 96.0f, 1.0f);
        graphics.blit(texture, -48, -48, 0.0f, 0.0f, 96, 96, 96, 96);
        pose.popPose();
    }

    static void glow(GuiGraphics graphics, float x, float y, float diameter, int rgb, float alpha) {
        LightFx.sprite(graphics, GLOW, x, y, diameter, diameter, 0.0f, rgb, alpha);
    }

    static void glow(GuiGraphics graphics, float x, float y, float width, float height, int rgb, float alpha) {
        LightFx.sprite(graphics, GLOW, x, y, width, height, 0.0f, rgb, alpha);
    }

    static void beam(GuiGraphics graphics, float centerX, float centerY, float length, float thickness, float angleDeg, int rgb, float alpha) {
        LightFx.sprite(graphics, BEAM, centerX, centerY, length, thickness, angleDeg, rgb, alpha);
    }

    static void ray(GuiGraphics graphics, float originX, float originY, float length, float thickness, float angleDeg, int rgb, float alpha) {
        float rad = angleDeg * ((float)Math.PI / 180);
        LightFx.beam(graphics, originX + Mth.cos((float)rad) * length / 2.0f, originY + Mth.sin((float)rad) * length / 2.0f, length, thickness, angleDeg, rgb, alpha);
    }

    static void ring(GuiGraphics graphics, float x, float y, float radius, int rgb, float alpha) {
        LightFx.sprite(graphics, RING, x, y, radius * 2.0f, radius * 2.0f, 0.0f, rgb, alpha);
    }

    static void ring(GuiGraphics graphics, float x, float y, float radiusX, float radiusY, int rgb, float alpha) {
        LightFx.sprite(graphics, RING, x, y, radiusX * 2.0f, radiusY * 2.0f, 0.0f, rgb, alpha);
    }

    static void arc(GuiGraphics graphics, float centerX, float centerY, float radius, float startDeg, float sweepDeg, float thickness, int rgb, float alpha) {
        if (Math.abs(sweepDeg) < 0.5f) {
            return;
        }
        int steps = Mth.clamp((int)Math.round(Math.abs(sweepDeg) * radius / 90.0f), (int)4, (int)160);
        for (int i = 0; i <= steps; ++i) {
            float angle = (startDeg + sweepDeg * (float)i / (float)steps) * ((float)Math.PI / 180);
            float end = Math.min(1.0f, (float)Math.min(i, steps - i) / 3.0f);
            LightFx.glow(graphics, centerX + Mth.cos((float)angle) * radius, centerY + Mth.sin((float)angle) * radius, thickness, rgb, alpha * (0.35f + 0.65f * end));
        }
    }

    private static interface Falloff {
        public float at(float var1, float var2);
    }
}

