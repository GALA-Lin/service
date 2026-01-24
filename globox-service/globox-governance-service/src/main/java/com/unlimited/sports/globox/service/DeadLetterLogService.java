package com.unlimited.sports.globox.service;

import com.unlimited.sports.globox.model.governance.entity.MQDeadLetterLog;
import org.springframework.amqp.core.Message;

import java.util.Map;

/**
 * 死信队列日志 Service
 */
public interface DeadLetterLogService {
    /**
     * DLQ 入库聚合：按 bizType + bizKey upsert（seenTimes++/lastSeenAt 更新）
     */
    void upsert(MQDeadLetterLog logRow);

    /**
     * body string -> payload map（解析失败存 _raw）
     */
    Map<String, Object> parsePayload(String bodyStr);

    /**
     * headers -> json safe map（把不可序列化对象转换掉）
     */
    Map<String, Object> makeHeadersJsonSafe(Map<String, Object> headers);
}
