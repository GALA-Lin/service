package com.unlimited.sports.globox.common.message.venue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动提醒延迟消息
 * 活动报名支付成功后发送延迟消息，在活动开始前提醒用户
 *
 * 消费时会根据participantId验证参与者记录是否仍然有效
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityReminderMessage implements Serializable {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 参与者ID
     */
    private Long participantId;

    /**
     * 报名时间（用于验证参与者记录是否被取消后重新报名）
     * 消费时检查参与者记录的created_at必须等于此时间
     */
    private LocalDateTime registrationTime;

    /**
     * 订单号
     */
    private Long orderNo;
}
