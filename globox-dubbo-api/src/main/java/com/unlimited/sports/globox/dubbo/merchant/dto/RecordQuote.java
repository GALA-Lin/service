package com.unlimited.sports.globox.dubbo.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;


/**
 * 槽数据
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecordQuote implements Serializable {
    /**
     * record ID
     */
    private Long recordId;

    /**
     * record 归属场地
     */
    private Long courtId;

    /**
     * 场地名称
     */
    private String courtName;

    /**
     * 预定日期
     */
    private LocalDate bookingDate;

    /**
     * 预定槽开始时间
     */
    private LocalTime startTime;

    /**
     * 预定槽结束时间
     */
    private LocalTime endTime;

    /**
     * 槽位单价
     */
    private BigDecimal unitPrice;

    /**
     * 槽位额外费用列表
     */
    private List<ExtraQuote> recordExtras;
}