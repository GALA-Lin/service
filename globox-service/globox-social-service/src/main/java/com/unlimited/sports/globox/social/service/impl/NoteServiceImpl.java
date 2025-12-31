package com.unlimited.sports.globox.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.result.SocialCode;
import com.unlimited.sports.globox.model.social.dto.CreateNoteRequest;
import com.unlimited.sports.globox.model.social.dto.NoteMediaRequest;
import com.unlimited.sports.globox.model.social.dto.UpdateNoteRequest;
import com.unlimited.sports.globox.model.social.entity.SocialNote;
import com.unlimited.sports.globox.model.social.entity.SocialNoteMedia;
import com.unlimited.sports.globox.model.social.entity.SocialNotePool;
import com.unlimited.sports.globox.model.social.vo.CursorPaginationResult;
import com.unlimited.sports.globox.model.social.vo.DraftNoteVo;
import com.unlimited.sports.globox.model.social.vo.NoteDetailVo;
import com.unlimited.sports.globox.model.social.vo.NoteListVo;
import com.unlimited.sports.globox.model.social.vo.NoteMediaVo;
import com.unlimited.sports.globox.social.mapper.SocialNoteMapper;
import com.unlimited.sports.globox.social.mapper.SocialNoteMediaMapper;
import com.unlimited.sports.globox.social.mapper.SocialNotePoolMapper;
import com.unlimited.sports.globox.social.service.NoteService;
import com.unlimited.sports.globox.social.util.CursorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 笔记服务实现
 */
@Service
@Slf4j
public class NoteServiceImpl implements NoteService {

    private static final int MAX_IMAGE_COUNT = 9;
    private static final int MAX_VIDEO_COUNT = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    @Autowired
    private SocialNoteMapper socialNoteMapper;

    @Autowired
    private SocialNoteMediaMapper socialNoteMediaMapper;

    @Autowired
    private SocialNotePoolMapper socialNotePoolMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> createNote(Long userId, CreateNoteRequest request) {
        if (userId == null) {
            return R.error(SocialCode.NOTE_NOT_FOUND);
        }

        if (!StringUtils.hasText(request.getContent())) {
            throw new GloboxApplicationException(SocialCode.NOTE_CONTENT_EMPTY);
        }

        SocialNote.Status status = parseStatus(request.getStatus());
        validateMedia(request.getMediaType(), request.getMediaList());

        if (status == SocialNote.Status.DRAFT) {
            LambdaQueryWrapper<SocialNote> draftQuery = new LambdaQueryWrapper<>();
            draftQuery.eq(SocialNote::getUserId, userId)
                    .eq(SocialNote::getStatus, SocialNote.Status.DRAFT);
            SocialNote oldDraft = socialNoteMapper.selectOne(draftQuery);
            if (oldDraft != null) {
                LambdaQueryWrapper<SocialNoteMedia> mediaDeleteQuery = new LambdaQueryWrapper<>();
                mediaDeleteQuery.eq(SocialNoteMedia::getNoteId, oldDraft.getNoteId());
                socialNoteMediaMapper.delete(mediaDeleteQuery);
                socialNoteMapper.deleteById(oldDraft.getNoteId());
                log.info("草稿已覆盖：userId={}, noteId={}", userId, oldDraft.getNoteId());
            }
        }

        SocialNote note = new SocialNote();
        note.setUserId(userId);
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setStatus(status);
        note.setAllowComment(request.getAllowComment() != null ? request.getAllowComment() : Boolean.TRUE);
        note.setLikeCount(0);
        note.setCommentCount(0);
        note.setCollectCount(0);
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());

        applyMediaSummary(note, request.getMediaType(), request.getMediaList());

        socialNoteMapper.insert(note);

        if (!CollectionUtils.isEmpty(request.getMediaList())) {
            insertMediaList(note.getNoteId(), request.getMediaList(), request.getMediaType());
        }

