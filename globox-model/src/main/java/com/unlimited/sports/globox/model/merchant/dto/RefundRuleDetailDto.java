package com.unlimited.sports.globox.model.merchant.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

/**
 * @since 2025/12/31 11:02
 *
 */ // ============= 退款规则明细 DTO =============
@Data
public class RefundRuleDetailDto {

    /**
     * 明细ID（更新时需要）
     */
    private Long venueRefundRuleDetailId;

    /**
     * 最小提前小时数（包含）
     */
    @NotNull(message = "最小提前小时数不能为空")
    @Min(value = 0, message = "最小提前小时数不能小于0")
    private Integer minHoursBefore;

    /**
     * 最大提前小时数（不包含），NULL表示无上限
     */
    @Min(value = 1, message = "最大提前小时数不能小于1")
    private Integer maxHoursBefore;

    /**
     * 退款比例（0-100）
     */
    @NotNull(message = "退款比例不能为空")
    @DecimalMin(value = "0.00", message = "退款比例不能小于0")
    @DecimalMax(value = "100.00", message = "退款比例不能大于100")
    private BigDecimal refundPercentage;



    /**
     * 手续费比例（0-100）
     */
    @DecimalMin(value = "0.00", message = "手续费比例不能小于0")
    @DecimalMax(value = "100.00", message = "手续费比例不能大于100")
    private BigDecimal handlingFeePercentage = BigDecimal.ZERO;

    /**
     * 说明文字
     */
    @Size(max = 500, message = "说明文字长度不能超过500字符")
    private String refundRuleDetailDesc;

    /**
     * 排序序号
     */
    private Integer sortOrderNum = 0;
}
