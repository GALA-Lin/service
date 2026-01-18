package com.unlimited.sports.globox.merchant.service;

import com.unlimited.sports.globox.merchant.util.MerchantAuthContext;
import com.unlimited.sports.globox.model.venue.dto.CreateActivityDto;
import org.springframework.web.multipart.MultipartFile;

/**
 * 活动管理Service接口
 */
public interface VenueActivityManagementService {

    /**
     * 创建活动（商家端）
     *
     * @param dto 创建活动请求
     * @param context 商家认证上下文（包含角色、商家ID、员工ID等信息）
     * @return 活动ID
     */
    Long createActivity(CreateActivityDto dto, MerchantAuthContext context);
}
