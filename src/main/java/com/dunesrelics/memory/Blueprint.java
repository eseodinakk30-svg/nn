package com.dunesrelics.memory;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A list of blocks to place, in the order a builder lays them: first clearing the site, then course by course from
 * the ground up, with water poured in after the blocks around it. Also used as a village's record of its own
 * buildings, which the builders compare with the world to find what needs repairing.
 */
public class Blueprint {
    public final String project;
    private final List<BlockState> palette = new ArrayList<>();
    private final Object2IntOpenHashMap<BlockState> paletteIndex = new Object2IntOpenHashMap<>();
    private final LongArrayList positions = new LongArrayList();
    private final IntArrayList states = new IntArrayList();
    /** While recording: the latest state for each position, in the order the positions were first written. */
    @Nullable
    private LinkedHashMap<Long, BlockState> draft = new LinkedHashMap<>();
    @Nullable
    private Long2IntOpenHashMap index;
    /** The next block to lay. */
    public int next;
    /** Game time when a block of it was last laid (or when it was planned). */
    public long lastProgress;
    /** The middle of the building at ground level: where the builders gather and what the chronicle refers to. */
    @Nullable
    public BlockPos anchor;

    public Blueprint(String project) {
        this.project = project;
        this.paletteIndex.defaultReturnValue(-1);
    }

    void record(BlockPos pos, BlockState state) {
        if (this.draft == null) {
            throw new IllegalStateException("blueprint already finished");
        }
        this.draft.put(pos.asLong(), state);
    }

    /** Puts the recorded blocks in building order; clearing is dropped where the site is already empty. */
    Blueprint finish(ServerLevel level) {
        List<Map.Entry<Long, BlockState>> entries = new ArrayList<>(this.draft != null ? this.draft.entrySet() : List.of());
        this.draft = null;
        entries.removeIf(e -> e.getValue().isAir() && level.getBlockState(BlockPos.of(e.getKey())).isAir());
        // List.sort is stable, so within a course the blocks keep the order they were drawn in.
        entries.sort(Comparator.<Map.Entry<Long, BlockState>>comparingInt(e -> e.getValue().isAir() ? 0 : 1)
                .thenComparingInt(e -> BlockPos.getY(e.getKey()))
                .thenComparingInt(e -> e.getValue().getFluidState().isEmpty() ? 0 : 1));
        long sumX = 0;
        long sumZ = 0;
        int minY = Integer.MAX_VALUE;
        int solid = 0;
        for (Map.Entry<Long, BlockState> entry : entries) {
            BlockPos pos = BlockPos.of(entry.getKey());
            this.add(pos, entry.getValue());
            if (!entry.getValue().isAir()) {
                sumX += pos.getX();
                sumZ += pos.getZ();
                minY = Math.min(minY, pos.getY());
                solid++;
            }
        }
        if (solid > 0) {
            this.anchor = new BlockPos((int) (sumX / solid), minY, (int) (sumZ / solid));
        }
        this.lastProgress = level.getGameTime();
        return this;
    }

    public void add(BlockPos pos, BlockState state) {
        this.positions.add(pos.asLong());
        this.states.add(this.paletteId(state));
        if (this.index != null) {
            this.index.put(pos.asLong(), this.positions.size() - 1);
        }
    }

    /** Replaces the state of one entry; air means the entry is no longer kept. */
    public void set(int i, BlockState state) {
        this.states.set(i, this.paletteId(state));
    }

    private int paletteId(BlockState state) {
        int id = this.paletteIndex.getInt(state);
        if (id < 0) {
            id = this.palette.size();
            this.palette.add(state);
            this.paletteIndex.put(state, id);
        }
        return id;
    }

    public int size() {
        return this.positions.size();
    }

    public boolean isDone() {
        return this.next >= this.size();
    }

    public BlockPos pos(int i) {
        return BlockPos.of(this.positions.getLong(i));
    }

    public BlockState state(int i) {
        return this.palette.get(this.states.getInt(i));
    }

    /** The entry for a position, or -1. */
    public int indexOf(BlockPos pos) {
        if (this.index == null) {
            this.index = new Long2IntOpenHashMap(this.positions.size());
            this.index.defaultReturnValue(-1);
            for (int i = 0; i < this.positions.size(); i++) {
                this.index.put(this.positions.getLong(i), i);
            }
        }
        return this.index.get(pos.asLong());
    }

    /** How many blocks are left to lay (not counting clearing). */
    public int remaining() {
        int left = 0;
        for (int i = this.next; i < this.size(); i++) {
            if (!this.state(i).isAir()) {
                left++;
            }
        }
        return left;
    }

    // ------------------------------------------------------------------------------------------ saving

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Project", this.project);
        tag.putInt("Next", this.next);
        tag.putLong("LastProgress", this.lastProgress);
        if (this.anchor != null) {
            tag.put("Anchor", NbtUtils.writeBlockPos(this.anchor));
        }
        ListTag paletteTag = new ListTag();
        for (BlockState state : this.palette) {
            paletteTag.add(NbtUtils.writeBlockState(state));
        }
        tag.put("Palette", paletteTag);
        tag.put("Positions", new LongArrayTag(this.positions.toLongArray()));
        tag.put("States", new IntArrayTag(this.states.toIntArray()));
        return tag;
    }

    public static Blueprint load(CompoundTag tag) {
        Blueprint blueprint = new Blueprint(tag.getString("Project"));
        blueprint.draft = null;
        blueprint.next = tag.getInt("Next");
        blueprint.lastProgress = tag.getLong("LastProgress");
        blueprint.anchor = tag.contains("Anchor") ? NbtUtils.readBlockPos(tag.getCompound("Anchor")) : null;
        List<BlockState> loaded = new ArrayList<>();
        for (Tag t : tag.getList("Palette", Tag.TAG_COMPOUND)) {
            loaded.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), (CompoundTag) t));
        }
        long[] positions = tag.getLongArray("Positions");
        int[] states = tag.getIntArray("States");
        for (int i = 0; i < Math.min(positions.length, states.length); i++) {
            BlockState state = states[i] >= 0 && states[i] < loaded.size() ? loaded.get(states[i]) : Blocks.AIR.defaultBlockState();
            blueprint.add(BlockPos.of(positions[i]), state);
        }
        return blueprint;
    }
}
