package com.livingvillages.regions;

import com.livingvillages.regions.command.NearestSettlementCommand;
import com.livingvillages.regions.network.RoadWeaverIntegrator;
import com.livingvillages.regions.network.SeedSender;
import com.livingvillages.regions.network.SettlementsSender;
import com.livingvillages.regions.spawn.CitySpawner;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.commands.Commands.literal;

/**
 * LivingVillages Regions mod initializer.
 *
 * <p>Registers the server-side world-seed sender
 * ({@link SeedSender#register()}), the settlements sender
 * ({@link SettlementsSender#register()}), the {@code /nearestcity} and
 * {@code /nearesttown} commands ({@link CommandRegistrationCallback}), and a
 * throttled server-tick listener that drives
 * {@link RoadWeaverIntegrator#onTick(MinecraftServer)} once per
 * {@value RoadWeaverIntegrator#TICK_THROTTLE} ticks (to scan for new villages,
 * register/connect them with RoadWeaver, and push their chunk coordinates to
 * clients) and {@link CitySpawner#onTick(MinecraftServer)} once per
 * {@value CitySpawner#TICK_THROTTLE} ticks (to top up villagers in towns/cities
 * and spawn a permanent trader at each city). The companion client entrypoint
 * ({@link RegionsClientMod}) handles the matching receivers and the settlement
 * title float-text listener.</p>
 */
public class RegionsMod implements ModInitializer {

    public static final String MOD_ID = "livingvillages-regions";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("LivingVillages Regions initializing");
        SeedSender.register();
        SettlementsSender.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, regCtx, env) ->
            dispatcher.register(literal("nearestcity")
                .executes(NearestSettlementCommand::nearestCity))
        );
        CommandRegistrationCallback.EVENT.register((dispatcher, regCtx, env) ->
            dispatcher.register(literal("nearesttown")
                .executes(NearestSettlementCommand::nearestTown))
        );
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int tick = server.getTickCount();
            if (tick % RoadWeaverIntegrator.TICK_THROTTLE == 0) {
                RoadWeaverIntegrator.onTick(server);
            }
            if (tick % CitySpawner.TICK_THROTTLE == 0) {
                CitySpawner.onTick(server);
            }
        });
    }
}
