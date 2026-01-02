package com.unlimited.sports.globox.coach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.coach.mapper.CoachCourseTypeMapper;
import com.unlimited.sports.globox.coach.mapper.CoachProfileMapper;
import com.unlimited.sports.globox.coach.service.ICoachInfoService;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.utils.DistanceUtils;
import com.unlimited.sports.globox.dubbo.user.UserDubboService;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoRequest;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoResponse;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import com.unlimited.sports.globox.model.coach.dto.GetCoachListDto;
import com.unlimited.sports.globox.model.coach.entity.CoachCourseType;
import com.unlimited.sports.globox.model.coach.entity.CoachProfile;
import com.unlimited.sports.globox.model.coach.enums.CoachAcceptVenueTypeEnum;
import com.unlimited.sports.globox.model.coach.enums.CoachServiceTypeEnum;
import com.unlimited.sports.globox.model.coach.enums.TeachingYearsFilterEnum;
import com.unlimited.sports.globox.model.coach.vo.CoachDetailVo;
import com.unlimited.sports.globox.model.coach.vo.CoachItemVo;
import com.unlimited.sports.globox.model.coach.vo.CoachServiceVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @since 2026/1/1 12:23
 * 教练服务实现
 */
@Slf4j
@Service
public class CoachInfoServiceImpl implements ICoachInfoService {

    @Autowired
    private CoachProfileMapper coachProfileMapper;

    @Autowired
    private CoachCourseTypeMapper coachCourseTypeMapper;

    @DubboReference(group = "rpc")
    private UserDubboService userDubboService;

    @Override
    public PaginationResult<CoachItemVo> searchCoaches(GetCoachListDto dto) {
        log.info("搜索教练列表 - keyword: {}, sortBy: {}, page: {}/{}",
                dto.getKeyword(), dto.getSortBy(), dto.getPage(), dto.getPageSize());

        // 解析教龄筛选（通过枚举获取）
        Integer minYears = null;
        Integer maxYears = null;
        TeachingYearsFilterEnum filterEnum = TeachingYearsFilterEnum.getByCode(dto.getTeachingYearsFilter());
        if (filterEnum != null) {
            minYears = filterEnum.getMinYears();
            maxYears = filterEnum.getMaxYears();
            // 可选：日志打印更清晰的筛选条件
            log.info("教龄筛选条件: {}", filterEnum.getDesc());
        }
        else {
            log.info("未选择教龄筛选条件/无效的教龄筛选编码: {}", dto.getTeachingYearsFilter());
        }


        // 计算分页偏移量
        int offset = (dto.getPage() - 1) * dto.getPageSize();

        // 调用Mapper进行搜索（在数据库层面完成所有过滤和排序）
        List<Map<String, Object>> searchResults = coachProfileMapper.searchCoaches(
                dto.getKeyword(),
                dto.getMinPrice(),
                dto.getMaxPrice(),
                dto.getServiceAreas(),
                dto.getCertifications(),
                minYears,
                maxYears,
                dto.getGender(),
                dto.getServiceTypes(),
                dto.getLatitude(),
                dto.getLongitude(),
                dto.getMaxDistance(),
                dto.getSortBy(),
                offset,
                dto.getPageSize()
        );

        // 查询总数
        long total = coachProfileMapper.countSearchCoaches(
                dto.getKeyword(),
                dto.getMinPrice(),
                dto.getMaxPrice(),
                dto.getServiceAreas(),
                dto.getCertifications(),
                minYears,
                maxYears,
                dto.getGender(),
                dto.getServiceTypes(),
                dto.getLatitude(),
                dto.getLongitude(),
                dto.getMaxDistance()
        );

        log.info("搜索结果总数：{}", total);

        // 转换为VO
        List<CoachItemVo> coachItemVos = convertToItemVo(searchResults);

        return PaginationResult.build(coachItemVos, total, dto.getPage(), dto.getPageSize());
    }

