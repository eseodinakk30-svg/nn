package com.dunesrelics.event;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.entity.Meerkat;
import com.dunesrelics.entity.Mummy;
import com.dunesrelics.entity.Pharaoh;
import com.dunesrelics.entity.Scarab;
import com.dunesrelics.entity.Scorpion;
import com.dunesrelics.entity.Vulture;
import com.dunesrelics.entity.volcanic.CinderWraith;
import com.dunesrelics.entity.volcanic.LavaCrab;
import com.dunesrelics.entity.volcanic.MagmaTitan;
import com.dunesrelics.entity.volcanic.Magmaling;
import com.dunesrelics.entity.volcanic.Salamander;
import com.dunesrelics.entity.world.Pirate;
import com.dunesrelics.entity.world.PirateCaptain;
import com.dunesrelics.entity.world.PirateGunner;
import com.dunesrelics.entity.world.Shade;
import net.minecraft.world.entity.npc.WanderingTrader;
import com.dunesrelics.registry.ModEntities;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DunesRelics.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEvents {
    private ModEvents() {}

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.MUMMY.get(), Mummy.createAttributes().build());
        event.put(ModEntities.PHARAOH.get(), Pharaoh.createAttributes().build());
        event.put(ModEntities.SCORPION.get(), Scorpion.createAttributes().build());
        event.put(ModEntities.SCARAB.get(), Scarab.createAttributes().build());
        event.put(ModEntities.MEERKAT.get(), Meerkat.createAttributes().build());
        event.put(ModEntities.VULTURE.get(), Vulture.createAttributes().build());
        event.put(ModEntities.MAGMA_TITAN.get(), MagmaTitan.createAttributes().build());
        event.put(ModEntities.SALAMANDER.get(), Salamander.createAttributes().build());
        event.put(ModEntities.LAVA_CRAB.get(), LavaCrab.createAttributes().build());
        event.put(ModEntities.MAGMALING.get(), Magmaling.createAttributes().build());
        event.put(ModEntities.CINDER_WRAITH.get(), CinderWraith.createAttributes().build());
        event.put(ModEntities.PIRATE.get(), Pirate.createAttributes().build());
        event.put(ModEntities.PIRATE_GUNNER.get(), PirateGunner.createAttributes().build());
        event.put(ModEntities.PIRATE_CAPTAIN.get(), PirateCaptain.createAttributes().build());
        event.put(ModEntities.TRAVELER.get(), WanderingTrader.createMobAttributes().build());
        event.put(ModEntities.SHADE.get(), Shade.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(ModEntities.MUMMY.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mummy::checkMummySpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.SCORPION.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.SCARAB.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.MEERKAT.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Meerkat::checkMeerkatSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.VULTURE.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Vulture::checkVultureSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.SALAMANDER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> !level.getBlockState(pos.below()).isAir(), SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.LAVA_CRAB.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                LavaCrab::checkCrabSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.MAGMALING.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.CINDER_WRAITH.get(), SpawnPlacements.Type.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.SHADE.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Shade::checkShadeSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.PIRATE.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.PIRATE_GUNNER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
