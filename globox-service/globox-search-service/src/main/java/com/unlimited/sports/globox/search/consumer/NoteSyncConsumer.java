package com.unlimited.sports.globox.search.consumer;

import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.constants.SearchMQConstants;
import com.unlimited.sports.globox.model.search.enums.SearchDocTypeEnum;
import com.unlimited.sports.globox.model.social.entity.SocialNote;
import com.unlimited.sports.globox.model.social.vo.NoteSyncBatchMessage;
import com.unlimited.sports.globox.model.social.vo.NoteSyncVo;
import com.unlimited.sports.globox.search.document.NoteSearchDocument;
import com.unlimited.sports.globox.search.document.UnifiedSearchDocument;
import com.unlimited.sports.globox.search.service.INoteSearchService;
import com.unlimited.sports.globox.search.service.IUnifiedSearchService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 笔记同步MQ消费者
 */
@Slf4j
@Component
@RabbitListener(queues = SearchMQConstants.QUEUE_NOTE_SYNC)
public class NoteSyncConsumer {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private INoteSearchService noteSearchService;

    @Autowired
    private IUnifiedSearchService unifiedSearchService;

    @RabbitHandler
    public void handleNoteSyncMessage(NoteSyncBatchMessage message, Channel channel, Message amqpMessage) throws IOException {
        List<NoteSyncVo> syncVos = message != null ? message.getNotes() : Collections.emptyList();
        try {
                if (syncVos == null || syncVos.isEmpty()) {
                    log.warn("[笔记同步消费] 消息为空");
                    return;
                }

                List<NoteSyncVo> validVos = syncVos.stream()
                        .filter(Objects::nonNull)
                        .filter(vo -> vo.getNoteId() != null)
                        .toList();
                if (validVos.isEmpty()) {
                    log.warn("[笔记同步消费] noteId为空");
                    return;
                }

                List<NoteSyncVo> toSave = new ArrayList<>();
                List<Long> toDelete = new ArrayList<>();
                for (NoteSyncVo vo : validVos) {
                    if (SocialNote.Status.PUBLISHED.equals(vo.getStatus())) {
                        toSave.add(vo);
                    } else {
                        toDelete.add(vo.getNoteId());
                    }
                }

                if (!toSave.isEmpty()) {
                    saveNotesToEs(toSave);
                }
                if (!toDelete.isEmpty()) {
                    deleteNotesFromEs(toDelete);
                }
                log.info("[笔记同步消费] 处理成功 - save={}, delete={}", toSave.size(), toDelete.size());
        } catch (Exception e) {
            log.error("[笔记同步消费] 处理失败 - count={}", syncVos != null ? syncVos.size() : 0, e);
        } finally {
            long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
            channel.basicAck(deliveryTag, false);
        }

    }

    /**
     * 保存笔记到ES
     */
    private void saveNotesToEs(List<NoteSyncVo> syncVos) {
        List<NoteSearchDocument> documents = new ArrayList<>(syncVos.size());
        List<UnifiedSearchDocument> unifiedDocs = new ArrayList<>(syncVos.size());

        for (NoteSyncVo syncVo : syncVos) {
            Double hotScore = noteSearchService.calculateHotScore(
                    syncVo.getLikeCount(),
                    syncVo.getCommentCount(),
                    syncVo.getCollectCount(),
                    syncVo.getCreatedAt()
            );

            NoteSearchDocument document = NoteSearchDocument.builder()
                    .id(SearchDocTypeEnum.buildSearchDocId(SearchDocTypeEnum.NOTE, syncVo.getNoteId()))
                    .noteId(syncVo.getNoteId())
                    .title(syncVo.getTitle())
                    .content(syncVo.getContent())
                    .tags(syncVo.getTags())
                    .userId(syncVo.getUserId())
                    .coverUrl(syncVo.getCoverUrl())
                    .likes(syncVo.getLikeCount() != null ? syncVo.getLikeCount() : 0)
                    .comments(syncVo.getCommentCount() != null ? syncVo.getCommentCount() : 0)
                    .saves(syncVo.getCollectCount() != null ? syncVo.getCollectCount() : 0)
                    .mediaType(syncVo.getMediaType())
                    .hotScore(hotScore)
                    .qualityScore(0.0)
                    .featured(syncVo.getFeatured() != null ? syncVo.getFeatured() : false)
                    .status(syncVo.getStatus() != null ? syncVo.getStatus().ordinal() : 0)
                    .createdAt(syncVo.getCreatedAt())
                    .updatedAt(syncVo.getUpdatedAt())
                    .build();
            documents.add(document);

            UnifiedSearchDocument unifiedDoc = UnifiedSearchDocument.builder()
                    .id(SearchDocTypeEnum.buildSearchDocId(SearchDocTypeEnum.NOTE, syncVo.getNoteId()))
                    .businessId(syncVo.getNoteId())
                    .dataType(SearchDocTypeEnum.NOTE.getValue())
                    .title(syncVo.getTitle())
                    .content(syncVo.getContent())
                    .tags(syncVo.getTags())
                    .location(null)
                    .region(null)
                    .coverUrl(syncVo.getCoverUrl())
                    .score(hotScore != null ? hotScore : 0.0)
                    .createdAt(syncVo.getCreatedAt())
                    .updatedAt(syncVo.getUpdatedAt())
                    .build();
            unifiedDocs.add(unifiedDoc);
        }

        elasticsearchOperations.save(documents);
        unifiedSearchService.saveOrUpdateToUnified(unifiedDocs);

        log.info("[笔记同步消费] 保存到ES成功 - count={}", documents.size());
    }

    /**
     * 从ES删除笔记
     */
    private void deleteNotesFromEs(List<Long> noteIds) {
        List<String> docIds = noteIds.stream()
                .filter(Objects::nonNull)
                .map(id -> SearchDocTypeEnum.buildSearchDocId(SearchDocTypeEnum.NOTE, id))
                .toList();
        if (docIds.isEmpty()) {
            return;
        }

        IdsQueryBuilder idsQuery = new IdsQueryBuilder().addIds(docIds.toArray(new String[0]));
        NativeSearchQuery deleteQuery = new NativeSearchQueryBuilder()
                .withQuery(idsQuery)
                .build();

        elasticsearchOperations.delete(deleteQuery, NoteSearchDocument.class);
        unifiedSearchService.deleteFromUnified(docIds);

        log.info("[笔记同步消费] 从ES删除成功 - count={}", docIds.size());
    }
}
