package com.unlimited.sports.globox.social.util;

import com.unlimited.sports.globox.common.constants.SearchMQConstants;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.model.social.entity.SocialNote;
import com.unlimited.sports.globox.model.social.vo.NoteSyncBatchMessage;
import com.unlimited.sports.globox.model.social.vo.NoteSyncVo;
import com.unlimited.sports.globox.social.service.NoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 笔记同步MQ消息发送器
 */
@Slf4j
@Component
public class NoteSyncMQSender {

    @Autowired
    private MQService mqService;

    @Lazy
    @Autowired
    private NoteService noteService;

    /**
     * 发送笔记同步消息到MQ（支持批量）
     */
    public void sendNoteSyncMessage(List<SocialNote> notes) {
        if (notes == null || notes.isEmpty()) {
            log.warn("[笔记同步MQ] 发送记录为空");
            return;
        }

        try {
            List<SocialNote> validNotes = notes.stream()
                    .filter(note -> note != null && note.getNoteId() != null)
                    .toList();

            if (validNotes.isEmpty()) {
                log.warn("[笔记同步MQ] 发送记录为空");
                return;
            }

            // 批量查询实际点赞数和评论数
            List<Long> noteIds = validNotes.stream()
                    .map(SocialNote::getNoteId)
                    .collect(Collectors.toList());
            Map<Long, Integer> likeCountMap = noteService.batchQueryLikeCounts(noteIds);
            Map<Long, Integer> commentCountMap = noteService.batchQueryCommentCounts(noteIds);

            List<NoteSyncVo> syncVos = validNotes.stream()
                    .map(note -> buildNoteSyncVo(note, likeCountMap, commentCountMap))
                    .toList();

            NoteSyncBatchMessage message = new NoteSyncBatchMessage(syncVos);

            mqService.send(
                    SearchMQConstants.EXCHANGE_TOPIC_SEARCH,
                    SearchMQConstants.ROUTING_NOTE_SYNC,
                    message
            );

            log.info("[笔记同步MQ] 发送成功- count={}", syncVos.size());
        } catch (Exception e) {
            log.error("[笔记同步MQ] 发送失败- count={}", notes.size(), e);
        }
    }

    /**
     * 构建NoteSyncVo
     */
    private NoteSyncVo buildNoteSyncVo(SocialNote note,
                                        Map<Long, Integer> likeCountMap,
                                        Map<Long, Integer> commentCountMap) {
        List<String> tagList = null;
        if (note.getTags() != null && !note.getTags().isEmpty()) {
            tagList = Arrays.asList(note.getTags().split(";"));
        }

        return NoteSyncVo.builder()
                .noteId(note.getNoteId())
                .userId(note.getUserId())
                .title(note.getTitle())
                .content(note.getContent())
                .tags(tagList)
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
    }
}
