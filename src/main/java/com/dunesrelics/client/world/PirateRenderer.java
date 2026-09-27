package com.dunesrelics.client.world;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.entity.world.PirateCaptain;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IllagerRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Vindicator;

/** Pirates and their captain: illagers with a cutlass drawn only when they fight. */
public class PirateRenderer<T extends Vindicator> extends IllagerRenderer<T> {
    private final ResourceLocation texture;

    public PirateRenderer(EntityRendererProvider.Context context, String texture, boolean tricorn) {
        super(context, new IllagerModel<>(context.bakeLayer(ModelLayers.VINDICATOR)), 0.5F);
        this.texture = DunesRelics.id("textures/entity/" + texture + ".png");
        this.addLayer(new ItemInHandLayer<T, IllagerModel<T>>(this, context.getItemInHandRenderer()) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing,
                               float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                if (entity.isAggressive()) {
                    super.render(pose, buffers, light, entity, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
                }
            }
        });
        if (tricorn) {
            this.addLayer(new TricornLayer<>(this, context.bakeLayer(TricornModel.ILLAGER_HAT),
                    DunesRelics.id("textures/entity/" + texture + "_hat.png")));
        }
    }

    public static PirateRenderer<com.dunesrelics.entity.world.Pirate> pirate(EntityRendererProvider.Context context) {
        return new PirateRenderer<>(context, "pirate", false);
    }

    public static PirateRenderer<PirateCaptain> captain(EntityRendererProvider.Context context) {
        return new PirateRenderer<>(context, "pirate_captain", true);
    }

    @Override
    protected void scale(T entity, PoseStack pose, float partialTick) {
        if (entity instanceof PirateCaptain) {
            pose.scale(1.12F, 1.12F, 1.12F);
        }
        super.scale(entity, pose, partialTick);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return this.texture;
    }
}
