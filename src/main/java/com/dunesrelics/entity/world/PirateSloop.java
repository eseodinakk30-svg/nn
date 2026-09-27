package com.dunesrelics.entity.world;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.memory.Currents;
import com.dunesrelics.memory.WorldMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A pirate sloop under sail: a single-masted ship with two guns a side and a crew of up to six. It cruises round its
 * home waters, turns its broadside on anyone who comes within range, and on a raid sails in to a coastal village,
 * bombards it and puts its crew ashore. Arrows, cannonballs and axes wear it down; when it breaks up it spills its
 * hold into the sea.
 */
public class PirateSloop extends Entity {
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(PirateSloop.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> HURT = SynchedEntityData.defineId(PirateSloop.class, EntityDataSerializers.INT);
    public static final float HULL = 120.0F;
    private static final int CREW = 6;
    private static final double SPEED = 0.16D;

    @Nullable
    private BlockPos home;
    /** The bell of the village it came to raid, until the crew is ashore. */
    @Nullable
    private BlockPos raid;
    @Nullable
    private Vec3 waypoint;
    private int fireCooldown = 60;
    private int idle;
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;

    public PirateSloop(EntityType<? extends PirateSloop> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    public void setHome(BlockPos home) {
        this.home = home.immutable();
    }

    public void setRaid(@Nullable BlockPos bell) {
        this.raid = bell;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DAMAGE, 0.0F);
        this.entityData.define(HURT, 0);
    }

    public float getDamage() {
        return this.entityData.get(DAMAGE);
    }

    public int getHurtTime() {
        return this.entityData.get(HURT);
    }

    // ------------------------------------------------------------------------------------------ what it is like to touch

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < CREW;
    }

