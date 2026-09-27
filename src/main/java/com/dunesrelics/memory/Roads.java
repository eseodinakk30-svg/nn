package com.dunesrelics.memory;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * Roads between villages: a path over the natural ground from bell to bell, lamp posts along it, and at each end a
 * milestone naming the village at the other end and how far it is. A road is laid once the whole way is loaded.
 */
public final class Roads {
    private Roads() {}

    /** The direction a traveller faces walking along (dx, dz), in Minecraft degrees. */
    private static float yaw(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    public static boolean build(ServerLevel level, WorldMemory memory, WorldMemory.VillageRecord from,
                                WorldMemory.VillageRecord to, long day) {
        if (from.roads.contains(to.bell.asLong())) {
            return false;
        }
        BlockPos a = from.bell;
        BlockPos b = to.bell;
        double dx = b.getX() - a.getX();
        double dz = b.getZ() - a.getZ();
        int length = (int) Math.sqrt(dx * dx + dz * dz);
        if (length < 16) {
            return false;
        }
        for (int i = 0; i <= length; i += 16) {
            BlockPos column = new BlockPos((int) (a.getX() + dx * i / length), a.getY(), (int) (a.getZ() + dz * i / length));
            if (!level.isLoaded(column)) {
                return false;
            }
        }
        Builders.path(level, memory, a, b, length + 1);
        Builders.Palette palette = Builders.palette(level.getBiome(a));
        double nx = -dz / length;
        double nz = dx / length;
        for (int i = 20; i < length - 16; i += 24) {
            int x = (int) Math.round(a.getX() + dx * i / length + nx * 2.0D);
            int z = (int) Math.round(a.getZ() + dz * i / length + nz * 2.0D);
            Builders.lampPost(level, memory, x, z, palette);
        }
        // a traveller leaving A reads the stone facing him: its face turned back towards A
        float towardsB = yaw(dx, dz);
        Builders.milestone(level, memory, (int) Math.round(a.getX() + dx * 8 / length - nx * 2.0D),
                (int) Math.round(a.getZ() + dz * 8 / length - nz * 2.0D), towardsB + 180.0F,
                Component.translatable(to.nameKey()), length);
        Builders.milestone(level, memory, (int) Math.round(b.getX() - dx * 8 / length + nx * 2.0D),
                (int) Math.round(b.getZ() - dz * 8 / length + nz * 2.0D), towardsB,
                Component.translatable(from.nameKey()), length);
        from.roads.add(b.asLong());
        to.roads.add(a.asLong());
        memory.record(day, "chronicle.dunesrelics.road", "#" + from.nameKey(), "#" + to.nameKey());
        memory.setDirty();
        return true;
    }
}
