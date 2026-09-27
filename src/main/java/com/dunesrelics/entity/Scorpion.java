package com.dunesrelics.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.dunesrelics.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;

/**
 * A desert scorpion. Like spiders, scorpions only hunt in the dark (or in the gloom of a sandstorm) and leave
 * you alone in daylight unless provoked. Their sting is venomous; they are immune to poison themselves.
 */
public class Scorpion extends Monster {
    /** Ticks until this scorpion's venom sac refills. */
    private int venomCooldown;

    public Scorpion(EntityType<? extends Scorpion> type, Level level) {
        super(type, level);
        this.xpReward = 6;
    }

    /** Hold a glass bottle up to a scorpion to milk its venom... if you dare. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.GLASS_BOTTLE)) {
            return super.mobInteract(player, hand);
        }
        if (this.venomCooldown > 0) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide) {
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(ModItems.SCORPION_VENOM.get())));
            this.playSound(SoundEvents.BOTTLE_FILL, 1.0F, 0.8F);
            this.venomCooldown = 6000;
            if (!player.getAbilities().instabuild) {
                this.setTarget(player);
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.venomCooldown > 0) {
            this.venomCooldown--;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("VenomCooldown", this.venomCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.venomCooldown = tag.getInt("VenomCooldown");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.ARMOR, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.35F));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, e -> this.isHunting()));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Meerkat.class, 10, true, false, e -> this.isHunting()));
    }

    private boolean isHunting() {
        return this.getLightLevelDependentMagicValue() < 0.5F || this.level().isRaining();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!super.doHurtTarget(target)) {
            return false;
        }
        if (target instanceof LivingEntity living) {
            int seconds = switch (this.level().getDifficulty()) {
                case NORMAL -> 6;
                case HARD -> 12;
                default -> 0;
            };
            if (seconds > 0) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, seconds * 20, 0), this);
            }
        }
        return true;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect() != MobEffects.POISON && super.canBeAffected(effect);
    }

    @Override
    public MobType getMobType() {
        return MobType.ARTHROPOD;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SPIDER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SPIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SPIDER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SPIDER_STEP, 0.15F, 1.3F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.3F;
    }
}
