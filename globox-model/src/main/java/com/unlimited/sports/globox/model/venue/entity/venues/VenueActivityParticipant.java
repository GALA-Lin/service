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
     * 报名状态：0=ACTIVE(有效) 1=CANCELLED(已取消)
     */
    @TableField("status")
    private Integer status;

    /**
     * 报名批次ID
     */
    @TableField("batch_id")
    private String batchId;

    /**
     * 用户手机号
     */
    @TableField("phone")
    private String phone;
}
