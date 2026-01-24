package com.unlimited.sports.globox.model.merchant.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 查询员工列表DTO
 * @since 2026-01-23
 */
@Data
public class QueryStaffDto {

    /**
     * 场馆ID（可选，筛选指定场馆的员工）
     */
    private Long venueId;

    /**
     * 员工状态：0=已离职，1=在职，2=停用
     */
    private Integer status;

    /**
     * 角色类型：1=场馆负责人(OWNER)，2=普通员工(STAFF)
     */
    private Integer roleType;

    /**
     * 员工显示名称（模糊查询）
     */
    private String displayName;

    /**
     * 职位名称（模糊查询）
     */
    private String jobTitle;

    /**
     * 工号（精确查询）
     */
    private String employeeNo;

    /**
     * 页码
     */
    @Min(value = 1, message = "页码必须大于0")
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小必须大于0")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer pageSize = 20;
}