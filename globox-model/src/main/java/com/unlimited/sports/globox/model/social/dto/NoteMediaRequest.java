package com.unlimited.sports.globox.model.social.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 笔记媒体请求项
 */
@Data
@Schema(description = "笔记媒体请求项")
public class NoteMediaRequest {

    @NotBlank(message = "媒体URL不能为空")
    @Schema(description = "媒体文件URL", example = "https://cdn.example.com/note/1.jpg", required = true)
    private String url;

    @Schema(description = "视频封面URL（视频类型必填）", example = "https://cdn.example.com/note/1-cover.jpg")
    private String coverUrl;

    @NotNull(message = "排序不能为空")
    @Schema(description = "展示排序", example = "1", required = true)
    private Integer sort;
}
