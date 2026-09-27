package com.dunesrelics.gametest;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.block.volcanic.HeartOfTheVolcanoBlock;
import com.dunesrelics.block.volcanic.SteamVentBlock;
import com.dunesrelics.block.volcanic.VolcanicForgeBlock;
import com.dunesrelics.entity.Scorpion;
import com.dunesrelics.entity.volcanic.LavaCrab;
import com.dunesrelics.entity.volcanic.MagmaTitan;
import com.dunesrelics.item.Tempering;
import com.dunesrelics.item.volcanic.SalamanderBootsItem;
import com.dunesrelics.registry.ModEntities;
import com.dunesrelics.registry.ModItems;
import com.dunesrelics.registry.VolcanicBlocks;
import com.dunesrelics.registry.VolcanicItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** In-game tests for the volcanic update's blocks, items and mobs. */
@GameTestHolder(DunesRelics.MODID)
@PrefixGameTestTemplate(false)
public final class VolcanicGameTests {
    private static final String ARENA = "arena";

    private VolcanicGameTests() {}

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

    /** A 5x5 pool of lava sources at y=2, walled in with stone. */
    private static void lavaPool(GameTestHelper helper) {
        fill(helper, 1, 2, Blocks.STONE);
        for (int x = 10; x <= 14; x++) {
            for (int z = 10; z <= 14; z++) {
                helper.setBlock(new BlockPos(x, 2, z), Blocks.LAVA);
            }
        }
    }

    private static void use(GameTestHelper helper, BlockPos pos, Player player) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = helper.getBlockState(pos);
        state.use(helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
    }

    @GameTest(template = ARENA)
    public static void forgeTempersAPickaxe(GameTestHelper helper) {
        fill(helper, 1, 1, Blocks.STONE);
        BlockPos pos = new BlockPos(12, 2, 12);
        helper.setBlock(pos, VolcanicBlocks.VOLCANIC_FORGE.get().defaultBlockState());
        Player player = helper.makeMockPlayer();

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
        use(helper, pos, player);
        helper.assertBlockProperty(pos, VolcanicForgeBlock.LAVA, VolcanicForgeBlock.MAX_LAVA);

        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
        use(helper, pos, player);
        helper.assertFalse(Tempering.isTempered(player.getMainHandItem()), "tempered without a fire opal");

        player.getInventory().add(new ItemStack(VolcanicItems.FIRE_OPAL.get(), 2));
        use(helper, pos, player);
        helper.assertTrue(Tempering.isTempered(player.getMainHandItem()), "pickaxe was not tempered");
        helper.assertTrue(player.getInventory().countItem(VolcanicItems.FIRE_OPAL.get()) == 1, "fire opal was not consumed");
        helper.assertBlockProperty(pos, VolcanicForgeBlock.LAVA, VolcanicForgeBlock.MAX_LAVA - 1);
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void steamVentLaunchesEntities(GameTestHelper helper) {
        fill(helper, 1, 1, Blocks.STONE);
        BlockPos vent = new BlockPos(12, 1, 12);
        helper.setBlock(vent, VolcanicBlocks.STEAM_VENT.get().defaultBlockState().setValue(SteamVentBlock.ACTIVE, true));
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(12, 2, 12));
        double startY = stand.getY();
        helper.succeedWhen(() -> helper.assertTrue(stand.getY() > startY + 2.5D, "the steam did not lift the armor stand"));
    }

