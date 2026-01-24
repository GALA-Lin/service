package com.unlimited.sports.globox.model.merchant.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 员工简要信息VO（用于列表展示）
 * @since 2026-01-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffSimpleVo {

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
     * 联系电话
     */
    private String contactPhone;

    /**
     * 联系邮箱
     */
    private String email;

    /**
     * 场馆名称
     */
    private String venueName;

    /**
     * 员工显示名称
     */
    private String displayName;

    /**
     * 职位名称
     */
    private String jobTitle;

    /**
     * 工号
     */
    private String employeeNo;

    /**
     * 角色类型名称
     */
    private String roleTypeName;

    /**
     * 状态：0=已离职，1=在职，2=停用
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 入职时间
     */
    private LocalDateTime hiredAt;
}