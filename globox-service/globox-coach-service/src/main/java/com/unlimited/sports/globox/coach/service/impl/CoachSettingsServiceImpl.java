package com.unlimited.sports.globox.coach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.unlimited.sports.globox.coach.mapper.CoachExtraInfoMapper;
import com.unlimited.sports.globox.coach.mapper.CoachProfileMapper;
import com.unlimited.sports.globox.coach.service.ICoachSettingsService;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.model.coach.dto.*;
import com.unlimited.sports.globox.model.coach.entity.CoachExtraInfo;
import com.unlimited.sports.globox.model.coach.entity.CoachProfile;
import com.unlimited.sports.globox.model.coach.enums.CoachAcceptVenueTypeEnum;
import com.unlimited.sports.globox.model.coach.enums.CoachStatusEnum;
import com.unlimited.sports.globox.model.coach.vo.CoachSettingsVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * @since 2026/1/12
 * 教练设置服务实现（优化版）
 *
 * 优化说明：
 * 1. 移除了 parseAreaString 方法，直接使用 List<String>
 * 2. 简化了服务区域的处理逻辑
 * 3. MyBatis-Plus 的 JacksonTypeHandler 自动处理 JSON 序列化/反序列化
 */
@Slf4j
@Service
public class CoachSettingsServiceImpl implements ICoachSettingsService {

    @Autowired
    private CoachProfileMapper coachProfileMapper;

    @Autowired
    private CoachExtraInfoMapper coachExtraInfoMapper;

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

