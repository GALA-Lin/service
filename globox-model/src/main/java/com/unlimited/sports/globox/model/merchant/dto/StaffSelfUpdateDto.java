package com.unlimited.sports.globox.model.merchant.dto;

import lombok.Data;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 员工自助修改信息DTO
 * 员工只能修改自己的部分信息
 * @since 2026-01-26
 */
@Data
public class StaffSelfUpdateDto {

    /**
     * 员工显示名称
     */
    @Size(max = 50, message = "显示名称长度不能超过50字符")
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
}