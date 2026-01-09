package com.unlimited.sports.globox.merchant.service;

import com.unlimited.sports.globox.model.merchant.dto.CourtCreateDto;
import com.unlimited.sports.globox.model.merchant.dto.CourtUpdateDto;
import com.unlimited.sports.globox.model.merchant.vo.CourtVo;
import com.unlimited.sports.globox.model.merchant.vo.MerchantVenueBasicInfo;
import com.unlimited.sports.globox.model.merchant.vo.MerchantVenueDetailVo;

import java.util.List;

/**
 * @author Linsen Hu
 * @since 2025/12/22 14:06
 * 场地管理
 */

public interface CourtManagementService {

    /**
     * 创建场地
     */
    CourtVo createCourt(Long merchantId, CourtCreateDto createDTO);

    /**
     * 更新场地
     */
    CourtVo updateCourt(Long merchantId, CourtUpdateDto updateDTO);

    /**
     * 删除场地
     */
    void deleteCourt(Long merchantId, Long courtId);

    /**
     * 查询场馆的所有场地
     */
    List<CourtVo> listCourtsByVenue(Long merchantId, Long venueId);

    /**
     * 启用/禁用场地
     */
    CourtVo toggleCourtStatus(Long merchantId, Long courtId, Integer status);

    List<MerchantVenueBasicInfo> getVenuesByMerchantId(Long merchantId);

    List<MerchantVenueDetailVo> getVenuesWithCourts(Long merchantId);
}
