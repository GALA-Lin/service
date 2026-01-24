package com.unlimited.sports.globox.model.governance.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.unlimited.sports.globox.common.enums.governance.MQBizTypeEnum;
import com.unlimited.sports.globox.common.enums.governance.MQDeadLetterLogHandleStatusEnum;
import com.unlimited.sports.globox.model.base.BaseEntity;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "mq_dead_letter_log", autoResultMap = true)
public class MQDeadLetterLog extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务类型（TINYINT）
     */
    private MQBizTypeEnum bizType;

    /**
     * 业务key，如 orderNo/activityId/recordIds hash
     */
    private String bizKey;

    private String queueName;
    private String exchangeName;
    private String routingKey;

    private String messageId;
    private String correlationId;

    /**
     * JSON payload
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> payload;

    /**
     * JSON headers
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> headers;

    private Long xdeathCount;

    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;

    private Integer seenTimes;

    /**
     * 1=NEW/2=REPLAYED/3=IGNORED/4=RESOLVED/5=FAILED
     */
    private MQDeadLetterLogHandleStatusEnum status;

    private String remark;
}