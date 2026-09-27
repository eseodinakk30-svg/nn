package com.dunesrelics.entity.world;

import com.dunesrelics.block.world.DreamcatcherBlock;
import com.dunesrelics.registry.ModPoiTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * The Shade: a tall, thin shape that walks the dark on moonless nights. It only moves while nobody is looking at it,
 * and freezes the instant you turn around. It will not step into light, and it crumbles away at sunrise.
 * Its touch brings Darkness. A dreamcatcher keeps them from being born nearby.
 */
public class Shade extends Monster {
    private static final EntityDataAccessor<Boolean> WATCHED = SynchedEntityData.defineId(Shade.class, EntityDataSerializers.BOOLEAN);
    /** Block light at which a Shade will not go on. */
    public static final int LIGHT_LIMIT = 8;
    private int watchedTicks;

    public Shade(EntityType<? extends Shade> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(WATCHED, false);
    }

    public boolean isWatched() {
        return this.entityData.get(WATCHED);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new StalkGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 32.0F));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** Whether any player is looking straight at this Shade and can see it. */
    private boolean isSeen() {
        for (Player player : this.level().players()) {
            if (player.isSpectator() || !player.isAlive() || player.distanceToSqr(this) > 64.0D * 64.0D) {
                continue;
            }
            Vec3 view = player.getViewVector(1.0F).normalize();
            Vec3 to = new Vec3(this.getX() - player.getX(), this.getEyeY() - player.getEyeY(), this.getZ() - player.getZ());
            double distance = to.length();
            if (view.dot(to.normalize()) > 1.0D - 0.05D / Math.max(1.0D, distance * 0.1D) - 0.12D && player.hasLineOfSight(this)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLit(Level level, BlockPos pos) {
        return level.getBrightness(LightLayer.BLOCK, pos) >= LIGHT_LIMIT;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            if (this.random.nextInt(4) == 0) {
                this.level().addParticle(ParticleTypes.SMOKE, this.getRandomX(0.4D), this.getRandomY(), this.getRandomZ(0.4D),
                        0.0D, 0.01D, 0.0D);
            }
            return;
        }
        if (this.level().isDay() && this.level().canSeeSky(this.blockPosition())) {
            // Sunrise: it crumbles to dust.
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(1.0D), this.getZ(),
                    30, 0.3D, 1.0D, 0.3D, 0.02D);
            this.playSound(SoundEvents.SOUL_ESCAPE, 1.5F, 0.5F);
            this.discard();
            return;
        }
        boolean seen = this.isSeen();
        if (seen != this.isWatched()) {
            this.entityData.set(WATCHED, seen);
            if (seen) {
                this.playSound(SoundEvents.WOODEN_DOOR_OPEN, 0.6F, 0.35F);
            }
        }
        if (seen) {
            this.watchedTicks++;
            this.getNavigation().stop();
            this.setDeltaMovement(0.0D, Math.min(0.0D, this.getDeltaMovement().y), 0.0D);
        } else {
            this.watchedTicks = 0;
        }
        if (isLit(this.level(), this.blockPosition()) && this.tickCount % 20 == 0) {
            // Light burns it: it shrinks back into the dark.
            this.hurt(this.damageSources().magic(), 2.0F);
            LivingEntity threat = this.getTarget();
            Vec3 away = threat != null ? DefaultRandomPos.getPosAway(this, 12, 7, threat.position())
                    : DefaultRandomPos.getPos(this, 12, 7);
            if (away != null && !seen) {
                this.getNavigation().moveTo(away.x, away.y, away.z, 1.3D);
            }
        }
    }

    @Override
    public boolean isPushable() {
        return !this.isWatched() && super.isPushable();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.isWatched()) {
            return false;
        }
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0), this);
        }
        return hurt;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isWatched() ? null : SoundEvents.SOUL_ESCAPE;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PHANTOM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PHANTOM_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F;
    }

    @Override
    public float getVoicePitch() {
        return 0.45F;
    }

    /** Surface only, in total darkness, at night, never near a dreamcatcher; more of them when there is no moon. */
    public static boolean checkShadeSpawnRules(EntityType<Shade> type, ServerLevelAccessor level, MobSpawnType reason,
                                               BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (reason != MobSpawnType.NATURAL && reason != MobSpawnType.CHUNK_GENERATION) {
            return Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
        }
        if (!level.canSeeSky(pos) || level.getBrightness(LightLayer.BLOCK, pos) > 0
                || !Monster.isDarkEnoughToSpawn(level, pos, random) || !checkMobSpawnRules(type, level, reason, pos, random)) {
            return false;
        }
        long time = level.dayTime() % 24000L;
        if (time < 13500L || time > 22500L) {
            return false;
        }
        int moon = level.getMoonPhase();
        if (random.nextInt(moon == 4 ? 1 : 3) != 0) {
            return false;
        }
        return !nearDreamcatcher(level.getLevel(), pos);
    }

    public static boolean nearDreamcatcher(ServerLevel level, BlockPos pos) {
        return level.getPoiManager().findClosest(holder -> holder.is(ModPoiTypes.DREAMCATCHER.getKey()), pos,
                DreamcatcherBlock.RADIUS, PoiManager.Occupancy.ANY).isPresent();
    }

    /** Creeps up on its target, but only while unseen, and never into the light. */
    static class StalkGoal extends Goal {
        private final Shade shade;
        private int attackCooldown;
        private int repath;

        StalkGoal(Shade shade) {
            this.shade = shade;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.shade.getTarget();
            return target != null && target.isAlive() && !this.shade.isWatched();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void stop() {
            this.shade.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = this.shade.getTarget();
            if (target == null) {
                return;
            }
            this.shade.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (isLit(this.shade.level(), target.blockPosition())) {
                // The target stands in the light: wait at the edge of the dark and watch.
                this.shade.getNavigation().stop();
                return;
            }
            double distance = this.shade.distanceToSqr(target);
            if (--this.repath <= 0) {
                this.repath = 5;
                this.shade.getNavigation().moveTo(target, distance > 36.0D ? 1.4D : 1.0D);
            }
            if (--this.attackCooldown <= 0 && distance < 3.2D) {
                this.attackCooldown = 20;
                this.shade.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                this.shade.doHurtTarget(target);
            }
        }
    }
}
