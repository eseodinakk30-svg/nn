package com.dunesrelics.entity.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** A pirate with a crossbow; also mans the ship's cannons. */
public class PirateGunner extends Pillager implements PirateCrew {
    @Nullable
    private BlockPos raidTarget;

    public PirateGunner(EntityType<? extends Pillager> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Pillager.createAttributes().add(Attributes.MAX_HEALTH, 22.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new MarchGoal<>(this, 1.0D));
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
