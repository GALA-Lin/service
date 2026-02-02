package com.unlimited.sports.globox.model.social.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.unlimited.sports.globox.common.enums.social.RallyTimeTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * 球帖表实体类
 * 对应数据库表：rally_posts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("rally_posts")
public class RallyPosts implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键，球帖ID
     */
    @TableId(value = "rally_post_id", type = IdType.AUTO)
    private Long rallyPostId;

    /**
     * 发起人ID
     */
    @TableField("initiator_id")
    private Long initiatorId;

    /**
     * 约球宣言
     */
    @TableField("rally_title")
    private String rallyTitle;

    /**
     * 区域
     */
    @TableField("rally_region")
    private String rallyRegion;

    /**
     * 球馆名称(可空)
     */
    @TableField("rally_venue_name")
    private String rallyVenueName;

    /**
     * 场地名称(可空)
     */
    @TableField("rally_court_name")
    private String rallyCourtName;

    /**
     * 日期
     */
    @TableField("rally_event_date")
    private LocalDate rallyEventDate;


    /**
     * 约球时段：0=上午；1=下午；2=晚上
     */
    @TableField("rally_time_type")
    private RallyTimeTypeEnum rallyTimeType;
    /**
      * 时间-开始
     */
    @TableField("rally_start_time")
    @DateTimeFormat(pattern = "HH:mm:ss")
    private LocalTime rallyStartTime;

    /**
     * 时间-结束
     */
    @TableField("rally_end_time")
    @DateTimeFormat(pattern = "HH:mm:ss")
    private LocalTime rallyEndTime;

    /**
     * 费用
     */
    @TableField("rally_cost")
    private BigDecimal rallyCost;

    /**
     * 承担方式: 0=发起人承担 1=AA分摊
     */
    @TableField("rally_cost_bearer")
    private int rallyCostBearer;

    /**
     * 活动类型: 0=不限 1=单打 2=双打
     */
    @TableField("rally_activity_type")
    private int rallyActivityType = RallyActivityTypeEnum.UNLIMITED.getCode();

    /**
     * 性别限制: 0=不限 1=仅男生 2=仅女生
     */
    @TableField("rally_gender_limit")
    private int rallyGenderLimit = RallyGenderLimitEnum.NO_LIMIT.getCode();

    /**
     * NTRP最低水平(1.5-7.0)
     */
    @TableField("rally_ntrp_min")
    private double rallyNtrpMin;

    /**
     * NTRP最高水平(1.5-7.0)
     */
    @TableField("rally_ntrp_max")
    private double rallyNtrpMax;

    /**
     * 总人数
     */
    @TableField("rally_total_people")
    private Integer rallyTotalPeople;

    /**
     * 剩余人数
     */
    @TableField("rally_remaining_people")
    private Integer rallyRemainingPeople;

    /**
     * 备注
     */
    @TableField("rally_notes")
    private String rallyNotes;

    /**
     * 状态: 0=已发布 1=已满员 2=已取消 3=已完成
     */
    @TableField("rally_status")
    private int rallyStatus = RallyPostsStatusEnum.PUBLISHED.getCode();

    /**
     * 创建时间
     */
    @TableField("rally_created_at")
    private LocalDateTime rallyCreatedAt;

    /**
     * 更新时间
     */
    @TableField("rally_updated_at")
    private LocalDateTime rallyUpdatedAt;
}