        // === 新增：查询真名显示设置 ===
        CoachExtraInfo extraInfo = coachExtraInfoMapper.selectById(coachUserId);
        Boolean displayRealName = extraInfo != null && extraInfo.getDisplayRealName() != null
                ? extraInfo.getDisplayRealName()
                : false;

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
                        .serviceArea(profile.getCoachServiceArea() != null ?
                                profile.getCoachServiceArea() : null)
                        .remoteServiceAreaList(profile.getCoachRemoteServiceArea() != null ?
                                profile.getCoachRemoteServiceArea() : Collections.emptyList())
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
                // === 新增：真名显示信息 ===
                .realNameInfo(CoachSettingsVo.RealNameInfo.builder()
                        .coachRealName(profile.getCoachRealName())
                        .displayRealName(displayRealName)
                        .build())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CoachProfile updateCoachLocation(UpdateCoachLocationDto dto) {
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
        return profile;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CoachProfile updateBasicSettings(UpdateCoachBasicSettingsDto dto) {
        log.info("更新教练基本设置 - coachUserId: {}", dto.getCoachUserId());

        verifyCoachExists(dto.getCoachUserId());

        // ========== 核心修改：和 updateServiceArea 保持一致，先构造实体对象 ==========
        CoachProfile updateEntity = new CoachProfile();

        // 保留原有非空判断，仅当 DTO 字段不为 null 时，给实体对象赋值（避免覆盖原有有效数据）
        if (dto.getCoachTeachingStyle() != null) {
            updateEntity.setCoachTeachingStyle(dto.getCoachTeachingStyle());
        }
        if (dto.getCoachSpecialtyTags() != null) {
            updateEntity.setCoachSpecialtyTags(dto.getCoachSpecialtyTags());
        }
        if (dto.getCoachAward() != null) {
            updateEntity.setCoachAward(dto.getCoachAward());
        }
        if (dto.getCoachTeachingYears() != null) {
            updateEntity.setCoachTeachingYears(dto.getCoachTeachingYears());
        }
        if (dto.getCoachCertificationFiles() != null) {
            updateEntity.setCoachCertificationFiles(dto.getCoachCertificationFiles());
        }
        if (dto.getCoachWorkPhotos() != null) {
            updateEntity.setCoachWorkPhotos(dto.getCoachWorkPhotos());
        }
        if (dto.getCoachWorkVideos() != null) {
            updateEntity.setCoachWorkVideos(dto.getCoachWorkVideos());
        }

        int updated = coachProfileMapper.update(
                updateEntity, // 第一个参数：要更新的字段（已赋值的实体对象）
                new LambdaUpdateWrapper<CoachProfile>()
                        .eq(CoachProfile::getCoachUserId, dto.getCoachUserId()) // 第二个参数：更新条件
        );

        if (updated == 0) {
            throw new GloboxApplicationException("更新教练基本设置失败");
        }

        log.info("教练基本设置更新成功 - coachUserId: {}", dto.getCoachUserId());

        return updateEntity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CoachProfile updateServiceArea(UpdateCoachServiceAreaDto dto) {
        log.info("更新教练服务区域 - coachUserId: {}", dto.getCoachUserId());

        verifyCoachExists(dto.getCoachUserId());

        CoachProfile updateEntity = new CoachProfile();
        updateEntity.setCoachServiceArea(dto.getCoachServiceArea());
        updateEntity.setCoachMinHours(dto.getCoachMinHours());
        updateEntity.setCoachRemoteServiceArea(dto.getCoachRemoteServiceArea());
        updateEntity.setCoachRemoteMinHours(dto.getCoachRemoteMinHours());

        int updated = coachProfileMapper.update(updateEntity,
                new LambdaUpdateWrapper<CoachProfile>()
                        .eq(CoachProfile::getCoachUserId, dto.getCoachUserId())
        );

        if (updated == 0) {
            throw new GloboxApplicationException("更新教练服务区域失败");
        }
        return updateEntity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CoachProfile updateCoachStatus(UpdateCoachStatusDto dto) {
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
        return null;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public CoachProfile updateDisplaySettings(UpdateCoachDisplaySettingsDto dto) {
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
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CoachProfile updateVenuePreference(UpdateCoachVenuePreferenceDto dto) {
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
        return null;
    }

    /**
     * 更新教练真名显示设置
     *
     * @param dto 真名设置
     */
    /**
     * 更新教练真名显示设置
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRealNameSettings(UpdateCoachRealNameSettingsDto dto) {
        log.info("更新教练真名显示设置 - coachUserId: {}, displayRealName: {}",
                dto.getCoachUserId(), dto.getDisplayRealName());

        // 1. 验证教练是否存在
        verifyCoachExists(dto.getCoachUserId());

        // 2. 更新 coach_profile 表的真实姓名（如果提供了）
        if (dto.getCoachRealName() != null && !dto.getCoachRealName().trim().isEmpty()) {
            int profileUpdated = coachProfileMapper.update(null,
                    new LambdaUpdateWrapper<CoachProfile>()
                            .eq(CoachProfile::getCoachUserId, dto.getCoachUserId())
                            .set(CoachProfile::getCoachRealName, dto.getCoachRealName().trim())
            );

            if (profileUpdated == 0) {
                throw new GloboxApplicationException("更新教练真实姓名失败");
            }
            log.info("教练真实姓名更新成功 - coachUserId: {}, realName: {}",
                    dto.getCoachUserId(), dto.getCoachRealName());
        }

        // 3. 更新或创建 coach_extra_info 表的显示设置
        CoachExtraInfo existingInfo = coachExtraInfoMapper.selectById(dto.getCoachUserId());

        if (existingInfo != null) {
            // 更新现有记录
            existingInfo.setDisplayRealName(dto.getDisplayRealName());
            int updated = coachExtraInfoMapper.updateById(existingInfo);

            if (updated == 0) {
                throw new GloboxApplicationException("更新真名显示设置失败");
            }
            log.info("真名显示设置更新成功 - coachUserId: {}", dto.getCoachUserId());
        } else {
            // 创建新记录
            CoachExtraInfo newInfo = new CoachExtraInfo();
            newInfo.setCoachUserId(dto.getCoachUserId());
            newInfo.setDisplayRealName(dto.getDisplayRealName());

            int inserted = coachExtraInfoMapper.insert(newInfo);
            if (inserted == 0) {
                throw new GloboxApplicationException("创建真名显示设置失败");
            }
            log.info("真名显示设置创建成功 - coachUserId: {}", dto.getCoachUserId());
        }
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
}