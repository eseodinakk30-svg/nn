package com.dunesrelics.worldgen.structure;

import com.dunesrelics.registry.ModStructures;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** A pirate ship at anchor in open water deep enough to float it. */
public class PirateShipStructure extends Structure {
    public static final Codec<PirateShipStructure> CODEC = simpleCodec(PirateShipStructure::new);

    public PirateShipStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        int seaLevel = context.chunkGenerator().getSeaLevel();
        // Deep enough under the keel all along the ship.
        for (int[] offset : new int[][]{{0, 0}, {14, 0}, {-14, 0}, {0, 14}, {0, -14}}) {
            int floor = context.chunkGenerator().getFirstOccupiedHeight(x + offset[0], z + offset[1], Heightmap.Types.OCEAN_FLOOR_WG,
                    context.heightAccessor(), context.randomState());
            if (floor > seaLevel - 7) {
                return Optional.empty();
            }
        }
        Direction bow = Direction.from2DDataValue(context.random().nextInt(4));
        BlockPos center = new BlockPos(x, seaLevel, z);
        return Optional.of(new GenerationStub(center, builder -> builder.addPiece(new PirateShipPiece(center, bow, seaLevel))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.PIRATE_SHIP.get();
    }
}
