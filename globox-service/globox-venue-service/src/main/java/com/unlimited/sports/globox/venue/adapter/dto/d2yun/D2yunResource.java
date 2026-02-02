package com.unlimited.sports.globox.venue.adapter.dto.d2yun;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * D2yun资源（时间槽）信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class D2yunResource {

    /**
     * 资源ID（格式：spaceId_time）
     */
    private String id;

    /**
     * 场地名称
     */
    private String name;

    @JsonProperty("space_id")
    private Long spaceId;

    /**
     * 时间（分钟，从0点开始计算）
     */
    private Integer time;

    /**
     * 价格（分，例如7500表示75元）
     */
    private Integer worth;

    /**
     * 单位（元）
     */
    private String unit;

    /**
     * 状态（1:可用, 4:锁定, 5:占用等）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    @JsonProperty("resource_id")
    private Long resourceId;

    @JsonProperty("schedule_id")
    private Long scheduleId;

    @JsonProperty("order_id")
    private Long orderId;
}
