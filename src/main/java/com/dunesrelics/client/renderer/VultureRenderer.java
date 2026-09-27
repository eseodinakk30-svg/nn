package com.dunesrelics.client.renderer;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.client.model.VultureModel;
import com.dunesrelics.entity.Vulture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class VultureRenderer extends MobRenderer<Vulture, VultureModel<Vulture>> {
    private static final ResourceLocation TEXTURE = DunesRelics.id("textures/entity/vulture.png");

    public VultureRenderer(EntityRendererProvider.Context context) {
        super(context, new VultureModel<>(context.bakeLayer(VultureModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(Vulture entity) {
        return TEXTURE;
    }

    @Override
    protected float getFlipDegrees(Vulture entity) {
        return 90.0F;
    }
}
