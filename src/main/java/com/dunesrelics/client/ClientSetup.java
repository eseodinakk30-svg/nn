package com.dunesrelics.client;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.client.model.MeerkatModel;
import com.dunesrelics.client.model.ScarabModel;
import com.dunesrelics.client.model.ScorpionModel;
import com.dunesrelics.client.model.VultureModel;
import com.dunesrelics.client.particle.SandGustParticle;
import com.dunesrelics.client.renderer.MeerkatRenderer;
import com.dunesrelics.client.renderer.MummyRenderer;
import com.dunesrelics.client.renderer.ScarabRenderer;
import com.dunesrelics.client.renderer.ScorpionRenderer;
import com.dunesrelics.client.renderer.VultureRenderer;
import com.dunesrelics.registry.ModEntities;
import com.dunesrelics.registry.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DunesRelics.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MUMMY.get(), MummyRenderer::new);
        event.registerEntityRenderer(ModEntities.PHARAOH.get(), MummyRenderer::new);
        event.registerEntityRenderer(ModEntities.SCORPION.get(), ScorpionRenderer::new);
        event.registerEntityRenderer(ModEntities.SCARAB.get(), ScarabRenderer::new);
        event.registerEntityRenderer(ModEntities.MEERKAT.get(), MeerkatRenderer::new);
        event.registerEntityRenderer(ModEntities.VULTURE.get(), VultureRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ScorpionModel.LAYER_LOCATION, ScorpionModel::createBodyLayer);
        event.registerLayerDefinition(ScarabModel.LAYER_LOCATION, ScarabModel::createBodyLayer);
        event.registerLayerDefinition(MeerkatModel.LAYER_LOCATION, MeerkatModel::createBodyLayer);
        event.registerLayerDefinition(VultureModel.LAYER_LOCATION, VultureModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SAND_GUST.get(), SandGustParticle.Provider::new);
    }
}
