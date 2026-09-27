package com.dunesrelics.entity.volcanic;

import com.dunesrelics.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.HitResult;

/** A glob of molten rock hurled out of an erupting volcano. Bursts into flame where it lands. */
public class VolcanicBomb extends ThrowableItemProjectile {
    public VolcanicBomb(EntityType<? extends VolcanicBomb> type, Level level) {
        super(type, level);
    }

    public VolcanicBomb(Level level, double x, double y, double z) {
        super(ModEntities.VOLCANIC_BOMB.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.MAGMA_BLOCK;
    }

    @Override
    protected float getGravity() {
        return 0.05F;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.3D, this.getZ(), 0.0D, 0.02D, 0.0D);
            this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY() + 0.2D, this.getZ(), 0.0D, 0.0D, 0.0D);
            if (this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.LAVA, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            }
        }
        if (this.tickCount > 400) {
            this.discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), 1.6F, false, Level.ExplosionInteraction.NONE);
            BlockPos pos = this.blockPosition();
            if (this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && this.random.nextBoolean()
                    && this.level().isEmptyBlock(pos) && BaseFireBlock.canBePlacedAt(this.level(), pos, net.minecraft.core.Direction.UP)) {
                this.level().setBlockAndUpdate(pos, BaseFireBlock.getState(this.level(), pos));
            }
            this.discard();
        }
    }
}
