package com.unlimited.sports.globox.search.consumer;

import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.constants.SearchMQConstants;
import com.unlimited.sports.globox.model.auth.vo.UserSyncBatchMessage;
import com.unlimited.sports.globox.model.auth.vo.UserSyncVo;
import com.unlimited.sports.globox.model.search.enums.SearchDocTypeEnum;
import com.unlimited.sports.globox.search.document.UnifiedSearchDocument;
import com.unlimited.sports.globox.search.document.UserSearchDocument;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户同步MQ消费者
 */
@Slf4j
@Component
@RabbitListener(queues = SearchMQConstants.QUEUE_USER_SYNC)
public class UserSyncConsumer {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private IUnifiedSearchService unifiedSearchService;

    @RabbitHandler
    public void handleUserSyncMessage(UserSyncBatchMessage message, Channel channel, Message amqpMessage) throws IOException {
        List<UserSyncVo> syncVos = message != null ? message.getUsers() : Collections.emptyList();
        try {
            if (syncVos == null || syncVos.isEmpty()) {
                log.warn("[用户同步消费] 消息为空");
                return;
            }
            List<UserSyncVo> validVos = syncVos.stream()
                    .filter(Objects::nonNull)
                    .filter(vo -> vo.getUserId() != null)
                    .toList();
            if (validVos.isEmpty()) {
                log.warn("[用户同步消费] userId为空");
                return;
            }
            Map<Boolean, List<UserSyncVo>> partitionedUser = validVos.stream()
                    .collect(Collectors.partitioningBy(
                            vo -> Boolean.TRUE.equals(vo.getCancelled())
                    ));
            List<UserSyncVo> toSave = partitionedUser.get(false);
            List<Long> toDelete = partitionedUser.get(true).stream()
                    .map(UserSyncVo::getUserId)
                    .collect(Collectors.toList());
            if (!toSave.isEmpty()) {
                saveUsersToEs(toSave);
            }
            if (!toDelete.isEmpty()) {
                deleteUsersFromEs(toDelete);
            }
            log.info("[用户同步消费] 处理成功 - save={}, delete={}", toSave.size(), toDelete.size());
        } catch (Exception e) {
            log.error("[用户同步消费] 处理失败 - count={}", syncVos != null ? syncVos.size() : 0, e);
        } finally {
            long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
            channel.basicAck(deliveryTag, false);
        }
    }

    /**
     * 保存用户到ES
     */
    private void saveUsersToEs(List<UserSyncVo> syncVos) {
        List<UserSearchDocument> documents = new ArrayList<>(syncVos.size());
        List<UnifiedSearchDocument> unifiedDocs = new ArrayList<>(syncVos.size());
        for (UserSyncVo syncVo : syncVos) {
            UserSearchDocument document = UserSearchDocument.builder()
                    .id(SearchDocTypeEnum.buildSearchDocId(SearchDocTypeEnum.USER, syncVo.getUserId()))
                    .userId(syncVo.getUserId())
                    .nickName(syncVo.getNickName())
                    .globoxNo(syncVo.getGloboxNo())
                    .avatarUrl(syncVo.getAvatarUrl())
                    .gender(syncVo.getGender())
                    .ntrp(syncVo.getNtrp())
                    .build();
            documents.add(document);
            UnifiedSearchDocument unifiedDoc = UnifiedSearchDocument.builder()
                    .id(SearchDocTypeEnum.buildSearchDocId(SearchDocTypeEnum.USER, syncVo.getUserId()))
                    .businessId(syncVo.getUserId())
                    .dataType(SearchDocTypeEnum.USER.getValue())
                    .title(syncVo.getNickName())
                    .content(null)
                    .tags(null)
                    .location(null)
                    .region(null)
                    .coverUrl(syncVo.getAvatarUrl())
                    .score(0.0)
                    .createdAt(null)
                    .updatedAt(null)
                    .build();
            unifiedDocs.add(unifiedDoc);
        }
        elasticsearchOperations.save(documents);
        unifiedSearchService.saveOrUpdateToUnified(unifiedDocs);
        log.info("[用户同步消费] 保存到ES成功 - count={}", documents.size());
    }

    /**
     * 从ES删除用户
     */
    private void deleteUsersFromEs(List<Long> userIds) {
        List<String> docIds = userIds.stream()
                .filter(Objects::nonNull)
                .map(id -> SearchDocTypeEnum.buildSearchDocId(SearchDocTypeEnum.USER, id))
                .toList();
        if (docIds.isEmpty()) {
            return;
        }
        IdsQueryBuilder idsQuery = new IdsQueryBuilder().addIds(docIds.toArray(new String[0]));
        NativeSearchQuery deleteQuery = new NativeSearchQueryBuilder()
                .withQuery(idsQuery)
                .build();
        elasticsearchOperations.delete(deleteQuery, UserSearchDocument.class);
        unifiedSearchService.deleteFromUnified(docIds);
        log.info("[用户同步消费] 从ES删除成功 - count={}", docIds.size());
    }
}
