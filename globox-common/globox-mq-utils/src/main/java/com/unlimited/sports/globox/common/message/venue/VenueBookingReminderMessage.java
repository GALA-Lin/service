package com.unlimited.sports.globox.common.message.venue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订场即将开始提醒延迟消息
 * 按照单个场地的连续时间段发送通知
 * 消费时会根据recordIds、userId和updateTime查询完整的槽位、场地、场馆信息
 * 使用updateTime确保通知只发送给真正的占用人，避免槽位释放后重新被占用导致通知错误
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueBookingReminderMessage implements Serializable {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 连续时间段的槽位记录ID列表（用于检查状态是否仍被占用）
     * 例如：8:30-9:00, 9:00-9:30 两个连续槽位，包含两个recordId
     */
    private List<Long> recordIds;

    /**
     * 槽位占用时间（用于验证槽位是否被释放后重新占用）
     * 消费时检查槽位的update_time必须等于此时间，确保通知给正确的用户
     */
    private LocalDateTime occupyTime;
}
