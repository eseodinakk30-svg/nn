package com.dunesrelics.memory;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The chronicle of the world's memory: roads laid, forests grown, villages built, pirates fought. It is read in the
 * World Chronicle book, which is simply a written book whose pages are refreshed from these entries.
 */
public final class Chronicle {
    private static final int ENTRIES_PER_PAGE = 3;

    private Chronicle() {}

    /**
     * One event. Arguments starting with '#' are translation keys (names), the rest are shown as they are.
     */
    public record Entry(long day, String key, List<String> args) {
        public Component text() {
            Object[] parts = new Object[this.args.size()];
            for (int i = 0; i < parts.length; i++) {
                String arg = this.args.get(i);
                parts[i] = arg.startsWith("#") ? Component.translatable(arg.substring(1)) : Component.literal(arg);
            }
            return Component.translatable(this.key, parts);
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("Day", this.day);
            tag.putString("Key", this.key);
            ListTag list = new ListTag();
            for (String arg : this.args) {
                list.add(StringTag.valueOf(arg));
            }
            tag.put("Args", list);
            return tag;
        }

        static Entry load(CompoundTag tag) {
            List<String> args = new ArrayList<>();
            for (Tag t : tag.getList("Args", Tag.TAG_STRING)) {
                args.add(t.getAsString());
            }
            return new Entry(tag.getLong("Day"), tag.getString("Key"), args);
        }
    }

    /** Writes the chronicle into the book's pages (the newest events on the last pages). */
    public static void writeBook(ServerLevel level, ItemStack book) {
        List<Entry> entries = WorldMemory.get(level).chronicle();
        CompoundTag tag = book.getOrCreateTag();
        if (tag.contains("pages") && tag.getInt("dr_entries") == entries.size()) {
            return;
        }
        ListTag pages = new ListTag();
        MutableComponent cover = Component.translatable("item.dunesrelics.world_chronicle.cover").withStyle(ChatFormatting.DARK_BLUE);
        if (entries.isEmpty()) {
            cover.append("\n\n").append(Component.translatable("item.dunesrelics.world_chronicle.empty").withStyle(ChatFormatting.GRAY));
        }
        pages.add(StringTag.valueOf(Component.Serializer.toJson(cover)));
        for (int start = 0; start < entries.size(); start += ENTRIES_PER_PAGE) {
            MutableComponent page = Component.empty();
            for (int i = start; i < Math.min(entries.size(), start + ENTRIES_PER_PAGE); i++) {
                Entry entry = entries.get(i);
                if (i > start) {
                    page.append("\n\n");
                }
                page.append(Component.translatable("item.dunesrelics.world_chronicle.day", entry.day() + 1)
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                page.append("\n");
                page.append(entry.text());
            }
            pages.add(StringTag.valueOf(Component.Serializer.toJson(page)));
        }
        tag.put("pages", pages);
        tag.putString("title", "World Chronicle");
        tag.putString("author", "The World");
        tag.putBoolean("resolved", true);
        tag.putInt("dr_entries", entries.size());
    }
}
