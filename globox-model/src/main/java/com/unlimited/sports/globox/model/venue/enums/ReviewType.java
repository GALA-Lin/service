package com.unlimited.sports.globox.model.venue.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReviewType {

    USER_COMMENT(1,"用户评论"),
    MERCHANT_REPLY(2,"商家回复"),
    ;


    private final int value;

    private final String description;

}
