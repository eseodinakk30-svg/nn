package com.dunesrelics.entity.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** A landing party walks from the beach towards the village it came to plunder, until something gets in its way. */
public class MarchGoal<T extends PathfinderMob & PirateCrew> extends Goal {
    private final T mob;
    private final double speed;
    private int recalc;

    public MarchGoal(T mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        BlockPos target = this.mob.getRaidTarget();
        if (target == null || this.mob.getTarget() != null) {
            return false;
        }
        if (target.closerToCenterThan(this.mob.position(), 6.0D)) {
            this.mob.setRaidTarget(null);
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse() && !this.mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        this.recalc = 0;
        this.moveOn();
    }

    @Override
    public void tick() {
        if (++this.recalc % 40 == 0) {
            this.moveOn();
        }
    }

    private void moveOn() {
        BlockPos target = this.mob.getRaidTarget();
        if (target != null) {
            this.mob.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, this.speed);
        }
    }
}
