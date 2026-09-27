package com.dunesrelics.item;

import com.dunesrelics.registry.ModParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Pharaoh's scepter.
 * <ul>
 *     <li>Use: unleashes a cone of scouring sand that damages, blinds and hurls back everything in front of you.</li>
 *     <li>Sneak + use: summons a sandstorm (or calms the one that is raging).</li>
 * </ul>
 */
public class ScepterOfSandsItem extends Item {
    private static final double RANGE = 7.0D;
    private static final float DAMAGE = 6.0F;

    public ScepterOfSandsItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (level instanceof ServerLevel serverLevel) {
                if (serverLevel.isRaining()) {
                    serverLevel.setWeatherParameters(12000, 0, false, false);
                    player.displayClientMessage(Component.translatable("item.dunesrelics.scepter_of_sands.calm"), true);
                } else {
                    serverLevel.setWeatherParameters(0, 6000, true, false);
                    player.displayClientMessage(Component.translatable("item.dunesrelics.scepter_of_sands.storm"), true);
                }
                stack.hurtAndBreak(10, player, p -> p.broadcastBreakEvent(hand));
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.6F);
            player.getCooldowns().addCooldown(this, 600);
        } else {
            if (level instanceof ServerLevel serverLevel) {
                blast(serverLevel, player);
                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 1.5F, 0.6F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.6F, 1.4F);
            player.getCooldowns().addCooldown(this, 40);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static void blast(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RANGE),
                e -> e != player && e.isAlive() && !e.isAlliedTo(player) && !(e instanceof Player p && p.isSpectator()));
        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).subtract(eye);
            double distance = toTarget.length();
            if (distance > RANGE || toTarget.normalize().dot(look) < 0.5D) {
                continue;
            }
            target.hurt(level.damageSources().playerAttack(player), DAMAGE);
            target.knockback(1.4D, -look.x, -look.z);
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60), player);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), player);
        }
        BlockParticleOption sand = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState());
        for (double d = 1.0D; d <= RANGE; d += 0.5D) {
            Vec3 point = eye.add(look.scale(d));
            double spread = d * 0.18D;
            level.sendParticles(sand, point.x, point.y - 0.3D, point.z, 5, spread, spread, spread, 0.05D);
            level.sendParticles(ModParticles.SAND_GUST.get(), point.x, point.y - 0.3D, point.z, 3, spread, spread, spread, 0.02D);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.dunesrelics.scepter_of_sands.desc").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.dunesrelics.scepter_of_sands.desc2").withStyle(ChatFormatting.GRAY));
    }
}
