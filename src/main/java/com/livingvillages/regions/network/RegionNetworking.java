package com.livingvillages.regions.network;

import net.minecraft.resources.ResourceLocation;

/**
 * Shared constants for the LivingVillages Regions network channel.
 *
 * <p>Minecraft 1.20.1 uses the {@code ResourceLocation} + {@code FriendlyByteBuf}
 * style of custom payload (the 1.20.5+ {@code Payload} API does not yet exist).
 * Channel ids are declared here so the server-side senders and client-side
 * receivers reference them.</p>
 */
public final class RegionNetworking {

    /** Channel id for the world-seed packet (8-byte long payload). */
    public static final ResourceLocation WORLD_SEED_PACKET_ID =
        new ResourceLocation("livingvillages", "world_seed");

    /**
     * Channel id for the settlements packet. Payload: {@code varInt count}
     * followed by {@code count &times; (int chunkX, int chunkZ)}. The client
     * accumulates these chunk coords and uses them (with the world seed) to
     * resolve per-settlement tier + name without needing client-side
     * StructureStart data, which 1.20.1 clients do not reliably expose.
     */
    public static final ResourceLocation SETTLEMENTS_PACKET_ID =
        new ResourceLocation("livingvillages", "settlements");

    private RegionNetworking() {
    }
}
