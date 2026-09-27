package com.dunesrelics.registry;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.block.entity.CannonBlockEntity;
import com.dunesrelics.block.entity.MillstoneBlockEntity;
import com.dunesrelics.block.entity.WaterWheelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DunesRelics.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<WaterWheelBlockEntity>> WATER_WHEEL = BLOCK_ENTITIES.register("water_wheel",
            () -> BlockEntityType.Builder.of(WaterWheelBlockEntity::new, WorldBlocks.WATER_WHEEL.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<MillstoneBlockEntity>> MILLSTONE = BLOCK_ENTITIES.register("millstone",
            () -> BlockEntityType.Builder.of(MillstoneBlockEntity::new, WorldBlocks.MILLSTONE.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<CannonBlockEntity>> CANNON = BLOCK_ENTITIES.register("cannon",
            () -> BlockEntityType.Builder.of(CannonBlockEntity::new, WorldBlocks.CANNON.get()).build(null));

    private ModBlockEntities() {}
}
