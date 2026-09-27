package com.dunesrelics.network;

import com.dunesrelics.client.world.ClientCurrents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * The river flow around a player (a square of cells, each an angle and a strength), and whether currents are on at
 * all. Sent every second.
 */
public record CurrentFieldMessage(boolean enabled, int originX, int originZ, byte[] angles, byte[] strengths) {
    public CurrentFieldMessage(int originX, int originZ, byte[] angles, byte[] strengths) {
        this(true, originX, originZ, angles, strengths);
    }

    public static CurrentFieldMessage off() {
        return new CurrentFieldMessage(false, 0, 0, new byte[0], new byte[0]);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.enabled);
        buf.writeInt(this.originX);
        buf.writeInt(this.originZ);
        buf.writeByteArray(this.angles);
        buf.writeByteArray(this.strengths);
    }

    public static CurrentFieldMessage decode(FriendlyByteBuf buf) {
        return new CurrentFieldMessage(buf.readBoolean(), buf.readInt(), buf.readInt(), buf.readByteArray(), buf.readByteArray());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientCurrents.receive(this)));
        context.get().setPacketHandled(true);
    }
}
