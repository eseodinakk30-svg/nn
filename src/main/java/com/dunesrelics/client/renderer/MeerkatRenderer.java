package com.dunesrelics.client.renderer;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.client.model.MeerkatModel;
import com.dunesrelics.entity.Meerkat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MeerkatRenderer extends MobRenderer<Meerkat, MeerkatModel<Meerkat>> {
    private static final ResourceLocation TEXTURE = DunesRelics.id("textures/entity/meerkat.png");

    public MeerkatRenderer(EntityRendererProvider.Context context) {
        super(context, new MeerkatModel<>(context.bakeLayer(MeerkatModel.LAYER_LOCATION)), 0.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(Meerkat entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(Meerkat entity, PoseStack poseStack, float partialTick) {
        if (entity.isBaby()) {
            poseStack.scale(0.55F, 0.55F, 0.55F);
        }
    }
}
