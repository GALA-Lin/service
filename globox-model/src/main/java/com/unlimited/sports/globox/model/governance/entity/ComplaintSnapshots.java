package com.unlimited.sports.globox.model.governance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.unlimited.sports.globox.common.enums.governance.ComplaintTargetTypeEnum;
import com.unlimited.sports.globox.model.base.BaseEntity;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("complaint_snapshots")
@EqualsAndHashCode(callSuper = true)
public class ComplaintSnapshots extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 举报单ID
     */
    private Long complaintId;

    /**
     * 举报目标类型
     */
    private ComplaintTargetTypeEnum targetType;

    /**
     * 目标主键ID（冗余，便于查询）
     */
    private Long targetId;

    /**
     * 被举报内容作者/所属用户ID（冗余）
     */
    private Long targetUserId;

    /**
     * 文本内容快照（可直接展示）
     */
    private String contentText;

    /**
     * 扩展内容快照
     */
    private String contentJson;
}