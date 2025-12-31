package com.unlimited.sports.globox.model.social.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 笔记媒体视图
 */
@Data
@Schema(description = "笔记媒体视图")
public class NoteMediaVo {

    @Schema(description = "媒体ID", example = "1")
    private Long mediaId;

    @Schema(description = "媒体类型", example = "IMAGE")
    private String mediaType;

    @Schema(description = "媒体文件URL", example = "https://cdn.example.com/note/1.jpg")
    private String url;

    @Schema(description = "视频封面URL", example = "https://cdn.example.com/note/1-cover.jpg")
    private String coverUrl;

    @Schema(description = "展示排序", example = "1")
    private Integer sort;
}
