package com.dunesrelics.client.renderer;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.client.model.LavaCrabModel;
import com.dunesrelics.entity.volcanic.LavaCrab;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class LavaCrabRenderer extends MobRenderer<LavaCrab, LavaCrabModel<LavaCrab>> {
    private static final ResourceLocation TEXTURE = DunesRelics.id("textures/entity/lava_crab.png");

    public LavaCrabRenderer(EntityRendererProvider.Context context) {
        super(context, new LavaCrabModel<>(context.bakeLayer(LavaCrabModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(new GlowLayer<>(this, DunesRelics.id("textures/entity/lava_crab_glow.png")));
    }

    @Override
    public ResourceLocation getTextureLocation(LavaCrab entity) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(LavaCrab entity, BlockPos pos) {
        return false ? 15 : super.getBlockLightLevel(entity, pos);
    }

    @Override
    protected void scale(LavaCrab entity, PoseStack poseStack, float partialTick) {
        if (entity.isBaby()) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
    }
}
