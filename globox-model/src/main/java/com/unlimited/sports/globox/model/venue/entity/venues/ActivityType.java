package com.unlimited.sports.globox.model.venue.entity.venues;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 活动类型表
 * 存储所有支持的活动类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("activity_type")
public class ActivityType {

    /**
     * 活动类型ID
     */
    @TableId(type = IdType.AUTO)
    private Long typeId;

    /**
     * 类型编码（唯一）
     * 如：GROUP_PLAY、MATCH、TRAINING等
     */
    private String typeCode;

    /**
     * 类型名称
     * 如：畅打、比赛、培训等
     */
    private String typeName;

    /**
     * 类型描述
     */
    private String description;

    /**
     * 是否启用
     */
    @Builder.Default
    private Boolean isEnabled = true;

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
