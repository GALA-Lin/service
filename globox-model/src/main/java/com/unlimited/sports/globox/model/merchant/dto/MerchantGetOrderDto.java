package com.unlimited.sports.globox.model.merchant.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @since 2026/1/26 16:24
 *
 */
@Data
public class MerchantGetOrderDto implements Serializable {


    /**
     * 场地 ID
     */
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

    /**
     * 订单号
     */
    private Long orderNo;
}

