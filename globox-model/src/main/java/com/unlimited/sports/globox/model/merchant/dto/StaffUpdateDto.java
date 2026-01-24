package com.unlimited.sports.globox.model.merchant.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * 更新员工信息DTO
 * @since 2026-01-23
 */
@Data
public class StaffUpdateDto {

    /**
     * 员工ID
     */
    @NotNull(message = "员工ID不能为空")
    private Long venueStaffId;

    /**
     * 所属场馆ID
     */
    private Long venueId;

    /**
     * 角色类型：1=场馆负责人(OWNER)，2=普通员工(STAFF)
     */
    @Min(value = 1, message = "角色类型必须为1或2")
    @Max(value = 2, message = "角色类型必须为1或2")
    private Integer roleType;

    /**
     * 员工显示名称
     */
    private String displayName;

    /**
     * 联系电话
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入有效的手机号码")
    private String contactPhone;

    /**
     * 联系邮箱
     */
    @Size(max = 64, message = "邮箱长度不能超过64字符")
    private String email;

    /**
     * 职位名称
     */
    @Size(max = 50, message = "职位名称长度不能超过50字符")
    private String jobTitle;

    /**
     * 工号
     */
    @Size(max = 50, message = "工号长度不能超过50字符")
    private String employeeNo;

    /**
     * 状态：0=已离职，1=在职，2=停用
     */
    @Min(value = 0, message = "状态必须为0-2之间")
    @Max(value = 2, message = "状态必须为0-2之间")
    private Integer status;

    /**
     * 入职时间
     */
    private LocalDateTime hiredAt;

    /**
     * 离职时间（当status=0时必填）
     */
    private LocalDateTime resignedAt;

    /**
     * 功能权限配置（JSON格式）
     */
    private String permissions;

    /**
     * 备注信息
     */
    @Size(max = 500, message = "备注长度不能超过500字符")
    private String remark;
}