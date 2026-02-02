package com.unlimited.sports.globox.model.venue.enums;

import com.unlimited.sports.globox.model.venue.vo.VenueDictItem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public enum CourtType {
    INDOOR(1, "室内"),
    OUTDOOR(2, "室外"),
    SEMI_COVERED(3, "风雨场"),
    ;

    private final int value;
    private final String description;

    CourtType(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static CourtType fromValue(int value) {
        return Arrays.stream(values())
                .filter(type -> type.value == value)
                .findFirst()
                .orElse(null);
    }

    /**
     * 批量转换code列表为描述列表
     *
     * @param courtTypeCodes 球场类型code列表
     * @return 描述列表
     */
    public static List<String> getDescriptionsByValues(List<Integer> courtTypeCodes) {
        if (courtTypeCodes == null || courtTypeCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return courtTypeCodes.stream()
                .map(CourtType::fromValue)
                .filter(type -> type != null)
                .map(CourtType::getDescription)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有球场类型的字典项列表
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
