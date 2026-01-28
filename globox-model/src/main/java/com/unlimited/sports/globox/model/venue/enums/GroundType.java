package com.unlimited.sports.globox.model.venue.enums;

import com.unlimited.sports.globox.model.venue.vo.VenueDictItem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public enum GroundType {
    HARD(1, "硬地"),
    CLAY(2, "红土"),
    GRASS(3, "草地"),
    OTHER(4, "其他");

    private final int value;
    private final String description;

    GroundType(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static GroundType fromValue(int value) {
        return Arrays.stream(values())
                .filter(type -> type.value == value)
                .findFirst()
                .orElse(null);
    }

    /**
     * 批量转换code列表为描述列表
     *
     * @param groundTypeCodes 地面类型code列表
     * @return 描述列表
     */
    public static List<String> getDescriptionsByValues(List<Integer> groundTypeCodes) {
        if (groundTypeCodes == null || groundTypeCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return groundTypeCodes.stream()
                .map(GroundType::fromValue)
                .filter(type -> type != null)
                .map(GroundType::getDescription)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有地面类型的字典项列表
     *
     * @return VenueDictItem列表
     */
    public static List<VenueDictItem> getDictItems() {
        return Arrays.stream(values())
                .map(type -> VenueDictItem.builder()
                        .value(type.getValue())
                        .description(type.getDescription())
                        .build())
                .collect(Collectors.toList());
    }
}
