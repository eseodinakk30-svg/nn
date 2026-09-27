package com.dunesrelics.client.renderer;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.client.model.MagmalingModel;
import com.dunesrelics.entity.volcanic.Magmaling;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class MagmalingRenderer extends MobRenderer<Magmaling, MagmalingModel<Magmaling>> {
    private static final ResourceLocation TEXTURE = DunesRelics.id("textures/entity/magmaling.png");

    public MagmalingRenderer(EntityRendererProvider.Context context) {
        super(context, new MagmalingModel<>(context.bakeLayer(MagmalingModel.LAYER_LOCATION)), 0.4F);
        this.addLayer(new GlowLayer<>(this, DunesRelics.id("textures/entity/magmaling_glow.png")));
    }

    @Override
    public ResourceLocation getTextureLocation(Magmaling entity) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(Magmaling entity, BlockPos pos) {
        return true ? 15 : super.getBlockLightLevel(entity, pos);
    }

    @Override
    protected void scale(Magmaling entity, PoseStack poseStack, float partialTick) {
        if (entity.isBaby()) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
    }
}
