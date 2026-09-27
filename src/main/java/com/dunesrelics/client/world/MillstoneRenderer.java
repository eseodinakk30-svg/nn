package com.dunesrelics.client.world;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.block.entity.MillstoneBlockEntity;
import com.dunesrelics.block.world.MillstoneBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.data.ModelData;

/** The upper (runner) stone of the mill, turning while it grinds. */
public class MillstoneRenderer implements BlockEntityRenderer<MillstoneBlockEntity> {
    public static final ResourceLocation RUNNER = DunesRelics.id("block/millstone_runner");

    public MillstoneRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MillstoneBlockEntity mill, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (mill.getLevel() == null) {
            return;
        }
        boolean active = mill.getBlockState().getValue(MillstoneBlock.ACTIVE);
        float angle = active ? (mill.getLevel().getGameTime() + partialTick) * 4.0F % 360.0F : 0.0F;
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(RUNNER);
        pose.pushPose();
        pose.translate(0.5D, 0.0D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.translate(-0.5D, 0.0D, -0.5D);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderType.solid()), null, model, 1.0F, 1.0F, 1.0F, light, overlay, ModelData.EMPTY,
                RenderType.solid());
        pose.popPose();
    }
}
