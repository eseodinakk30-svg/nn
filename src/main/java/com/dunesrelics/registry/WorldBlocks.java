package com.dunesrelics.registry;

import com.dunesrelics.block.world.CannonBlock;
import com.dunesrelics.block.world.ClamBlock;
import com.dunesrelics.block.world.DreamcatcherBlock;
import com.dunesrelics.block.world.MillstoneBlock;
import com.dunesrelics.block.world.SeashellBlock;
import com.dunesrelics.block.world.WaterTroughBlock;
import com.dunesrelics.block.world.WaterWheelBlock;
import com.dunesrelics.block.world.WetSandBlock;
import com.dunesrelics.block.world.WindmillSailsBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.RegistryObject;

import static com.dunesrelics.registry.ModBlocks.register;

/** Blocks of the third update, "Living World": water mills, irrigation, tides, pirates and dreams. */
public final class WorldBlocks {
    // ---------------------------------------------------------------- Water power and irrigation
    public static final RegistryObject<Block> WATER_WHEEL = register("water_wheel", () -> new WaterWheelBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.0F)
                    .sound(SoundType.WOOD).noOcclusion().ignitedByLava().forceSolidOn()));
    public static final RegistryObject<Block> WINDMILL_SAILS = register("windmill_sails", () -> new WindmillSailsBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).instrument(NoteBlockInstrument.BASS).strength(1.5F)
                    .sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
    public static final RegistryObject<Block> MILLSTONE = register("millstone", () -> new MillstoneBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops().strength(2.5F, 6.0F).sound(SoundType.STONE).noOcclusion()));
    public static final RegistryObject<Block> WATER_TROUGH = register("water_trough", () -> new WaterTroughBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(1.0F)
                    .sound(SoundType.WOOD).noOcclusion().randomTicks().ignitedByLava().forceSolidOn()));

    // ---------------------------------------------------------------- Tides
    public static final RegistryObject<Block> WET_SAND = register("wet_sand", () -> new WetSandBlock(
            BlockBehaviour.Properties.copy(Blocks.SAND).mapColor(MapColor.SAND).randomTicks()));
    public static final RegistryObject<Block> SEASHELL = register("seashell", () -> new SeashellBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).instabreak().noCollission()
                    .sound(SoundType.CORAL_BLOCK).randomTicks().pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> CLAM = register("clam", () -> new ClamBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_LIGHT_GRAY).strength(0.4F).noOcclusion()
                    .sound(SoundType.BONE_BLOCK).randomTicks().pushReaction(PushReaction.DESTROY)));

    // ---------------------------------------------------------------- Pirates and dreams
    public static final RegistryObject<Block> CANNON = register("cannon", () -> new CannonBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(4.0F, 6.0F)
                    .sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<Block> DREAMCATCHER = register("dreamcatcher", () -> new DreamcatcherBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).instabreak().noCollission().sound(SoundType.WOOL)
                    .pushReaction(PushReaction.DESTROY)));

    private WorldBlocks() {}

    /** Forces the class to load so its blocks are added to the deferred register. */
    public static void init() {}
}
