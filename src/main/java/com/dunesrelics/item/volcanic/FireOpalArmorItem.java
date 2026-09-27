package com.dunesrelics.item.volcanic;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Wearing the full fire opal set grants permanent Fire Resistance. */
public class FireOpalArmorItem extends ArmorItem {
    public FireOpalArmorItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || this.type != Type.HELMET || level.getGameTime() % 40 != 0
                || !(entity instanceof LivingEntity living) || living.getItemBySlot(EquipmentSlot.HEAD) != stack) {
            return;
        }
        for (EquipmentSlot armorSlot : new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!(living.getItemBySlot(armorSlot).getItem() instanceof FireOpalArmorItem)) {
                return;
            }
        }
        living.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, true, false, true));
    }
}
