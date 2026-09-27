package com.dunesrelics.client.renderer;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.client.model.ScarabModel;
import com.dunesrelics.entity.Scarab;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ScarabRenderer extends MobRenderer<Scarab, ScarabModel<Scarab>> {
    private static final ResourceLocation TEXTURE = DunesRelics.id("textures/entity/scarab.png");

    public ScarabRenderer(EntityRendererProvider.Context context) {
        super(context, new ScarabModel<>(context.bakeLayer(ScarabModel.LAYER_LOCATION)), 0.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(Scarab entity) {
        return TEXTURE;
    }

    @Override
    protected float getFlipDegrees(Scarab entity) {
        return 180.0F;
    }
}
