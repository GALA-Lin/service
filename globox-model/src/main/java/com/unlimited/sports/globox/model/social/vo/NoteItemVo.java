package com.unlimited.sports.globox.model.social.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记列表项视图
 */
@Data
@Schema(description = "笔记列表项视图")
public class NoteItemVo {

    @Schema(description = "笔记ID", example = "1")
    private Long noteId;

    @Schema(description = "作者ID", example = "1")
    private Long userId;

    @Schema(description = "标题", example = "今天打球收获分享")
    private String title;

    @Schema(description = "正文（截断）", example = "今天练习了发球和截击...")
    private String content;

    @Schema(description = "封面图URL（首图或视频封面）", example = "https://cdn.example.com/note/1.jpg")
    private String coverUrl;

    @Schema(description = "媒体类型", example = "IMAGE")
    private String mediaType;

    @Schema(description = "点赞数", example = "10")
    private Integer likeCount;

    @Schema(description = "评论数", example = "5")
    private Integer commentCount;

    @Schema(description = "状态", example = "PUBLISHED")
    private String status;

    @Schema(description = "创建时间", example = "2025-12-26T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "当前用户是否已点赞", example = "true")
    private Boolean liked;
}

