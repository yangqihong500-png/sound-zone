package com.soundzone.track.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 曲目标签目录：曲目元数据保留语言/年代/风格/场景/情绪五类；建域过滤只开放语言/年代/风格/情绪。
 *
 * <p>【假设】Demo 阶段为静态目录：打标结果落在 Track.tags（扁平集合）， 类别仅用于建域时的分组展示与入参校验，过滤判定仍按标签值匹配。 正式版由自动打标模型输出
 * (category, value) 二元组（docs/03）。
 */
public final class TagCatalog {

    private TagCatalog() {}

    public static final Map<String, List<String>> CATALOG =
            Map.of(
                    "语言", List.of("华语", "粤语", "日语", "韩语", "英语", "纯音乐"),
                    "年代", List.of("70s", "80s", "90s", "00s", "10s", "20s"),
                    "风格",
                            List.of(
                                    "流行",
                                    "摇滚",
                                    "电子",
                                    "说唱",
                                    "民谣",
                                    "爵士",
                                    "古典",
                                    "City Pop",
                                    "Lo-Fi",
                                    "抖音热曲"),
                    "场景", List.of("自习", "健身", "旅行", "通勤", "睡前", "工作", "手工", "深夜"),
                    "情绪", List.of("舒缓", "治愈", "亢奋", "忧郁", "情歌", "专注"));

    /** 场景由域的必选 scene 字段承担，不再作为 Music Filter 的可选维度。 */
    public static final Map<String, List<String>> FILTER_CATALOG = filterCatalog();

    /** 校验标签是否存在于目录（未知标签拒绝，保证过滤规则可判定） */
    public static boolean isKnown(String tag) {
        return CATALOG.values().stream().anyMatch(list -> list.contains(tag));
    }

    /** 新建过滤规则只能使用过滤目录中的标签。 */
    public static boolean isFilterable(String tag) {
        return FILTER_CATALOG.values().stream().anyMatch(list -> list.contains(tag));
    }

    /** 返回标签所属类别，供白名单执行“同类 OR、跨类 AND”。 */
    public static Optional<String> categoryOf(String tag) {
        return CATALOG.entrySet().stream()
                .filter(entry -> entry.getValue().contains(tag))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private static Map<String, List<String>> filterCatalog() {
        Map<String, List<String>> catalog = new LinkedHashMap<>();
        for (String category : List.of("语言", "年代", "风格", "情绪"))
            catalog.put(category, CATALOG.get(category));
        return Map.copyOf(catalog);
    }
}
