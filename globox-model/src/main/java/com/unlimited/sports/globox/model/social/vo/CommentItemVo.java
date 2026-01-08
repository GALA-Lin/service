package com.unlimited.sports.globox.model.social.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论列表项视图
 */
@Data
@Schema(description = "评论列表项视图")
public class CommentItemVo {

    @Schema(description = "评论ID", example = "1")
    private Long commentId;

    @Schema(description = "笔记ID", example = "1")
    private Long noteId;

    @Schema(description = "评论者ID", example = "1")
    private Long userId;

    @Schema(description = "评论者昵称", example = "这个是昵称")
    private String nickName;

    @Schema(description = "评论者头像URL", example = "https://globox-dev-1386561970.cos.ap-chengdu.myqcloud.com/avatar/2026-01-03/c2cdd9219824420fa6c8956f760197de.jpg")
    private String avatarUrl;

    @Schema(description = "评论内容", example = "说得很有道理！")
    private String content;

    @Schema(description = "点赞数", example = "10")
    private Integer likeCount;

    @Schema(description = "创建时间", example = "2025-12-26T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "当前用户是否已点赞", example = "true")
    private Boolean liked;

    @Schema(description = "父评论ID（一级评论为空）", example = "123")
    private Long parentId;

    @Schema(description = "回复对象用户ID", example = "456")
    private Long replyToUserId;

    @Schema(description = "回复对象用户昵称", example = "被回复的用户")
    private String replyToUserName;
}



