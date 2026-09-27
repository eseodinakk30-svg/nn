package com.dunesrelics.entity.volcanic;

import com.dunesrelics.item.volcanic.MagmaHammerItem;
import com.dunesrelics.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Magma Titan: a colossus of basalt and molten rock that sleeps in the heart of every volcano.
 * <ul>
 *     <li>Punches send you flying and set you ablaze.</li>
 *     <li>Ground Slam: a burning shockwave around it.</li>
 *     <li>Magma Barrage: a volley of fireballs at targets that keep their distance.</li>
 *     <li>At half health it erupts: it calls two Magmalings and volcanic bombs rain around its foe.</li>
 * </ul>
 */
public class MagmaTitan extends Monster {
    private static final EntityDataAccessor<Integer> SLAM_ANIM = SynchedEntityData.defineId(MagmaTitan.class, EntityDataSerializers.INT);
    public static final int SLAM_DURATION = 16;

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10).setDarkenScreen(true);
    private int slamCooldown = 100;
    private int barrageCooldown = 60;
    private int eruptionCooldown = 200;
    private boolean enraged;

    public MagmaTitan(EntityType<? extends MagmaTitan> type, Level level) {
        super(type, level);
        this.xpReward = 150;
        this.setMaxUpStep(1.5F);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 320.0D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.ARMOR, 16.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SLAM_ANIM, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Remaining ticks of the ground slam animation (synced to clients). */
    public int getSlamAnim() {
        return this.entityData.get(SLAM_ANIM);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        int slam = this.getSlamAnim();
        if (slam > 0) {
            this.entityData.set(SLAM_ANIM, slam - 1);
            if (slam == SLAM_DURATION / 2) {
                MagmaHammerItem.slam((ServerLevel) this.level(), this, this.position(), 7.0D, 10.0F);
            }
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        float pace = this.enraged ? 0.6F : 1.0F;
        double distance = this.distanceTo(target);

        if (--this.slamCooldown <= 0 && distance < 6.5D && this.onGround() && slam == 0) {
            this.slamCooldown = (int) (140 * pace);
            this.entityData.set(SLAM_ANIM, SLAM_DURATION);
            this.playSound(SoundEvents.RAVAGER_ROAR, 1.5F, 0.6F);
        }
        if (--this.barrageCooldown <= 0 && distance > 5.0D && this.hasLineOfSight(target)) {
            this.barrageCooldown = (int) (80 * pace);
            this.barrage(level, target, this.enraged ? 5 : 3);
        }
        if (this.enraged && --this.eruptionCooldown <= 0) {
            this.eruptionCooldown = 160;
            for (int i = 0; i < 5; i++) {
                VolcanicBomb bomb = new VolcanicBomb(level, target.getX() + (this.random.nextDouble() - 0.5D) * 12.0D,
                        target.getY() + 18.0D + this.random.nextInt(6), target.getZ() + (this.random.nextDouble() - 0.5D) * 12.0D);
                bomb.setOwner(this);
                bomb.shoot(0.0D, -1.0D, 0.0D, 0.6F, 4.0F);
                level.addFreshEntity(bomb);
            }
            this.playSound(SoundEvents.GENERIC_EXPLODE, 2.0F, 0.5F);
        }
        if (!this.enraged && this.getHealth() < this.getMaxHealth() * 0.5F) {
            this.enrage(level, target);
        }
    }

    private void barrage(ServerLevel level, LivingEntity target, int count) {
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.5D) - this.getY(0.75D);
        double dz = target.getZ() - this.getZ();
        for (int i = 0; i < count; i++) {
            double spread = (i - (count - 1) / 2.0D) * 0.12D;
            SmallFireball fireball = new SmallFireball(level, this, dx + spread * dz, dy, dz - spread * dx);
            fireball.setPos(this.getX(), this.getY(0.75D), this.getZ());
            level.addFreshEntity(fireball);
        }
        this.playSound(SoundEvents.BLAZE_SHOOT, 2.0F, 0.6F);
    }

    private void enrage(ServerLevel level, LivingEntity target) {
        this.enraged = true;
        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1000000, 0, false, false));
        for (int i = 0; i < 2; i++) {
            Magmaling minion = ModEntities.MAGMALING.get().create(level);
            if (minion == null) {
                continue;
            }
            BlockPos pos = this.blockPosition().offset(this.random.nextInt(5) - 2, 0, this.random.nextInt(5) - 2);
            minion.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, this.random.nextFloat() * 360.0F, 0.0F);
            minion.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null, null);
            minion.setTarget(target);
            level.addFreshEntity(minion);
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 0.5F);
        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(32.0D))) {
            player.displayClientMessage(Component.translatable("entity.dunesrelics.magma_titan.enraged"), true);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt) {
            target.setSecondsOnFire(4);
            target.setDeltaMovement(target.getDeltaMovement().add(0.0D, 0.45D, 0.0D));
            target.hurtMarked = true;
        }
        return hurt;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Fire and lava only make it stronger.
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect() != MobEffects.POISON && effect.getEffect() != MobEffects.MOVEMENT_SLOWDOWN
                && effect.getEffect() != MobEffects.WEAKNESS && super.canBeAffected(effect);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Enraged", this.enraged);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.enraged = tag.getBoolean("Enraged");
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BLAZE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.5F, 0.5F);
    }

    @Override
    public float getVoicePitch() {
        return 0.45F + this.random.nextFloat() * 0.1F;
    }
}
