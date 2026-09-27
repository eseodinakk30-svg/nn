package com.dunesrelics.client;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.client.model.CinderWraithModel;
import com.dunesrelics.client.model.LavaCrabModel;
import com.dunesrelics.client.model.MagmaTitanModel;
import com.dunesrelics.client.model.MagmalingModel;
import com.dunesrelics.client.model.MeerkatModel;
import com.dunesrelics.client.model.SalamanderModel;
import com.dunesrelics.client.model.ScarabModel;
import com.dunesrelics.client.model.ScorpionModel;
import com.dunesrelics.client.model.VultureModel;
import com.dunesrelics.client.particle.SandGustParticle;
import com.dunesrelics.client.renderer.CinderWraithRenderer;
import com.dunesrelics.client.renderer.LavaCrabRenderer;
import com.dunesrelics.client.renderer.MagmaTitanRenderer;
import com.dunesrelics.client.renderer.MagmalingRenderer;
import com.dunesrelics.client.renderer.MeerkatRenderer;
import com.dunesrelics.client.renderer.SalamanderRenderer;
import com.dunesrelics.client.renderer.VolcanicBombRenderer;
import com.dunesrelics.client.renderer.MummyRenderer;
import com.dunesrelics.client.renderer.ScarabRenderer;
import com.dunesrelics.client.renderer.ScorpionRenderer;
import com.dunesrelics.client.renderer.VultureRenderer;
import com.dunesrelics.registry.ModEntities;
import com.dunesrelics.registry.ModBlockEntities;
import com.dunesrelics.registry.WorldBlocks;
import com.dunesrelics.registry.WorldItems;
import com.dunesrelics.block.world.WaterTroughBlock;
import com.dunesrelics.client.model.ShadeModel;
import com.dunesrelics.client.world.MillstoneRenderer;
import com.dunesrelics.client.world.PirateGunnerRenderer;
import com.dunesrelics.client.world.PirateRenderer;
import com.dunesrelics.client.world.ShadeRenderer;
import com.dunesrelics.client.world.TravelerRenderer;
import com.dunesrelics.client.world.TricornModel;
import com.dunesrelics.client.world.WaterWheelRenderer;
import com.dunesrelics.item.world.TideClockItem;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
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
        event.registerEntityRenderer(ModEntities.MAGMA_TITAN.get(), MagmaTitanRenderer::new);
        event.registerEntityRenderer(ModEntities.SALAMANDER.get(), SalamanderRenderer::new);
        event.registerEntityRenderer(ModEntities.LAVA_CRAB.get(), LavaCrabRenderer::new);
        event.registerEntityRenderer(ModEntities.MAGMALING.get(), MagmalingRenderer::new);
        event.registerEntityRenderer(ModEntities.CINDER_WRAITH.get(), CinderWraithRenderer::new);
        event.registerEntityRenderer(ModEntities.VOLCANIC_BOMB.get(), VolcanicBombRenderer::new);
        event.registerEntityRenderer(ModEntities.PIRATE.get(), PirateRenderer::pirate);
        event.registerEntityRenderer(ModEntities.PIRATE_CAPTAIN.get(), PirateRenderer::captain);
        event.registerEntityRenderer(ModEntities.PIRATE_GUNNER.get(), PirateGunnerRenderer::new);
        event.registerEntityRenderer(ModEntities.TRAVELER.get(), TravelerRenderer::new);
        event.registerEntityRenderer(ModEntities.SHADE.get(), ShadeRenderer::new);
        event.registerEntityRenderer(ModEntities.CANNONBALL.get(), context -> new ThrownItemRenderer<>(context, 1.5F, false));
        event.registerBlockEntityRenderer(ModBlockEntities.WATER_WHEEL.get(), WaterWheelRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MILLSTONE.get(), MillstoneRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ScorpionModel.LAYER_LOCATION, ScorpionModel::createBodyLayer);
        event.registerLayerDefinition(ScarabModel.LAYER_LOCATION, ScarabModel::createBodyLayer);
        event.registerLayerDefinition(MeerkatModel.LAYER_LOCATION, MeerkatModel::createBodyLayer);
        event.registerLayerDefinition(VultureModel.LAYER_LOCATION, VultureModel::createBodyLayer);
        event.registerLayerDefinition(MagmaTitanModel.LAYER_LOCATION, MagmaTitanModel::createBodyLayer);
        event.registerLayerDefinition(SalamanderModel.LAYER_LOCATION, SalamanderModel::createBodyLayer);
        event.registerLayerDefinition(LavaCrabModel.LAYER_LOCATION, LavaCrabModel::createBodyLayer);
        event.registerLayerDefinition(MagmalingModel.LAYER_LOCATION, MagmalingModel::createBodyLayer);
        event.registerLayerDefinition(CinderWraithModel.LAYER_LOCATION, CinderWraithModel::createBodyLayer);
        event.registerLayerDefinition(ShadeModel.LAYER_LOCATION, ShadeModel::createBodyLayer);
        event.registerLayerDefinition(TricornModel.ILLAGER_HAT, TricornModel::createIllagerHat);
        event.registerLayerDefinition(TricornModel.ARMOR_HAT, TricornModel::createArmorHat);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(WaterWheelRenderer.WHEEL);
        event.register(MillstoneRenderer.RUNNER);
    }

    @SubscribeEvent
    public static void blockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> level != null && pos != null
                ? BiomeColors.getAverageWaterColor(level, pos) : 0x3F76E4, WorldBlocks.WATER_TROUGH.get());
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(WorldItems.TIDE_CLOCK.get(), DunesRelics.id("tide"),
                (stack, level, entity, seed) -> {
                    net.minecraft.world.level.Level world = level != null ? level : entity != null ? entity.level() : null;
                    return world == null ? 0.5F : TideClockItem.needle(world);
                }));
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SAND_GUST.get(), SandGustParticle.Provider::new);
    }
}
