package com.unlimited.sports.globox.model.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户媒体视图
 */
@Data
@Schema(description = "用户媒体视图")
public class UserMediaVo {

    @Schema(description = "媒体ID", example = "1")
    private Long mediaId;

    @Schema(description = "媒体类型", example = "IMAGE")
    private String mediaType;

    @Schema(description = "媒体文件URL", example = "https://cdn.example.com/media/1.jpg")
    private String url;

    @Schema(description = "视频封面URL", example = "https://cdn.example.com/media/1-cover.jpg")
    private String coverUrl;

    @Schema(description = "视频时长（秒）", example = "12")
    private Integer duration;

    @Schema(description = "文件大小（字节）", example = "2456789")
    private Long size;

    @Schema(description = "展示排序", example = "1")
    private Integer sort;
}

