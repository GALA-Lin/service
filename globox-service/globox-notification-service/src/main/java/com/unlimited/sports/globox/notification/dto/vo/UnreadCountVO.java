package com.unlimited.sports.globox.notification.dto.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 未读消息数量统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountVO {

    /**
     * 各类型消息的未读数量列表
     */
    private List<UnreadCountItemVO> items;

    /**
     * 总未读数量
     */
    @Builder.Default
    private Integer totalUnreadCount = 0;
}
