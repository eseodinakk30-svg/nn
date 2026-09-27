package com.dunesrelics.block.entity;

import com.dunesrelics.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Only exists so the wheel can be drawn turning; all the logic lives in the block state. */
public class WaterWheelBlockEntity extends BlockEntity {
    /** Client-side animation state. */
    public float angle;
    public float speed;
    public double lastRenderTime = -1.0D;

    public WaterWheelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_WHEEL.get(), pos, state);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(this.worldPosition).inflate(1.5D);
    }
}
