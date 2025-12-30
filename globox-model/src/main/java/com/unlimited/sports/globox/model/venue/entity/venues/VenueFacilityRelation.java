package com.unlimited.sports.globox.model.venue.entity.venues;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("venue_facility_relation")
public class VenueFacilityRelation {
    @TableId(type = IdType.AUTO)
    private Long facilityRelationId;

    /**
     * 绑定的场馆id
     */
    private Long venueId;

    private Long facilityId; // todo 后续如果是字典表,需要绑定id,现在暂时为空,2026-02-28

    /**
     * 设置名称
     */
    private String facilityName;


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
