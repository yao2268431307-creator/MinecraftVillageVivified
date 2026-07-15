package com.livingvillages.regions.network;

import com.livingvillages.regions.client.RegionTitleDisplay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side receiver for the settlements packet.
 *
 * <p>Accumulates settlement chunk coordinates (from both the join bulk-send and
 * incremental single-settlement packets) into {@link RegionTitleDisplay}'s
 * client store, and clears it on disconnect so a different world's settlements
 * never leak across sessions.</p>
 */
public final class SettlementsReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger("livingvillages");

    private SettlementsReceiver() {
    }

    /** Register the receiver + disconnect-clear. Call from a {@link ClientModInitializer}. */
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(RegionNetworking.SETTLEMENTS_PACKET_ID,
            (client, handler, buf, responseSender) -> {
                int count = buf.readVarInt();
                int[][] coords = new int[count][2];
                for (int i = 0; i < count; i++) {
                    coords[i][0] = buf.readInt();
                    coords[i][1] = buf.readInt();
                }
                client.execute(() -> {
                    for (int[] c : coords) {
                        RegionTitleDisplay.addSettlement(c[0], c[1]);
                    }
                    LOGGER.info("Received {} settlements (total now {})",
                        count, RegionTitleDisplay.settlementCount());
                });
            });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            client.execute(RegionTitleDisplay::clearSettlements));
    }
}
