package com.unlimited.sports.globox.social.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.unlimited.sports.globox.common.constants.SearchMQConstants;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.model.search.dto.NoteEngagementSyncMessage;
import com.unlimited.sports.globox.model.search.dto.NoteEngagementSyncMessage.NoteEngagementItem;
import com.unlimited.sports.globox.model.social.entity.SocialNoteLike;
import com.unlimited.sports.globox.model.social.event.NoteLikeEvent;
import com.unlimited.sports.globox.social.mapper.SocialNoteLikeMapper;
import com.unlimited.sports.globox.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

import static com.unlimited.sports.globox.social.consts.SocialRedisKeyConstants.*;

/**
 * 笔记点赞事件异步同步服务
 *
 * Redis Hash 结构：
 *   key:   note:like:pending
 *   field: {userId}:{noteId}
 *   value: NoteLikeEvent JSON（包含 userId, noteId, likeStatus, likeTime）
 *
 * 同步策略：RENAME 快照
 *   1. RENAME pending → processing（原子操作，新写入自动创建新 pending）
 *   2. 从 processing 读取并批量同步到 DB
 *   3. 同步成功后 DEL processing
 */
@Service
@Slf4j
public class NoteLikeSyncService {

    @Autowired
    private RedisService redisService;

    @Autowired
    private JsonUtils jsonUtils;

    @Autowired
    private SocialNoteLikeService socialNoteLikeService;

    @Autowired
    private SocialNoteLikeMapper socialNoteLikeMapper;

    @Autowired
    private MQService mqService;

    /**
     * 添加点赞事件到 Redis Hash
     */
    public void addLikeEvent(Long userId, Long noteId) {
        NoteLikeEvent event = NoteLikeEvent.builder()
                .userId(userId)
                .noteId(noteId)
                .likeStatus(NoteLikeEvent.STATUS_LIKE)
                .likeTime(LocalDateTime.now())
                .build();
        String field = buildLikedMapField(userId, noteId);
        redisService.setCacheMapValue(LIKE_EVENTS_PENDING, field, jsonUtils.objectToJson(event));
    }

    /**
     * 添加取消点赞事件到 Redis Hash
     */
    public void addUnlikeEvent(Long userId, Long noteId) {
        NoteLikeEvent event = NoteLikeEvent.builder()
                .userId(userId)
                .noteId(noteId)
                .likeStatus(NoteLikeEvent.STATUS_UNLIKE)
                .likeTime(LocalDateTime.now())
                .build();
        String field = buildLikedMapField(userId, noteId);
        redisService.setCacheMapValue(LIKE_EVENTS_PENDING, field, jsonUtils.objectToJson(event));
    }

    /**
     * 获取单个用户对某笔记的 pending 点赞状态
     *
     * @return null=无 pending 事件, true=pending 点赞, false=pending 取消点赞
     */
    public Boolean getPendingLikeStatus(Long userId, Long noteId) {
        try {
            String field = buildLikedMapField(userId, noteId);
            String json = redisService.getHashValue(LIKE_EVENTS_PENDING, field, String.class);
            if (json == null) {
                return null;
            }
            NoteLikeEvent event = jsonUtils.jsonToPojo(json, NoteLikeEvent.class);
            return event.isLike();
        } catch (Exception e) {
            log.warn("获取pending点赞状态失败: userId={}, noteId={}", userId, noteId, e);
            return null;
        }
    }

