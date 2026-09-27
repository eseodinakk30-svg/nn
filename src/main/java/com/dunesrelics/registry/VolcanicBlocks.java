package com.dunesrelics.registry;

import com.dunesrelics.block.volcanic.AshLayerBlock;
import com.dunesrelics.block.volcanic.CooledLavaCrustBlock;
import com.dunesrelics.block.volcanic.EmberLogBlock;
import com.dunesrelics.block.volcanic.EmberSaplingBlock;
import com.dunesrelics.block.volcanic.FirePepperBushBlock;
import com.dunesrelics.block.volcanic.FireblossomBlock;
import com.dunesrelics.block.volcanic.HeartOfTheVolcanoBlock;
import com.dunesrelics.block.volcanic.LavaLilyBlock;
import com.dunesrelics.block.volcanic.MoltenPumiceSpongeBlock;
import com.dunesrelics.block.volcanic.PumiceSpongeBlock;
import com.dunesrelics.item.volcanic.MoltenPumiceItem;
import com.dunesrelics.block.volcanic.SteamVentBlock;
import com.dunesrelics.block.volcanic.VolcanicForgeBlock;
import com.dunesrelics.block.volcanic.VolcanicPlantBlock;
import com.dunesrelics.block.grower.EmberTreeGrower;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MagmaBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SandBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.RegistryObject;

import static com.dunesrelics.registry.ModBlocks.register;
import static com.dunesrelics.registry.ModBlocks.registerWithItem;
import static com.dunesrelics.registry.ModBlocks.stairs;

/** Blocks of the second update, "Ash & Ember": volcanoes, ash fields and the magma caverns. */
public final class VolcanicBlocks {
    // ---------------------------------------------------------------- Scoria
    public static final RegistryObject<Block> SCORIA = register("scoria", () -> new Block(stone(MapColor.COLOR_RED, 1.5F, SoundType.TUFF)));
    public static final RegistryObject<Block> SCORIA_STAIRS = stairs("scoria_stairs", SCORIA);
    public static final RegistryObject<Block> SCORIA_SLAB = register("scoria_slab", () -> new SlabBlock(stone(MapColor.COLOR_RED, 1.5F, SoundType.TUFF)));
    public static final RegistryObject<Block> SCORIA_WALL = register("scoria_wall", () -> new WallBlock(stone(MapColor.COLOR_RED, 1.5F, SoundType.TUFF)));
    public static final RegistryObject<Block> POLISHED_SCORIA = register("polished_scoria", () -> new Block(stone(MapColor.COLOR_RED, 1.5F, SoundType.POLISHED_DEEPSLATE)));
    public static final RegistryObject<Block> POLISHED_SCORIA_STAIRS = stairs("polished_scoria_stairs", POLISHED_SCORIA);
    public static final RegistryObject<Block> POLISHED_SCORIA_SLAB = register("polished_scoria_slab", () -> new SlabBlock(stone(MapColor.COLOR_RED, 1.5F, SoundType.POLISHED_DEEPSLATE)));
    public static final RegistryObject<Block> POLISHED_SCORIA_WALL = register("polished_scoria_wall", () -> new WallBlock(stone(MapColor.COLOR_RED, 1.5F, SoundType.POLISHED_DEEPSLATE)));
    public static final RegistryObject<Block> SCORIA_BRICKS = register("scoria_bricks", () -> new Block(stone(MapColor.COLOR_RED, 1.8F, SoundType.DEEPSLATE_BRICKS)));
    public static final RegistryObject<Block> SCORIA_BRICK_STAIRS = stairs("scoria_brick_stairs", SCORIA_BRICKS);
    public static final RegistryObject<Block> SCORIA_BRICK_SLAB = register("scoria_brick_slab", () -> new SlabBlock(stone(MapColor.COLOR_RED, 1.8F, SoundType.DEEPSLATE_BRICKS)));
    public static final RegistryObject<Block> SCORIA_BRICK_WALL = register("scoria_brick_wall", () -> new WallBlock(stone(MapColor.COLOR_RED, 1.8F, SoundType.DEEPSLATE_BRICKS)));
    public static final RegistryObject<Block> CRACKED_SCORIA_BRICKS = register("cracked_scoria_bricks", () -> new Block(stone(MapColor.COLOR_RED, 1.8F, SoundType.DEEPSLATE_BRICKS)));
    public static final RegistryObject<Block> CHISELED_SCORIA_BRICKS = register("chiseled_scoria_bricks", () -> new Block(stone(MapColor.COLOR_RED, 1.8F, SoundType.DEEPSLATE_BRICKS)));
    public static final RegistryObject<Block> MOLTEN_SCORIA = register("molten_scoria", () -> new MagmaBlock(
            stone(MapColor.NETHER, 1.5F, SoundType.TUFF).lightLevel(s -> 9).hasPostProcess((s, l, p) -> true).emissiveRendering((s, l, p) -> true)));

