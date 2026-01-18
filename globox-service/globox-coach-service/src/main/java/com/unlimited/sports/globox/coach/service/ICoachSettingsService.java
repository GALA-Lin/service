package com.unlimited.sports.globox.coach.service;

import com.unlimited.sports.globox.model.coach.dto.*;
import com.unlimited.sports.globox.model.coach.vo.CoachSettingsVo;
import org.springframework.transaction.annotation.Transactional;

/**
 * @since 2026/1/12 17:00
 *
 */
public interface ICoachSettingsService {

    /**
     * 获取教练设置信息
     *
     * @param coachUserId 教练用户ID
     * @return 教练设置信息
     */
    CoachSettingsVo getCoachSettings(Long coachUserId);

    /**
     * 更新教练位置信息
     *
     * @param dto 位置信息
     */
    void updateCoachLocation(UpdateCoachLocationDto dto);

    /**
     * 更新教练基本设置
     *
     * @param dto 基本设置
     */
    void updateBasicSettings(UpdateCoachBasicSettingsDto dto);

    /**
     * 更新教练服务区域
     *
     * @param dto 服务区域设置
     */
    void updateServiceArea(UpdateCoachServiceAreaDto dto);

    /**
     * 更新教练状态
     *
     * @param dto 状态设置
     */
    void updateCoachStatus(UpdateCoachStatusDto dto);

    /**
     * 更新教练展示信息
     *
     * @param dto 展示信息
     */
    void updateDisplaySettings(UpdateCoachDisplaySettingsDto dto);

    /**
     * 更新教练场地偏好
     *
     * @param dto 场地偏好设置
     */
    void updateVenuePreference(UpdateCoachVenuePreferenceDto dto);
}