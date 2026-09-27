package com.dunesrelics.network;

import com.dunesrelics.DunesRelics;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** The mod's own messages between server and client. */
public final class ModNetwork {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(DunesRelics.id("main"), () -> VERSION,
            VERSION::equals, VERSION::equals);

    private ModNetwork() {}

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(CurrentFieldMessage.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CurrentFieldMessage::encode)
                .decoder(CurrentFieldMessage::decode)
                .consumerMainThread(CurrentFieldMessage::handle)
                .add();
    }
}