    @Override
    public CoachDetailVo getCoachDetail(Long coachUserId, Double latitude, Double longitude) {
        log.info("获取教练详情 - coachUserId: {}", coachUserId);

        CoachProfile profile = coachProfileMapper.selectOne(
                new LambdaQueryWrapper<CoachProfile>()
                        .eq(CoachProfile::getCoachUserId, coachUserId)
                        .eq(CoachProfile::getCoachStatus, 1)
        );

        if (profile == null) {
            throw new GloboxApplicationException("教练不存在或暂停接单");
        }

        // 查询用户基本信息
        BatchUserInfoRequest request = new BatchUserInfoRequest();
        request.setUserIds(Collections.singletonList(coachUserId));

        log.info("查询教练用户信息 - coachUserId: {}", coachUserId);

        BatchUserInfoResponse response = userDubboService.batchGetUserInfo(request);

        if (response == null || response.getUsers() == null || response.getUsers().isEmpty()) {
            log.error("无法获取教练基本信息 - coachUserId: {}", coachUserId);
            throw new GloboxApplicationException("无法获取教练基本信息，请联系用户服务管理员");
        }

        UserInfoVo userInfo = response.getUsers().get(0);

        // 查询教练的所有课程服务
        List<CoachCourseType> services = coachCourseTypeMapper.selectList(
                new LambdaQueryWrapper<CoachCourseType>()
                        .eq(CoachCourseType::getCoachUserId, coachUserId)
                        .eq(CoachCourseType::getCoachIsActive, 1)
                        .orderByAsc(CoachCourseType::getCoachServiceTypeEnum)
        );

        // 转换课程服务为VO
        List<CoachServiceVo> serviceVos = services.stream()
                .map(service -> {
                    CoachServiceTypeEnum typeEnum = CoachServiceTypeEnum.fromValue(service.getCoachServiceTypeEnum());
                    return CoachServiceVo.builder()
                            .serviceTypeId(service.getCoachCourseTypeId())
                            .serviceName(service.getCoachCourseTypeName())
                            .serviceType(service.getCoachServiceTypeEnum())
                            .serviceTypeDesc(typeEnum.getDescription())
                            .duration(service.getCoachDuration())
                            .price(service.getCoachPrice())
                            .description(service.getCoachDescription())
                            .build();
                })
                .collect(Collectors.toList());

        // 计算距离
        BigDecimal distance = null;
        if (latitude != null && longitude != null &&
                profile.getCoachLatitude() != null && profile.getCoachLongitude() != null) {
            distance = DistanceUtils.calculateDistance(
                    latitude,
                    longitude,
                    profile.getCoachLatitude(),
                    profile.getCoachLongitude()
            );
        }

        // 解析JSON字段
        List<String> certificationLevels = parseJsonArray(profile.getCoachCertificationLevel());
        List<String> certificationFiles = parseJsonArray(profile.getCoachCertificationFiles());
        List<String> specialtyTags = parseJsonArray(profile.getCoachSpecialtyTags());
        List<String> workPhotos = parseJsonArray(profile.getCoachWorkPhotos());

        // 获取场地类型描述
        String venueTypeDesc = "";
        try {
            if (profile.getCoachAcceptVenueType() != null) {
                venueTypeDesc = CoachAcceptVenueTypeEnum.fromValue(profile.getCoachAcceptVenueType()).getDescription();
            }
        } catch (Exception e) {
            log.warn("未知的场地类型: {}", profile.getCoachAcceptVenueType());
        }

        // 构建简单信息VO（用于详情页的基本信息展示）
        CoachItemVo simpleInfo = CoachItemVo.builder()
                .coachUserInfo(userInfo)
                .coachServiceArea(profile.getCoachServiceArea())
                .coachTeachingYears(profile.getCoachTeachingYears())
                .coachRatingScore(profile.getCoachRatingScore())
                .coachRatingCount(profile.getCoachRatingCount())
                .coachMinPrice(profile.getCoachMinPrice())
                .coachCertificationLevels(certificationLevels)
                .distance(distance)
                .isRecommended(profile.getIsRecommendedCoach() == 1)
                .build();

        // 构建详情VO
        return CoachDetailVo.builder()
                .coachSimpleInfo(simpleInfo)
                .coachTotalCourses(profile.getCoachTotalCourses())
                .coachTotalStudents(profile.getCoachTotalStudents())
                .coachTotalHours(profile.getCoachTotalHours())
                .coachTeachingStyle(profile.getCoachTeachingStyle())
                .coachWorkPhotos(workPhotos)
                .coachSpecialtyTags(specialtyTags)
                .coachRemoteServiceArea(profile.getCoachRemoteServiceArea())
                .coachRemoteMinHours(profile.getCoachRemoteMinHours())
                .coachAcceptVenueType(profile.getCoachAcceptVenueType())
                .coachAcceptVenueTypeDesc(venueTypeDesc)
                .services(serviceVos)
                .coachCertificationFiles(certificationFiles)
                .build();
    }

