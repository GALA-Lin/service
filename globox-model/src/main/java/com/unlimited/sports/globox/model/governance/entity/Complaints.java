package com.unlimited.sports.globox.model.governance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.unlimited.sports.globox.common.enums.ClientType;
import com.unlimited.sports.globox.common.enums.governance.ComplaintReasonTypeEnum;
import com.unlimited.sports.globox.common.enums.governance.ComplaintStatusEnum;
import com.unlimited.sports.globox.common.enums.governance.ComplaintTargetTypeEnum;
import com.unlimited.sports.globox.model.base.BaseEntity;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("complaints")
@EqualsAndHashCode(callSuper = true)
public class Complaints extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 举报人
     */
    private Long userId;

    /**
     * 端类型（APP/小程序/商家端等）
     */
    private ClientType clientType;

    /**
     * 举报目标类型
     */
    private ComplaintTargetTypeEnum targetType;

    /**
     * 目标主键ID（不同业务表的ID）
     */
    private Long targetId;

    /**
     * 被举报内容作者/所属用户ID（冗余，便于查询）
     */
    private Long targetUserId;

    /**
     * 举报原因
     */
    private ComplaintReasonTypeEnum reason;

    /**
     * 举报补充描述
     */
    private String description;

    /**
     * 状态
     */
    private ComplaintStatusEnum status;

    /**
     * 处理完成时间
     */
    private LocalDateTime handledAt;
}