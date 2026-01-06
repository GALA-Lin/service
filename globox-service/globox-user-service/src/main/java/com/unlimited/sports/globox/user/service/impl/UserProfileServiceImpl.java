package com.unlimited.sports.globox.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.enums.FileTypeEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.result.UserAuthCode;
import com.unlimited.sports.globox.model.auth.dto.UpdateUserProfileRequest;
import com.unlimited.sports.globox.model.auth.dto.UserRacketRequest;
import com.unlimited.sports.globox.model.auth.entity.RacketDict;
import com.unlimited.sports.globox.model.auth.entity.StyleTag;
import com.unlimited.sports.globox.model.auth.entity.UserProfile;
import com.unlimited.sports.globox.model.auth.entity.UserRacket;
import com.unlimited.sports.globox.model.auth.entity.UserStyleTag;
import com.unlimited.sports.globox.model.auth.vo.StarCardVo;
import com.unlimited.sports.globox.model.auth.vo.ProfileOptionsVo;
import com.unlimited.sports.globox.model.auth.vo.RacketDictNodeVo;
import com.unlimited.sports.globox.model.auth.vo.StyleTagVo;
import com.unlimited.sports.globox.model.auth.vo.UserProfileVo;
import com.unlimited.sports.globox.model.auth.vo.UserRacketVo;
import com.unlimited.sports.globox.model.venue.vo.FileUploadVo;
import com.unlimited.sports.globox.user.mapper.RacketDictMapper;
import com.unlimited.sports.globox.user.mapper.StyleTagMapper;
import com.unlimited.sports.globox.user.mapper.UserProfileMapper;
import com.unlimited.sports.globox.user.mapper.UserRacketMapper;
import com.unlimited.sports.globox.user.mapper.UserStyleTagMapper;
import com.unlimited.sports.globox.user.service.FileUploadService;
import com.unlimited.sports.globox.user.service.UserProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Autowired
    private UserProfileMapper userProfileMapper;

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
        racketQuery.eq(UserRacket::getUserId, userId);
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

            Map<Long, String> modelNameMap = racketDicts.stream()
                    .collect(Collectors.toMap(RacketDict::getRacketId, RacketDict::getName));

            racketVos = userRackets.stream()
                    .map(ur -> {
                        UserRacketVo vo = new UserRacketVo();
                        vo.setRacketModelId(ur.getRacketModelId());
                        vo.setRacketModelName(modelNameMap.getOrDefault(ur.getRacketModelId(), ""));
                        vo.setIsPrimary(ur.getIsPrimary());
                        return vo;
                    })
                    .collect(Collectors.toList());
        }

        // 3. 查询用户标签列表
        LambdaQueryWrapper<UserStyleTag> tagQuery = new LambdaQueryWrapper<>();
        tagQuery.eq(UserStyleTag::getUserId, userId);
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
        vo.setRackets(racketVos);
        vo.setStyleTags(styleTagVos);

        return R.ok(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> updateUserProfile(Long userId, UpdateUserProfileRequest request) {
        if (userId == null) {
            return R.error(UserAuthCode.USER_NOT_EXIST);
        }

        // 1. 查询用户资料
        UserProfile profile = getUserProfileById(userId);
        if (profile == null) {
            return R.error(UserAuthCode.USER_NOT_EXIST);
        }

        // 2. 先校验球拍列表（如果提供）
        List<UserRacketRequest> distinctRackets = null;
        if (request.getRackets() != null) {
            // 2.1 去重（按 racketModelId 去重）
            Map<Long, UserRacketRequest> distinctRacketMap = new HashMap<>();
            for (UserRacketRequest r : request.getRackets()) {
                if (r.getRacketModelId() == null) {
                    throw new GloboxApplicationException(UserAuthCode.INVALID_PARAM);
                }
                distinctRacketMap.put(r.getRacketModelId(), r);
            }
            distinctRackets = new ArrayList<>(distinctRacketMap.values());

            // 2.2 空列表=清空：先删除，然后跳过校验/插入
            if (CollectionUtils.isEmpty(distinctRackets)) {
                LambdaQueryWrapper<UserRacket> deleteQuery = new LambdaQueryWrapper<>();
                deleteQuery.eq(UserRacket::getUserId, userId);
                userRacketMapper.delete(deleteQuery);
            } else {
                // 2.3 校验球拍型号是否存在且为MODEL级别
                List<Long> modelIds = distinctRackets.stream()
                        .map(UserRacketRequest::getRacketModelId)
                        .collect(Collectors.toList());

                LambdaQueryWrapper<RacketDict> dictQuery = new LambdaQueryWrapper<>();
                dictQuery.in(RacketDict::getRacketId, modelIds);
                List<RacketDict> racketDicts = racketDictMapper.selectList(dictQuery);

                if (racketDicts.size() != modelIds.size()) {
                    throw new GloboxApplicationException(UserAuthCode.INVALID_RACKET_ID);
                }

                boolean hasInvalidLevel = racketDicts.stream()
                        .anyMatch(dict -> dict.getLevel() != RacketDict.Level.MODEL);
                if (hasInvalidLevel) {
                    throw new GloboxApplicationException(UserAuthCode.INVALID_RACKET_LEVEL);
                }

                boolean hasInactive = racketDicts.stream()
                        .anyMatch(dict -> dict.getStatus() != RacketDict.RacketStatus.ACTIVE);
                if (hasInactive) {
                    throw new GloboxApplicationException(UserAuthCode.INACTIVE_RACKET_MODEL);
                }

                // 2.4 主力拍唯一性校验
                long primaryCount = distinctRackets.stream()
                        .filter(r -> Boolean.TRUE.equals(r.getIsPrimary()))
                        .count();
                if (primaryCount > 1) {
                    throw new GloboxApplicationException(UserAuthCode.MULTIPLE_PRIMARY_RACKET);
                }
            }
        }

        // 3. 先校验标签列表（如果提供）
        List<Long> distinctTagIds = null;
        if (request.getTagIds() != null) {
            // 3.1 去重
            if (request.getTagIds().stream().anyMatch(id -> id == null)) {
                throw new GloboxApplicationException(UserAuthCode.INVALID_PARAM);
            }
            distinctTagIds = request.getTagIds().stream()
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toList());

            // 3.2 空列表=清空：先删除，然后跳过校验/插入
            if (CollectionUtils.isEmpty(distinctTagIds)) {
                LambdaQueryWrapper<UserStyleTag> deleteTagQuery = new LambdaQueryWrapper<>();
                deleteTagQuery.eq(UserStyleTag::getUserId, userId);
                userStyleTagMapper.delete(deleteTagQuery);
            } else {
                // 3.3 校验标签是否存在且为ACTIVE状态
                LambdaQueryWrapper<StyleTag> tagQuery = new LambdaQueryWrapper<>();
                tagQuery.in(StyleTag::getTagId, distinctTagIds)
                        .eq(StyleTag::getStatus, StyleTag.Status.ACTIVE);
                List<StyleTag> validTags = styleTagMapper.selectList(tagQuery);

                if (validTags.size() != distinctTagIds.size()) {
                    throw new GloboxApplicationException(UserAuthCode.INVALID_STYLE_TAG);
                }
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
            try {
                profile.setGender(UserProfile.Gender.valueOf(request.getGender()));
                needUpdate = true;
            } catch (IllegalArgumentException e) {
                throw new GloboxApplicationException(UserAuthCode.INVALID_PARAM);
            }
        }
        if (request.getSportsYears() != null) {
            profile.setSportsYears(request.getSportsYears());
            needUpdate = true;
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
                throw new GloboxApplicationException(UserAuthCode.INVALID_PARAM);
            }
        }
        if (StringUtils.hasText(request.getHomeDistrict())) {
            profile.setHomeDistrict(request.getHomeDistrict());
            needUpdate = true;
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

        // 5. 再更新球拍列表（如果提供且非空）
        if (request.getRackets() != null && !CollectionUtils.isEmpty(distinctRackets)) {
            // 5.1 删除用户所有球拍
            LambdaQueryWrapper<UserRacket> deleteQuery = new LambdaQueryWrapper<>();
            deleteQuery.eq(UserRacket::getUserId, userId);
            userRacketMapper.delete(deleteQuery);

            // 5.2 批量插入（使用已校验的 distinctRackets）
            for (UserRacketRequest racketReq : distinctRackets) {
                UserRacket userRacket = new UserRacket();
                userRacket.setUserId(userId);
                userRacket.setRacketModelId(racketReq.getRacketModelId());
                userRacket.setIsPrimary(Boolean.TRUE.equals(racketReq.getIsPrimary()));
                userRacketMapper.insert(userRacket);
            }
        }

        // 6. 再更新标签列表（如果提供且非空）
        if (request.getTagIds() != null && !CollectionUtils.isEmpty(distinctTagIds)) {
            // 6.1 删除用户所有标签
            LambdaQueryWrapper<UserStyleTag> deleteTagQuery = new LambdaQueryWrapper<>();
            deleteTagQuery.eq(UserStyleTag::getUserId, userId);
            userStyleTagMapper.delete(deleteTagQuery);

            // 6.2 批量插入（使用已校验的 distinctTagIds）
            for (Long tagId : distinctTagIds) {
                UserStyleTag userStyleTag = new UserStyleTag();
                userStyleTag.setUserId(userId);
                userStyleTag.setTagId(tagId);
                userStyleTagMapper.insert(userStyleTag);
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
                .eq(UserRacket::getIsPrimary, true);
        UserRacket primaryRacket = userRacketMapper.selectOne(racketQuery);

        String mainRacketModelName = null;
        if (primaryRacket != null) {
            RacketDict racketDict = racketDictMapper.selectById(primaryRacket.getRacketModelId());
            if (racketDict != null && racketDict.getLevel() == RacketDict.Level.MODEL) {
                mainRacketModelName = racketDict.getName();
            }
        }

        // 3. 查询球风标签列表
        LambdaQueryWrapper<UserStyleTag> tagQuery = new LambdaQueryWrapper<>();
        tagQuery.eq(UserStyleTag::getUserId, userId);
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
        vo.setSportsYears(profile.getSportsYears());
        vo.setPreferredHand(profile.getPreferredHand() != null ? profile.getPreferredHand().name() : null);
        vo.setMainRacketModelName(mainRacketModelName);
        vo.setHomeDistrict(profile.getHomeDistrict());
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
}
