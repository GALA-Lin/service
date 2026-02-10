package com.unlimited.sports.globox.social.dubbo;

import com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.internal.StringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.result.SocialCode;
import com.unlimited.sports.globox.dubbo.social.INoteSearchDataService;
import com.unlimited.sports.globox.model.social.entity.SocialNote;
import com.unlimited.sports.globox.model.social.entity.SocialNoteLike;
import com.unlimited.sports.globox.model.social.vo.NoteSyncVo;
import com.unlimited.sports.globox.social.mapper.SocialNoteLikeMapper;
import com.unlimited.sports.globox.social.mapper.SocialNoteMapper;
import com.unlimited.sports.globox.social.service.NoteLikeSyncService;
import com.unlimited.sports.globox.social.service.NoteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 笔记搜索数据RPC服务实现
 * 供搜索服务调用，获取笔记数据用于增量同步到Elasticsearch
 */
@Component
@Slf4j
@DubboService(group = "rpc")
public class NoteSearchDataServiceImpl implements INoteSearchDataService {

    @Autowired
    private SocialNoteMapper noteMapper;

    @Autowired
    private SocialNoteLikeMapper noteLikeMapper;

    @Autowired
    private NoteLikeSyncService noteLikeSyncService;

    @Autowired
    private NoteService noteService;

    /**
     * 增量同步笔记数据
     *
     * @param updatedTime 上一次同步的时间戳，为null表示全量同步，不为null表示增量同步
     * @return 笔记同步数据列表（NoteSyncVO格式）
     */
    @Override
    public RpcResult<List<NoteSyncVo>> syncNoteData(LocalDateTime updatedTime) {
        try {
            log.info("开始同步笔记数据: updatedTime={}", updatedTime);

            // 不过滤状态
            LambdaQueryWrapper<SocialNote> wrapper = new LambdaQueryWrapper<>();
            if (updatedTime != null) {
                // 增量同步：查询 updated_at > updatedTime 的数据
                wrapper.gt(SocialNote::getUpdatedAt, updatedTime);
            }
            List<SocialNote> notes = noteMapper.selectList(wrapper);

            if (notes == null || notes.isEmpty()) {
                log.info("没有需要同步的笔记数据");
                return RpcResult.ok(List.of());
            }

            log.info("查询到笔记数据: 数量={}", notes.size());

            // 批量查询实际点赞数和评论数
            List<Long> noteIds = notes.stream().map(SocialNote::getNoteId).toList();
            Map<Long, Integer> likeCountMap = noteService.batchQueryLikeCounts(noteIds);
            Map<Long, Integer> commentCountMap = noteService.batchQueryCommentCounts(noteIds);

            //  转换为NoteSyncVO
            List<NoteSyncVo> syncVOs = notes.stream()
                    .map(note -> convertNoteToSyncVO(note, likeCountMap, commentCountMap))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return RpcResult.ok(syncVOs);

        } catch (Exception e) {
            log.error("同步笔记数据异常: updatedTime={}", updatedTime, e);
            return RpcResult.error(SocialCode.NOTE_SYNC_FAILED);
        }
    }

    /**
     * 批量查询用户对笔记的点赞状态
     * 综合查询数据库和Redis中未同步的点赞事件
     *
     * @param userId 用户ID
     * @param noteIds 笔记ID列表
     * @return 用户已点赞的笔记ID集合（包括数据库和Redis中未同步的）
     */
    @Override
    public RpcResult<Set<Long>> queryUserLikedNoteIds(Long userId, List<Long> noteIds) {
        try {
            if (userId == null || noteIds == null || noteIds.isEmpty()) {
                return RpcResult.ok(Collections.emptySet());
            }
            // 1. 优先从Redis Hash获取pending状态（覆盖大部分近期操作）
            Map<Long, Boolean> pendingStatus = noteLikeSyncService.batchGetPendingLikeStatus(userId, noteIds);
            Set<Long> finalLikedNoteIds = new HashSet<>();
            // 2. Redis中已有明确状态的直接采用
            Set<Long> resolvedNoteIds = new HashSet<>();
            pendingStatus.forEach((noteId, liked) -> {
                resolvedNoteIds.add(noteId);
                if (liked) {
                    finalLikedNoteIds.add(noteId);
                }
            });
            // 3. Redis中没有pending状态的noteId，回退到DB查询
            List<Long> unresolvedNoteIds = noteIds.stream()
                    .filter(noteId -> !resolvedNoteIds.contains(noteId))
                    .toList();
            if (!unresolvedNoteIds.isEmpty()) {
                LambdaQueryWrapper<SocialNoteLike> dbQuery = new LambdaQueryWrapper<>();
                dbQuery.eq(SocialNoteLike::getUserId, userId)
                        .in(SocialNoteLike::getNoteId, unresolvedNoteIds)
                        .eq(SocialNoteLike::getDeleted, false);
                List<SocialNoteLike> dbLikes = noteLikeMapper.selectList(dbQuery);
                dbLikes.forEach(like -> finalLikedNoteIds.add(like.getNoteId()));
            }

            log.info("用户点赞状态查询完成: userId={}, 已点赞笔记数={}", userId, finalLikedNoteIds.size());
            return RpcResult.ok(finalLikedNoteIds);
        } catch (Exception e) {
            log.error("查询用户点赞状态异常: userId={}, noteIds数量={}", userId, noteIds != null ? noteIds.size() : 0, e);
            return RpcResult.error(SocialCode.USER_NOTE_STATISTIC_ERROR);
        }
    }

    /**
     * 将SocialNote转换为NoteSyncVO
     */
    private NoteSyncVo convertNoteToSyncVO(SocialNote note,
                                           Map<Long, Integer> likeCountMap,
                                           Map<Long, Integer> commentCountMap) {
        try {
            if (note == null || note.getNoteId() == null) {
                return null;
            }

            // 解析标签（从JSON字符串转为List）
            List<String> tags = null;
            if (!StringUtil.isNullOrEmpty(note.getTags())) {
                try {
                    tags = List.of(note.getTags().split(";"));
                } catch (Exception e) {
                    log.error("解析笔记标签失败: noteId={}, tags={}", note.getNoteId(), note.getTags(), e);
                    tags = List.of();
                }
            }

            return NoteSyncVo.builder()
                    .noteId(note.getNoteId())
                    .userId(note.getUserId())
                    .title(note.getTitle())
                    .content(note.getContent())
                    .tags(tags != null ? tags : List.of())
                    .coverUrl(note.getCoverUrl())
                    .mediaType(note.getMediaType() != null ? note.getMediaType().name() : null)
                    .likeCount(likeCountMap.getOrDefault(note.getNoteId(), 0))
                    .commentCount(commentCountMap.getOrDefault(note.getNoteId(), 0))
                    .collectCount(note.getCollectCount() != null ? note.getCollectCount() : 0)
                    .featured(note.getFeatured() != null ? note.getFeatured() : false)
                    .status(note.getStatus())
                    .createdAt(note.getCreatedAt())
                    .updatedAt(note.getUpdatedAt())
                    .build();

        } catch (Exception e) {
            log.error("转换SocialNote为SyncVO失败: noteId={}", note.getNoteId(), e);
            return null;
        }
    }

}
