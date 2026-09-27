package com.dunesrelics.client.world;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.memory.Currents;
import com.dunesrelics.network.CurrentFieldMessage;
import com.dunesrelics.registry.ModParticles;
import com.dunesrelics.registry.ModTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The player's own boat and swimming are simulated by the client, so the current pushes them here; and the foam
 * drifting on the surface shows which way the water runs. River flow comes from the server; the ocean streams are
 * worked out here, the same way the server does.
 */
@Mod.EventBusSubscriber(modid = DunesRelics.MODID, value = Dist.CLIENT)
public final class ClientCurrents {
    private static boolean enabled = true;
    private static int originX;
    private static int originZ;
    private static byte[] angles = new byte[0];
    private static byte[] strengths = new byte[0];

    private ClientCurrents() {}

    public static void receive(CurrentFieldMessage message) {
        enabled = message.enabled();
        originX = message.originX();
        originZ = message.originZ();
        angles = message.angles();
        strengths = message.strengths();
    }

    public static Vec3 flow(ClientLevel level, BlockPos pos) {
        if (!enabled) {
            return Vec3.ZERO;
        }
        Holder<Biome> biome = level.getBiome(pos);
        if (Currents.isRiver(biome)) {
            int i = Math.floorDiv(pos.getX(), Currents.CELL) - originX;
            int j = Math.floorDiv(pos.getZ(), Currents.CELL) - originZ;
            int index = i * Currents.FIELD + j;
            if (i < 0 || j < 0 || i >= Currents.FIELD || j >= Currents.FIELD || index >= angles.length) {
                return Vec3.ZERO;
            }
            return Currents.decode(angles[index], strengths[index]);
        }
        if (biome.is(ModTags.HAS_CURRENTS)) {
            return Currents.oceanStream(pos.getX(), pos.getZ());
        }
        return Vec3.ZERO;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (event.phase != TickEvent.Phase.END || player == null || level == null || minecraft.isPaused()) {
            return;
        }
        if (player.getVehicle() instanceof Boat boat && boat.isControlledByLocalInstance() && boat.isInWater()) {
            boat.setDeltaMovement(boat.getDeltaMovement().add(flow(level, boat.blockPosition())));
        } else if (!player.isPassenger() && player.isInWater() && !player.getAbilities().flying) {
            player.setDeltaMovement(player.getDeltaMovement().add(flow(level, player.blockPosition()).scale(0.6D)));
        }
        foam(level, player);
    }

    /** Flecks of foam on the surface, and bubbles under it, drifting with the water around the player. */
    private static void foam(ClientLevel level, LocalPlayer player) {
        if (!enabled) {
            return;
        }
        RandomSource random = level.random;
        for (int n = 0; n < 4; n++) {
            int x = player.getBlockX() + random.nextInt(41) - 20;
            int z = player.getBlockZ() + random.nextInt(41) - 20;
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            BlockPos surface = new BlockPos(x, y, z);
            if (!level.getFluidState(surface).is(FluidTags.WATER) || !level.getBlockState(surface.above()).isAir()) {
                continue;
            }
            Vec3 flow = flow(level, surface);
            double strength = flow.length();
            if (strength < 0.008D || random.nextDouble() * Currents.STREAM > strength) {
                continue;
            }
            level.addParticle(ModParticles.FOAM.get(), x + random.nextDouble(), y + 0.92D, z + random.nextDouble(),
                    flow.x * 10.0D, 0.0D, flow.z * 10.0D);
        }
        if (player.isUnderWater() && random.nextInt(3) == 0) {
            Vec3 flow = flow(level, player.blockPosition());
            if (flow.lengthSqr() > 1.0E-4D) {
                level.addParticle(ParticleTypes.BUBBLE, player.getX() + random.nextGaussian() * 2.0D, player.getEyeY() + random.nextGaussian(),
                        player.getZ() + random.nextGaussian() * 2.0D, flow.x * 10.0D, 0.02D, flow.z * 10.0D);
            }
        }
    }
}
