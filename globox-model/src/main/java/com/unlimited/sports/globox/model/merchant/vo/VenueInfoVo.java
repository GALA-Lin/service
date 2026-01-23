package com.unlimited.sports.globox.model.merchant.vo;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/**
 * 商家场馆详情视图（包含营业时间和场地列表）
 * @since 2026-01-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueInfoVo {

    /**
     * 场馆ID
     */
    private Long venueId;

    /**
     * 场馆名称
     */
    private String venueName;

    /**
     * 场馆地址
     */
    private String address;

    /**
     * 所属区域
     */
    private String region;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 场馆描述
     */
    private String description;

    /**
     * 提前几天开放订场
     */
    private Integer maxAdvanceDays;

    /**
     * 槽位可见时间
     */
    private LocalTime slotVisibilityTime;

    /**
     * 场馆图片URL列表
     */
    private List<String> imageUrls;

    /**
     * 设施标签列表
     */
    private List<String> facilities;

    /**
     * 场馆状态：1-正常，0-暂停营业
     */
    private Integer status;

    /**
     * 场馆状态描述
     */
    private String statusDesc;

    /**
     * 营业时间列表
     */
    private List<VenueBusinessHoursVo> businessHours;

    /**
     * 场馆下的场地列表
     */
    private List<CourtVo> courts;
}