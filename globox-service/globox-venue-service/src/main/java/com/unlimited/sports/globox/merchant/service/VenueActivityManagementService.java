package com.unlimited.sports.globox.merchant.service;

import com.unlimited.sports.globox.merchant.util.MerchantAuthContext;
import com.unlimited.sports.globox.model.merchant.vo.ActivityCreationResultVo;
import com.unlimited.sports.globox.model.venue.dto.CreateActivityDto;
import com.unlimited.sports.globox.model.venue.dto.UpdateActivityDto;

import java.time.LocalDate;
import java.util.List;

/**
 * 活动管理Service接口
 */
public interface VenueActivityManagementService {

    /**
     * 创建活动（商家端）
     *
     * @param dto 创建活动请求
     * @param context 商家认证上下文（包含角色、商家ID、员工ID等信息）
     * @return 活动详情
     */
    ActivityCreationResultVo createActivity(CreateActivityDto dto, MerchantAuthContext context);

    /**
     *
     * @param activityId
     * @param context
     * @param cancelReason
     */
    void cancelActivity(Long activityId, MerchantAuthContext context, String cancelReason);

    /**
     * 查询商家所有活动（包含正常和取消）
     */
    List<ActivityCreationResultVo> getMerchantActivities(MerchantAuthContext context);

    /**
     * 根据场馆ID查询活动
     */
    List<ActivityCreationResultVo> getActivitiesByVenueId(Long venueId, LocalDate activityDate);

    // 在 VenueActivityManagementService.java 中添加

    /**
     * 更新活动（商家端）
     *
     * @param activityId 活动ID
     * @param dto        更新活动请求
     * @param context    商家认证上下文
     * @return 更新后的活动详情
     */
    ActivityCreationResultVo updateActivity(Long activityId, UpdateActivityDto dto, MerchantAuthContext context);
}
