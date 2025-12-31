package com.unlimited.sports.globox.social.controller;

import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.result.SocialCode;
import com.unlimited.sports.globox.model.social.dto.CreateNoteRequest;
import com.unlimited.sports.globox.model.social.dto.UpdateNoteRequest;
import com.unlimited.sports.globox.model.social.vo.CursorPaginationResult;
import com.unlimited.sports.globox.model.social.vo.DraftNoteVo;
import com.unlimited.sports.globox.model.social.vo.NoteDetailVo;
import com.unlimited.sports.globox.model.social.vo.NoteListVo;
import com.unlimited.sports.globox.social.service.NoteService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 笔记控制器
 */
@RestController
@RequestMapping("/social/notes")
@Tag(name = "笔记模块", description = "笔记发布、编辑、删除、查询接口")
@SecurityRequirement(name = "bearerAuth")
public class NoteController {

    @Autowired
    private NoteService noteService;

    @GetMapping
    @Operation(summary = "获取笔记列表", description = "探索页笔记列表（最新流），仅返回 PUBLISHED 状态，按时间倒序，支持游标分页")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "3009", description = "游标格式错误"),
            @ApiResponse(responseCode = "3011", description = "每页数量不能超过50"),
            @ApiResponse(responseCode = "3012", description = "排序方式无效"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<CursorPaginationResult<NoteListVo>> getNoteList(
            @Parameter(description = "排序方式（latest-最新，默认latest）", example = "latest")
            @RequestParam(value = "sort", required = false, defaultValue = "latest") String sort,
            @Parameter(description = "游标（可选，格式：2025-12-28T10:00:00|123）", example = "2025-12-28T10:00:00|123")
            @RequestParam(value = "cursor", required = false) String cursor,
            @Parameter(description = "每页数量（默认10，最大50）", example = "10")
            @RequestParam(value = "size", required = false) Integer size) {
        // 校验sort参数
        if (!"latest".equals(sort)) {
            throw new GloboxApplicationException(SocialCode.NOTE_SORT_INVALID);
        }
        return noteService.getNoteListLatest(cursor, size);
    }

    @GetMapping("/pool")
    @Operation(summary = "获取池子流笔记列表", description = "获取精选池笔记列表（随机），支持游标分页")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "3009", description = "游标格式错误"),
            @ApiResponse(responseCode = "3010", description = "池子流必须提供seed参数"),
            @ApiResponse(responseCode = "3011", description = "每页数量不能超过50"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<CursorPaginationResult<NoteListVo>> getNoteListPool(
            @Parameter(description = "随机种子（必填，建议使用时间戳）", example = "1735286400000", required = true)
            @RequestParam(value = "seed") Long seed,
            @Parameter(description = "游标（可选，格式：2025-12-28T10:00:00|123）", example = "2025-12-28T10:00:00|123")
            @RequestParam(value = "cursor", required = false) String cursor,
            @Parameter(description = "每页数量（默认10，最大50）", example = "10")
            @RequestParam(value = "size", required = false) Integer size) {
        return noteService.getNoteListPool(seed, cursor, size);
    }

    @GetMapping("/{noteId}")
    @Operation(summary = "获取笔记详情", description = "获取笔记完整信息（包含媒体列表）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "3006", description = "笔记不存在或已删除"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<NoteDetailVo> getNoteDetail(
            @Parameter(description = "笔记ID", example = "1", required = true)
            @PathVariable Long noteId) {
        return noteService.getNoteDetail(noteId);
    }

    @GetMapping("/mine")
    @Operation(summary = "获取我的笔记列表", description = "获取当前用户的笔记列表（排除 DELETED 状态），支持分页")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<PaginationResult<NoteListVo>> getMyNotes(
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @Parameter(description = "页码（从1开始）", example = "1")
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "10")
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return noteService.getMyNotes(userId, page, pageSize);
    }

    @GetMapping("/draft")
    @Operation(summary = "获取我的草稿", description = "获取当前用户的草稿（每个用户仅保留1份）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功（无草稿时返回null）"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<DraftNoteVo> getDraft(
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId) {
        return noteService.getDraft(userId);
    }

    @PostMapping
    @Operation(summary = "创建笔记", description = "发布笔记或保存草稿（通过 status 区分：PUBLISHED/ DRAFT），草稿会覆盖旧草稿")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "3001", description = "正文不能为空"),
            @ApiResponse(responseCode = "3002", description = "媒体不能为空"),
            @ApiResponse(responseCode = "3003", description = "图片最多9张，视频仅1条"),
            @ApiResponse(responseCode = "3004", description = "媒体类型不合法"),
            @ApiResponse(responseCode = "3005", description = "视频必须提供封面图"),
            @ApiResponse(responseCode = "3008", description = "笔记状态无效"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<Long> createNote(
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @Parameter(description = "创建笔记请求", required = true)
            @Validated @RequestBody CreateNoteRequest request) {
        return noteService.createNote(userId, request);
    }

    @PutMapping("/{noteId}")
    @Operation(summary = "更新笔记", description = "更新笔记内容，媒体列表全量替换（传空列表将清空媒体）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "3001", description = "正文不能为空"),
            @ApiResponse(responseCode = "3003", description = "图片最多9张，视频仅1条"),
            @ApiResponse(responseCode = "3004", description = "媒体类型不合法"),
            @ApiResponse(responseCode = "3005", description = "视频必须提供封面图"),
            @ApiResponse(responseCode = "3006", description = "笔记不存在或已删除"),
            @ApiResponse(responseCode = "3007", description = "无权限操作"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<String> updateNote(
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @Parameter(description = "笔记ID", example = "1", required = true)
            @PathVariable Long noteId,
            @Parameter(description = "更新笔记请求", required = true)
            @Validated @RequestBody UpdateNoteRequest request) {
        return noteService.updateNote(userId, noteId, request);
    }

    @DeleteMapping("/{noteId}")
    @Operation(summary = "删除笔记", description = "软删除笔记（状态置为 DELETED），仅作者可操作")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "3006", description = "笔记不存在或已删除"),
            @ApiResponse(responseCode = "3007", description = "无权限操作"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<String> deleteNote(
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @Parameter(description = "笔记ID", example = "1", required = true)
            @PathVariable Long noteId) {
        return noteService.deleteNote(userId, noteId);
    }
}
