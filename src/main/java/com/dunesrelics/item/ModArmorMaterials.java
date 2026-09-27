package com.dunesrelics.item;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.registry.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

public enum ModArmorMaterials implements ArmorMaterial {
    BRONZE("bronze", 18, new int[]{2, 5, 6, 2}, 16, SoundEvents.ARMOR_EQUIP_IRON, 0.5F, 0.0F,
            () -> Ingredient.of(ModItems.BRONZE_INGOT.get())),
    /** Amber goggles barely protect, but let you see through sandstorms. */
    AMBER_GOGGLES("amber_goggles", 10, new int[]{1, 1, 1, 1}, 12, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F,
            () -> Ingredient.of(ModItems.AMBER.get()));

    /** Durability per slot, indexed boots, leggings, chestplate, helmet (same as vanilla). */
    private static final int[] HEALTH_PER_SLOT = new int[]{13, 15, 16, 11};

    private final String name;
    private final int durabilityMultiplier;
    /** Protection indexed boots, leggings, chestplate, helmet. */
    private final int[] protection;
    private final int enchantmentValue;
    private final SoundEvent sound;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repairIngredient;

    ModArmorMaterials(String name, int durabilityMultiplier, int[] protection, int enchantmentValue, SoundEvent sound,
                      float toughness, float knockbackResistance, Supplier<Ingredient> repairIngredient) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.protection = protection;
        this.enchantmentValue = enchantmentValue;
        this.sound = sound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repairIngredient = repairIngredient;
    }

    private static int slotIndex(ArmorItem.Type type) {
        return switch (type) {
            case BOOTS -> 0;
            case LEGGINGS -> 1;
            case CHESTPLATE -> 2;
            case HELMET -> 3;
        };
    }

    @Override public int getDurabilityForType(ArmorItem.Type type) { return HEALTH_PER_SLOT[slotIndex(type)] * this.durabilityMultiplier; }
    @Override public int getDefenseForType(ArmorItem.Type type) { return this.protection[slotIndex(type)]; }
    @Override public int getEnchantmentValue() { return this.enchantmentValue; }
    @Override public SoundEvent getEquipSound() { return this.sound; }
    @Override public Ingredient getRepairIngredient() { return this.repairIngredient.get(); }
    @Override public String getName() { return DunesRelics.MODID + ":" + this.name; }
    @Override public float getToughness() { return this.toughness; }
    @Override public float getKnockbackResistance() { return this.knockbackResistance; }
}
