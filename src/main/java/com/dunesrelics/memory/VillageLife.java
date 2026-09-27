package com.dunesrelics.memory;

import com.dunesrelics.entity.world.PirateCrew;
import com.dunesrelics.entity.world.VillageWorker;
import com.dunesrelics.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Smart villagers and growing villages.
 * <ul>
 *     <li>Villages grow: every few days they add a house, a lamp, a well, a market stall or a field, faster when the
 *     players trade with them.</li>
 *     <li>Villagers have names and characters, remember the players (greetings, gifts back and forth, grudges when
 *     their homes are wrecked, gratitude when monsters are fought off) and say so.</li>
 *     <li>They work: fishermen bring in fish, farmers widen their fields, smiths take up arms against raiders and
 *     pirates, and the bell rings the curfew at dusk.</li>
 * </ul>
 * Reputation is kept in the vanilla gossip system, so prices follow it too.
 */
public final class VillageLife {
    public static final String NAME = "dr_name";
    public static final String TRAIT = "dr_trait";
    private static final String GIFT_DAY = "dr_gift_day";
    private static final String GIFTS_TODAY = "dr_gifts_today";
    private static final String GAVE_DAY = "dr_gave_day";
    private static final String GREETED = "dr_greeted";
    private static final String SEEN = "dr_seen";
    private static final String ARMED = "dr_armed";

    public static final int FRIENDLY = 0;
    public static final int GRUMPY = 1;
    public static final int GREEDY = 2;
    public static final int BRAVE = 3;

    private VillageLife() {}

    // ------------------------------------------------------------------------------------------ identity

    public static void ensureIdentity(Villager villager) {
        CompoundTag data = villager.getPersistentData();
        if (!data.contains(NAME)) {
            data.putInt(NAME, Names.randomVillager(villager.getRandom()));
            data.putInt(TRAIT, villager.getRandom().nextInt(Names.TRAITS));
        }
    }

    public static Component name(Villager villager) {
        return Component.translatable(Names.villagerKey(villager.getPersistentData().getInt(NAME)));
    }

    public static int trait(Villager villager) {
        return villager.getPersistentData().getInt(TRAIT);
    }

    private static Component profession(Villager villager) {
        VillagerProfession profession = villager.getVillagerData().getProfession();
        return Component.translatable("entity.minecraft.villager." + profession.name());
    }

    /** "Name (profession): line" in the action bar. */
    public static void say(Villager villager, Player player, String line, Object... args) {
        Component text = Component.translatable("villager.dunesrelics.says", name(villager), profession(villager),
                Component.translatable(line, args));
        player.displayClientMessage(text, true);
    }

    private static String traitKey(Villager villager) {
        return switch (trait(villager)) {
            case GRUMPY -> "grumpy";
            case GREEDY -> "greedy";
            case BRAVE -> "brave";
            default -> "friendly";
        };
    }

    // ------------------------------------------------------------------------------------------ greetings and gifts

    /** Called every second for each player: the nearest villager in view may greet them, or toss them a gift. */
    public static void greet(ServerLevel level, ServerPlayer player, long day) {
        if (player.isSpectator()) {
            return;
        }
        List<Villager> near = level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(5.0D),
                v -> v.isAlive() && !v.isBaby() && !v.isSleeping() && v.hasLineOfSight(player) && !v.isTrading());
        if (near.isEmpty()) {
            return;
        }
        Villager villager = near.get(level.random.nextInt(near.size()));
        ensureIdentity(villager);
        CompoundTag data = villager.getPersistentData();
        CompoundTag greeted = data.getCompound(GREETED);
        String id = player.getStringUUID();
        long now = level.getGameTime();
        if (now - greeted.getLong(id) < 6000L) {
            return;
        }
        greeted.putLong(id, now);
        data.put(GREETED, greeted);
        CompoundTag seen = data.getCompound(SEEN);
        long lastSeen = seen.contains(id) ? seen.getLong(id) : -1L;
        seen.putLong(id, day);
        data.put(SEEN, seen);

