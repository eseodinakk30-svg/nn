package com.dunesrelics.registry;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.block.AloeVeraBlock;
import com.dunesrelics.block.AncientUrnBlock;
import com.dunesrelics.block.CattailBlock;
import com.dunesrelics.block.DesertFlowerBlock;
import com.dunesrelics.block.DuneGrassBlock;
import com.dunesrelics.block.FlammableBlocks;
import com.dunesrelics.block.PalmLeavesBlock;
import com.dunesrelics.block.PalmLogBlock;
import com.dunesrelics.block.PalmSaplingBlock;
import com.dunesrelics.block.QuicksandBlock;
import com.dunesrelics.block.SarcophagusBlock;
import com.dunesrelics.block.grower.PalmTreeGrower;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;
import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, DunesRelics.MODID);

    // ---------------------------------------------------------------- Limestone
    public static final RegistryObject<Block> LIMESTONE = register("limestone", () -> new Block(limestone(1.2F, SoundType.CALCITE)));
    public static final RegistryObject<Block> LIMESTONE_STAIRS = stairs("limestone_stairs", LIMESTONE);
    public static final RegistryObject<Block> LIMESTONE_SLAB = register("limestone_slab", () -> new SlabBlock(limestone(1.2F, SoundType.CALCITE)));
    public static final RegistryObject<Block> LIMESTONE_WALL = register("limestone_wall", () -> new WallBlock(limestone(1.2F, SoundType.CALCITE)));

    public static final RegistryObject<Block> POLISHED_LIMESTONE = register("polished_limestone", () -> new Block(limestone(1.5F, SoundType.CALCITE)));
    public static final RegistryObject<Block> POLISHED_LIMESTONE_STAIRS = stairs("polished_limestone_stairs", POLISHED_LIMESTONE);
    public static final RegistryObject<Block> POLISHED_LIMESTONE_SLAB = register("polished_limestone_slab", () -> new SlabBlock(limestone(1.5F, SoundType.CALCITE)));
    public static final RegistryObject<Block> POLISHED_LIMESTONE_WALL = register("polished_limestone_wall", () -> new WallBlock(limestone(1.5F, SoundType.CALCITE)));

    public static final RegistryObject<Block> LIMESTONE_BRICKS = register("limestone_bricks", () -> new Block(limestone(1.5F, SoundType.STONE)));
    public static final RegistryObject<Block> LIMESTONE_BRICK_STAIRS = stairs("limestone_brick_stairs", LIMESTONE_BRICKS);
    public static final RegistryObject<Block> LIMESTONE_BRICK_SLAB = register("limestone_brick_slab", () -> new SlabBlock(limestone(1.5F, SoundType.STONE)));
    public static final RegistryObject<Block> LIMESTONE_BRICK_WALL = register("limestone_brick_wall", () -> new WallBlock(limestone(1.5F, SoundType.STONE)));

    public static final RegistryObject<Block> CRACKED_LIMESTONE_BRICKS = register("cracked_limestone_bricks", () -> new Block(limestone(1.5F, SoundType.STONE)));
    public static final RegistryObject<Block> CHISELED_LIMESTONE_BRICKS = register("chiseled_limestone_bricks", () -> new Block(limestone(1.5F, SoundType.STONE)));
    public static final RegistryObject<Block> LIMESTONE_PILLAR = register("limestone_pillar", () -> new RotatedPillarBlock(limestone(1.5F, SoundType.STONE)));
    public static final RegistryObject<Block> GILDED_LIMESTONE = register("gilded_limestone", () -> new Block(limestone(1.5F, SoundType.STONE).mapColor(MapColor.GOLD)));

    // ---------------------------------------------------------------- Amber & bronze
    public static final RegistryObject<Block> AMBER_ORE = register("amber_ore", () -> new DropExperienceBlock(
            limestone(2.5F, SoundType.CALCITE), UniformInt.of(2, 5)));
    public static final RegistryObject<Block> AMBER_BLOCK = register("amber_block", () -> new HalfTransparentBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.5F).sound(SoundType.AMETHYST)
                    .noOcclusion().isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false)
                    .isViewBlocking((s, l, p) -> false)));
    public static final RegistryObject<Block> AMBER_LAMP = register("amber_lamp", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.8F).sound(SoundType.GLASS)
                    .lightLevel(s -> 15)));
    public static final RegistryObject<Block> BRONZE_BLOCK = register("bronze_block", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).instrument(NoteBlockInstrument.BELL)
                    .requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.COPPER)));

    // ---------------------------------------------------------------- Palm wood
    public static final RegistryObject<Block> STRIPPED_PALM_LOG = register("stripped_palm_log",
            () -> new PalmLogBlock(BlockBehaviour.Properties.copy(Blocks.STRIPPED_OAK_LOG).mapColor(MapColor.SAND), null));
    public static final RegistryObject<Block> STRIPPED_PALM_WOOD = register("stripped_palm_wood",
            () -> new PalmLogBlock(BlockBehaviour.Properties.copy(Blocks.STRIPPED_OAK_WOOD).mapColor(MapColor.SAND), null));
    public static final RegistryObject<Block> PALM_LOG = register("palm_log",
            () -> new PalmLogBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG).mapColor(MapColor.TERRACOTTA_BROWN), STRIPPED_PALM_LOG));
    public static final RegistryObject<Block> PALM_WOOD = register("palm_wood",
            () -> new PalmLogBlock(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD).mapColor(MapColor.TERRACOTTA_BROWN), STRIPPED_PALM_WOOD));
    public static final RegistryObject<Block> PALM_PLANKS = register("palm_planks",
            () -> new FlammableBlocks.Planks(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).mapColor(MapColor.SAND)));
    public static final RegistryObject<Block> PALM_STAIRS = register("palm_stairs",
            () -> new FlammableBlocks.Stairs(() -> ModBlocks.PALM_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.OAK_STAIRS).mapColor(MapColor.SAND)));
    public static final RegistryObject<Block> PALM_SLAB = register("palm_slab",
            () -> new FlammableBlocks.Slab(BlockBehaviour.Properties.copy(Blocks.OAK_SLAB).mapColor(MapColor.SAND)));
    public static final RegistryObject<Block> PALM_FENCE = register("palm_fence",
            () -> new FlammableBlocks.Fence(BlockBehaviour.Properties.copy(Blocks.OAK_FENCE).mapColor(MapColor.SAND)));
    public static final RegistryObject<Block> PALM_FENCE_GATE = register("palm_fence_gate",
            () -> new FlammableBlocks.FenceGate(BlockBehaviour.Properties.copy(Blocks.OAK_FENCE_GATE).mapColor(MapColor.SAND)));
    public static final RegistryObject<Block> PALM_DOOR = registerWithItem("palm_door",
            () -> new DoorBlock(BlockBehaviour.Properties.copy(Blocks.OAK_DOOR).mapColor(MapColor.SAND), BlockSetType.OAK),
            block -> new DoubleHighBlockItem(block, new Item.Properties()));
    public static final RegistryObject<Block> PALM_TRAPDOOR = register("palm_trapdoor",
            () -> new TrapDoorBlock(BlockBehaviour.Properties.copy(Blocks.OAK_TRAPDOOR).mapColor(MapColor.SAND), BlockSetType.OAK));
    public static final RegistryObject<Block> PALM_BUTTON = register("palm_button",
            () -> new ButtonBlock(BlockBehaviour.Properties.copy(Blocks.OAK_BUTTON), BlockSetType.OAK, 30, true));
    public static final RegistryObject<Block> PALM_PRESSURE_PLATE = register("palm_pressure_plate",
            () -> new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING,
                    BlockBehaviour.Properties.copy(Blocks.OAK_PRESSURE_PLATE).mapColor(MapColor.SAND), BlockSetType.OAK));
    public static final RegistryObject<Block> PALM_LEAVES = register("palm_leaves",
            () -> new PalmLeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES).mapColor(MapColor.PLANT)));
    public static final RegistryObject<Block> PALM_SAPLING = register("palm_sapling",
            () -> new PalmSaplingBlock(new PalmTreeGrower(), BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)));
    public static final RegistryObject<Block> POTTED_PALM_SAPLING = BLOCKS.register("potted_palm_sapling",
            () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, PALM_SAPLING, BlockBehaviour.Properties.copy(Blocks.POTTED_OAK_SAPLING)));

    // ---------------------------------------------------------------- Plants
    public static final RegistryObject<Block> DUNE_GRASS = register("dune_grass", () -> new DuneGrassBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).replaceable().noCollission().instabreak()
                    .sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XYZ).ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> DESERT_ROSE = register("desert_rose",
            () -> new DesertFlowerBlock(BlockBehaviour.Properties.copy(Blocks.POPPY)));
    public static final RegistryObject<Block> POTTED_DESERT_ROSE = BLOCKS.register("potted_desert_rose",
            () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, DESERT_ROSE, BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY)));
    public static final RegistryObject<Block> ALOE_VERA = register("aloe_vera", () -> new AloeVeraBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).randomTicks().noCollission().instabreak()
                    .sound(SoundType.SWEET_BERRY_BUSH).offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> CATTAIL = registerWithItem("cattail", () -> new CattailBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).replaceable().noCollission().instabreak()
                            .sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ).ignitedByLava()
                            .pushReaction(PushReaction.DESTROY)),
            block -> new DoubleHighBlockItem(block, new Item.Properties()));

    // ---------------------------------------------------------------- Ancient things
    public static final RegistryObject<Block> QUICKSAND = register("quicksand", () -> new QuicksandBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.6F).sound(SoundType.SAND)
                    .dynamicShape().isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false)
                    .isViewBlocking((s, l, p) -> true)));
    public static final RegistryObject<Block> ANCIENT_URN = register("ancient_urn", () -> new AncientUrnBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(0.5F)
                    .sound(SoundType.DECORATED_POT).noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> SARCOPHAGUS = register("sarcophagus", () -> new SarcophagusBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(4.0F, 1200.0F).sound(SoundType.STONE).noOcclusion()));

    private ModBlocks() {}

    private static BlockBehaviour.Properties limestone(float hardness, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops().strength(hardness, 6.0F).sound(sound);
    }

    public static RegistryObject<Block> stairs(String name, RegistryObject<Block> base) {
        return register(name, () -> new StairBlock(() -> base.get().defaultBlockState(), BlockBehaviour.Properties.copy(base.get())));
    }

    public static <T extends Block> RegistryObject<T> register(String name, Supplier<T> block) {
        return registerWithItem(name, block, b -> new BlockItem(b, new Item.Properties()));
    }

    public static <T extends Block> RegistryObject<T> registerWithItem(String name, Supplier<T> block, Function<T, Item> item) {
        RegistryObject<T> object = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> item.apply(object.get()));
        return object;
    }

    public static void registerFlowerPots() {
        FlowerPotBlock pot = (FlowerPotBlock) Blocks.FLOWER_POT;
        pot.addPlant(PALM_SAPLING.getId(), POTTED_PALM_SAPLING);
        pot.addPlant(DESERT_ROSE.getId(), POTTED_DESERT_ROSE);
    }
}
