package com.unlimited.sports.globox.social.util;

import com.unlimited.sports.globox.common.constants.SearchMQConstants;
import com.unlimited.sports.globox.model.social.entity.SocialNote;
import com.unlimited.sports.globox.model.social.vo.NoteSyncBatchMessage;
import com.unlimited.sports.globox.model.social.vo.NoteSyncVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 笔记同步MQ消息发送器
 */
@Slf4j
@Component
public class NoteSyncMQSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送笔记同步消息到MQ（支持批量）
     */
    public void sendNoteSyncMessage(List<SocialNote> notes) {
        if (notes == null || notes.isEmpty()) {
            log.warn("[笔记同步MQ] 发送记录为空");
            return;
        }

        try {
            List<NoteSyncVo> syncVos = notes.stream()
                    .filter(note -> note != null && note.getNoteId() != null)
                    .map(this::buildNoteSyncVo)
                    .toList();

            if (syncVos.isEmpty()) {
                log.warn("[笔记同步MQ] 发送记录为空");
                return;
            }

            NoteSyncBatchMessage message = new NoteSyncBatchMessage(syncVos);

            rabbitTemplate.convertAndSend(
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
    private NoteSyncVo buildNoteSyncVo(SocialNote note) {
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
                .likeCount(note.getLikeCount())
                .commentCount(note.getCommentCount())
                .collectCount(note.getCollectCount())
                .featured(note.getFeatured() != null ? note.getFeatured() : false)
                .status(note.getStatus())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}
