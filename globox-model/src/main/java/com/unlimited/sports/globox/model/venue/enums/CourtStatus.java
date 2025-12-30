package com.unlimited.sports.globox.model.venue.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CourtStatus {
    CLOSED(0, "不开放"),
    OPEN(1, "开放");

    private final int value;
    private final String description;






    public static CourtStatus fromValue(int value) {
        for (CourtStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown CourtStatus value: " + value);
    }
}
