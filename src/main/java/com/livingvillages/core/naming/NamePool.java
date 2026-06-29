package com.livingvillages.core.naming;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class NamePool {
    private final Map<BiomeMood, List<String>> prefixes;
    private final List<String> smallSuffixes;
    private final List<String> centerSuffixes;

    public NamePool() {
        EnumMap<BiomeMood, List<String>> map = new EnumMap<BiomeMood, List<String>>(BiomeMood.class);
        map.put(BiomeMood.PLAINS, Arrays.asList("晴原", "麦浪", "向阳", "丰谷", "金穗", "和风", "长云"));
        map.put(BiomeMood.FOREST, Arrays.asList("翠林", "密叶", "深根", "苍木", "幽林", "青萝"));
        map.put(BiomeMood.SNOWY, Arrays.asList("望雪", "至冬", "凌霜", "素雪", "寒山", "凝冰", "朔风"));
        map.put(BiomeMood.DESERT, Arrays.asList("落日", "沙海", "孤烟", "远尘", "赤岩", "热风"));
        map.put(BiomeMood.MOUNTAIN, Arrays.asList("云顶", "凌霄", "望岳", "临渊", "叠石", "高岭"));
        map.put(BiomeMood.OCEAN, Arrays.asList("望海", "听涛", "临潮", "碧波", "白沙"));
        map.put(BiomeMood.RIVER, Arrays.asList("望川", "临流", "渡口", "水畔"));
        prefixes = Collections.unmodifiableMap(map);
        smallSuffixes = Arrays.asList("村", "庄", "集", "里");
        centerSuffixes = Arrays.asList("镇", "邑", "驿");
    }

    public List<String> prefixes(BiomeMood mood) {
        List<String> result = prefixes.get(mood);
        return result == null ? prefixes.get(BiomeMood.PLAINS) : result;
    }

    public List<String> smallSuffixes() {
        return smallSuffixes;
    }

    public List<String> centerSuffixes() {
        return centerSuffixes;
    }
}

