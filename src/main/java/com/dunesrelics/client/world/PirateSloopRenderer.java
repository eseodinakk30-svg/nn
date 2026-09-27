package com.dunesrelics.client.world;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.entity.world.PirateSloop;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.model.data.ModelData;

/**
 * The sloop is drawn from a JSON model of ordinary blocks' textures (planks, wool), a third of its size and scaled
 * up, turned to its heading, rocking on the swell and shuddering when hit.
 */
public class PirateSloopRenderer extends EntityRenderer<PirateSloop> {
    public static final ResourceLocation MODEL = DunesRelics.id("entity/pirate_sloop");

    public PirateSloopRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 2.0F;
    }

    @Override
    public void render(PirateSloop sloop, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(MODEL);
        float time = sloop.tickCount + partialTick;
        float hurt = sloop.getHurtTime() - partialTick;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.rotLerp(partialTick, sloop.yRotO, sloop.getYRot())));
        // the swell, and a shudder when a shot strikes home
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(time * 0.05F) * 2.5F + (hurt > 0.0F ? Mth.sin(hurt) * hurt * 0.6F : 0.0F)));
        pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(time * 0.037F + 1.0F) * 1.2F));
        pose.scale(2.5F, 2.5F, 2.5F);
        pose.translate(-0.5D, 0.0D, -0.5D);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderType.cutout()), null, model, 1.0F, 1.0F, 1.0F, light, OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY, RenderType.cutout());
        pose.popPose();
        super.render(sloop, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(PirateSloop sloop) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
