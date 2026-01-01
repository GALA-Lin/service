package com.unlimited.sports.globox.dubbo.order.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 商家模块获取订单分页 - 请求 DTO
 */
@Data
public class MerchantGetOrderPageRequestDto implements Serializable {

    /**
     * 商家 ID 列表
     * - 不能为空
     * - 至少包含一个商家
     */
    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    @NotNull(message = "场地ID不能为空")
    private Long venueId;

    /**
     * 页码（从 1 开始）
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于等于 1")
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小必须大于等于 1")
    private Integer pageSize = 10;
}
