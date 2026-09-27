package com.dunesrelics.event;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.entity.Meerkat;
import com.dunesrelics.entity.Mummy;
import com.dunesrelics.entity.Pharaoh;
import com.dunesrelics.entity.Scarab;
import com.dunesrelics.entity.Scorpion;
import com.dunesrelics.entity.Vulture;
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
    }
}
