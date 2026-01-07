package com.unlimited.sports.globox.dubbo.coach.dto;

import com.unlimited.sports.globox.dubbo.merchant.dto.ExtraQuote;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * @since 2026/1/6 14:26
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CoachSlotQuote implements Serializable {

    /**
     * 时段记录ID
     */
    private Long recordId;

    /**
     * 教练ID
     */
    @NotNull
    private Long coachId;

    /**
     * 教练姓名
     */
    private String coachName;

    /**
     * 预约日期
     */
    @NotNull
    private LocalDate bookingDate;

    /**
     * 开始时间
     */
    @NotNull
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 槽位额外费用列表
     */
    private List<ExtraQuote> recordExtras;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 时段模板ID
     */
    private Long slotTemplateId;

    /**
     * 时长（分钟）
     */
    private Integer durationMinutes;

    /**
     * 服务类型
     */
    private Integer serviceType;

    /**
     * 服务类型描述
     */
    private String serviceTypeDesc;
}
