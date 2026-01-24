package com.unlimited.sports.globox.social.controller;

import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.social.dto.CreateCommentRequest;
import com.unlimited.sports.globox.model.social.vo.CommentItemVo;
import com.unlimited.sports.globox.model.social.vo.CursorPaginationResult;
import com.unlimited.sports.globox.social.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 笔记评论控制器
 */
@RestController
@RequestMapping("/social/notes/{noteId}/comments")
@Tag(name = "评论模块", description = "笔记评论相关接口")
@SecurityRequirement(name = "bearerAuth")
public class NoteCommentController {

    @Autowired
    private CommentService commentService;

    @GetMapping
    @Operation(summary = "获取评论列表", description = "获取指定笔记的评论列表，支持游标分页，仅返回已发布（PUBLISHED）状态的评论")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "3006", description = "笔记不存在或已删除"),
            @ApiResponse(responseCode = "3026", description = "评论游标格式错误"),
            @ApiResponse(responseCode = "3027", description = "每页数量不能超过50"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<CursorPaginationResult<CommentItemVo>> getCommentList(
            @Parameter(description = "笔记ID", example = "1", required = true)
            @PathVariable("noteId") Long noteId,
            @Parameter(description = "游标（格式：{createdAt}|{commentId}，例如：2025-12-28T10:00:00|123）")
            @RequestParam(value = "cursor", required = false) String cursor,
            @Parameter(description = "每页数量（默认10，最大50）", example = "10")
            @RequestParam(value = "size", required = false) Integer size,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(value = RequestHeaderConstants.HEADER_USER_ID, required = false) Long userId) {
        return commentService.getCommentList(noteId, cursor, size, userId);
    }

    @PostMapping
    @Operation(summary = "发布评论", description = "在指定笔记下发布评论，支持一级评论和回复评论（仅支持一级回复）。评论内容会进行敏感词过滤")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "发布成功"),
            @ApiResponse(responseCode = "3006", description = "笔记不存在或已删除"),
            @ApiResponse(responseCode = "3021", description = "评论内容不能为空"),
            @ApiResponse(responseCode = "3022", description = "评论已关闭"),
            @ApiResponse(responseCode = "3025", description = "父评论不存在或不属于该笔记"),
            @ApiResponse(responseCode = "8004", description = "存在敏感词，请修改后重试"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<Long> createComment(
            @Parameter(description = "笔记ID", example = "1", required = true)
            @PathVariable("noteId") Long noteId,
            @Parameter(description = "创建评论请求", required = true)
            @RequestBody @Validated CreateCommentRequest request,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(value = RequestHeaderConstants.HEADER_USER_ID, required = false) Long userId) {
        return commentService.createComment(userId, noteId, request);
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "删除评论", description = "删除指定评论，评论作者或笔记作者可删除，删除父评论时会级联删除所有回复")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "3006", description = "笔记不存在或已删除"),
            @ApiResponse(responseCode = "3023", description = "评论不存在或已删除"),
            @ApiResponse(responseCode = "3024", description = "无权限删除评论"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<String> deleteComment(
            @Parameter(description = "笔记ID", example = "1", required = true)
            @PathVariable("noteId") Long noteId,
            @Parameter(description = "评论ID", example = "123", required = true)
            @PathVariable("commentId") Long commentId,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(value = RequestHeaderConstants.HEADER_USER_ID, required = false) Long userId) {
        return commentService.deleteComment(userId, noteId, commentId);
    }

    @PostMapping("/{commentId}/like")
    @Operation(summary = "点赞评论", description = "点赞指定评论，支持幂等操作")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "点赞成功"),
            @ApiResponse(responseCode = "3006", description = "笔记不存在或已删除"),
            @ApiResponse(responseCode = "3023", description = "评论不存在或已删除"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<String> likeComment(
            @Parameter(description = "笔记ID", example = "1", required = true)
            @PathVariable("noteId") Long noteId,
            @Parameter(description = "评论ID", example = "123", required = true)
            @PathVariable("commentId") Long commentId,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(value = RequestHeaderConstants.HEADER_USER_ID, required = false) Long userId) {
        return commentService.likeComment(userId, noteId, commentId);
    }

    @DeleteMapping("/{commentId}/like")
    @Operation(summary = "取消点赞评论", description = "取消点赞指定评论")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "取消点赞成功"),
            @ApiResponse(responseCode = "3006", description = "笔记不存在或已删除"),
            @ApiResponse(responseCode = "3023", description = "评论不存在或已删除"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<String> unlikeComment(
            @Parameter(description = "笔记ID", example = "1", required = true)
            @PathVariable("noteId") Long noteId,
            @Parameter(description = "评论ID", example = "123", required = true)
            @PathVariable("commentId") Long commentId,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(value = RequestHeaderConstants.HEADER_USER_ID, required = false) Long userId) {
        return commentService.unlikeComment(userId, noteId, commentId);
    }
}

