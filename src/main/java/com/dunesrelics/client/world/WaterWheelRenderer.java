package com.dunesrelics.client.world;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.block.entity.WaterWheelBlockEntity;
import com.dunesrelics.block.world.WaterWheelBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.data.ModelData;

/** Turns the wheel (a JSON model drawn around its axle) while water drives it, easing in and out. */
public class WaterWheelRenderer implements BlockEntityRenderer<WaterWheelBlockEntity> {
    public static final ResourceLocation WHEEL = DunesRelics.id("block/water_wheel_wheel");

    public WaterWheelRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WaterWheelBlockEntity wheel, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (wheel.getLevel() == null) {
            return;
        }
        double time = wheel.getLevel().getGameTime() + (double) partialTick;
        float dt = wheel.lastRenderTime < 0 ? 0.0F : (float) Math.max(0.0D, Math.min(5.0D, time - wheel.lastRenderTime));
        wheel.lastRenderTime = time;
        boolean spinning = wheel.getBlockState().getValue(WaterWheelBlock.SPINNING);
        wheel.speed += ((spinning ? 1.0F : 0.0F) - wheel.speed) * Math.min(1.0F, dt * 0.04F);
        wheel.angle = (wheel.angle + wheel.speed * dt * 3.0F) % 360.0F;

        BakedModel model = Minecraft.getInstance().getModelManager().getModel(WHEEL);
        pose.pushPose();
        pose.translate(0.5D, 0.5D, 0.5D);
        if (wheel.getBlockState().getValue(WaterWheelBlock.AXIS) == Direction.Axis.Z) {
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        pose.mulPose(Axis.XP.rotationDegrees(wheel.angle));
        pose.translate(-0.5D, -0.5D, -0.5D);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderType.cutout()), null, model, 1.0F, 1.0F, 1.0F, light, overlay, ModelData.EMPTY,
                RenderType.cutout());
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(WaterWheelBlockEntity wheel) {
        return true;
    }
}
