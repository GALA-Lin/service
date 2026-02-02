package com.unlimited.sports.globox.social.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.model.social.entity.SocialNote;
import com.unlimited.sports.globox.model.social.entity.SocialNoteLike;
import com.unlimited.sports.globox.model.social.event.NoteLikeEvent;
import com.unlimited.sports.globox.social.mapper.SocialNoteMapper;
import com.unlimited.sports.globox.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.unlimited.sports.globox.social.consts.SocialRedisKeyConstants.*;

/**
 * 笔记点赞事件异步同步服务
 * 负责从 Redis Set 消费事件，批量同步到数据库
 */
@Service
@Slf4j
public class NoteLikeSyncService {

    @Autowired
    private RedisService redisService;

    @Autowired
    private SocialNoteLikeService socialNoteLikeService;

    @Autowired
    private SocialNoteMapper socialNoteMapper;


    @Autowired
    private JsonUtils jsonUtils;



    @PostConstruct
    void init() {
        log.info("删除旧数据");
        redisService.deleteObject(LIKE_EVENTS_PENDING);
        redisService.deleteObject(LIKE_EVENTS_PROCESSING);
    }
    /**
     * 添加点赞事件到 Redis Set
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param existsInDb 记录在数据库中是否存在
     * @param isDeletedInDb 记录在数据库中是否被软删除
     */
    public void addLikeEvent(Long userId, Long noteId, Boolean existsInDb, Boolean isDeletedInDb) {
        // 1. 创建点赞事件，记录当前数据库状态
        NoteLikeEvent event = NoteLikeEvent.builder()
                .userId(userId)
                .noteId(noteId)
                .action(NoteLikeEvent.LikeAction.LIKE)
                .existsInDb(existsInDb)
                .isDeletedInDb(isDeletedInDb)
                .build();

        // 2. 生成所有可能的旧事件
        List<String> possibleOldEvents = generatePossibleOldEvents(userId, noteId);

        // 3. 写入 Redis Set（删除旧事件，添加新事件）
        addEventToRedis(possibleOldEvents,jsonUtils.objectToJson(event));
    }

    /**
     * 添加取消点赞事件到 Redis Set
     *
     * @param userId 用户ID
     * @param noteId 笔记ID
     * @param existsInDb 记录在数据库中是否存在
     * @param isDeletedInDb 记录在数据库中是否被软删除
     */
    public void addUnlikeEvent(Long userId, Long noteId, Boolean existsInDb, Boolean isDeletedInDb) {
        // 1. 创建取消点赞事件，记录当前数据库状态
        NoteLikeEvent event = NoteLikeEvent.builder()
                .userId(userId)
                .noteId(noteId)
                .action(NoteLikeEvent.LikeAction.UNLIKE)
                .existsInDb(existsInDb)
                .isDeletedInDb(isDeletedInDb)
                .build();

        // 2. 生成所有可能的旧事件
        List<String> possibleOldEvents = generatePossibleOldEvents(userId, noteId);

        // 3. 写入 Redis Set（删除旧事件，添加新事件）
        addEventToRedis(possibleOldEvents,jsonUtils.objectToJson(event));
    }

    /**
     * 生成所有可能的旧事件值（JSON格式）
     * 覆盖所有 action × existsInDb × isDeletedInDb 的组合
     *
     * @return 所有可能的旧事件字符串列表
     */
    private List<String> generatePossibleOldEvents(Long userId, Long noteId) {
        return Arrays.asList(
            jsonUtils.objectToJson(NoteLikeEvent.builder().userId(userId).noteId(noteId).action(NoteLikeEvent.LikeAction.LIKE).existsInDb(false).isDeletedInDb(false).build()),
            jsonUtils.objectToJson(NoteLikeEvent.builder().userId(userId).noteId(noteId).action(NoteLikeEvent.LikeAction.LIKE).existsInDb(true).isDeletedInDb(true).build()),
            jsonUtils.objectToJson(NoteLikeEvent.builder().userId(userId).noteId(noteId).action(NoteLikeEvent.LikeAction.UNLIKE).existsInDb(true).isDeletedInDb(false).build())
        );
    }

