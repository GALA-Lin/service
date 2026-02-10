package com.unlimited.sports.globox.model.social.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 笔记点赞事件
 * 存储在 Redis Hash 中，field = {userId}:{noteId}，value = 本对象的 JSON
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NoteLikeEvent {

    /** 用户ID */
    private Long userId;

    /** 笔记ID */
    private Long noteId;

    /** 点赞状态：1=点赞，0=取消点赞 */
    private Integer likeStatus;

    /** 点赞/取消点赞时间 */
    private LocalDateTime likeTime;

    public static final int STATUS_LIKE = 1;
    public static final int STATUS_UNLIKE = 0;

    public boolean isLike() {
        return STATUS_LIKE == likeStatus;
    }
}
