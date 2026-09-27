package com.dunesrelics.memory;

import com.dunesrelics.registry.WorldItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Villagers asking for help. A villager who likes you may ask you to bring him something of his trade, to clear the
 * monsters that prowl round the village at night, or to find his cow that wandered off. He writes it down on a note
 * (which keeps count of how it is going) and pays when you come back to him.
 */
public final class Requests {
    public static final String ACTIVE = "dr_request";

    private Requests() {}

    // ------------------------------------------------------------------------------------------ asking

    /** What a villager of this trade might ask for: {item, count} pairs. */
    private static Object[][] wishes(VillagerProfession profession) {
        String name = profession.name();
        return switch (name) {
            case "farmer" -> new Object[][]{{Items.BONE_MEAL, 16}, {Items.PUMPKIN_PIE, 4}};
            case "fisherman" -> new Object[][]{{Items.COOKED_COD, 8}, {Items.STRING, 12}};
            case "librarian" -> new Object[][]{{Items.PAPER, 24}, {Items.BOOK, 4}};
            case "cleric" -> new Object[][]{{Items.ROTTEN_FLESH, 16}, {Items.GLASS_BOTTLE, 8}};
            case "armorer", "weaponsmith", "toolsmith" -> new Object[][]{{Items.IRON_INGOT, 8}, {Items.COAL, 16}};
            case "butcher" -> new Object[][]{{Items.COAL, 8}, {Items.SWEET_BERRIES, 16}};
            case "cartographer" -> new Object[][]{{Items.GLASS_PANE, 12}, {Items.COMPASS, 1}};
            case "fletcher" -> new Object[][]{{Items.STICK, 32}, {Items.FEATHER, 12}};
            case "leatherworker" -> new Object[][]{{Items.LEATHER, 8}, {Items.RABBIT_HIDE, 6}};
            case "mason" -> new Object[][]{{Items.CLAY_BALL, 16}, {Items.STONE, 32}};
            case "shepherd" -> new Object[][]{{Items.WHITE_WOOL, 8}, {Items.SHEARS, 1}};
            default -> new Object[][]{{Items.BREAD, 6}, {Items.POPPY, 4}};
        };
    }

