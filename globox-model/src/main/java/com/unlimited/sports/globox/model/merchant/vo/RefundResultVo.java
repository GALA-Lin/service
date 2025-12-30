package com.unlimited.sports.globox.model.merchant.vo;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @since  2025/12/22 09:42
 * 退款请求结果VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResultVo {

    /**
     * 是否成功申请退款
     */
    @NonNull
    private Boolean success;

    /**
     * 退款单号
     */
    @NonNull
    private String refundNo;

    /**
     * 退款金额
     */
    @NonNull
    private BigDecimal refundAmount;

    /**
     * 退款状态：1=退款中，2=成功，3=失败
     */
    @NonNull
    private Integer refundStatus;

    /**
     * 失败原因
     */
    private String failureReason;

    /**
     * 退款时间
     */
    @NonNull
    private LocalDateTime refundTime;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建成功的结果
     */
    public static RefundResultVo success(String refundNo, BigDecimal amount) {
        return RefundResultVo.builder()
                .success(true)
                .refundNo(refundNo)
                .refundAmount(amount)
                .refundStatus(1) // 退款中
                .refundTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建失败的结果
     */
    public static RefundResultVo failure(String errorCode, String errorMessage) {
        return RefundResultVo.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}