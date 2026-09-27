package com.dunesrelics.registry;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.worldgen.structure.VolcanoPiece;
import com.dunesrelics.worldgen.structure.VolcanoStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, DunesRelics.MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, DunesRelics.MODID);

    public static final RegistryObject<StructureType<VolcanoStructure>> VOLCANO = STRUCTURE_TYPES.register("volcano",
            () -> () -> VolcanoStructure.CODEC);
    public static final RegistryObject<StructurePieceType> VOLCANO_PIECE = STRUCTURE_PIECES.register("volcano",
            () -> (StructurePieceType.ContextlessType) VolcanoPiece::new);

    private ModStructures() {}
}
