package com.unlimited.sports.globox.model.venue.vo;

import com.unlimited.sports.globox.common.enums.order.ChargeModeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 附加费用信息
 * 公共VO，用于订单预览和订单详情
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtraChargeVo {

    /**
     * 费用类型ID
     */
    @NotNull
    private Long chargeTypeId;

    /**
     * 费用名称
     */
    @NotNull
    private String chargeName;

    /**
     * 计费模式
     */
    @NotNull
    private ChargeModeEnum chargeMode;

    /**
     * 固定值 / 单价
     */
    @NotNull
    private BigDecimal fixedValue;

    /**
     * 实际收费金额
     */
    @NotNull
    private BigDecimal chargeAmount;

    /**
     * 是否默认费用（1=默认必选，0=可选）
     */
    private Integer isDefault;
}
