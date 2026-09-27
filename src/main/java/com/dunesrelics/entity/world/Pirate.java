package com.dunesrelics.entity.world;

import com.dunesrelics.registry.WorldItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

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