    /** The crew sit along the deck, two abreast. */
    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        int index = Math.max(0, this.getPassengers().indexOf(passenger));
        double side = index % 2 == 0 ? -0.7D : 0.7D;
        double along = 2.0D - (index / 2) * 1.8D;
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        double x = this.getX() + side * Mth.cos(yaw) - along * Mth.sin(yaw);
        double z = this.getZ() + side * Mth.sin(yaw) + along * Mth.cos(yaw);
        move.accept(passenger, x, this.getY() + 0.65D, z);
    }

    // ------------------------------------------------------------------------------------------ sailing

    /** The height of the water surface under the hull, or NaN on dry land. */
    private double waterSurface() {
        BlockPos pos = this.blockPosition();
        for (int dy = 1; dy >= -2; dy--) {
            BlockPos p = pos.above(dy);
            if (this.level().getFluidState(p).is(FluidTags.WATER)) {
                return p.getY() + this.level().getFluidState(p).getHeight(this.level(), p);
            }
        }
        return Double.NaN;
    }

    private boolean waterAt(Vec3 at) {
        BlockPos p = BlockPos.containing(at.x, this.getY() + 0.2D, at.z);
        return this.level().getFluidState(p).is(FluidTags.WATER) || this.level().getFluidState(p.below()).is(FluidTags.WATER);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getHurtTime() > 0) {
            this.entityData.set(HURT, this.getHurtTime() - 1);
        }
        if (this.level().isClientSide) {
            this.tickLerp();
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        double surface = this.waterSurface();
        Vec3 motion = this.getDeltaMovement();
        if (Double.isNaN(surface)) {
            motion = motion.add(0.0D, -0.04D, 0.0D).scale(0.8D);
        } else {
            double wanted = surface - 0.35D;
            motion = new Vec3(motion.x * 0.9D, (wanted - this.getY()) * 0.2D, motion.z * 0.9D);
            motion = motion.add(Currents.flow(level, this.blockPosition()).scale(0.5D));
        }
        LivingEntity foe = this.findFoe(level);
        Vec3 heading = this.steer(level, foe);
        float yaw = (float) (Mth.atan2(-heading.x, heading.z) * Mth.RAD_TO_DEG);
        this.setYRot(Mth.approachDegrees(this.getYRot(), yaw, 2.5F));
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot());
        Vec3 ahead = this.position().add(forward.scale(4.5D));
        double speed = this.waterAt(ahead) ? SPEED : 0.0D;
        if (foe != null && this.distanceTo(foe) < 24.0D) {
            speed *= 0.5D;
        }
        motion = motion.add(forward.x * speed * 0.1D, 0.0D, forward.z * speed * 0.1D);
        this.setDeltaMovement(motion);
        this.move(MoverType.SELF, motion);
        if (foe != null) {
            this.fireAt(level, foe);
        }
        if (this.raid != null && !this.waterAt(ahead) && !this.getPassengers().isEmpty()) {
            this.landCrew(level);
        }
        if (++this.idle > 6000 && level.getNearestPlayer(this, 128.0D) == null) {
            // sails off over the horizon
            this.getPassengers().forEach(Entity::discard);
            this.discard();
        }
    }

    @Nullable
    private LivingEntity findFoe(ServerLevel level) {
        Player player = level.getNearestPlayer(this.getX(), this.getY(), this.getZ(), 40.0D,
                p -> !p.isSpectator() && !((Player) p).isCreative() && p.isAlive());
        if (player != null) {
            this.idle = 0;
            return player;
        }
        if (this.raid != null) {
            List<Villager> villagers = level.getEntitiesOfClass(Villager.class, this.getBoundingBox().inflate(36.0D, 12.0D, 36.0D));
            if (!villagers.isEmpty()) {
                return villagers.get(this.random.nextInt(villagers.size()));
            }
        }
        return null;
    }

    /** Where to head: towards the village it raids, round its foe (keeping its broadside to it), or its cruising ground. */
    private Vec3 steer(ServerLevel level, @Nullable LivingEntity foe) {
        Vec3 here = this.position();
        if (foe != null && this.raid == null) {
            Vec3 to = foe.position().subtract(here).multiply(1.0D, 0.0D, 1.0D);
            double distance = to.length();
            if (distance > 24.0D) {
                return to.normalize();
            }
            if (distance < 12.0D) {
                return to.normalize().scale(-1.0D);
            }
            // circle round, guns towards it
            return new Vec3(-to.z, 0.0D, to.x).normalize();
        }
        if (this.raid != null) {
            return Vec3.atCenterOf(this.raid).subtract(here).multiply(1.0D, 0.0D, 1.0D).normalize();
        }
        BlockPos centre = this.home != null ? this.home : this.blockPosition();
        if (this.waypoint == null || this.waypoint.distanceToSqr(here.x, this.waypoint.y, here.z) < 16.0D || this.random.nextInt(600) == 0) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double distance = 16.0D + this.random.nextDouble() * 40.0D;
            this.waypoint = new Vec3(centre.getX() + Math.cos(angle) * distance, this.getY(), centre.getZ() + Math.sin(angle) * distance);
        }
        Vec3 to = this.waypoint.subtract(here).multiply(1.0D, 0.0D, 1.0D);
        return to.lengthSqr() < 1.0E-4D ? Vec3.directionFromRotation(0.0F, this.getYRot()) : to.normalize();
    }

    /** A broadside: a cannonball from the side facing the foe, aimed to fall on it. */
    private void fireAt(ServerLevel level, LivingEntity foe) {
        if (--this.fireCooldown > 0 || this.distanceTo(foe) > 40.0D || this.getPassengers().isEmpty() && this.raid == null) {
            return;
        }
        this.fireCooldown = 50 + this.random.nextInt(30);
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot());
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 to = foe.position().subtract(this.position());
        Vec3 side = right.dot(to) >= 0.0D ? right : right.scale(-1.0D);
        Vec3 muzzle = this.position().add(side.scale(1.8D)).add(forward.scale(this.random.nextBoolean() ? 0.8D : -0.8D)).add(0.0D, 1.0D, 0.0D);
        Vec3 aim = foe.getEyePosition().subtract(muzzle);
        double horizontal = Math.sqrt(aim.x * aim.x + aim.z * aim.z);
        double time = Math.max(1.0D, horizontal / 1.6D);
        Vec3 velocity = new Vec3(aim.x / time, (aim.y + 0.5D * 0.04D * time * time) / time, aim.z / time)
                .add(this.random.nextGaussian() * 0.04D, 0.0D, this.random.nextGaussian() * 0.04D);
        Cannonball ball = new Cannonball(level, muzzle.x, muzzle.y, muzzle.z);
        ball.setOwner(this);
        ball.setDeltaMovement(velocity);
        level.addFreshEntity(ball);
        level.playSound(null, BlockPos.containing(muzzle), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 1.2F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 14, 0.3D, 0.3D, 0.3D, 0.05D);
        level.sendParticles(ParticleTypes.EXPLOSION, muzzle.x, muzzle.y, muzzle.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    /** Aground by the village: the crew jump ashore and make for the bell, and the sloop stands off. */
    private void landCrew(ServerLevel level) {
        BlockPos bell = this.raid;
        for (Entity passenger : List.copyOf(this.getPassengers())) {
            passenger.stopRiding();
            Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot());
            passenger.teleportTo(passenger.getX() + forward.x * 4.0D, passenger.getY() + 0.5D, passenger.getZ() + forward.z * 4.0D);
            if (passenger instanceof PirateCrew crew) {
                crew.setRaidTarget(bell);
            }
        }
        level.playSound(null, this.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 32.0F, 0.9F);
        this.raid = null;
        this.home = this.blockPosition().offset(0, 0, 0);
    }

    // ------------------------------------------------------------------------------------------ damage

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || this.isRemoved() || this.isInvulnerableTo(source)) {
            return false;
        }
        if (source.getEntity() instanceof PirateCrew || source.getEntity() == this) {
            return false;
        }
        float damage = source.is(DamageTypeTags.IS_EXPLOSION) ? amount * 2.0F : amount;
        this.entityData.set(DAMAGE, this.getDamage() + damage);
        this.entityData.set(HURT, 10);
        this.markHurt();
        this.playSound(SoundEvents.WOOD_HIT, 1.0F, 0.7F);
        if (this.getDamage() >= HULL) {
            this.breakUp((ServerLevel) this.level(), source);
        }
        return true;
    }

    /** Breaks up and sinks: planks and the contents of its hold float up, and the crew are in the water. */
    private void breakUp(ServerLevel level, DamageSource source) {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.0D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.5D, this.getZ(), 80, 2.0D, 0.5D, 2.0D, 0.2D);
        this.playSound(SoundEvents.GENERIC_EXPLODE, 2.0F, 0.6F);
        this.ejectPassengers();
        this.spawnAtLocation(new ItemStack(Items.DARK_OAK_PLANKS, 6 + this.random.nextInt(8)));
        LootTable hold = level.getServer().getLootData().getLootTable(DunesRelics.id("chests/pirate_hold"));
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, this.position())
                .create(LootContextParamSets.CHEST);
        for (ItemStack stack : hold.getRandomItems(params)) {
            this.spawnAtLocation(stack, 1.0F);
        }
        if (source.getEntity() instanceof Player player) {
            WorldMemory.get(level).record(level.getDayTime() / 24000L, "chronicle.dunesrelics.sloop_sunk", player.getName().getString());
        }
        this.discard();
    }

    // ------------------------------------------------------------------------------------------ client movement

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpSteps = 10;
    }

    private void tickLerp() {
        if (this.lerpSteps > 0) {
            double x = this.getX() + (this.lerpX - this.getX()) / this.lerpSteps;
            double y = this.getY() + (this.lerpY - this.getY()) / this.lerpSteps;
            double z = this.getZ() + (this.lerpZ - this.getZ()) / this.lerpSteps;
            this.setYRot(this.getYRot() + (float) Mth.wrapDegrees(this.lerpYRot - this.getYRot()) / this.lerpSteps);
            this.lerpSteps--;
            this.setPos(x, y, z);
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(4.0D, 4.0D, 4.0D);
    }

    // ------------------------------------------------------------------------------------------ saving

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Damage", this.getDamage());
        if (this.home != null) {
            tag.put("Home", NbtUtils.writeBlockPos(this.home));
        }
        if (this.raid != null) {
            tag.put("Raid", NbtUtils.writeBlockPos(this.raid));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DAMAGE, tag.getFloat("Damage"));
        this.home = tag.contains("Home") ? NbtUtils.readBlockPos(tag.getCompound("Home")) : null;
        this.raid = tag.contains("Raid") ? NbtUtils.readBlockPos(tag.getCompound("Raid")) : null;
    }
}
