package com.unlimited.sports.globox.model.merchant.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.unlimited.sports.globox.model.merchant.enums.DefaultStatusEnum;
import com.unlimited.sports.globox.model.merchant.enums.EnableStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @since 2025/12/30 17:06
 * 退款规则关联表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("venue_refund_rules")
public class VenueRefundRule implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 规则ID
     */
    @TableId(value = "venue_refund_rule_id", type = IdType.AUTO)
    private Long venueRefundRuleId;

    /**
     * 商家ID
     */
    @TableField("merchant_id")
    private Long merchantId;

    /**
     * 场馆ID（NULL表示商家默认规则）
     */
    @TableField("venue_id")
    private Long venueId;

    /**
     * 规则名称
     */
    @TableField("venue_refund_rule_name")
    private String venueRefundRuleName;

    /**
     * 是否为默认规则：0-否，1-是
     */
    @TableField("is_default")
    private DefaultStatusEnum isDefault;

    /**
     * 是否启用：0-禁用，1-启用
     */
    @TableField("is_enabled")
    private EnableStatusEnum isEnabled;

    /**
     * 规则描述说明
     */
    @TableField("venue_refund_rule_desc")
    private String venueRefundRuleDesc;

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
