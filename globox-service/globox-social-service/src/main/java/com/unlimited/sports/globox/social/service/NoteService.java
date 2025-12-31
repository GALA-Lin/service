package com.unlimited.sports.globox.social.service;

import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.social.dto.CreateNoteRequest;
import com.unlimited.sports.globox.model.social.dto.UpdateNoteRequest;
import com.unlimited.sports.globox.model.social.vo.CursorPaginationResult;
import com.unlimited.sports.globox.model.social.vo.DraftNoteVo;
import com.unlimited.sports.globox.model.social.vo.NoteDetailVo;
import com.unlimited.sports.globox.model.social.vo.NoteListVo;

/**
 * 笔记服务接口
 */
public interface NoteService {

    R<Long> createNote(Long userId, CreateNoteRequest request);

    R<String> updateNote(Long userId, Long noteId, UpdateNoteRequest request);

    R<String> deleteNote(Long userId, Long noteId);

    R<NoteDetailVo> getNoteDetail(Long noteId);

    R<PaginationResult<NoteListVo>> getNoteList(Integer page, Integer pageSize);

    R<PaginationResult<NoteListVo>> getMyNotes(Long userId, Integer page, Integer pageSize);

    R<DraftNoteVo> getDraft(Long userId);

    /**
     * 获取最新流笔记列表（游标分页）
     *
     * @param cursor 游标（可选）
     * @param size   每页数量
     * @return 游标分页结果
     */
    R<CursorPaginationResult<NoteListVo>> getNoteListLatest(String cursor, Integer size);

    /**
     * 获取推荐池子流笔记列表（随机但稳定）
     *
     * @param seed   随机种子（必填）
     * @param cursor 游标（可选）
     * @param size   每页数量
     * @return 游标分页结果
     */
    R<CursorPaginationResult<NoteListVo>> getNoteListPool(Long seed, String cursor, Integer size);
}
