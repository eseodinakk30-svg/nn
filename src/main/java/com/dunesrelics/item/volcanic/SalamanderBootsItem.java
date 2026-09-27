package com.dunesrelics.item.volcanic;

import com.dunesrelics.registry.VolcanicBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Boots made from shed salamander scales: lava under your feet cools into a walkable crust, like Frost Walker. */
public class SalamanderBootsItem extends ArmorItem {
    private static final int RADIUS = 2;

    public SalamanderBootsItem(ArmorMaterial material, Properties properties) {
        super(material, Type.BOOTS, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof LivingEntity living) || living.getItemBySlot(EquipmentSlot.FEET) != stack
                || !living.onGround()) {
            return;
        }
        coolLava(level, living.blockPosition());
    }

    public static void coolLava(Level level, BlockPos center) {
        BlockState crust = VolcanicBlocks.COOLED_LAVA_CRUST.get().defaultBlockState();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RADIUS, -1, -RADIUS), center.offset(RADIUS, -1, RADIUS))) {
            if (pos.closerThan(center.below(), RADIUS + 0.5D) && level.getBlockState(pos).is(Blocks.LAVA)
                    && level.getFluidState(pos).isSource() && level.getBlockState(pos.above()).isAir()) {
                level.setBlockAndUpdate(pos, crust);
            }
        }
    }
}
