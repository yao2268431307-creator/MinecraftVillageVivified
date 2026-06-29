package com.livingvillages.adapter.config;

import com.livingvillages.core.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads ModConfig from TOML file at mod initialization.
 * Falls back to defaults on any error.
 */
public final class ConfigLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger("livingvillages");
    private static ModConfig cachedConfig;

    private ConfigLoader() {}

    public static ModConfig load() {
        if (cachedConfig != null) return cachedConfig;
        try {
            // TODO: Read config/livingvillages.toml, parse TOML, construct ModConfig
            cachedConfig = ModConfig.defaults();
            LOGGER.info("Loaded default config");
        } catch (Exception e) {
            LOGGER.warn("Config load failed, using defaults: {}", e.getMessage());
            cachedConfig = ModConfig.defaults();
        }
        return cachedConfig;
    }

    public static ModConfig reload() {
        cachedConfig = null;
        return load();
    }
}
