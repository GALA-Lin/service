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
 * 场馆第三方平台配置表
 * 存储每个away场馆在第三方平台的具体配置信息（认证、API地址等）
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("venue_third_party_config")
public class VenueThirdPartyConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 配置ID，主键
     */
    @TableId(value = "config_id", type = IdType.AUTO)
    private Long configId;

    /**
     * 场馆ID
     */
    @TableField("venue_id")
    private Long venueId;

    /**
     * 第三方平台ID（关联third_party_platform表）
     */
    @TableField("third_party_platform_id")
    private Long thirdPartyPlatformId;

    /**
     * 该场馆在第三方平台的ID
     * 场小二：spaceId
     * aitennis：stadiumId
     */
    @TableField("third_party_venue_id")
    private String thirdPartyVenueId;

    /**
     * 该场馆专用的API地址（为空则使用平台默认地址）
     */
    @TableField("api_url")
    private String apiUrl;

    /**
     * 登录账号或Token字段1
     * 场小二：存储Token
     * aitennis：可为空
     */
    @TableField("username")
    private String username;

    /**
     * 登录密码或Token字段2（加密存储）
     * 场小二：可为空
     * aitennis：存储JWT Token
     */
    @TableField("password")
    private String password;

    /**
     * 其他额外配置信息（JSON格式）
     * 场小二：{"adminId": "3133", "channel": "Web", "version": "29"}
     * aitennis：{"client": "Web"}
     */
    @TableField("extra_config")
    private String extraConfig;

    /**
     * 配置状态：1=启用 0=禁用
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
