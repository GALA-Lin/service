package com.unlimited.sports.globox.model.venue.entity.venues;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

import java.util.Arrays;

/**
 * 活动参与者状态枚举
 */
@Getter
public enum VenueActivityParticipantStatusEnum {
    /**
     * 有效报名
     */
    ACTIVE(0, "有效"),

    /**
     * 已取消
     */
    CANCELLED(1, "已取消");

    @EnumValue
    private final Integer value;
    private final String description;

    VenueActivityParticipantStatusEnum(Integer value, String description) {
        this.value = value;
        this.description = description;
    }

    public static VenueActivityParticipantStatusEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(VenueActivityParticipantStatusEnum.values())
                .filter(status -> status.value.equals(value))
                .findFirst()
                .orElse(null);
    }
}
