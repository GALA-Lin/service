package com.unlimited.sports.globox.model.venue.enums;

import com.unlimited.sports.globox.model.venue.vo.VenueDictItem;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 距离筛选枚举
 * 用于在搜索时根据距离进行筛选
 */
@Getter
@AllArgsConstructor
public enum DistanceFilter {
    ONE_KM(1, "1km内", 1.0),
    THREE_KM(3, "3km内", 3.0),
    TEN_KM(10, "10km内", 10.0);

    private final int value;
    private final String description;
    private final Double maxDistance;

    public static DistanceFilter fromValue(int value) {
        return Arrays.stream(values())
                .filter(filter -> filter.value == value)
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取所有距离筛选的字典项列表
     *
     * @return VenueDictItem列表
     */
    public static List<VenueDictItem> getDictItems() {
        return Arrays.stream(values())
                .map(filter -> VenueDictItem.builder()
                        .value(filter.getValue())
                        .description(filter.getDescription())
                        .build())
                .collect(Collectors.toList());
    }
}
