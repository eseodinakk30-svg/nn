package com.dunesrelics.block;

import com.dunesrelics.registry.ModDamageTypes;
import com.dunesrelics.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Quicksand looks like ordinary sand, but anything that steps on it slowly sinks.
 * Holding jump climbs back out, sneaking stops the sinking, and staying under for too long suffocates.
 * Desert creatures (see the {@code dunesrelics:quicksand_walkers} tag) walk over it as if it were solid.
 */
public class QuicksandBlock extends Block {
    private static final Vec3 STUCK_MULTIPLIER = new Vec3(0.35D, 0.25D, 0.35D);

    public QuicksandBlock(Properties properties) {
        super(properties);
    }

    public static boolean canWalkOn(Entity entity) {
        return entity.getType().is(ModTags.QUICKSAND_WALKERS);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext) {
            Entity entity = entityContext.getEntity();
            if (entity instanceof FallingBlockEntity
                    || entity != null && canWalkOn(entity) && context.isAbove(Shapes.block(), pos, false)) {
                return Shapes.block();
            }
        }
        return Shapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (canWalkOn(entity)) {
            return;
        }
        if (!(entity instanceof LivingEntity) || entity.getFeetBlockState().is(this)) {
            entity.makeStuckInBlock(state, STUCK_MULTIPLIER);
        }
        if (entity instanceof LivingEntity living && !level.isClientSide && living.tickCount % 10 == 0) {
            BlockPos eyes = BlockPos.containing(living.getEyePosition());
            if (level.getBlockState(eyes).is(this)) {
                living.hurt(ModDamageTypes.quicksand(level), 1.0F);
            }
        }
        if (level.isClientSide && entity.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D && level.random.nextInt(3) == 0) {
            RandomSource random = level.random;
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    entity.getX() + (random.nextDouble() - 0.5D) * entity.getBbWidth(), pos.getY() + 1.0D,
                    entity.getZ() + (random.nextDouble() - 0.5D) * entity.getBbWidth(), 0.0D, 0.0D, 0.0D);
        }
    }

    /** Lets trapped entities climb out while holding jump, and hold still while sneaking. */
    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return !canWalkOn(entity);
    }

    @Override
    public @Nullable BlockPathTypes getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
        return mob != null && canWalkOn(mob) ? BlockPathTypes.WALKABLE : BlockPathTypes.POWDER_SNOW;
    }
}