    /**
     * 批量获取用户在 Redis 中未同步的点赞/取消点赞状态
     * 一次 HMGET 拿到所有结果，无需循环
     *
     * @param userId  用户ID
     * @param noteIds 笔记ID列表
     * @return key=noteId, value=true(点赞)/false(取消点赞)，不在 pending 中的 noteId 不包含在结果中
     */
    public Map<Long, Boolean> batchGetPendingLikeStatus(Long userId, List<Long> noteIds) {
        if (userId == null || noteIds == null || noteIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            // 一次 HMGET
            List<String> hKeys = noteIds.stream()
                    .map(noteId -> buildLikedMapField(userId, noteId))
                    .toList();
            List<String> values = redisService.getMultiCacheMapValue(LIKE_EVENTS_PENDING, hKeys, new TypeReference<>() {});
            if (values == null) {
                return Collections.emptyMap();
            }
            // 解析结果（单条解析异常不影响其他）
            Map<Long, Boolean> result = new HashMap<>();
            for (int i = 0; i < noteIds.size() && i < values.size(); i++) {
                String json = values.get(i);
                if (json == null) {
                    continue;
                }
                try {
                    NoteLikeEvent event = jsonUtils.jsonToPojo(json, NoteLikeEvent.class);
                    result.put(noteIds.get(i), event.isLike());
                } catch (Exception e) {
                    log.warn("解析pending点赞事件失败: noteId={}, json={}", noteIds.get(i), json, e);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("批量获取pending点赞状态失败: userId={}", userId, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 执行同步任务（供定时任务调用）：
     * 1. 将 Redis Hash 中的点赞事件刷到 DB
     * 2. 将脏 noteId 的互动数据（likeCount、commentCount、collectCount）同步到搜索服务
     */
    public void executeSync() {
        // 第一步：刷点赞事件到 DB
        flushLikeEventsToDb();
        // 第二步：同步互动数据到 ES
        syncEngagementToEs();
    }

    /**
     * 将 Redis Hash 中的点赞/取消点赞事件刷到数据库
     */
    private void flushLikeEventsToDb() {
        try {
            if (!Boolean.TRUE.equals(redisService.hasKey(LIKE_EVENTS_PENDING))) {
                log.debug("[点赞刷DB] 无待同步事件，跳过");
                return;
            }
            try {
                redisService.renameKey(LIKE_EVENTS_PENDING, LIKE_EVENTS_PROCESSING);
            } catch (Exception e) {
                log.warn("[点赞刷DB] RENAME 失败（pending 可能为空）: {}", e.getMessage());
                return;
            }
            Map<String, String> entries = redisService.getCacheMap(LIKE_EVENTS_PROCESSING, new TypeReference<>() {});
            if (entries == null || entries.isEmpty()) {
                redisService.deleteObject(LIKE_EVENTS_PROCESSING);
                log.debug("[点赞刷DB] 无待同步事件，跳过");
                return;
            }
            log.info("[点赞刷DB] 开始同步: count={}", entries.size());
            List<NoteLikeEvent> likeEvents = new ArrayList<>();
            List<NoteLikeEvent> unlikeEvents = new ArrayList<>();
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                try {
                    NoteLikeEvent event = jsonUtils.jsonToPojo(entry.getValue(), NoteLikeEvent.class);
                    if (event.isLike()) {
                        likeEvents.add(event);
                    } else {
                        unlikeEvents.add(event);
                    }
                } catch (Exception e) {
                    log.warn("[点赞刷DB] 解析事件失败: field={}, value={}", entry.getKey(), entry.getValue(), e);
                }
            }
            syncToDB(likeEvents, unlikeEvents);
            redisService.deleteObject(LIKE_EVENTS_PROCESSING);
            log.info("[点赞刷DB] 同步完成: like={}, unlike={}", likeEvents.size(), unlikeEvents.size());

        } catch (Exception e) {
            log.error("[点赞刷DB] 同步失败（processing 保留以供重试）", e);
        }
    }

    /**
     * 将增量计数器同步到搜索服务（ES）
     * RENAME 快照两个 delta Hash，合并后发送 MQ
     */
    private void syncEngagementToEs() {
        try {
            // 1. 快照点赞增量
            Map<String, String> likeDeltaMap = snapshotHash(NOTE_LIKE_DELTA, NOTE_LIKE_DELTA_PROCESSING);
            // 2. 快照评论增量
            Map<String, String> commentDeltaMap = snapshotHash(NOTE_COMMENT_DELTA, NOTE_COMMENT_DELTA_PROCESSING);
            if (likeDeltaMap.isEmpty() && commentDeltaMap.isEmpty()) {
                log.debug("[互动同步] 无增量数据，跳过");
                return;
            }
            // 3. 合并所有 noteId
            Set<Long> allNoteIds = new HashSet<>();
            likeDeltaMap.keySet().forEach(k -> allNoteIds.add(Long.parseLong(k)));
            commentDeltaMap.keySet().forEach(k -> allNoteIds.add(Long.parseLong(k)));
            // 4. 构建增量消息
            List<NoteEngagementItem> items = new ArrayList<>(allNoteIds.size());
            for (Long noteId : allNoteIds) {
                int likeDelta = parseDelta(likeDeltaMap.get(noteId.toString()));
                int commentDelta = parseDelta(commentDeltaMap.get(noteId.toString()));
                if (likeDelta == 0 && commentDelta == 0) {
                    continue;
                }
                items.add(NoteEngagementItem.builder()
                        .noteId(noteId)
                        .likeDelta(likeDelta)
                        .commentDelta(commentDelta)
                        .build());
            }
            if (items.isEmpty()) {
                log.debug("[互动同步] 增量全为0，跳过");
                cleanupProcessing();
                return;
            }
            mqService.send(
                    SearchMQConstants.EXCHANGE_TOPIC_SEARCH,
                    SearchMQConstants.ROUTING_NOTE_ENGAGEMENT_SYNC,
                    NoteEngagementSyncMessage.builder().items(items).build()
            );
            cleanupProcessing();
            log.info("[互动同步] 同步完成: noteCount={}", items.size());

        } catch (Exception e) {
            log.error("[互动同步] 同步失败（processing 保留以供重试）", e);
        }
    }

    /**
     * RENAME 快照一个 Hash，返回其所有 field-value；如果 key 不存在则返回空 Map
     */
    private Map<String, String> snapshotHash(String sourceKey, String processingKey) {
        if (!Boolean.TRUE.equals(redisService.hasKey(sourceKey))) {
            return Collections.emptyMap();
        }
        try {
            redisService.renameKey(sourceKey, processingKey);
        } catch (Exception e) {
            log.warn("[互动同步] RENAME 失败 {}: {}", sourceKey, e.getMessage());
            return Collections.emptyMap();
        }
        Map<String, String> entries = redisService.getCacheMap(processingKey, new TypeReference<>() {});
        return entries != null ? entries : Collections.emptyMap();
    }

    private void cleanupProcessing() {
        redisService.deleteObject(NOTE_LIKE_DELTA_PROCESSING);
        redisService.deleteObject(NOTE_COMMENT_DELTA_PROCESSING);
    }

    private int parseDelta(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 批量同步到数据库（只处理点赞记录，不维护 social_note 的计数字段）
     * - 点赞: INSERT ... ON DUPLICATE KEY UPDATE deleted=false
     * - 取消点赞: UPDATE SET deleted=true WHERE (userId, noteId)
     */
    private void syncToDB(List<NoteLikeEvent> likeEvents, List<NoteLikeEvent> unlikeEvents) {
        // 1. 处理点赞事件：upsert
        if (!likeEvents.isEmpty()) {
            for (NoteLikeEvent event : likeEvents) {
                try {
                    socialNoteLikeMapper.upsertLike(event.getUserId(), event.getNoteId());
                } catch (Exception e) {
                    log.error("点赞 upsert 失败: userId={}, noteId={}", event.getUserId(), event.getNoteId(), e);
                }
            }
        }

        // 2. 处理取消点赞事件：软删除
        if (!unlikeEvents.isEmpty()) {
            LambdaUpdateWrapper<SocialNoteLike> deleteWrapper = new LambdaUpdateWrapper<>();
            unlikeEvents.forEach(event -> {
                deleteWrapper.or(w -> w.eq(SocialNoteLike::getUserId, event.getUserId())
                        .eq(SocialNoteLike::getNoteId, event.getNoteId())
                        .eq(SocialNoteLike::getDeleted, false));
            });
            deleteWrapper.set(SocialNoteLike::getDeleted, true);
            socialNoteLikeService.update(null, deleteWrapper);
        }
    }

    private static String buildLikedMapField(Long userId, Long noteId) {
        return userId + ":" + noteId;
    }
}
