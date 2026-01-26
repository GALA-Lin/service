package com.unlimited.sports.globox.model.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.unlimited.sports.globox.common.enums.payment.ProfitSharingStatusEnum;
import com.unlimited.sports.globox.model.base.BaseEntity;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("payment_profit_sharing")
public class PaymentProfitSharing extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关联支付单ID
     */
    @TableField("payment_id")
    private Long paymentId;

    /**
     * 提供给第三方支付的分账编号（你表里是 bigint）
     * 备注：微信 v3 分账 out_order_no 通常是字符串，建议后续改成 varchar(64)
     */
    @TableField("out_profit_sharing_no")
    private String outProfitSharingNo;

    /**
     * 商户订单号（支付单号）
     */
    @TableField("out_trade_no")
    private String outTradeNo;

    /**
     * 由第三方提供的分账编号（有些渠道可能为空/无此概念）
     */
    @TableField("profit_sharing_no")
    private String profitSharingNo;

    @TableField("trade_no")
    private String tradeNo;

    private Long receiverId;

    /**
     * 接收方收到的分账金额
     */
    @TableField("amount")
    private BigDecimal amount;

    /**
     * 分账状态（建议配套枚举）
     */
    @TableField("status")
    private ProfitSharingStatusEnum status;
}