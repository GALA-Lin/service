package com.unlimited.sports.globox.social.service;

import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.social.dto.PublishNoteRequest;
import com.unlimited.sports.globox.model.social.dto.SaveDraftRequest;
import com.unlimited.sports.globox.model.social.dto.UpdateNoteRequest;
import com.unlimited.sports.globox.model.social.vo.CursorPaginationResult;
import com.unlimited.sports.globox.model.social.vo.DraftNoteVo;
import com.unlimited.sports.globox.model.social.vo.NoteDetailVo;
import com.unlimited.sports.globox.model.social.vo.NoteItemVo;

/**
 * 笔记服务接口
 */
public interface NoteService {

    /**
     * 保存草稿（upsert）
     * 规则：title/content/media 至少一个非空，否则返回 NOTE_DRAFT_EMPTY
     * 每个用户只保留一条草稿，有则更新，无则插入
     */
    R<Long> saveDraft(Long userId, SaveDraftRequest request);

    /**
     * 直接发布新笔记
     * 规则：创建 PUBLISHED 状态的笔记，强校验 content 和 mediaList 必填
     * 发布成功后自动清理用户所有草稿
     *
     * @param userId  用户ID
     * @param request 发布请求
     * @return 新创建的笔记ID
     */
    R<Long> publishNote(Long userId, PublishNoteRequest request);

    R<String> updateNote(Long userId, Long noteId, UpdateNoteRequest request);

    R<String> deleteNote(Long userId, Long noteId);

    R<NoteDetailVo> getNoteDetail(Long noteId, Long userId);

    R<PaginationResult<NoteItemVo>> getNoteList(Integer page, Integer pageSize);

    R<PaginationResult<NoteItemVo>> getMyNotes(Long userId, Integer page, Integer pageSize);

    R<DraftNoteVo> getDraft(Long userId);

    /**
     * 获取最新流笔记列表（游标分页）
     *
     * @param cursor 游标（可选）
     * @param size   每页数量
     * @param userId 用户ID（可选，用于查询 liked 状态）
     * @return 游标分页结果
     */
    R<CursorPaginationResult<NoteItemVo>> getNoteListLatest(String cursor, Integer size, Long userId);

    /**
     * 获取推荐池子流笔记列表（随机但稳定）
     *
     * @param cursor 游标（可选，包含seed）
     * @param size   每页数量
     * @param userId 用户ID（可选，用于查询 liked 状态）
     * @return 游标分页结果
     */
    R<CursorPaginationResult<NoteItemVo>> getNoteListPool(String cursor, Integer size, Long userId);

    /**
     * 获取最热流笔记列表
     *
     * @param cursor 游标（可选）
     * @param size   每页数量
     * @param userId 用户ID（可选，用于查询 liked 状态）
     * @return 游标分页结果
     */
    R<CursorPaginationResult<NoteItemVo>> getNoteListHot(String cursor, Integer size, Long userId);

    /**
     * 获取我点赞的笔记列表
     *
     * @param userId 用户ID
     * @param cursor 游标（可选）
     * @param size   每页数量
     * @return 游标分页结果
     */
    R<CursorPaginationResult<NoteItemVo>> getLikedNotes(Long userId, String cursor, Integer size);

    /**
     * 点赞笔记
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @return 成功提示
     */
    R<String> likeNote(Long userId, Long noteId);

    /**
     * 取消点赞
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @return 成功提示
     */
    R<String> unlikeNote(Long userId, Long noteId);

    /**
     * 删除笔记媒体
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param mediaId 媒体ID
     * @return 成功提示
     */
    R<String> deleteNoteMedia(Long userId, Long noteId, Long mediaId);

    /**
     * 获取探索页笔记流（统一入口）
     *
     * @param sort   排序方式：pool-推荐，latest-最新，hot-最热
     * @param cursor 游标（可选，带前缀）
     * @param size   每页数量
     * @param userId 用户ID（可选，用于查询 liked 状态）
     * @return 游标分页结果
     */
    R<CursorPaginationResult<NoteItemVo>> getNoteFeed(String sort, String cursor, Integer size, Long userId);

    /**
     * 获取首页推荐笔记（编辑精选）
     *
     * @param cursor 游标（可选，带前缀）
     * @param size   每页数量
     * @param userId 用户ID（可选，用于查询 liked 状态）
     * @return 游标分页结果
     */
    R<CursorPaginationResult<NoteItemVo>> getHomeNotes(String cursor, Integer size, Long userId);
}
