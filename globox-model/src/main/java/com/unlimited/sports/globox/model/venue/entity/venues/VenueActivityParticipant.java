package com.unlimited.sports.globox.model.venue.entity.venues;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 活动参与者表
 * 记录用户报名参加活动的信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("venue_activity_participant")
public class VenueActivityParticipant {

    /**
     * 参与记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long participantId;

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 报名时间
     */
    @TableField(value = "registration_time", fill = FieldFill.INSERT)
    private LocalDateTime registrationTime;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 删除版本号
     * 0: 正常报名状态，>0: 已取消（值为删除时对应的participantId）
     * 通过 (userId + activityId + deleteVersion)建立唯一索引, 防止用户短时间内重复报名
     * 用户取消报名之后,deleteVersion改为此条记录的participantId,保证历史取消记录不会被唯一索引阻碍,历史记录可追溯
     * 后续若活动需要添加新的状态(不局限于报名/取消报名),可添加新字段status,来表示状态,deleteVersion仅表示这条活动参与绑定记录是否有效
     */
    @TableField("delete_version")
    private Long deleteVersion;
}
