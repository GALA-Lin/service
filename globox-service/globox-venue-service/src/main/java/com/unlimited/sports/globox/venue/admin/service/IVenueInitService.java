package com.unlimited.sports.globox.venue.admin.service;

import com.unlimited.sports.globox.venue.admin.dto.CreateVenueInitDto;
import com.unlimited.sports.globox.venue.admin.vo.VenueInitResultVo;

/**
 * 场馆初始化服务接口
 */
public interface IVenueInitService {

    /**
     * 一键创建场馆及所有相关配置
     *
     * @param merchantId 商家ID
     * @param dto 场馆配置信息（包含图片URL列表）
     * @return 创建结果
     */
    VenueInitResultVo createVenue(Long merchantId, CreateVenueInitDto dto);
}
