package com.dunesrelics.entity;

import com.dunesrelics.registry.ModEntities;
import com.dunesrelics.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Meerkats live in small colonies on the dunes. They stand up on their hind legs to keep a lookout,
 * are immune to venom, and gang up on scorpions and scarabs. Breed them with dates.
 */
public class Meerkat extends Animal {
    private static final EntityDataAccessor<Boolean> STANDING = SynchedEntityData.defineId(Meerkat.class, EntityDataSerializers.BOOLEAN);

    private float standAnim;
    private float standAnimO;
    @Nullable
    private java.util.UUID trusted;
    private int alarmTicks;
    private int alarmCooldown;
    @Nullable
    private BlockPos jukebox;
    private boolean dancing;

    public Meerkat(EntityType<? extends Meerkat> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    public static boolean checkMeerkatSpawnRules(EntityType<? extends Meerkat> type, LevelAccessor level, MobSpawnType reason,
                                                 BlockPos pos, RandomSource random) {
        BlockState ground = level.getBlockState(pos.below());
        return (ground.is(BlockTags.SAND) || ground.is(BlockTags.DIRT)) && level.getRawBrightness(pos, 0) > 8;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STANDING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6D) {
            @Override
            protected boolean shouldPanic() {
                return this.mob.getLastHurtByMob() instanceof Player || this.mob.isOnFire() || this.mob.isFreezing();
            }
        });
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35D, true));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.2D, Ingredient.of(ModItems.DATES.get()), false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(6, new LookoutGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Scorpion.class, 10, true, false, e -> !this.isBaby()));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Scarab.class, 10, true, false, e -> !this.isBaby()));
    }

    public boolean isStanding() {
        return this.entityData.get(STANDING) || this.dancing;
    }

    /** True while music plays at a jukebox nearby (client side only, like dancing parrots). */
    public boolean isDancing() {
        return this.dancing;
    }

    @Override
    public void setRecordPlayingNearby(BlockPos pos, boolean playing) {
        this.jukebox = pos;
        this.dancing = playing;
    }

    /**
     * Meerkats love scorpions. Feed one a scorpion stinger and it will trust you: whenever you are nearby it keeps
     * watch, and when monsters approach it stands up, sounds the alarm and makes them glow.
     */
    @Override
    public InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.SCORPION_STINGER.get()) && !this.isBaby()) {
            if (!this.level().isClientSide) {
                this.usePlayerItem(player, hand, held);
                this.trusted = player.getUUID();
                this.heal(4.0F);
                ((ServerLevel) this.level()).sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                        this.getX(), this.getY() + 0.7D, this.getZ(), 4, 0.3D, 0.3D, 0.3D, 0.0D);
                this.playSound(SoundEvents.FOX_EAT, 1.0F, 1.5F);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    public boolean trusts(Player player) {
        return player.getUUID().equals(this.trusted);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.dancing && (this.jukebox == null || !this.jukebox.closerToCenterThan(this.position(), 3.46D)
                || !this.level().getBlockState(this.jukebox).is(net.minecraft.world.level.block.Blocks.JUKEBOX))) {
            this.dancing = false;
            this.jukebox = null;
        }
        if (this.level().isClientSide) {
            return;
        }
        if (this.alarmCooldown > 0) {
            this.alarmCooldown--;
        }
        if (this.alarmTicks > 0 && --this.alarmTicks == 0) {
            this.setStanding(false);
        }
        if (this.trusted == null || this.tickCount % 20 != 0) {
            return;
        }
        Player friend = this.level().getPlayerByUUID(this.trusted);
        if (friend == null || friend.distanceToSqr(this) > 24 * 24) {
            return;
        }
        java.util.List<net.minecraft.world.entity.Mob> threats = this.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                this.getBoundingBox().inflate(16.0D), e -> e instanceof net.minecraft.world.entity.monster.Enemy && e.isAlive());
        if (threats.isEmpty()) {
            return;
        }
        for (net.minecraft.world.entity.Mob threat : threats) {
            threat.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
        }
        this.setStanding(true);
        this.alarmTicks = 40;
        if (this.alarmCooldown == 0) {
            this.playSound(SoundEvents.FOX_SCREECH, 1.0F, 1.6F);
            this.alarmCooldown = 100;
        }
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.trusted != null) {
            tag.putUUID("Trusted", this.trusted);
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.trusted = tag.hasUUID("Trusted") ? tag.getUUID("Trusted") : null;
    }

    public void setStanding(boolean standing) {
        this.entityData.set(STANDING, standing);
    }

    /** Smoothed 0..1 progress of the lookout pose, for the model. */
    public float getStandAnim(float partialTick) {
        return Mth.lerp(partialTick, this.standAnimO, this.standAnim);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.standAnimO = this.standAnim;
            float target = this.isStanding() ? 1.0F : 0.0F;
            this.standAnim += (target - this.standAnim) * 0.25F;
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModItems.DATES.get());
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.MEERKAT.get().create(level);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect() != MobEffects.POISON && super.canBeAffected(effect);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FOX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.6F;
    }

    /** Stand up on the hind legs for a while and watch the horizon, especially when a player comes close. */
    static class LookoutGoal extends Goal {
        private final Meerkat meerkat;
        private int ticksLeft;
        @Nullable
        private Player watched;

        LookoutGoal(Meerkat meerkat) {
            this.meerkat = meerkat;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (!this.meerkat.onGround() || this.meerkat.isInWater() || this.meerkat.getTarget() != null || this.meerkat.isInLove()) {
                return false;
            }
            this.watched = this.meerkat.level().getNearestPlayer(this.meerkat, 10.0D);
            int chance = this.watched != null ? 25 : 180;
            return this.meerkat.getRandom().nextInt(chance) == 0;
        }

        @Override
        public void start() {
            this.ticksLeft = 60 + this.meerkat.getRandom().nextInt(100);
            this.meerkat.getNavigation().stop();
            this.meerkat.setStanding(true);
        }

        @Override
        public boolean canContinueToUse() {
            return this.ticksLeft > 0 && this.meerkat.getTarget() == null && this.meerkat.getLastHurtByMob() == null;
        }

        @Override
        public void tick() {
            this.ticksLeft--;
            if (this.watched != null && this.watched.isAlive()) {
                this.meerkat.getLookControl().setLookAt(this.watched, 30.0F, 30.0F);
            } else if (this.ticksLeft % 30 == 0) {
                double angle = this.meerkat.getRandom().nextDouble() * Math.PI * 2.0D;
                this.meerkat.getLookControl().setLookAt(this.meerkat.getX() + Math.cos(angle) * 8.0D,
                        this.meerkat.getEyeY() + 1.0D, this.meerkat.getZ() + Math.sin(angle) * 8.0D);
            }
        }

        @Override
        public void stop() {
            this.meerkat.setStanding(false);
            this.watched = null;
        }
    }
}
