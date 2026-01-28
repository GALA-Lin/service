package com.unlimited.sports.globox.model.search.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SearchDocTypeEnum {

    VENUE("VENUE","VENUE_"),
    COACH("COACH","COACH_"),
    USER("USER","USER_"),
    NOTE("NOTE","NOTE_"),
    RALLY("RALLY","RALLY_"),
    ;


    private final String value;

    private final String idPrefix;
}
