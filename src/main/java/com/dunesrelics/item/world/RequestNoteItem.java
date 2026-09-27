package com.dunesrelics.item.world;

import com.dunesrelics.memory.Names;
import com.dunesrelics.memory.Requests;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A villager's request, written down: who asked, what for, how far along it is, and what he will pay. */
public class RequestNoteItem extends Item {
    public RequestNoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag note = Requests.request(stack);
        if (note == null) {
            return;
        }
        String key = "item.dunesrelics.request_note.";
        tooltip.add(Component.translatable(key + "from", Component.translatable(Names.villagerKey(note.getInt("VillagerName"))),
                Component.translatable(Names.villageKey(note.getInt("Village")))).withStyle(ChatFormatting.GRAY));
        switch (note.getString("Kind")) {
            case "bring" -> {
                Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(note.getString("Item")));
                tooltip.add(Component.translatable(key + "bring", note.getInt("Goal"), item.getDescription())
                        .withStyle(ChatFormatting.YELLOW));
            }
            case "hunt" -> tooltip.add(Component.translatable(key + "hunt", note.getInt("Progress"), note.getInt("Goal"))
                    .withStyle(ChatFormatting.YELLOW));
            default -> tooltip.add(Component.translatable(key + "find").withStyle(ChatFormatting.YELLOW));
        }
        tooltip.add(Component.translatable(key + "reward", note.getInt("Reward")).withStyle(ChatFormatting.GREEN));
    }
}
