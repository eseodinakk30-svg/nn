package com.dunesrelics.client.renderer;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.entity.Mummy;
import com.dunesrelics.entity.Pharaoh;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

/** Renders mummies and the (slightly larger) Pharaoh with the humanoid zombie model. */
public class MummyRenderer extends AbstractZombieRenderer<Mummy, ZombieModel<Mummy>> {
    private static final ResourceLocation MUMMY = DunesRelics.id("textures/entity/mummy.png");
    private static final ResourceLocation PHARAOH = DunesRelics.id("textures/entity/pharaoh.png");

    public MummyRenderer(EntityRendererProvider.Context context) {
        super(context,
                new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)),
                new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)));
    }

    @Override
    public ResourceLocation getTextureLocation(Zombie entity) {
        return entity instanceof Pharaoh ? PHARAOH : MUMMY;
    }

    @Override
    protected void scale(Mummy entity, PoseStack poseStack, float partialTick) {
        if (entity instanceof Pharaoh) {
            poseStack.scale(1.15F, 1.15F, 1.15F);
        }
        super.scale(entity, poseStack, partialTick);
    }
}
