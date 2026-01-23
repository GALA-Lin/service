package com.unlimited.sports.globox.merchant.service;

import com.unlimited.sports.globox.model.merchant.dto.VenueCreateDto;
import com.unlimited.sports.globox.model.merchant.dto.VenueUpdateDto;
import com.unlimited.sports.globox.model.merchant.vo.MerchantVenueBasicInfo;
import com.unlimited.sports.globox.model.merchant.vo.MerchantVenueDetailVo;
import com.unlimited.sports.globox.model.merchant.vo.VenueInfoVo;

/**
 * 场馆管理Service
 * @author Linsen Hu
 * @since 2026-01-15
 */
public interface VenueManagementService {

    /**
     * 创建场馆（包含营业时间）
     *
     * @param merchantId 商家ID
     * @param dto        创建场馆DTO
     * @return 场馆基本信息
     */
    MerchantVenueBasicInfo createVenue(Long merchantId, VenueCreateDto dto);

    /**
     * 更新场馆（包含营业时间）
     *
     * @param merchantId 商家ID
     * @param dto        更新场馆DTO
     * @return 场馆基本信息
     */
    MerchantVenueBasicInfo updateVenue(Long merchantId, VenueUpdateDto dto);

    /**
     * 删除场馆
     *
     * @param merchantId 商家ID
     * @param venueId    场馆ID
     */
    void deleteVenue(Long merchantId, Long venueId);

    /**
     * 切换场馆状态
     *
     * @param merchantId 商家ID
     * @param venueId    场馆ID
     * @param status     状态（1-正常，0-暂停营业）
     * @return 场馆基本信息
     */
    MerchantVenueBasicInfo toggleVenueStatus(Long merchantId, Long venueId, Integer status);

    /**
     * 获取场馆详情（包含营业时间）
     *
     * @param merchantId 商家ID
     * @param venueId    场馆ID
     * @return 场馆详情
     */
    VenueInfoVo getVenueDetail(Long merchantId, Long venueId);
}