package com.unlimited.sports.globox.model.social.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 草稿列表项视图
 */
@Data
@Schema(description = "草稿列表项视图")
public class DraftNoteItemVo {

    @Schema(description = "笔记ID", example = "1")
    private Long noteId;

    @Schema(description = "标题", example = "今天打球收获分享")
    private String title;

    @Schema(description = "正文（截断）", example = "今天练习了发球和截击...")
    private String content;

    @Schema(description = "封面图URL", example = "https://cdn.example.com/note/1.jpg")
    private String coverUrl;

    @Schema(description = "媒体类型", example = "IMAGE")
    private String mediaType;

    @Schema(description = "更新时间", example = "2025-12-28T10:00:00")
    private LocalDateTime updatedAt;
}

