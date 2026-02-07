package com.unlimited.sports.globox.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.common.enums.FileTypeEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.result.UserAuthCode;
import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.enums.ClientType;
import com.unlimited.sports.globox.common.utils.RequestContextHolder;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.dubbo.social.SocialRelationDubboService;
import com.unlimited.sports.globox.dubbo.social.dto.UserRelationStatusDto;
import com.unlimited.sports.globox.dubbo.user.RegionDubboService;
import com.unlimited.sports.globox.dubbo.user.dto.RegionDto;
import com.unlimited.sports.globox.model.auth.dto.SetGloboxNoRequest;
import com.unlimited.sports.globox.model.auth.dto.UpdateStarCardPortraitRequest;
import com.unlimited.sports.globox.model.auth.dto.UpdateUserProfileRequest;
import com.unlimited.sports.globox.model.auth.dto.UserRacketRequest;
import com.unlimited.sports.globox.model.auth.entity.AuthUser;
import com.unlimited.sports.globox.model.auth.entity.RacketDict;
import com.unlimited.sports.globox.model.auth.entity.StyleTag;
import com.unlimited.sports.globox.model.auth.entity.UserProfile;
import com.unlimited.sports.globox.model.auth.entity.UserRacket;
import com.unlimited.sports.globox.model.auth.entity.UserStyleTag;
import com.unlimited.sports.globox.model.auth.enums.GenderEnum;
import com.unlimited.sports.globox.model.auth.vo.SetGloboxNoResultVo;
import com.unlimited.sports.globox.model.auth.vo.StarCardPortraitVo;
import com.unlimited.sports.globox.model.auth.vo.StarCardVo;
import com.unlimited.sports.globox.model.auth.vo.ProfileOptionsVo;
import com.unlimited.sports.globox.model.auth.vo.RacketDictNodeVo;
import com.unlimited.sports.globox.model.auth.vo.StyleTagVo;
import com.unlimited.sports.globox.model.auth.vo.UserProfileVo;
import com.unlimited.sports.globox.model.auth.vo.UserRacketVo;
import com.unlimited.sports.globox.model.auth.vo.UserSearchItemVo;
import com.unlimited.sports.globox.model.auth.vo.UserSearchResultVo;
import com.unlimited.sports.globox.model.venue.vo.FileUploadVo;
import com.unlimited.sports.globox.user.mapper.RacketDictMapper;
import com.unlimited.sports.globox.user.mapper.StyleTagMapper;
import com.unlimited.sports.globox.user.mapper.AuthUserMapper;
import com.unlimited.sports.globox.user.mapper.UserProfileMapper;
import com.unlimited.sports.globox.user.mapper.UserRacketMapper;
import com.unlimited.sports.globox.user.mapper.UserStyleTagMapper;
import com.unlimited.sports.globox.user.prop.UserProfileDefaultProperties;
import com.unlimited.sports.globox.user.service.FileUploadService;
import com.unlimited.sports.globox.user.service.PortraitMattingService;
import com.unlimited.sports.globox.user.service.UserProfileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.Year;

/**
 * 用户资料服务实现类
 *
 * @author Wreckloud
 * @since 2025/12/20
 */