    // ---------------------------------------------------------------- Pumice
    public static final RegistryObject<Block> PUMICE = register("pumice", () -> new Block(stone(MapColor.QUARTZ, 0.8F, SoundType.CALCITE)));
    public static final RegistryObject<Block> PUMICE_BRICKS = register("pumice_bricks", () -> new Block(stone(MapColor.QUARTZ, 1.0F, SoundType.CALCITE)));
    public static final RegistryObject<Block> PUMICE_BRICK_STAIRS = stairs("pumice_brick_stairs", PUMICE_BRICKS);
    public static final RegistryObject<Block> PUMICE_BRICK_SLAB = register("pumice_brick_slab", () -> new SlabBlock(stone(MapColor.QUARTZ, 1.0F, SoundType.CALCITE)));

    /** Soaks up lava like a sponge soaks up water. */
    public static final RegistryObject<Block> PUMICE_SPONGE = register("pumice_sponge", () -> new PumiceSpongeBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(0.8F).sound(SoundType.CALCITE)));
    public static final RegistryObject<Block> MOLTEN_PUMICE_SPONGE = registerWithItem("molten_pumice_sponge", () -> new MoltenPumiceSpongeBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.FIRE).strength(0.8F).sound(SoundType.CALCITE).lightLevel(s -> 12)
                            .hasPostProcess((s, l, p) -> true).emissiveRendering((s, l, p) -> true)),
            block -> new MoltenPumiceItem(block, new Item.Properties().stacksTo(16)));

    // ---------------------------------------------------------------- Obsidian & glass
    public static final RegistryObject<Block> OBSIDIAN_BRICKS = register("obsidian_bricks", () -> new Block(obsidian()));
    public static final RegistryObject<Block> OBSIDIAN_BRICK_STAIRS = stairs("obsidian_brick_stairs", OBSIDIAN_BRICKS);
    public static final RegistryObject<Block> OBSIDIAN_BRICK_SLAB = register("obsidian_brick_slab", () -> new SlabBlock(obsidian()));
    public static final RegistryObject<Block> OBSIDIAN_BRICK_WALL = register("obsidian_brick_wall", () -> new WallBlock(obsidian()));
    public static final RegistryObject<Block> CHISELED_OBSIDIAN = register("chiseled_obsidian", () -> new Block(obsidian()));
    public static final RegistryObject<Block> VOLCANIC_GLASS = register("volcanic_glass", () -> new GlassBlock(BlockBehaviour.Properties.copy(Blocks.GLASS).mapColor(MapColor.COLOR_BLACK)));
    public static final RegistryObject<Block> VOLCANIC_GLASS_PANE = register("volcanic_glass_pane", () -> new IronBarsBlock(BlockBehaviour.Properties.copy(Blocks.GLASS_PANE).mapColor(MapColor.COLOR_BLACK)) {});

    // ---------------------------------------------------------------- Ash & sand
    public static final RegistryObject<Block> ASH_BLOCK = register("ash_block", () -> new SandBlock(0x5A5654,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.5F).sound(SoundType.SAND)));
    public static final RegistryObject<Block> ASH_LAYER = register("ash_layer", () -> new AshLayerBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).replaceable().strength(0.1F)
                    .sound(SoundType.SAND).isViewBlocking((s, l, p) -> s.getValue(AshLayerBlock.LAYERS) >= 8)
                    .pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> BLACK_SAND = register("black_sand", () -> new SandBlock(0x25221F,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.5F).sound(SoundType.SAND)));

    // ---------------------------------------------------------------- Ores & minerals
    public static final RegistryObject<Block> FIRE_OPAL_ORE = register("fire_opal_ore", () -> new DropExperienceBlock(
            stone(MapColor.COLOR_RED, 3.0F, SoundType.TUFF), UniformInt.of(3, 7)));
    public static final RegistryObject<Block> DEEPSLATE_FIRE_OPAL_ORE = register("deepslate_fire_opal_ore", () -> new DropExperienceBlock(
            BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_DIAMOND_ORE), UniformInt.of(3, 7)));
    public static final RegistryObject<Block> FIRE_OPAL_BLOCK = register("fire_opal_block", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).requiresCorrectToolForDrops().strength(5.0F, 6.0F)
                    .sound(SoundType.AMETHYST).lightLevel(s -> 7)));
    public static final RegistryObject<Block> SULFUR_ORE = register("sulfur_ore", () -> new DropExperienceBlock(
            stone(MapColor.COLOR_YELLOW, 2.0F, SoundType.TUFF), UniformInt.of(1, 3)));
    public static final RegistryObject<Block> SULFUR_BLOCK = register("sulfur_block", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1.0F).sound(SoundType.CALCITE)));
    public static final RegistryObject<Block> SULFUR_CLUSTER = register("sulfur_cluster", () -> new VolcanicPlantBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).noCollission().instabreak()
                    .sound(SoundType.AMETHYST_CLUSTER).offsetType(BlockBehaviour.OffsetType.XZ).lightLevel(s -> 3)
                    .pushReaction(PushReaction.DESTROY), 9.0D));

    // ---------------------------------------------------------------- Emberwood (does not burn)
    public static final RegistryObject<Block> STRIPPED_EMBER_LOG = register("stripped_ember_log",
            () -> new EmberLogBlock(ember(MapColor.COLOR_ORANGE, 2.0F), null));
    public static final RegistryObject<Block> STRIPPED_EMBER_WOOD = register("stripped_ember_wood",
            () -> new EmberLogBlock(ember(MapColor.COLOR_ORANGE, 2.0F), null));
    public static final RegistryObject<Block> EMBER_LOG = register("ember_log",
            () -> new EmberLogBlock(ember(MapColor.COLOR_BLACK, 2.0F), STRIPPED_EMBER_LOG));
    public static final RegistryObject<Block> EMBER_WOOD = register("ember_wood",
            () -> new EmberLogBlock(ember(MapColor.COLOR_BLACK, 2.0F), STRIPPED_EMBER_WOOD));
    public static final RegistryObject<Block> EMBER_PLANKS = register("ember_planks", () -> new Block(ember(MapColor.COLOR_ORANGE, 2.0F)));
    public static final RegistryObject<Block> EMBER_STAIRS = stairs("ember_stairs", EMBER_PLANKS);
    public static final RegistryObject<Block> EMBER_SLAB = register("ember_slab", () -> new SlabBlock(ember(MapColor.COLOR_ORANGE, 2.0F)));
    public static final RegistryObject<Block> EMBER_FENCE = register("ember_fence", () -> new FenceBlock(ember(MapColor.COLOR_ORANGE, 2.0F)));
    public static final RegistryObject<Block> EMBER_FENCE_GATE = register("ember_fence_gate",
            () -> new FenceGateBlock(ember(MapColor.COLOR_ORANGE, 2.0F), WoodType.CRIMSON));
    public static final RegistryObject<Block> EMBER_DOOR = registerWithItem("ember_door",
            () -> new DoorBlock(ember(MapColor.COLOR_ORANGE, 3.0F).noOcclusion(), BlockSetType.CRIMSON),
            block -> new DoubleHighBlockItem(block, new Item.Properties()));
    public static final RegistryObject<Block> EMBER_TRAPDOOR = register("ember_trapdoor",
            () -> new TrapDoorBlock(ember(MapColor.COLOR_ORANGE, 3.0F).noOcclusion(), BlockSetType.CRIMSON));
    public static final RegistryObject<Block> EMBER_BUTTON = register("ember_button",
            () -> new ButtonBlock(BlockBehaviour.Properties.copy(Blocks.CRIMSON_BUTTON), BlockSetType.CRIMSON, 30, true));
    public static final RegistryObject<Block> EMBER_PRESSURE_PLATE = register("ember_pressure_plate",
            () -> new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING,
                    BlockBehaviour.Properties.copy(Blocks.CRIMSON_PRESSURE_PLATE), BlockSetType.CRIMSON));
    public static final RegistryObject<Block> EMBER_LEAVES = register("ember_leaves", () -> new LeavesBlock(
            BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES).mapColor(MapColor.FIRE).lightLevel(s -> 6)
                    .sound(SoundType.AZALEA_LEAVES).ignitedByLava()));
    public static final RegistryObject<Block> EMBER_SAPLING = register("ember_sapling",
            () -> new EmberSaplingBlock(new EmberTreeGrower(), BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING).lightLevel(s -> 4)));
    public static final RegistryObject<Block> POTTED_EMBER_SAPLING = ModBlocks.BLOCKS.register("potted_ember_sapling",
            () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, EMBER_SAPLING, BlockBehaviour.Properties.copy(Blocks.POTTED_OAK_SAPLING).lightLevel(s -> 4)));

    // ---------------------------------------------------------------- Plants
    public static final RegistryObject<Block> ASH_GRASS = register("ash_grass", () -> new VolcanicPlantBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).replaceable().noCollission().instabreak()
                    .sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XYZ).pushReaction(PushReaction.DESTROY), 12.0D));
    public static final RegistryObject<Block> FIREBLOSSOM = register("fireblossom", () -> new FireblossomBlock(
            BlockBehaviour.Properties.copy(Blocks.POPPY).lightLevel(s -> 7)));
    public static final RegistryObject<Block> POTTED_FIREBLOSSOM = ModBlocks.BLOCKS.register("potted_fireblossom",
            () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, FIREBLOSSOM, BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY).lightLevel(s -> 7)));
    public static final RegistryObject<Block> LAVA_LILY = registerWithItem("lava_lily", () -> new LavaLilyBlock(
                    BlockBehaviour.Properties.copy(Blocks.LILY_PAD).mapColor(MapColor.FIRE).lightLevel(s -> 5)),
            block -> new PlaceOnWaterBlockItem(block, new Item.Properties()));
    public static final RegistryObject<Block> FIRE_PEPPER_BUSH = ModBlocks.BLOCKS.register("fire_pepper_bush", () -> new FirePepperBushBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).randomTicks().noCollission().instabreak()
                    .sound(SoundType.SWEET_BERRY_BUSH).pushReaction(PushReaction.DESTROY)));

    // ---------------------------------------------------------------- Mechanics
    public static final RegistryObject<Block> STEAM_VENT = register("steam_vent", () -> new SteamVentBlock(
            stone(MapColor.COLOR_GRAY, 1.5F, SoundType.TUFF)));
    public static final RegistryObject<Block> VOLCANIC_FORGE = register("volcanic_forge", () -> new VolcanicForgeBlock(
            stone(MapColor.COLOR_BLACK, 3.5F, SoundType.DEEPSLATE_BRICKS).noOcclusion()
                    .lightLevel(s -> s.getValue(VolcanicForgeBlock.LAVA) > 0 ? 13 : 0)));
    public static final RegistryObject<Block> HEART_OF_THE_VOLCANO = register("heart_of_the_volcano", () -> new HeartOfTheVolcanoBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.FIRE).instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(5.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 15)
                    .hasPostProcess((s, l, p) -> true).emissiveRendering((s, l, p) -> true)));
    public static final RegistryObject<Block> COOLED_LAVA_CRUST = ModBlocks.BLOCKS.register("cooled_lava_crust", () -> new CooledLavaCrustBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.5F).sound(SoundType.BASALT)
                    .lightLevel(s -> 4 + s.getValue(CooledLavaCrustBlock.AGE) * 3).randomTicks().noLootTable()
                    .pushReaction(PushReaction.DESTROY)));

    private VolcanicBlocks() {}

    /** Forces the class to load so its blocks are added to the deferred register. */
    public static void init() {}

    private static BlockBehaviour.Properties stone(MapColor color, float hardness, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(hardness, 6.0F).sound(sound);
    }

    private static BlockBehaviour.Properties obsidian() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(35.0F, 1200.0F);
    }

    private static BlockBehaviour.Properties ember(MapColor color, float hardness) {
        return BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASS).strength(hardness)
                .sound(SoundType.NETHER_WOOD);
    }

    public static void registerFlowerPots() {
        FlowerPotBlock pot = (FlowerPotBlock) Blocks.FLOWER_POT;
        pot.addPlant(EMBER_SAPLING.getId(), POTTED_EMBER_SAPLING);
        pot.addPlant(FIREBLOSSOM.getId(), POTTED_FIREBLOSSOM);
    }
}
