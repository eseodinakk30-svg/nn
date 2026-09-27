package com.dunesrelics.block.entity;

import com.dunesrelics.block.world.CannonBlock;
import com.dunesrelics.entity.world.Cannonball;
import com.dunesrelics.entity.world.PirateCrew;
import com.dunesrelics.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The cannon's charge. Cannons on pirate ships are "crewed": while a pirate stands next to one it keeps firing at
 * players in front of it, and it never runs out of shot.
 */
public class CannonBlockEntity extends BlockEntity {
    private static final double SPEED = 1.7D;
    private static final double GRAVITY = 0.04D;
    private boolean powder;
    private boolean ball;
    private boolean crewed;
    private int cooldown = 40;

    public CannonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CANNON.get(), pos, state);
    }

    public boolean hasPowder() {
        return this.powder;
    }

    public boolean hasBall() {
        return this.ball;
    }

    public boolean isLoaded() {
        return this.powder && this.ball;
    }

    public void setPowder(boolean powder) {
        this.powder = powder;
        this.sync();
    }

    public void setBall(boolean ball) {
        this.ball = ball;
        this.sync();
    }

    public void setCrewed(boolean crewed) {
        this.crewed = crewed;
        this.setChanged();
    }

    private void sync() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.getBlockState();
            if (state.getValue(CannonBlock.LOADED) != this.isLoaded()) {
                this.level.setBlock(this.worldPosition, state.setValue(CannonBlock.LOADED, this.isLoaded()), 3);
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CannonBlockEntity cannon) {
        if (!cannon.crewed || --cannon.cooldown > 0) {
            return;
        }
        cannon.cooldown = 20;
        Direction facing = state.getValue(CannonBlock.FACING);
        AABB near = new AABB(pos).inflate(4.0D);
        if (level.getEntitiesOfClass(Mob.class, near, m -> m instanceof PirateCrew && m.isAlive()).isEmpty()) {
            return;
        }
        Vec3 muzzle = Vec3.atCenterOf(pos).add(facing.getStepX() * 0.9D, 0.3D, facing.getStepZ() * 0.9D);
        Player target = null;
        double best = 48.0D * 48.0D;
        for (Player player : level.players()) {
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }
            Vec3 to = player.position().subtract(muzzle);
            double distance = to.lengthSqr();
            Vec3 flat = new Vec3(to.x, 0.0D, to.z).normalize();
            if (distance < best && distance > 16.0D && flat.x * facing.getStepX() + flat.z * facing.getStepZ() > 0.6D) {
                best = distance;
                target = player;
            }
        }
        if (target != null) {
            cannon.fire(target);
            cannon.cooldown = 60 + level.random.nextInt(60);
        }
    }

    /** Fires straight ahead, or with a ballistic aim at {@code target}. */
    public void fire(@Nullable LivingEntity target) {
        if (!(this.level instanceof ServerLevel level)) {
            return;
        }
        Direction facing = this.getBlockState().getValue(CannonBlock.FACING);
        Vec3 muzzle = Vec3.atCenterOf(this.worldPosition).add(facing.getStepX() * 0.9D, 0.3D, facing.getStepZ() * 0.9D);
        Vec3 velocity;
        if (target != null) {
            Vec3 to = target.getEyePosition().subtract(muzzle);
            double horizontal = Math.sqrt(to.x * to.x + to.z * to.z);
            double time = Math.max(1.0D, horizontal / SPEED);
            double vy = (to.y + 0.5D * GRAVITY * time * time) / time;
            velocity = new Vec3(to.x / time, vy, to.z / time);
            velocity = velocity.add(level.random.nextGaussian() * 0.03D, 0.0D, level.random.nextGaussian() * 0.03D);
        } else {
            velocity = new Vec3(facing.getStepX() * SPEED, 0.25D, facing.getStepZ() * SPEED);
        }
        Cannonball ball = new Cannonball(level, muzzle.x, muzzle.y, muzzle.z);
        ball.setDeltaMovement(velocity);
        level.addFreshEntity(ball);
        level.playSound(null, this.worldPosition, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.4F, 1.3F);
        level.sendParticles(ParticleTypes.EXPLOSION, muzzle.x, muzzle.y, muzzle.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 12, 0.2D, 0.2D, 0.2D, 0.05D);
        if (!this.crewed) {
            this.powder = false;
            this.ball = false;
            this.sync();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("Powder", this.powder);
        tag.putBoolean("Ball", this.ball);
        tag.putBoolean("Crewed", this.crewed);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.powder = tag.getBoolean("Powder");
        this.ball = tag.getBoolean("Ball");
        this.crewed = tag.getBoolean("Crewed");
    }
}
