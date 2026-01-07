package com.unlimited.sports.globox.model.venue.entity.venues;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 第三方平台表
 * 存储所有支持的第三方场馆管理平台信息
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("third_party_platform")
public class ThirdPartyPlatform implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 平台ID，主键
     */
    @TableId(value = "platform_id", type = IdType.AUTO)
    private Long platformId;

    /**
     * 平台代码（changxiaoer/aitennis等，对应枚举code）
     */
    @TableField("platform_code")
    private String platformCode;

    /**
     * 平台名称（场小二/爱网球等）
     */
    @TableField("platform_name")
    private String platformName;

    /**
     * 平台描述
     */
    @TableField("platform_desc")
    private String platformDesc;

    /**
     * 平台基础API地址
     */
    @TableField("base_api_url")
    private String baseApiUrl;

    /**
     * 平台状态：1=启用 0=禁用
     */
    @TableField("status")
    private Integer status;

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
