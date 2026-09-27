package com.dunesrelics.entity.world;

import com.dunesrelics.memory.WorldMemory;
import com.dunesrelics.registry.ModEntities;
import com.dunesrelics.registry.WorldItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The captain of a pirate ship. Tougher than any pirate, he whistles for his crew and lobs cannonballs at anyone who
 * keeps their distance. Drops his tricorn hat.
 */
public class PirateCaptain extends Vindicator implements PirateCrew {
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10).setDarkenScreen(false);
    @Nullable
    private BlockPos raidTarget;
    private int whistleCooldown = 100;
    private int throwCooldown = 60;
    /** The player he has called out to fight one on one (only once in his life), and whether he has done so. */
    @Nullable
    private UUID duelist;
    private boolean dueled;

    public PirateCaptain(EntityType<? extends Vindicator> type, Level level) {
        super(type, level);
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Vindicator.createAttributes()
                .add(Attributes.MAX_HEALTH, 90.0D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D)
                .add(Attributes.MOVEMENT_SPEED, 0.33D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(4, new MarchGoal<>(this, 1.0D));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(WorldItems.CUTLASS.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    public void applyRaidBuffs(int wave, boolean unused) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // ------------------------------------------------------------------------------------------ the duel

    @Nullable
    @Override
    public UUID getDuelist() {
        return this.duelist;
    }

    @Override
    public void setDuelist(@Nullable UUID duelist) {
        this.duelist = duelist;
    }

    @Override
    public boolean hasSurrendered() {
        return false;
    }

    @Override
    public void surrender() {
    }

    private void announce(Component message) {
        for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(48.0D))) {
            player.displayClientMessage(message, false);
        }
    }

    /** The first player to come close gets called out: one on one, and the crew keeps out of it. */
    private void startDuel(Player player) {
        this.duelist = player.getUUID();
        this.dueled = true;
        this.setTarget(player);
        this.announce(Component.translatable("entity.dunesrelics.pirate_captain.duel", this.getDisplayName(), player.getDisplayName())
                .withStyle(ChatFormatting.GOLD));
        this.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 2.0F, 0.6F);
    }

    /** Somebody cheated (struck the crew, or helped the challenger): the whole crew comes for them. */
    public void breakDuel(LivingEntity cheat) {
        if (this.duelist == null) {
            return;
        }
        this.duelist = null;
        this.announce(Component.translatable("entity.dunesrelics.pirate_captain.cheat", this.getDisplayName()).withStyle(ChatFormatting.RED));
        this.playSound(SoundEvents.NOTE_BLOCK_FLUTE.value(), 2.0F, 1.8F);
        for (Mob mob : this.level().getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(32.0D), m -> m instanceof PirateCrew)) {
            ((PirateCrew) mob).setDuelist(null);
            mob.setTarget(cheat);
        }
    }

    private void keepDuel(ServerLevel level) {
        Player player = this.duelist != null ? level.getPlayerByUUID(this.duelist) : null;
        if (player == null || !player.isAlive() || player.isSpectator() || player.distanceTo(this) > 40.0F) {
            // the challenger fell or ran: the crew goes back to its business
            this.duelist = null;
            for (Mob mob : level.getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(48.0D), m -> m instanceof PirateCrew)) {
                ((PirateCrew) mob).setDuelist(null);
            }
            return;
        }
        this.setTarget(player);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(32.0D),
                m -> m instanceof PirateCrew && m != this)) {
            ((PirateCrew) mob).setDuelist(this.duelist);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // anyone but the challenger joining in breaks the duel
        if (this.duelist != null && source.getEntity() instanceof LivingEntity attacker && !attacker.getUUID().equals(this.duelist)) {
            this.breakDuel(attacker);
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (this.duelist != null && source.getEntity() instanceof Player winner && winner.getUUID().equals(this.duelist)
                && this.level() instanceof ServerLevel level) {
            this.announce(Component.translatable("entity.dunesrelics.pirate_captain.surrender").withStyle(ChatFormatting.GOLD));
            for (Mob mob : level.getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(48.0D),
                    m -> m instanceof PirateCrew && m != this)) {
                ((PirateCrew) mob).surrender();
            }
            WorldMemory.get(level).record(level.getDayTime() / 24000L, "chronicle.dunesrelics.duel", winner.getName().getString());
        }
        super.die(source);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (this.level() instanceof ServerLevel duelLevel) {
            if (this.duelist != null) {
                this.keepDuel(duelLevel);
                // in a duel he fights with the cutlass alone: no whistling for help, no cannonballs
                return;
            }
            if (!this.dueled && this.tickCount % 10 == 0) {
                Player challenger = duelLevel.getNearestPlayer(this, 10.0D);
                if (challenger != null && !challenger.isCreative() && !challenger.isSpectator() && this.hasLineOfSight(challenger)) {
                    this.startDuel(challenger);
                    return;
                }
            }
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (--this.whistleCooldown <= 0) {
            this.whistleCooldown = 240;
            int crew = level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(16.0D),
                    e -> e instanceof PirateCrew && e != this).size();
            if (crew < 5) {
                this.playSound(SoundEvents.NOTE_BLOCK_FLUTE.value(), 2.0F, 1.8F);
                for (int i = 0; i < 2; i++) {
                    Pirate pirate = ModEntities.PIRATE.get().create(level);
                    if (pirate == null) {
                        continue;
                    }
                    BlockPos pos = this.blockPosition().offset(this.random.nextInt(5) - 2, 0, this.random.nextInt(5) - 2);
                    pirate.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, this.random.nextFloat() * 360.0F, 0.0F);
                    pirate.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.MOB_SUMMONED, null, null);
                    pirate.setTarget(target);
                    level.addFreshEntity(pirate);
                }
            }
        }
        double distance = this.distanceTo(target);
        if (--this.throwCooldown <= 0 && distance > 6.0D && distance < 24.0D && this.hasLineOfSight(target)) {
            this.throwCooldown = 100;
            Vec3 from = this.getEyePosition();
            Vec3 to = target.position().subtract(from);
            double horizontal = Math.sqrt(to.x * to.x + to.z * to.z);
            double speed = 1.0D;
            double time = Math.max(1.0D, horizontal / speed);
            Cannonball ball = new Cannonball(level, from.x, from.y, from.z);
            ball.setOwner(this);
            ball.setDeltaMovement(to.x / time, (to.y + 0.5D * 0.04D * time * time) / time, to.z / time);
            level.addFreshEntity(ball);
            this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            this.playSound(SoundEvents.SNOWBALL_THROW, 1.0F, 0.5F);
        }
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

    @Nullable
    @Override
    public BlockPos getRaidTarget() {
        return this.raidTarget;
    }

    @Override
    public void setRaidTarget(@Nullable BlockPos target) {
        this.raidTarget = target;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Dueled", this.dueled);
        if (this.raidTarget != null) {
            tag.put("RaidTarget", NbtUtils.writeBlockPos(this.raidTarget));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.dueled = tag.getBoolean("Dueled");
        this.raidTarget = tag.contains("RaidTarget") ? NbtUtils.readBlockPos(tag.getCompound("RaidTarget")) : null;
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }
}
