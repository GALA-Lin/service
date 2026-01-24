package com.unlimited.sports.globox.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.enums.governance.MQDeadLetterLogHandleStatusEnum;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.mapper.MqDeadLetterLogMapper;
import com.unlimited.sports.globox.model.governance.entity.MQDeadLetterLog;
import com.unlimited.sports.globox.service.DeadLetterLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 死信队列日志 Service
 */
@Slf4j
@Service
public class DeadLetterLogServiceImpl implements DeadLetterLogService {

    @Autowired
    private MqDeadLetterLogMapper deadLetterLogMapper;

    @Autowired
    private JsonUtils jsonUtils;


    @Override
    public void upsert(MQDeadLetterLog logRow) {

        if (logRow.getBizType() == null || !StringUtils.hasText(logRow.getBizKey())) {
            // bizType/bizKey 为空会导致聚合失效，至少保证 bizKey 不空
            // 你也可以在这里强制兜底赋值
            log.warn("[DLQ] bizType/bizKey invalid, bizType={}, bizKey={}", logRow.getBizType(), logRow.getBizKey());
        }

        // 查是否存在（按 bizType+bizKey 聚合）
        MQDeadLetterLog existed = deadLetterLogMapper.selectOne(
                new LambdaQueryWrapper<MQDeadLetterLog>()
                        .eq(MQDeadLetterLog::getBizType, logRow.getBizType())
                        .eq(MQDeadLetterLog::getBizKey, logRow.getBizKey()));

        LocalDateTime now = LocalDateTime.now();

        if (existed == null) {
            logRow.setStatus(MQDeadLetterLogHandleStatusEnum.NEW);
            logRow.setSeenTimes(1);
            logRow.setFirstSeenAt(Optional.ofNullable(logRow.getFirstSeenAt()).orElse(now));
            logRow.setLastSeenAt(Optional.ofNullable(logRow.getLastSeenAt()).orElse(now));
            deadLetterLogMapper.insert(logRow);
            return;
        }

        // 更新聚合字段
        existed.setLastSeenAt(now);
        existed.setSeenTimes((existed.getSeenTimes() == null ? 0 : existed.getSeenTimes()) + 1);

        // xdeath_count 取更大值（避免回退）
        long oldX = existed.getXdeathCount() == null ? 0L : existed.getXdeathCount();
        long newX = logRow.getXdeathCount() == null ? 0L : logRow.getXdeathCount();
        existed.setXdeathCount(Math.max(oldX, newX));

        // 保留最新 payload/headers 便于排查
        existed.setPayload(logRow.getPayload());
        existed.setHeaders(logRow.getHeaders());

        // 如果之前拿不到 exchange/routing，这里补上
        if (StringUtils.hasText(logRow.getExchangeName())) {
            existed.setExchangeName(logRow.getExchangeName());
        }
        if (StringUtils.hasText(logRow.getRoutingKey())) {
            existed.setRoutingKey(logRow.getRoutingKey());
        }
        if (StringUtils.hasText(logRow.getQueueName())) {
            existed.setQueueName(logRow.getQueueName());
        }

        // status：如果已经 RESOLVED/IGNORED，一般不自动改回 NEW（看你策略）
        // 这里默认：如果是 NEW/REPLAYED/FAILED 才保持原状；不动 RESOLVED/IGNORED
        deadLetterLogMapper.updateById(existed);
    }

    @Override
    public Map<String, Object> parsePayload(String bodyStr) {
        if (!StringUtils.hasText(bodyStr)) return new HashMap<>();
        try {
            // 你需要在 JsonUtils 里提供 json->Map 的方法；没有的话我下面给替代写法
            return jsonUtils.jsonToMap(bodyStr);
        } catch (Exception e) {
            Map<String, Object> m = new HashMap<>();
            m.put("_raw", bodyStr);
            return m;
        }
    }

    @Override
    public Map<String, Object> makeHeadersJsonSafe(Map<String, Object> headers) {
        Map<String, Object> out = new HashMap<>();
        if (headers == null) return out;

        for (Map.Entry<String, Object> e : headers.entrySet()) {
            out.put(e.getKey(), makeJsonSafe(e.getValue()));
        }
        return out;
    }

    private Object makeJsonSafe(Object v) {
        if (v == null) return null;
        if (v instanceof String || v instanceof Number || v instanceof Boolean) {
            return v;
        }
        if (v instanceof Date d) {
            return d.getTime();
        }
        if (v instanceof byte[] b) {
            return Base64.getEncoder().encodeToString(b);
        }

        if (v instanceof List<?> list) {
            List<Object> newList = new ArrayList<>(list.size());
            for (Object o : list) newList.add(makeJsonSafe(o));
            return newList;
        }
        if (v instanceof Map<?, ?> map) {
            Map<String, Object> newMap = new HashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                newMap.put(String.valueOf(e.getKey()), makeJsonSafe(e.getValue()));
            }
            return newMap;
        }

        return String.valueOf(v);
    }
}
