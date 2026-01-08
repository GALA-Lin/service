package com.unlimited.sports.globox.model.venue.entity.venues;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 价格模板实体
 */
@Data
@TableName("venue_price_template")
public class VenuePriceTemplate {

    /**
     * 价格模板ID
     */
    @TableId(value = "template_id", type = IdType.AUTO)
    private Long templateId;

    /**
     * 商家ID
     */
    @TableField("merchant_id")
    private Long merchantId;

    /**
     * 模板名称
     */
    @TableField("template_name")
    private String templateName;

    /**
     * 是否为默认模板
     */
    @TableField("is_default")
    private Boolean isDefault;

    /**
     * 是否启用
     */
    @TableField("is_enabled")
    private Boolean isEnabled;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