    /**
     * 从 Redis Set 获取指定用户的点赞事件（仅返回该用户-笔记的最新事件）
     * 使用 Lua 脚本在 Redis 端检查所有可能的状态
     */
    public NoteLikeEvent getPendingLikeEventFromSet(Long userId, Long noteId) {
        try {
            String luaScript = """
                    local key0 = ARGV[1]
                    local key1 = ARGV[2]
                    local key2 = ARGV[3]
                    if redis.call('SISMEMBER', KEYS[1], key0) == 1 then
                        return key0
                    elseif redis.call('SISMEMBER', KEYS[1], key1) == 1 then
                        return key1
                    elseif redis.call('SISMEMBER', KEYS[1], key2) == 1 then
                        return key2
                    else
                        return nil
                    end
                    """;

            List<String> possibleKeys = generatePossibleOldEvents(userId, noteId);
            String eventValue = redisService.executeLuaScript(luaScript, String.class,
                    Collections.singletonList(LIKE_EVENTS_PENDING),
                    possibleKeys.get(0), possibleKeys.get(1), possibleKeys.get(2));
            if (eventValue == null || eventValue.isEmpty()) {
                return null;
            }
            return jsonUtils.jsonToPojo(eventValue, NoteLikeEvent.class);
        } catch (Exception e) {
            log.warn("Failed to read pending like events from set: userId={}, noteId={}", userId, noteId, e);
            return null;
        }
    }