        int reputation = villager.getPlayerReputation(player);
        String tier = reputation < -20 ? "hostile" : reputation < 20 ? "neutral" : reputation < 60 ? "friendly" : "hero";
        villager.getLookControl().setLookAt(player);
        if (lastSeen >= 0 && day - lastSeen >= 3 && reputation >= 0) {
            say(villager, player, "villager.dunesrelics.missed", player.getDisplayName());
        } else if (reputation >= 0 && Requests.maybeAsk(level, WorldMemory.get(level), villager, player, level.random)) {
            // asked a favour instead of passing the time of day
        } else if (reputation >= 0 && level.random.nextInt(4) == 0 && tellNews(level, villager, player, day)) {
            // told the latest news
        } else {
            say(villager, player, "villager.dunesrelics.greet." + tier + "." + traitKey(villager), player.getDisplayName());
        }
        if (reputation >= 20) {
            level.broadcastEntityEvent(villager, (byte) 14);
            maybeGiveGift(level, villager, player, day, reputation);
        } else if (reputation < -20) {
            level.broadcastEntityEvent(villager, (byte) 13);
        }
    }

    /** "Have you heard? Pirates landed near Pine Shore!" (something from the chronicle of the last three days). */
    private static boolean tellNews(ServerLevel level, Villager villager, ServerPlayer player, long day) {
        List<Chronicle.Entry> recent = WorldMemory.get(level).chronicle().stream().filter(e -> e.day() >= day - 3).toList();
        if (recent.isEmpty()) {
            return false;
        }
        Chronicle.Entry entry = recent.get(level.random.nextInt(recent.size()));
        say(villager, player, "villager.dunesrelics.news", entry.text());
        return true;
    }

    private static void maybeGiveGift(ServerLevel level, Villager villager, ServerPlayer player, long day, int reputation) {
        CompoundTag data = villager.getPersistentData();
        if (data.getLong(GAVE_DAY) == day + 1) {
            return;
        }
        float chance = (reputation >= 60 ? 0.45F : 0.2F) + (trait(villager) == FRIENDLY ? 0.15F : 0.0F)
                - (trait(villager) == GREEDY ? 0.15F : 0.0F);
        if (level.random.nextFloat() >= chance) {
            return;
        }
        VillagerProfession profession = villager.getVillagerData().getProfession();
        ResourceLocation tableId = profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT
                ? new ResourceLocation("gameplay/hero_of_the_village/baby_gift")
                : new ResourceLocation("gameplay/hero_of_the_village/" + profession.name() + "_gift");
        LootTable table = level.getServer().getLootData().getLootTable(tableId);
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, villager.position())
                .withParameter(LootContextParams.THIS_ENTITY, villager).create(LootContextParamSets.GIFT);
        List<ItemStack> gifts = table.getRandomItems(params);
        if (gifts.isEmpty()) {
            return;
        }
        for (ItemStack gift : gifts) {
            BehaviorUtils.throwItem(villager, gift, player.position());
        }
        data.putLong(GAVE_DAY, day + 1);
        say(villager, player, "villager.dunesrelics.gift." + traitKey(villager));
    }

    /** A sneaking player offering a flower, a treat or an emerald: the villager accepts it and remembers. */
    public static boolean receiveGift(ServerLevel level, Villager villager, Player player, ItemStack stack, long day) {
        if (!stack.is(ModTags.VILLAGER_GIFTS) || villager.isBaby() || villager.isSleeping()) {
            return false;
        }
        ensureIdentity(villager);
        CompoundTag data = villager.getPersistentData();
        if (data.getLong(GIFT_DAY) != day + 1) {
            data.putLong(GIFT_DAY, day + 1);
            data.putInt(GIFTS_TODAY, 0);
        }
        if (data.getInt(GIFTS_TODAY) >= 3) {
            say(villager, player, "villager.dunesrelics.enough");
            return true;
        }
        data.putInt(GIFTS_TODAY, data.getInt(GIFTS_TODAY) + 1);
        boolean emerald = stack.is(Items.EMERALD);
        int amount = trait(villager) == GREEDY ? (emerald ? 20 : 4) : trait(villager) == GRUMPY ? 6 : 10;
        villager.getGossips().add(player.getUUID(), GossipType.MINOR_POSITIVE, amount);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.broadcastEntityEvent(villager, (byte) 12);
        level.playSound(null, villager.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
        say(villager, player, "villager.dunesrelics.thanks." + traitKey(villager));
        return true;
    }

    /** Wrecking a villager's bed, workplace, bell or door is not forgotten. */
    public static void onBlockBroken(ServerLevel level, Player player, BlockPos pos, BlockState state) {
        boolean poi = PoiTypes.forState(state).isPresent();
        boolean property = poi || state.is(BlockTags.DOORS) || state.is(Blocks.CHEST) || state.is(Blocks.BARREL);
        if (!property) {
            return;
        }
        List<Villager> witnesses = level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(16.0D),
                v -> v.isAlive() && !v.isSleeping() && v.hasLineOfSight(player));
        if (witnesses.isEmpty() || level.getPoiManager().findClosest(h -> h.is(PoiTypes.MEETING), pos, 48,
                PoiManager.Occupancy.ANY).isEmpty()) {
            return;
        }
        int weight = state.is(Blocks.BELL) || state.is(BlockTags.BEDS) ? 25 : 12;
        for (Villager villager : witnesses) {
            ensureIdentity(villager);
            villager.getGossips().add(player.getUUID(), GossipType.MINOR_NEGATIVE, weight);
            level.broadcastEntityEvent(villager, (byte) 13);
        }
        say(witnesses.get(0), player, "villager.dunesrelics.angry." + traitKey(witnesses.get(0)));
    }

    /** Players who fight off monsters, raiders and pirates near a village are thanked for it. */
    public static void onMonsterKilled(ServerLevel level, Player player, LivingEntity victim) {
        boolean enemy = victim instanceof Raider || victim instanceof PirateCrew;
        List<Villager> witnesses = level.getEntitiesOfClass(Villager.class, victim.getBoundingBox().inflate(24.0D),
                v -> v.isAlive() && !v.isBaby());
        for (Villager villager : witnesses) {
            villager.getGossips().add(player.getUUID(), GossipType.MINOR_POSITIVE, enemy ? 8 : 3);
        }
        if (enemy && !witnesses.isEmpty() && level.random.nextInt(3) == 0) {
            Villager speaker = witnesses.get(level.random.nextInt(witnesses.size()));
            ensureIdentity(speaker);
            say(speaker, player, "villager.dunesrelics.cheer");
        }
    }

    // ------------------------------------------------------------------------------------------ work

    /** Called every five seconds for the villagers around each player. */
    public static void work(ServerLevel level, Villager villager, RandomSource random) {
        if (villager.isBaby() || !villager.getBrain().isActive(Activity.WORK)) {
            return;
        }
        VillagerProfession profession = villager.getVillagerData().getProfession();
        BlockPos site = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).map(GlobalPos::pos).orElse(null);
        if (site == null) {
            return;
        }
        if (profession == VillagerProfession.FISHERMAN && random.nextInt(10) == 0) {
            fish(level, villager, site, random);
        } else if (profession == VillagerProfession.FARMER && random.nextInt(18) == 0) {
            widenField(level, site, random);
        }
    }

    /** The fisherman brings his catch home to his barrel. */
    private static void fish(ServerLevel level, Villager villager, BlockPos barrel, RandomSource random) {
        if (!(level.getBlockEntity(barrel) instanceof BarrelBlockEntity container)) {
            return;
        }
        int fish = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.is(Items.COD) || stack.is(Items.SALMON)) {
                fish += stack.getCount();
            }
        }
        if (fish >= 24) {
            return;
        }
        for (int attempt = 0; attempt < 24; attempt++) {
            BlockPos water = barrel.offset(random.nextInt(21) - 10, random.nextInt(5) - 3, random.nextInt(21) - 10);
            if (level.getFluidState(water).is(FluidTags.WATER) && level.getFluidState(water).isSource()
                    && level.getBlockState(water.above()).isAir()) {
                HopperBlockEntity.addItem(null, container, new ItemStack(random.nextInt(10) < 7 ? Items.COD : Items.SALMON), null);
                level.sendParticles(ParticleTypes.SPLASH, water.getX() + 0.5D, water.getY() + 1.0D, water.getZ() + 0.5D,
                        12, 0.3D, 0.1D, 0.3D, 0.1D);
                level.playSound(null, water, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.6F, 1.0F);
                return;
            }
        }
    }

    /** The farmer tills a little more ground next to the field, where water is close enough. */
    private static void widenField(ServerLevel level, BlockPos composter, RandomSource random) {
        int farmland = 0;
        for (BlockPos p : BlockPos.betweenClosed(composter.offset(-8, -2, -8), composter.offset(8, 1, 8))) {
            if (level.getBlockState(p).is(Blocks.FARMLAND)) {
                farmland++;
            }
        }
        if (farmland == 0 || farmland >= 48) {
            return;
        }
        for (int attempt = 0; attempt < 30; attempt++) {
            BlockPos p = composter.offset(random.nextInt(13) - 6, random.nextInt(3) - 2, random.nextInt(13) - 6);
            if (!level.getBlockState(p).is(Blocks.GRASS_BLOCK) && !level.getBlockState(p).is(Blocks.DIRT)
                    || !level.getBlockState(p.above()).isAir()) {
                continue;
            }
            BlockState crop = null;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (level.getBlockState(p.relative(direction)).is(Blocks.FARMLAND)) {
                    BlockState neighbor = level.getBlockState(p.relative(direction).above());
                    crop = neighbor.getBlock() instanceof CropBlock cropBlock ? cropBlock.getStateForAge(0) : Blocks.WHEAT.defaultBlockState();
                    break;
                }
            }
            if (crop == null || !nearWater(level, p)) {
                continue;
            }
            level.setBlock(p, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(p.above(), crop, Block.UPDATE_ALL);
            return;
        }
    }

    private static boolean nearWater(ServerLevel level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 1, 4))) {
            if (level.getFluidState(p).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Smiths, butchers, masons and the brave take up weapons when raiders or pirates come, and strike those who get
     * close; they put the weapons away when it is over. Called every half second.
     */
    public static void defend(ServerLevel level, Villager villager) {
        if (villager.isBaby() || villager.isSleeping()) {
            return;
        }
        CompoundTag data = villager.getPersistentData();
        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, villager.getBoundingBox().inflate(16.0D),
                e -> e.isAlive() && (e instanceof Raider || e instanceof PirateCrew));
        boolean danger = !enemies.isEmpty() || level.getRaidAt(villager.blockPosition()) != null;
        if (!danger) {
            if (data.getBoolean(ARMED)) {
                data.putBoolean(ARMED, false);
                villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
            return;
        }
        VillagerProfession profession = villager.getVillagerData().getProfession();
        boolean fighter = profession == VillagerProfession.ARMORER || profession == VillagerProfession.WEAPONSMITH
                || profession == VillagerProfession.TOOLSMITH || profession == VillagerProfession.BUTCHER
                || profession == VillagerProfession.MASON || trait(villager) == BRAVE;
        if (!fighter) {
            return;
        }
        if (!data.getBoolean(ARMED) && villager.getMainHandItem().isEmpty()) {
            data.putBoolean(ARMED, true);
            villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(
                    profession == VillagerProfession.BUTCHER || profession == VillagerProfession.TOOLSMITH ? Items.IRON_AXE : Items.IRON_SWORD));
        }
        if (!data.getBoolean(ARMED)) {
            return;
        }
        for (LivingEntity enemy : enemies) {
            if (enemy.distanceToSqr(villager) < 2.8D * 2.8D) {
                villager.getLookControl().setLookAt(enemy);
                villager.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                enemy.hurt(level.damageSources().mobAttack(villager), 5.0F);
                enemy.knockback(0.4D, villager.getX() - enemy.getX(), villager.getZ() - enemy.getZ());
                break;
            }
        }
    }

    /** At dusk the village bell rings, and everybody heads home. */
    public static void curfew(ServerLevel level, WorldMemory memory) {
        for (WorldMemory.VillageRecord record : memory.villages()) {
            if (!level.isLoaded(record.bell) || !WorldReactions.isAttended(level, record.bell)) {
                continue;
            }
            BlockState state = level.getBlockState(record.bell);
            if (state.getBlock() instanceof BellBlock bell) {
                bell.attemptToRing(level, record.bell, null);
            }
        }
    }

    // ------------------------------------------------------------------------------------------ growth

    /** Makes the world notice every village whose bell is near a player. */
    public static void discoverVillages(ServerLevel level, WorldMemory memory, long day) {
        Set<BlockPos> bells = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            bells.addAll(level.getPoiManager().findAll(h -> h.is(PoiTypes.MEETING), p -> true, player.blockPosition(), 96,
                    PoiManager.Occupancy.ANY).map(BlockPos::immutable).collect(Collectors.toSet()));
        }
        for (BlockPos bell : bells) {
            memory.village(bell, day, Names.randomVillage(level.random));
        }
    }

    /** Each morning a village near a player may take on one project. */
    public static void growVillages(ServerLevel level, WorldMemory memory, long day, boolean force) {
        discoverVillages(level, memory, day);
        for (WorldMemory.VillageRecord record : memory.villages()) {
            if (!level.isLoaded(record.bell) || !WorldReactions.isAttended(level, record.bell) || record.lastGrowthDay == day
                    || !(level.getBlockState(record.bell).getBlock() instanceof BellBlock)) {
                continue;
            }
            List<Villager> villagers = level.getEntitiesOfClass(Villager.class, new AABB(record.bell).inflate(48.0D));
            if (villagers.isEmpty()) {
                continue;
            }
            expand(level, memory, record, day, villagers.size());
            float chance = 0.5F + Math.min(record.prosperity, 50) * 0.01F;
            if (!force && level.random.nextFloat() >= chance) {
                continue;
            }
            // one building at a time for each builder the village has
            long builders = level.getEntitiesOfClass(VillageWorker.class, new AABB(record.bell).inflate(96.0D),
                    w -> w.getJob() == VillageWorker.Job.BUILDER && record.bell.equals(w.getHomeBell())).size();
            if (record.sites.size() >= Math.max(1L, builders)) {
                continue;
            }
            String project = grow(level, memory, record, villagers.size(), level.random);
            if (project != null) {
                record.lastGrowthDay = day;
                record.projects++;
                memory.setDirty();
            }
        }
    }

    /** A family moves into an empty bed: a new villager comes to the bell. Returns false if there is no free bed. */
    public static boolean settle(ServerLevel level, WorldMemory.VillageRecord record) {
        if (!level.isLoaded(record.bell)) {
            return false;
        }
        long beds = level.getPoiManager().getCountInRange(h -> h.is(PoiTypes.HOME), record.bell, 64, PoiManager.Occupancy.ANY);
        int villagers = level.getEntitiesOfClass(Villager.class, new AABB(record.bell).inflate(64.0D)).size();
        if (beds <= villagers) {
            return false;
        }
        Villager villager = EntityType.VILLAGER.create(level);
        if (villager == null) {
            return false;
        }
        BlockPos spot = record.bell.relative(Direction.Plane.HORIZONTAL.getRandomDirection(level.random), 2);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spot.getX(), spot.getZ());
        villager.setVillagerData(villager.getVillagerData().setType(VillagerType.byBiome(level.getBiome(record.bell))));
        villager.moveTo(spot.getX() + 0.5D, y, spot.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        villager.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.BREEDING, null, null);
        level.addFreshEntity(villager);
        return true;
    }

    /**
     * A village reaching out: a road to the nearest village it is not yet joined to (within 320 blocks), and once it
     * has sixteen villagers, every six days some of them leave to found a hamlet of their own a little way off.
     */
    public static void expand(ServerLevel level, WorldMemory memory, WorldMemory.VillageRecord record, long day, int villagers) {
        WorldMemory.VillageRecord neighbour = null;
        double best = 320.0D * 320.0D;
        for (WorldMemory.VillageRecord other : memory.villages()) {
            double d = other.bell.distSqr(record.bell);
            if (other != record && d < best && d > 32 * 32 && !record.roads.contains(other.bell.asLong())) {
                best = d;
                neighbour = other;
            }
        }
        if (neighbour != null && level.isLoaded(neighbour.bell)) {
            Roads.build(level, memory, record, neighbour, day);
        }
        if (villagers >= 16 && day - record.lastDaughter >= 6) {
            record.lastDaughter = day;
            for (int attempt = 0; attempt < 12; attempt++) {
                double angle = level.random.nextDouble() * Math.PI * 2.0D;
                double distance = 80.0D + level.random.nextDouble() * 60.0D;
                BlockPos spot = record.bell.offset((int) (Math.cos(angle) * distance), 0, (int) (Math.sin(angle) * distance));
                if (!level.isLoaded(spot) || memory.nearestVillage(spot, 64.0D) != null) {
                    continue;
                }
                BlockPos bell = WorldReactions.buildHamlet(level, memory, spot, level.random);
                if (bell == null) {
                    continue;
                }
                WorldMemory.VillageRecord daughter = memory.village(bell, day, Names.randomVillage(level.random));
                memory.record(day, "chronicle.dunesrelics.daughter", "#" + record.nameKey(), "#" + daughter.nameKey());
                Roads.build(level, memory, record, daughter, day);
                break;
            }
            memory.setDirty();
        }
    }

    /**
     * What the village needs most: beds first, then water, a storehouse for its workers, flour, a smithy, safety, a
     * chapel, and after that a nicer place to live. There is no end to it: a village keeps growing as long as it has
     * room around it.
     */
    public static String chooseProject(ServerLevel level, WorldMemory.VillageRecord record, int villagers, RandomSource random) {
        long beds = level.getPoiManager().getCountInRange(h -> h.is(PoiTypes.HOME), record.bell, 48 + record.houses * 2,
                PoiManager.Occupancy.ANY);
        if (beds <= villagers + 1) {
            return "house";
        }
        if (!record.well) {
            return "well";
        }
        if (!record.storehouse && record.houses >= 1) {
            return "storehouse";
        }
        if (!record.windmill && record.projects >= 2) {
            return "windmill";
        }
        if (!record.smithy && record.houses >= 4) {
            return "smithy";
        }
        if (!record.watchtower && (record.lastPirateRaid > 0 || record.projects >= 4)) {
            return "watchtower";
        }
        if (!record.chapel && record.houses >= 7) {
            return "chapel";
        }
        if (!record.benches && random.nextBoolean()) {
            return "benches";
        }
        int roll = random.nextInt(100);
        return roll < 20 ? "lamp" : roll < 35 ? "field" : roll < 50 ? "stall" : roll < 65 ? "flowers" : "house";
    }

    /** Picks a project the village needs and draws up its blueprint; returns the project, or null if there is no room today. */
    public static String grow(ServerLevel level, WorldMemory memory, WorldMemory.VillageRecord record, int villagers, RandomSource random) {
        String project = chooseProject(level, record, villagers, random);
        Blueprint blueprint = plan(level, memory, record, project, random);
        if (blueprint == null) {
            return null;
        }
        record.sites.add(blueprint);
        return project;
    }

    /**
     * Finds a spot for a project near the village and records its blueprint (nothing is built yet: the builders do
     * that). Returns null if no spot was found.
     */
    @Nullable
    public static Blueprint plan(ServerLevel level, WorldMemory memory, WorldMemory.VillageRecord record, String project,
                                 RandomSource random) {
        Builders.Palette palette = Builders.palette(level.getBiome(record.bell));
        for (int attempt = 0; attempt < 30; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            // the village spreads out as it grows
            double distance = (project.equals("windmill") || project.equals("watchtower") ? 20.0D : 10.0D)
                    + random.nextDouble() * 30.0D + record.houses * 1.5D + attempt * 0.5D;
            int x = record.bell.getX() + (int) (Math.cos(angle) * distance);
            int z = record.bell.getZ() + (int) (Math.sin(angle) * distance);
            boolean[] ok = {false};
            Blueprint blueprint = Builders.record(level, project, () -> ok[0] = draw(level, memory, record, project, palette, x, z, random));
            if (ok[0] && blueprint.size() > 0) {
                switch (project) {
                    case "house" -> record.houses++;
                    case "well" -> record.well = true;
                    case "windmill" -> record.windmill = true;
                    case "watchtower" -> record.watchtower = true;
                    case "benches" -> record.benches = true;
                    case "storehouse" -> record.storehouse = true;
                    case "smithy" -> record.smithy = true;
                    case "chapel" -> record.chapel = true;
                    default -> { }
                }
                return blueprint;
            }
        }
        return null;
    }

    /** Draws one project at (x, z); returns false if the spot does not suit it. */
    private static boolean draw(ServerLevel level, WorldMemory memory, WorldMemory.VillageRecord record, String project,
                                Builders.Palette palette, int x, int z, RandomSource random) {
        Direction towardsBell = Direction.fromYRot(Math.toDegrees(Math.atan2(record.bell.getZ() - z, record.bell.getX() - x)) - 90.0D);
        switch (project) {
            case "house" -> {
                int sx = towardsBell.getAxis() == Direction.Axis.Z ? 7 : 5;
                int sz = towardsBell.getAxis() == Direction.Axis.Z ? 5 : 7;
                int y = Builders.site(level, memory, x, z, sx, sz, 2, 7);
                if (y == Builders.FAIL) {
                    return false;
                }
                Block station = Builders.WORKSTATIONS[random.nextInt(Builders.WORKSTATIONS.length)];
                BlockPos door = Builders.house(level, new BlockPos(x, y, z), y, towardsBell, palette, station);
                Builders.path(level, memory, door, record.bell, 48);
                return true;
            }
            case "lamp" -> {
                return Builders.lampPost(level, memory, x, z, palette);
            }
            case "well" -> {
                int y = Builders.site(level, memory, x, z, 3, 3, 1, 5);
                if (y == Builders.FAIL) {
                    return false;
                }
                Builders.well(level, new BlockPos(x, y, z), y, palette);
                return true;
            }
            case "stall" -> {
                int y = Builders.site(level, memory, x, z, 3, 2, 1, 4);
                if (y == Builders.FAIL) {
                    return false;
                }
                Builders.stall(level, new BlockPos(x, y, z), y, palette, random);
                return true;
            }
            case "windmill" -> {
                int y = Builders.site(level, memory, x, z, 5, 5, 1, 12);
                if (y == Builders.FAIL) {
                    return false;
                }
                // the sails face away from the village, where the wind comes over the fields
                Builders.windmill(level, new BlockPos(x, y, z), y, palette, towardsBell.getOpposite());
                return true;
            }
            case "watchtower" -> {
                int y = Builders.site(level, memory, x, z, 3, 3, 1, 11);
                if (y == Builders.FAIL) {
                    return false;
                }
                Builders.watchtower(level, new BlockPos(x, y, z), y, palette);
                return true;
            }
            case "benches" -> {
                return Builders.benches(level, memory, record.bell, palette);
            }
            case "storehouse", "smithy", "chapel" -> {
                int w = 5;
                int d = project.equals("storehouse") ? 5 : project.equals("smithy") ? 7 : 9;
                int sx = towardsBell.getAxis() == Direction.Axis.Z ? w : d;
                int sz = towardsBell.getAxis() == Direction.Axis.Z ? d : w;
                int y = Builders.site(level, memory, x, z, sx, sz, 2, project.equals("chapel") ? 12 : 8);
                if (y == Builders.FAIL) {
                    return false;
                }
                BlockPos min = new BlockPos(x, y, z);
                switch (project) {
                    case "storehouse" -> Builders.storehouse(level, min, y, palette, towardsBell);
                    case "smithy" -> Builders.smithy(level, min, y, palette, towardsBell);
                    default -> Builders.chapel(level, min, y, palette, towardsBell);
                }
                BlockPos door = Builders.local(min, towardsBell, w, d, 2, 0, 0).relative(towardsBell);
                Builders.path(level, memory, door, record.bell, 48);
                return true;
            }
            case "flowers" -> {
                return Builders.flowerBed(level, memory, x, z, random);
            }
            default -> {
                int y = Builders.site(level, memory, x, z, 5, 5, 1, 3);
                if (y == Builders.FAIL) {
                    return false;
                }
                Builders.field(level, new BlockPos(x, y, z), y, 5, random);
                return true;
            }
        }
    }

    public static final String SPOUSE = "dr_spouse";

    /**
     * A child is born in a village: the chronicle notes it, and if its parents were not yet married, there is a
     * wedding first (the bell rings, and the village celebrates).
     */
    public static void family(ServerLevel level, Villager a, Villager b, Villager child) {
        ensureIdentity(a);
        ensureIdentity(b);
        ensureIdentity(child);
        WorldMemory memory = WorldMemory.get(level);
        WorldMemory.VillageRecord village = memory.nearestVillage(a.blockPosition(), 96.0D);
        long day = level.getDayTime() / 24000L;
        String place = village != null ? "#" + village.nameKey() : "?";
        CompoundTag da = a.getPersistentData();
        CompoundTag db = b.getPersistentData();
        boolean married = da.hasUUID(SPOUSE) && da.getUUID(SPOUSE).equals(b.getUUID());
        if (!married) {
            da.putUUID(SPOUSE, b.getUUID());
            db.putUUID(SPOUSE, a.getUUID());
            memory.record(day, "chronicle.dunesrelics.wedding", place, "#" + Names.villagerKey(da.getInt(NAME)),
                    "#" + Names.villagerKey(db.getInt(NAME)));
            if (village != null && level.getBlockState(village.bell).getBlock() instanceof BellBlock bell) {
                bell.attemptToRing(level, village.bell, null);
            }
            for (Villager guest : level.getEntitiesOfClass(Villager.class, a.getBoundingBox().inflate(16.0D))) {
                level.broadcastEntityEvent(guest, (byte) 14);
            }
        }
        memory.record(day, "chronicle.dunesrelics.birth", "#" + Names.villagerKey(child.getPersistentData().getInt(NAME)), place);
    }

    /** Trading makes a village prosper, and it grows faster. */
    public static void onTrade(WorldMemory memory, BlockPos pos) {
        WorldMemory.VillageRecord record = memory.nearestVillage(pos, 64.0D);
        if (record != null) {
            record.prosperity++;
            memory.setDirty();
        }
    }

    /** The UUIDs of villagers already handled this tick, so crowds near several players are not counted twice. */
    public static Set<UUID> newHandledSet() {
        return new HashSet<>();
    }
}
