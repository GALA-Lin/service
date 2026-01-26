package com.unlimited.sports.globox.model.venue.entity.venues;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 场馆活动表
 * 用于支持畅打、比赛、培训等活动
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "venue_activity", autoResultMap = true)
public class VenueActivity {

    /**
     * 活动ID
     */
    @TableId(type = IdType.AUTO)
    private Long activityId;

    /**
     * 场馆ID
     */
    private Long venueId;

    /**
     * 场地ID
     */
    private Long courtId;

    @TableField("merchant_batch_id")
    private Long merchantBatchId;

    /**
     * 活动类型ID（外键，指向activity_type表）
     */
    private Long activityTypeId;

    /**
     * 活动类型描述（冗余字段，用于查询时不需要join activity_type表）
     * 如：畅打、比赛、培训等
     */
    private String activityTypeDesc;

    /**
     * 活动名称
     */
    private String activityName;

    /**
     * 活动图片
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> imageUrls;

    /**
     * 活动日期
     */
    private LocalDate activityDate;

    /**
     * 开始时间
     */
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 最大参与人数（可选，有些活动不限人数）
     */
    private Integer maxParticipants;

    /**
     * 单个用户最多允许报名几个名额（默认1个，null表示不限制）
     */
    private Integer maxQuotaPerUser;

    /**
     * 当前参与人数
     */
    @Builder.Default
    private Integer currentParticipants = 0;

    /**
     * 单人价格（可选，有些活动免费）
     */
    private BigDecimal unitPrice;

    /**
     * 活动描述
     */
    private String description;

    /**
     * 报名截止时间
     */
    private LocalDateTime registrationDeadline;

    /**
     * 组织者ID（商家或管理员）
     */
    private Long organizerId;

    /**
     * 组织者类型：1=MERCHANT(商家)，2=ADMIN(管理员)
     */
    private Integer organizerType;


    /**
     * 组织者姓名(商家名称/员工displayName)
     */
    private String organizerName;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 参与用户的最低NTRP水平要求
     * 范围：1.0 - 7.0，允许半值（1.5, 2.5等）
     * NULL表示无要求
     */
    private Double minNtrpLevel;

    /**
     * 活动配置（JSON格式，存储不同活动类型的特殊配置）
     */
    private String activityConfig;


    /**
     * 活动状态 1 -- 正常 2 -- 已被取消
     */
    private Integer status;
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
}
