package com.unlimited.sports.globox.model.venue.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum VenueActivityStatusEnum {

    NORMAL(1,"正常"),
    CANCELLED(2,"取消"),
    ;

    private final Integer value;

    private final String desc;


}
