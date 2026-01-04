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

    /**
     * 设施ID（对应FacilityType的value）
     * 1=停车场, 2=更衣室, 3=穿线机
     */
    private Integer facilityId;

    /**
     * 设施名称（冗余字段，便于查询）
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