        log.info("笔记创建成功：userId={}, noteId={}, status={}", userId, note.getNoteId(), status);
        return R.ok(note.getNoteId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> updateNote(Long userId, Long noteId, UpdateNoteRequest request) {
        if (userId == null || noteId == null) {
            return R.error(SocialCode.NOTE_NOT_FOUND);
        }

        SocialNote note = socialNoteMapper.selectById(noteId);
        if (note == null || note.getStatus() == SocialNote.Status.DELETED) {
            return R.error(SocialCode.NOTE_NOT_FOUND);
        }

        if (!note.getUserId().equals(userId)) {
            throw new GloboxApplicationException(SocialCode.NOTE_PERMISSION_DENIED);
        }

        if (!StringUtils.hasText(request.getContent())) {
            throw new GloboxApplicationException(SocialCode.NOTE_CONTENT_EMPTY);
        }

        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setAllowComment(request.getAllowComment() != null ? request.getAllowComment() : note.getAllowComment());
        note.setUpdatedAt(LocalDateTime.now());

        if (CollectionUtils.isEmpty(request.getMediaList())) {
            note.setMediaType(null);
            note.setCoverUrl(null);
        } else {
        validateMedia(request.getMediaType(), request.getMediaList());
            applyMediaSummary(note, request.getMediaType(), request.getMediaList());
        }

        socialNoteMapper.updateById(note);

        LambdaQueryWrapper<SocialNoteMedia> deleteQuery = new LambdaQueryWrapper<>();
        deleteQuery.eq(SocialNoteMedia::getNoteId, noteId);
        socialNoteMediaMapper.delete(deleteQuery);

        if (!CollectionUtils.isEmpty(request.getMediaList())) {
            insertMediaList(noteId, request.getMediaList(), request.getMediaType());
        }

        log.info("笔记更新成功：userId={}, noteId={}", userId, noteId);
        return R.ok("更新成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> deleteNote(Long userId, Long noteId) {
        if (userId == null || noteId == null) {
            return R.error(SocialCode.NOTE_NOT_FOUND);
        }

        SocialNote note = socialNoteMapper.selectById(noteId);
        if (note == null || note.getStatus() == SocialNote.Status.DELETED) {
            return R.error(SocialCode.NOTE_NOT_FOUND);
        }

        if (!note.getUserId().equals(userId)) {
            throw new GloboxApplicationException(SocialCode.NOTE_PERMISSION_DENIED);
        }

        note.setStatus(SocialNote.Status.DELETED);
        note.setUpdatedAt(LocalDateTime.now());
        socialNoteMapper.updateById(note);

        log.info("笔记删除成功：userId={}, noteId={}", userId, noteId);
        return R.ok("删除成功");
    }

    @Override
    public R<NoteDetailVo> getNoteDetail(Long noteId) {
        if (noteId == null) {
            return R.error(SocialCode.NOTE_NOT_FOUND);
        }

        SocialNote note = socialNoteMapper.selectById(noteId);
        if (note == null || note.getStatus() != SocialNote.Status.PUBLISHED) {
            return R.error(SocialCode.NOTE_NOT_FOUND);
        }

        LambdaQueryWrapper<SocialNoteMedia> mediaQuery = new LambdaQueryWrapper<>();
        mediaQuery.eq(SocialNoteMedia::getNoteId, noteId)
                .orderByAsc(SocialNoteMedia::getSort);
        List<SocialNoteMedia> mediaList = socialNoteMediaMapper.selectList(mediaQuery);

        NoteDetailVo vo = new NoteDetailVo();
        BeanUtils.copyProperties(note, vo);
        vo.setStatus(note.getStatus() != null ? note.getStatus().name() : null);
        vo.setMediaType(note.getMediaType() != null ? note.getMediaType().name() : null);

        List<NoteMediaVo> mediaVos = mediaList.stream()
                .map(media -> {
                    NoteMediaVo mediaVo = new NoteMediaVo();
                    BeanUtils.copyProperties(media, mediaVo);
                    mediaVo.setMediaType(media.getMediaType() != null ? media.getMediaType().name() : null);
                    return mediaVo;
                })
                .collect(Collectors.toList());
        vo.setMediaList(mediaVos);

        return R.ok(vo);
    }

    @Override
    public R<PaginationResult<NoteListVo>> getNoteList(Integer page, Integer pageSize) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        LambdaQueryWrapper<SocialNote> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SocialNote::getStatus, SocialNote.Status.PUBLISHED)
                .orderByDesc(SocialNote::getCreatedAt);

        Page<SocialNote> pageParam = new Page<>(page, pageSize);
        IPage<SocialNote> pageResult = socialNoteMapper.selectPage(pageParam, queryWrapper);

        List<NoteListVo> voList = pageResult.getRecords().stream()
                .map(note -> {
                    NoteListVo vo = new NoteListVo();
                    BeanUtils.copyProperties(note, vo);
                    vo.setStatus(note.getStatus() != null ? note.getStatus().name() : null);
                    vo.setMediaType(note.getMediaType() != null ? note.getMediaType().name() : null);
                    if (StringUtils.hasText(note.getContent()) && note.getContent().length() > 100) {
                        vo.setContent(note.getContent().substring(0, 100) + "...");
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        PaginationResult<NoteListVo> result = PaginationResult.build(voList, pageResult.getTotal(), page, pageSize);
        return R.ok(result);
    }

    @Override
    public R<PaginationResult<NoteListVo>> getMyNotes(Long userId, Integer page, Integer pageSize) {
        if (userId == null) {
            return R.error(SocialCode.NOTE_NOT_FOUND);
        }
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        LambdaQueryWrapper<SocialNote> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SocialNote::getUserId, userId)
                .ne(SocialNote::getStatus, SocialNote.Status.DELETED)
                .orderByDesc(SocialNote::getCreatedAt);

        Page<SocialNote> pageParam = new Page<>(page, pageSize);
        IPage<SocialNote> pageResult = socialNoteMapper.selectPage(pageParam, queryWrapper);

        List<NoteListVo> voList = pageResult.getRecords().stream()
                .map(note -> {
                    NoteListVo vo = new NoteListVo();
                    BeanUtils.copyProperties(note, vo);
                    vo.setStatus(note.getStatus() != null ? note.getStatus().name() : null);
                    vo.setMediaType(note.getMediaType() != null ? note.getMediaType().name() : null);
                    if (StringUtils.hasText(note.getContent()) && note.getContent().length() > 100) {
                        vo.setContent(note.getContent().substring(0, 100) + "...");
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        PaginationResult<NoteListVo> result = PaginationResult.build(voList, pageResult.getTotal(), page, pageSize);
        return R.ok(result);
    }

    @Override
    public R<DraftNoteVo> getDraft(Long userId) {
        if (userId == null) {
            return R.error(SocialCode.NOTE_NOT_FOUND);
        }

        LambdaQueryWrapper<SocialNote> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SocialNote::getUserId, userId)
                .eq(SocialNote::getStatus, SocialNote.Status.DRAFT)
                .orderByDesc(SocialNote::getCreatedAt)
                .last("LIMIT 1");
        SocialNote draft = socialNoteMapper.selectOne(queryWrapper);

        if (draft == null) {
            return R.ok(null);
        }

        LambdaQueryWrapper<SocialNoteMedia> mediaQuery = new LambdaQueryWrapper<>();
        mediaQuery.eq(SocialNoteMedia::getNoteId, draft.getNoteId())
                .orderByAsc(SocialNoteMedia::getSort);
        List<SocialNoteMedia> mediaList = socialNoteMediaMapper.selectList(mediaQuery);

        DraftNoteVo vo = new DraftNoteVo();
        vo.setNoteId(draft.getNoteId());
        vo.setTitle(draft.getTitle());
        vo.setContent(draft.getContent());
        vo.setStatus(draft.getStatus() != null ? draft.getStatus().name() : null);
        vo.setMediaType(draft.getMediaType() != null ? draft.getMediaType().name() : null);

        List<NoteMediaVo> mediaVos = mediaList.stream()
                .map(media -> {
                    NoteMediaVo mediaVo = new NoteMediaVo();
                    BeanUtils.copyProperties(media, mediaVo);
                    mediaVo.setMediaType(media.getMediaType() != null ? media.getMediaType().name() : null);
                    return mediaVo;
                })
                .collect(Collectors.toList());
        vo.setMediaList(mediaVos);

        return R.ok(vo);
    }

    private SocialNote.Status parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new GloboxApplicationException(SocialCode.NOTE_STATUS_INVALID);
        }
        try {
            return SocialNote.Status.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new GloboxApplicationException(SocialCode.NOTE_STATUS_INVALID);
        }
    }

    private void validateMedia(String mediaType, List<NoteMediaRequest> mediaList) {
        if (CollectionUtils.isEmpty(mediaList)) {
            throw new GloboxApplicationException(SocialCode.NOTE_MEDIA_EMPTY);
        }

        if (!StringUtils.hasText(mediaType)) {
            throw new GloboxApplicationException(SocialCode.NOTE_MEDIA_TYPE_INVALID);
        }

        try {
            SocialNote.MediaType type = SocialNote.MediaType.valueOf(mediaType);
            int imageCount = 0;
            int videoCount = 0;

            for (NoteMediaRequest mediaReq : mediaList) {
                if (!StringUtils.hasText(mediaReq.getUrl())) {
                    throw new GloboxApplicationException(SocialCode.NOTE_MEDIA_EMPTY);
                }

                if (type == SocialNote.MediaType.IMAGE) {
                    imageCount++;
                } else if (type == SocialNote.MediaType.VIDEO) {
                    videoCount++;
                    if (!StringUtils.hasText(mediaReq.getCoverUrl())) {
                        throw new GloboxApplicationException(SocialCode.NOTE_VIDEO_COVER_REQUIRED);
                    }
                }
            }

            if (type == SocialNote.MediaType.IMAGE && imageCount > MAX_IMAGE_COUNT) {
                throw new GloboxApplicationException(SocialCode.NOTE_MEDIA_TOO_MANY);
            }
            if (type == SocialNote.MediaType.VIDEO && videoCount > MAX_VIDEO_COUNT) {
                throw new GloboxApplicationException(SocialCode.NOTE_MEDIA_TOO_MANY);
            }

        } catch (IllegalArgumentException ex) {
            throw new GloboxApplicationException(SocialCode.NOTE_MEDIA_TYPE_INVALID);
        }
    }

    private void insertMediaList(Long noteId, List<NoteMediaRequest> mediaList, String mediaType) {
        for (NoteMediaRequest mediaReq : mediaList) {
            SocialNoteMedia media = new SocialNoteMedia();
            media.setNoteId(noteId);
            media.setMediaType(SocialNoteMedia.MediaType.valueOf(mediaType));
            media.setUrl(mediaReq.getUrl());
            media.setCoverUrl(mediaReq.getCoverUrl());
            media.setSort(mediaReq.getSort() != null ? mediaReq.getSort() : 0);
            media.setCreatedAt(LocalDateTime.now());
            socialNoteMediaMapper.insert(media);
        }
    }

    private void applyMediaSummary(SocialNote note, String mediaType, List<NoteMediaRequest> mediaList) {
        if (!StringUtils.hasText(mediaType) || CollectionUtils.isEmpty(mediaList)) {
            return;
        }
        note.setMediaType(SocialNote.MediaType.valueOf(mediaType));
        NoteMediaRequest firstMedia = mediaList.get(0);
        if (SocialNote.MediaType.VIDEO.name().equals(mediaType) && StringUtils.hasText(firstMedia.getCoverUrl())) {
            note.setCoverUrl(firstMedia.getCoverUrl());
        } else if (SocialNote.MediaType.IMAGE.name().equals(mediaType) && StringUtils.hasText(firstMedia.getUrl())) {
            note.setCoverUrl(firstMedia.getUrl());
        }
    }

    @Override
    public R<CursorPaginationResult<NoteListVo>> getNoteListLatest(String cursor, Integer size) {
        // 1. 参数校验
        if (size == null || size < 1) {
            size = DEFAULT_PAGE_SIZE;
        }
        if (size > MAX_PAGE_SIZE) {
            throw new GloboxApplicationException(SocialCode.NOTE_PAGE_SIZE_EXCEEDED);
        }

        // 2. 解析游标
        CursorUtils.Cursor cursorObj = null;
        if (StringUtils.hasText(cursor)) {
            cursorObj = CursorUtils.parseCursor(cursor);
        }

        // 3. 构建查询条件
        LambdaQueryWrapper<SocialNote> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SocialNote::getStatus, SocialNote.Status.PUBLISHED);

        // 4. 添加游标条件
        if (cursorObj != null) {
            // 提取为final变量，以便在lambda中使用
            final LocalDateTime cursorTime = cursorObj.getCreatedAt();
            final Long cursorId = cursorObj.getNoteId();
            queryWrapper.and(wrapper -> wrapper
                    .lt(SocialNote::getCreatedAt, cursorTime)
                    .or(w -> w.eq(SocialNote::getCreatedAt, cursorTime)
                            .lt(SocialNote::getNoteId, cursorId))
            );
        }

        // 5. 排序
        queryWrapper.orderByDesc(SocialNote::getCreatedAt)
                .orderByDesc(SocialNote::getNoteId);

        // 6. 查询（多查一条判断hasMore）
        queryWrapper.last("LIMIT " + (size + 1));
        List<SocialNote> notes = socialNoteMapper.selectList(queryWrapper);

        // 7. 判断是否有更多数据
        boolean hasMore = notes.size() > size;
        if (hasMore) {
            notes = notes.subList(0, size);
        }

        // 8. 转换为VO
        List<NoteListVo> voList = convertToNoteListVo(notes);

        // 9. 构建nextCursor
        String nextCursor = null;
        if (hasMore && !voList.isEmpty()) {
            NoteListVo lastVo = voList.get(voList.size() - 1);
            nextCursor = CursorUtils.buildCursor(lastVo.getCreatedAt(), lastVo.getNoteId());
        }

        // 10. 构建结果
        CursorPaginationResult<NoteListVo> result = new CursorPaginationResult<>();
        result.setList(voList);
        result.setNextCursor(nextCursor);
        result.setHasMore(hasMore);

        return R.ok(result);
    }

    @Override
    public R<CursorPaginationResult<NoteListVo>> getNoteListPool(Long seed, String cursor, Integer size) {
        // 1. 参数校验
        if (seed == null) {
            throw new GloboxApplicationException(SocialCode.NOTE_POOL_SEED_REQUIRED);
        }
        if (size == null || size < 1) {
            size = DEFAULT_PAGE_SIZE;
        }
        if (size > MAX_PAGE_SIZE) {
            throw new GloboxApplicationException(SocialCode.NOTE_PAGE_SIZE_EXCEEDED);
        }

        // 2. 解析游标
        CursorUtils.Cursor cursorObj = null;
        if (StringUtils.hasText(cursor)) {
            cursorObj = CursorUtils.parseCursor(cursor);
        }

        // 3. 查询所有ENABLED的池子记录
        LambdaQueryWrapper<SocialNotePool> poolQuery = new LambdaQueryWrapper<>();
        poolQuery.eq(SocialNotePool::getPoolStatus, SocialNotePool.PoolStatus.ENABLED);
        List<SocialNotePool> allPoolList = socialNotePoolMapper.selectList(poolQuery);

        if (allPoolList.isEmpty()) {
            CursorPaginationResult<NoteListVo> result = new CursorPaginationResult<>();
            result.setList(Collections.emptyList());
            result.setNextCursor(null);
            result.setHasMore(false);
            return R.ok(result);
        }

        // 4. 批量查询笔记（只查询PUBLISHED状态的）
        List<Long> allNoteIds = allPoolList.stream()
                .map(SocialNotePool::getNoteId)
                .collect(Collectors.toList());
        LambdaQueryWrapper<SocialNote> noteQuery = new LambdaQueryWrapper<>();
        noteQuery.in(SocialNote::getNoteId, allNoteIds)
                .eq(SocialNote::getStatus, SocialNote.Status.PUBLISHED);
        List<SocialNote> allNotes = socialNoteMapper.selectList(noteQuery);

        // 5. 构建笔记映射
        Map<Long, SocialNote> noteMap = allNotes.stream()
                .collect(Collectors.toMap(SocialNote::getNoteId, note -> note));

        // 6. 过滤出有效的池子记录（对应的笔记存在且已发布）
        List<SocialNotePool> validPoolList = allPoolList.stream()
                .filter(pool -> noteMap.containsKey(pool.getNoteId()))
                .collect(Collectors.toList());

        // 7. 在内存中按ABS(shuffle_key - seed)排序，然后按created_at和note_id排序
        validPoolList.sort((p1, p2) -> {
            long sort1 = Math.abs(p1.getShuffleKey() - seed);
            long sort2 = Math.abs(p2.getShuffleKey() - seed);
            if (sort1 != sort2) {
                return Long.compare(sort1, sort2);
            }
            // 如果排序值相同，按created_at倒序，再按note_id倒序
            SocialNote n1 = noteMap.get(p1.getNoteId());
            SocialNote n2 = noteMap.get(p2.getNoteId());
            if (n1 != null && n2 != null) {
                int timeCompare = n2.getCreatedAt().compareTo(n1.getCreatedAt());
                if (timeCompare != 0) {
                    return timeCompare;
                }
                return Long.compare(p2.getNoteId(), p1.getNoteId());
            }
            return 0;
        });

        // 8. 如果有游标，过滤掉游标之前的记录
        if (cursorObj != null) {
            // 提取为final变量，以便在lambda中使用
            final Long cursorNoteId = cursorObj.getNoteId();
            // 找到游标对应的池子记录
            SocialNotePool cursorPool = validPoolList.stream()
                    .filter(pool -> pool.getNoteId().equals(cursorNoteId))
                    .findFirst()
                    .orElse(null);

            if (cursorPool != null) {
                SocialNote cursorNote = noteMap.get(cursorNoteId);
                if (cursorNote != null) {
                    // 提取为final变量，以便在lambda中使用
                    final long cursorSortValue = Math.abs(cursorPool.getShuffleKey() - seed);
                    final LocalDateTime cursorTime = cursorNote.getCreatedAt();
                    // 过滤：排序值小于游标，或排序值相等但创建时间更早，或创建时间相等但noteId更小
                    validPoolList = validPoolList.stream()
                            .filter(pool -> {
                                SocialNote note = noteMap.get(pool.getNoteId());
                                if (note == null) {
                                    return false;
                                }
                                long poolSortValue = Math.abs(pool.getShuffleKey() - seed);
                                if (poolSortValue < cursorSortValue) {
                                    return true;
                                }
                                if (poolSortValue == cursorSortValue) {
                                    int timeCompare = note.getCreatedAt().compareTo(cursorTime);
                                    if (timeCompare < 0) {
                                        return true;
                                    }
                                    if (timeCompare == 0 && pool.getNoteId() < cursorNoteId) {
                                        return true;
                                    }
                                }
                                return false;
                            })
                            .collect(Collectors.toList());
                }
            }
        }

        // 9. 取size+1条记录（判断hasMore）
        boolean hasMore = validPoolList.size() > size;
        List<SocialNotePool> resultPoolList = hasMore
                ? validPoolList.subList(0, size)
                : validPoolList;

        // 10. 按池子顺序构建笔记列表
        List<SocialNote> sortedNotes = resultPoolList.stream()
                .map(pool -> noteMap.get(pool.getNoteId()))
                .filter(note -> note != null)
                .collect(Collectors.toList());

        // 11. 转换为VO
        List<NoteListVo> voList = convertToNoteListVo(sortedNotes);

        // 12. 构建nextCursor
        String nextCursor = null;
        if (hasMore && !voList.isEmpty()) {
            NoteListVo lastVo = voList.get(voList.size() - 1);
            nextCursor = CursorUtils.buildCursor(lastVo.getCreatedAt(), lastVo.getNoteId());
        }

        // 13. 构建结果
        CursorPaginationResult<NoteListVo> result = new CursorPaginationResult<>();
        result.setList(voList);
        result.setNextCursor(nextCursor);
        result.setHasMore(hasMore);

        return R.ok(result);
    }

    /**
     * 将笔记列表转换为VO列表（公共方法）
     */
    private List<NoteListVo> convertToNoteListVo(List<SocialNote> notes) {
        return notes.stream()
                .map(note -> {
                    NoteListVo vo = new NoteListVo();
                    BeanUtils.copyProperties(note, vo);
                    vo.setStatus(note.getStatus() != null ? note.getStatus().name() : null);
                    vo.setMediaType(note.getMediaType() != null ? note.getMediaType().name() : null);
                    if (StringUtils.hasText(note.getContent()) && note.getContent().length() > 100) {
                        vo.setContent(note.getContent().substring(0, 100) + "...");
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }
}
