package com.dunesrelics.item.world;

import com.dunesrelics.DunesRelics;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Washed up at low tide. Uncork it: a treasure map, a pirate's chart, a pearl, or just a few words from far away. */
public class MessageInABottleItem extends Item {
    public static final net.minecraft.resources.ResourceLocation LOOT = DunesRelics.id("gameplay/message_in_a_bottle");

    public MessageInABottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            LootTable table = serverLevel.getServer().getLootData().getLootTable(LOOT);
            LootParams params = new LootParams.Builder(serverLevel).withParameter(LootContextParams.ORIGIN, player.position())
                    .withParameter(LootContextParams.THIS_ENTITY, player).withLuck(player.getLuck()).create(LootContextParamSets.CHEST);
            for (ItemStack found : table.getRandomItems(params)) {
                if (!player.getInventory().add(found)) {
                    player.drop(found, false);
                }
            }
            player.displayClientMessage(Component.translatable("item.dunesrelics.message_in_a_bottle.note."
                    + level.random.nextInt(6)), false);
            level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (!player.getInventory().add(bottle)) {
                    player.drop(bottle, false);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
