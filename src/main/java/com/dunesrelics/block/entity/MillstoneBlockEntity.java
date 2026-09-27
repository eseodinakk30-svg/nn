package com.dunesrelics.block.entity;

import com.dunesrelics.block.world.MillstoneBlock;
import com.dunesrelics.block.world.WaterWheelBlock;
import com.dunesrelics.recipe.MillingRecipe;
import com.dunesrelics.registry.ModBlockEntities;
import com.dunesrelics.registry.ModRecipes;
import com.dunesrelics.registry.WorldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Holds the grain waiting to be milled (slot 0) and the milled product (slot 1). */
public class MillstoneBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final int INPUT = 0;
    public static final int OUTPUT = 1;
    private static final int[] TOP_SLOTS = {INPUT};
    private static final int[] BOTTOM_SLOTS = {OUTPUT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private int progress;

    public MillstoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MILLSTONE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MillstoneBlockEntity mill) {
        boolean working = false;
        Optional<MillingRecipe> recipe = mill.findRecipe(mill.items.get(INPUT));
        if (recipe.isPresent() && isDriven(level, pos)) {
            ItemStack result = recipe.get().getResultItem(level.registryAccess());
            ItemStack output = mill.items.get(OUTPUT);
            boolean fits = output.isEmpty() || ItemStack.isSameItemSameTags(output, result)
                    && output.getCount() + result.getCount() <= output.getMaxStackSize();
            if (fits) {
                working = true;
                if (++mill.progress >= recipe.get().getTime()) {
                    mill.progress = 0;
                    mill.items.get(INPUT).shrink(1);
                    if (output.isEmpty()) {
                        mill.items.set(OUTPUT, result.copy());
                    } else {
                        output.grow(result.getCount());
                    }
                    mill.setChanged();
                }
            }
        }
        if (!working && mill.progress != 0) {
            mill.progress = 0;
        }
        if (state.getValue(MillstoneBlock.ACTIVE) != working) {
            level.setBlock(pos, state.setValue(MillstoneBlock.ACTIVE, working), 3);
        }
    }

    /** A spinning water wheel beside the stone, with its axle pointing at it, turns the runner stone. */
    public static boolean isDriven(Level level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (neighbor.is(WorldBlocks.WATER_WHEEL.get()) && neighbor.getValue(WaterWheelBlock.SPINNING)
                    && neighbor.getValue(WaterWheelBlock.AXIS) == direction.getAxis()) {
                return true;
            }
        }
        return false;
    }

    private Optional<MillingRecipe> findRecipe(ItemStack stack) {
        if (stack.isEmpty() || this.level == null) {
            return Optional.empty();
        }
        return this.level.getRecipeManager().getRecipeFor(ModRecipes.MILLING.get(), new SimpleContainer(stack), this.level);
    }

    public boolean canInsert(ItemStack stack) {
        ItemStack input = this.items.get(INPUT);
        return this.findRecipe(stack).isPresent()
                && (input.isEmpty() || ItemStack.isSameItemSameTags(input, stack) && input.getCount() < input.getMaxStackSize());
    }

    /** Puts as much of the stack as fits into the input slot and returns what is left. */
    public ItemStack insert(ItemStack stack) {
        ItemStack input = this.items.get(INPUT);
        if (input.isEmpty()) {
            this.items.set(INPUT, stack);
            this.setChanged();
            return ItemStack.EMPTY;
        }
        int moved = Math.min(stack.getCount(), input.getMaxStackSize() - input.getCount());
        input.grow(moved);
        stack.shrink(moved);
        this.setChanged();
        return stack;
    }

    /** Takes the milled product, or the unmilled grain when there is no product yet. */
    public ItemStack takeAll() {
        int slot = this.items.get(OUTPUT).isEmpty() ? INPUT : OUTPUT;
        ItemStack taken = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        if (!taken.isEmpty()) {
            this.setChanged();
        }
        return taken;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, this.items);
        tag.putInt("Progress", this.progress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items);
        this.progress = tag.getInt("Progress");
    }

    // ------------------------------------------------------------------------------------------ container

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? BOTTOM_SLOTS : TOP_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == INPUT && side != Direction.DOWN && this.findRecipe(stack).isPresent();
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT && side == Direction.DOWN;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == INPUT && this.findRecipe(stack).isPresent();
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) {
            this.setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.level != null && this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D,
                this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }
}
