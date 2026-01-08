package com.unlimited.sports.globox.model.venue.dto;

import lombok.Data;

/**
 * 获取订单列表查询参数
 */
@Data
public class GetOrderListDto {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单状态：null-全部，1-待确认(PENDING)，2-已确认(CONFIRMED)，3-已完成(COMPLETED)，4-已取消(CANCELLED)
     */
    private Integer orderStatus;

    /**
     * 页码（从1开始）
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
