package com.dunesrelics.item.world;

import com.dunesrelics.memory.Chronicle;
import com.dunesrelics.memory.WorldMemory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A book in which the world writes down what it remembers: roads, farms, hamlets, travellers, pirates. */
public class ChronicleItem extends Item {
    public ChronicleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level instanceof ServerLevel serverLevel && entity.tickCount % 40 == 0) {
            Chronicle.writeBook(serverLevel, stack);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            Chronicle.writeBook(serverLevel, stack);
        } else {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.dunesrelics.client.world.ClientHooks.openBook(stack));
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int entries = stack.hasTag() ? stack.getTag().getInt("dr_entries") : 0;
        tooltip.add(Component.translatable("item.dunesrelics.world_chronicle.entries", entries).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    /** Unused helper kept for tests: the number of entries the world has recorded so far. */
    public static int entries(ServerLevel level) {
        return WorldMemory.get(level).chronicle().size();
    }
}