    @GameTest(template = ARENA)
    public static void pumiceSpongeSoaksUpLava(GameTestHelper helper) {
        lavaPool(helper);
        BlockPos sponge = new BlockPos(12, 3, 12);
        helper.setBlock(sponge, VolcanicBlocks.PUMICE_SPONGE.get());
        helper.assertTrue(count(helper, Blocks.LAVA) == 0, "lava is left in the pool");
        helper.assertBlockPresent(VolcanicBlocks.MOLTEN_PUMICE_SPONGE.get(), sponge);
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void salamanderBootsCoolLavaForAWhile(GameTestHelper helper) {
        lavaPool(helper);
        BlockPos feet = new BlockPos(12, 3, 12);
        SalamanderBootsItem.coolLava(helper.getLevel(), helper.absolutePos(feet));
        helper.assertBlockPresent(VolcanicBlocks.COOLED_LAVA_CRUST.get(), feet.below());
        helper.assertTrue(count(helper, VolcanicBlocks.COOLED_LAVA_CRUST.get()) >= 9, "the crust is too small");
        // left alone, the crust melts back into lava
        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.LAVA, feet.below()));
    }

    @GameTest(template = ARENA)
    public static void everyVolcanicMobSpawns(GameTestHelper helper) {
        fill(helper, 1, 1, VolcanicBlocks.SCORIA.get());
        EntityType<?>[] types = {ModEntities.MAGMA_TITAN.get(), ModEntities.SALAMANDER.get(), ModEntities.LAVA_CRAB.get(),
                ModEntities.MAGMALING.get(), ModEntities.CINDER_WRAITH.get()};
        int x = 3;
        for (EntityType<?> type : types) {
            helper.spawn(type, new BlockPos(x, 2, 12));
            x += 4;
        }
        helper.runAfterDelay(20, () -> {
            for (EntityType<?> type : types) {
                helper.assertEntityPresent(type);
            }
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void heartAwakensTheTitan(GameTestHelper helper) {
        fill(helper, 1, 1, Blocks.OBSIDIAN);
        BlockPos pos = new BlockPos(12, 2, 12);
        BlockState dormant = VolcanicBlocks.HEART_OF_THE_VOLCANO.get().defaultBlockState().setValue(HeartOfTheVolcanoBlock.DORMANT, true);
        helper.setBlock(pos, dormant);
        MagmaTitan titan = HeartOfTheVolcanoBlock.awaken(helper.getLevel(), helper.absolutePos(pos), dormant, null);
        helper.assertTrue(titan != null, "no titan awoke");
        helper.assertBlockProperty(pos, HeartOfTheVolcanoBlock.DORMANT, false);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void emberSaplingGrowsOnAsh(GameTestHelper helper) {
        fill(helper, 1, 1, VolcanicBlocks.ASH_BLOCK.get());
        BlockPos pos = new BlockPos(12, 2, 12);
        helper.setBlock(pos, VolcanicBlocks.EMBER_SAPLING.get());
        ServerLevel level = helper.getLevel();
        SaplingBlock sapling = (SaplingBlock) VolcanicBlocks.EMBER_SAPLING.get();
        for (int i = 0; i < 2; i++) {
            BlockState state = helper.getBlockState(pos);
            if (state.is(sapling)) {
                sapling.advanceTree(level, helper.absolutePos(pos), state, level.random);
            }
        }
        helper.assertBlockPresent(VolcanicBlocks.EMBER_LOG.get(), pos);
        helper.assertTrue(count(helper, VolcanicBlocks.EMBER_LEAVES.get()) > 8, "ember tree has no crown");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void lavaCrabGoesInABucket(GameTestHelper helper) {
        fill(helper, 1, 1, VolcanicBlocks.BLACK_SAND.get());
        LavaCrab crab = helper.spawn(ModEntities.LAVA_CRAB.get(), new BlockPos(12, 2, 12));
        Player player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
        crab.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().is(VolcanicItems.LAVA_CRAB_BUCKET.get()), "crab was not scooped up");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void scorpionCanBeMilked(GameTestHelper helper) {
        fill(helper, 1, 1, Blocks.SAND);
        Scorpion scorpion = helper.spawn(ModEntities.SCORPION.get(), new BlockPos(12, 2, 12));
        Player player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
        scorpion.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().is(ModItems.SCORPION_VENOM.get()), "no venom was collected");
        helper.succeed();
    }
}
