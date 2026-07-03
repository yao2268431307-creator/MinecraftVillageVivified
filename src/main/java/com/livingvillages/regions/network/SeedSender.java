package com.livingvillages.regions.network;

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
 * Server-side world-seed sender.
 *
 * <p>On {@link ServerPlayConnectionEvents#JOIN} the overworld seed is read from
 * the {@link ServerLevel} and pushed to the joining player's client through a
 * custom channel ({@link RegionNetworking#WORLD_SEED_PACKET_ID}). The client
 * handler ({@link SeedReceiver}) forwards it into
 * {@code RegionTitleDisplay.setWorldSeed(long)} so the deterministic region
 * names can resolve on the client without exposing the seed through any vanilla
 * client API.</p>
 *
 * <p>Only the overworld seed is sent — region names are derived per-overworld.
 * Other dimensions share {@link Level#OVERWORLD}'s seed for naming purposes,
 * so a single packet per player-join is sufficient.</p>
 *
 * <p>Failure modes: if the overworld {@link ServerLevel} is somehow not loaded
 * when a player joins (extremely unlikely — the overworld is the first
 * dimension loaded by a dedicated server), the packet is silently skipped; the
 * client stays in its seed-unset state and simply shows no region float-text
 * until a subsequent join re-fires this handler.</p>
 */
public final class SeedSender {

    private static final Logger LOGGER = LoggerFactory.getLogger("livingvillages");

    private SeedSender() {
    }

    /** Register the JOIN listener. Call once from a {@code ModInitializer}. */
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            if (player == null) {
                return;
            }
            MinecraftServer srv = player.getServer();
            if (srv == null) {
                return;
            }
            ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
            if (overworld == null) {
                return;
            }
            long seed = overworld.getSeed();
            LOGGER.info("Sending world seed {} to {}", seed, player.getName().getString());
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(8));
            buf.writeLong(seed);
            ServerPlayNetworking.send(player, RegionNetworking.WORLD_SEED_PACKET_ID, buf);
        });
    }
}
