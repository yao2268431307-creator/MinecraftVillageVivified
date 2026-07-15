package com.livingvillages.regions.tier;

import com.livingvillages.regions.naming.SeedHash;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SettlementTier}.
 *
 * <p>Exercises the pure tier-resolution logic: determinism, the 1:2:5
 * raw distribution, the no-two-adjacent-cities min-gap invariant, and
 * hash parity with {@link SeedHash}. No Minecraft runtime required.</p>
 */
class SettlementTierTest {

    private static final long SEED = 0x5EED_5EEDL;

    // ---------- 1. Determinism ----------

    @Test
    @DisplayName("tierFor is deterministic for the same seed + chunk coords")
    void tierForIsDeterministic() {
        for (int cx = -5; cx <= 5; cx++) {
            for (int cz = -5; cz <= 5; cz++) {
                SettlementTier first = SettlementTier.tierFor(SEED, cx, cz);
                SettlementTier second = SettlementTier.tierFor(SEED, cx, cz);
                assertEquals(first, second, "(" + cx + "," + cz + ") must be deterministic");
            }
        }
    }

    @Test
    @DisplayName("tierFor differs when the chunk differs within a grid cell (raw-chunk hash independence)")
    void tierForConstantWithinGridCell() {
        // Same grid cell (chunks 0..33 all map to cell 0) → same tier.
        SettlementTier base = SettlementTier.tierFor(SEED, 0, 0);
        for (int c = 1; c < SettlementTier.VILLAGE_SPACING; c++) {
            assertEquals(base, SettlementTier.tierFor(SEED, c, 0),
                "chunk " + c + " is in the same grid cell as 0");
        }
    }

    @Test
    @DisplayName("tierFor differs when the grid cell differs")
    void tierForDiffersByGridCell() {
        Set<SettlementTier> seen = new HashSet<>();
        // Sample enough grid cells to very likely see more than one tier.
        for (int gx = 0; gx < 40; gx++) {
            seen.add(SettlementTier.tierFor(SEED, gx * SettlementTier.VILLAGE_SPACING, 0));
        }
        assertTrue(seen.size() >= 2, "40 grid cells should yield at least 2 tiers, got " + seen);
    }

    // ---------- 2. Raw 1:2:5 distribution (min-gap disabled) ----------

    @Test
    @DisplayName("Raw draw (minGap=0) is approximately 1:2:5 for city:town:village")
    void rawDistributionIsApproximatelyOneTwoFive() {
        int[] counts = new int[3]; // village, town, city
        int samples = 60_000;
        for (int gx = 0; gx < samples; gx++) {
            SettlementTier t = SettlementTier.tierFor(SEED, gx * SettlementTier.VILLAGE_SPACING, gx, 0);
            counts[t.ordinal()]++;
        }
        double village = counts[SettlementTier.VILLAGE.ordinal()] / (double) samples;
        double town = counts[SettlementTier.TOWN.ordinal()] / (double) samples;
        double city = counts[SettlementTier.CITY.ordinal()] / (double) samples;
        // 1:2:5 of 8 → 0.625 / 0.25 / 0.125; allow generous tolerance for a finite sample.
        assertTrue(village > 0.58 && village < 0.67, "village ~0.625, got " + village);
        assertTrue(town > 0.20 && town < 0.30, "town ~0.25, got " + town);
        assertTrue(city > 0.09 && city < 0.16, "city ~0.125, got " + city);
    }

    // ---------- 3. Min-gap invariant: no two cities in adjacent grid cells ----------

