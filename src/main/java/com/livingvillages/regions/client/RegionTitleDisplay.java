package com.livingvillages.regions.client;

import com.livingvillages.regions.biome.BiomeRegionResolver;
import com.livingvillages.regions.biome.RegionType;
import com.livingvillages.regions.naming.RegionNameGenerator;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Client-side region name float-text (action bar) display.
 *
 * <p>Every {@value #TICK_THROTTLE} ticks (0.5s) the player's current super-cell
 * is resolved into a {@link RegionType} via {@link BiomeRegionResolver} and the
 * deterministic region name is generated via {@link RegionNameGenerator}. When
 * the resolved name changes, it is shown on the action bar through
 * {@link net.minecraft.world.entity.Entity#displayClientMessage(Component, boolean)}
 * with {@code aboveHotbar = true}.</p>
 *
 * <p>Excluded biomes (cave / ocean / river / beach) yield a {@code null}
 * region type — in that case no float-text is shown and {@link #lastRegion}
 * is cleared, so the name re-appears when the player leaves the excluded
 * area and re-enters a region.</p>
 *
 * <h2>World seed injection</h2>
 *
 * <p>Vanilla 1.20.1 {@code ClientLevel} does <em>not</em> expose the world
 * seed to the client (the seed is server-side only and is deliberately
 * withheld to prevent seed-extraction attacks). Region names are derived
 * deterministically from the world seed, so the client must obtain the seed
 * through a side channel. A future server-&gt;client packet handler (out of
 * scope for this module) is expected to call {@link #setWorldSeed(long)} once
 * the server has authenticated the player. Until then, the float-text is
 * suppressed and this listener is a no-op.</p>
 *
 * <h2>Stage 1</h2>
 *
 * <p>This is a stage-1 implementation: only the region name is shown. The
 * village-name overlay is added in a later stage (see {@code DESIGN.md}
 * section "客户端飘字").</p>
 *
 * <p>This class implements {@link ClientTickEvents.EndTick} but does
 * not self-register; the caller (typically a {@code ClientModInitializer}
 * in a later module) is responsible for invoking
 * {@code ClientTickEvents.END_CLIENT_TICK.register(new RegionTitleDisplay())}.</p>
 */
public class RegionTitleDisplay implements ClientTickEvents.EndTick {

    /** Super-cell size in blocks (= {@code 7 * 34 * 16}). */
    public static final int SUPER_CELL_SIZE = 7 * 34 * 16;

    /** Tick interval between position checks (10 = 0.5s on a 20 tps server). */
    private static final int TICK_THROTTLE = 10;

    /** Sentinel value indicating the world seed has not yet been received. */
    private static final long SEED_UNSET = Long.MIN_VALUE;

    /** Last shown region name; used to suppress duplicate float-text. */
    private static String lastRegion = "";

    /** World seed injected by an external packet handler; {@link #SEED_UNSET} until then. */
    private static long worldSeed = SEED_UNSET;

    /**
     * Inject the world seed from a server-&gt;client packet handler. Once set,
     * the float-text listener becomes active. Setting the same seed again is
     * a no-op; setting a different seed clears {@link #lastRegion} so the
     * new world's region name re-fires immediately.
     *
     * @param seed the world seed received from the server
     */
    public static void setWorldSeed(long seed) {
        if (seed != worldSeed) {
            worldSeed = seed;
            lastRegion = "";
        }
    }

    @Override
    public void onEndTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        // No seed yet: cannot resolve deterministic names. Stay quiet.
        if (worldSeed == SEED_UNSET) {
            return;
        }
        // Throttle: only check once every TICK_THROTTLE ticks. player.tickCount
        // is MC's built-in tick counter, no need for a self-managed counter.
        if (mc.player.tickCount % TICK_THROTTLE != 0) {
            return;
        }

        int blockX = mc.player.getBlockX();
        int blockZ = mc.player.getBlockZ();
        int superX = Math.floorDiv(blockX, SUPER_CELL_SIZE);
        int superZ = Math.floorDiv(blockZ, SUPER_CELL_SIZE);

        RegionType type = BiomeRegionResolver.resolveRegionType(mc.level, blockX, blockZ);
        if (type == null) {
            // In a cave / water / beach biome: no float-text. Clear the last
            // name so the message re-fires when the player re-enters a region.
            lastRegion = "";
            return;
        }

        String regionName = RegionNameGenerator.regionName(worldSeed, superX, superZ, type);
        if (!regionName.equals(lastRegion)) {
            mc.player.displayClientMessage(Component.literal(regionName), true);
            lastRegion = regionName;
        }
    }
}
