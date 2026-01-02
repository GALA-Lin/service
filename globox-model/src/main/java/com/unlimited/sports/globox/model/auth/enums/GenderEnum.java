package com.unlimited.sports.globox.model.auth.enums;


import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @since 2026/1/2 14:35
 *
 */
@Getter
@AllArgsConstructor
public enum GenderEnum {
    FEMALE(0, "FEMALE", "女"),
    MALE(1, "MALE", "男"),
    OTHER(2, "OTHER", "其他");

    private final Integer code;

    @EnumValue
    @JsonValue
    private final String value;

    private final String description;

    public static GenderEnum fromValue(String value) {
        if (value == null) return null;

        for (GenderEnum gender : values()) {
            if (gender.value.equalsIgnoreCase(value)) {
                return gender;
            }
        }
        return null;
    }

    public static GenderEnum fromCode(Integer code) {
        if (code == null) return null;

        for (GenderEnum gender : values()) {
            if (gender.code.equals(code)) {
                return gender;
            }
        }
        return null;
    }
}
