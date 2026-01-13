package com.unlimited.sports.globox.venue.adapter.dto.aitennis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 爱网球场地时间槽事件
 */
@Data
public class AitennisCourtEvent {

    /**
     * 开始时间（格式：0700）
     */
    @JsonProperty("start_time")
    private String startTime;

    /**
     * 结束时间（格式：0800）
     */
    @JsonProperty("end_time")
    private String endTime;

    /**
     * 场地ID
     */
    @JsonProperty("court_id")
    private String courtId;

    /**
     * 场地名称
     */
    @JsonProperty("court_name")
    private String courtName;

    /**
     * 类型：rent_price（可租赁）、lock_court（已锁场）
     */
    private String type;

    /**
     * 时间槽项目列表
     */
    private List<AitennisEventItem> items;
}
