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
public class SetGloboxNoRequest {

    @NotBlank(message = "球盒号不能为空")
    @Pattern(regexp = "^\\d{9}$", message = "球盒号格式不正确，仅支持9位数字")
    @Schema(description = "球盒号（9位数字，按时间递增）",
            example = "260330001",
            required = true)
    private String globoxNo;
}
