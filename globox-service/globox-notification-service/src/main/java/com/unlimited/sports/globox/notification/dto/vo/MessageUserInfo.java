package com.unlimited.sports.globox.notification.dto.vo;

import lombok.*;

/**
 * 消息发送者用户信息
 * 用于在通知消息中展示发送者的基本信息（头像、昵称）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageUserInfo {

    /**
     * 用户ID
     */
    @NonNull
    private Long userId;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像URL
     */
    private String avatarUrl;
}
