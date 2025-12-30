package com.unlimited.sports.globox.model.merchant.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * @since 2025/12/29 17:44
 * 已预订时段信息
 */
@Data
@Builder
public class BookedSlotInfoVo {
    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 预订日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate bookingDate;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String userNickname;
}
