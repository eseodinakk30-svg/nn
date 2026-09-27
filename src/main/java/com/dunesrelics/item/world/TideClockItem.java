package com.dunesrelics.item.world;

import com.dunesrelics.memory.Tides;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Its needle follows the tide. Use it to read the sea: flood or ebb, and how long until it turns. */
public class TideClockItem extends Item {
    public TideClockItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            double tide = Tides.tide(level);
            String state = tide > 0.45D ? "high" : tide < -0.45D ? "low" : isRising(level) ? "rising" : "falling";
            long minutes = Math.max(1L, Tides.ticksToTurn(level) / 1200L);
            player.displayClientMessage(Component.translatable("item.dunesrelics.tide_clock." + state, minutes), true);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    private static boolean isRising(Level level) {
        return level.getDayTime() % 12000L >= 6000L;
    }

    /** 0 at low water, 1 at high water, for the item model's needle. */
    public static float needle(Level level) {
        return (float) ((Tides.tide(level) + 1.0D) / 2.0D);
    }
}
