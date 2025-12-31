package com.unlimited.sports.globox.model.merchant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @since 2025/12/30 17:14
 * 退款规则明细表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("venue_refund_rule_details")
public class VenueRefundRuleDetail implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 明细ID
     */
    @TableId(value = "venue_refund_rule_detail_id", type = IdType.AUTO)
    private Long venueRefundRuleDetailId;

    /**
     * 关联的规则ID
     */
    @TableField("venue_refund_rule_id")
    private Long venueRefundRuleId;

    /**
     * 最小提前小时数（包含）
     */
    @TableField("min_hours_before")
    private Integer minHoursBefore;

    /**
     * 最大提前小时数（不包含），NULL表示无上限
     */
    @TableField("max_hours_before")
    private Integer maxHoursBefore;

    /**
     * 退款比例（0-100）
     */
    @TableField("refund_percentage")
    private BigDecimal refundPercentage;

    /**
     * 是否需要联系商家：0-自动退款，1-需要联系商家
     */
    @TableField("need_contact_merchant")
    private Integer needContactMerchant;

    /**
     * 手续费比例（0-100）
     */
    @TableField("handling_fee_percentage")
    private BigDecimal handlingFeePercentage;

    /**
     * 该阶梯的说明文字
     */
    @TableField("refund_rule_detail_desc")
    private String refundRuleDetailDesc;

    /**
     * 排序序号
     */
    @TableField("sort_order_num")
    private Integer sortOrderNum;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}