package com.livingvillages.core.villagegen;

import com.livingvillages.core.config.ModConfig;
import com.livingvillages.core.data.Vec3i;
import com.livingvillages.core.data.VillageRecord;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class KCenterGeneratorTest {

    private static final ModConfig CFG = ModConfig.defaults();

    @Test
    void emptyPositions_returnsEmpty() {
        assertTrue(KCenterGenerator.generateClusteredVillages(42, List.of(), CFG).isEmpty());
    }

    @Test
    void preservesAllVillages() {
        List<Vec3i> vanilla = generateGridPositions(70); // 70 villages, K≈10
        List<VillageRecord> result = KCenterGenerator.generateClusteredVillages(42, vanilla, CFG);
        // Should have K center villages + redirected satellites
        assertTrue(result.size() >= vanilla.size(),
                "Should have at least as many villages as vanilla: " + result.size() + " >= " + vanilla.size());
    }

    @Test
    void allIdsUnique() {
        List<Vec3i> vanilla = generateGridPositions(50);
        List<VillageRecord> result = KCenterGenerator.generateClusteredVillages(42, vanilla, CFG);
        Set<UUID> ids = new HashSet<>();
        for (VillageRecord v : result) assertTrue(ids.add(v.id()), "Duplicate UUID: " + v.id());
    }

    @Test
    void deterministic() {
        List<Vec3i> vanilla = generateGridPositions(30);
        List<VillageRecord> a = KCenterGenerator.generateClusteredVillages(42, vanilla, CFG);
        List<VillageRecord> b = KCenterGenerator.generateClusteredVillages(42, vanilla, CFG);
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) assertEquals(a.get(i).id(), b.get(i).id());
    }

    @Test
    void defaultRecordValues() {
        List<Vec3i> vanilla = List.of(new Vec3i(0, 0, 0));
        List<VillageRecord> result = KCenterGenerator.generateClusteredVillages(1, vanilla, CFG);
        for (VillageRecord v : result) {
            assertEquals("unresolved", v.biomeCategory());
            assertEquals(0, v.bedCount());
            assertFalse(v.placed());
        }
    }

    private static List<Vec3i> generateGridPositions(int count) {
        List<Vec3i> list = new ArrayList<>();
        int cols = (int) Math.ceil(Math.sqrt(count));
        for (int i = 0; i < count; i++) {
            int x = (i % cols) * 544 + 272;  // 34 chunks spacing
            int z = (i / cols) * 544 + 272;
            list.add(new Vec3i(x, 0, z));
        }
        return list;
    }
}
