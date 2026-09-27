package com.dunesrelics.client.renderer;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.client.model.MagmaTitanModel;
import com.dunesrelics.entity.volcanic.MagmaTitan;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class MagmaTitanRenderer extends MobRenderer<MagmaTitan, MagmaTitanModel<MagmaTitan>> {
    private static final ResourceLocation TEXTURE = DunesRelics.id("textures/entity/magma_titan.png");

    public MagmaTitanRenderer(EntityRendererProvider.Context context) {
        super(context, new MagmaTitanModel<>(context.bakeLayer(MagmaTitanModel.LAYER_LOCATION)), 1.3F);
        this.addLayer(new GlowLayer<>(this, DunesRelics.id("textures/entity/magma_titan_glow.png")));
    }

    @Override
    public ResourceLocation getTextureLocation(MagmaTitan entity) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(MagmaTitan entity, BlockPos pos) {
        return true ? 15 : super.getBlockLightLevel(entity, pos);
    }

    @Override
    protected void scale(MagmaTitan entity, PoseStack poseStack, float partialTick) {
        if (entity.isBaby()) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
    }
}
