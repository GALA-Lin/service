package com.unlimited.sports.globox.model.merchant.vo;

import lombok.*;

import java.time.LocalDateTime;

/**
 * @since 2025-12-18-22:41
 * 场地情况视图
 */
@Data
@Builder
public class CourtVo {

    /**
     * 场地ID
     */
    @NonNull
    private Long courtId;

    /**
     * 场馆ID
     */
    @NonNull
    private Long venueId;

    /**
     * 场地名称
     */
    private String name;

    /**
     * 场地地面类型
     */
    @NonNull
    private Integer groundType;

    /**
     * 场地地面类型名称
     */
    private String groundTypeName;

    /**
     * 场地类型
     */
    @NonNull
    private Integer courtType;

    /**
     * 场地类型名称
     */
    private String courtTypeName;

    /**
     * 状态
     */

    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}