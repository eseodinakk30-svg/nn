package com.dunesrelics.item;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

/**
 * Tempering, done at a Volcanic Forge: a tool, weapon or piece of armor quenched in lava with a Fire Opal.
 * Tempered weapons hit harder, tempered tools dig faster and tempered armor is tougher. An item can only be
 * tempered once.
 */
public final class Tempering {
    public static final String TAG = "dunesrelics_tempered";
    public static final double DAMAGE_BONUS = 1.5D;
    public static final float SPEED_BONUS = 1.2F;
    public static final double ARMOR_BONUS = 1.0D;
    public static final double TOUGHNESS_BONUS = 1.0D;

    private Tempering() {}

    public static boolean canTemper(ItemStack stack) {
        return !stack.isEmpty() && !isTempered(stack) && stack.getCount() == 1
                && (stack.getItem() instanceof DiggerItem || stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof ArmorItem || stack.getItem() instanceof TridentItem);
    }

    public static boolean isTempered(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(TAG);
    }

    public static void temper(ItemStack stack) {
        stack.getOrCreateTag().putBoolean(TAG, true);
    }
}
