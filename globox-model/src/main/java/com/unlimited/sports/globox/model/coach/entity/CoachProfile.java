package com.unlimited.sports.globox.model.coach.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.unlimited.sports.globox.model.coach.vo.VideoItem;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * @since 2025/12/29 11:47
 * 教练扩展信息表
 *
 * 优化说明：
 * 1. coach_remote_service_area 改为使用 JacksonTypeHandler
 * 2. 数据库存储 JSON 数组格式：["锦江区","成华区"]
 * 3. Java 对象直接使用 List<String> 类型，自动序列化/反序列化
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "coach_profiles", autoResultMap = true)
public class CoachProfile implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 教练档案ID
     */
    @TableId(value = "coach_profiles_id", type = IdType.AUTO)
    private Long coachProfilesId;

    /**
     * 关联用户ID
     */
    @TableField(value = "coach_user_id")
    private Long coachUserId;

    /**
     * 证书等级（JSON数组）
     */
    @TableField(value = "coach_certification_level", typeHandler = JacksonTypeHandler.class)
    private List<String> coachCertificationLevel;

    /**
     * 主要奖项
     */
    @TableField(value = "coach_award", typeHandler = JacksonTypeHandler.class)
    private List<String> coachAward;

    /**
     * 证书附件URL（JSON数组）
     */
    @TableField(value = "coach_certification_files", typeHandler = JacksonTypeHandler.class)
    private List<String> coachCertificationFiles;

    /**
     * 教学图片（JSON数组）
     */
    @TableField(value = "coach_work_photos", typeHandler = JacksonTypeHandler.class)
    private List<String> coachWorkPhotos;

    /**
     * 教学视频
     */
    @TableField(value = "coach_work_videos", typeHandler = JacksonTypeHandler.class)
    private List<VideoItem> coachWorkVideos;

    /**
     * 教龄
     */
    @TableField(value = "coach_teaching_years")
    private Integer coachTeachingYears;

    /**
     * 专长标签（JSON数组）
     */
    @TableField(value = "coach_specialty_tags", typeHandler = JacksonTypeHandler.class)
    private List<String> coachSpecialtyTags;

    /**
     * 教学风格简述
     */
    @TableField(value = "coach_teaching_style")
    private String coachTeachingStyle;

    /**
     * 常驻服务区域（1个）
     */
    @TableField(value = "coach_service_area")
    private String coachServiceArea;

    /**
     * 可接受的远距离服务区域（JSON数组）
     * 数据库存储格式：["锦江区","成华区"]
     * Java对象类型：List<String>
     */
    @TableField(value = "coach_remote_service_area", typeHandler = JacksonTypeHandler.class)
    private List<String> coachRemoteServiceArea;

    /**
     * 最短授课时长（小时）
     */
    @TableField(value = "coach_min_hours")
    private Integer coachMinHours;

    /**
     * 远距离区域最低授课时长（小时），默认2小时
     */
    @TableField(value = "coach_remote_min_hours")
    private Integer coachRemoteMinHours;

    /**
     * 接受场地类型：1-红土，2-草地，3-XXX 4-XXX 0-都可以
     */
    @TableField(value = "coach_accept_venue_type")
    private Integer coachAcceptVenueType;

    /**
     * 最低课程价格（用于筛选展示）
     */
    @TableField(value = "coach_min_price")
    private BigDecimal coachMinPrice;

    /**
     * 最高课程价格
     */
    @TableField(value = "coach_max_price")
    private BigDecimal coachMaxPrice;

    /**
     * 累计学员数
     */
    @TableField(value = "coach_total_students")
    private Integer coachTotalStudents;

    /**
     * 累计授课时长
     */
    @TableField(value = "coach_total_hours")
    private BigDecimal coachTotalHours;

    /**
     * 累计完成课程数（订单数）
     */
    @TableField(value = "coach_total_courses")
    private Integer coachTotalCourses;

    /**
     * 综合评分（0-5）
     */
    @TableField(value = "coach_rating_score")
    private BigDecimal coachRatingScore;

    /**
     * 评价数
     */
    @TableField(value = "coach_rating_count")
    private Integer coachRatingCount;

    /**
     * 教练所在纬度
     */
    @TableField("coach_latitude")
    private Double coachLatitude;

    /**
     * 教练所在精度
     */
    @TableField("coach_longitude")
    private Double coachLongitude;

    /**
     * 教练性别：0-女，1-男
     */
    @TableField(exist = false)
    private Integer genderCode;

    /**
     * 教练状态：0-暂停接单，1-正常接单，2-休假中
     */
    @TableField(value = "coach_status")
    private Integer coachStatus;

    /**
     * 是否推荐教练：0-否，1-是
     */
    @TableField(value = "is_recommended_coach")
    private Integer isRecommendedCoach;

    /**
     * 展示排序权重（定义视图：数值越大越靠前）
     */
    @TableField(value = "coach_display_order")
    private Integer coachDisplayOrder;

    /**
     * 审核状态：0-待审核，1-已通过，2-已拒绝
     */
    @TableField(value = "coach_audit_status")
    private Integer coachAuditStatus;

    /**
     * 审核备注
     */
    @TableField(value = "coach_audit_remark")
    private String coachAuditRemark;

    /**
     * 审核完成时间
     */
    @TableField(value = "coach_audit_time")
    private LocalDateTime coachAuditTime;

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
     * 获取远距离服务区域列表（兼容方法，已废弃）
     * @deprecated 直接使用 getCoachRemoteServiceArea() 即可获取 List<String>
     */
    @Deprecated
    public List<String> getRemoteServiceAreaList() {
        return coachRemoteServiceArea != null ? coachRemoteServiceArea : Collections.emptyList();
    }
}