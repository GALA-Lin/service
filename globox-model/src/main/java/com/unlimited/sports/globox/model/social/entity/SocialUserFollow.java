package com.unlimited.sports.globox.model.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 关注关系实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("social_user_follow")
public class SocialUserFollow {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关注者ID
     */
    private Long userId;

    /**
     * 被关注者ID
     */
    private Long followUserId;

    /**
     * 关注时间
     */
    private LocalDateTime createdAt;
}




