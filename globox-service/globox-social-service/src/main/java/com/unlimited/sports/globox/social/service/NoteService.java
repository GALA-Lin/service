package com.unlimited.sports.globox.social.service;

import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.social.dto.DirectPublishNoteRequest;
import com.unlimited.sports.globox.model.social.dto.PublishNoteRequest;
import com.unlimited.sports.globox.model.social.dto.SaveDraftRequest;
import com.unlimited.sports.globox.model.social.dto.UpdateNoteRequest;
import com.unlimited.sports.globox.model.social.enums.NoteTag;
import com.unlimited.sports.globox.model.social.vo.CursorPaginationResult;
import com.unlimited.sports.globox.model.social.vo.DraftNoteItemVo;
import com.unlimited.sports.globox.model.social.vo.DraftNoteVo;
import com.unlimited.sports.globox.model.social.vo.NoteDetailVo;
import com.unlimited.sports.globox.model.social.vo.NoteItemVo;

import java.util.List;

/**
 * 笔记服务接口
 */
public interface NoteService {

    /**
     * 保存草稿（支持新建和更新）
     * 规则：标题/正文/媒体至少一项非空；mediaList=null 不更新媒体；mediaList=[] 允许清空媒体（仅草稿）
     * 如果传 noteId 则更新该草稿，否则新建草稿
     */
    R<Long> saveDraft(Long userId, SaveDraftRequest request);

    /**
     * 直接发布笔记（不含 noteId）
     * 强校验 content 和 mediaList 必填，新建 PUBLISHED 状态的笔记
     */
    R<Long> directPublishNote(Long userId, DirectPublishNoteRequest request);

    /**
     * 草稿转正发布
     * 规则：必须传 noteId（草稿ID），将该草稿状态改为 PUBLISHED
     * 强校验 content 和 mediaList 必填
     */
    R<Long> publishDraftNote(Long userId, PublishNoteRequest request);

    R<String> updateNote(Long userId, Long noteId, UpdateNoteRequest request);

    R<String> deleteNote(Long userId, Long noteId);

    R<NoteDetailVo> getNoteDetail(Long noteId, Long userId);

    R<PaginationResult<NoteItemVo>> getNoteList(Integer page, Integer pageSize);

    R<PaginationResult<NoteItemVo>> getMyNotes(Long userId, Integer page, Integer pageSize);

    R<PaginationResult<DraftNoteItemVo>> getDrafts(Long userId, Integer page, Integer pageSize);

    /**
     * 获取最新一条草稿
     */
    R<DraftNoteVo> getDraft(Long userId, Long noteId);

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
     * 获取推荐池子流笔记列表
     *
     * @param cursor 游标（可选，包含seed）
     * @param size   每页数量
     * @param userId 用户ID（可选，用于查询 liked 状态）
     * @return 游标分页结果
     */
    R<CursorPaginationResult<NoteItemVo>> getNoteListPool(String cursor, Integer size, Long userId);

    /**
     * 获取指定用户的已发布笔记（用于他人主页，带拉黑校验）
     */
    R<PaginationResult<NoteItemVo>> getUserNotes(Long targetUserId, Integer page, Integer pageSize, Long viewerId);

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
     * 获取指定用户点赞的笔记列表（用于他人主页，带拉黑校验）
     */
    R<CursorPaginationResult<NoteItemVo>> getUserLikedNotes(Long targetUserId, String cursor, Integer size, Long viewerId);

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

    /**
     * 获取所有可用的笔记标签
     *
     * @return 标签字典列表
     */
    R<List<NoteTag.DictItem>> getNoteTags();

    /**
     * 批量设置笔记精选状态
     *
     * @param noteIds  笔记ID列表
     * @param featured 是否精选
     * @return 操作结果
     */
    R<String> setNotesFeatured(List<Long> noteIds, Boolean featured);
}
