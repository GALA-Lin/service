package com.unlimited.sports.globox.model.social.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 草稿笔记视图
 */
@Data
@Schema(description = "草稿笔记视图")
public class DraftNoteVo {

    @Schema(description = "笔记ID", example = "1")
    private Long noteId;

    @Schema(description = "标题", example = "今天打球收获分享")
    private String title;

    @Schema(description = "正文", example = "今天练习了发球和截击，感觉有提升。")
    private String content;

    @Schema(description = "状态", example = "DRAFT")
    private String status;

    @Schema(description = "媒体类型", example = "IMAGE")
    private String mediaType;

    @Schema(description = "媒体列表")
    private List<NoteMediaVo> mediaList;
}
