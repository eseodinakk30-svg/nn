package com.dunesrelics.memory;

import net.minecraft.util.RandomSource;

/** Names are translation keys, so every language can say them its own way. */
public final class Names {
    public static final int VILLAGER_NAMES = 48;
    public static final int VILLAGE_NAMES = 32;
    public static final int TRAITS = 4;

    private Names() {}

    public static String villagerKey(int index) {
        return "name.dunesrelics.villager." + Math.floorMod(index, VILLAGER_NAMES);
    }

    public static String villageKey(int index) {
        return "name.dunesrelics.village." + Math.floorMod(index, VILLAGE_NAMES);
    }

    public static int randomVillager(RandomSource random) {
        return random.nextInt(VILLAGER_NAMES);
    }

    public static int randomVillage(RandomSource random) {
        return random.nextInt(VILLAGE_NAMES);
    }
}
