package com.unlimited.sports.globox.model.coach.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @since 2025/12/29 11:47
 * 教练扩展信息表
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "coach_profiles")
public class CoachProfiles implements Serializable {

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
     * PTR/USPTA等级：1-PTR初级，2-PTR中级，3-PTR高级 ....
     */
    @TableField(value = "coach_certification_level")
    private List<Integer> coachCertificationLevel;

    /**
     * 证书附件URL
     */
    @TableField(value = "coach_certification_files")
    private List<String> coachCertificationFiles;

    /**
     * 教学视频数组：[{"url":"视频地址","name":"视频名称","duration":120,"cover_url":"封面图地址","sort":1}]
     */
    @TableField(
            value = "coach_teaching_videos",
            typeHandler = JacksonTypeHandler.class
    )
    private List<TeachingVideo> coachTeachingVideos;

    /**
     * 教学图片数组：[{"url":"图片地址","name":"图片名称","sort":1}]
     */
    @TableField(
            value = "coach_teaching_photos",
            typeHandler = JacksonTypeHandler.class
    )
    private List<TeachingPhoto> coachTeachingPhotos;

    // 内部类
    @Data
    public static class TeachingVideo {
        private String url;
        private String name;
        private Integer duration;
        private String coverUrl;
        private Integer sort;
    }

    @Data
    public static class TeachingPhoto {
        private String url;
        private String name;
        private Integer sort;
    }

    /**
     * 教龄
     */
    @TableField(value = "coach_teaching_years")
    private Integer coachTeachingYears;

    /**
     * 专长标签：["青少年教学","成人入门","技术提升"]等
     */
    @TableField(value = "coach_specialty_tags")
    private String coachSpecialtyTags;

    /**
     * 教学风格简述
     */
    @TableField(value = "coach_teaching_style")
    private String coachTeachingStyle;

    /**
     * 常驻区域
     */
    @TableField(value = "coach_service_area")
    private String coachServiceArea;

    /**
     * 可接受的远距离服务区域
     */
    @TableField(value = "coach_remote_service_area")
    private String coachRemoteServiceArea;

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



}
