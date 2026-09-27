package com.dunesrelics.sandstorm;

import com.dunesrelics.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Sandstorms are the desert's weather. Deserts never see rain, so whenever it rains elsewhere in the world,
 * biomes tagged {@code dunesrelics:has_sandstorms} are swept by a sandstorm instead. Storms therefore come and go
 * with the weather cycle (and respond to {@code /weather}).
 */
public final class Sandstorm {
    private Sandstorm() {}

    public static boolean isSandstormBiome(Level level, BlockPos pos) {
        return level.getBiome(pos).is(ModTags.HAS_SANDSTORMS);
    }

    /** True when a sandstorm is raging at this position and it is open to the sky. */
    public static boolean isExposed(Level level, BlockPos pos) {
        return level.isRaining() && level.canSeeSky(pos) && isSandstormBiome(level, pos);
    }
}