    /**
     * 转换搜索结果为列表项VO
     */
    private List<CoachItemVo> convertToItemVo(List<Map<String, Object>> searchResults) {
        if (searchResults.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> coachUserIds = searchResults.stream()
                .map(result -> ((Number) result.get("coachUserId")).longValue())
                .collect(Collectors.toList());

        // 批量查询用户基本信息
        BatchUserInfoRequest request = new BatchUserInfoRequest();
        request.setUserIds(coachUserIds);

        // 添加日志
        log.info("批量查询用户信息 - userIds: {}", coachUserIds);

        BatchUserInfoResponse response = userDubboService.batchGetUserInfo(request);

        // 添加日志和空值检查
        if (response == null) {
            log.warn("用户服务返回为空");
            // 使用空Map避免NPE
            Map<Long, UserInfoVo> emptyMap = new HashMap<>();
            return buildCoachItemVos(searchResults, emptyMap);
        }

        if (response.getUsers() == null || response.getUsers().isEmpty()) {
            log.warn("用户服务返回的用户列表为空 - 请求的userIds: {}", coachUserIds);
            Map<Long, UserInfoVo> emptyMap = new HashMap<>();
            return buildCoachItemVos(searchResults, emptyMap);
        }

        Map<Long, UserInfoVo> userInfoMap = response.getUsers().stream()
                .collect(Collectors.toMap(UserInfoVo::getUserId, userInfo -> userInfo));

        log.info("成功获取用户信息 - 数量: {}", userInfoMap.size());

        return buildCoachItemVos(searchResults, userInfoMap);
    }

    // 提取构建逻辑
    private List<CoachItemVo> buildCoachItemVos(List<Map<String, Object>> searchResults,
                                                Map<Long, UserInfoVo> userInfoMap) {
        return searchResults.stream().map(result -> {
            Long coachUserId = ((Number) result.get("coachUserId")).longValue();
            UserInfoVo userInfo = userInfoMap.get(coachUserId);

            // 如果用户信息不存在，记录警告
            if (userInfo == null) {
                log.warn("教练用户信息缺失 - coachUserId: {}", coachUserId);
            }

            String certificationLevelStr = (String) result.get("certificationLevel");
            List<String> certificationTags = parseJsonArray(certificationLevelStr);

            BigDecimal distance = result.get("distance") != null
                    ? new BigDecimal(result.get("distance").toString())
                    : null;

            return CoachItemVo.builder()
                    .coachUserInfo(userInfo)  // 可能为null，前端需要处理
                    .coachServiceArea((String) result.get("serviceArea"))
                    .coachTeachingYears(result.get("teachingYears") != null ?
                            ((Number) result.get("teachingYears")).intValue() : 0)
                    .coachRatingScore(result.get("ratingScore") != null ?
                            new BigDecimal(result.get("ratingScore").toString()) : BigDecimal.ZERO)
                    .coachRatingCount(result.get("ratingCount") != null ?
                            ((Number) result.get("ratingCount")).intValue() : 0)
                    .coachCertificationLevels(certificationTags)
                    .coachMinPrice(result.get("minPrice") != null ?
                            new BigDecimal(result.get("minPrice").toString()) : BigDecimal.ZERO)
                    .distance(distance)
                    .isRecommended(result.get("isRecommended") != null &&
                            ((Number) result.get("isRecommended")).intValue() == 1)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 解析JSON数组字符串为List
     */
    private List<String> parseJsonArray(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // 简单的JSON数组解析（去掉方括号和引号）
            String cleaned = jsonStr.trim()
                    .replaceAll("^\\[|]$", "")
                    .replaceAll("\"", "");

            if (cleaned.isEmpty()) {
                return Collections.emptyList();
            }

            return Arrays.stream(cleaned.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("解析JSON数组失败: {}", jsonStr, e);
            return Collections.emptyList();
        }
    }
}