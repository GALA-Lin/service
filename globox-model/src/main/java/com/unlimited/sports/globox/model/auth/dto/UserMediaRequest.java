package com.unlimited.sports.globox.model.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 用户媒体请求项
 */
@Data
@Schema(description = "用户媒体请求项")
public class UserMediaRequest {

    @NotNull(message = "媒体类型不能为空")
    @Pattern(regexp = "(?i)^(IMAGE|VIDEO)$", message = "媒体类型必须是 IMAGE 或 VIDEO")
    @Schema(description = "媒体类型", example = "IMAGE", required = true, allowableValues = {"IMAGE", "VIDEO"})
    private String mediaType;

    @Schema(description = "媒体文件URL", example = "https://cdn.example.com/media/1.jpg", required = true)
    private String url;

    @Schema(description = "视频封面URL（视频类型必填，前端保证）", example = "https://cdn.example.com/media/1-cover.jpg")
    private String coverUrl;

    @Schema(description = "视频时长（秒，仅视频类型）", example = "12")
    private Integer duration;

    @Schema(description = "文件大小（字节）", example = "2456789")
    private Long size;

    @Schema(description = "展示排序", example = "1")
    private Integer sort;
}

