package com.dunesrelics.entity.world;

import com.dunesrelics.registry.WorldItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** A pirate illager with a cutlass. Crews the pirate ships and comes ashore to plunder coastal villages. */
public class Pirate extends Vindicator implements PirateCrew {
    @Nullable
    private BlockPos raidTarget;

    public Pirate(EntityType<? extends Vindicator> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Vindicator.createAttributes()
                .add(Attributes.MAX_HEALTH, 22.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new MarchGoal<>(this, 1.0D));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(random.nextFloat() < 0.8F ? WorldItems.CUTLASS.get() : Items.IRON_SWORD));
    }

    @Override
    public void applyRaidBuffs(int wave, boolean unused) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(WorldItems.CUTLASS.get()));
    }

    // ------------------------------------------------------------------------------------------ duels

    @Nullable
    private UUID duelist;
    private boolean surrendered;
    private int surrenderedFor;

    @Nullable
    @Override
    public UUID getDuelist() {
        return this.duelist;
    }

    @Override
    public void setDuelist(@Nullable UUID duelist) {
        this.duelist = duelist;
    }

    @Override
    public boolean hasSurrendered() {
        return this.surrendered;
    }

    @Override
    public void surrender() {
        this.surrendered = true;
        this.duelist = null;
        Duels.surrender(this);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !Duels.holdsBack(this, target) && super.canAttack(target);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.surrendered) {
            this.surrenderedFor++;
        }
        Duels.tick(this, this, this.surrenderedFor);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Duels.onHurt(this, this, source);
        return super.hurt(source, amount);
    }

    @Nullable
    @Override
    public BlockPos getRaidTarget() {
        return this.raidTarget;
    }

    @Override
    public void setRaidTarget(@Nullable BlockPos target) {
        this.raidTarget = target;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Surrendered", this.surrendered);
        if (this.raidTarget != null) {
            tag.put("RaidTarget", NbtUtils.writeBlockPos(this.raidTarget));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.surrendered = tag.getBoolean("Surrendered");
        this.raidTarget = tag.contains("RaidTarget") ? NbtUtils.readBlockPos(tag.getCompound("RaidTarget")) : null;
    }
}
