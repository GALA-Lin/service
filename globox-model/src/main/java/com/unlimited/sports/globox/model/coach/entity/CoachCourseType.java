package com.unlimited.sports.globox.model.coach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @since 2025/12/29 11:49
 * 教练课程服务表
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "coach_course_type")
public class CoachCourseType implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 服务ID
     */
    @TableId(value = "coach_services_id", type = IdType.AUTO)
    private Long coachServicesId;

    /**
     * 教练ID
     */
    @TableField(value = "coach_user_id")
    private Long coachUserId;

    /**
     * 服务名称："一对一私教课"
     */
    @TableField(value = "coach_service_name")
    private String coachServiceName;

    /**
     * 1-一对一，2-一对二，3-小班(3-6人)
     */
    @TableField(value = "coach_service_type")
    private Integer coachServiceType;

    /**
     * 时长（分钟）
     */
    @TableField(value = "coach_duration")
    private Integer coachDuration;

    /**
     * 价格（元）
     */
    @TableField(value = "coach_price")
    private BigDecimal coachPrice;

    /**
     * 服务描述
     */
    @TableField(value = "coach_description")
    private String coachDescription;

    /**
     * 0-停用，1-启用
     */
    @TableField(value = "coach_is_active")
    private Integer coachIsActive;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at",fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}
