package com.unlimited.sports.globox.common.message.social;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 约球即将开始提醒延迟消息
 * 当约球创建时，如果指定了开始时间，发送延迟消息到队列
 * 延迟时间 = 约球时间 - 1小时
 * 消费时会根据rallyId查询约球信息，检查约球是否还存在，获取发起人和参与者信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RallyStartingReminderMessage implements Serializable {
    /**
     * 约球ID（消费时用来查询约球及其参与者信息）
     */
    private Long rallyId;
}
