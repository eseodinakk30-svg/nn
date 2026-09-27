package com.dunesrelics.entity.world;

import com.dunesrelics.registry.ModEntities;
import com.dunesrelics.registry.WorldItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** A heavy iron ball: it flies in a long arc and bursts on impact, hurting whoever is near but sparing the blocks. */
public class Cannonball extends ThrowableItemProjectile {
    public Cannonball(EntityType<? extends Cannonball> type, Level level) {
        super(type, level);
    }

    public Cannonball(Level level, double x, double y, double z) {
        super(ModEntities.CANNONBALL.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return WorldItems.CANNONBALL.get();
    }

    @Override
    protected float getGravity() {
        return 0.04F;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.2D, this.getZ(), 0.0D, 0.0D, 0.0D);
        }
        if (this.tickCount > 200) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity owner = this.getOwner();
        result.getEntity().hurt(this.damageSources().thrown(this, owner), 8.0F);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), 1.6F, false, Level.ExplosionInteraction.NONE);
            this.discard();
        }
    }
}
