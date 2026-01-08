package com.unlimited.sports.globox.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @since 2026/1/8 15:47
 *
 */
@Getter
@AllArgsConstructor
public enum MerchantErrorCode implements ResultCode {
    /**
     * 退款申请时间晚于活动开始时间
     */
    REFUND_TIME_AFTER_EVENT_START(3001, "退款申请时间晚于活动开始时间，不支持退款"),
    /**
     * 未找到可用的退款规则
     */
    NO_VALID_REFUND_RULE(3002, "未找到可用的退款规则，不支持退款"),
    /**
     * 退款规则未配置明细
     */
    REFUND_RULE_NO_DETAILS(3003, "退款规则未配置明细，不支持退款"),
    /**
     * 未匹配任何规则明细
     */
    NO_MATCHED_REFUND_RULE_DETAIL(3004, "未匹配到适用的退款规则，不支持退款"),
    /**
     * 匹配到退款比例为0的规则（超过退款时间）
     */
    REFUND_RATIO_ZERO(3005, "超过退款时间无法退款，请阅读退款详情");

    private final Integer code;
    private final String message;

}
