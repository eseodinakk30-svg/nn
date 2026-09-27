package com.dunesrelics.client.world;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.entity.world.PirateGunner;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IllagerRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class PirateGunnerRenderer extends IllagerRenderer<PirateGunner> {
    private static final ResourceLocation TEXTURE = DunesRelics.id("textures/entity/pirate_gunner.png");

    public PirateGunnerRenderer(EntityRendererProvider.Context context) {
        super(context, new IllagerModel<>(context.bakeLayer(ModelLayers.PILLAGER)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new TricornLayer<>(this, context.bakeLayer(TricornModel.ILLAGER_HAT),
                DunesRelics.id("textures/entity/pirate_gunner_hat.png")));
    }

    @Override
    public ResourceLocation getTextureLocation(PirateGunner entity) {
        return TEXTURE;
    }
}
