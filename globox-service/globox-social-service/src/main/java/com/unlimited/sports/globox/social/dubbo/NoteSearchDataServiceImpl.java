package com.unlimited.sports.globox.social.dubbo;

import com.alibaba.nacos.shaded.io.grpc.netty.shaded.io.netty.util.internal.StringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.result.SocialCode;
import com.unlimited.sports.globox.dubbo.social.INoteSearchDataService;
import com.unlimited.sports.globox.model.social.dto.NoteStatisticsDto;
import com.unlimited.sports.globox.model.social.entity.SocialNote;
import com.unlimited.sports.globox.model.social.entity.SocialNoteComment;
import com.unlimited.sports.globox.model.social.entity.SocialNoteLike;
import com.unlimited.sports.globox.model.social.vo.NoteSyncVo;
import com.unlimited.sports.globox.social.mapper.SocialNoteCommentMapper;
import com.unlimited.sports.globox.social.mapper.SocialNoteLikeMapper;
import com.unlimited.sports.globox.social.mapper.SocialNoteMapper;
import com.unlimited.sports.globox.social.service.NoteLikeSyncService;
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
    private SocialNoteCommentMapper noteCommentMapper;

    @Autowired
    private NoteLikeSyncService noteLikeSyncService;

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

            //  转换为NoteSyncVO
            List<NoteSyncVo> syncVOs = notes.stream()
                    .map(this::convertNoteToSyncVO)
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

            log.info("查询用户点赞状态: userId={}, noteIds数量={}", userId, noteIds.size());

            // 1. 查询数据库中用户已点赞的笔记（deleted=false）
            LambdaQueryWrapper<SocialNoteLike> dbQuery = new LambdaQueryWrapper<>();
            dbQuery.eq(SocialNoteLike::getUserId, userId)
                    .in(SocialNoteLike::getNoteId, noteIds)
                    .eq(SocialNoteLike::getDeleted, false);
            List<SocialNoteLike> dbLikes = noteLikeMapper.selectList(dbQuery);

            Set<Long> dbLikedNoteIds = dbLikes.stream()
                    .map(SocialNoteLike::getNoteId)
                    .collect(Collectors.toSet());
            log.debug("数据库中已点赞的笔记: {}", dbLikedNoteIds.size());

            // 2. 查询Redis中未同步的点赞事件
            // 包含两种状态：
            // - LIKE + !existsInDb + !isDeletedInDb（新点赞）
            // - LIKE + existsInDb + isDeletedInDb（恢复点赞）
            Set<Long> pendingLikedNoteIds = noteLikeSyncService.batchGetPendingLikedNoteIds(userId, noteIds);
            log.debug("Redis中待同步的点赞笔记: {}", pendingLikedNoteIds.size());

            // 3. 查询Redis中未同步的取消点赞事件
            // UNLIKE + existsInDb + !isDeletedInDb（取消点赞）
            Set<Long> pendingUnlikedNoteIds = noteLikeSyncService.batchGetPendingUnlikedNoteIds(userId, noteIds);
            log.debug("Redis中待同步的取消点赞笔记: {}", pendingUnlikedNoteIds.size());

            // 4. 合并结果：(数据库已点赞 + Redis点赞) - Redis取消点赞
            Set<Long> finalLikedNoteIds = new HashSet<>();
            finalLikedNoteIds.addAll(dbLikedNoteIds);
            finalLikedNoteIds.addAll(pendingLikedNoteIds);
            finalLikedNoteIds.removeAll(pendingUnlikedNoteIds);

            log.info("用户点赞状态查询完成: userId={}, 最终已点赞笔记数={}", userId, finalLikedNoteIds.size());
            return RpcResult.ok(finalLikedNoteIds);

        } catch (Exception e) {
            log.error("查询用户点赞状态异常: userId={}, noteIds数量={}", userId, noteIds != null ? noteIds.size() : 0, e);
            return RpcResult.error(SocialCode.USER_NOTE_STATISTIC_ERROR);
        }
    }

    /**
     * 批量查询笔记统计信息
     *
     * @param noteIds 笔记ID列表
     * @param userId 当前用户ID
     * @return 笔记统计信息Map
     */
    @Override
    public RpcResult<Map<Long, NoteStatisticsDto>> queryNotesStatistics(List<Long> noteIds, Long userId) {
        try {
            if (noteIds == null || noteIds.isEmpty()) {
                return RpcResult.ok(Collections.emptyMap());
            }

            log.info("批量查询笔记统计信息: noteIds数量={}, userId={}", noteIds.size(), userId);

            // 查询点赞数
            Map<Long, Integer> likeCounts = queryNoteLikeCounts(noteIds);

            // 查询评论数
            Map<Long, Integer> commentCounts = queryNoteCommentCounts(noteIds);

            // 查询用户点赞状态和未同步的增量
            Set<Long> userLikedNoteIds = Collections.emptySet();
            Map<Long, Integer> likeDelta = new HashMap<>();

            if (userId != null && userId > 0) {
                // 查询未同步的点赞事件
                Set<Long> pendingLiked = noteLikeSyncService.batchGetPendingLikedNoteIds(userId, noteIds);
                Set<Long> pendingUnliked = noteLikeSyncService.batchGetPendingUnlikedNoteIds(userId, noteIds);

                // 计算增量：点赞+1，取消点赞-1
                pendingLiked.forEach(noteId -> likeDelta.put(noteId, 1));
                pendingUnliked.forEach(noteId -> likeDelta.put(noteId, -1));

                // 查询用户点赞状态
                RpcResult<Set<Long>> likedResult = queryUserLikedNoteIds(userId, noteIds);
                if (likedResult.isSuccess() && likedResult.getData() != null) {
                    userLikedNoteIds = likedResult.getData();
                }
            }

            // 合并结果
            Set<Long> finalUserLikedNoteIds = userLikedNoteIds;
            Map<Long, NoteStatisticsDto> resultMap = noteIds.stream()
                    .collect(Collectors.toMap(
                            noteId -> noteId,
                            noteId -> {
                                int likeCount = likeCounts.getOrDefault(noteId, 0) + likeDelta.getOrDefault(noteId, 0);
                                int commentCount = commentCounts.getOrDefault(noteId, 0);
                                boolean isLiked = finalUserLikedNoteIds.contains(noteId);

                                return NoteStatisticsDto.builder()
                                        .noteId(noteId)
                                        .likeCount(Math.max(0, likeCount))
                                        .commentCount(Math.max(0, commentCount))
                                        .isLiked(isLiked)
                                        .build();
                            }
                    ));

            log.info("笔记统计信息查询完成: {} 条", resultMap.size());
            return RpcResult.ok(resultMap);

        } catch (Exception e) {
            log.error("批量查询笔记统计信息异常: noteIds数量={}", noteIds != null ? noteIds.size() : 0, e);
            return RpcResult.error(SocialCode.USER_NOTE_STATISTIC_ERROR);
        }
    }

    /**
     * 查询笔记点赞数
     */
    private Map<Long, Integer> queryNoteLikeCounts(List<Long> noteIds) {
        QueryWrapper<SocialNoteLike> wrapper = new QueryWrapper<>();
        wrapper.select("note_id, COUNT(*) as count")
                .in("note_id", noteIds)
                .eq("deleted", false)
                .groupBy("note_id");

        List<Map<String, Object>> results = noteLikeMapper.selectMaps(wrapper);

        return results.stream()
                .collect(Collectors.toMap(
                        map -> ((Number) map.get("note_id")).longValue(),
                        map -> ((Number) map.get("count")).intValue()
                ));
    }

    /**
     * 查询笔记评论数
     */
    private Map<Long, Integer> queryNoteCommentCounts(List<Long> noteIds) {
        QueryWrapper<SocialNoteComment> wrapper = new QueryWrapper<>();
        wrapper.select("note_id, COUNT(*) as count")
                .in("note_id", noteIds)
                .eq("status", SocialNoteComment.Status.PUBLISHED)
                .groupBy("note_id");

        List<Map<String, Object>> results = noteCommentMapper.selectMaps(wrapper);

        return results.stream()
                .collect(Collectors.toMap(
                        map -> ((Number) map.get("note_id")).longValue(),
                        map -> ((Number) map.get("count")).intValue()
                ));
    }


    /**
     * 将SocialNote转换为NoteSyncVO
     */
    private NoteSyncVo convertNoteToSyncVO(SocialNote note) {
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
                    .likeCount(note.getLikeCount() != null ? note.getLikeCount() : 0)
                    .commentCount(note.getCommentCount() != null ? note.getCommentCount() : 0)
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
