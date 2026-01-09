package com.unlimited.sports.globox.model.merchant.vo;

import lombok.*;

import java.util.List;

/**
 * @since 2026/1/9
 * 商家场馆详情视图（包含场地列表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantVenueDetailVo {

    /**
     * 场馆ID
     */
    private Long venueId;

    /**
     * 场馆官方名称
     */
    private String venueName;

    /**
     * 场馆详细地址
     */
    private String address;

    /**
     * 所属区域或行政区
     */
    private String region;

    /**
     * 场馆图片URL列表
     */
    private List<String> imageUrls;

    /**
     * 场馆状态：1-正常，0-暂停营业
     */
    private Integer status;

    /**
     * 场馆状态描述
     */
    private String statusDesc;

    /**
     * 场馆下的场地列表
     */
    private List<CourtVo> courts;
}