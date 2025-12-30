package com.unlimited.sports.globox.model.merchant.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * @since  2025/12/22 09:38
 * 退款请求DTO
 */

@Data
@Builder
public class RefundRequestDto {

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 退款类型：1=全额退款，2=部分退款
     */
    private Integer refundType;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 退款原因
     */
    private String refundReason;

    /**
     * 取消的时段ID列表（部分退款时使用）
     */
    private List<Long> cancelledSlotIds;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商家ID
     */
    private Long merchantId;

    /**
     * 场馆ID
     */
    private Long venueId;

    /**
     * 扩展字段（预留）
     */
    private String extInfo;
}