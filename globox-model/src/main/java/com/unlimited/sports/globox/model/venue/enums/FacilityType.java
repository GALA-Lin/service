package com.unlimited.sports.globox.model.venue.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import com.unlimited.sports.globox.model.venue.vo.VenueDictItem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 便利设施枚举
 * 用于在搜索时根据设施进行筛选
 * value为设施ID，用于前端传参和数据库查询
 */
@Getter
@AllArgsConstructor
public enum FacilityType {
    PARKING(1, "停车场"),
    TOILET(2, "卫生间"),
    CHANGING_ROOM(3, "更衣室"),
    REST_AREA(4, "休息区"),
    WIFI(5, "Wifi"),
    EQUIPMENT_RENTAL(6, "器材租赁"),
    STRING_SERVICE(7, "穿线服务"),
    WATER_DISPENSER(8, "饮水机"),
    SHOWER_ROOM(9, "淋浴室"),
    FITNESS_CENTER(10, "体能中心"),
    SERVING_MACHINE(11, "发球机"),
    PICKLEBALL(12, "匹克球")

    ;

    private final int value;
    private final String description;

    public static FacilityType fromValue(int value) {
        return Arrays.stream(values())
                .filter(type -> type.value == value)
                .findFirst()
                .orElse(null);
    }

    /**
     * 批量转换code列表为描述列表
     *
     * @param facilityCodes 设施code列表
     * @return 描述列表
     */
    public static List<String> getDescriptionsByValues(List<Integer> facilityCodes) {
        if (facilityCodes == null || facilityCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return facilityCodes.stream()
                .map(FacilityType::fromValue)
                .filter(type -> type != null)
                .map(FacilityType::getDescription)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有设施的字典项列表
     *
     * @return VenueDictItem列表
     */
    public static List<VenueDictItem> getDictItems() {
        return Arrays.stream(values())
                .map(facility -> VenueDictItem.builder()
                        .value(facility.getValue())
                        .description(facility.getDescription())
                        .build())
                .collect(Collectors.toList());
    }
}
