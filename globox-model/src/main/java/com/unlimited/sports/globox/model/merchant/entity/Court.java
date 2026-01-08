package com.unlimited.sports.globox.model.merchant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @since 2025-12-18-10:41
 * 场地信息表
 */

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("courts")
public class Court implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 场地ID，主键
     */
    @TableId(value = "court_id", type = IdType.AUTO)
    private Long courtId;

    /**
     * 所属场馆ID
     */
    @TableField("venue_id")
    private Long venueId;

    /**
     * 场地名称
     */
    @TableField("name")
    private String name;

    /**
     * 场地地面类型：1=硬地，2=红土，3=草地，4=其他
     */
    @TableField("ground_type")
    private Integer groundType;

    /**
     * 场地类型：1=室内，2=室外，3=风雨场，4=半封闭
     */
    @TableField("court_type")
    private Integer courtType;

    /**
     * 状态：0-不开放，1-开放
     */
    @TableField("status")
    private Integer status;

    /**
     * 该场地在第三方平台的ID
     */
    @TableField("third_party_court_id")
    private String thirdPartyCourtId;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
