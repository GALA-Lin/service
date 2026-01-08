package com.unlimited.sports.globox.social.controller;

import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.social.dto.PublishNoteRequest;
import com.unlimited.sports.globox.model.social.dto.SaveDraftRequest;
import com.unlimited.sports.globox.model.social.dto.UpdateNoteRequest;
import com.unlimited.sports.globox.model.social.vo.CursorPaginationResult;
import com.unlimited.sports.globox.model.social.vo.DraftNoteVo;
import com.unlimited.sports.globox.model.social.vo.NoteDetailVo;
import com.unlimited.sports.globox.model.social.vo.NoteItemVo;
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

    @GetMapping("/feed")
    @Operation(summary = "获取探索页笔记流", description = "统一探索接口，支持推荐/最新/最热三种排序方式，仅返回已发布（PUBLISHED）状态的笔记")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "3009", description = "游标格式错误"),
            @ApiResponse(responseCode = "3011", description = "每页数量不能超过50"),
            @ApiResponse(responseCode = "3012", description = "排序方式无效"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<CursorPaginationResult<NoteItemVo>> getNoteFeed(
            @Parameter(description = "排序方式：pool-推荐，latest-最新，hot-最热", example = "pool", required = true)
            @RequestParam(value = "sort") String sort,
            @Parameter(description = "游标（带前缀，例如：pool|123456|2025-12-28T10:00:00|123）")
            @RequestParam(value = "cursor", required = false) String cursor,
            @Parameter(description = "每页数量（默认10，最大50）", example = "10")
            @RequestParam(value = "size", required = false) Integer size,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(value = RequestHeaderConstants.HEADER_USER_ID, required = false) Long userId) {
        return noteService.getNoteFeed(sort, cursor, size, userId);
    }

    @GetMapping("/home")
    @Operation(summary = "获取首页推荐笔记", description = "获取首页推荐（编辑精选）笔记，seed由后端生成并写入cursor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "3009", description = "游标格式错误"),
            @ApiResponse(responseCode = "3011", description = "每页数量不能超过50"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<CursorPaginationResult<NoteItemVo>> getHomeNotes(
            @Parameter(description = "游标（带前缀，格式：pool|{seed}|{createdAt}|{noteId}，例如：pool|123456|2025-12-28T10:00:00|123）")
            @RequestParam(value = "cursor", required = false) String cursor,
            @Parameter(description = "每页数量（默认10，最大50）", example = "10")
            @RequestParam(value = "size", required = false) Integer size,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(value = RequestHeaderConstants.HEADER_USER_ID, required = false) Long userId) {
        return noteService.getHomeNotes(cursor, size, userId);
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
            @PathVariable Long noteId,
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(value = RequestHeaderConstants.HEADER_USER_ID, required = false) Long userId) {
        return noteService.getNoteDetail(noteId, userId);
    }

    @GetMapping("/mine")
    @Operation(summary = "获取我的笔记列表", description = "获取当前用户的笔记列表（排除 DELETED 状态），支持分页")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<PaginationResult<NoteItemVo>> getMyNotes(
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

    @PostMapping("/draft")
    @Operation(summary = "保存草稿", description = "保存或更新草稿（upsert），每个用户只保留一条草稿。标题、正文、媒体至少填写一项")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "保存成功，返回笔记ID"),
            @ApiResponse(responseCode = "3018", description = "草稿不能完全为空"),
            @ApiResponse(responseCode = "3003", description = "图片最多9张，视频仅1条"),
            @ApiResponse(responseCode = "3004", description = "媒体类型不合法"),
            @ApiResponse(responseCode = "3005", description = "视频必须提供封面图"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<Long> saveDraft(
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @Parameter(description = "保存草稿请求", required = true)
            @Validated @RequestBody SaveDraftRequest request) {
        return noteService.saveDraft(userId, request);
    }

    @PostMapping("/publish")
    @Operation(summary = "发布笔记", description = "直接发布新笔记，创建 PUBLISHED 状态的笔记。发布成功后自动清理用户所有草稿。正文和媒体列表必填")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "发布成功，返回新创建的笔记ID"),
            @ApiResponse(responseCode = "3001", description = "正文不能为空"),
            @ApiResponse(responseCode = "3020", description = "发布失败：媒体列表不能为空"),
            @ApiResponse(responseCode = "3003", description = "图片最多9张，视频仅1条"),
            @ApiResponse(responseCode = "3004", description = "媒体类型不合法"),
            @ApiResponse(responseCode = "3005", description = "视频必须提供封面图"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<Long> publishNote(
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @Parameter(description = "发布笔记请求", required = true)
            @Validated @RequestBody PublishNoteRequest request) {
        return noteService.publishNote(userId, request);
    }

    @PutMapping("/{noteId}")
    @Operation(summary = "更新笔记", description = "更新草稿或已发布笔记。草稿：标题/正文/媒体至少一项非空；已发布：正文必填。mediaList=null 不更新媒体，mediaList=[] 报错（清空请使用删除媒体接口）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "3001", description = "正文不能为空（已发布笔记）"),
            @ApiResponse(responseCode = "3018", description = "草稿不能完全为空"),
            @ApiResponse(responseCode = "3016", description = "不允许清空笔记的所有媒体"),
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

    @GetMapping("/liked")
    @Operation(summary = "获取我点赞的笔记列表", description = "获取当前登录用户点赞的笔记列表，按点赞时间倒序排列，支持游标分页")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "3009", description = "游标格式错误"),
            @ApiResponse(responseCode = "3011", description = "每页数量不能超过50"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<CursorPaginationResult<NoteItemVo>> getLikedNotes(
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @Parameter(description = "游标（可选，格式：{likeCreatedAt}|{likeId}，例如：2025-12-28T10:00:00|123）", example = "2025-12-28T10:00:00|123")
            @RequestParam(value = "cursor", required = false) String cursor,
            @Parameter(description = "每页数量（默认10，最大50）", example = "10")
            @RequestParam(value = "size", required = false) Integer size) {
        return noteService.getLikedNotes(userId, cursor, size);
    }

    @PostMapping("/{noteId}/like")
    @Operation(summary = "点赞笔记", description = "对已发布笔记进行点赞，重复点赞视为成功（幂等）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "点赞成功"),
            @ApiResponse(responseCode = "3006", description = "笔记不存在或已删除"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<String> likeNote(
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @Parameter(description = "笔记ID", example = "1", required = true)
            @PathVariable Long noteId) {
        return noteService.likeNote(userId, noteId);
    }

    @DeleteMapping("/{noteId}/like")
    @Operation(summary = "取消点赞", description = "取消对已发布笔记的点赞，未点赞时也返回成功（幂等）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "取消点赞成功"),
            @ApiResponse(responseCode = "3006", description = "笔记不存在或已删除"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<String> unlikeNote(
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @Parameter(description = "笔记ID", example = "1", required = true)
            @PathVariable Long noteId) {
        return noteService.unlikeNote(userId, noteId);
    }

    @DeleteMapping("/{noteId}/media/{mediaId}")
    @Operation(summary = "删除笔记媒体", description = "删除笔记中的单个媒体，删除后不能使媒体数量为0，仅作者可操作")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "3006", description = "笔记不存在或已删除"),
            @ApiResponse(responseCode = "3007", description = "无权限操作"),
            @ApiResponse(responseCode = "3016", description = "不允许清空笔记的所有媒体"),
            @ApiResponse(responseCode = "3017", description = "媒体不存在"),
            @ApiResponse(responseCode = "2021", description = "无效的Token")
    })
    public R<String> deleteNoteMedia(
            @Parameter(description = "用户ID（由网关自动注入）", hidden = false)
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId,
            @Parameter(description = "笔记ID", example = "1", required = true)
            @PathVariable Long noteId,
            @Parameter(description = "媒体ID", example = "1", required = true)
            @PathVariable Long mediaId) {
        return noteService.deleteNoteMedia(userId, noteId, mediaId);
    }
}