@Service
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {

    private static final int MAX_BATCH_SIZE = 50;
    private static final int SPORTS_YEAR_THRESHOLD = 1900;

    @Autowired
    private UserProfileDefaultProperties userProfileDefaultProperties;;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private AuthUserMapper authUserMapper;

    @Autowired
    private UserRacketMapper userRacketMapper;

    @Autowired
    private UserStyleTagMapper userStyleTagMapper;

    @Autowired
    private RacketDictMapper racketDictMapper;

    @Autowired
    private StyleTagMapper styleTagMapper;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private PortraitMattingService portraitMattingService;

    @DubboReference(group = "rpc")
    private SocialRelationDubboService socialRelationDubboService;

    @DubboReference(group = "rpc")
    private RegionDubboService regionDubboService;

    @Value("${user.globox-no.cooldown-seconds:5184000}")
    private long globoxNoCooldownSeconds;

    @Override
    public UserProfile getUserProfileById(Long userId) {
        if (userId == null) {
            return null;
        }

        // 根据主键userId查询
        LambdaQueryWrapper<UserProfile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserProfile::getUserId, userId);
        return userProfileMapper.selectOne(queryWrapper);
    }

    @Override
    public List<UserProfile> batchGetUserProfile(List<Long> userIds) {
        // 1. 空值检查
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyList();
        }

        // 2. 去重（复用去重后的列表）
        List<Long> distinctIds = userIds.stream()
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        if (distinctIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 数量限制检查（基于去重后的数量）
        if (distinctIds.size() > MAX_BATCH_SIZE) {
            throw new GloboxApplicationException(UserAuthCode.BATCH_QUERY_TOO_LARGE);
        }

        // 4. 批量查询（使用去重后的列表）
        LambdaQueryWrapper<UserProfile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(UserProfile::getUserId, distinctIds);
        List<UserProfile> profiles = userProfileMapper.selectList(queryWrapper);

        log.debug("批量查询用户资料：请求数量={}, 去重后={}, 返回数量={}", 
                userIds.size(), distinctIds.size(), profiles.size());

        return profiles;
    }

    @Override
    public R<UserProfileVo> getUserProfile(Long userId) {
        if (userId == null) {
            return R.error(UserAuthCode.USER_NOT_EXIST);
        }

        // 1. 查询用户基础资料
        UserProfile profile = getUserProfileById(userId);
        if (profile == null) {
            return R.error(UserAuthCode.USER_NOT_EXIST);
        }

        // 2. 查询用户球拍列表
        LambdaQueryWrapper<UserRacket> racketQuery = new LambdaQueryWrapper<>();
        racketQuery.eq(UserRacket::getUserId, userId)
                .eq(UserRacket::getDeleted, false);
        List<UserRacket> userRackets = userRacketMapper.selectList(racketQuery);

        List<UserRacketVo> racketVos = new ArrayList<>();
        if (!CollectionUtils.isEmpty(userRackets)) {
            // 批量查询球拍型号信息
            List<Long> modelIds = userRackets.stream()
                    .map(UserRacket::getRacketModelId)
                    .distinct()
                    .collect(Collectors.toList());

            LambdaQueryWrapper<RacketDict> dictQuery = new LambdaQueryWrapper<>();
            dictQuery.in(RacketDict::getRacketId, modelIds)
                    .eq(RacketDict::getLevel, RacketDict.Level.MODEL);
            List<RacketDict> racketDicts = racketDictMapper.selectList(dictQuery);

            Map<Long, RacketDict> modelMap = racketDicts.stream()
                    .collect(Collectors.toMap(RacketDict::getRacketId, r -> r));

            // 查系列、品牌，构建全名
            List<Long> seriesIds = racketDicts.stream()
                    .map(RacketDict::getParentId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, RacketDict> seriesMap = Collections.emptyMap();
            Map<Long, RacketDict> brandMap = Collections.emptyMap();
            if (!CollectionUtils.isEmpty(seriesIds)) {
                List<RacketDict> seriesList = racketDictMapper.selectBatchIds(seriesIds);
                seriesMap = seriesList.stream().collect(Collectors.toMap(RacketDict::getRacketId, r -> r));
                List<Long> brandIds = seriesList.stream()
                        .map(RacketDict::getParentId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(brandIds)) {
                    List<RacketDict> brandList = racketDictMapper.selectBatchIds(brandIds);
                    brandMap = brandList.stream().collect(Collectors.toMap(RacketDict::getRacketId, r -> r));
                }
            }

            Map<Long, RacketDict> finalSeriesMap = seriesMap;
            Map<Long, RacketDict> finalBrandMap = brandMap;
            racketVos = userRackets.stream()
                    .map(ur -> {
                        UserRacketVo vo = new UserRacketVo();
                        vo.setRacketModelId(ur.getRacketModelId());
                        RacketDict model = modelMap.get(ur.getRacketModelId());
                        if (model != null) {
                            // 短称：品牌 + 型号（不含系列）
                            vo.setRacketModelName(buildShortRacketName(model, finalSeriesMap, finalBrandMap));
                            // 全称：品牌 + 系列 + 型号
                            vo.setRacketModelFullName(buildFullRacketName(model, finalSeriesMap, finalBrandMap));
                        }
                        vo.setIsPrimary(ur.getIsPrimary());
                        return vo;
                    })
                    .collect(Collectors.toList());
        }

        // 3. 查询用户标签列表
        LambdaQueryWrapper<UserStyleTag> tagQuery = new LambdaQueryWrapper<>();
        tagQuery.eq(UserStyleTag::getUserId, userId)
                .eq(UserStyleTag::getDeleted, false);
        List<UserStyleTag> userStyleTags = userStyleTagMapper.selectList(tagQuery);

        List<StyleTagVo> styleTagVos = new ArrayList<>();
        if (!CollectionUtils.isEmpty(userStyleTags)) {
            List<Long> tagIds = userStyleTags.stream()
                    .map(UserStyleTag::getTagId)
                    .distinct()
                    .collect(Collectors.toList());

            LambdaQueryWrapper<StyleTag> styleTagQuery = new LambdaQueryWrapper<>();
            styleTagQuery.in(StyleTag::getTagId, tagIds);
            List<StyleTag> styleTags = styleTagMapper.selectList(styleTagQuery);

            styleTagVos = styleTags.stream()
                    .map(st -> {
                        StyleTagVo vo = new StyleTagVo();
                        vo.setTagId(st.getTagId());
                        vo.setName(st.getName());
                        return vo;
                    })
                    .collect(Collectors.toList());
        }

        // 4. 组装VO
        UserProfileVo vo = new UserProfileVo();
        BeanUtils.copyProperties(profile, vo);
        vo.setGender(profile.getGender() != null ? profile.getGender().name() : null);
        vo.setPreferredHand(profile.getPreferredHand() != null ? profile.getPreferredHand().name() : null);
        vo.setSportsYears(resolveSportsYears(profile.getSportsStartYear()));
        vo.setRackets(racketVos);
        vo.setStyleTags(styleTagVos);
        vo.setIsFollowed(false);
        vo.setIsMutual(false);
        vo.setHomeDistrictName(getRegionNameByCode(profile.getHomeDistrict()));
        if (!StringUtils.hasText(vo.getAvatarUrl())) {
            String clientType = RequestContextHolder.getHeader(RequestHeaderConstants.HEADER_CLIENT_TYPE);
            String defaultAvatarUrl = resolveDefaultAvatarUrl(clientType);
            if (StringUtils.hasText(defaultAvatarUrl)) {
                vo.setAvatarUrl(defaultAvatarUrl);
            }
        }

        // 设置默认球星卡
        if (ObjectUtils.isEmpty(vo.getPortraitUrl()) && userProfileDefaultProperties.getEnableDefaultStarCard()) {
            if (GenderEnum.FEMALE.equals(profile.getGender())) {
                vo.setPortraitUrl(userProfileDefaultProperties.getDefaultStarCardFemaleUrl());
            } else {
                vo.setPortraitUrl(userProfileDefaultProperties.getDefaultStarCardMaleUrl());
            }
        }
        AuthUser authUser = authUserMapper.selectById(userId);
        if (authUser != null && authUser.getRole() != null) {
            vo.setRole(authUser.getRole().name());
        }

        return R.ok(vo);
    }

    @Override
    public R<UserProfileVo> getUserProfile(Long userId, Long viewerId) {
        R<UserProfileVo> result = getUserProfile(userId);
        if (result == null || !result.success()) {
            return result;
        }
        UserProfileVo vo = result.getData();
        if (vo == null || viewerId == null || userId == null || viewerId.equals(userId)) {
            return result;
        }
        RpcResult<UserRelationStatusDto> relationResult = socialRelationDubboService.getRelationStatus(viewerId, userId);
        if (relationResult != null && relationResult.isSuccess() && relationResult.getData() != null) {
            UserRelationStatusDto relationStatus = relationResult.getData();
            vo.setIsFollowed(relationStatus.getIsFollowed());
            vo.setIsMutual(relationStatus.getIsMutual());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> updateUserProfile(Long userId, UpdateUserProfileRequest request) {
        if (userId == null) {
            return R.error(UserAuthCode.USER_NOT_EXIST);
        }
        if (request == null) {
            throw new GloboxApplicationException(UserAuthCode.INVALID_PARAM);
        }
        boolean hasAnyField = StringUtils.hasText(request.getAvatarUrl())
                || StringUtils.hasText(request.getPortraitUrl())
                || StringUtils.hasText(request.getNickName())
                || StringUtils.hasText(request.getSignature())
                || StringUtils.hasText(request.getGender())
                || request.getSportsYears() != null
                || request.getNtrp() != null
                || StringUtils.hasText(request.getPreferredHand())
                || StringUtils.hasText(request.getHomeDistrict())
                || request.getPower() != null
                || request.getSpeed() != null
                || request.getServe() != null
                || request.getVolley() != null
                || request.getStamina() != null
                || request.getMental() != null
                || request.getRackets() != null
                || request.getTagIds() != null;
        if (!hasAnyField) {
            throw new GloboxApplicationException(UserAuthCode.INVALID_PARAM);
        }

        // 1. 查询用户资料
        UserProfile profile = getUserProfileById(userId);
        if (profile == null) {
            return R.error(UserAuthCode.USER_NOT_EXIST);
        }

        // 2. 先校验球拍列表（如果提供）
        List<UserRacketRequest> distinctRackets = null;
        if (request.getRackets() != null) {
            // 2.1 过滤无效条目并去重（按 racketModelId 去重）
            Map<Long, UserRacketRequest> distinctRacketMap = new HashMap<>();
            int invalidCount = 0;
            for (UserRacketRequest r : request.getRackets()) {
                if (r.getRacketModelId() == null) {
                    invalidCount++;
                    continue; // 忽略无效条目，不阻止其他字段更新
                }
                distinctRacketMap.put(r.getRacketModelId(), r);
            }
            if (invalidCount > 0) {
                log.warn("用户资料更新：球拍列表中有 {} 个无效条目被忽略：userId={}", invalidCount, userId);
            }
            distinctRackets = new ArrayList<>(distinctRacketMap.values());

            // 2.2 空列表=清空：先删除，然后跳过校验/插入
            if (CollectionUtils.isEmpty(distinctRackets)) {
                LambdaUpdateWrapper<UserRacket> cancelQuery = new LambdaUpdateWrapper<>();
                cancelQuery.eq(UserRacket::getUserId, userId)
                        .set(UserRacket::getDeleted, true);
                userRacketMapper.update(null, cancelQuery);
            } else {
                // 2.3 校验球拍型号是否存在且为MODEL级别（过滤无效的，而不是全部失败）
                List<Long> modelIds = distinctRackets.stream()
                        .map(UserRacketRequest::getRacketModelId)
                        .collect(Collectors.toList());

                LambdaQueryWrapper<RacketDict> dictQuery = new LambdaQueryWrapper<>();
                dictQuery.in(RacketDict::getRacketId, modelIds);
                List<RacketDict> racketDicts = racketDictMapper.selectList(dictQuery);

                // 构建有效的球拍ID集合（存在、MODEL级别、ACTIVE状态）
                Map<Long, RacketDict> validRacketMap = racketDicts.stream()
                        .filter(dict -> dict.getLevel() == RacketDict.Level.MODEL)
                        .filter(dict -> dict.getStatus() == RacketDict.RacketStatus.ACTIVE)
                        .collect(Collectors.toMap(RacketDict::getRacketId, dict -> dict));

                // 过滤出有效的球拍条目
                List<UserRacketRequest> validRackets = distinctRackets.stream()
                        .filter(r -> validRacketMap.containsKey(r.getRacketModelId()))
                        .collect(Collectors.toList());

                int filteredCount = distinctRackets.size() - validRackets.size();
                if (filteredCount > 0) {
                    log.warn("用户资料更新：球拍列表中有 {} 个无效球拍被过滤：userId={}", filteredCount, userId);
                }

                distinctRackets = validRackets;

                // 2.4 主力拍唯一性校验（如果有多个，只保留第一个）
                if (!CollectionUtils.isEmpty(distinctRackets)) {
                    long primaryCount = distinctRackets.stream()
                            .filter(r -> Boolean.TRUE.equals(r.getIsPrimary()))
                            .count();
                    if (primaryCount > 1) {
                        log.warn("用户资料更新：多个主力拍，只保留第一个：userId={}", userId);
                        boolean[] foundPrimary = {false};
                        distinctRackets.forEach(r -> {
                            if (Boolean.TRUE.equals(r.getIsPrimary())) {
                                if (foundPrimary[0]) {
                                    r.setIsPrimary(false); // 后续的主力拍改为非主力
                                } else {
                                    foundPrimary[0] = true;
                                }
                            }
                        });
                    }
                }
            }
        }

        // 3. 先校验标签列表（如果提供）
        List<Long> distinctTagIds = null;
        if (request.getTagIds() != null) {
            // 3.1 过滤 null 并去重
            long nullCount = request.getTagIds().stream().filter(id -> id == null).count();
            if (nullCount > 0) {
                log.warn("用户资料更新：标签列表中有 {} 个 null 被忽略：userId={}", nullCount, userId);
            }
            distinctTagIds = request.getTagIds().stream()
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toList());

            // 3.2 空列表=清空：先删除，然后跳过校验/插入
            if (CollectionUtils.isEmpty(distinctTagIds)) {
                LambdaUpdateWrapper<UserStyleTag> cancelTagQuery = new LambdaUpdateWrapper<>();
                cancelTagQuery.eq(UserStyleTag::getUserId, userId)
                        .set(UserStyleTag::getDeleted, true);
                userStyleTagMapper.update(null, cancelTagQuery);
            } else {
                // 3.3 校验标签是否存在且为ACTIVE状态（过滤无效的，而不是全部失败）
                LambdaQueryWrapper<StyleTag> tagQuery = new LambdaQueryWrapper<>();
                tagQuery.in(StyleTag::getTagId, distinctTagIds)
                        .eq(StyleTag::getStatus, StyleTag.Status.ACTIVE);
                List<StyleTag> validTags = styleTagMapper.selectList(tagQuery);

                // 只保留有效的标签ID
                List<Long> validTagIds = validTags.stream()
                        .map(StyleTag::getTagId)
                        .collect(Collectors.toList());

                int filteredCount = distinctTagIds.size() - validTagIds.size();
                if (filteredCount > 0) {
                    log.warn("用户资料更新：标签列表中有 {} 个无效标签被过滤：userId={}", filteredCount, userId);
                }

                distinctTagIds = validTagIds;
            }
        }

        // 4. 再更新基础资料（Patch更新，仅更新非空字段）
        boolean needUpdate = false;
        if (StringUtils.hasText(request.getAvatarUrl())) {
            profile.setAvatarUrl(request.getAvatarUrl());
            needUpdate = true;
        }
        if (StringUtils.hasText(request.getPortraitUrl())) {
            profile.setPortraitUrl(request.getPortraitUrl());
            needUpdate = true;
        }
        if (StringUtils.hasText(request.getNickName())) {
            profile.setNickName(request.getNickName());
            needUpdate = true;
        }
        if (StringUtils.hasText(request.getSignature())) {
            profile.setSignature(request.getSignature());
            needUpdate = true;
        }
        if (StringUtils.hasText(request.getGender())) {
            GenderEnum genderEnum = GenderEnum.fromValue(request.getGender());
            if (genderEnum == null) {
                log.warn("用户资料更新：无效的性别值被忽略：userId={}, gender={}", userId, request.getGender());
            } else {
                profile.setGender(genderEnum);
                needUpdate = true;
            }
        }
        Integer sportsYearsNumber = parseSportsYears(request.getSportsYears());
        if (sportsYearsNumber != null) {
            Integer startYear = convertYearsToStartYear(sportsYearsNumber);
            if (startYear == null) {
                log.warn("用户资料更新：无效的球龄年数被忽略：userId={}, sportsYears={}", userId, request.getSportsYears());
            } else {
                profile.setSportsStartYear(startYear);
                needUpdate = true;
            }
        }
        if (request.getNtrp() != null) {
            profile.setNtrp(request.getNtrp());
            needUpdate = true;
        }
        if (StringUtils.hasText(request.getPreferredHand())) {
            try {
                profile.setPreferredHand(UserProfile.PreferredHand.valueOf(request.getPreferredHand()));
                needUpdate = true;
            } catch (IllegalArgumentException e) {
                log.warn("用户资料更新：无效的持拍手值被忽略：userId={}, preferredHand={}", userId, request.getPreferredHand());
            }
        }
        if (StringUtils.hasText(request.getHomeDistrict())) {
            if (!isRegionCodeValid(request.getHomeDistrict())) {
                log.warn("用户资料更新：无效的常驻区域被忽略：userId={}, homeDistrict={}", userId, request.getHomeDistrict());
            } else {
                profile.setHomeDistrict(request.getHomeDistrict());
                needUpdate = true;
            }
        }
        if (request.getPower() != null) {
            profile.setPower(request.getPower());
            needUpdate = true;
        }
        if (request.getSpeed() != null) {
            profile.setSpeed(request.getSpeed());
            needUpdate = true;
        }
        if (request.getServe() != null) {
            profile.setServe(request.getServe());
            needUpdate = true;
        }
        if (request.getVolley() != null) {
            profile.setVolley(request.getVolley());
            needUpdate = true;
        }
        if (request.getStamina() != null) {
            profile.setStamina(request.getStamina());
            needUpdate = true;
        }
        if (request.getMental() != null) {
            profile.setMental(request.getMental());
            needUpdate = true;
        }

        if (needUpdate) {
            userProfileMapper.updateById(profile);
        }

        // 5. 再更新球拍列表（如果提供）
        if (request.getRackets() != null) {
            if (CollectionUtils.isEmpty(distinctRackets)) {
                // 空列表=清空：删除用户所有球拍
                LambdaUpdateWrapper<UserRacket> cancelQuery = new LambdaUpdateWrapper<>();
                cancelQuery.eq(UserRacket::getUserId, userId)
                        .set(UserRacket::getDeleted, true);
                userRacketMapper.update(null, cancelQuery);
            } else {
                // 增量更新：查询现有记录
                LambdaQueryWrapper<UserRacket> existingQuery = new LambdaQueryWrapper<>();
                existingQuery.eq(UserRacket::getUserId, userId)
                        .eq(UserRacket::getDeleted, false);
                List<UserRacket> existingRackets = userRacketMapper.selectList(existingQuery);

                // 构建现有记录Map（key = racketModelId），并清理重复的 active 记录
                Map<Long, List<UserRacket>> existingRacketGroup = existingRackets.stream()
                        .collect(Collectors.groupingBy(UserRacket::getRacketModelId));
                Map<Long, UserRacket> existingRacketMap = new HashMap<>();
                List<Long> duplicateRacketIds = new ArrayList<>();
                existingRacketGroup.forEach((modelId, list) -> {
                    if (!list.isEmpty()) {
                        existingRacketMap.put(modelId, list.get(0));
                        if (list.size() > 1) {
                            for (int i = 1; i < list.size(); i++) {
                                if (list.get(i).getUserRacketId() != null) {
                                    duplicateRacketIds.add(list.get(i).getUserRacketId());
                                }
                            }
                        }
                    }
                });
                if (!duplicateRacketIds.isEmpty()) {
                    LambdaUpdateWrapper<UserRacket> dedupeQuery = new LambdaUpdateWrapper<>();
                    dedupeQuery.in(UserRacket::getUserRacketId, duplicateRacketIds)
                            .set(UserRacket::getDeleted, true);
                    userRacketMapper.update(null, dedupeQuery);
                }

                // 构建请求Map（key = racketModelId）
                Map<Long, UserRacketRequest> requestRacketMap = distinctRackets.stream()
                        .collect(Collectors.toMap(UserRacketRequest::getRacketModelId, r -> r));

                // 计算差异
                Set<Long> requestModelIds = requestRacketMap.keySet();
                Set<Long> existingModelIds = existingRacketMap.keySet();

                // toAdd：请求里有，数据库里没有
                List<Long> toAddModelIds = requestModelIds.stream()
                        .filter(modelId -> !existingModelIds.contains(modelId))
                        .collect(Collectors.toList());

                // toRemove：数据库里有，请求里没有
                List<Long> toRemoveModelIds = existingModelIds.stream()
                        .filter(modelId -> !requestModelIds.contains(modelId))
                        .collect(Collectors.toList());

                // toUpdate：两边都有，但 isPrimary 可能变化
                List<UserRacketRequest> toUpdateRackets = requestModelIds.stream()
                        .filter(existingModelIds::contains)
                        .map(requestRacketMap::get)
                        .filter(req -> {
                            UserRacket existing = existingRacketMap.get(req.getRacketModelId());
                            Boolean reqIsPrimary = Boolean.TRUE.equals(req.getIsPrimary());
                            Boolean existingIsPrimary = Boolean.TRUE.equals(existing.getIsPrimary());
                            return !Objects.equals(reqIsPrimary, existingIsPrimary);
                        })
                        .collect(Collectors.toList());

                // 执行操作
                boolean hasChanges = !toAddModelIds.isEmpty() || !toRemoveModelIds.isEmpty() || !toUpdateRackets.isEmpty();

                if (hasChanges) {
                    // toRemove：软删除
                    if (!toRemoveModelIds.isEmpty()) {
                        LambdaUpdateWrapper<UserRacket> removeQuery = new LambdaUpdateWrapper<>();
                        removeQuery.eq(UserRacket::getUserId, userId)
                                .in(UserRacket::getRacketModelId, toRemoveModelIds)
                                .set(UserRacket::getDeleted, true);
                        userRacketMapper.update(null, removeQuery);
                    }

                    // toUpdate：更新 isPrimary
                    for (UserRacketRequest req : toUpdateRackets) {
                        UserRacket existing = existingRacketMap.get(req.getRacketModelId());
                        LambdaUpdateWrapper<UserRacket> updateQuery = new LambdaUpdateWrapper<>();
                        updateQuery.eq(UserRacket::getUserRacketId, existing.getUserRacketId())
                                .set(UserRacket::getIsPrimary, Boolean.TRUE.equals(req.getIsPrimary()));
                        userRacketMapper.update(null, updateQuery);
                    }

                    // toAdd：新增或复用已删除记录（批量查 deleted=true）
                    Map<Long, UserRacket> deletedRacketMap = new HashMap<>();
                    if (!toAddModelIds.isEmpty()) {
                        LambdaQueryWrapper<UserRacket> deletedQuery = new LambdaQueryWrapper<>();
                        deletedQuery.eq(UserRacket::getUserId, userId)
                                .in(UserRacket::getRacketModelId, toAddModelIds)
                                .eq(UserRacket::getDeleted, true);
                        List<UserRacket> deletedRackets = userRacketMapper.selectList(deletedQuery);
                        deletedRacketMap = deletedRackets.stream()
                                .collect(Collectors.toMap(UserRacket::getRacketModelId, r -> r, (a, b) -> a));
                    }

                    for (Long modelId : toAddModelIds) {
                        UserRacketRequest req = requestRacketMap.get(modelId);
                        UserRacket deletedRacket = deletedRacketMap.get(modelId);

                        if (deletedRacket != null) {
                            // 复用已删除记录：复活
                            LambdaUpdateWrapper<UserRacket> reviveQuery = new LambdaUpdateWrapper<>();
                            reviveQuery.eq(UserRacket::getUserRacketId, deletedRacket.getUserRacketId())
                                    .set(UserRacket::getDeleted, false)
                                    .set(UserRacket::getIsPrimary, Boolean.TRUE.equals(req.getIsPrimary()));
                            userRacketMapper.update(null, reviveQuery);
                        } else {
                            // 插入新行
                            UserRacket userRacket = new UserRacket();
                            userRacket.setUserId(userId);
                            userRacket.setRacketModelId(modelId);
                            userRacket.setIsPrimary(Boolean.TRUE.equals(req.getIsPrimary()));
                            userRacket.setDeleted(false);
                            userRacketMapper.insert(userRacket);
                        }
                    }
                }
            }
        }

        // 6. 再更新标签列表（如果提供）
        if (request.getTagIds() != null) {
            if (CollectionUtils.isEmpty(distinctTagIds)) {
                // 空列表=清空：删除用户所有标签
                LambdaUpdateWrapper<UserStyleTag> cancelTagQuery = new LambdaUpdateWrapper<>();
                cancelTagQuery.eq(UserStyleTag::getUserId, userId)
                        .set(UserStyleTag::getDeleted, true);
                userStyleTagMapper.update(null, cancelTagQuery);
            } else {
                // 增量更新：查询现有记录
                LambdaQueryWrapper<UserStyleTag> existingTagQuery = new LambdaQueryWrapper<>();
                existingTagQuery.eq(UserStyleTag::getUserId, userId)
                        .eq(UserStyleTag::getDeleted, false);
                List<UserStyleTag> existingTags = userStyleTagMapper.selectList(existingTagQuery);

                // 构建现有标签ID集合，并清理重复的 active 记录
                Map<Long, List<UserStyleTag>> existingTagGroup = existingTags.stream()
                        .collect(Collectors.groupingBy(UserStyleTag::getTagId));
                Set<Long> existingTagIds = existingTagGroup.keySet();
                List<Long> duplicateTagIds = new ArrayList<>();
                existingTagGroup.forEach((tagId, list) -> {
                    if (list.size() > 1) {
                        for (int i = 1; i < list.size(); i++) {
                            if (list.get(i).getUserStyleTagId() != null) {
                                duplicateTagIds.add(list.get(i).getUserStyleTagId());
                            }
                        }
                    }
                });
                if (!duplicateTagIds.isEmpty()) {
                    LambdaUpdateWrapper<UserStyleTag> dedupeQuery = new LambdaUpdateWrapper<>();
                    dedupeQuery.in(UserStyleTag::getUserStyleTagId, duplicateTagIds)
                            .set(UserStyleTag::getDeleted, true);
                    userStyleTagMapper.update(null, dedupeQuery);
                }

                // 构建请求标签ID集合
                Set<Long> requestTagIds = distinctTagIds.stream()
                        .collect(Collectors.toSet());

                // 计算差异
                // toAdd：请求里有，数据库里没有
                List<Long> toAddTagIds = requestTagIds.stream()
                        .filter(tagId -> !existingTagIds.contains(tagId))
                        .collect(Collectors.toList());

                // toRemove：数据库里有，请求里没有
                List<Long> toRemoveTagIds = existingTagIds.stream()
                        .filter(tagId -> !requestTagIds.contains(tagId))
                        .collect(Collectors.toList());

                // 执行操作
                boolean hasChanges = !toAddTagIds.isEmpty() || !toRemoveTagIds.isEmpty();

                if (hasChanges) {
                    // toRemove：软删除
                    if (!toRemoveTagIds.isEmpty()) {
                        LambdaUpdateWrapper<UserStyleTag> removeQuery = new LambdaUpdateWrapper<>();
                        removeQuery.eq(UserStyleTag::getUserId, userId)
                                .in(UserStyleTag::getTagId, toRemoveTagIds)
                                .set(UserStyleTag::getDeleted, true);
                        userStyleTagMapper.update(null, removeQuery);
                    }

                    // toAdd：新增或复用已删除记录（批量查 deleted=true）
                    Map<Long, UserStyleTag> deletedTagMap = new HashMap<>();
                    if (!toAddTagIds.isEmpty()) {
                        LambdaQueryWrapper<UserStyleTag> deletedQuery = new LambdaQueryWrapper<>();
                        deletedQuery.eq(UserStyleTag::getUserId, userId)
                                .in(UserStyleTag::getTagId, toAddTagIds)
                                .eq(UserStyleTag::getDeleted, true);
                        List<UserStyleTag> deletedTags = userStyleTagMapper.selectList(deletedQuery);
                        deletedTagMap = deletedTags.stream()
                                .collect(Collectors.toMap(UserStyleTag::getTagId, t -> t, (a, b) -> a));
                    }

                    for (Long tagId : toAddTagIds) {
                        UserStyleTag deletedTag = deletedTagMap.get(tagId);

                        if (deletedTag != null) {
                            // 复用已删除记录：复活
                            LambdaUpdateWrapper<UserStyleTag> reviveQuery = new LambdaUpdateWrapper<>();
                            reviveQuery.eq(UserStyleTag::getUserStyleTagId, deletedTag.getUserStyleTagId())
                                    .set(UserStyleTag::getDeleted, false);
                            userStyleTagMapper.update(null, reviveQuery);
                        } else {
                            // 插入新行
                            UserStyleTag userStyleTag = new UserStyleTag();
                            userStyleTag.setUserId(userId);
                            userStyleTag.setTagId(tagId);
                            userStyleTag.setDeleted(false);
                            userStyleTagMapper.insert(userStyleTag);
                        }
                    }
                }
            }
        }

        log.info("用户资料更新成功：userId={}", userId);
        return R.ok("资料更新成功");
    }

    @Override
    public R<ProfileOptionsVo> getProfileOptions() {
        // 1. 查询球拍字典（仅ACTIVE状态）
        LambdaQueryWrapper<RacketDict> racketQuery = new LambdaQueryWrapper<>();
        racketQuery.eq(RacketDict::getStatus, RacketDict.RacketStatus.ACTIVE)
                .orderByAsc(RacketDict::getSort);
        List<RacketDict> racketDicts = racketDictMapper.selectList(racketQuery);

        // 构建树形结构
        List<RacketDictNodeVo> racketOptions = buildRacketTree(racketDicts);

        // 2. 查询球风标签（仅ACTIVE状态）
        LambdaQueryWrapper<StyleTag> tagQuery = new LambdaQueryWrapper<>();
        tagQuery.eq(StyleTag::getStatus, StyleTag.Status.ACTIVE)
                .orderByAsc(StyleTag::getSort);
        List<StyleTag> styleTags = styleTagMapper.selectList(tagQuery);

        List<StyleTagVo> styleTagVos = styleTags.stream()
                .map(st -> {
                    StyleTagVo vo = new StyleTagVo();
                    vo.setTagId(st.getTagId());
                    vo.setName(st.getName());
                    return vo;
                })
                .collect(Collectors.toList());

        // 3. 组装VO
        ProfileOptionsVo vo = new ProfileOptionsVo();
        vo.setRacketOptions(racketOptions);
        vo.setStyleTags(styleTagVos);

        return R.ok(vo);
    }

    @Override
    public R<List<StyleTagVo>> getStyleTags() {
        // 查询球风标签（仅ACTIVE状态）
        LambdaQueryWrapper<StyleTag> tagQuery = new LambdaQueryWrapper<>();
        tagQuery.eq(StyleTag::getStatus, StyleTag.Status.ACTIVE)
                .orderByAsc(StyleTag::getSort);
        List<StyleTag> styleTags = styleTagMapper.selectList(tagQuery);

        List<StyleTagVo> styleTagVos = styleTags.stream()
                .map(st -> {
                    StyleTagVo vo = new StyleTagVo();
                    vo.setTagId(st.getTagId());
                    vo.setName(st.getName());
                    return vo;
                })
                .collect(Collectors.toList());

        return R.ok(styleTagVos);
    }

    @Override
    public R<StarCardVo> getStarCard(Long userId) {
        if (userId == null) {
            return R.error(UserAuthCode.USER_NOT_EXIST);
        }

        // 1. 查询用户基础资料
        UserProfile profile = getUserProfileById(userId);
        if (profile == null) {
            return R.error(UserAuthCode.USER_NOT_EXIST);
        }

        // 2. 查询主力拍（is_primary=1）
        LambdaQueryWrapper<UserRacket> racketQuery = new LambdaQueryWrapper<>();
        racketQuery.eq(UserRacket::getUserId, userId)
                .eq(UserRacket::getDeleted, false)
                .eq(UserRacket::getIsPrimary, true);
        UserRacket primaryRacket = userRacketMapper.selectOne(racketQuery);

        String mainRacketModelName = null;
        if (primaryRacket != null) {
            RacketDict racketDict = racketDictMapper.selectById(primaryRacket.getRacketModelId());
            if (racketDict != null && racketDict.getLevel() == RacketDict.Level.MODEL) {
                // 查询系列和品牌信息，构建短称（品牌 + 型号，不含系列）
                Map<Long, RacketDict> seriesMap = Collections.emptyMap();
                Map<Long, RacketDict> brandMap = Collections.emptyMap();
                if (racketDict.getParentId() != null) {
                    RacketDict series = racketDictMapper.selectById(racketDict.getParentId());
                    if (series != null) {
                        seriesMap = Collections.singletonMap(series.getRacketId(), series);
                        if (series.getParentId() != null) {
                            RacketDict brand = racketDictMapper.selectById(series.getParentId());
                            if (brand != null) {
                                brandMap = Collections.singletonMap(brand.getRacketId(), brand);
                            }
                        }
                    }
                }
                mainRacketModelName = buildShortRacketName(racketDict, seriesMap, brandMap);
            }
        }

        // 3. 查询球风标签列表
        LambdaQueryWrapper<UserStyleTag> tagQuery = new LambdaQueryWrapper<>();
        tagQuery.eq(UserStyleTag::getUserId, userId)
                .eq(UserStyleTag::getDeleted, false);
        List<UserStyleTag> userStyleTags = userStyleTagMapper.selectList(tagQuery);

        List<StyleTagVo> styleTagVos = new ArrayList<>();
        if (!CollectionUtils.isEmpty(userStyleTags)) {
            List<Long> tagIds = userStyleTags.stream()
                    .map(UserStyleTag::getTagId)
                    .collect(Collectors.toList());

            LambdaQueryWrapper<StyleTag> styleTagQuery = new LambdaQueryWrapper<>();
            styleTagQuery.in(StyleTag::getTagId, tagIds);
            List<StyleTag> styleTags = styleTagMapper.selectList(styleTagQuery);

            styleTagVos = styleTags.stream()
                    .map(st -> {
                        StyleTagVo vo = new StyleTagVo();
                        vo.setTagId(st.getTagId());
                        vo.setName(st.getName());
                        return vo;
                    })
                    .collect(Collectors.toList());
        }

        // 4. 组装VO
        StarCardVo vo = new StarCardVo();
        vo.setNickName(profile.getNickName());
        vo.setPortraitUrl(profile.getPortraitUrl());
        vo.setSignature(profile.getSignature());
        vo.setNtrp(profile.getNtrp());
        vo.setStyleTags(styleTagVos);
        vo.setSportsYears(resolveSportsYears(profile.getSportsStartYear()));
        vo.setPreferredHand(profile.getPreferredHand() != null ? profile.getPreferredHand().name() : null);
        vo.setMainRacketModelName(mainRacketModelName);
        vo.setHomeDistrict(profile.getHomeDistrict());
        vo.setHomeDistrictName(getRegionNameByCode(profile.getHomeDistrict()));
        vo.setPower(profile.getPower());
        vo.setSpeed(profile.getSpeed());
        vo.setServe(profile.getServe());
        vo.setVolley(profile.getVolley());
        vo.setStamina(profile.getStamina());
        vo.setMental(profile.getMental());

        return R.ok(vo);
    }

    /**
     * 构建球拍字典树形结构
     */
    private List<RacketDictNodeVo> buildRacketTree(List<RacketDict> racketDicts) {
        if (CollectionUtils.isEmpty(racketDicts)) {
            return Collections.emptyList();
        }

        Map<Long, RacketDictNodeVo> nodeMap = new HashMap<>();
        List<RacketDictNodeVo> rootNodes = new ArrayList<>();

        // 第一遍：创建所有节点
        for (RacketDict dict : racketDicts) {
            RacketDictNodeVo node = new RacketDictNodeVo();
            node.setRacketId(dict.getRacketId());
            node.setParentId(dict.getParentId());
            node.setLevel(dict.getLevel().name());
            node.setName(dict.getName());
            node.setSort(dict.getSort());
            node.setChildren(new ArrayList<>());
            nodeMap.put(dict.getRacketId(), node);
        }

        // 第二遍：构建父子关系
        for (RacketDictNodeVo node : nodeMap.values()) {
            if (node.getParentId() == null) {
                // 根节点（品牌）
                rootNodes.add(node);
            } else {
                // 子节点（系列或型号）
                RacketDictNodeVo parent = nodeMap.get(node.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }

        // 排序
        rootNodes.sort((a, b) -> Integer.compare(a.getSort(), b.getSort()));
        for (RacketDictNodeVo node : nodeMap.values()) {
            if (!CollectionUtils.isEmpty(node.getChildren())) {
                node.getChildren().sort((a, b) -> Integer.compare(a.getSort(), b.getSort()));
            }
        }

        return rootNodes;
    }

    private Integer resolveSportsYears(Integer storedValue) {
        if (storedValue == null) {
            return null;
        }
        int currentYear = Year.now().getValue();
        if (storedValue >= SPORTS_YEAR_THRESHOLD && storedValue <= currentYear) {
            return currentYear - storedValue + 1;
        }
        return storedValue;
    }

    private Integer convertYearsToStartYear(Integer years) {
        if (years == null) {
            return null;
        }
        if (years == 0) {
            return 0;
        }
        if (years < 0) {
            return null;
        }
        int currentYear = Year.now().getValue();
        int startYear = currentYear - years + 1;
        if (startYear < SPORTS_YEAR_THRESHOLD) {
            return null;
        }
        return startYear;
    }

    private Integer parseSportsYears(String yearsStr) {
        if (!StringUtils.hasText(yearsStr)) {
            return null;
        }
        String trimmed = yearsStr.trim();
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isRegionCodeValid(String code) {
        RpcResult<RegionDto> result = regionDubboService.getRegionByCode(code);
        return result != null && result.isSuccess() && result.getData() != null;
    }

    private String getRegionNameByCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        RpcResult<RegionDto> result = regionDubboService.getRegionByCode(code);
        if (result == null || !result.isSuccess() || result.getData() == null) {
            return null;
        }
        return result.getData().getName();
    }

    private String buildFullRacketName(RacketDict model,
                                       Map<Long, RacketDict> seriesMap,
                                       Map<Long, RacketDict> brandMap) {
        String modelName = model.getName();
        RacketDict series = model.getParentId() == null ? null : seriesMap.get(model.getParentId());
        RacketDict brand = (series != null && series.getParentId() != null) ? brandMap.get(series.getParentId()) : null;
        StringBuilder sb = new StringBuilder();
        if (brand != null) {
            sb.append(brand.getName()).append(" ");
        }
        if (series != null) {
            sb.append(series.getName()).append(" ");
        }
        if (modelName != null) {
            sb.append(modelName);
        }
        return sb.toString().trim();
    }

    /**
     * 构建球拍短称（品牌 + 型号，不含系列）
     */
    private String buildShortRacketName(RacketDict model,
                                        Map<Long, RacketDict> seriesMap,
                                        Map<Long, RacketDict> brandMap) {
        String modelName = model.getName();
        RacketDict series = model.getParentId() == null ? null : seriesMap.get(model.getParentId());
        RacketDict brand = (series != null && series.getParentId() != null) ? brandMap.get(series.getParentId()) : null;
        StringBuilder sb = new StringBuilder();
        if (brand != null) {
            sb.append(brand.getName()).append(" ");
        }
        if (modelName != null) {
            sb.append(modelName);
        }
        return sb.toString().trim();
    }

    @Override
    public R<FileUploadVo> uploadAvatar(MultipartFile file) {
        try {
            // 调用文件上传服务上传头像
            String fileUrl = fileUploadService.uploadFile(file, FileTypeEnum.AVATAR);

            // 构建返回结果
            FileUploadVo vo = new FileUploadVo(
                    fileUrl,
                    file.getOriginalFilename(),
                    file.getSize()
            );

            log.info("头像上传成功: fileUrl={}", fileUrl);
            return R.ok(vo);
        } catch (GloboxApplicationException e) {
            log.error("头像上传失败: {}", e.getMessage());
            return R.error(e);
        } catch (Exception e) {
            log.error("头像上传异常", e);
            return R.<FileUploadVo>error(UserAuthCode.UPLOAD_FILE_FAILED).message("头像上传失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<SetGloboxNoResultVo> setGloboxNo(Long userId, SetGloboxNoRequest request) {
        if (userId == null) {
            return R.error(UserAuthCode.USER_NOT_EXIST);
        }
        if (request == null || !StringUtils.hasText(request.getGloboxNo())) {
            return R.error(UserAuthCode.INVALID_PARAM);
        }

        String globoxNo = request.getGloboxNo().trim();
        if (!globoxNo.matches("^\\d{9}$")) {
            return R.error(UserAuthCode.USERNAME_INVALID_FORMAT);
        }

        UserProfile profile = getUserProfileById(userId);
        if (profile == null) {
            return R.error(UserAuthCode.USER_NOT_EXIST);
        }

        String existingGloboxNo = profile.getGloboxNo();
        LocalDateTime lastChanged = profile.getLastGloboxNoChangedAt();
        boolean isFirstTime = !StringUtils.hasText(existingGloboxNo);

        if (!isFirstTime && existingGloboxNo.equals(globoxNo)) {
            LocalDateTime cooldownUntil = lastChanged != null
                    ? lastChanged.plusSeconds(globoxNoCooldownSeconds)
                    : null;
            return R.ok(SetGloboxNoResultVo.builder()
                    .globoxNo(existingGloboxNo)
                    .cooldownUntil(cooldownUntil)
                    .build());
        }

        if (!isFirstTime && lastChanged != null) {
            LocalDateTime cooldownExpire = lastChanged.plusSeconds(globoxNoCooldownSeconds);
            if (LocalDateTime.now().isBefore(cooldownExpire)) {
                log.warn("球盒号修改冷却期未到：userId={}, lastChanged={}, cooldownExpire={}",
                        userId, lastChanged, cooldownExpire);
                SetGloboxNoResultVo result = SetGloboxNoResultVo.builder()
                        .globoxNo(profile.getGloboxNo())
                        .cooldownUntil(cooldownExpire)
                        .build();
                return R.<SetGloboxNoResultVo>error(UserAuthCode.USERNAME_COOLDOWN_NOT_EXPIRED)
                        .message("球盒号修改冷却期未到，请稍后再试")
                        .data(result);
            }
        }

        try {
            LambdaQueryWrapper<UserProfile> uniqueCheck = new LambdaQueryWrapper<>();
            uniqueCheck.eq(UserProfile::getGloboxNo, globoxNo)
                    .ne(UserProfile::getUserId, userId);
            Long exists = userProfileMapper.selectCount(uniqueCheck);
            if (exists != null && exists > 0) {
                log.warn("球盒号已被占用：userId={}, globoxNo={}", userId, globoxNo);
                return R.error(UserAuthCode.USERNAME_ALREADY_TAKEN);
            }

            profile.setGloboxNo(globoxNo);
            profile.setLastGloboxNoChangedAt(LocalDateTime.now());
            userProfileMapper.updateById(profile);

            LocalDateTime cooldownUntil = profile.getLastGloboxNoChangedAt()
                    .plusSeconds(globoxNoCooldownSeconds);
            return R.ok(SetGloboxNoResultVo.builder()
                    .globoxNo(globoxNo)
                    .cooldownUntil(cooldownUntil)
                    .build());
        } catch (DuplicateKeyException e) {
            log.warn("球盒号已被占用：userId={}, globoxNo={}", userId, globoxNo);
            return R.error(UserAuthCode.USERNAME_ALREADY_TAKEN);
        }
    }

    @Override
    public R<UserSearchResultVo> searchUsersByGloboxNo(String keyword, Integer page, Integer pageSize) {
        if (!StringUtils.hasText(keyword)) {
            return R.ok(UserSearchResultVo.builder()
                    .users(Collections.emptyList())
                    .total(0L)
                    .page(page != null ? page : 1)
                    .pageSize(pageSize != null ? pageSize : 20)
                    .build());
        }

        String keywordValue = keyword.trim();
        String keywordEscaped = keywordValue.replace("'", "''");
        int currentPage = (page != null && page > 0) ? page : 1;
        int size = (pageSize != null && pageSize > 0 && pageSize <= 100) ? pageSize : 20;

        Page<UserProfile> profilePage = new Page<>(currentPage, size);
        LambdaQueryWrapper<UserProfile> query = new LambdaQueryWrapper<>();
        query.and(wrapper -> wrapper
                .eq(UserProfile::getGloboxNo, keywordValue)
                .or()
                .likeRight(UserProfile::getGloboxNo, keywordValue)
        );
        query.isNotNull(UserProfile::getGloboxNo);
        query.eq(UserProfile::getCancelled, false);
        query.last("ORDER BY CASE WHEN globox_no = '" + keywordEscaped + "' THEN 0 ELSE 1 END, user_id ASC");

        Page<UserProfile> result = userProfileMapper.selectPage(profilePage, query);
        List<UserSearchItemVo> items = result.getRecords().stream()
                .map(profileItem -> UserSearchItemVo.builder()
                        .userId(profileItem.getUserId())
                        .globoxNo(profileItem.getGloboxNo())
                        .nickName(profileItem.getNickName())
                        .avatarUrl(profileItem.getAvatarUrl())
                        .build())
                .collect(Collectors.toList());

        return R.ok(UserSearchResultVo.builder()
                .users(items)
                .total(result.getTotal())
                .page(currentPage)
                .pageSize(size)
                .build());
    }

    @Override
    public R<StarCardPortraitVo> getStarCardPortrait(Long userId) {
        UserProfile profile = getUserProfileById(userId);
        Assert.isNotEmpty(profile, UserAuthCode.USER_NOT_EXIST);

        // 设置默认球星卡
        if (ObjectUtils.isEmpty(profile.getPortraitUrl()) && userProfileDefaultProperties.getEnableDefaultStarCard()) {
            if (GenderEnum.FEMALE.equals(profile.getGender())) {
                profile.setPortraitUrl(userProfileDefaultProperties.getDefaultStarCardFemaleUrl());
            } else {
                profile.setPortraitUrl(userProfileDefaultProperties.getDefaultStarCardMaleUrl());
            }
        }

        StarCardPortraitVo vo = StarCardPortraitVo.builder()
                .portraitUrl(profile.getPortraitUrl())
                .build();

        return R.ok(vo);
    }

    @Override
    public R<String> updateStarCardPortrait(Long userId, UpdateStarCardPortraitRequest request) {
        UserProfile profile = getUserProfileById(userId);
        Assert.isNotEmpty(profile, UserAuthCode.USER_NOT_EXIST);

        String portraitUrl = request.getPortraitUrl();

        // 如果为空字符串或 null，设置为 null（删除）
        if (portraitUrl == null || portraitUrl.trim().isEmpty()) {
            profile.setPortraitUrl(null);
        } else {
            profile.setPortraitUrl(portraitUrl);
        }

        userProfileMapper.updateById(profile);

        log.info("更新球星卡肖像成功：userId={}, portraitUrl={}", userId, portraitUrl);
        return R.ok("球星卡肖像更新成功");
    }

    @Override
    public R<String> uploadStarCardPortrait(Long userId, MultipartFile file) {
        try {
            // 验证用户存在
            UserProfile profile = getUserProfileById(userId);
            if (profile == null) {
                return R.error(UserAuthCode.USER_NOT_EXIST);
            }

            // 在主线程中读取文件内容（避免 MultipartFile 跨线程传递问题）
            if (file == null || file.isEmpty()) {
                return R.<String>error(UserAuthCode.MISSING_UPLOAD_FILE).message("缺少上传文件");
            }

            byte[] fileContent = file.getBytes();
            String originalFilename = file.getOriginalFilename();

            // 异步处理抠图和数据更新（传递 byte[] 和文件名而不是 MultipartFile）
            portraitMattingService.processAsync(userId, fileContent, originalFilename);

            // 立即返回审核中状态
            log.info("球星卡肖像上传请求已接收: userId={}, filename={}, size={}",
                    userId, originalFilename, fileContent.length);
            return R.ok("球星卡肖像已提交，审核中");

        } catch (GloboxApplicationException e) {
            log.error("球星卡肖像上传异常: userId={}, {}", userId, e.getMessage());
            return R.error(e);
        } catch (Exception e) {
            log.error("球星卡肖像上传异常: userId={}", userId, e);
            return R.<String>error(UserAuthCode.PORTRAIT_MATTING_FAILED).message("球星卡肖像提交失败");
        }
    }

    private String resolveDefaultAvatarUrl(String clientType) {
        if (StringUtils.hasText(clientType)) {
            ClientType type = ClientType.fromValue(clientType);
            if (ClientType.APP.equals(type) && StringUtils.hasText(userProfileDefaultProperties.getDefaultAvatarUrlApp())) {
                return userProfileDefaultProperties.getDefaultAvatarUrlApp();
            }
            if (ClientType.THIRD_PARTY_JSAPI.equals(type) && StringUtils.hasText(userProfileDefaultProperties.getDefaultAvatarUrlMiniapp())) {
                return userProfileDefaultProperties.getDefaultAvatarUrlMiniapp();
            }
        }
        if (StringUtils.hasText(userProfileDefaultProperties.getDefaultAvatarUrl())) {
            return userProfileDefaultProperties.getDefaultAvatarUrl();
        }
        return "";
    }
}
