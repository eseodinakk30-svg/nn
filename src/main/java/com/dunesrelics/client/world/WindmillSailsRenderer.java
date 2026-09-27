package com.dunesrelics.client.world;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.block.entity.WindmillSailsBlockEntity;
import com.dunesrelics.block.world.WindmillSailsBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.data.ModelData;

/** Turns the sails (a JSON model drawn around the hub) with the wind, easing in and out. */
public class WindmillSailsRenderer implements BlockEntityRenderer<WindmillSailsBlockEntity> {
    public static final ResourceLocation SAILS = DunesRelics.id("block/windmill_sails_blades");

    public WindmillSailsRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WindmillSailsBlockEntity sails, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (sails.getLevel() == null) {
            return;
        }
        double time = sails.getLevel().getGameTime() + (double) partialTick;
        float dt = sails.lastRenderTime < 0 ? 0.0F : (float) Math.max(0.0D, Math.min(5.0D, time - sails.lastRenderTime));
        sails.lastRenderTime = time;
        int wind = sails.getBlockState().getValue(WindmillSailsBlock.WIND);
        sails.speed += (wind - sails.speed) * Math.min(1.0F, dt * 0.02F);
        sails.angle = (sails.angle + sails.speed * dt * 1.6F) % 360.0F;

        BakedModel model = Minecraft.getInstance().getModelManager().getModel(SAILS);
        float facing = sails.getBlockState().getValue(WindmillSailsBlock.FACING).toYRot();
        pose.pushPose();
        pose.translate(0.5D, 0.5D, 0.5D);
        // The model faces north; turn it to the block's facing, then spin it around its own axle.
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - facing));
        pose.mulPose(Axis.ZP.rotationDegrees(sails.angle));
        // the blades are modelled at half size: 3 blocks from the hub to the tips
        pose.scale(2.0F, 2.0F, 1.0F);
        pose.translate(-0.5D, -0.5D, -0.5D);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderType.cutout()), null, model, 1.0F, 1.0F, 1.0F, light, OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY, RenderType.cutout());
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(WindmillSailsBlockEntity sails) {
        return true;
    }
}
