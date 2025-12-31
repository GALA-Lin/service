package com.unlimited.sports.globox.model.coach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @since 2025/12/29 11:54
 * 教练认证申请表
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "coach_applications")
public class CoachApplications implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value ="coach_applications_id" , type = IdType.AUTO)
    private Long coach_applications_id;

    /**
     * 申请用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 真实姓名
     */
    @TableField("real_name")
    private String realName;

    /**
     * 身份证号（加密存储）
     */
    @TableField("id_card")
    private String idCard;

    /**
     * 联系电话
     */
    @TableField("phone")
    private String phone;

    /**
     * 证书类型：PTR、USPTA、CTA等
     */
    @TableField("coach_certification_type")
    private List<String>  coachCertificationType;

    /**
     * 证书等级
     */
    @TableField("coach_certification_level")
    private List<Integer> coachCertificationLevel;

    /**
     * 证书编号
     */
    @TableField("coach_certification_number")
    private List<String> coachCertificationNumber;

    /**
     * 证书照片：[{url, name}]
     */
    @TableField("coach_certification_files")
    private List<String> coachCertificationFiles;

    /**
     * 教学年限
     */
    @TableField("coach_teaching_years")
    private Integer coachTeachingYears;

    /**
     * 教学经历描述
     */
    @TableField("coach_teaching_experience")
    private String coachTeachingExperience;

    /**
     * 工作照片：[url1, url2]
     */
    @TableField("coach_work_photos")
    private String coachWorkPhotos;

    /**
     * 自我介绍
     */
    @TableField("coach_self_introduction")
    private String coachSelfIntroduction;

    /**
     * 擅长领域标签
     */
    @TableField("coach_specialty_tags")
    private String coachSpecialtyTags;

    /**
     * 申请状态：0-待审核，1-已通过，2-已拒绝，3-需补充材料
     */
    @TableField("coach_application_status")
    private Integer coachApplicationStatus;

    /**
     * 审核意见
     */
    @TableField("audit_remark")
    private String auditRemark;

    /**
     * 审核人ID
     */
    @TableField("auditor_id")
    private Long auditorId;

    /**
     * 审核时间
     */
    @TableField("audit_time")
    private LocalDateTime auditTime;

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
