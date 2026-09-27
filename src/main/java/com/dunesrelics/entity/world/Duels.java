package com.dunesrelics.entity.world;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;

/**
 * A pirate captain's duel, as the crew sees it: they stand back while their captain fights one on one, rush in if the
 * challenger cheats, and throw down their arms (and in the end jump overboard) if their captain falls fairly.
 */
final class Duels {
    /** How long surrendered pirates stand about before they swim for it. */
    static final int SURRENDER_TICKS = 1200;

    private Duels() {}

    /** Whether the crew member may not attack this target: he has surrendered, or it is the captain's duellist. */
    static boolean holdsBack(PirateCrew crew, LivingEntity target) {
        return crew.hasSurrendered() || crew.getDuelist() != null && target.getUUID().equals(crew.getDuelist());
    }

    /** Every tick for a crew member. Returns true once he has swum for it (the caller should stop). */
    static boolean tick(Mob mob, PirateCrew crew, int surrenderedFor) {
        if (crew.hasSurrendered()) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            if (mob instanceof Raider raider) {
                raider.setCelebrating(true);
            }
            if (surrenderedFor > SURRENDER_TICKS && mob.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SPLASH, mob.getX(), mob.getY() + 0.5D, mob.getZ(), 30, 0.4D, 0.4D, 0.4D, 0.1D);
                mob.playSound(SoundEvents.PLAYER_SPLASH, 1.0F, 1.0F);
                mob.discard();
                return true;
            }
            return false;
        }
        LivingEntity target = mob.getTarget();
        if (target != null && holdsBack(crew, target)) {
            mob.setTarget(null);
        }
        return false;
    }

    /** The duellist struck one of the crew: that was no fair fight, and the captain calls them all in. */
    static void onHurt(Mob mob, PirateCrew crew, DamageSource source) {
        if (crew.getDuelist() != null && source.getEntity() instanceof LivingEntity attacker
                && attacker.getUUID().equals(crew.getDuelist())) {
            for (PirateCaptain captain : mob.level().getEntitiesOfClass(PirateCaptain.class, mob.getBoundingBox().inflate(48.0D))) {
                captain.breakDuel(attacker);
            }
        }
    }

    /** Throws down his weapon and raises his hands. */
    static void surrender(Mob mob) {
        ItemStack weapon = mob.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!weapon.isEmpty()) {
            mob.spawnAtLocation(weapon.copy());
            mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        mob.setTarget(null);
        mob.getNavigation().stop();
    }
}
