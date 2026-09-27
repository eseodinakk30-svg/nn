package com.dunesrelics.client.world;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.entity.world.VillageWorker;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Village workers: villagers dressed for their trade, with free arms for their tools. */
public class WorkerRenderer extends HumanoidMobRenderer<VillageWorker, FolkModel<VillageWorker>> {
    private static final ResourceLocation BUILDER = DunesRelics.id("textures/entity/village_builder.png");
    private static final ResourceLocation LUMBERJACK = DunesRelics.id("textures/entity/lumberjack.png");
    private static final ResourceLocation QUARRYMAN = DunesRelics.id("textures/entity/quarryman.png");

    public WorkerRenderer(EntityRendererProvider.Context context) {
        super(context, new FolkModel<>(context.bakeLayer(FolkModel.LAYER)), 0.5F);
    }

    @Override
    protected void scale(VillageWorker worker, PoseStack pose, float partialTick) {
        pose.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    public ResourceLocation getTextureLocation(VillageWorker worker) {
        return switch (worker.getJob()) {
            case BUILDER -> BUILDER;
            case LUMBERJACK -> LUMBERJACK;
            case QUARRYMAN -> QUARRYMAN;
        };
    }
}
