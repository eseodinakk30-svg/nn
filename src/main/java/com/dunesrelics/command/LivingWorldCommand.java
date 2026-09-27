package com.dunesrelics.command;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.memory.Names;
import com.dunesrelics.memory.PirateRaids;
import com.dunesrelics.memory.Tides;
import com.dunesrelics.memory.VillageLife;
import com.dunesrelics.memory.WorldMemory;
import com.dunesrelics.memory.WorldReactions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * {@code /livingworld} (operators): look at what the world remembers around you, and hurry it along.
 * <ul>
 *     <li>{@code status}: the region's building, time spent, stage and trail; the tide;</li>
 *     <li>{@code react [mornings]}: run the next mornings' reactions right away;</li>
 *     <li>{@code stage <1..4>}: pretend you have lived here long enough for that stage;</li>
 *     <li>{@code grow}: let the villages near you build something now;</li>
 *     <li>{@code hamlet}: found a small hamlet (a cottage, a field, a bell and two villagers) where you stand;</li>
 *     <li>{@code pirates}: send a pirate landing party to the nearest coastal village.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = DunesRelics.MODID)
public final class LivingWorldCommand {
    private LivingWorldCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("livingworld").requires(source -> source.hasPermission(2))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("react").executes(context -> react(context.getSource(), 1))
                        .then(Commands.argument("mornings", IntegerArgumentType.integer(1, 30))
                                .executes(context -> react(context.getSource(), IntegerArgumentType.getInteger(context, "mornings")))))
                .then(Commands.literal("stage").then(Commands.argument("stage", IntegerArgumentType.integer(1, 4))
                        .executes(context -> stage(context.getSource(), IntegerArgumentType.getInteger(context, "stage")))))
                .then(Commands.literal("grow").executes(context -> grow(context.getSource())))
                .then(Commands.literal("hamlet").executes(context -> hamlet(context.getSource())))
                .then(Commands.literal("pirates").executes(context -> pirates(context.getSource()))));
    }

    private static int status(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        WorldMemory memory = WorldMemory.get(level);
        WorldMemory.Region region = memory.region(pos);
        source.sendSuccess(() -> Component.translatable("commands.dunesrelics.livingworld.status", region.placed,
                region.presence / 24000L, region.computeStage(), region.trailLength, region.farmBuilt,
                region.hamlet != null ? region.hamlet.toShortString() : "-"), false);
        source.sendSuccess(() -> Component.translatable("commands.dunesrelics.livingworld.tide",
                String.format("%.2f", Tides.tide(level))), false);
        return 1;
    }

    private static int react(CommandSourceStack source, int mornings) {
        ServerLevel level = source.getLevel();
        WorldMemory memory = WorldMemory.get(level);
        WorldMemory.Region region = memory.region(BlockPos.containing(source.getPosition()));
        long day = level.getDayTime() / 24000L;
        for (int i = 1; i <= mornings; i++) {
            region.lastReactionDay = -1L;
            WorldReactions.react(level, memory, region, day, level.random);
            for (WorldMemory.VillageRecord record : memory.villages()) {
                record.lastGrowthDay = -1L;
            }
            VillageLife.growVillages(level, memory, day, true);
        }
        source.sendSuccess(() -> Component.translatable("commands.dunesrelics.livingworld.reacted", mornings), true);
        return mornings;
    }

    private static int stage(CommandSourceStack source, int stage) {
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        WorldMemory memory = WorldMemory.get(level);
        WorldMemory.Region region = memory.region(pos);
        long[] placed = {150, 400, 1000, 2500};
        long[] days = {1, 3, 5, 8};
        while (region.placed < placed[stage - 1]) {
            region.addBlock(pos);
        }
        region.presence = Math.max(region.presence, days[stage - 1] * 24000L);
        memory.touch(pos);
        memory.setDirty();
        source.sendSuccess(() -> Component.translatable("commands.dunesrelics.livingworld.stage", stage), true);
        return stage;
    }

    private static int grow(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        WorldMemory memory = WorldMemory.get(level);
        for (WorldMemory.VillageRecord record : memory.villages()) {
            record.lastGrowthDay = -1L;
        }
        VillageLife.growVillages(level, memory, level.getDayTime() / 24000L, true);
        source.sendSuccess(() -> Component.translatable("commands.dunesrelics.livingworld.grew"), true);
        return 1;
    }

    private static int hamlet(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        WorldMemory memory = WorldMemory.get(level);
        long day = level.getDayTime() / 24000L;
        BlockPos bell = WorldReactions.buildHamlet(level, memory, BlockPos.containing(source.getPosition()), level.random);
        if (bell == null) {
            source.sendFailure(Component.translatable("commands.dunesrelics.livingworld.no_room"));
            return 0;
        }
        WorldMemory.VillageRecord record = memory.village(bell, day, Names.randomVillage(level.random));
        memory.record(day, "chronicle.dunesrelics.hamlet", "#" + record.nameKey());
        memory.setDirty();
        source.sendSuccess(() -> Component.translatable("commands.dunesrelics.livingworld.hamlet",
                Component.translatable(record.nameKey())), true);
        return 1;
    }

    private static int pirates(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        WorldMemory memory = WorldMemory.get(level);
        VillageLife.discoverVillages(level, memory, level.getDayTime() / 24000L);
        WorldMemory.VillageRecord record = memory.nearestVillage(BlockPos.containing(source.getPosition()), 160.0D);
        if (record == null) {
            source.sendFailure(Component.translatable("commands.dunesrelics.livingworld.no_village"));
            return 0;
        }
        if (record.sea == null) {
            record.sea = PirateRaids.findSea(level, record.bell, level.random);
            record.coast = record.sea != null ? 1 : 2;
        }
        if (!PirateRaids.land(level, memory, record, level.getDayTime() / 24000L, level.random)) {
            source.sendFailure(Component.translatable("commands.dunesrelics.livingworld.no_sea"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.dunesrelics.livingworld.pirates"), true);
        return 1;
    }
}
