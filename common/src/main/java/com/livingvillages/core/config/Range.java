package com.livingvillages.core.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runtime-visible range constraint for numeric config fields.
 *
 * <p>Used for documentation and runtime validation in {@link ModConfig}'s compact constructor.
 * {@link RetentionPolicy#RUNTIME} allows the config loader (MC Adapter) to inspect
 * constraints at runtime for generating TOML schema documentation.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.RECORD_COMPONENT)
public @interface Range {
    /** Inclusive minimum value. */
    double min();

    /** Inclusive maximum value. */
    double max();
}
