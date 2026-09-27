package com.dunesrelics.gametest;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.block.entity.CannonBlockEntity;
import com.dunesrelics.block.world.CannonBlock;
import com.dunesrelics.block.world.WaterTroughBlock;
import com.dunesrelics.block.world.WaterWheelBlock;
import com.dunesrelics.entity.world.Shade;
import com.dunesrelics.entity.world.VillageWorker;
import com.dunesrelics.memory.Blueprint;
import com.dunesrelics.memory.Builders;
import com.dunesrelics.memory.Construction;
import com.dunesrelics.memory.VillageLife;
import com.dunesrelics.memory.WorldMemory;
import com.dunesrelics.registry.ModEntities;
import com.dunesrelics.registry.WorldBlocks;
import com.dunesrelics.registry.WorldItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** In-game tests for the living world update: mills, troughs, cannons, villages and the Shade. */
@GameTestHolder(DunesRelics.MODID)
@PrefixGameTestTemplate(false)
public final class LivingWorldGameTests {
    private static final String ARENA = "arena";

    private LivingWorldGameTests() {}

    private static void fill(GameTestHelper helper, int fromY, int toY, Block block) {
        for (int x = 0; x < 24; x++) {
            for (int z = 0; z < 24; z++) {
                for (int y = fromY; y <= toY; y++) {
                    helper.setBlock(new BlockPos(x, y, z), block);
                }
            }
        }
    }

