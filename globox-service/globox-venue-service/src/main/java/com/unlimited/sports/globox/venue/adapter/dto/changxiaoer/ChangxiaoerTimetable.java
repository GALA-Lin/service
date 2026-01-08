package com.unlimited.sports.globox.venue.adapter.dto.changxiaoer;

import lombok.Data;

/**
 * 场小二时间槽信息
 */
@Data
public class ChangxiaoerTimetable {

    /**
     * 开始时间（格式：HH:mm）
     */
    private String startTime;

    /**
     * 结束时间（格式：HH:mm）
     */
    private String endTime;

    /**
     * 是否可预订
     */
    private Boolean bookable;

    /**
     * 价格
     */
    private Double price;
}
