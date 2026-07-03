package com.livingvillages.regions.data;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link RegionStateStore}.
 *
 * <p>These tests exercise the pure logic of the store — the
 * {@code isProcessed} / {@code markProcessed} contract, the chunk-key
 * derivation, and the NBT round-trip — without booting the Minecraft
 * runtime. A {@link CompoundTag} is constructable directly in the test
 * classpath (loom provides the MC jar), so {@link RegionStateStore#save(CompoundTag)}
 * and {@link RegionStateStore#fromNbt(CompoundTag)} can be called explicitly
 * rather than going through {@link RegionStateStore#load(net.minecraft.server.level.ServerLevel)}.</p>
 */
class RegionStateStoreTest {

    @Test
    @DisplayName("Fresh store reports no chunks as processed")
    void freshStoreHasNothingProcessed() {
        RegionStateStore store = new RegionStateStore();
        assertFalse(store.isProcessed(0, 0));
        assertFalse(store.isProcessed(10, -5));
        assertEquals(0, store.size());
    }

    @Test
    @DisplayName("markProcessed makes isProcessed return true for that chunk")
    void markProcessedMakesChunkProcessed() {
        RegionStateStore store = new RegionStateStore();
        store.markProcessed(3, 7);
        assertTrue(store.isProcessed(3, 7));
        assertEquals(1, store.size());
    }

    @Test
    @DisplayName("Different chunks do not collide (positive coords)")
    void differentChunksDoNotCollidePositive() {
        RegionStateStore store = new RegionStateStore();
        store.markProcessed(0, 0);
        assertTrue(store.isProcessed(0, 0));
        assertFalse(store.isProcessed(0, 1));
        assertFalse(store.isProcessed(1, 0));
        assertFalse(store.isProcessed(1, 1));
    }

    @Test
    @DisplayName("Different chunks do not collide (negative coords)")
    void differentChunksDoNotCollideNegative() {
        RegionStateStore store = new RegionStateStore();
        store.markProcessed(-1, -1);
        assertTrue(store.isProcessed(-1, -1));
        assertFalse(store.isProcessed(-1, 0));
        assertFalse(store.isProcessed(0, -1));
    }

    @Test
    @DisplayName("chunkKey matches ChunkPos.toLong(x, z) bit layout")
    void chunkKeyMatchesChunkPosToLongLayout() {
        // ChunkPos.toLong(x, z) = (x & 0xFFFFFFFFL) | ((long) z << 32)
        // Verify by extracting x and z back out of the key with the same
        // masks ChunkPos uses.
        for (int x : new int[] {0, 1, -1, 100, -100, 12345, -12345, 30000, -30000}) {
            for (int z : new int[] {0, 1, -1, 50, -50, 18765, -18765}) {
                long key = RegionStateStore.chunkKey(x, z);
                int extractedX = (int) (key & 0xFFFFFFFFL);
                int extractedZ = (int) (key >>> 32);
                assertEquals(x, extractedX, "x mismatch for (" + x + "," + z + ")");
                assertEquals(z, extractedZ, "z mismatch for (" + x + "," + z + ")");
            }
        }
    }

    @Test
    @DisplayName("chunkKey is distinct for distinct (x, z) pairs")
    void chunkKeyDistinctForDistinctPairs() {
        assertNotEquals(RegionStateStore.chunkKey(0, 1),
            RegionStateStore.chunkKey(1, 0));
        assertNotEquals(RegionStateStore.chunkKey(-1, 0),
            RegionStateStore.chunkKey(0, -1));
        // (x=0,z=0) and (x=0,z=0) are equal (sanity)
        assertEquals(RegionStateStore.chunkKey(0, 0),
            RegionStateStore.chunkKey(0, 0));
    }

    @Test
    @DisplayName("markProcessed is idempotent (size stays 1, no double-count)")
    void markProcessedIsIdempotent() {
        RegionStateStore store = new RegionStateStore();
        store.markProcessed(2, 2);
        store.markProcessed(2, 2);
        store.markProcessed(2, 2);
        assertEquals(1, store.size());
        assertTrue(store.isProcessed(2, 2));
    }

    @Test
    @DisplayName("size() counts distinct processed chunks")
    void sizeCountsDistinctChunks() {
        RegionStateStore store = new RegionStateStore();
        store.markProcessed(1, 1);
        store.markProcessed(2, 2);
        store.markProcessed(3, 3);
        store.markProcessed(1, 1); // dup
        assertEquals(3, store.size());
    }

    @Test
    @DisplayName("NBT round-trip preserves the processed set (positive coords)")
    void nbtRoundTripPreservesPositive() {
        RegionStateStore original = new RegionStateStore();
        original.markProcessed(10, 20);
        original.markProcessed(30, 40);

        CompoundTag tag = original.save(new CompoundTag());

        // Simulate deserialize the way DimensionDataStorage would.
        RegionStateStore restored = RegionStateStoreTest.loadFromTag(tag);

        assertTrue(restored.isProcessed(10, 20));
        assertTrue(restored.isProcessed(30, 40));
        assertFalse(restored.isProcessed(10, 40));
        assertEquals(2, restored.size());
    }

    @Test
    @DisplayName("NBT round-trip preserves the processed set (negative coords)")
    void nbtRoundTripPreservesNegative() {
        RegionStateStore original = new RegionStateStore();
        original.markProcessed(-5, -10);
        original.markProcessed(-100, -200);

        CompoundTag tag = original.save(new CompoundTag());
        RegionStateStore restored = RegionStateStoreTest.loadFromTag(tag);

        assertTrue(restored.isProcessed(-5, -10));
        assertTrue(restored.isProcessed(-100, -200));
        assertFalse(restored.isProcessed(5, 10));
        assertEquals(2, restored.size());
    }

    @Test
    @DisplayName("NBT round-trip of an empty store yields an empty store")
    void nbtRoundTripEmpty() {
        RegionStateStore original = new RegionStateStore();
        CompoundTag tag = original.save(new CompoundTag());
        RegionStateStore restored = RegionStateStoreTest.loadFromTag(tag);

        assertEquals(0, restored.size());
        assertFalse(restored.isProcessed(0, 0));
    }

    @Test
    @DisplayName("NBT round-trip of a larger set is exact")
    void nbtRoundTripExactForLargerSet() {
        RegionStateStore original = new RegionStateStore();
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                original.markProcessed(x, z);
            }
        }
        // Sanity: 7x7 = 49 distinct chunks
        assertEquals(49, original.size());

        CompoundTag tag = original.save(new CompoundTag());
        RegionStateStore restored = RegionStateStoreTest.loadFromTag(tag);

        assertEquals(49, restored.size());
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                assertTrue(restored.isProcessed(x, z),
                    "missing (" + x + "," + z + ") after round-trip");
            }
        }
        // A few chunks just outside the range should still be false.
        assertFalse(restored.isProcessed(4, 0));
        assertFalse(restored.isProcessed(0, 4));
        assertFalse(restored.isProcessed(-4, -4));
    }

    @Test
    @DisplayName("Saved tag uses the documented key name")
    void savedTagUsesDocumentedKeyName() {
        RegionStateStore store = new RegionStateStore();
        store.markProcessed(1, 2);
        CompoundTag tag = store.save(new CompoundTag());
        assertTrue(tag.contains("processedVillages"),
            "saved tag should contain the 'processedVillages' key");
    }

    /**
     * Deserialize a store from a tag, mirroring the private
     * {@link RegionStateStore#fromNbt(CompoundTag)} that {@code DimensionDataStorage}
     * would invoke via the function passed to {@code computeIfAbsent}.
     *
     * <p>Reflection is used because {@code fromNbt} is private; the test
     * otherwise could not exercise the exact deserialize path used in
     * production without spinning up a {@code ServerLevel}.</p>
     */
    private static RegionStateStore loadFromTag(CompoundTag tag) {
        try {
            var method = RegionStateStore.class.getDeclaredMethod(
                "fromNbt", CompoundTag.class);
            method.setAccessible(true);
            return (RegionStateStore) method.invoke(null, tag);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("fromNbt not accessible", e);
        }
    }
}