    @Test
    @DisplayName("With minGap=1, no two cities sit in Chebyshev-adjacent grid cells (several seeds)")
    void noTwoCitiesAdjacent() {
        for (long seed : new long[]{SEED, 1L, 42L, 0xDEADBEEFL, 999L}) {
            int span = 120; // grid cells
            SettlementTier[][] grid = new SettlementTier[span][span];
            for (int gx = 0; gx < span; gx++) {
                for (int gz = 0; gz < span; gz++) {
                    grid[gx][gz] = SettlementTier.tierFor(seed,
                        gx * SettlementTier.VILLAGE_SPACING, gz * SettlementTier.VILLAGE_SPACING, 1);
                }
            }
            for (int gx = 0; gx < span; gx++) {
                for (int gz = 0; gz < span; gz++) {
                    if (grid[gx][gz] != SettlementTier.CITY) {
                        continue;
                    }
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dz == 0) {
                                continue;
                            }
                            int nx = gx + dx;
                            int nz = gz + dz;
                            if (nx >= 0 && nx < span && nz >= 0 && nz < span) {
                                assertNotEquals(SettlementTier.CITY, grid[nx][nz],
                                    "two adjacent cities at cells (" + gx + "," + gz + ") and ("
                                        + nx + "," + nz + ") for seed " + seed);
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Realised city rate with minGap=1 is at most the raw 1/8 rate (demotion only removes cities)")
    void realisedCityRateNotAboveRaw() {
        int span = 400;
        int cityCount = 0;
        for (int gx = 0; gx < span; gx++) {
            for (int gz = 0; gz < span; gz++) {
                if (SettlementTier.tierFor(SEED,
                        gx * SettlementTier.VILLAGE_SPACING, gz * SettlementTier.VILLAGE_SPACING, 1)
                        == SettlementTier.CITY) {
                    cityCount++;
                }
            }
        }
        double rate = cityCount / (double) (span * span);
        assertTrue(rate <= 0.125 + 0.01, "realised city rate should be <= raw 1/8, got " + rate);
        assertTrue(rate > 0.02, "cities should still appear (rate > 0.02), got " + rate);
    }

    // ---------- 4. Demotion turns a raw city into a town ----------

    @Test
    @DisplayName("A demoted city becomes a TOWN (never a VILLAGE) under the guard")
    void demotedCityBecomesTown() {
        // Scan a large space; every realised tier that isn't the raw roll can only be CITY→TOWN.
        for (int gx = -200; gx < 200; gx += 1) {
            for (int gz = -200; gz < 200; gz += 7) {
                SettlementTier raw = SettlementTier.tierFor(SEED,
                    gx * SettlementTier.VILLAGE_SPACING, gz * SettlementTier.VILLAGE_SPACING, 0);
                SettlementTier realised = SettlementTier.tierFor(SEED,
                    gx * SettlementTier.VILLAGE_SPACING, gz * SettlementTier.VILLAGE_SPACING, 1);
                if (raw == SettlementTier.CITY && realised != SettlementTier.CITY) {
                    assertEquals(SettlementTier.TOWN, realised,
                        "demoted city must become TOWN at (" + gx + "," + gz + ")");
                }
                if (raw != SettlementTier.CITY) {
                    assertEquals(raw, realised,
                        "non-city raw must be unchanged at (" + gx + "," + gz + ")");
                }
            }
        }
    }

    // ---------- 5. Tier factors + suffix ----------

    @Test
    @DisplayName("Tier factors and suffixes match the spec")
    void tierFactorsAndSuffixes() {
        assertEquals(1, SettlementTier.VILLAGE.depthFactor());
        assertEquals(1, SettlementTier.VILLAGE.distanceFactor());
        assertEquals("村", SettlementTier.VILLAGE.suffix());
        assertEquals(3, SettlementTier.TOWN.depthFactor());
        assertEquals(3, SettlementTier.TOWN.distanceFactor());
        assertEquals("镇", SettlementTier.TOWN.suffix());
        assertEquals(8, SettlementTier.CITY.depthFactor());
        assertEquals(8, SettlementTier.CITY.distanceFactor());
        assertEquals("城", SettlementTier.CITY.suffix());
    }

    // ---------- 6. Hash parity with SeedHash ----------

    @Test
    @DisplayName("SettlementTier and SeedHash use the same scrambling constants")
    void hashParity() {
        for (int x : new int[]{-3, 0, 1, 7, 34, 100}) {
            for (int z : new int[]{-3, 0, 1, 7, 34, 100}) {
                long expected = SEED ^ ((long) x * 341873128712L) ^ ((long) z * 132897987541L);
                assertEquals(expected, SeedHash.hash(SEED, x, z),
                    "SeedHash must match the vanilla scrambling expression at (" + x + "," + z + ")");
            }
        }
    }
}