    private static int count(GameTestHelper helper, Block block) {
        int found = 0;
        for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 23, 23, 23)) {
            if (helper.getBlockState(pos).is(block)) {
                found++;
            }
        }
        return found;
    }

    private static void use(GameTestHelper helper, BlockPos pos, Player player) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = helper.getBlockState(pos);
        state.use(helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
    }

    /** A water source with glass on every side but {@code open}, so it feeds one block and floods nothing. */
    private static void cupOfWater(GameTestHelper helper, BlockPos pos, Direction open) {
        for (Direction direction : Direction.values()) {
            if (direction != open && direction != Direction.UP) {
                helper.setBlock(pos.relative(direction), Blocks.GLASS);
            }
        }
        helper.setBlock(pos, Blocks.WATER);
    }

    /** A hollow stone room with a 3x3x3 inside, starting at {@code min}. */
    private static void room(GameTestHelper helper, BlockPos min) {
        for (BlockPos pos : BlockPos.betweenClosed(min, min.offset(4, 4, 4))) {
            boolean inside = pos.getX() > min.getX() && pos.getX() < min.getX() + 4 && pos.getY() > min.getY()
                    && pos.getY() < min.getY() + 4 && pos.getZ() > min.getZ() && pos.getZ() < min.getZ() + 4;
            helper.setBlock(pos.immutable(), inside ? Blocks.AIR : Blocks.STONE);
        }
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void waterWheelTurnsInAStreamAndIsNotWashedAway(GameTestHelper helper) {
        fill(helper, 1, 2, Blocks.STONE);
        BlockPos wheel = new BlockPos(10, 3, 10);
        helper.setBlock(wheel, WorldBlocks.WATER_WHEEL.get().defaultBlockState().setValue(WaterWheelBlock.AXIS, Direction.Axis.Z));
        // the stream runs into the paddles beside the hub (and would run into the hub itself if it could)
        helper.setBlock(new BlockPos(8, 3, 10), Blocks.WATER);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(WorldBlocks.WATER_WHEEL.get(), wheel);
            helper.assertBlockProperty(wheel, WaterWheelBlock.SPINNING, true);
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void millGrindsWheatIntoFlourThroughHoppers(GameTestHelper helper) {
        fill(helper, 1, 1, Blocks.STONE);
        // a trough fed by a water source pours onto a wheel, whose axle turns the millstone beside it
        BlockPos wheel = new BlockPos(10, 3, 10);
        BlockPos trough = wheel.above();
        BlockPos mill = wheel.west();
        cupOfWater(helper, trough.east(), Direction.WEST);
        helper.setBlock(trough, WorldBlocks.WATER_TROUGH.get());
        helper.setBlock(wheel, WorldBlocks.WATER_WHEEL.get().defaultBlockState().setValue(WaterWheelBlock.AXIS, Direction.Axis.X));
        helper.setBlock(mill, WorldBlocks.MILLSTONE.get());
        // a hopper feeds grain in from the top, another takes flour out of the bottom into a chest
        helper.setBlock(mill.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        helper.setBlock(mill.below(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.WEST));
        BlockPos chest = mill.below().west();
        helper.setBlock(chest, Blocks.CHEST);
        ((HopperBlockEntity) helper.getBlockEntity(mill.above())).setItem(0, new ItemStack(Items.WHEAT, 2));

        helper.succeedWhen(() -> {
            helper.assertBlockProperty(wheel, WaterWheelBlock.SPINNING, true);
            ChestBlockEntity box = (ChestBlockEntity) helper.getBlockEntity(chest);
            helper.assertTrue(box.countItem(WorldItems.FLOUR.get()) == 2, "the mill did not grind both wheat into flour");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 800)
    public static void troughsCarryWaterAndDrainWhenTheSourceGoes(GameTestHelper helper) {
        fill(helper, 1, 1, Blocks.STONE);
        BlockPos source = new BlockPos(5, 2, 5);
        cupOfWater(helper, source, Direction.EAST);
        for (int x = 6; x <= 11; x++) {
            helper.setBlock(new BlockPos(x, 2, 5), WorldBlocks.WATER_TROUGH.get());
        }
        helper.startSequence()
                .thenWaitUntil(() -> {
                    for (int x = 6; x <= 11; x++) {
                        helper.assertBlockProperty(new BlockPos(x, 2, 5), WaterTroughBlock.WATER, WaterTroughBlock.MAX_WATER - (x - 6));
                    }
                })
                .thenExecute(() -> helper.setBlock(source, Blocks.AIR))
                .thenWaitUntil(() -> {
                    for (int x = 6; x <= 11; x++) {
                        helper.assertBlockProperty(new BlockPos(x, 2, 5), WaterTroughBlock.WATER, 0);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void troughIrrigatesNearbyFarmland(GameTestHelper helper) {
        fill(helper, 1, 1, Blocks.STONE);
        BlockPos trough = new BlockPos(6, 2, 12);
        BlockPos near = new BlockPos(9, 1, 12);
        BlockPos far = new BlockPos(13, 1, 12);
        helper.setBlock(trough, WorldBlocks.WATER_TROUGH.get());
        helper.setBlock(near, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0));
        helper.setBlock(far, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0));
        ServerLevel level = helper.getLevel();
        WaterTroughBlock.irrigate(level, helper.absolutePos(trough), level.random);
        helper.assertBlockProperty(near, FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE);
        helper.assertBlockProperty(far, FarmBlock.MOISTURE, 0);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void cannonLoadsAndFiresOnRedstone(GameTestHelper helper) {
        fill(helper, 1, 1, Blocks.STONE);
        for (int y = 2; y <= 7; y++) {
            for (int z = 8; z <= 16; z++) {
                helper.setBlock(new BlockPos(16, y, z), Blocks.STONE);
            }
        }
        BlockPos pos = new BlockPos(4, 2, 12);
        helper.setBlock(pos, WorldBlocks.CANNON.get().defaultBlockState().setValue(CannonBlock.FACING, Direction.EAST));
        Player player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GUNPOWDER));
        use(helper, pos, player);
        helper.assertBlockProperty(pos, CannonBlock.LOADED, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(WorldItems.CANNONBALL.get()));
        use(helper, pos, player);
        helper.assertBlockProperty(pos, CannonBlock.LOADED, true);
        helper.assertTrue(player.getMainHandItem().isEmpty(), "the cannonball was not used up");

        helper.setBlock(pos.south(), Blocks.REDSTONE_BLOCK);
        helper.assertEntityPresent(ModEntities.CANNONBALL.get());
        helper.assertBlockProperty(pos, CannonBlock.LOADED, false);
        helper.assertFalse(((CannonBlockEntity) helper.getBlockEntity(pos)).isLoaded(), "the cannon is still loaded");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void villagersBuildHousesWithABedADoorAndAWorkplace(GameTestHelper helper) {
        fill(helper, 1, 1, Blocks.STONE);
        BlockPos min = helper.absolutePos(new BlockPos(4, 2, 4));
        BlockPos step = Builders.house(helper.getLevel(), min, min.getY(), Direction.SOUTH, Builders.OAK, Blocks.BARREL);
        helper.assertTrue(step != null, "no front step");
        helper.assertTrue(count(helper, Blocks.RED_BED) == 2, "the house has no bed");
        helper.assertTrue(count(helper, Blocks.OAK_DOOR) == 2, "the house has no door");
        helper.assertTrue(count(helper, Blocks.BARREL) == 1, "the house has no workplace");
        helper.assertTrue(count(helper, Blocks.LANTERN) == 1, "the house has no lantern");
        helper.assertTrue(count(helper, Blocks.GLASS_PANE) >= 4, "the house has no windows");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void villagerRemembersGiftsButOnlyThreeADay(GameTestHelper helper) {
        fill(helper, 1, 1, Blocks.STONE);
        Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(12, 2, 12));
        Player player = helper.makeMockPlayer();
        ServerLevel level = helper.getLevel();
        long day = level.getDayTime() / 24000L;

        helper.assertFalse(VillageLife.receiveGift(level, villager, player, new ItemStack(Items.DIRT), day), "dirt is not a gift");
        ItemStack flowers = new ItemStack(Items.POPPY, 8);
        for (int i = 0; i < 3; i++) {
            helper.assertTrue(VillageLife.receiveGift(level, villager, player, flowers, day), "the villager refused a flower");
        }
        int reputation = villager.getPlayerReputation(player);
        helper.assertTrue(reputation > 0, "the villager forgot the gifts");
        helper.assertTrue(flowers.getCount() == 5, "the flowers were not handed over");

        VillageLife.receiveGift(level, villager, player, flowers, day);
        helper.assertTrue(villager.getPlayerReputation(player) == reputation, "a fourth gift in a day still counted");
        helper.assertTrue(flowers.getCount() == 5, "the villager took a fourth flower");
        helper.assertTrue(villager.getPersistentData().contains(VillageLife.NAME), "the villager has no name");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void blueprintsAreLaidFromTheGroundUp(GameTestHelper helper) {
        fill(helper, 1, 1, Blocks.GRASS_BLOCK);
        ServerLevel level = helper.getLevel();
        BlockPos min = helper.absolutePos(new BlockPos(4, 1, 4));
        Blueprint house = Builders.record(level, "house",
                () -> Builders.house(level, min, min.getY(), Direction.SOUTH, Builders.OAK, Blocks.BARREL));
        helper.assertTrue(house.size() > 100, "the house blueprint is too small: " + house.size());
        helper.assertBlockPresent(Blocks.GRASS_BLOCK, new BlockPos(4, 1, 4));
        helper.assertTrue(count(helper, Blocks.RED_BED) == 0, "recording a blueprint built something");
        int lastY = Integer.MIN_VALUE;
        for (int i = 0; i < house.size(); i++) {
            if (house.state(i).isAir()) {
                continue;
            }
            helper.assertTrue(house.pos(i).getY() >= lastY, "the blueprint is not laid course by course");
            lastY = house.pos(i).getY();
        }
        Construction.buildNow(level, house);
        helper.assertTrue(count(helper, Blocks.RED_BED) == 2, "the bed is not whole");
        helper.assertTrue(count(helper, Blocks.OAK_DOOR) == 2, "the door is not whole");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 2400)
    public static void builderPutsUpAWellBlockByBlock(GameTestHelper helper) {
        fill(helper, 0, 2, Blocks.GRASS_BLOCK);
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(4, 3, 4), Blocks.BELL);
        BlockPos bell = helper.absolutePos(new BlockPos(4, 3, 4));
        WorldMemory.VillageRecord record = WorldMemory.get(level).village(bell, 0L, 0);
        record.surveyed = true;
        BlockPos corner = helper.absolutePos(new BlockPos(12, 2, 12));
        Blueprint well = Builders.record(level, "well", () -> Builders.well(level, corner, corner.getY(), Builders.OAK));
        helper.assertTrue(well.size() > 20, "the well blueprint is too small: " + well.size());
        record.sites.add(well);
        VillageWorker builder = helper.spawn(ModEntities.VILLAGE_BUILDER.get(), new BlockPos(7, 3, 7));
        builder.setHomeBell(bell);
        helper.succeedWhen(() -> {
            helper.assertTrue(record.sites.isEmpty(), "the builder has not finished the well: " + well.next + " of " + well.size());
            helper.assertBlockPresent(Blocks.COBBLESTONE_WALL, new BlockPos(12, 2, 12));
            helper.assertTrue(helper.getBlockState(new BlockPos(13, 2, 13)).is(Blocks.WATER), "there is no water in the well");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 2400)
    public static void builderRepairsWhatWasDestroyedButNotWhatAPlayerChanged(GameTestHelper helper) {
        fill(helper, 1, 1, Blocks.GRASS_BLOCK);
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(3, 2, 3), Blocks.BELL);
        BlockPos bell = helper.absolutePos(new BlockPos(3, 2, 3));
        WorldMemory.VillageRecord record = WorldMemory.get(level).village(bell, 0L, 1);
        BlockPos min = helper.absolutePos(new BlockPos(10, 1, 10));
        Blueprint house = Builders.record(level, "house",
                () -> Builders.house(level, min, min.getY(), Direction.SOUTH, Builders.OAK, Blocks.BARREL));
        Construction.buildNow(level, house);
        Construction.adopt(record, house);
        record.surveyed = true;
        // a creeper's work, and a player who put a stone block into one of the holes
        BlockPos[] holes = {new BlockPos(10, 2, 11), new BlockPos(10, 3, 11), new BlockPos(10, 4, 12), new BlockPos(11, 5, 10),
                new BlockPos(12, 5, 10), new BlockPos(16, 2, 12)};
        BlockState[] was = new BlockState[holes.length];
        for (int i = 0; i < holes.length; i++) {
            was[i] = helper.getBlockState(holes[i]);
            helper.assertFalse(was[i].isAir(), "nothing to break at " + holes[i]);
            helper.setBlock(holes[i], Blocks.AIR);
        }
        helper.setBlock(holes[0], Blocks.STONE);
        Construction.inspect(level, record, 100000);
        helper.assertTrue(record.repairs.size() == holes.length - 1, "found " + record.repairs.size() + " holes to repair");
        VillageWorker builder = helper.spawn(ModEntities.VILLAGE_BUILDER.get(), new BlockPos(5, 2, 5));
        builder.setHomeBell(bell);
        helper.succeedWhen(() -> {
            helper.assertTrue(record.repairs.isEmpty(), record.repairs.size() + " holes are still open");
            for (int i = 1; i < holes.length; i++) {
                helper.assertTrue(helper.getBlockState(holes[i]).is(was[i].getBlock()), "not repaired: " + holes[i]);
            }
            helper.assertBlockPresent(Blocks.STONE, holes[0]);
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void dreamcatcherWardsTheShades(GameTestHelper helper) {
        fill(helper, 1, 1, Blocks.STONE);
        helper.setBlock(new BlockPos(12, 8, 12), Blocks.OAK_PLANKS);
        helper.setBlock(new BlockPos(12, 7, 12), WorldBlocks.DREAMCATCHER.get());
        BlockPos below = helper.absolutePos(new BlockPos(12, 2, 12));
        helper.succeedWhen(() -> helper.assertTrue(Shade.nearDreamcatcher(helper.getLevel(), below),
                "the dreamcatcher is not a point of interest"));
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void shadeShrinksFromLightButSurvivesTheDark(GameTestHelper helper) {
        fill(helper, 1, 1, Blocks.STONE);
        room(helper, new BlockPos(3, 1, 3));
        room(helper, new BlockPos(14, 1, 14));
        helper.setBlock(new BlockPos(5, 1, 5), Blocks.GLOWSTONE);
        Shade lit = helper.spawn(ModEntities.SHADE.get(), new BlockPos(5, 2, 5));
        Shade dark = helper.spawn(ModEntities.SHADE.get(), new BlockPos(16, 2, 16));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(lit.getHealth() < lit.getMaxHealth(), "the light did not hurt the shade"))
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(dark.isAlive() && dark.getHealth() == dark.getMaxHealth(), "the shade was hurt in the dark");
                    helper.assertFalse(lit.isWatched(), "the shade thinks it is watched with nobody around");
                })
                .thenSucceed();
    }
}
