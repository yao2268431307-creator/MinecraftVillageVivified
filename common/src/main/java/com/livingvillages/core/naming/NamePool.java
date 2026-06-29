package com.livingvillages.core.naming;

import java.util.List;
import java.util.Map;

/**
 * Curated Chinese word pools for biome-based village naming.
 *
 * <p>Each biome category has a dedicated {@link BiomeWordPool} containing
 * prefixes, midfixes, and suffixes. Words are chosen to avoid negative
 * connotations when combined.</p>
 *
 * <p>Naming formula:</p>
 * <ul>
 *   <li>clusterName = prefix(2 chars) + midfix(1 char) + suffix(1 char) → 4 chars</li>
 *   <li>centerTownName = clusterName + "镇"</li>
 *   <li>satelliteName = prefix(2 chars) + suffix(1 char) + "村" → 4 chars</li>
 * </ul>
 */
public final class NamePool {

    private NamePool() {}

    /** A word pool for one biome category. */
    public record BiomeWordPool(
            List<String> prefixes,  // 2-character prefixes
            List<String> midfixes,  // 1-character midfixes
            List<String> suffixes   // 1-character suffixes
    ) {}

    /** Map from biome category key to its word pool. */
    public static final Map<String, BiomeWordPool> POOLS = Map.of(
            "plains", new BiomeWordPool(
                    List.of("晴", "麦", "风", "原", "野", "田", "云", "暖", "丰", "金",
                            "绿", "广", "平", "明", "和", "春", "秋", "望", "远", "新"),
                    List.of("原", "野", "田", "风", "云", "光", "和", "明", "丰", "平"),
                    List.of("原", "野", "风", "晴", "和", "明", "丰", "平", "光", "润")
            ),
            "desert", new BiomeWordPool(
                    List.of("金", "沙", "炎", "驼", "漠", "泉", "烈", "阳", "荒", "旱",
                            "焰", "赤", "烁", "煌", "烽", "热", "烟", "焦", "干", "戈"),
                    List.of("沙", "漠", "炎", "金", "烈", "荒", "旱", "烽", "烟", "驼"),
                    List.of("沙", "漠", "炎", "金", "泉", "烈", "旱", "烟", "戈", "阳")
            ),
            "taiga", new BiomeWordPool(
                    List.of("松", "雪", "桦", "冷", "涛", "岭", "霜", "寒", "冰", "杉",
                            "峻", "森", "北", "冬", "云", "苍", "针", "雾", "白", "青"),
                    List.of("松", "岭", "桦", "雪", "针", "森", "寒", "霜", "冷", "杉"),
                    List.of("松", "岭", "雪", "寒", "涛", "霜", "冷", "森", "冰", "杉")
            ),
            "snowy", new BiomeWordPool(
                    List.of("冰", "霜", "白", "晶", "冬", "银", "寒", "素", "净", "莹",
                            "雪", "凌", "皓", "洁", "玉", "玄", "凝", "朔", "清", "冽"),
                    List.of("雪", "冰", "霜", "白", "冬", "寒", "银", "晶", "凝", "皓"),
                    List.of("雪", "冰", "霜", "白", "冬", "寒", "莹", "净", "皓", "凌")
            ),
            "savanna", new BiomeWordPool(
                    List.of("赤", "日", "狮", "金", "旱", "稀", "热", "烈", "荒", "原",
                            "阳", "焰", "火", "霞", "煌", "橙", "暮", "野", "风", "光"),
                    List.of("原", "野", "日", "金", "热", "赤", "旱", "荒", "焰", "风"),
                    List.of("原", "野", "日", "炎", "热", "荒", "旱", "金", "风", "霞")
            ),
            "swamp", new BiomeWordPool(
                    List.of("沼", "雾", "藤", "暗", "水", "影", "幽", "静", "深", "绿",
                            "湿", "梦", "碧", "沉", "渺", "烟", "泪", "苔", "渊", "涟"),
                    List.of("沼", "水", "雾", "暗", "影", "深", "藤", "幽", "绿", "沉"),
                    List.of("沼", "水", "影", "雾", "暗", "深", "绿", "静", "幽", "涟")
            ),
            "jungle", new BiomeWordPool(
                    List.of("密", "翠", "藤", "绿", "深", "莽", "繁", "茂", "湿", "雨",
                            "丛", "森", "碧", "浓", "荫", "野", "苍", "葱", "郁", "蓁"),
                    List.of("林", "莽", "深", "密", "翠", "丛", "藤", "绿", "繁", "森"),
                    List.of("林", "莽", "深", "藤", "绿", "密", "丛", "翠", "荫", "野")
            ),
            "other", new BiomeWordPool(
                    List.of("晴", "麦", "风", "原", "野", "田", "云", "暖", "丰", "金",
                            "绿", "广", "平", "明", "和", "春", "秋", "望", "远", "新"),
                    List.of("原", "野", "田", "风", "云", "光", "和", "明", "丰", "平"),
                    List.of("原", "野", "风", "晴", "和", "明", "丰", "平", "光", "润")
            )
    );

    /**
     * Get the word pool for a biome category, falling back to "other".
     */
    public static BiomeWordPool forCategory(String category) {
        return POOLS.getOrDefault(category, POOLS.get("other"));
    }
}
