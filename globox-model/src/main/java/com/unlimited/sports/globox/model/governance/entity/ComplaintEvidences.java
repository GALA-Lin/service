package com.unlimited.sports.globox.model.governance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.unlimited.sports.globox.model.base.BaseEntity;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("complaint_evidences")
@EqualsAndHashCode(callSuper = true)
public class ComplaintEvidences extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 举报单ID
     */
    private Long complaintId;

    /**
     * 序号（1-9）
     */
    private Integer seq;

    /**
     * 存放 cos url
     */
    private String url;
}