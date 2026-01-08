package com.unlimited.sports.globox.model.social.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("filter_region")
public class FilterRegion implements Serializable {

    @TableId("filter_region_id")
    private Long filterRegionId;

    @TableField("filter_region_name")
    private String filterRegionName;
}
