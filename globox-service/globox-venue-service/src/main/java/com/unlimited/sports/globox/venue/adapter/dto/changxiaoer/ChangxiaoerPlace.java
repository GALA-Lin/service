package com.unlimited.sports.globox.venue.adapter.dto.changxiaoer;

import lombok.Data;
import java.util.List;

/**
 * 场小二场地信息
 */
@Data
public class ChangxiaoerPlace {

    /**
     * 场地ID
     */
    private Long placeId;

    /**
     * 场地名称
     */
    private String placeName;

    /**
     * 时间表（槽位列表）
     */
    private List<ChangxiaoerTimetable> timetables;
}
