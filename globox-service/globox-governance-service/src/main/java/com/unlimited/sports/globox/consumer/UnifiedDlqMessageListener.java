package com.unlimited.sports.globox.consumer;

import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.enums.governance.MQBizTypeEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.GovernanceCode;
import com.unlimited.sports.globox.model.governance.entity.MQDeadLetterLog;
import com.unlimited.sports.globox.service.DeadLetterLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnifiedDlqMessageListener implements ChannelAwareMessageListener {

    private final DeadLetterLogService deadLetterLogService;

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {

        long tag = message.getMessageProperties().getDeliveryTag();
        String queue = message.getMessageProperties().getConsumerQueue();
        String msgId = message.getMessageProperties().getMessageId();

        try {
            // 1) 解析基础信息
            MQDeadLetterLog logRow = buildLog(queue, message);

            log.info("[DLQ] received queue={}, msgId={}, bizType={}, bizKey={}, xdeathCount={}",
                    queue, msgId, logRow.getBizType(), logRow.getBizKey(), logRow.getXdeathCount());

            // 2) 入库（upsert）
            deadLetterLogService.upsert(logRow);

            // 3) 告警（限频）
        } catch (Throwable ex) {
            log.error("[DLQ] handler error, but ACK to avoid backlog. queue={}, msgId={}",
                    queue, message.getMessageProperties().getMessageId(), ex);
        } finally {
            channel.basicAck(tag, false);
        }
    }


    /**
     * 从 DLQ 消息构建 MQDeadLetterLog（第一步）
     */
    private MQDeadLetterLog buildLog(String queue, Message message) {

        MessageProperties mp = message.getMessageProperties();
        Map<String, Object> headers = mp.getHeaders() == null ? Collections.emptyMap() : mp.getHeaders();

        // 1) x-death count（累计死信次数）
        long xDeathCount = extractXDeathCount(headers);

        // 2) 原始 exchange / routingKey（优先用你自定义的 x-orig-*，否则用 x-first-death-*，再否则 fallback）
        String exchange = firstNonBlank(
                asString(headers.get("x-orig-exchange")),
                asString(headers.get("x-first-death-exchange")),
                mp.getReceivedExchange()
        );
        String routingKey = firstNonBlank(
                asString(headers.get("x-orig-routing-key")),
                asString(headers.get("x-first-death-routing-key")),
                mp.getReceivedRoutingKey()
        );

        // 3) payload（body -> Map；解析失败则存 _raw）
        String bodyStr = message.getBody() == null ? "" : new String(message.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> payloadMap = deadLetterLogService.parsePayload(bodyStr);

        // 4) headers
        Map<String, Object> headersSafe = deadLetterLogService.makeHeadersJsonSafe(headers);

        // 5) bizType / bizKey
        MQBizTypeEnum bizType = resolveBizType(headersSafe);
        String bizKey = resolveBizKey(headersSafe, queue, mp, bodyStr);

        // 6) 构建实体
        LocalDateTime now = LocalDateTime.now();

        return MQDeadLetterLog.builder()
                .bizType(bizType)
                .bizKey(bizKey)
                .queueName(queue)
                .exchangeName(exchange)
                .routingKey(routingKey)
                .messageId(mp.getMessageId())
                .correlationId(mp.getCorrelationId())
                .payload(payloadMap)
                .headers(headersSafe)
                .xdeathCount(xDeathCount)
                .firstSeenAt(now)
                .lastSeenAt(now)
                .seenTimes(1)
                .remark(null)
                .build();
    }

    private MQBizTypeEnum resolveBizType(Map<String, Object> headers) throws GloboxApplicationException {
        // 推荐：业务侧投 Final-DLX 时带 x-biz-type
        Object v = headers.get("x-biz-type");
        Integer code = null;
        if (v instanceof Number n) {
            code = n.intValue();
        } else if (v instanceof String s && StringUtils.hasText(s)) {
            try {
                code = Integer.parseInt(s.trim());
            } catch (Exception ignore) {}
        }
        MQBizTypeEnum byHeader = MQBizTypeEnum.of(code);
        if (byHeader != null) {
            return byHeader;
        }

        throw new GloboxApplicationException(GovernanceCode.DEAD_LETTER_BIZ_TYPE_NOT_EXIST);
    }

    private String resolveBizKey(Map<String, Object> headers,
            String queue,
            MessageProperties mp,
            String bodyStr) {

        // 1) 业务侧带 x-biz-key
        String key = asString(headers.get("x-biz-key"));
        if (StringUtils.hasText(key)) {
            return key;
        }

        // 2) 使用 messageId / correlationId
        if (StringUtils.hasText(mp.getMessageId())) {
            return queue + ":" + mp.getMessageId();
        }
        if (StringUtils.hasText(mp.getCorrelationId())) {
            return queue + ":" + mp.getCorrelationId();
        }

        // 3) 最后兜底：body hash（避免无限 insert）
        return queue + ":BODY:" + Integer.toHexString(Objects.hashCode(bodyStr));
    }

    private long extractXDeathCount(Map<String, Object> headers) {
        Object xDeath = headers.get("x-death");
        if (!(xDeath instanceof List<?> list) || list.isEmpty()) return 0L;

        long sum = 0L;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> m)) continue;
            Object count = m.get("count");
            if (count instanceof Long l) sum += l;
            else if (count instanceof Integer i) sum += i.longValue();
        }
        return sum;
    }

    private String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private String firstNonBlank(String... arr) {
        if (arr == null) return null;
        for (String s : arr) {
            if (StringUtils.hasText(s)) return s;
        }
        return null;
    }
}