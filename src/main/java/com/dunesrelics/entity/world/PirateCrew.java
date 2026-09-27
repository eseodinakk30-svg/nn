package com.dunesrelics.entity.world;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Every pirate: they share cannons, march together on a raid and are remembered by the villagers who fight them. */
public interface PirateCrew {
    /** Where a landing party is heading (usually a village bell), or null. */
    @Nullable
    BlockPos getRaidTarget();

    void setRaidTarget(@Nullable BlockPos target);

    /** The player their captain is fighting one on one: the crew keeps out of it. */
    @Nullable
    UUID getDuelist();

    void setDuelist(@Nullable UUID duelist);

    /** Their captain lost a fair duel: they have thrown down their weapons. */
    boolean hasSurrendered();

    void surrender();
}
