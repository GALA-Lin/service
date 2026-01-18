package com.unlimited.sports.globox.coach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.unlimited.sports.globox.coach.mapper.CoachProfileMapper;
import com.unlimited.sports.globox.coach.service.ICoachSettingsService;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.model.coach.dto.*;
import com.unlimited.sports.globox.model.coach.entity.CoachProfile;
import com.unlimited.sports.globox.model.coach.enums.CoachAcceptVenueTypeEnum;
import com.unlimited.sports.globox.model.coach.enums.CoachStatusEnum;
import com.unlimited.sports.globox.model.coach.vo.CoachSettingsVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 2026/1/12
 * 教练设置服务实现
 */
@Slf4j
@Service
public class CoachSettingsServiceImpl implements ICoachSettingsService {

    @Autowired
    private CoachProfileMapper coachProfileMapper;

    @Override
    public CoachSettingsVo getCoachSettings(Long coachUserId) {
        log.info("获取教练设置信息 - coachUserId: {}", coachUserId);

        CoachProfile profile = coachProfileMapper.selectOne(
                new LambdaQueryWrapper<CoachProfile>()
                        .eq(CoachProfile::getCoachUserId, coachUserId)
        );

        if (profile == null) {
            throw new GloboxApplicationException("教练信息不存在");
        }

        // 获取状态描述
        String statusDesc = "";
        try {
            CoachStatusEnum statusEnum = CoachStatusEnum.fromValue(profile.getCoachStatus());
            statusDesc = statusEnum.getDescription();
        } catch (Exception e) {
            log.warn("未知的教练状态: {}", profile.getCoachStatus());
        }

        // 获取场地类型描述
        String venueTypeDesc = "";
        try {
            if (profile.getCoachAcceptVenueType() != null) {
                CoachAcceptVenueTypeEnum venueEnum =
                        CoachAcceptVenueTypeEnum.fromValue(profile.getCoachAcceptVenueType());
                venueTypeDesc = venueEnum.getDescription();
            }
        } catch (Exception e) {
            log.warn("未知的场地类型: {}", profile.getCoachAcceptVenueType());
        }

        // 解析服务区域列表
        List<String> serviceAreaList = parseAreaString(profile.getCoachServiceArea());
        List<String> remoteServiceAreaList = parseAreaString(profile.getCoachRemoteServiceArea());

        // 构建返回结果
        return CoachSettingsVo.builder()
                .coachUserId(coachUserId)
                .coachStatus(profile.getCoachStatus())
                .coachStatusDesc(statusDesc)
                .locationInfo(CoachSettingsVo.LocationInfo.builder()
                        .latitude(profile.getCoachLatitude())
                        .longitude(profile.getCoachLongitude())
                        .build())
                .serviceAreaInfo(CoachSettingsVo.ServiceAreaInfo.builder()
                        .coachServiceArea(profile.getCoachServiceArea())
                        .serviceAreaList(serviceAreaList)
                        .coachRemoteServiceArea(profile.getCoachRemoteServiceArea())
                        .remoteServiceAreaList(remoteServiceAreaList)
                        .coachRemoteMinHours(profile.getCoachRemoteMinHours())
                        .build())
                .basicSettingsInfo(CoachSettingsVo.BasicSettingsInfo.builder()
                        .coachTeachingStyle(profile.getCoachTeachingStyle())
                        .coachSpecialtyTags(profile.getCoachSpecialtyTags() != null ?
                                profile.getCoachSpecialtyTags() : Collections.emptyList())
                        .coachAward(profile.getCoachAward() != null ?
                                profile.getCoachAward() : Collections.emptyList())
                        .coachTeachingYears(profile.getCoachTeachingYears())
                        .build())
                .displaySettingsInfo(CoachSettingsVo.DisplaySettingsInfo.builder()
                        .coachWorkPhotos(profile.getCoachWorkPhotos() != null ?
                                profile.getCoachWorkPhotos() : Collections.emptyList())
                        .coachWorkVideos(profile.getCoachWorkVideos() != null ?
                                profile.getCoachWorkVideos() : Collections.emptyList())
                        .coachCertificationFiles(profile.getCoachCertificationFiles() != null ?
                                profile.getCoachCertificationFiles() : Collections.emptyList())
                        .coachCertificationLevel(profile.getCoachCertificationLevel() != null ?
                                profile.getCoachCertificationLevel() : Collections.emptyList())
                        .build())
                .venuePreferenceInfo(CoachSettingsVo.VenuePreferenceInfo.builder()
                        .coachAcceptVenueType(profile.getCoachAcceptVenueType())
                        .coachAcceptVenueTypeDesc(venueTypeDesc)
                        .build())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCoachLocation(UpdateCoachLocationDto dto) {
        log.info("更新教练位置 - coachUserId: {}, lat: {}, lng: {}",
                dto.getCoachUserId(), dto.getLatitude(), dto.getLongitude());

        // 验证教练是否存在
        CoachProfile profile = coachProfileMapper.selectOne(
                new LambdaQueryWrapper<CoachProfile>()
                        .eq(CoachProfile::getCoachUserId, dto.getCoachUserId())
        );

        if (profile == null) {
            throw new GloboxApplicationException("教练信息不存在");
        }

        // 更新位置
        int updated = coachProfileMapper.update(null,
                new LambdaUpdateWrapper<CoachProfile>()
                        .eq(CoachProfile::getCoachUserId, dto.getCoachUserId())
                        .set(CoachProfile::getCoachLatitude, dto.getLatitude())
                        .set(CoachProfile::getCoachLongitude, dto.getLongitude())
        );

        if (updated == 0) {
            throw new GloboxApplicationException("更新教练位置失败");
        }

        log.info("教练位置更新成功 - coachUserId: {}", dto.getCoachUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBasicSettings(UpdateCoachBasicSettingsDto dto) {
        log.info("更新教练基本设置 - coachUserId: {}", dto.getCoachUserId());

        verifyCoachExists(dto.getCoachUserId());

        LambdaUpdateWrapper<CoachProfile> updateWrapper =
                new LambdaUpdateWrapper<CoachProfile>()
                        .eq(CoachProfile::getCoachUserId, dto.getCoachUserId());

        if (dto.getCoachTeachingStyle() != null) {
            updateWrapper.set(CoachProfile::getCoachTeachingStyle, dto.getCoachTeachingStyle());
        }
        if (dto.getCoachSpecialtyTags() != null) {
            updateWrapper.set(CoachProfile::getCoachSpecialtyTags, dto.getCoachSpecialtyTags());
        }
        if (dto.getCoachAward() != null) {
            updateWrapper.set(CoachProfile::getCoachAward, dto.getCoachAward());
        }
        if (dto.getCoachTeachingYears() != null) {
            updateWrapper.set(CoachProfile::getCoachTeachingYears, dto.getCoachTeachingYears());
        }

        int updated = coachProfileMapper.update(null, updateWrapper);
        if (updated == 0) {
            throw new GloboxApplicationException("更新教练基本设置失败");
        }

        log.info("教练基本设置更新成功 - coachUserId: {}", dto.getCoachUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateServiceArea(UpdateCoachServiceAreaDto dto) {
        log.info("更新教练服务区域 - coachUserId: {}", dto.getCoachUserId());

        verifyCoachExists(dto.getCoachUserId());

        LambdaUpdateWrapper<CoachProfile> updateWrapper =
                new LambdaUpdateWrapper<CoachProfile>()
                        .eq(CoachProfile::getCoachUserId, dto.getCoachUserId())
                        .set(CoachProfile::getCoachServiceArea, dto.getCoachServiceArea());

        if (dto.getCoachRemoteServiceArea() != null) {
            updateWrapper.set(CoachProfile::getCoachRemoteServiceArea,
                    dto.getCoachRemoteServiceArea());
        }
        if (dto.getCoachRemoteMinHours() != null) {
            updateWrapper.set(CoachProfile::getCoachRemoteMinHours,
                    dto.getCoachRemoteMinHours());
        }

        int updated = coachProfileMapper.update(null, updateWrapper);
        if (updated == 0) {
            throw new GloboxApplicationException("更新教练服务区域失败");
        }

        log.info("教练服务区域更新成功 - coachUserId: {}", dto.getCoachUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCoachStatus(UpdateCoachStatusDto dto) {
        log.info("更新教练状态 - coachUserId: {}, status: {}",
                dto.getCoachUserId(), dto.getCoachStatus());

        verifyCoachExists(dto.getCoachUserId());

        // 验证状态值是否合法
        try {
            CoachStatusEnum.fromValue(dto.getCoachStatus());
        } catch (Exception e) {
            throw new GloboxApplicationException("无效的教练状态值: " + dto.getCoachStatus());
        }

        int updated = coachProfileMapper.update(null,
                new LambdaUpdateWrapper<CoachProfile>()
                        .eq(CoachProfile::getCoachUserId, dto.getCoachUserId())
                        .set(CoachProfile::getCoachStatus, dto.getCoachStatus())
        );

        if (updated == 0) {
            throw new GloboxApplicationException("更新教练状态失败");
        }

        log.info("教练状态更新成功 - coachUserId: {}", dto.getCoachUserId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateDisplaySettings(UpdateCoachDisplaySettingsDto dto) {
        log.info("更新教练展示信息 - coachUserId: {}", dto.getCoachUserId());

        verifyCoachExists(dto.getCoachUserId());

        LambdaUpdateWrapper<CoachProfile> updateWrapper =
                new LambdaUpdateWrapper<CoachProfile>()
                        .eq(CoachProfile::getCoachUserId, dto.getCoachUserId());

        if (dto.getCoachWorkPhotos() != null) {
            updateWrapper.set(CoachProfile::getCoachWorkPhotos, dto.getCoachWorkPhotos());
        }
        if (dto.getCoachWorkVideos() != null) {
            updateWrapper.set(CoachProfile::getCoachWorkVideos, dto.getCoachWorkVideos());
        }
        if (dto.getCoachCertificationFiles() != null) {
            updateWrapper.set(CoachProfile::getCoachCertificationFiles,
                    dto.getCoachCertificationFiles());
        }
        if (dto.getCoachCertificationLevel() != null) {
            updateWrapper.set(CoachProfile::getCoachCertificationLevel,
                    dto.getCoachCertificationLevel());
        }

        int updated = coachProfileMapper.update(null, updateWrapper);
        if (updated == 0) {
            throw new GloboxApplicationException("更新教练展示信息失败");
        }

        log.info("教练展示信息更新成功 - coachUserId: {}", dto.getCoachUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVenuePreference(UpdateCoachVenuePreferenceDto dto) {
        log.info("更新教练场地偏好 - coachUserId: {}, venueType: {}",
                dto.getCoachUserId(), dto.getCoachAcceptVenueType());

        verifyCoachExists(dto.getCoachUserId());

        // 验证场地类型是否合法
        try {
            CoachAcceptVenueTypeEnum.fromValue(dto.getCoachAcceptVenueType());
        } catch (Exception e) {
            throw new GloboxApplicationException("无效的场地类型值: " + dto.getCoachAcceptVenueType());
        }

        int updated = coachProfileMapper.update(null,
                new LambdaUpdateWrapper<CoachProfile>()
                        .eq(CoachProfile::getCoachUserId, dto.getCoachUserId())
                        .set(CoachProfile::getCoachAcceptVenueType, dto.getCoachAcceptVenueType())
        );

        if (updated == 0) {
            throw new GloboxApplicationException("更新教练场地偏好失败");
        }

        log.info("教练场地偏好更新成功 - coachUserId: {}", dto.getCoachUserId());
    }

    // ========== 私有辅助方法 ==========

    /**
     * 验证教练是否存在
     */
    private void verifyCoachExists(Long coachUserId) {
        Long count = coachProfileMapper.selectCount(
                new LambdaQueryWrapper<CoachProfile>()
                        .eq(CoachProfile::getCoachUserId, coachUserId)
        );

        if (count == null || count == 0) {
            throw new GloboxApplicationException("教练信息不存在");
        }
    }

    /**
     * 解析区域字符串为列表
     */
    private List<String> parseAreaString(String areaString) {
        if (areaString == null || areaString.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(areaString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}