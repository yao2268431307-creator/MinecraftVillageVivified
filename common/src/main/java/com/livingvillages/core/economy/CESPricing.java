package com.livingvillages.core.economy;

import java.util.*;

/**
 * CES (Constant Elasticity of Substitution) production function pricing.
 *
 * <p>For each cluster, computes equilibrium prices based on local supply and demand:</p>
 * <pre>
 *   equilibriumPrice = basePrice × (demand / supply)^(1/σ)
 * </pre>
 *
 * <p>Where σ (sigma) is the CES elasticity of substitution.
 * When supply → 0, price is capped at basePrice × 10.</p>
 */
final class CESPricing {

    /** Maximum price multiplier when supply approaches zero. */
    private static final double PRICE_CAP_MULTIPLIER = 10.0;

    /**
     * Per-capita daily demand for each commodity (tunable).
     */
    static final Map<String, Double> PER_CAPITA_DEMAND = Map.of(
            "food", 0.5,
            "wood", 0.3,
            "stone", 0.2,
            "iron", 0.1,
            "luxury", 0.05
    );

    /**
     * Base price for each commodity (before supply/demand adjustment).
     */
    static final Map<String, Double> BASE_PRICES = Map.of(
            "food", 10.0,
            "wood", 8.0,
            "stone", 12.0,
            "iron", 25.0,
            "luxury", 50.0
    );

    /** All known commodity item IDs. */
    static final List<String> COMMODITIES = List.of("food", "wood", "stone", "iron", "luxury");

    private CESPricing() {}

    /**
     * Compute equilibrium price for a commodity in a cluster.
     *
     * @param itemId      the commodity
     * @param population  total population (sum of bedCount)
     * @param localSupply local warehouse stock (sum of member villages)
     * @param sigma       CES elasticity (from config)
     * @return equilibrium price
     */
    static double computePrice(String itemId, int population, long localSupply, double sigma) {
        double basePrice = BASE_PRICES.getOrDefault(itemId, 10.0);
        double perCapita = PER_CAPITA_DEMAND.getOrDefault(itemId, 0.1);
        double demand = population * perCapita;

        if (localSupply <= 0 || demand <= 0) {
            return basePrice * PRICE_CAP_MULTIPLIER;
        }

        // CES equilibrium: price = basePrice × (demand / supply)^(1/σ)
        double ratio = demand / localSupply;
        double exponent = 1.0 / sigma;
        double price = basePrice * Math.pow(ratio, exponent);

        // Cap
        return Math.min(price, basePrice * PRICE_CAP_MULTIPLIER);
    }

    /**
     * Compute CES production output Q.
     *
     * <p>Q = A × (α·L^ρ + (1-α)·K^ρ)^(1/ρ), where ρ = (σ-1)/σ</p>
     *
     * @param labor      L (population)
     * @param capital    K (warehouse stock)
     * @param sigma      CES elasticity
     * @param alpha      labor share (default 0.6)
     * @param totalFactor total factor productivity A (default 1.0)
     * @return production output Q
     */
    static double cesProduction(double labor, double capital, double sigma, double alpha, double totalFactor) {
        if (sigma == 1.0) {
            // Cobb-Douglas limit as σ→1
            return totalFactor * Math.pow(labor, alpha) * Math.pow(Math.max(capital, 1), 1 - alpha);
        }

        double rho = (sigma - 1.0) / sigma;
        double term = alpha * Math.pow(Math.max(labor, 0.1), rho)
                + (1 - alpha) * Math.pow(Math.max(capital, 1), rho);

        return totalFactor * Math.pow(term, 1.0 / rho);
    }
}
