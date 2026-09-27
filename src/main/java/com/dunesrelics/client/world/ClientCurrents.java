package com.dunesrelics.client.world;

import com.dunesrelics.DunesRelics;
import com.dunesrelics.memory.Tides;
import com.dunesrelics.registry.ModTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The player's own boat and swimming are simulated by the client, so the current has to push them here.
 */
@Mod.EventBusSubscriber(modid = DunesRelics.MODID, value = Dist.CLIENT)
public final class ClientCurrents {
    private ClientCurrents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (event.phase != TickEvent.Phase.END || player == null || minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        if (player.getVehicle() instanceof Boat boat && boat.isControlledByLocalInstance() && boat.isInWater()
                && minecraft.level.getBiome(boat.blockPosition()).is(ModTags.HAS_CURRENTS)) {
            Vec3 push = Tides.current(minecraft.level, boat.blockPosition());
            boat.setDeltaMovement(boat.getDeltaMovement().add(push));
        } else if (!player.isPassenger() && player.isInWater() && !player.getAbilities().flying
                && minecraft.level.getBiome(player.blockPosition()).is(ModTags.HAS_CURRENTS)) {
            Vec3 push = Tides.current(minecraft.level, player.blockPosition());
            player.setDeltaMovement(player.getDeltaMovement().add(push.scale(0.5D)));
        }
    }
}
