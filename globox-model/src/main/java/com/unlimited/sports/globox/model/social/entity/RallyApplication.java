package com.unlimited.sports.globox.model.social.entity;


import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 加入申请表实体类
 * 对应数据库表：rally_application
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("rally_application")
public class RallyApplication implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键，申请ID
     */
    @TableId(value = "application_id", type = IdType.AUTO)
    private Long applicationId;
    /**
     * 申请加入的帖子的id
     */
    @TableField("rally_post_id")
    private Long rallyPostId;
    /**
     * 申请人id
     */
    @TableField("applicant_id")
    private Long applicantId;

    /**
     * 申请时间
     */
    @TableField("applied_at")
    private LocalDateTime appliedAt;

    /**
     * 申请理由
     */
    @TableField("reason")
    private String reason;

    /**
     * 申请状态: 0=待审核 1=已接受 2=已拒绝
     */
    @TableField("status")
    private int status = RallyApplyStatusEnum.PENDING.getCode();

    /**
     * 审批时间
     */
    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    /**
     * 发起人ID
     */
    @TableField("reviewed_by")
    private Long reviewedBy;

    @TableLogic
    private Boolean deleted;

}