    /**
     * Maybe asks the player for a favour (a fifth of the time, one request per villager at a time). Returns true if
     * a note was handed over.
     */
    public static boolean maybeAsk(ServerLevel level, WorldMemory memory, Villager villager, Player player, RandomSource random) {
        CompoundTag data = villager.getPersistentData();
        if (data.getBoolean(ACTIVE) || villager.isBaby() || random.nextInt(5) != 0 || hasNoteFor(player, villager.getUUID())) {
            return false;
        }
        WorldMemory.VillageRecord village = memory.nearestVillage(villager.blockPosition(), 64.0D);
        if (village == null) {
            return false;
        }
        CompoundTag note = new CompoundTag();
        note.putUUID("Villager", villager.getUUID());
        note.putInt("VillagerName", villager.getPersistentData().getInt(VillageLife.NAME));
        note.putLong("Bell", village.bell.asLong());
        note.putInt("Village", village.name);
        int kind = random.nextInt(level.isNight() ? 2 : 3);
        if (kind == 0) {
            Object[][] wishes = wishes(villager.getVillagerData().getProfession());
            Object[] wish = wishes[random.nextInt(wishes.length)];
            note.putString("Kind", "bring");
            note.putString("Item", BuiltInRegistries.ITEM.getKey((Item) wish[0]).toString());
            note.putInt("Goal", (Integer) wish[1]);
            note.putInt("Reward", 3 + (Integer) wish[1] / 4);
        } else if (kind == 1) {
            note.putString("Kind", "hunt");
            note.putInt("Goal", 3 + random.nextInt(4));
            note.putInt("Reward", 6 + random.nextInt(5));
        } else {
            Cow cow = lostCow(level, village.bell, random);
            if (cow == null) {
                return false;
            }
            note.putString("Kind", "find");
            note.putUUID("Animal", cow.getUUID());
            note.putInt("Goal", 1);
            note.putInt("Reward", 8 + random.nextInt(5));
        }
        note.putInt("Progress", 0);
        ItemStack stack = new ItemStack(WorldItems.REQUEST_NOTE.get());
        stack.getOrCreateTag().put("Request", note);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        data.putBoolean(ACTIVE, true);
        VillageLife.say(villager, player, "villager.dunesrelics.request." + note.getString("Kind"));
        level.playSound(null, villager.blockPosition(), SoundEvents.VILLAGER_TRADE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        return true;
    }

    /** A cow wandered off: somewhere 40 to 70 blocks from the bell, named, and in no hurry to come back. */
    @Nullable
    private static Cow lostCow(ServerLevel level, BlockPos bell, RandomSource random) {
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = 40.0D + random.nextDouble() * 30.0D;
            int x = bell.getX() + (int) (Math.cos(angle) * distance);
            int z = bell.getZ() + (int) (Math.sin(angle) * distance);
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!level.isLoaded(pos) || !level.getBlockState(pos.below()).getFluidState().isEmpty()) {
                continue;
            }
            Cow cow = EntityType.COW.create(level);
            if (cow == null) {
                return null;
            }
            cow.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            cow.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            cow.setCustomName(Component.translatable("name.dunesrelics.cow." + random.nextInt(8)));
            cow.setPersistenceRequired();
            level.addFreshEntity(cow);
            return cow;
        }
        return null;
    }

    // ------------------------------------------------------------------------------------------ keeping count

    @Nullable
    public static CompoundTag request(ItemStack stack) {
        return stack.is(WorldItems.REQUEST_NOTE.get()) && stack.hasTag() && stack.getTag().contains("Request")
                ? stack.getTag().getCompound("Request") : null;
    }

    private static boolean hasNoteFor(Player player, UUID villager) {
        for (ItemStack stack : player.getInventory().items) {
            CompoundTag note = request(stack);
            if (note != null && note.hasUUID("Villager") && note.getUUID("Villager").equals(villager)) {
                return true;
            }
        }
        return false;
    }

    /** A monster killed near a village: counts towards the player's hunting requests for that village. */
    public static void onMonsterKilled(Player player, LivingEntity victim) {
        for (ItemStack stack : player.getInventory().items) {
            CompoundTag note = request(stack);
            if (note == null || !note.getString("Kind").equals("hunt") || note.getInt("Progress") >= note.getInt("Goal")) {
                continue;
            }
            BlockPos bell = BlockPos.of(note.getLong("Bell"));
            if (victim.blockPosition().closerThan(bell, 64.0D)) {
                note.putInt("Progress", note.getInt("Progress") + 1);
                if (note.getInt("Progress") >= note.getInt("Goal")) {
                    player.displayClientMessage(Component.translatable("item.dunesrelics.request_note.done"), true);
                }
            }
        }
    }

    /**
     * The player comes back to the villager who asked: if the request is done (or he has brought the goods), it is
     * settled. Returns true if a request of this villager's was handled.
     */
    public static boolean tryComplete(ServerLevel level, Villager villager, Player player, ItemStack held) {
        for (ItemStack stack : player.getInventory().items) {
            CompoundTag note = request(stack);
            if (note == null || !note.hasUUID("Villager") || !note.getUUID("Villager").equals(villager.getUUID())) {
                continue;
            }
            boolean done = switch (note.getString("Kind")) {
                case "bring" -> {
                    Item wanted = BuiltInRegistries.ITEM.get(new ResourceLocation(note.getString("Item")));
                    int goal = note.getInt("Goal");
                    if (held.is(wanted) && held.getCount() >= goal) {
                        held.shrink(goal);
                        yield true;
                    }
                    yield false;
                }
                case "hunt" -> note.getInt("Progress") >= note.getInt("Goal");
                case "find" -> {
                    Entity animal = note.hasUUID("Animal") ? level.getEntity(note.getUUID("Animal")) : null;
                    yield animal != null && animal.isAlive() && animal.distanceTo(villager) < 16.0F;
                }
                default -> false;
            };
            if (!done) {
                VillageLife.say(villager, player, "villager.dunesrelics.request.waiting");
                return true;
            }
            reward(level, villager, player, note);
            stack.shrink(1);
            return true;
        }
        return false;
    }

    private static void reward(ServerLevel level, Villager villager, Player player, CompoundTag note) {
        int emeralds = note.getInt("Reward");
        ItemStack pay = new ItemStack(Items.EMERALD, emeralds);
        if (!player.getInventory().add(pay)) {
            player.drop(pay, false);
        }
        villager.getGossips().add(player.getUUID(), GossipType.MAJOR_POSITIVE, 10);
        villager.getPersistentData().putBoolean(ACTIVE, false);
        level.broadcastEntityEvent(villager, (byte) 14);
        level.playSound(null, villager.blockPosition(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        VillageLife.say(villager, player, "villager.dunesrelics.request.thanks", emeralds);
        WorldMemory memory = WorldMemory.get(level);
        WorldMemory.VillageRecord village = memory.villageAt(BlockPos.of(note.getLong("Bell")));
        if (village != null) {
            village.prosperity += 2;
            memory.setDirty();
        }
    }
}
