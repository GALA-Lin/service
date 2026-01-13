package com.unlimited.sports.globox.venue.adapter.dto.aitennis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 爱网球事件数据
 */
@Data
public class AitennisEventData {

    /**
     * 价格
     */
    private String price;

    /**
     * 原价
     */
    @JsonProperty("origin_price")
    private String originPrice;

    /**
     * 锁场备注
     */
    private String remark;
}
