package com.unlimited.sports.globox.model.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 设置球盒号请求
 */
@Data
@Schema(description = "设置球盒号请求")
public class SetUsernameRequest {

    @NotBlank(message = "球盒号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9]{4,20}$", message = "球盒号格式不正确，仅支持4-20位字母和数字")
    @Schema(description = "球盒号（4-20位字母或数字，大小写不敏感，每隔60天可自行修改）",
            example = "GloboxPlayer123", 
            required = true)
    private String username;
}


