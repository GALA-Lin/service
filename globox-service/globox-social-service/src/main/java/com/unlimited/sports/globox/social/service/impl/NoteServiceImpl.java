package com.unlimited.sports.globox.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.result.SocialCode;
import com.unlimited.sports.globox.model.social.dto.NoteMediaRequest;
import com.unlimited.sports.globox.model.social.dto.PublishNoteRequest;
import com.unlimited.sports.globox.model.social.dto.SaveDraftRequest;
import com.unlimited.sports.globox.model.social.dto.UpdateNoteRequest;
import com.unlimited.sports.globox.model.social.entity.SocialNote;
import com.unlimited.sports.globox.model.social.entity.SocialNoteLike;
import com.unlimited.sports.globox.model.social.entity.SocialNoteMedia;
import com.unlimited.sports.globox.model.social.entity.SocialNotePool;
import com.unlimited.sports.globox.model.social.vo.CursorPaginationResult;
import com.unlimited.sports.globox.model.social.vo.DraftNoteVo;
import com.unlimited.sports.globox.model.social.vo.NoteDetailVo;
import com.unlimited.sports.globox.model.social.vo.NoteItemVo;
import com.unlimited.sports.globox.model.social.vo.NoteMediaVo;
import com.unlimited.sports.globox.social.mapper.SocialNoteLikeMapper;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Autowired
    private SocialNoteLikeMapper socialNoteLikeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> saveDraft(Long userId, SaveDraftRequest request) {
        if (userId == null) {
            return R.error(SocialCode.NOTE_NOT_FOUND);
        }

        // 至少一项非空
        if (!hasAnyContent(request.getTitle(), request.getContent(), request.getMediaList())) {
            throw new GloboxApplicationException(SocialCode.NOTE_DRAFT_EMPTY);
        }

        // 查找现有草稿（事务保护），按创建时间倒序，保留最新
        LambdaQueryWrapper<SocialNote> query = new LambdaQueryWrapper<>();
        query.eq(SocialNote::getUserId, userId)
                .eq(SocialNote::getStatus, SocialNote.Status.DRAFT)
                .orderByDesc(SocialNote::getCreatedAt);
        List<SocialNote> drafts = socialNoteMapper.selectList(query);

        SocialNote draft = null;
        if (!drafts.isEmpty()) {
            draft = drafts.get(0);
            if (drafts.size() > 1) {
                for (int i = 1; i < drafts.size(); i++) {
                    socialNoteMapper.deleteById(drafts.get(i).getNoteId());
                    log.warn("清理多余草稿：userId={}, noteId={}", userId, drafts.get(i).getNoteId());
                }
            }
        }

        // 如果提供媒体且非空，校验媒体
        if (request.getMediaList() != null && !request.getMediaList().isEmpty()) {
            validateMedia(request.getMediaType(), request.getMediaList());
        }

        if (draft != null) {
            // 更新草稿
            draft.setTitle(request.getTitle());
            draft.setContent(request.getContent());
            draft.setAllowComment(request.getAllowComment() != null ? request.getAllowComment() : draft.getAllowComment());
            draft.setUpdatedAt(LocalDateTime.now());

            if (request.getMediaList() != null) {
                // 更新媒体摘要
                applyMediaSummary(draft, request.getMediaType(), request.getMediaList());
                socialNoteMapper.updateById(draft);

                // 替换媒体
                LambdaQueryWrapper<SocialNoteMedia> deleteQuery = new LambdaQueryWrapper<>();
                deleteQuery.eq(SocialNoteMedia::getNoteId, draft.getNoteId());
                socialNoteMediaMapper.delete(deleteQuery);

                if (!request.getMediaList().isEmpty()) {
                    insertMediaList(draft.getNoteId(), request.getMediaList(), request.getMediaType());
                }
            } else {
                socialNoteMapper.updateById(draft);
            }

            log.info("草稿更新成功：userId={}, noteId={}", userId, draft.getNoteId());
            return R.ok(draft.getNoteId());
        } else {
            // 新建草稿
            SocialNote newDraft = SocialNote.builder()
                    .userId(userId)
                    .title(request.getTitle())
                    .content(request.getContent())
                    .status(SocialNote.Status.DRAFT)
                    .allowComment(request.getAllowComment() != null ? request.getAllowComment() : Boolean.TRUE)
                    .likeCount(0)
                    .commentCount(0)
                    .collectCount(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            applyMediaSummary(newDraft, request.getMediaType(), request.getMediaList());
            socialNoteMapper.insert(newDraft);

            if (request.getMediaList() != null && !request.getMediaList().isEmpty()) {
                insertMediaList(newDraft.getNoteId(), request.getMediaList(), request.getMediaType());
            }

            log.info("草稿创建成功：userId={}, noteId={}", userId, newDraft.getNoteId());
            return R.ok(newDraft.getNoteId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Long> publishNote(Long userId, PublishNoteRequest request) {
        // 1. 参数校验
        if (userId == null) {
            return R.error(SocialCode.NOTE_NOT_FOUND);
        }

        // 2. 强校验：content 必填
        if (!StringUtils.hasText(request.getContent())) {
            throw new GloboxApplicationException(SocialCode.NOTE_CONTENT_EMPTY);
        }

        // 3. 强校验：mediaList 必填且非空
        if (CollectionUtils.isEmpty(request.getMediaList())) {
            throw new GloboxApplicationException(SocialCode.NOTE_PUBLISH_VALIDATION_FAILED);
        }

        // 4. 校验媒体类型和媒体列表
        validateMedia(request.getMediaType(), request.getMediaList());

        // 5. 创建 PUBLISHED 笔记
        SocialNote note = SocialNote.builder()
                .userId(userId)
                .title(request.getTitle())
                .content(request.getContent())
                .status(SocialNote.Status.PUBLISHED)
                .allowComment(request.getAllowComment() != null ? request.getAllowComment() : Boolean.TRUE)
                .likeCount(0)
                .commentCount(0)
                .collectCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 6. 设置媒体摘要（coverUrl）
        applyMediaSummary(note, request.getMediaType(), request.getMediaList());

        // 7. 插入笔记
        socialNoteMapper.insert(note);

        // 8. 插入媒体列表
        insertMediaList(note.getNoteId(), request.getMediaList(), request.getMediaType());

        // 9. 发布成功后，删除用户所有草稿
        LambdaQueryWrapper<SocialNote> draftQuery = new LambdaQueryWrapper<>();
        draftQuery.eq(SocialNote::getUserId, userId)
                .eq(SocialNote::getStatus, SocialNote.Status.DRAFT);
        List<SocialNote> drafts = socialNoteMapper.selectList(draftQuery);

        if (!drafts.isEmpty()) {
            for (SocialNote draft : drafts) {
                // 删除草稿的媒体
                LambdaQueryWrapper<SocialNoteMedia> mediaDeleteQuery = new LambdaQueryWrapper<>();
                mediaDeleteQuery.eq(SocialNoteMedia::getNoteId, draft.getNoteId());
                socialNoteMediaMapper.delete(mediaDeleteQuery);
                // 删除草稿
                socialNoteMapper.deleteById(draft.getNoteId());
            }
            log.info("发布成功后清理草稿：userId={}, 清理数量={}", userId, drafts.size());
        }

        log.info("笔记发布成功：userId={}, noteId={}", userId, note.getNoteId());
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

        // 按状态校验内容
        if (note.getStatus() == SocialNote.Status.PUBLISHED) {
            // 已发布：正文必填
        if (!StringUtils.hasText(request.getContent())) {
                throw new GloboxApplicationException(SocialCode.NOTE_CONTENT_EMPTY);
            }
        } else if (note.getStatus() == SocialNote.Status.DRAFT) {
            // 草稿：至少一项非空
            boolean hasContent = hasAnyContent(request.getTitle(), request.getContent(), request.getMediaList());
            if (!hasContent) {
                throw new GloboxApplicationException(SocialCode.NOTE_DRAFT_EMPTY);
            }
        }

        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setAllowComment(request.getAllowComment() != null ? request.getAllowComment() : note.getAllowComment());
        note.setUpdatedAt(LocalDateTime.now());

        if (request.getMediaList() != null) {
            // mediaList 传了但为空，禁止清空
            if (request.getMediaList().isEmpty()) {
                throw new GloboxApplicationException(SocialCode.NOTE_MEDIA_CLEAR_NOT_ALLOWED);
            }
            // mediaList 传了且非空，全量替换
            validateMedia(request.getMediaType(), request.getMediaList());
            applyMediaSummary(note, request.getMediaType(), request.getMediaList());
            
            socialNoteMapper.updateById(note);
            
            // 删除旧媒体
            LambdaQueryWrapper<SocialNoteMedia> deleteQuery = new LambdaQueryWrapper<>();
            deleteQuery.eq(SocialNoteMedia::getNoteId, noteId);
            socialNoteMediaMapper.delete(deleteQuery);
            
            // 插入新媒体
            insertMediaList(noteId, request.getMediaList(), request.getMediaType());
        } else {
            // mediaList 未传（null），不更新媒体相关字段
            socialNoteMapper.updateById(note);
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
    public R<NoteDetailVo> getNoteDetail(Long noteId, Long userId) {
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

        // 查询 liked 状态
        if (userId != null) {
            Set<Long> likedNoteIds = socialNoteLikeMapper.selectLikedNoteIdsByUser(userId, Collections.singletonList(noteId));
            vo.setLiked(likedNoteIds.contains(noteId));
        }

        return R.ok(vo);
    }

    @Override
    public R<PaginationResult<NoteItemVo>> getNoteList(Integer page, Integer pageSize) {
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

        List<NoteItemVo> voList = pageResult.getRecords().stream()
                .map(note -> {
                    NoteItemVo vo = new NoteItemVo();
                    BeanUtils.copyProperties(note, vo);
                    vo.setStatus(note.getStatus() != null ? note.getStatus().name() : null);
                    vo.setMediaType(note.getMediaType() != null ? note.getMediaType().name() : null);
                    if (StringUtils.hasText(note.getContent()) && note.getContent().length() > 100) {
                        vo.setContent(note.getContent().substring(0, 100) + "...");
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        PaginationResult<NoteItemVo> result = PaginationResult.build(voList, pageResult.getTotal(), page, pageSize);
        return R.ok(result);
    }

    @Override
    public R<PaginationResult<NoteItemVo>> getMyNotes(Long userId, Integer page, Integer pageSize) {
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

        List<NoteItemVo> voList = pageResult.getRecords().stream()
                .map(note -> {
                    NoteItemVo vo = new NoteItemVo();
                    BeanUtils.copyProperties(note, vo);
                    vo.setStatus(note.getStatus() != null ? note.getStatus().name() : null);
                    vo.setMediaType(note.getMediaType() != null ? note.getMediaType().name() : null);
                    if (StringUtils.hasText(note.getContent()) && note.getContent().length() > 100) {
                        vo.setContent(note.getContent().substring(0, 100) + "...");
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        PaginationResult<NoteItemVo> result = PaginationResult.build(voList, pageResult.getTotal(), page, pageSize);
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
    public R<CursorPaginationResult<NoteItemVo>> getNoteListLatest(String cursor, Integer size, Long userId) {
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
            cursorObj = CursorUtils.parseLatestCursor(cursor);
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
        List<NoteItemVo> voList = convertToNoteItemVo(notes, userId);

        // 9. 构建nextCursor
        String nextCursor = null;
        if (hasMore && !voList.isEmpty()) {
            NoteItemVo lastVo = voList.get(voList.size() - 1);
            nextCursor = CursorUtils.buildLatestCursor(lastVo.getCreatedAt(), lastVo.getNoteId());
        }

        // 10. 构建结果
        CursorPaginationResult<NoteItemVo> result = new CursorPaginationResult<>();
        result.setList(voList);
        result.setNextCursor(nextCursor);
        result.setHasMore(hasMore);

        return R.ok(result);
    }

    @Override
    public R<CursorPaginationResult<NoteItemVo>> getNoteListPool(String cursor, Integer size, Long userId) {
        // 1. 参数校验
        if (size == null || size < 1) {
            size = DEFAULT_PAGE_SIZE;
        }
        if (size > MAX_PAGE_SIZE) {
            throw new GloboxApplicationException(SocialCode.NOTE_PAGE_SIZE_EXCEEDED);
        }

        // 2. 解析游标，获取seed（如果存在），否则生成新的seed
        Long seed;
        CursorUtils.PoolCursor poolCursorObj = null;
        if (StringUtils.hasText(cursor)) {
            poolCursorObj = CursorUtils.parsePoolCursor(cursor);
            seed = poolCursorObj.getSeed();
        } else {
            // 生成1-1000000之间的随机数作为seed
            seed = (long) (Math.random() * 1000000) + 1;
        }

        // 3. 查询所有ENABLED的池子记录
        LambdaQueryWrapper<SocialNotePool> poolQuery = new LambdaQueryWrapper<>();
        poolQuery.eq(SocialNotePool::getPoolStatus, SocialNotePool.PoolStatus.ENABLED);
        List<SocialNotePool> allPoolList = socialNotePoolMapper.selectList(poolQuery);

        if (allPoolList.isEmpty()) {
            CursorPaginationResult<NoteItemVo> result = new CursorPaginationResult<>();
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
        if (poolCursorObj != null) {
            // 提取为final变量，以便在lambda中使用
            final Long cursorNoteId = poolCursorObj.getNoteId();
            final LocalDateTime cursorTime = poolCursorObj.getCreatedAt();
            // 找到游标对应的池子记录
            SocialNotePool cursorPool = validPoolList.stream()
                    .filter(pool -> pool.getNoteId().equals(cursorNoteId))
                    .findFirst()
                    .orElse(null);

            if (cursorPool != null) {
                // 提取为final变量，以便在lambda中使用
                final long cursorSortValue = Math.abs(cursorPool.getShuffleKey() - seed);
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
        List<NoteItemVo> voList = convertToNoteItemVo(sortedNotes, userId);

        // 12. 构建nextCursor（包含seed）
        String nextCursor = null;
        if (hasMore && !voList.isEmpty()) {
            NoteItemVo lastVo = voList.get(voList.size() - 1);
            nextCursor = CursorUtils.buildPoolCursor(seed, lastVo.getCreatedAt(), lastVo.getNoteId());
        }

        // 13. 构建结果
        CursorPaginationResult<NoteItemVo> result = new CursorPaginationResult<>();
        result.setList(voList);
        result.setNextCursor(nextCursor);
        result.setHasMore(hasMore);

        return R.ok(result);
    }

    @Override
    public R<CursorPaginationResult<NoteItemVo>> getNoteListHot(String cursor, Integer size, Long userId) {
        // 1. 参数校验
        if (size == null || size < 1) {
            size = DEFAULT_PAGE_SIZE;
        }
        if (size > MAX_PAGE_SIZE) {
            throw new GloboxApplicationException(SocialCode.NOTE_PAGE_SIZE_EXCEEDED);
        }

        // 2. 解析游标
        CursorUtils.HotCursor hotCursorObj = null;
        if (StringUtils.hasText(cursor)) {
            hotCursorObj = CursorUtils.parseHotCursor(cursor);
        }

        // 3. 构建查询条件
        LambdaQueryWrapper<SocialNote> query = new LambdaQueryWrapper<>();
        query.eq(SocialNote::getStatus, SocialNote.Status.PUBLISHED);

        // 4. 如果有游标，添加游标条件
        if (hotCursorObj != null) {
            final Integer cursorLikeCount = hotCursorObj.getLikeCount();
            final LocalDateTime cursorTime = hotCursorObj.getCreatedAt();
            final Long cursorNoteId = hotCursorObj.getNoteId();
            query.and(wrapper -> wrapper
                    .lt(SocialNote::getLikeCount, cursorLikeCount)
                    .or(w -> w.eq(SocialNote::getLikeCount, cursorLikeCount)
                            .lt(SocialNote::getCreatedAt, cursorTime))
                    .or(w -> w.eq(SocialNote::getLikeCount, cursorLikeCount)
                            .eq(SocialNote::getCreatedAt, cursorTime)
                            .lt(SocialNote::getNoteId, cursorNoteId))
            );
        }

        // 5. 排序：按点赞数倒序，相同点赞数按创建时间倒序，再按笔记ID倒序
        query.orderByDesc(SocialNote::getLikeCount)
                .orderByDesc(SocialNote::getCreatedAt)
                .orderByDesc(SocialNote::getNoteId);

        // 6. 查询 size+1 条记录（判断 hasMore）
        query.last("LIMIT " + (size + 1));
        List<SocialNote> notes = socialNoteMapper.selectList(query);
        boolean hasMore = notes.size() > size;
        List<SocialNote> resultNotes = hasMore ? notes.subList(0, size) : notes;

        // 7. 转换为VO
        List<NoteItemVo> voList = convertToNoteItemVo(resultNotes, userId);

        // 8. 构建nextCursor
        String nextCursor = null;
        if (hasMore && !voList.isEmpty()) {
            NoteItemVo lastVo = voList.get(voList.size() - 1);
            nextCursor = CursorUtils.buildHotCursor(lastVo.getLikeCount(), lastVo.getCreatedAt(), lastVo.getNoteId());
        }

        // 9. 构建结果
        CursorPaginationResult<NoteItemVo> result = new CursorPaginationResult<>();
        result.setList(voList);
        result.setNextCursor(nextCursor);
        result.setHasMore(hasMore);

        return R.ok(result);
    }

    @Override
    public R<CursorPaginationResult<NoteItemVo>> getLikedNotes(Long userId, String cursor, Integer size) {
        // 1. 参数校验
        if (size == null || size < 1) {
            size = DEFAULT_PAGE_SIZE;
        }
        if (size > MAX_PAGE_SIZE) {
            throw new GloboxApplicationException(SocialCode.NOTE_PAGE_SIZE_EXCEEDED);
        }

        // 2. 解析游标
        CursorUtils.LikedCursor likedCursorObj = null;
        LocalDateTime cursorTime = null;
        Long cursorLikeId = null;
        if (StringUtils.hasText(cursor)) {
            likedCursorObj = CursorUtils.parseLikedCursor(cursor);
            cursorTime = likedCursorObj.getLikeCreatedAt();
            cursorLikeId = likedCursorObj.getLikeId();
        }

        // 3. 使用 JOIN 查询获取点赞的笔记（查询 size+1 条以判断是否有更多）
        List<SocialNote> notes = socialNoteLikeMapper.selectLikedNotesWithJoin(
                userId, cursorTime, cursorLikeId, size + 1);

        boolean hasMore = notes.size() > size;
        List<SocialNote> resultNotes = hasMore ? notes.subList(0, size) : notes;

        if (resultNotes.isEmpty()) {
            CursorPaginationResult<NoteItemVo> result = new CursorPaginationResult<>();
            result.setList(Collections.emptyList());
            result.setNextCursor(null);
            result.setHasMore(false);
            return R.ok(result);
        }

        // 4. 批量查询点赞记录以获取 like_id 和 created_at（用于构建游标）
        List<Long> noteIds = resultNotes.stream()
                .map(SocialNote::getNoteId)
                .collect(Collectors.toList());
        LambdaQueryWrapper<SocialNoteLike> likeQuery = new LambdaQueryWrapper<>();
        likeQuery.eq(SocialNoteLike::getUserId, userId)
                .in(SocialNoteLike::getNoteId, noteIds);
        List<SocialNoteLike> likes = socialNoteLikeMapper.selectList(likeQuery);
        Map<Long, SocialNoteLike> likeMap = likes.stream()
                .collect(Collectors.toMap(SocialNoteLike::getNoteId, like -> like));

        // 5. 转换为VO，并设置 liked=true（因为这些都是用户点赞的笔记）
        List<NoteItemVo> voList = convertToNoteItemVo(resultNotes, userId);
        // 点赞列表中的笔记都是已点赞的
        voList.forEach(vo -> vo.setLiked(true));

        // 6. 构建nextCursor（使用最后一条笔记对应的点赞记录）
        String nextCursor = null;
        if (hasMore && !resultNotes.isEmpty()) {
            SocialNote lastNote = resultNotes.get(resultNotes.size() - 1);
            SocialNoteLike lastLike = likeMap.get(lastNote.getNoteId());
            if (lastLike != null) {
                nextCursor = CursorUtils.buildLikedCursor(lastLike.getCreatedAt(), lastLike.getLikeId());
            }
        }

        // 7. 构建结果
        CursorPaginationResult<NoteItemVo> result = new CursorPaginationResult<>();
        result.setList(voList);
        result.setNextCursor(nextCursor);
        result.setHasMore(hasMore);

        return R.ok(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> likeNote(Long userId, Long noteId) {
        // 1. 查询笔记是否存在且已发布
        SocialNote note = socialNoteMapper.selectById(noteId);
        if (note == null || note.getStatus() != SocialNote.Status.PUBLISHED) {
            throw new GloboxApplicationException(SocialCode.NOTE_NOT_FOUND);
        }

        // 2. 检查是否已点赞（幂等性）
        LambdaQueryWrapper<SocialNoteLike> query = new LambdaQueryWrapper<>();
        query.eq(SocialNoteLike::getUserId, userId)
                .eq(SocialNoteLike::getNoteId, noteId);
        SocialNoteLike existingLike = socialNoteLikeMapper.selectOne(query);

        if (existingLike == null) {
            // 3. 插入点赞记录
            SocialNoteLike like = new SocialNoteLike();
            like.setUserId(userId);
            like.setNoteId(noteId);
            like.setCreatedAt(LocalDateTime.now());
            socialNoteLikeMapper.insert(like);

            // 4. 原子更新笔记点赞数
            socialNoteMapper.incrementLikeCount(noteId);
        }

        return R.ok("点赞成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> unlikeNote(Long userId, Long noteId) {
        // 1. 查询笔记是否存在且已发布
        SocialNote note = socialNoteMapper.selectById(noteId);
        if (note == null || note.getStatus() != SocialNote.Status.PUBLISHED) {
            throw new GloboxApplicationException(SocialCode.NOTE_NOT_FOUND);
        }

        // 2. 查询点赞记录
        LambdaQueryWrapper<SocialNoteLike> query = new LambdaQueryWrapper<>();
        query.eq(SocialNoteLike::getUserId, userId)
                .eq(SocialNoteLike::getNoteId, noteId);
        SocialNoteLike existingLike = socialNoteLikeMapper.selectOne(query);

        if (existingLike != null) {
            // 3. 删除点赞记录
            socialNoteLikeMapper.deleteById(existingLike.getLikeId());

            // 4. 原子更新笔记点赞数（确保不为负数）
            socialNoteMapper.decrementLikeCount(noteId);
        }

        return R.ok("取消点赞成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> deleteNoteMedia(Long userId, Long noteId, Long mediaId) {
        // 1. 查询笔记是否存在
        SocialNote note = socialNoteMapper.selectById(noteId);
        if (note == null || note.getStatus() == SocialNote.Status.DELETED) {
            return R.error(SocialCode.NOTE_NOT_FOUND);
        }

        // 2. 权限校验：仅作者可删
        if (!note.getUserId().equals(userId)) {
            throw new GloboxApplicationException(SocialCode.NOTE_PERMISSION_DENIED);
        }

        // 3. 状态校验：仅允许 DRAFT/PUBLISHED
        if (note.getStatus() != SocialNote.Status.DRAFT && note.getStatus() != SocialNote.Status.PUBLISHED) {
            throw new GloboxApplicationException(SocialCode.NOTE_NOT_FOUND);
        }

        // 4. 查询媒体是否存在
        SocialNoteMedia media = socialNoteMediaMapper.selectById(mediaId);
        if (media == null || !media.getNoteId().equals(noteId)) {
            throw new GloboxApplicationException(SocialCode.NOTE_MEDIA_NOT_FOUND);
        }

        // 5. 查询该笔记的所有媒体
        LambdaQueryWrapper<SocialNoteMedia> query = new LambdaQueryWrapper<>();
        query.eq(SocialNoteMedia::getNoteId, noteId);
        List<SocialNoteMedia> allMedia = socialNoteMediaMapper.selectList(query);

        // 6. 校验：删除后不能为0
        if (allMedia.size() <= 1) {
            throw new GloboxApplicationException(SocialCode.NOTE_MEDIA_CLEAR_NOT_ALLOWED);
        }

        // 7. 删除媒体
        socialNoteMediaMapper.deleteById(mediaId);

        // 8. 如果删除的是封面对应的媒体，重新生成封面
        boolean needUpdateCover = note.getCoverUrl() != null && note.getCoverUrl().equals(media.getUrl());
        if (needUpdateCover || (media.getMediaType() == SocialNoteMedia.MediaType.VIDEO &&
                                note.getCoverUrl() != null && note.getCoverUrl().equals(media.getCoverUrl()))) {
            // 使用剩余媒体的第一条重新生成 coverUrl
            List<SocialNoteMedia> remainingMedia = allMedia.stream()
                    .filter(m -> !m.getMediaId().equals(mediaId))
                    .sorted(Comparator.comparing(SocialNoteMedia::getSort))
                    .collect(Collectors.toList());

            if (!remainingMedia.isEmpty()) {
                SocialNoteMedia firstMedia = remainingMedia.get(0);
                if (firstMedia.getMediaType() == SocialNoteMedia.MediaType.IMAGE) {
                    note.setCoverUrl(firstMedia.getUrl());
                } else if (firstMedia.getMediaType() == SocialNoteMedia.MediaType.VIDEO) {
                    note.setCoverUrl(firstMedia.getCoverUrl());
                }
                socialNoteMapper.updateById(note);
            }
        }

        log.info("媒体删除成功：userId={}, noteId={}, mediaId={}", userId, noteId, mediaId);
        return R.ok("删除成功");
    }

    @Override
    public R<CursorPaginationResult<NoteItemVo>> getNoteFeed(String sort, String cursor, Integer size, Long userId) {
        // 参数校验
        if (sort == null || sort.trim().isEmpty()) {
            throw new GloboxApplicationException(SocialCode.NOTE_SORT_INVALID);
        }
        
        // 根据 sort 分发到对应逻辑
        switch (sort) {
            case "pool":
                // TODO: 探索页推荐算法待实现，等待后续决定推荐算法
                // 目前暂时返回最新流作为占位符
                // 注意：首页推荐（/home）使用管理员精选（pool），探索页推荐（/feed?sort=pool）将使用推荐算法
                return getNoteFeedLatest(cursor, size, userId);
            case "latest":
                return getNoteFeedLatest(cursor, size, userId);
            case "hot":
                return getNoteFeedHot(cursor, size, userId);
            default:
                throw new GloboxApplicationException(SocialCode.NOTE_SORT_INVALID);
        }
    }

    @Override
    public R<CursorPaginationResult<NoteItemVo>> getHomeNotes(String cursor, Integer size, Long userId) {
        // 首页推荐：使用管理员精选（pool）逻辑
        // 支持管理员增删精选内容，每次请求获得随机顺序，平等展示每个精选内容
        return getNoteListPool(cursor, size, userId);
    }

    /**
     * 最新流（latest）内部实现
     * 按创建时间倒序排列
     */
    private R<CursorPaginationResult<NoteItemVo>> getNoteFeedLatest(String cursor, Integer size, Long userId) {
        // 复用 getNoteListLatest 逻辑
        return getNoteListLatest(cursor, size, userId);
    }

    /**
     * 最热流（hot）内部实现
     * 按点赞数倒序排列
     */
    private R<CursorPaginationResult<NoteItemVo>> getNoteFeedHot(String cursor, Integer size, Long userId) {
        // 复用 getNoteListHot 逻辑
        return getNoteListHot(cursor, size, userId);
    }

    /**
     * 校验是否至少有一项内容（标题/正文/媒体）
     */
    private boolean hasAnyContent(String title, String content, List<?> mediaList) {
        return StringUtils.hasText(title)
                || StringUtils.hasText(content)
                || (mediaList != null && !mediaList.isEmpty());
    }

    /**
     * 将笔记列表转换为VO列表（公共方法）
     * 
     * @param notes 笔记列表
     * @param userId 当前用户ID（可选，用于查询 liked 状态）
     * @return VO列表
     */
    private List<NoteItemVo> convertToNoteItemVo(List<SocialNote> notes, Long userId) {
        if (CollectionUtils.isEmpty(notes)) {
            return Collections.emptyList();
        }

        // 1. 转换为VO
        List<NoteItemVo> voList = notes.stream()
                .map(note -> {
                    NoteItemVo vo = new NoteItemVo();
                    BeanUtils.copyProperties(note, vo);
                    vo.setStatus(note.getStatus() != null ? note.getStatus().name() : null);
                    vo.setMediaType(note.getMediaType() != null ? note.getMediaType().name() : null);
                    if (StringUtils.hasText(note.getContent()) && note.getContent().length() > 100) {
                        vo.setContent(note.getContent().substring(0, 100) + "...");
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        // 2. 如果提供了 userId，批量查询 liked 状态
        if (userId != null) {
            List<Long> noteIds = notes.stream()
                    .map(SocialNote::getNoteId)
                    .collect(Collectors.toList());
            Set<Long> likedNoteIds = socialNoteLikeMapper.selectLikedNoteIdsByUser(userId, noteIds);
            
            // 3. 填充 liked 字段
            Map<Long, NoteItemVo> voMap = voList.stream()
                    .collect(Collectors.toMap(NoteItemVo::getNoteId, vo -> vo));
            likedNoteIds.forEach(noteId -> {
                NoteItemVo vo = voMap.get(noteId);
                if (vo != null) {
                    vo.setLiked(true);
                }
            });
        }

        return voList;
    }

    /**
     * 将笔记列表转换为VO列表（不查询 liked 状态，用于向后兼容）
     */
    private List<NoteItemVo> convertToNoteItemVo(List<SocialNote> notes) {
        return convertToNoteItemVo(notes, null);
    }
}
