package com.unlimited.sports.globox.dubbo.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 创建订单前查询价格 - 结果 DTO
 *
 * @author dk
 * @since 2025/12/22 17:52
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class PricingResultDto implements Serializable {

    /**
     * 涉及的 record 信息列表
     */
    private List<RecordQuote> recordQuote;

    /**
     * 订单级额外收费信息列表
     */
    private List<OrderLevelExtraQuote> orderLevelExtras;

    /**
     * VENUE 订单场馆归属：1=home平台，2=away平台
     */
    private Integer sourcePlatform;

    /**
     * 提供方（场馆/教练）名称（快照）
     */
    private  String sellerName;

    /**
     * 提供方 ID（场馆/教练）
     */
    private Long sellerId;

    /**
     * 预定日期
     */
    private LocalDate bookingDate;
}


