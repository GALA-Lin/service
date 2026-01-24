package com.unlimited.sports.globox.model.merchant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @since 2025-12-18-11:18
 * 场馆员工关联实体类
 */
@Data
@TableName("venue_staff")
public class VenueStaff implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 员工关联ID
     */
    @TableId(value = "venue_staff_id", type = IdType.AUTO)
    private Long venueStaffId;

    /**
     * 员工用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 所属场馆ID
     */
    @TableField("venue_id")
    private Long venueId;

    /**
     * 所属商家ID（冗余字段，便于跨场馆查询）
     */
    @TableField("merchant_id")
    private Long merchantId;

    /**
     * 角色类型：1=场馆负责人(OWNER)，2=普通员工(STAFF)
     */
    @TableField("role_type")
    private Integer roleType;

    /**
     * 功能权限配置，JSON格式
     */
    @TableField("permissions")
    private String permissions;

    /**
     * 员工显示名称
     */
    @TableField("display_name")
    private String displayName;

    /**
     * 联系电话
     */
    @TableField("contact_phone")
    private String contactPhone;

    /**
     * 联系邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 职位名称：店长、前台、运营等
     */
    @TableField("job_title")
    private String jobTitle;

    /**
     * 工号
     */
    @TableField("employee_no")
    private String employeeNo;

    /**
     * 状态：0=已离职，1=在职，2=停用
     */
    @TableField("status")
    private Integer status;

    /**
     * 入职时间
     */
    @TableField("hired_at")
    private LocalDateTime hiredAt;

    /**
     * 离职时间
     */
    @TableField("resigned_at")
    private LocalDateTime resignedAt;

    /**
     * 备注信息
     */
    @TableField("remark")
    private String remark;

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
     * 创建人ID
     */
    @TableField("created_by")
    private Long createdBy;
}