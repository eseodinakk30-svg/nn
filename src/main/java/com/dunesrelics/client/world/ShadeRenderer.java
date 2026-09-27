package com.dunesrelics.client.world;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.client.model.ShadeModel;
import com.dunesrelics.client.renderer.GlowLayer;
import com.dunesrelics.entity.world.Shade;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ShadeRenderer extends MobRenderer<Shade, ShadeModel<Shade>> {
    private static final ResourceLocation TEXTURE = DunesRelics.id("textures/entity/shade.png");

    public ShadeRenderer(EntityRendererProvider.Context context) {
        super(context, new ShadeModel<>(context.bakeLayer(ShadeModel.LAYER_LOCATION)), 0.3F);
        this.addLayer(new GlowLayer<>(this, DunesRelics.id("textures/entity/shade_glow.png")));
    }

    @Override
    public ResourceLocation getTextureLocation(Shade entity) {
        return TEXTURE;
    }
}
