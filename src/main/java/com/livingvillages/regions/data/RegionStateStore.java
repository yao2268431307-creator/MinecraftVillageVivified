package com.livingvillages.regions.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Persists the set of chunk keys for villages that have already been processed
 * by the LivingVillages cluster mod, so villages are not registered with
 * RoadWeaver or decorated twice (e.g. after a world reload).
 *
 * <p>Stored as a {@link SavedData} entry named {@value #DATA_NAME} in the
 * overworld's {@code data/} directory. The processed-chunk set is serialized
 * to a single {@code long[]} via {@link CompoundTag#putLongArray(String, long[])}
 * — the compact and idiomatic choice for 1.20.1, which has no
 * {@code HolderLookup.Provider} on {@link SavedData#save(CompoundTag)}.</p>
 *
 * <p>Chunk keys follow {@link net.minecraft.world.level.ChunkPos#toLong(long, long)}
 * semantics: {@code (chunkX & 0xFFFFFFFFL) | ((long) chunkZ << 32)}.</p>
 */
public class RegionStateStore extends SavedData {

    private static final String DATA_NAME = "livingvillages_regions";
    private static final String KEY_PROCESSED = "processedVillages";

    private final Set<Long> processedVillages = new HashSet<>();

    public RegionStateStore() {
    }

    /**
     * Load (or create) the {@link RegionStateStore} for the given level.
     *
     * @param level the server level the store is attached to
     * @return a non-null store; a fresh empty one is created on first access
     */
    public static RegionStateStore load(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            RegionStateStore::fromNbt,
            RegionStateStore::new,
            DATA_NAME);
    }

    private static RegionStateStore fromNbt(CompoundTag tag) {
        RegionStateStore store = new RegionStateStore();
        for (long key : tag.getLongArray(KEY_PROCESSED)) {
            store.processedVillages.add(key);
        }
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        long[] arr = new long[processedVillages.size()];
        int i = 0;
        for (long key : processedVillages) {
            arr[i++] = key;
        }
        tag.putLongArray(KEY_PROCESSED, arr);
        return tag;
    }

    /**
     * Whether the village at the given chunk has already been processed.
     *
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return {@code true} if {@link #markProcessed(int, int)} has been called
     *         for this chunk since the store was loaded/created
     */
    public boolean isProcessed(int chunkX, int chunkZ) {
        return processedVillages.contains(chunkKey(chunkX, chunkZ));
    }

    /**
     * Mark the village at the given chunk as processed and flag the store
     * as dirty so it will be persisted on the next save pass.
     *
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     */
    public void markProcessed(int chunkX, int chunkZ) {
        if (processedVillages.add(chunkKey(chunkX, chunkZ))) {
            setDirty();
        }
    }

    /**
     * @return the number of chunks currently recorded as processed
     */
    public int size() {
        return processedVillages.size();
    }

    /**
     * @return an unmodifiable view of the processed-chunk key set, for
     *         shipping to clients (the settlement set)
     */
    public Set<Long> processedChunkKeys() {
        return Collections.unmodifiableSet(processedVillages);
    }

    /**
     * Decode the chunk X coordinate from a 64-bit chunk key.
     *
     * @param key a chunk key from {@link #chunkKey(int, int)}
     * @return the chunk X coordinate
     */
    public static int chunkX(long key) {
        return (int) (key & 0xFFFFFFFFL);
    }

    /**
     * Decode the chunk Z coordinate from a 64-bit chunk key.
     *
     * @param key a chunk key from {@link #chunkKey(int, int)}
     * @return the chunk Z coordinate
     */
    public static int chunkZ(long key) {
        return (int) (key >>> 32);
    }

    /**
     * Compute a 64-bit chunk key, matching {@link
     * net.minecraft.world.level.ChunkPos#toLong(long, long)}:
     * {@code (chunkX & 0xFFFFFFFFL) | ((long) chunkZ << 32)}.
     *
     * <p>Package-private so it can be unit-tested without the MC runtime.</p>
     *
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return the 64-bit chunk key
     */
    static long chunkKey(int chunkX, int chunkZ) {
        return (long) chunkX & 0xFFFFFFFFL | ((long) chunkZ << 32);
    }
}
