package com.unlimited.sports.globox.coach.controller;

import com.unlimited.sports.globox.coach.service.ICoachSettingsService;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.coach.dto.*;
import com.unlimited.sports.globox.model.coach.vo.CoachSettingsVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static com.unlimited.sports.globox.common.constants.RequestHeaderConstants.HEADER_USER_ID;

/**
 * @since 2026/1/12
 * 教练设置管理接口
 */
@Slf4j
@RestController
@RequestMapping("/coach/settings")
public class CoachSettingsController {

    @Autowired
    private ICoachSettingsService coachSettingsService;

    /**
     * 获取教练设置信息
     *
     * @param coachUserId 教练用户ID（从请求头获取）
     * @return 教练设置信息
     */
    @GetMapping
    public R<CoachSettingsVo> getCoachSettings(
            @RequestHeader(HEADER_USER_ID) Long coachUserId) {
        log.info("获取教练设置信息 - coachUserId: {}", coachUserId);
        CoachSettingsVo settings = coachSettingsService.getCoachSettings(coachUserId);
        return R.ok(settings);
    }

    /**
     * 更新教练位置信息（单独接口）
     * 用于教练打开前端教练中心时自动获取并更新位置
     *
     * @param coachUserId 教练用户ID（从请求头获取）
     * @param dto 位置信息
     * @return 更新结果
     */
    @PatchMapping("/location")
    public R<Void> updateCoachLocation(
            @RequestHeader(HEADER_USER_ID) Long coachUserId,
            @Valid @RequestBody UpdateCoachLocationDto dto) {
        log.info("更新教练位置 - coachUserId: {}, lat: {}, lng: {}",
                coachUserId, dto.getLatitude(), dto.getLongitude());
        dto.setCoachUserId(coachUserId);
        coachSettingsService.updateCoachLocation(dto);
        return R.ok();
    }

    /**
     * 更新教练基本设置（不含位置）
     *
     * @param coachUserId 教练用户ID
     * @param dto 基本设置信息
     * @return 更新结果
     */
    @PutMapping("/basic")
    public R<Void> updateBasicSettings(
            @RequestHeader(HEADER_USER_ID) Long coachUserId,
            @Valid @RequestBody UpdateCoachBasicSettingsDto dto) {
        log.info("更新教练基本设置 - coachUserId: {}", coachUserId);
        dto.setCoachUserId(coachUserId);
        coachSettingsService.updateBasicSettings(dto);
        return R.ok();
    }

    /**
     * 更新教练服务区域设置
     *
     * @param coachUserId 教练用户ID
     * @param dto 服务区域设置
     * @return 更新结果
     */
    @PutMapping("/service-area")
    public R<Void> updateServiceArea(
            @RequestHeader(HEADER_USER_ID) Long coachUserId,
            @Valid @RequestBody UpdateCoachServiceAreaDto dto) {
        log.info("更新教练服务区域 - coachUserId: {}", coachUserId);
        dto.setCoachUserId(coachUserId);
        coachSettingsService.updateServiceArea(dto);
        return R.ok();
    }

    /**
     * 更新教练状态（接单状态）
     *
     * @param coachUserId 教练用户ID（从请求头获取）
     * @param dto 状态设置
     * @return 更新结果
     */
    @PatchMapping("/status")
    public R<Void> updateCoachStatus(
            @RequestHeader(HEADER_USER_ID) Long coachUserId,
            @Valid @RequestBody UpdateCoachStatusDto dto) {
        log.info("更新教练状态 - coachUserId: {}, status: {}",
                coachUserId, dto.getCoachStatus());
        dto.setCoachUserId(coachUserId);
        coachSettingsService.updateCoachStatus(dto);
        return R.ok();
    }

    /**
     * 更新教练展示信息（照片、视频、标签等）
     *
     * @param coachUserId 教练用户ID（从请求头获取）
     * @param dto 展示信息
     * @return 更新结果
     */
    @PutMapping("/display")
    public R<Void> updateDisplaySettings(
            @RequestHeader(HEADER_USER_ID) Long coachUserId,
            @Valid @RequestBody UpdateCoachDisplaySettingsDto dto) {
        log.info("更新教练展示信息 - coachUserId: {}", coachUserId);
        dto.setCoachUserId(coachUserId);
        coachSettingsService.updateDisplaySettings(dto);
        return R.ok();
    }

    /**
     * 更新教练场地偏好设置
     *
     * @param coachUserId 教练用户ID（从请求头获取）
     * @param dto 场地偏好设置
     * @return 更新结果
     */
    @PutMapping("/venue-preference")
    public R<Void> updateVenuePreference(
            @RequestHeader(HEADER_USER_ID) Long coachUserId,
            @Valid @RequestBody UpdateCoachVenuePreferenceDto dto) {
        log.info("更新教练场地偏好 - coachUserId: {}", coachUserId);
        dto.setCoachUserId(coachUserId);
        coachSettingsService.updateVenuePreference(dto);
        return R.ok();
    }
}