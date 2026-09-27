package com.dunesrelics.client.renderer;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.client.model.CinderWraithModel;
import com.dunesrelics.entity.volcanic.CinderWraith;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class CinderWraithRenderer extends MobRenderer<CinderWraith, CinderWraithModel<CinderWraith>> {
    private static final ResourceLocation TEXTURE = DunesRelics.id("textures/entity/cinder_wraith.png");

    public CinderWraithRenderer(EntityRendererProvider.Context context) {
        super(context, new CinderWraithModel<>(context.bakeLayer(CinderWraithModel.LAYER_LOCATION)), 0.3F);
        this.addLayer(new GlowLayer<>(this, DunesRelics.id("textures/entity/cinder_wraith_glow.png")));
    }

    @Override
    public ResourceLocation getTextureLocation(CinderWraith entity) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(CinderWraith entity, BlockPos pos) {
        return false ? 15 : super.getBlockLightLevel(entity, pos);
    }

    @Override
    protected void scale(CinderWraith entity, PoseStack poseStack, float partialTick) {
        if (entity.isBaby()) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
    }
}
