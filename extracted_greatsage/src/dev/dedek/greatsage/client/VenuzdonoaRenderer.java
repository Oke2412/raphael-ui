/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.entity.ItemRenderer
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.ItemDisplayContext
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 */
package dev.dedek.greatsage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dedek.greatsage.content.GreatSageContent;
import dev.dedek.greatsage.content.VenuzdonoaEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class VenuzdonoaRenderer
extends EntityRenderer<VenuzdonoaEntity> {
    private static final float BURIED_Y = -1.65f;
    private static final float PLANTED_Y = 0.95f;
    private static final float RISE_SPIN_DEGREES = 180.0f;
    private static final float SCALE = 1.0f;
    private final ItemRenderer itemRenderer;
    private final ItemStack stack;

    public VenuzdonoaRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.stack = new ItemStack((ItemLike)GreatSageContent.VENUZDONOA_ITEM.get());
    }

    public void render(VenuzdonoaEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float progress = entity.riseProgress(partialTick);
        float eased = VenuzdonoaRenderer.easeOutCubic(progress);
        float y = Mth.lerp((float)eased, (float)-1.65f, (float)0.95f);
        if (progress >= 1.0f) {
            float since = (float)entity.tickCount + partialTick - 44.0f;
            y += (float)Math.exp((double)(-since) / 5.0) * 0.12f * Mth.cos((float)(since * 0.9f));
        }
        poseStack.pushPose();
        poseStack.translate(0.0f, y, 0.0f);
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot() + (1.0f - eased) * 180.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0f));
        poseStack.scale(1.0f, 1.0f, 1.0f);
        this.itemRenderer.renderStatic(this.stack, ItemDisplayContext.NONE, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
        super.render((Entity)entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static float easeOutCubic(float t) {
        float inv = 1.0f - t;
        return 1.0f - inv * inv * inv;
    }

    public ResourceLocation getTextureLocation(VenuzdonoaEntity entity) {
        return ResourceLocation.withDefaultNamespace((String)"textures/atlas/blocks.png");
    }
}

