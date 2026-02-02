package com.unlimited.sports.globox.social.controller;

import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.social.vo.NoteDetailVo;
import com.unlimited.sports.globox.social.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 帖子分享页接口（公开访问）
 */
@RestController
@RequestMapping("/share/social/notes")
@Tag(name = "帖子分享", description = "帖子分享页公开接口")
public class ShareNoteController {

    @Autowired
    private NoteService noteService;

    @GetMapping("/{noteId}")
    @Operation(summary = "获取帖子分享详情", description = "公开访问的帖子详情页接口")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "3006", description = "帖子不存在或已删除")
    })
    public R<NoteDetailVo> getSharedNoteDetail(
            @Parameter(description = "帖子ID", example = "1", required = true)
            @PathVariable Long noteId) {
        R<NoteDetailVo> result = noteService.getNoteDetail(noteId, null);
        if (result.success() && result.getData() != null && result.getData().getLiked() == null) {
            result.getData().setLiked(false);
        }
        return result;
    }
}
