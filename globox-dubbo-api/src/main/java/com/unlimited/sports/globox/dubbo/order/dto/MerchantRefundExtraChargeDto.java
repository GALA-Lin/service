package com.unlimited.sports.globox.dubbo.order.dto;

import com.unlimited.sports.globox.common.enums.order.RefundStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRefundExtraChargeDto implements Serializable {

    @NotNull
    private Long extraChargeId;

    /**
     * 商家数据库的额外费用模版 id
     */
    @NotNull
    private Long chargeTypeId;

    /**
     * 订单项ID：订单级额外费用为空
     */
    @Null
    private Long orderItemId;

    @NotNull
    private String chargeName;

    /**
     * 本次退款金额（快照）
     */
    @NotNull
    private BigDecimal refundAmount;
}