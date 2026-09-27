package com.dunesrelics.registry;

import net.minecraft.world.level.GameRules;

/** Game rules to switch the living-world systems off, one by one. */
public final class ModGameRules {
    public static final GameRules.Key<GameRules.BooleanValue> WORLD_MEMORY =
            GameRules.register("doWorldMemory", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> VILLAGE_GROWTH =
            GameRules.register("doVillageGrowth", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> PIRATE_RAIDS =
            GameRules.register("doPirateRaids", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> TIDES =
            GameRules.register("doTides", GameRules.Category.UPDATES, GameRules.BooleanValue.create(true));

    private ModGameRules() {}

    /** Forces the class to load so the rules are registered before any world is created. */
    public static void init() {}
}
