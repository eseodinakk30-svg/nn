package com.dunesrelics.entity.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

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
        if (this.raidTarget != null) {
            tag.put("RaidTarget", NbtUtils.writeBlockPos(this.raidTarget));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.raidTarget = tag.contains("RaidTarget") ? NbtUtils.readBlockPos(tag.getCompound("RaidTarget")) : null;
    }
}
