package com.unlimited.sports.globox.model.merchant.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @since 2026/1/9 18:33
 * 商家场馆基本信息视图
 */
@Data
@Builder
public class MerchantVenueBasicInfo {

    /**
     * 场馆ID
     */
    private Long venueId;

    /**
     * 场馆官方名称
     */
    private String name;

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
}