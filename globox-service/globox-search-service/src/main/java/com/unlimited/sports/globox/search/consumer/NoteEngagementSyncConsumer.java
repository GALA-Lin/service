package com.unlimited.sports.globox.search.consumer;

import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.constants.SearchMQConstants;
import com.unlimited.sports.globox.model.search.dto.NoteEngagementSyncMessage;
import com.unlimited.sports.globox.model.search.dto.NoteEngagementSyncMessage.NoteEngagementItem;
import com.unlimited.sports.globox.model.search.enums.SearchDocTypeEnum;
import com.unlimited.sports.globox.search.document.NoteSearchDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 笔记互动数据增量同步MQ消费者
 * 接收社交服务发送的增量数据，通过 ES script 增量更新 likes/comments 并重算 hotScore
 */
@Slf4j
@Component
@RabbitListener(queues = SearchMQConstants.QUEUE_NOTE_ENGAGEMENT_SYNC)
public class NoteEngagementSyncConsumer {

    /**
     * ES Painless 脚本：增量更新 likes/comments，防止负数
     */
    private static final String DELTA_SCRIPT =
            "ctx._source.likes = Math.max(0, (ctx._source.likes ?: 0) + params.likeDelta);" +
            "ctx._source.comments = Math.max(0, (ctx._source.comments ?: 0) + params.commentDelta);";

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @RabbitHandler
    public void handleEngagementSyncMessage(NoteEngagementSyncMessage message, Channel channel, Message amqpMessage) throws IOException {
        try {
            if (message == null || message.getItems() == null || message.getItems().isEmpty()) {
                log.warn("[互动同步消费] 消息为空");
                return;
            }

            List<NoteEngagementItem> items = message.getItems();
            List<UpdateQuery> updateQueries = new ArrayList<>(items.size());

            for (NoteEngagementItem item : items) {
                if (item.getNoteId() == null) {
                    continue;
                }

                String docId = SearchDocTypeEnum.buildSearchDocId(SearchDocTypeEnum.NOTE, item.getNoteId());

                Map<String, Object> params = new HashMap<>();
                params.put("likeDelta", item.getLikeDelta() != null ? item.getLikeDelta() : 0);
                params.put("commentDelta", item.getCommentDelta() != null ? item.getCommentDelta() : 0);

                updateQueries.add(UpdateQuery.builder(docId)
                        .withScript(DELTA_SCRIPT)
                        .withParams(params)
                        .build());
            }

            if (!updateQueries.isEmpty()) {
                IndexCoordinates indexCoordinates = elasticsearchOperations
                        .getIndexCoordinatesFor(NoteSearchDocument.class);
                elasticsearchOperations.bulkUpdate(updateQueries, indexCoordinates);
                log.info("[互动同步消费] 增量更新ES成功: count={}", updateQueries.size());
            }

        } catch (Exception e) {
            log.error("[互动同步消费] 处理失败: count={}",
                    message != null && message.getItems() != null ? message.getItems().size() : 0, e);
        } finally {
            long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
            channel.basicAck(deliveryTag, false);
        }
    }
}
