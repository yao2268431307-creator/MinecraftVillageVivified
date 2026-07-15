package com.livingvillages.regions.network;

import com.livingvillages.regions.data.RegionStateStore;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-side sender of discovered settlement chunk coordinates.
 *
 * <p>The client cannot reliably read village {@code StructureStart} data from
 * its own chunks (1.20.1 clients do not receive the structure dynamic registry
 * or complete start maps). Instead, the server &mdash; which discovers villages
 * authoritatively in {@link RoadWeaverIntegrator} &mdash; pushes the set of
 * known settlement chunk coordinates to the client. The client then resolves
 * each settlement's tier and name deterministically from
 * {@code (worldSeed, chunkX, chunkZ)}, needing no client-side structure data.</p>
 *
 * <p>Packets are sent:</p>
 * <ul>
 *   <li>on {@link ServerPlayConnectionEvents#JOIN} &mdash; the full set of
 *       already-processed settlements, so villages discovered in a previous
 *       session name themselves without re-discovery;</li>
 *   <li>incrementally, whenever {@link RoadWeaverIntegrator} marks a new
 *       village chunk processed &mdash; a one-element packet to every online
 *       player so the new settlement is named as the player approaches.</li>
 * </ul>
 *
 * <p>Both use the {@link RegionNetworking#SETTLEMENTS_PACKET_ID} channel and the
 * same list format; the client accumulates (set semantics dedup), so join and
 * incremental packets compose safely.</p>
 */
public final class SettlementsSender {

    private static final Logger LOGGER = LoggerFactory.getLogger("livingvillages");

    private SettlementsSender() {
    }

    /** Register the JOIN listener that sends the full known set on player join. */
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            if (player == null) {
                return;
            }
            ServerLevel overworld = server.getLevel(Level.OVERWORLD);
            if (overworld == null) {
                return;
            }
            sendKnown(player, overworld);
        });
    }

    /**
     * Send the full set of already-processed settlement chunks to a player.
     *
     * @param player    the joining player
     * @param overworld the overworld (where {@link RegionStateStore} lives)
     */
    public static void sendKnown(ServerPlayer player, ServerLevel overworld) {
        RegionStateStore store = RegionStateStore.load(overworld);
        FriendlyByteBuf buf = encode(store.processedChunkKeys());
        ServerPlayNetworking.send(player, RegionNetworking.SETTLEMENTS_PACKET_ID, buf);
        LOGGER.info("Sent {} known settlements to {}",
            store.processedChunkKeys().size(), player.getName().getString());
    }

    /**
     * Send a single newly-discovered settlement to every online player.
     *
     * @param server the minecraft server
     * @param chunkX the settlement's generation chunk X
     * @param chunkZ the settlement's generation chunk Z
     */
    public static void sendOne(MinecraftServer server, int chunkX, int chunkZ) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(12));
            buf.writeVarInt(1);
            buf.writeInt(chunkX);
            buf.writeInt(chunkZ);
            ServerPlayNetworking.send(player, RegionNetworking.SETTLEMENTS_PACKET_ID, buf);
        }
    }

    private static FriendlyByteBuf encode(java.util.Set<Long> chunkKeys) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(8 + chunkKeys.size() * 8));
        buf.writeVarInt(chunkKeys.size());
        for (long key : chunkKeys) {
            buf.writeInt(RegionStateStore.chunkX(key));
            buf.writeInt(RegionStateStore.chunkZ(key));
        }
        return buf;
    }
}