    /**
     * 批量获取用户在 Redis 中未同步的点赞笔记ID
     * 只检查两种点赞状态：
     * 1. LIKE + !existsInDb + !isDeletedInDb（新点赞）
     * 2. LIKE + existsInDb + isDeletedInDb（恢复点赞）
     *
     * @param userId 用户ID
     * @param noteIds 笔记ID列表
     * @return Redis中未同步的已点赞笔记ID集合
     */
    public Set<Long> batchGetPendingLikedNoteIds(Long userId, List<Long> noteIds) {
        if (userId == null || noteIds == null || noteIds.isEmpty()) {
            return Collections.emptySet();
        }

        try {
            // 构造所有可能的点赞事件 JSON 和对应的 noteId 映射
            Map<String, Long> eventToNoteIdMap = new HashMap<>();
            noteIds.forEach(noteId -> {
                // 点赞状态1: LIKE + !existsInDb + !isDeletedInDb（新点赞）
                String likeEvent1 = jsonUtils.objectToJson(NoteLikeEvent.builder()
                        .userId(userId)
                        .noteId(noteId)
                        .action(NoteLikeEvent.LikeAction.LIKE)
                        .existsInDb(false)
                        .isDeletedInDb(false)
                        .build());
                eventToNoteIdMap.put(likeEvent1, noteId);
                
                // 点赞状态2: LIKE + existsInDb + isDeletedInDb（恢复点赞）
                String likeEvent2 = jsonUtils.objectToJson(NoteLikeEvent.builder()
                        .userId(userId)
                        .noteId(noteId)
                        .action(NoteLikeEvent.LikeAction.LIKE)
                        .existsInDb(true)
                        .isDeletedInDb(true)
                        .build());
                eventToNoteIdMap.put(likeEvent2, noteId);
            });

            // 批量检查是否存在
            Map<Object, Boolean> memberResults = redisService.isMember(LIKE_EVENTS_PENDING, eventToNoteIdMap.keySet().toArray());
            
            // 过滤出存在的事件对应的 noteId
            return memberResults.entrySet().stream()
                    .filter(entry -> Boolean.TRUE.equals(entry.getValue()))
                    .map(entry -> eventToNoteIdMap.get(entry.getKey().toString()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("批量获取pending点赞状态失败: userId={}, noteIds={}", userId, noteIds, e);
            return Collections.emptySet();
        }
    }

    /**
     * 批量获取用户在 Redis 中未同步的取消点赞笔记ID
     * 只检查一种取消点赞状态：
     * UNLIKE + existsInDb + !isDeletedInDb（取消点赞）
     *
     * @param userId 用户ID
     * @param noteIds 笔记ID列表
     * @return Redis中未同步的已取消点赞笔记ID集合
     */
    public Set<Long> batchGetPendingUnlikedNoteIds(Long userId, List<Long> noteIds) {
        if (userId == null || noteIds == null || noteIds.isEmpty()) {
            return Collections.emptySet();
        }

        try {
            // 构造取消点赞事件 JSON 和对应的 noteId 映射
            Map<String, Long> eventToNoteIdMap = noteIds.stream()
                    .collect(Collectors.toMap(
                            noteId -> jsonUtils.objectToJson(NoteLikeEvent.builder()
                                    .userId(userId)
                                    .noteId(noteId)
                                    .action(NoteLikeEvent.LikeAction.UNLIKE)
                                    .existsInDb(true)
                                    .isDeletedInDb(false)
                                    .build()),
                            noteId -> noteId
                    ));

            // 批量检查是否存在
            Map<Object, Boolean> memberResults = redisService.isMember(LIKE_EVENTS_PENDING, eventToNoteIdMap.keySet().toArray());
            
            // 过滤出存在的事件对应的 noteId
            return memberResults.entrySet().stream()
                    .filter(entry -> Boolean.TRUE.equals(entry.getValue()))
                    .map(entry -> eventToNoteIdMap.get(entry.getKey().toString()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("批量获取pending取消点赞状态失败: userId={}, noteIds={}", userId, noteIds, e);
            return Collections.emptySet();
        }
    }

    /**
     * 将事件添加到 Redis Set
     * 删除所有可能的旧事件，然后添加新事件
     *
     * @param possibleOldEvents 所有可能的旧事件列表（3种组合）
     * @param newEvent 新事件字符串
     */
    private void addEventToRedis(List<String> possibleOldEvents, String newEvent) {
        // 先删除所有可能的旧事件
        redisService.deleteMember(LIKE_EVENTS_PENDING, possibleOldEvents.toArray());
        // 再添加新事件
        redisService.addMember(LIKE_EVENTS_PENDING, newEvent);
    }

    /**
     * 定时任务：每分钟同步一次 Redis Set 事件到数据库
     * 流程：
     * 1. Lua 脚本从 pending 移动  到 processing
     * 2. 解析事件，按 (userId:noteId) 去重，取最新事件
     * 3. 根据事件中的状态字段判断是 insert/restore/delete
     * 4. 批量执行数据库操作
     * 5. 成功后从 processing 删除，失败则保留以供重试
     */
    @Scheduled(fixedRate = 60000)  // 每 60000ms（1分钟）执行一次
    public void syncLikeEventsToDb() {
        try {
            log.info("开始同步点赞数据到数据库");

            // 1. 从 pending 获取所有事件
            Set<String> pendingMembers = redisService.getSetMembers(LIKE_EVENTS_PENDING);
            
            if (pendingMembers == null || pendingMembers.isEmpty()) {
                log.info("没有需要同步的点赞事件");
                return;
            }
            
            // 2. 移动到 processing（先添加到 processing，再从 pending 删除）
            List<String> events = new ArrayList<>(pendingMembers);
            for (String event : events) {
                redisService.addMember(LIKE_EVENTS_PROCESSING, event);
                redisService.deleteMember(LIKE_EVENTS_PENDING, event);
            }
            
            if (events.isEmpty()) {
                log.info("没有需要同步的点赞事件");
                return;
            }

            log.debug("取出 {} events 移动到 processing set", events.size());

            // 2. 解析事件对象
            List<NoteLikeEvent> likeEvents = events.stream()
                    .<NoteLikeEvent>map(eventValue -> jsonUtils.jsonToPojo(eventValue, NoteLikeEvent.class))
                    .filter(Objects::nonNull)
                    .toList();

            if (likeEvents.isEmpty()) {
                log.warn("获取到事件为空或无法解析");
                // 删除无法解析的事件
                redisService.deleteMember(LIKE_EVENTS_PROCESSING, events.toArray());
                return;
            }

            // 3. 按 (userId:noteId) 去重，由于 Set 中同一 key 只有一个事件，直接取即可
            Map<String, NoteLikeEvent> finalStates = likeEvents.stream()
                    .collect(Collectors.toMap(
                            NoteLikeEvent::toEventKey,
                            event -> event,
                            (existing, current) -> current
                    ));

            // 4. 批量同步到数据库
            List<String> successfulEvents = batchSyncToDB(new ArrayList<>(finalStates.values()));

            // 5. 从 processing 删除已同步成功的事件
            if (!successfulEvents.isEmpty()) {
                redisService.deleteMember(LIKE_EVENTS_PROCESSING, successfulEvents.toArray());
                log.info("Like events sync completed: {} events processed successfully", successfulEvents.size());
            }

        } catch (Exception e) {
            log.error("Error during like events sync", e);
            // 不抛出异常，避免打断定时任务的后续执行
        }
    }

    /**
     * 批量同步到数据库
     * 完全根据事件中的状态字段进行操作，无需查询数据库：
     * - LIKE + !existsInDb → insert
     * - LIKE + existsInDb + isDeletedInDb → update SET deleted=false
     * - UNLIKE + existsInDb + !isDeletedInDb → update SET deleted=true
     *
     * @return 同步成功的事件列表（完整的 Set value）
     */
    private List<String> batchSyncToDB (List<NoteLikeEvent> events) {
        if (events.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> successfulEvents = new ArrayList<>();
        List<SocialNoteLike> toInsert = new ArrayList<>();
        List<NoteLikeEvent> toRestore = new ArrayList<>();      // LIKE + 已软删除 → 恢复
        List<NoteLikeEvent> toDelete = new ArrayList<>();       // UNLIKE + 存在未删 → 软删除
        Map<Long, Integer> noteCountChanges = new HashMap<>();
        // 1. 分类事件（只有 3 种业务上真正存在的状态）
        for (NoteLikeEvent event : events) {
            try {
                if (event.getAction() == NoteLikeEvent.LikeAction.LIKE) {
                    if (!event.getExistsInDb()) {
                        // LIKE:0:0 - 完全新增
                        SocialNoteLike like = new SocialNoteLike();
                        like.setUserId(event.getUserId());
                        like.setNoteId(event.getNoteId());
                        like.setCreatedAt(LocalDateTime.now());
                        like.setDeleted(false);
                        toInsert.add(like);
                        noteCountChanges.put(event.getNoteId(),
                                noteCountChanges.getOrDefault(event.getNoteId(), 0) + 1);
                    } else {
                        // LIKE:1:1 - 恢复已软删除的
                        toRestore.add(event);
                        noteCountChanges.put(event.getNoteId(),
                                noteCountChanges.getOrDefault(event.getNoteId(), 0) + 1);
                    }
                    successfulEvents.add(jsonUtils.objectToJson(event));
                } else {
                    // UNLIKE:1:0 - 取消点赞（软删除）
                    toDelete.add(event);
                    noteCountChanges.put(event.getNoteId(),
                            noteCountChanges.getOrDefault(event.getNoteId(), 0) - 1);
                    successfulEvents.add(jsonUtils.objectToJson(event));
                }
            } catch (Exception e) {
                log.error("Failed to process like event: userId={}, noteId={}, action={}",
                        event.getUserId(), event.getNoteId(), event.getAction(), e);
                // 该事件处理失败，不加入 successfulEvents，会在 processing 中保留供下次重试
            }
        }

        // 2. 批量执行数据库操作
        try {
            // 插入新的点赞记录
            if (!toInsert.isEmpty()) {
                socialNoteLikeService.saveBatch(toInsert);
            }

            // 恢复软删除的记录（按 (userId, noteId) 条件批量更新）
            if (!toRestore.isEmpty()) {
                LambdaUpdateWrapper<SocialNoteLike> restoreWrapper = new LambdaUpdateWrapper<>();
                toRestore.forEach(event -> {
                    restoreWrapper.or(w -> w.eq(SocialNoteLike::getUserId, event.getUserId())
                            .eq(SocialNoteLike::getNoteId, event.getNoteId()));
                });
                restoreWrapper.set(SocialNoteLike::getDeleted, false);
                socialNoteLikeService.update(null, restoreWrapper);
            }

            // 软删除存在的记录（按 (userId, noteId) 条件批量更新）
            if (!toDelete.isEmpty()) {
                LambdaUpdateWrapper<SocialNoteLike> deleteWrapper = new LambdaUpdateWrapper<>();
                toDelete.forEach(event -> {
                    deleteWrapper.or(w -> w.eq(SocialNoteLike::getUserId, event.getUserId())
                            .eq(SocialNoteLike::getNoteId, event.getNoteId()));
                });
                deleteWrapper.set(SocialNoteLike::getDeleted, true);
                socialNoteLikeService.update(null, deleteWrapper);
            }

            // 一次性批量更新所有笔记的点赞计数
            if (!noteCountChanges.isEmpty()) {
                updateNoteLikeCounts(noteCountChanges);
            }

            log.debug("Synced {} like events to database", successfulEvents.size());

        } catch (Exception e) {
            log.error("Failed to batch sync like events to database", e);
            // 数据库操作失败，不返回任何 successfulEvents，所有事件都会保留在 processing 中供重试
            return Collections.emptyList();
        }

        return successfulEvents;
    }

    /**
     * 一次性批量更新所有笔记的点赞计数
     * 使用 CASE WHEN SQL 语句，避免多次数据库往返
     */
    private void updateNoteLikeCounts(Map<Long, Integer> noteCountChanges) {
        try {
            // 构造 CASE WHEN SQL 语句
            StringBuilder caseSql = new StringBuilder("like_count = CASE ");
            for (Map.Entry<Long, Integer> entry : noteCountChanges.entrySet()) {
                Long noteId = entry.getKey();
                Integer delta = entry.getValue();

                if (delta > 0) {
                    caseSql.append("WHEN note_id = ").append(noteId)
                            .append(" THEN like_count + ").append(delta).append(" ");
                } else if (delta < 0) {
                    // 减少时确保不为负
                    caseSql.append("WHEN note_id = ").append(noteId)
                            .append(" THEN CASE WHEN like_count >= ").append(-delta)
                            .append(" THEN like_count - ").append(-delta)
                            .append(" ELSE 0 END ");
                }
            }
            caseSql.append("ELSE like_count END");

            // 批量更新
            List<Long> allNoteIds = new ArrayList<>(noteCountChanges.keySet());
            LambdaUpdateWrapper<SocialNote> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.in(SocialNote::getNoteId, allNoteIds)
                    .setSql(caseSql.toString());
            socialNoteMapper.update(null, updateWrapper);

            log.debug("Updated like counts for {} notes", allNoteIds.size());
        } catch (Exception e) {
            log.warn("Failed to update like counts for notes", e);
        }
    }


}
