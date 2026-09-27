package com.dunesrelics.client.renderer;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.client.model.ScorpionModel;
import com.dunesrelics.entity.Scorpion;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ScorpionRenderer extends MobRenderer<Scorpion, ScorpionModel<Scorpion>> {
    private static final ResourceLocation TEXTURE = DunesRelics.id("textures/entity/scorpion.png");

    public ScorpionRenderer(EntityRendererProvider.Context context) {
        super(context, new ScorpionModel<>(context.bakeLayer(ScorpionModel.LAYER_LOCATION)), 0.8F);
    }

    @Override
    public ResourceLocation getTextureLocation(Scorpion entity) {
        return TEXTURE;
    }

    @Override
    protected float getFlipDegrees(Scorpion entity) {
        return 180.0F;
    }
}
