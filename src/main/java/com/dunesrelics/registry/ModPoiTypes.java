package com.dunesrelics.registry;

import com.dunesrelics.DunesRelics;
import com.google.common.collect.ImmutableSet;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModPoiTypes {
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, DunesRelics.MODID);

    /** Lets spawn checks find a dreamcatcher nearby without scanning blocks. */
    public static final RegistryObject<PoiType> DREAMCATCHER = POI_TYPES.register("dreamcatcher",
            () -> new PoiType(ImmutableSet.copyOf(WorldBlocks.DREAMCATCHER.get().getStateDefinition().getPossibleStates()), 0, 1));

    private ModPoiTypes() {}
}
