package com.dunesrelics.item.volcanic;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The Magma Titan's hammer. Use it to slam the ground: nearby enemies are thrown back and set ablaze. */
public class MagmaHammerItem extends SwordItem {
    private static final double RADIUS = 4.5D;

    public MagmaHammerItem(Tier tier, int damage, float speed, Properties properties) {
        super(tier, damage, speed, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            slam(serverLevel, player, player.position(), RADIUS, 8.0F);
            stack.hurtAndBreak(2, player, p -> p.broadcastBreakEvent(hand));
        }
        player.getCooldowns().addCooldown(this, 60);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** A shockwave around {@code center}. Shared with the Magma Titan's own ground slam. */
    public static void slam(ServerLevel level, LivingEntity source, Vec3 center, double radius, float damage) {
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, source.getBoundingBox().inflate(radius),
                e -> e != source && e.isAlive() && !e.isAlliedTo(source) && !(e instanceof Player p && (p.isCreative() || p.isSpectator())))) {
            double distance = target.position().distanceTo(center);
            if (distance > radius) {
                continue;
            }
            Vec3 away = target.position().subtract(center).multiply(1.0D, 0.0D, 1.0D);
            away = away.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 0.0D) : away.normalize();
            target.hurt(source instanceof Player player ? level.damageSources().playerAttack(player)
                    : level.damageSources().mobAttack(source), damage);
            target.setDeltaMovement(target.getDeltaMovement().add(away.scale(1.2D)).add(0.0D, 0.55D, 0.0D));
            target.hurtMarked = true;
            target.setSecondsOnFire(4);
        }
        for (int i = 0; i < 40; i++) {
            double angle = i / 40.0D * Math.PI * 2.0D;
            level.sendParticles(ParticleTypes.LAVA, center.x + Math.cos(angle) * radius * 0.7D, center.y + 0.2D,
                    center.z + Math.sin(angle) * radius * 0.7D, 1, 0.1D, 0.0D, 0.1D, 0.0D);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x + Math.cos(angle) * radius * 0.5D, center.y + 0.3D,
                    center.z + Math.sin(angle) * radius * 0.5D, 1, 0.1D, 0.1D, 0.1D, 0.02D);
        }
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 0.6F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8F, 0.5F);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.setSecondsOnFire(4);
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.dunesrelics.magma_hammer.desc").withStyle(ChatFormatting.GOLD));
    }
}
