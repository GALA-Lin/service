package com.unlimited.sports.globox.social.controller;

import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.social.vo.CommentItemVo;
import com.unlimited.sports.globox.model.social.vo.CursorPaginationResult;
import com.unlimited.sports.globox.social.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 帖子评论分享页接口（公开访问）
 */
@RestController
@RequestMapping("/share/social/notes/{noteId}/comments")
@Tag(name = "帖子评论分享", description = "帖子评论分享页公开接口")
public class ShareNoteCommentController {

    @Autowired
    private CommentService commentService;

    @GetMapping
    @Operation(summary = "获取帖子评论分享列表", description = "公开访问的帖子评论列表接口")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "3006", description = "帖子不存在或已删除"),
            @ApiResponse(responseCode = "3026", description = "评论游标格式错误"),
            @ApiResponse(responseCode = "3027", description = "每页数量不能超过50")
    })
    public R<CursorPaginationResult<CommentItemVo>> getSharedCommentList(
            @Parameter(description = "帖子ID", example = "1", required = true)
            @PathVariable("noteId") Long noteId,
            @Parameter(description = "游标")
            @RequestParam(value = "cursor", required = false) String cursor,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(value = "size", required = false) Integer size) {
        R<CursorPaginationResult<CommentItemVo>> result = commentService.getCommentList(noteId, cursor, size, null);
        if (result.success() && result.getData() != null) {
            List<CommentItemVo> list = result.getData().getList();
            if (list != null) {
                for (CommentItemVo item : list) {
                    if (item != null && item.getLiked() == null) {
                        item.setLiked(false);
                    }
                }
            }
        }
        return result;
    }
}
