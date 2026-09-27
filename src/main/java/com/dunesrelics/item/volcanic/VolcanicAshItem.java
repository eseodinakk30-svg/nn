package com.dunesrelics.item.volcanic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Volcanic ash makes rich soil: it works just like bone meal. */
public class VolcanicAshItem extends Item {
    public VolcanicAshItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (BoneMealItem.applyBonemeal(context.getItemInHand(), level, pos, context.getPlayer())) {
            if (!level.isClientSide) {
                level.levelEvent(1505, pos, 0);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
