package com.dunesrelics.client.world;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.AbstractIllager;

/** Draws a tricorn on an illager's head. */
public class TricornLayer<T extends AbstractIllager> extends RenderLayer<T, IllagerModel<T>> {
    private final ModelPart hat;
    private final ResourceLocation texture;

    public TricornLayer(RenderLayerParent<T, IllagerModel<T>> parent, ModelPart hat, ResourceLocation texture) {
        super(parent);
        this.hat = hat;
        this.texture = texture;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        pose.pushPose();
        this.getParentModel().getHead().translateAndRotate(pose);
        this.hat.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(this.texture)), light,
                LivingEntityRenderer.getOverlayCoords(entity, 0.0F));
        pose.popPose();
    }
}
