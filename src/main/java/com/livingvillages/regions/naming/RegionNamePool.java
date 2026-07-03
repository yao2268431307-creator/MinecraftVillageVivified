package com.livingvillages.regions.naming;

import com.livingvillages.regions.biome.RegionType;
import java.util.List;
import java.util.Map;

/**
 * Static word pools for region and village naming.
 *
 * <p>Each {@link RegionType} maps to a {@link RegionWords} record containing
 * a list of 2-character prefixes, a single midfix ("之"), and a list of
 * suffixes. The pools are consulted by {@code RegionNameGenerator} (M4) to
 * build deterministic names of the form {@code <prefix>之<suffix>} (e.g.
 * "风沙之地"), and by {@code villageName} to build {@code <prefix><village
 * suffix>} (e.g. "风沙村").</p>
 *
 * <p>This class is a pure data holder with no MC runtime dependency, so it
 * can be unit-tested directly.</p>
 */
public final class RegionNamePool {

    /**
     * Word pool for a single region type.
     *
     * @param prefixes 2-character prefixes (≥8 entries for specific biomes,
     *                 4 entries for {@link RegionType#OTHER})
     * @param midfix   the joining character, typically "之"
     * @param suffixes region suffixes (≥3 entries)
     */
    public record RegionWords(
        List<String> prefixes,
        String midfix,
        List<String> suffixes
    ) {
    }

    /**
     * All region word pools keyed by {@link RegionType}.
     */
    public static final Map<RegionType, RegionWords> POOLS = Map.of(
        RegionType.DESERT, new RegionWords(
            List.of("风沙", "烈日", "炽阳", "荒漠", "金沙", "烈焰", "旱地", "驼铃", "戈壁", "灼热"),
            "之",
            List.of("地", "领", "境", "域")
        ),
        RegionType.SNOWY, new RegionWords(
            List.of("霜白", "凛冬", "雪原", "冰封", "银白", "寒月", "永冻", "皓雪", "玉霜", "玄冬"),
            "之",
            List.of("地", "领", "境", "域")
        ),
        RegionType.PLAINS, new RegionWords(
            List.of("晴原", "麦浪", "暖阳", "丰饶", "金穗", "和风", "翠野", "广袤", "明光", "暖野"),
            "之",
            List.of("地", "领", "境", "原")
        ),
        RegionType.TAIGA, new RegionWords(
            List.of("松涛", "雪岭", "寒林", "霜枝", "苍针", "北境", "冷杉", "玄松"),
            "之",
            List.of("林", "领", "境", "域")
        ),
        RegionType.SAVANNA, new RegionWords(
            List.of("赤原", "日炎", "旱风", "金草", "烈阳", "稀树", "暖原", "暮野"),
            "之",
            List.of("地", "领", "原", "域")
        ),
        RegionType.SWAMP, new RegionWords(
            List.of("雾沼", "幽影", "深泽", "暗藤", "碧水", "沉渊", "迷雾", "苔泽"),
            "之",
            List.of("地", "泽", "境", "域")
        ),
        RegionType.JUNGLE, new RegionWords(
            List.of("翠林", "密莽", "雨荫", "繁茂", "苍翠", "深丛", "碧藤", "郁林"),
            "之",
            List.of("林", "野", "境", "域")
        ),
        RegionType.MOUNTAIN, new RegionWords(
            List.of("云峰", "霜岭", "峻岳", "苍崖", "孤峰", "雪顶", "玄岩", "高岭"),
            "之",
            List.of("山", "岭", "峰", "岳")
        ),
        RegionType.FOREST, new RegionWords(
            List.of("幽林", "深翠", "暗木", "古树", "苍林", "碧荫", "密枝", "老林"),
            "之",
            List.of("林", "野", "境", "域")
        ),
        RegionType.OTHER, new RegionWords(
            List.of("异境", "未知", "迷地", "荒原"),
            "之",
            List.of("地", "域", "境")
        )
    );

    /**
     * Village name suffixes shared across all region types.
     */
    public static final List<String> VILLAGE_SUFFIXES = List.of("村", "镇", "集", "庄", "堡");

    private RegionNamePool() {
        // utility class, no instances
    }

    /**
     * Look up the word pool for a region type.
     *
     * @param type the region type
     * @return the corresponding word pool, or {@code null} if {@code type}
     *         is {@code null} or unknown
     */
    public static RegionWords forType(RegionType type) {
        if (type == null) {
            return null;
        }
        return POOLS.get(type);
    }
}
