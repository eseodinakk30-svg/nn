package com.dunesrelics.item.volcanic;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/** A fire opal blade sets whatever it strikes ablaze. */
public class FireOpalSwordItem extends SwordItem {
    public FireOpalSwordItem(Tier tier, int damage, float speed, Properties properties) {
        super(tier, damage, speed, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.setSecondsOnFire(3);
        return super.hurtEnemy(stack, target, attacker);
    }
}
