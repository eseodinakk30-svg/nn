package com.dunesrelics.entity;

import com.dunesrelics.registry.ModEntities;
import com.dunesrelics.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

/**
 * The Pharaoh: a boss sealed inside the sarcophagi of ancient tombs.
 * <ul>
 *     <li>Summons swarms of scarabs.</li>
 *     <li>Lays the Curse of the Sands on nearby players (Slowness and Mining Fatigue).</li>
 *     <li>At half health it becomes enraged: it calls two mummies to its side and moves faster.</li>
 * </ul>
 * Drops the Scepter of Sands and the Pharaoh armor trim.
 */
public class Pharaoh extends Mummy {
    private static final int SUMMON_INTERVAL = 180;
    private static final int CURSE_INTERVAL = 260;

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10).setDarkenScreen(true);
    private int summonCooldown = 80;
    private int curseCooldown = 140;
    private boolean enraged;

    public Pharaoh(EntityType<? extends Pharaoh> type, Level level) {
        super(type, level);
        this.xpReward = 100;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mummy.createAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                                  @Nullable SpawnGroupData groupData, @Nullable CompoundTag tag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData, tag);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setItemSlot(slot, ItemStack.EMPTY);
        }
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.SCEPTER_OF_SANDS.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.setCanPickUpLoot(false);
        return data;
    }

    @Override
    public void setBaby(boolean baby) {
        // The Pharaoh is never a child.
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        float speedUp = this.enraged ? 0.6F : 1.0F;

        if (--this.summonCooldown <= 0) {
            this.summonCooldown = (int) (SUMMON_INTERVAL * speedUp);
            this.summonScarabs(level, target, 2 + this.random.nextInt(2));
        }
        if (--this.curseCooldown <= 0) {
            this.curseCooldown = (int) (CURSE_INTERVAL * speedUp);
            this.castCurse(level);
        }
        if (!this.enraged && this.getHealth() < this.getMaxHealth() * 0.5F) {
            this.enrage(level, target);
        }
    }

    private void summonScarabs(ServerLevel level, LivingEntity target, int count) {
        for (int i = 0; i < count; i++) {
            Scarab scarab = ModEntities.SCARAB.get().create(level);
            if (scarab == null) {
                continue;
            }
            BlockPos pos = this.findSummonPos(level);
            scarab.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, this.random.nextFloat() * 360.0F, 0.0F);
            scarab.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null, null);
            scarab.setSummoned(600);
            scarab.setTarget(target);
            level.addFreshEntity(scarab);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                    scarab.getX(), scarab.getY() + 0.2D, scarab.getZ(), 15, 0.3D, 0.1D, 0.3D, 0.1D);
        }
        this.playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.5F, 0.7F);
    }

    private void castCurse(ServerLevel level) {
        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(12.0D))) {
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), this);
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 1), this);
        }
        for (int i = 0; i < 48; i++) {
            float angle = i / 48.0F * ((float) Math.PI * 2.0F);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                    this.getX() + Mth.cos(angle) * 4.0D, this.getY() + 0.5D, this.getZ() + Mth.sin(angle) * 4.0D,
                    2, 0.2D, 0.3D, 0.2D, 0.05D);
        }
        level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 2.0D, this.getZ(), 20, 0.6D, 0.6D, 0.6D, 0.02D);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 2.0F, 0.5F);
    }

    private void enrage(ServerLevel level, LivingEntity target) {
        this.enraged = true;
        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1000000, 0, false, false));
        for (int i = 0; i < 2; i++) {
            Mummy mummy = ModEntities.MUMMY.get().create(level);
            if (mummy == null) {
                continue;
            }
            BlockPos pos = this.findSummonPos(level);
            mummy.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, this.random.nextFloat() * 360.0F, 0.0F);
            mummy.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null, null);
            mummy.setBaby(false);
            mummy.setTarget(target);
            level.addFreshEntity(mummy);
            mummy.spawnAnim();
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 0.5F);
        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(32.0D))) {
            player.displayClientMessage(Component.translatable("entity.dunesrelics.pharaoh.enraged"), true);
        }
    }

    private BlockPos findSummonPos(ServerLevel level) {
        for (int attempt = 0; attempt < 8; attempt++) {
            BlockPos pos = this.blockPosition().offset(this.random.nextInt(7) - 3, 0, this.random.nextInt(7) - 3);
            if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                    && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                    && !level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) {
                return pos;
            }
        }
        return this.blockPosition();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0), this);
        }
        return hurt;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect() == MobEffects.POISON || effect.getEffect() == MobEffects.WEAKNESS
                || effect.getEffect() == MobEffects.MOVEMENT_SLOWDOWN) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Enraged", this.enraged);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.enraged = tag.getBoolean("Enraged");
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    public float getVoicePitch() {
        return 0.55F + this.random.nextFloat() * 0.1F;
    }
}
