package com.unlimited.sports.globox.model.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.unlimited.sports.globox.model.base.BaseEntity;
import lombok.*;

/**
 * 订单活动绑定表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName(value ="order_activities")
@EqualsAndHashCode(callSuper = true)
public class OrderActivities extends BaseEntity {
    private Long orderNo;

    /**
     * 关联的活动 id
     */
    private Long activityId;

    /**
     * 活动类型 code
     */
    private String activityTypeCode;

    /**
     * 活动类型名称
     */
    private String activityTypeName;

}