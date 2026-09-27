package com.dunesrelics.memory;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.entity.world.Shade;
import com.dunesrelics.registry.ModGameRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Wires the living-world systems to the game: what players do, and the passing of days. */
@Mod.EventBusSubscriber(modid = DunesRelics.MODID)
public final class MemoryEvents {
    private MemoryEvents() {}

    private static boolean isOverworld(Level level) {
        return level instanceof ServerLevel && level.dimension() == Level.OVERWORLD;
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level && isOverworld(level)) {
            WorldMemory memory = WorldMemory.get(level);
            memory.region(event.getPos()).addBlock(event.getPos());
            memory.touch(event.getPos());
            memory.setDirty();
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && isOverworld(level) && !event.getPlayer().isCreative()) {
            WorldMemory.get(level).touch(event.getPos());
        }
        if (event.getLevel() instanceof ServerLevel level) {
            VillageLife.onBlockBroken(level, event.getPlayer(), event.getPos(), event.getState());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level) || player.tickCount % 100 != 0 || player.isSpectator()) {
            return;
        }
        if (isOverworld(level)) {
            WorldMemory memory = WorldMemory.get(level);
            WorldMemory.Region region = memory.existingRegion(player.blockPosition());
            if (region != null && region.placed > 0) {
                region.presence += 100;
                memory.setDirty();
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || !isOverworld(level)) {
            return;
        }
        GameRules rules = level.getGameRules();
        long time = level.getGameTime();
        if (rules.getBoolean(ModGameRules.TIDES)) {
            Currents.tick(level);
        }
        if (time % 20 == 0) {
            Currents.sync(level, rules.getBoolean(ModGameRules.TIDES));
        }
        if (time % 10 == 0) {
            Set<UUID> handled = VillageLife.newHandledSet();
            for (ServerPlayer player : level.players()) {
                for (Villager villager : level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(64.0D))) {
                    if (!handled.add(villager.getUUID())) {
                        continue;
                    }
                    VillageLife.defend(level, villager);
                    if (time % 100 == 0) {
                        VillageLife.work(level, villager, level.random);
                    }
                }
            }
        }
        if (time % 20 != 0) {
            return;
        }
        WorldMemory memory = WorldMemory.get(level);
        long day = level.getDayTime() / 24000L;
        long timeOfDay = level.getDayTime() % 24000L;
        for (ServerPlayer player : level.players()) {
            VillageLife.greet(level, player, day);
        }
        if (time % 40 == 0 && rules.getBoolean(ModGameRules.TIDES)) {
            Tides.tickShore(level, memory, level.random);
        }
        if (time % 100 == 0 && rules.getBoolean(ModGameRules.VILLAGE_GROWTH)) {
            // the villages near players: surveyed, checked for damage, and given builders
            if (time % 600 == 0) {
                VillageLife.discoverVillages(level, memory, day);
            }
            Set<Long> looked = new HashSet<>();
            for (ServerPlayer player : level.players()) {
                for (WorldMemory.VillageRecord record : memory.villages()) {
                    if (record.bell.distSqr(player.blockPosition()) < 128 * 128 && looked.add(record.bell.asLong())) {
                        Construction.tickVillage(level, memory, record);
                    }
                }
            }
        }
        if (memory.lastDay < 0) {
            memory.lastDay = day;
            memory.setDirty();
        }
        if (day > memory.lastDay) {
            memory.lastDay = day;
            memory.setDirty();
            newDay(level, memory, day, false);
        }
        if (timeOfDay >= 12500L && memory.lastDusk != day) {
            memory.lastDusk = day;
            memory.setDirty();
            if (rules.getBoolean(ModGameRules.VILLAGE_GROWTH)) {
                VillageLife.curfew(level, memory);
            }
        }
        if (timeOfDay >= 14000L && memory.lastNight != day) {
            memory.lastNight = day;
            memory.setDirty();
            if (rules.getBoolean(ModGameRules.PIRATE_RAIDS)) {
                PirateRaids.checkNight(level, memory, day, level.random);
            }
        }
    }

    /** The morning's changes: the world answers its players, and the villages grow. */
    public static void newDay(ServerLevel level, WorldMemory memory, long day, boolean force) {
        GameRules rules = level.getGameRules();
        if (rules.getBoolean(ModGameRules.WORLD_MEMORY)) {
            for (WorldMemory.Region region : memory.regions()) {
                if (region.placed > 0 && region.computeStage() >= 1 && WorldReactions.isAttended(level, region.center())) {
                    WorldReactions.react(level, memory, region, day, level.random);
                }
            }
        }
        if (rules.getBoolean(ModGameRules.VILLAGE_GROWTH)) {
            VillageLife.growVillages(level, memory, day, force);
        }
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        Currents.clear();
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof Villager villager) {
            VillageLife.ensureIdentity(villager);
        } else if (entity instanceof Phantom && !event.loadedFromDisk() && Shade.nearDreamcatcher(level, entity.blockPosition())) {
            // Dreamcatchers keep the phantoms away.
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof Villager villager && event.getEntity().isShiftKeyDown()
                && event.getLevel() instanceof ServerLevel level) {
            ItemStack stack = event.getItemStack();
            if (VillageLife.receiveGift(level, villager, event.getEntity(), stack, level.getDayTime() / 24000L)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player player && event.getEntity() instanceof Enemy
                && event.getEntity().level() instanceof ServerLevel level) {
            VillageLife.onMonsterKilled(level, player, event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent event) {
        if (event.getEntity().level() instanceof ServerLevel level && isOverworld(level)) {
            VillageLife.onTrade(WorldMemory.get(level), event.getAbstractVillager().blockPosition());
        }
    }

    @SubscribeEvent
    public static void onFished(ItemFishedEvent event) {
        if (!event.getDrops().isEmpty() && event.getEntity().level() instanceof ServerLevel) {
            Tides.onFished(event.getEntity(), event.getHookEntity().position(), event.getDrops().get(0));
        }
    }

    /** Area check used by the dreamcatcher tooltip and tests. */
    public static AABB around(Entity entity, double radius) {
        return entity.getBoundingBox().inflate(radius);
    }
}
