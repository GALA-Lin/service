package com.unlimited.sports.globox.model.search.enums;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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



    public static String buildSearchDocId(SearchDocTypeEnum searchType,Long id) {
        if(searchType == null || id == null) {
            log.error("[SearchDocTypeEnum.buildSearchDocId] 类型或id不能为空,id: {}",id);
            throw new GloboxApplicationException("[SearchDocTypeEnum.buildSearchDocId] 类型或id不能为空");
        }
        return searchType.getIdPrefix() + id;
    }
}
