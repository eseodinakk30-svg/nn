package com.dunesrelics.block.entity;

import com.dunesrelics.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Only exists so the sails can be drawn turning; the wind itself is in the block state. */
public class WindmillSailsBlockEntity extends BlockEntity {
    /** Client-side animation state. */
    public float angle;
    public float speed;
    public double lastRenderTime = -1.0D;

    public WindmillSailsBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WINDMILL_SAILS.get(), pos, state);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(this.worldPosition).inflate(4.0D);
    }
}
