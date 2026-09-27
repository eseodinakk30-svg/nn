package com.dunesrelics.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Vultures circle high above the dunes. They are scavengers: they leave healthy travellers alone,
 * but swoop down on any player who is badly hurt (4 hearts or less), and fight back when attacked.
 */
public class Vulture extends PathfinderMob {
    private static final float SCAVENGE_HEALTH = 8.0F;
    /** Players who fed this vulture: it will not hunt them any more. */
    private final java.util.Set<java.util.UUID> friends = new java.util.HashSet<>();
    private int feedCooldown;

    public Vulture(EntityType<? extends Vulture> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.FLYING_SPEED, 0.6D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    public static boolean checkVultureSpawnRules(EntityType<? extends Vulture> type, LevelAccessor level, MobSpawnType reason,
                                                 BlockPos pos, RandomSource random) {
        BlockState ground = level.getBlockState(pos.below());
        return (ground.is(BlockTags.SAND) || ground.is(BlockTags.TERRACOTTA)) && level.getRawBrightness(pos, 0) > 8;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.4D, true));
        this.goalSelector.addGoal(4, new SoarGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                e -> e.getHealth() <= SCAVENGE_HEALTH && !this.friends.contains(e.getUUID())));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                                  @Nullable SpawnGroupData groupData, @Nullable CompoundTag tag) {
        this.setNoGravity(true);
        return super.finalizeSpawn(level, difficulty, reason, groupData, tag);
    }

    /** Throw a vulture some rotten flesh: it drops a feather and stops seeing you as a meal. */
    @Override
    protected net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        net.minecraft.world.item.ItemStack held = player.getItemInHand(hand);
        if (!held.is(net.minecraft.world.item.Items.ROTTEN_FLESH) || this.feedCooldown > 0) {
            return super.mobInteract(player, hand);
        }
        if (!this.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            this.friends.add(player.getUUID());
            if (this.getTarget() == player) {
                this.setTarget(null);
            }
            this.spawnAtLocation(com.dunesrelics.registry.ModItems.VULTURE_FEATHER.get());
            this.feedCooldown = 1200;
            this.playSound(SoundEvents.PARROT_EAT, 1.0F, 0.6F);
            ((net.minecraft.server.level.ServerLevel) this.level()).sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                    this.getX(), this.getY() + 0.8D, this.getZ(), 3, 0.3D, 0.3D, 0.3D, 0.0D);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.feedCooldown > 0) {
            this.feedCooldown--;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (java.util.UUID friend : this.friends) {
            list.add(net.minecraft.nbt.NbtUtils.createUUID(friend));
        }
        tag.put("Friends", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.friends.clear();
        for (net.minecraft.nbt.Tag entry : tag.getList("Friends", net.minecraft.nbt.Tag.TAG_INT_ARRAY)) {
            this.friends.add(net.minecraft.nbt.NbtUtils.loadUUID(entry));
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.55F + this.random.nextFloat() * 0.1F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    /** Wide, lazy circles on the thermals, drifting slowly across the desert. */
    static class SoarGoal extends Goal {
        private final Vulture vulture;
        private Vec3 center = Vec3.ZERO;
        private float angle;
        private float radius;
        private float direction;
        private double altitude;

        SoarGoal(Vulture vulture) {
            this.vulture = vulture;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.vulture.getTarget() == null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.vulture.getTarget() == null;
        }

        @Override
        public void start() {
            RandomSource random = this.vulture.getRandom();
            this.center = this.vulture.position();
            this.radius = 8.0F + random.nextFloat() * 10.0F;
            this.direction = random.nextBoolean() ? 1.0F : -1.0F;
            this.angle = (float) Mth.atan2(this.vulture.getZ() - this.center.z, this.vulture.getX() - this.center.x);
            this.pickAltitude();
        }

        private void pickAltitude() {
            Level level = this.vulture.level();
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(this.center.x), Mth.floor(this.center.z));
            this.altitude = Math.min(ground + 14 + this.vulture.getRandom().nextInt(12), level.getMaxBuildHeight() - 8);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            RandomSource random = this.vulture.getRandom();
            if (random.nextInt(400) == 0) {
                // Drift to a new thermal.
                this.center = this.center.add(random.nextGaussian() * 12.0D, 0.0D, random.nextGaussian() * 12.0D);
                this.pickAltitude();
            }
            if (random.nextInt(200) == 0) {
                this.radius = Mth.clamp(this.radius + (random.nextFloat() - 0.5F) * 6.0F, 6.0F, 20.0F);
            }
            Vec3 wanted = new Vec3(this.center.x + Mth.cos(this.angle) * this.radius, this.altitude,
                    this.center.z + Mth.sin(this.angle) * this.radius);
            if (this.vulture.position().distanceToSqr(wanted) < 9.0D || this.vulture.horizontalCollision) {
                this.angle += this.direction * 0.35F;
                wanted = new Vec3(this.center.x + Mth.cos(this.angle) * this.radius, this.altitude,
                        this.center.z + Mth.sin(this.angle) * this.radius);
            }
            this.vulture.getMoveControl().setWantedPosition(wanted.x, wanted.y, wanted.z, 1.0D);
        }
    }
}
