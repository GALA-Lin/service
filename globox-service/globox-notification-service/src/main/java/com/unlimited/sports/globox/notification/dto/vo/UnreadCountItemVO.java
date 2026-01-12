package com.unlimited.sports.globox.notification.dto.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 未读消息数量项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountItemVO {

    /**
     * 消息类型
     * explore=探索消息, rally=球局消息, system=系统消息
     */
    private String type;

    /**
     * 未读数量
     */
    @Builder.Default
    private Integer unReadCount = 0;
}
