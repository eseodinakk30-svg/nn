package com.dunesrelics.worldgen.structure;

import com.dunesrelics.registry.ModStructures;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A volcano: a cone of scoria, basalt and ash rising out of the sea (or the ash fields), with a lava-filled crater,
 * rivers of cooled lava down its flanks and a magma chamber inside where the Magma Titan sleeps.
 */
public class VolcanoStructure extends Structure {
    public static final Codec<VolcanoStructure> CODEC = simpleCodec(VolcanoStructure::new);

    public VolcanoStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        int floor = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                context.heightAccessor(), context.randomState());
        int seaLevel = context.chunkGenerator().getSeaLevel();
        BlockPos center = new BlockPos(x, floor, z);
        return Optional.of(new GenerationStub(center, builder ->
                builder.addPiece(new VolcanoPiece(context.random().nextInt(), center, seaLevel))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.VOLCANO.get();
    }
}
