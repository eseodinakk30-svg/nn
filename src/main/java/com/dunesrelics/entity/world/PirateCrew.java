package com.dunesrelics.entity.world;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/** Every pirate: they share cannons, march together on a raid and are remembered by the villagers who fight them. */
public interface PirateCrew {
    /** Where a landing party is heading (usually a village bell), or null. */
    @Nullable
    BlockPos getRaidTarget();

    void setRaidTarget(@Nullable BlockPos target);
}
