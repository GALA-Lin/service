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
 * 拉黑关系实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("social_user_block")
public class SocialUserBlock {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 拉黑人ID
     */
    private Long userId;

    /**
     * 被拉黑人ID
     */
    private Long blockedUserId;

    /**
     * 拉黑时间
     */
    private LocalDateTime createdAt;

    /**
     * 是否已删除
     */
    private Boolean deleted;
}





