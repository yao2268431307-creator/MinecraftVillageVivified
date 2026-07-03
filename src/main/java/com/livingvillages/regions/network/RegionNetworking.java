package com.livingvillages.regions.network;

import net.minecraft.resources.ResourceLocation;

/**
 * Shared constants for the LivingVillages Regions network channel.
 *
 * <p>Minecraft 1.20.1 uses the {@code ResourceLocation} + {@code FriendlyByteBuf}
 * style of custom payload (the 1.20.5+ {@code Payload} API does not yet exist).
 * A single channel id is declared here so both the server-side
 * {@link SeedSender} and the client-side {@link SeedReceiver} reference it.</p>
 *
 * <p>The packet body is exactly 8 bytes — one signed {@code long} carrying the
 * overworld world seed. There is no length prefix or version byte; the channel id
 * itself disambiguates the payload.</p>
 */
public final class RegionNetworking {

    /** Channel id for the world-seed packet. */
    public static final ResourceLocation WORLD_SEED_PACKET_ID =
        new ResourceLocation("livingvillages", "world_seed");

    private RegionNetworking() {
    }
}
