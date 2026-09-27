package com.dunesrelics.sandstorm;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.entity.volcanic.VolcanicBomb;
import com.dunesrelics.registry.ModTags;
import com.dunesrelics.registry.VolcanicBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.dunesrelics.registry.ModStructures;
import com.dunesrelics.worldgen.structure.VolcanoPiece;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.HashSet;
import java.util.Set;

/**
 * Weather of the volcanic biomes (tagged {@code dunesrelics:has_ashfall}), which never see rain:
 * <ul>
 *     <li>when it rains elsewhere, ash falls here and slowly settles on the ground;</li>
 *     <li>during thunderstorms the volcanoes erupt and volcanic bombs rain from the sky.</li>
 * </ul>
 * Also heals players bathing in hot springs.
 */
@Mod.EventBusSubscriber(modid = DunesRelics.MODID)
public final class VolcanicWeather {
    private VolcanicWeather() {}

    public static boolean isAshfallBiome(Level level, BlockPos pos) {
        return level.getBiome(pos).is(ModTags.HAS_ASHFALL);
    }

    public static boolean isErupting(Level level, BlockPos pos) {
        return level.isThundering() && isAshfallBiome(level, pos);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.getGameTime() % 20 != 0) {
            return;
        }
        RandomSource random = level.random;
        erupt(level, random);
        for (ServerPlayer player : level.players()) {
            BlockPos pos = player.blockPosition();
            if (level.getGameTime() % 40 == 0 && player.isInWater() && isAshfallBiome(level, pos)
                    && level.getBlockState(pos.below()).is(VolcanicBlocks.PUMICE.get())) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, true));
            }
            if (!level.isRaining() || player.isSpectator() || !isAshfallBiome(level, pos)) {
                continue;
            }
            if (level.isThundering() && random.nextFloat() < 0.35F) {
                launchBomb(level, player, random);
            }
            if (random.nextFloat() < 0.35F) {
                settleAsh(level, pos, random);
            }
        }
    }

    /**
     * Volcanoes near players smoke all the time; while it rains they smoke harder, and in a thunderstorm they erupt:
     * lava fountains out of the crater and bombs fly from it onto the flanks.
     */
    private static void erupt(ServerLevel level, RandomSource random) {
        Set<Long> done = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            for (StructureStart start : level.structureManager().startsForStructure(new ChunkPos(player.blockPosition()),
                    structure -> structure.type() == ModStructures.VOLCANO.get())) {
                if (start.getPieces().isEmpty() || !(start.getPieces().get(0) instanceof VolcanoPiece piece)) {
                    continue;
                }
                BlockPos crater = piece.craterTop();
                if (!done.add(crater.asLong()) || !level.isLoaded(crater)) {
                    continue;
                }
                boolean erupting = level.isThundering();
                boolean raining = level.isRaining();
                for (ServerPlayer viewer : level.players()) {
                    if (viewer.blockPosition().distSqr(crater) > 256 * 256) {
                        continue;
                    }
                    level.sendParticles(viewer, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, crater.getX() + 0.5D, crater.getY() + 1.0D,
                            crater.getZ() + 0.5D, erupting ? 6 : raining ? 3 : 1, 1.5D, 0.5D, 1.5D, 0.02D);
                    if (erupting) {
                        level.sendParticles(viewer, ParticleTypes.LAVA, true, crater.getX() + 0.5D, crater.getY() + 1.0D,
                                crater.getZ() + 0.5D, 30, 2.0D, 1.0D, 2.0D, 0.5D);
                        level.sendParticles(viewer, ParticleTypes.FLAME, true, crater.getX() + 0.5D, crater.getY() + 2.0D,
                                crater.getZ() + 0.5D, 20, 1.5D, 3.0D, 1.5D, 0.15D);
                        level.sendParticles(viewer, ParticleTypes.LARGE_SMOKE, true, crater.getX() + 0.5D, crater.getY() + 4.0D,
                                crater.getZ() + 0.5D, 25, 2.5D, 4.0D, 2.5D, 0.08D);
                    }
                }
                if (!erupting) {
                    continue;
                }
                level.playSound(null, crater, SoundEvents.GENERIC_EXPLODE, SoundSource.WEATHER, 6.0F, 0.35F + random.nextFloat() * 0.15F);
                int bombs = 1 + random.nextInt(3);
                for (int i = 0; i < bombs; i++) {
                    VolcanicBomb bomb = new VolcanicBomb(level, crater.getX() + 0.5D, crater.getY() + 2.0D, crater.getZ() + 0.5D);
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    double speed = 0.35D + random.nextDouble() * 0.6D;
                    bomb.setDeltaMovement(Math.cos(angle) * speed, 1.1D + random.nextDouble() * 0.6D, Math.sin(angle) * speed);
                    level.addFreshEntity(bomb);
                }
            }
        }
    }

    private static void launchBomb(ServerLevel level, ServerPlayer player, RandomSource random) {
        double x = player.getX() + (random.nextDouble() - 0.5D) * 48.0D;
        double z = player.getZ() + (random.nextDouble() - 0.5D) * 48.0D;
        BlockPos column = BlockPos.containing(x, player.getY(), z);
        if (!level.canSeeSky(column.above(3)) && !level.canSeeSky(column)) {
            return;
        }
        VolcanicBomb bomb = new VolcanicBomb(level, x, player.getY() + 32.0D + random.nextInt(12), z);
        bomb.shoot(random.nextGaussian() * 0.2D, -1.0D, random.nextGaussian() * 0.2D, 0.7F, 6.0F);
        level.addFreshEntity(bomb);
    }

    /** Ash piles up (at most a few layers) on natural ground under the open sky. */
    private static void settleAsh(ServerLevel level, BlockPos near, RandomSource random) {
        int x = near.getX() + random.nextInt(33) - 16;
        int z = near.getZ() + random.nextInt(33) - 16;
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
        BlockState at = level.getBlockState(top);
        BlockState below = level.getBlockState(top.below());
        if (at.is(VolcanicBlocks.ASH_LAYER.get())) {
            int layers = at.getValue(SnowLayerBlock.LAYERS);
            if (layers < 3) {
                level.setBlockAndUpdate(top, at.setValue(SnowLayerBlock.LAYERS, layers + 1));
            }
        } else if (at.isAir() && (below.is(VolcanicBlocks.ASH_BLOCK.get()) || below.is(VolcanicBlocks.SCORIA.get())
                || below.is(VolcanicBlocks.BLACK_SAND.get()) || below.is(net.minecraft.world.level.block.Blocks.BASALT))) {
            level.setBlockAndUpdate(top, VolcanicBlocks.ASH_LAYER.get().defaultBlockState());
        }
    }
}
