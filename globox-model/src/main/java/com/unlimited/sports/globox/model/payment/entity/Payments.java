package com.unlimited.sports.globox.model.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.unlimited.sports.globox.common.enums.ClientType;
import com.unlimited.sports.globox.common.enums.ThirdPartyJsapiEnum;
import com.unlimited.sports.globox.common.enums.order.PaymentTypeEnum;
import com.unlimited.sports.globox.common.enums.order.SellerTypeEnum;
import com.unlimited.sports.globox.common.enums.payment.PaymentStatusEnum;
import com.unlimited.sports.globox.model.base.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.*;

/**
 * 支付信息表
 * 字段特殊说明：
 * - outTradeNo 对外业务编号：是我们提供给支付平台的对外业务编号，与订单编号一样需要保持业务系统中唯一
 * - tradeNo 第三方交易编号：由支付平台提供，支付回调中可以拿到
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "payments")
@EqualsAndHashCode(callSuper = true)
public class Payments extends BaseEntity {

    /**
     * 对外业务编号
     */
    private String outTradeNo;

    /**
     * 订单编号
     */
    private Long orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 服务提供方类型
     */
    private SellerTypeEnum sellerType;

    /**
     * 分账时收款人 id
     */
    private Long receiverId;

    /**
     * 支付类型（1=WECHAT / 2=ALIPAY）
     */
    private PaymentTypeEnum paymentType;

    /**
     * 下单端
     */
    private ClientType clientType;

    /**
     * 用户 open id
     */
    private String openId;

    /**
     * 第三方交易编号，由第三方平台提供
     */
    private String tradeNo;

    /**
     * 标识哪一个三方小程序
     */
    private ThirdPartyJsapiEnum thirdPartyJsapi;

    /**
     * 退款时提供
     */
    private String outRequestNo;

    /**
     * 支付金额
     */
    private BigDecimal totalAmount;

    /**
     * 已退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 交易内容
     */
    private String subject;

    /**
     * 支付状态
     */
    private PaymentStatusEnum paymentStatus;

    /**
     * 是否是活动订单
     */
    private Boolean activity;

    /**
     * 是否需要分账
     */
    private Boolean profitSharing;

    /**
     * 用户支付时间
     */
    private LocalDateTime paymentAt;

    /**
     * 回调时间
     */
    private LocalDateTime callbackAt;

    /**
     * 回调信息
     */
    private String callbackContent;
}