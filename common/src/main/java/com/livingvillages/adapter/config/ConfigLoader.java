package com.livingvillages.adapter.config;

import com.livingvillages.core.config.ModConfig;

/**
 * Loads ModConfig from TOML file at mod initialization.
 *
 * <p>MC Adapter layer. Reads {@code config/livingvillages.toml}, deserializes
 * into a ModConfig record, falling back to defaults on error.</p>
 *
 * <p>Note: Template stub. Full implementation requires a TOML library
 * (e.g. toml4j, night-config) or manual parsing.</p>
 */
public final class ConfigLoader {

    private ConfigLoader() {}

    private static ModConfig cachedConfig = null;

    /**
     * Load config from TOML file, falling back to defaults on any error.
     *
     * @return valid ModConfig instance
     */
    public static ModConfig load() {
        if (cachedConfig != null) {
            return cachedConfig;
        }

        try {
            // MC: read config/livingvillages.toml from classpath or config dir
            // Parse TOML into ModConfig fields, validating @Range constraints
            // On parse error or validation failure → log WARN, use defaults
            cachedConfig = ModConfig.defaults();
        } catch (Exception e) {
            System.err.println("[LivingVillages] WARN: Config load failed, using defaults: " + e.getMessage());
            cachedConfig = ModConfig.defaults();
        }
        return cachedConfig;
    }

    /** Reload config at runtime. */
    public static ModConfig reload() {
        cachedConfig = null;
        return load();
    }
}
