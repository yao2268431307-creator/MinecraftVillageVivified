package com.livingvillages.regions.network;

import com.livingvillages.regions.client.RegionTitleDisplay;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side world-seed receiver.
 *
 * <p>On receipt of {@link RegionNetworking#WORLD_SEED_PACKET_ID} the long is
 * read off the buffer and forwarded into
 * {@link RegionTitleDisplay#setWorldSeed(long)} on the client thread. The
 * read is performed on the network thread (as required by the Fabric API
 * contract for {@link ClientPlayNetworking.PlayChannelHandler#receive});
 * only the side-effectful mutation of the client-side static state is
 * scheduled onto the client thread via {@code client.execute(...)}.</p>
 *
 * <p>Register once from a {@code ClientModInitializer}.</p>
 */
public final class SeedReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger("livingvillages");

    private SeedReceiver() {
    }

    /** Register the global client receiver. Call once from a {@code ClientModInitializer}. */
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(RegionNetworking.WORLD_SEED_PACKET_ID,
            (client, handler, buf, responseSender) -> {
                // readLong must run on the network thread, before the buffer is released.
                final long seed = buf.readLong();
                client.execute(() -> {
                    LOGGER.info("Received world seed {}", seed);
                    RegionTitleDisplay.setWorldSeed(seed);
                });
            });
    }
}
