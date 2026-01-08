package com.unlimited.sports.globox.model.venue.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum CourtStatus {
    CLOSED(0, "不开放"),
    OPEN(1, "开放");

    private final int value;
    private final String description;






    public static CourtStatus fromValue(int value) {
        return Arrays.stream(values())
                .filter(status -> status.value == value)
                .findFirst()
                .orElse(null);
    }
}
