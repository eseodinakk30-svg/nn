package com.dunesrelics.gametest;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.block.AncientUrnBlock;
import com.dunesrelics.block.SarcophagusBlock;
import com.dunesrelics.entity.Pharaoh;
import com.dunesrelics.registry.ModBlocks;
import com.dunesrelics.registry.ModEntities;
import com.dunesrelics.worldgen.feature.AncientRuinFeature;
import com.dunesrelics.worldgen.feature.PalmTreeFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

/**
 * In-game tests, run in CI with {@code ./gradlew runGameTestServer}. Starting the test server also loads
 * every registry and datapack file of the mod (biome, features, loot tables, recipes...), so a broken JSON file
 * fails the build too.
 */
@GameTestHolder(DunesRelics.MODID)
@PrefixGameTestTemplate(false)
public final class ModGameTests {
    private static final String ARENA = "arena";

    private ModGameTests() {}

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
        for (int x = 0; x < 24; x++) {
            for (int z = 0; z < 24; z++) {
                for (int y = 0; y < 24; y++) {
                    if (helper.getBlockState(new BlockPos(x, y, z)).is(block)) {
                        found++;
                    }
                }
            }
        }
        return found;
    }

    @GameTest(template = ARENA)
    public static void palmTreeGrowsOnSand(GameTestHelper helper) {
        BlockPos ground = new BlockPos(12, 1, 12);
        helper.setBlock(ground, Blocks.SAND);
        ServerLevel level = helper.getLevel();
        helper.assertTrue(PalmTreeFeature.grow(level, level.random, helper.absolutePos(ground.above())), "palm tree did not grow");
        helper.assertBlockPresent(ModBlocks.PALM_LOG.get(), ground.above());
        helper.assertTrue(count(helper, ModBlocks.PALM_LEAVES.get()) > 10, "palm tree has no crown");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void ruinGeneratesWithTomb(GameTestHelper helper) {
        fill(helper, 1, 15, Blocks.SANDSTONE);
        fill(helper, 16, 16, Blocks.SAND);
        ServerLevel level = helper.getLevel();
        helper.assertTrue(AncientRuinFeature.generate(level, level.random, helper.absolutePos(new BlockPos(12, 17, 12)), true),
                "ruin did not generate");
        helper.assertTrue(count(helper, ModBlocks.SARCOPHAGUS.get()) == 1, "tomb has no sarcophagus");
        helper.assertTrue(count(helper, Blocks.CHEST) >= 3, "ruin and tomb should hold three chests");
        helper.assertTrue(count(helper, Blocks.LADDER) >= 5, "tomb shaft has no ladder");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void oasisGeneratesInSand(GameTestHelper helper) {
        fill(helper, 1, 8, Blocks.SAND);
        ServerLevel level = helper.getLevel();
        FeaturePlaceContext<NoneFeatureConfiguration> context = new FeaturePlaceContext<>(Optional.empty(), level,
                level.getChunkSource().getGenerator(), level.random, helper.absolutePos(new BlockPos(12, 9, 12)),
                NoneFeatureConfiguration.INSTANCE);
        helper.assertTrue(com.dunesrelics.registry.ModFeatures.OASIS.get().place(context), "oasis did not generate");
        helper.assertTrue(count(helper, Blocks.WATER) > 10, "oasis has no pond");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void everyMobSpawns(GameTestHelper helper) {
        fill(helper, 1, 1, Blocks.SAND);
        EntityType<?>[] types = {ModEntities.MUMMY.get(), ModEntities.PHARAOH.get(), ModEntities.SCORPION.get(),
                ModEntities.SCARAB.get(), ModEntities.MEERKAT.get(), ModEntities.VULTURE.get()};
        int x = 3;
        for (EntityType<?> type : types) {
            helper.spawn(type, new BlockPos(x, 2, 12));
            x += 3;
        }
        helper.runAfterDelay(20, () -> {
            for (EntityType<?> type : types) {
                helper.assertEntityPresent(type);
            }
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void sealedSarcophagusReleasesThePharaoh(GameTestHelper helper) {
        fill(helper, 1, 1, Blocks.SANDSTONE);
        BlockPos pos = new BlockPos(12, 2, 12);
        BlockState sealed = ModBlocks.SARCOPHAGUS.get().defaultBlockState().setValue(SarcophagusBlock.SEALED, true);
        helper.setBlock(pos, sealed);
        Pharaoh pharaoh = SarcophagusBlock.awaken(helper.getLevel(), helper.absolutePos(pos), sealed, null);
        helper.assertTrue(pharaoh != null, "no pharaoh was released");
        helper.assertBlockProperty(pos, SarcophagusBlock.SEALED, false);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void sealedUrnShatters(GameTestHelper helper) {
        fill(helper, 1, 1, Blocks.SANDSTONE);
        BlockPos pos = new BlockPos(12, 2, 12);
        helper.setBlock(pos, ModBlocks.ANCIENT_URN.get().defaultBlockState().setValue(AncientUrnBlock.SEALED, true));
        helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
        helper.assertBlockPresent(Blocks.AIR, pos);
        helper.succeed();
    }

    /** A walled pit of quicksand, 3x3 wide and 4 deep, with sandstone all around it. */
    private static void quicksandPit(GameTestHelper helper) {
        fill(helper, 1, 5, Blocks.SANDSTONE);
        for (int x = 11; x <= 13; x++) {
            for (int z = 11; z <= 13; z++) {
                for (int y = 2; y <= 5; y++) {
                    helper.setBlock(new BlockPos(x, y, z), ModBlocks.QUICKSAND.get());
                }
            }
        }
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void entitiesSinkInQuicksand(GameTestHelper helper) {
        quicksandPit(helper);
        // An armor stand has no AI that could climb out, but is subject to normal physics.
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(12, 6, 12));
        double start = stand.getY();
        helper.succeedWhen(() -> helper.assertTrue(stand.getY() < start - 1.5D, "armor stand is not sinking"));
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void scorpionWalksOnQuicksand(GameTestHelper helper) {
        quicksandPit(helper);
        Mob scorpion = helper.spawn(ModEntities.SCORPION.get(), new BlockPos(12, 6, 12));
        double surface = helper.absolutePos(new BlockPos(12, 6, 12)).getY();
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(scorpion.getY() >= surface - 0.01D, "scorpion sank into quicksand");
            helper.succeed();
        });
    }
}
