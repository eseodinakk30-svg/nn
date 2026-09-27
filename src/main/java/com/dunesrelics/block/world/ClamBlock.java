package com.dunesrelics.block.world;

import net.minecraft.world.level.block.Block;

/** A clam washed up on the beach. Break it open for a chance at a pearl (Fortune helps). */
public class ClamBlock extends ShoreBlock {
    public ClamBlock(Properties properties) {
        super(properties, Block.box(3, 0, 3, 13, 4, 13));
        this.registerDefaultState(this.stateDefinition.any().setValue(NATURAL, false));
    }
}
