package com.dunesrelics.client.renderer;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.client.model.SalamanderModel;
import com.dunesrelics.entity.volcanic.Salamander;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class SalamanderRenderer extends MobRenderer<Salamander, SalamanderModel<Salamander>> {
    private static final ResourceLocation TEXTURE = DunesRelics.id("textures/entity/salamander.png");

    public SalamanderRenderer(EntityRendererProvider.Context context) {
        super(context, new SalamanderModel<>(context.bakeLayer(SalamanderModel.LAYER_LOCATION)), 0.4F);
        this.addLayer(new GlowLayer<>(this, DunesRelics.id("textures/entity/salamander_glow.png")));
    }

    @Override
    public ResourceLocation getTextureLocation(Salamander entity) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(Salamander entity, BlockPos pos) {
        return false ? 15 : super.getBlockLightLevel(entity, pos);
    }

    @Override
    protected void scale(Salamander entity, PoseStack poseStack, float partialTick) {
        if (entity.isBaby()) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
    }
}
