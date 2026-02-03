package com.unlimited.sports.globox.model.venue.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public enum VenueThirdPartyConfigStatusEnum {
    SUSPENDED(0, "禁用"),
    NORMAL(1, "启用");

    private final int value;
    private final String description;



    public static VenueThirdPartyConfigStatusEnum fromValue(int value) {
        return Arrays.stream(values())
                .filter(status -> status.value == value)
                .findFirst()
                .orElse(null);
    }
}
