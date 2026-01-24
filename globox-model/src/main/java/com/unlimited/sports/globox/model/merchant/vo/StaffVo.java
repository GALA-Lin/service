package com.unlimited.sports.globox.model.merchant.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 员工信息VO
 * @since 2026-01-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffVo {

    /**
     * 员工关联ID
     */
    private Long venueStaffId;

    /**
     * 员工用户ID
     */
    private Long userId;

    /**
     * 所属场馆ID
     */
    private Long venueId;

    /**
     * 场馆名称
     */
    private String venueName;

    /**
     * 所属商家ID
     */
    private Long merchantId;

    /**
     * 角色类型：1=场馆负责人(OWNER)，2=普通员工(STAFF)
     */
    private Integer roleType;

    /**
     * 角色类型名称
     */
    private String roleTypeName;

    /**
     * 员工显示名称
     */
    private String displayName;


    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 联系邮箱
     */
    private String email;

    /**
     * 职位名称
     */
    private String jobTitle;

    /**
     * 工号
     */
    private String employeeNo;

    /**
     * 状态：0=已离职，1=在职，2=停用
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 功能权限配置（JSON格式）
     */
    private String permissions;

    /**
     * 入职时间
     */
    private LocalDateTime hiredAt;

    /**
     * 离职时间
     */
    private LocalDateTime resignedAt;

    /**
     * 备注信息
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建人名称
     */
    private String createdByName;
}