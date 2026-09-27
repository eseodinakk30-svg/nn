package com.dunesrelics.entity.volcanic;

import com.dunesrelics.registry.ModEntities;
import com.dunesrelics.registry.VolcanicItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.Nullable;

/**
 * The fire salamander: a lizard that lives by lava and walks across its surface.
 * Tame it with Fire Peppers. A tamed salamander follows you, fights beside you and sets its foes ablaze,
 * puts you out when you catch fire (eating the flames heals it), and now and then sheds a scale.
 */
public class Salamander extends TamableAnimal {
    private int scaleTime;
    private int fireEatCooldown;

    public Salamander(EntityType<? extends Salamander> type, Level level) {
        super(type, level);
        this.scaleTime = this.random.nextInt(6000) + 6000;
        this.setPathfindingMalus(BlockPathTypes.LAVA, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.1D, 8.0F, 2.0F, false));
        this.goalSelector.addGoal(5, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new TemptGoal(this, 1.1D, Ingredient.of(VolcanicItems.FIRE_PEPPER.get()), false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (this.isTame()) {
            if (this.isFood(stack) && this.getHealth() < this.getMaxHealth()) {
                if (!this.level().isClientSide) {
                    this.usePlayerItem(player, hand, stack);
                    this.heal(6.0F);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            InteractionResult result = super.mobInteract(player, hand);
            if (!result.consumesAction() && this.isOwnedBy(player)) {
                if (!this.level().isClientSide) {
                    this.setOrderedToSit(!this.isOrderedToSit());
                    this.jumping = false;
                    this.navigation.stop();
                    this.setTarget(null);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            return result;
        }
        if (this.isFood(stack)) {
            if (!this.level().isClientSide) {
                this.usePlayerItem(player, hand, stack);
                if (this.random.nextInt(3) == 0 && !ForgeEventFactory.onAnimalTame(this, player)) {
                    this.tame(player);
                    this.navigation.stop();
                    this.setTarget(null);
                    this.setOrderedToSit(true);
                    this.level().broadcastEntityEvent(this, (byte) 7);
                } else {
                    this.level().broadcastEntityEvent(this, (byte) 6);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            if (this.random.nextInt(20) == 0) {
                this.level().addParticle(ParticleTypes.SMALL_FLAME, this.getRandomX(0.5D), this.getY() + 0.3D,
                        this.getRandomZ(0.5D), 0.0D, 0.01D, 0.0D);
            }
            return;
        }
        if (!this.isTame() || this.isBaby()) {
            return;
        }
        if (--this.scaleTime <= 0) {
            this.spawnAtLocation(VolcanicItems.SALAMANDER_SCALE.get());
            this.playSound(SoundEvents.CHICKEN_EGG, 1.0F, 1.2F);
            this.scaleTime = this.random.nextInt(6000) + 6000;
        }
        if (this.fireEatCooldown > 0) {
            this.fireEatCooldown--;
            return;
        }
        LivingEntity owner = this.getOwner();
        if (owner != null && owner.isOnFire() && this.distanceToSqr(owner) < 64.0D) {
            owner.clearFire();
            this.heal(2.0F);
            this.fireEatCooldown = 40;
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.FLAME, owner.getX(), owner.getY() + 1.0D, owner.getZ(),
                    16, 0.3D, 0.5D, 0.3D, 0.02D);
            this.playSound(SoundEvents.FIRE_EXTINGUISH, 0.8F, 1.4F);
        }
    }

    @Override
    public boolean canStandOnFluid(FluidState fluid) {
        return fluid.is(FluidTags.LAVA);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt) {
            target.setSecondsOnFire(3);
        }
        return hurt;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(VolcanicItems.FIRE_PEPPER.get());
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Salamander baby = ModEntities.SALAMANDER.get().create(level);
        if (baby != null && this.isTame() && this.getOwnerUUID() != null) {
            baby.setOwnerUUID(this.getOwnerUUID());
            baby.setTame(true);
        }
        return baby;
    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ScaleTime", this.scaleTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ScaleTime")) {
            this.scaleTime = tag.getInt("ScaleTime");
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.AXOLOTL_IDLE_AIR;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AXOLOTL_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AXOLOTL_DEATH;
    }
}
