package com.unlimited.sports.globox.dubbo.coach.dto;

import com.unlimited.sports.globox.dubbo.merchant.dto.OrderLevelExtraQuote;
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
 * @since 2026/1/6 14:23
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CoachPricingResultDto implements Serializable {

    /**
     * 时段报价信息列表
     */
    @NotNull
    private List<CoachSlotQuote> slotQuotes;

    /**
     * 订单级额外收费信息列表
     */
    private List<OrderLevelExtraQuote> orderLevelExtras;

    /**
     * VENUE 订单场馆归属：1=home平台，2=away平台
     */
    private Integer sourcePlatform = 1;

    /**
     * 教练用户ID
     */
    @NotNull
    private Long sellerId;

    /**
     * 预约日期
     */
    private LocalDate bookingDate;


    /**
     * 教练姓名
     */
    private String coachName;

    /**
     * 总价格
     */
    private BigDecimal totalPrice;

    /**
     * 服务类型描述
     */
    private String serviceTypeDesc;

    /**
     * 可接受区域列表
     */
    private List<String> acceptableAreas;

    /**
     * 场地要求说明
     */
    private String venueRequirementDesc;
}
