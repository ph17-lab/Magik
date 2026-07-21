package com.magik.client.render;

import com.magik.MagikMod;
import com.magik.entity.MagicBoltEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Custom spell renderer: a pulsing camera-facing SQUARE energy block with a
 * translucent halo, drawn fullbright with a banded pixel-art texture per
 * element. Replaces the vanilla thrown-item look entirely.
 */
public class MagicBoltRenderer extends EntityRenderer<MagicBoltEntity> {

    private static final ResourceLocation[] TEXTURES;

    static {
        MagicBoltEntity.Variant[] variants = MagicBoltEntity.Variant.values();
        TEXTURES = new ResourceLocation[variants.length];
        for (MagicBoltEntity.Variant variant : variants) {
            TEXTURES[variant.ordinal()] = new ResourceLocation(MagikMod.MOD_ID,
                    "textures/entity/bolt_" + variant.name().toLowerCase(java.util.Locale.ROOT) + ".png");
        }
    }

    public MagicBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MagicBoltEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTick;
        float pulse = 0.85F + 0.15F * Mth.sin(age * 0.6F);
        float size = baseSize(entity.getVariant()) * pulse;

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.1D, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        VertexConsumer consumer = buffer.getBuffer(
                RenderType.entityTranslucentEmissive(getTextureLocation(entity)));
        drawQuad(poseStack, consumer, size, 255);
        drawQuad(poseStack, consumer, size * 1.35F, 110);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    /** The Fireball is a big square block of flame; the rest stay compact. */
    private static float baseSize(MagicBoltEntity.Variant variant) {
        return switch (variant) {
            case FIRE -> 0.85F;
            case SUPREME -> 0.7F;
            default -> 0.45F;
        };
    }

    private static void drawQuad(PoseStack poseStack, VertexConsumer consumer, float radius, int alpha) {
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, -radius, -radius, 0.0F, 1.0F, alpha);
        vertex(consumer, pose, radius, -radius, 1.0F, 1.0F, alpha);
        vertex(consumer, pose, radius, radius, 1.0F, 0.0F, alpha);
        vertex(consumer, pose, -radius, radius, 0.0F, 0.0F, alpha);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               float x, float y, float u, float v, int alpha) {
        consumer.vertex(pose.pose(), x, y, 0.0F)
                .color(255, 255, 255, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(MagicBoltEntity entity) {
        return TEXTURES[entity.getVariant().ordinal()];
    }
}
