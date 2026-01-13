package com.unlimited.sports.globox.coach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.coach.mapper.CoachCourseTypeMapper;
import com.unlimited.sports.globox.coach.mapper.CoachProfileMapper;
import com.unlimited.sports.globox.coach.service.ICoachInfoService;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.common.utils.DistanceUtils;
import com.unlimited.sports.globox.dubbo.user.UserDubboService;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoRequest;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoResponse;
import com.unlimited.sports.globox.dubbo.user.dto.UserInfoDto;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import com.unlimited.sports.globox.model.coach.dto.GetCoachListDto;
import com.unlimited.sports.globox.model.coach.entity.CoachCourseType;
import com.unlimited.sports.globox.model.coach.entity.CoachProfile;
import com.unlimited.sports.globox.model.coach.enums.CoachAcceptVenueTypeEnum;
import com.unlimited.sports.globox.model.coach.enums.CoachServiceTypeEnum;
import com.unlimited.sports.globox.model.coach.enums.TeachingYearsFilterEnum;
import com.unlimited.sports.globox.model.coach.vo.CoachDetailVo;
import com.unlimited.sports.globox.model.coach.vo.CoachItemVo;
import com.unlimited.sports.globox.model.coach.vo.CoachListResponse;
import com.unlimited.sports.globox.model.coach.vo.CoachServiceVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
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
    public CoachListResponse searchCoaches(GetCoachListDto dto) {
        log.info("搜索教练列表 - keyword: {}, sortBy: {}, page: {}/{}",
                dto.getKeyword(), dto.getSortBy(), dto.getPage(), dto.getPageSize());

        // 解析教龄筛选
        Integer minYears = null;
        Integer maxYears = null;
        TeachingYearsFilterEnum filterEnum = TeachingYearsFilterEnum.getByCode(dto.getTeachingYearsFilter());
        if (filterEnum != null) {
            minYears = filterEnum.getMinYears();
            maxYears = filterEnum.getMaxYears();
            log.info("教龄筛选条件: {}", filterEnum.getDesc());
        }

        // 计算分页偏移量
        int offset = (dto.getPage() - 1) * dto.getPageSize();

        // 调用Mapper进行搜索
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

        // 构建分页结果
        PaginationResult<CoachItemVo> paginationResult = PaginationResult.build(
                coachItemVos, total, dto.getPage(), dto.getPageSize()
        );

        // 查询所有教练的证书和区域集合（用于筛选选项）
        Set<String> availableCertifications = new HashSet<>();
        Set<String> availableServiceAreas = new HashSet<>();
        CoachListResponse.PriceRange priceRange = getFilterOptions(
                availableCertifications,
                availableServiceAreas
        );

        // 构建返回结果
        return CoachListResponse.builder()
                .coaches(paginationResult)
                .availableCertifications(availableCertifications)
                .availableServiceAreas(availableServiceAreas)
                .priceRange(priceRange)
                .build();
    }

    /**
     * 获取筛选选项（证书、区域、价格区间）
     */
    private CoachListResponse.PriceRange getFilterOptions(
            Set<String> certifications,
            Set<String> serviceAreas) {

        List<CoachProfile> allCoaches = coachProfileMapper.selectAllForFilters();


//        // 查询所有正常接单的教练
//        List<CoachProfile> allCoaches = coachProfileMapper.selectList(
//                new LambdaQueryWrapper<CoachProfile>()
//                        .eq(CoachProfile::getCoachStatus, 1)
//                        .eq(CoachProfile::getCoachAuditStatus, 1)
//        );

        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;

        for (CoachProfile coach : allCoaches) {
            // 收集证书
            if (coach.getCoachCertificationLevel() != null) {
                certifications.addAll(coach.getCoachCertificationLevel());
            }

            // 收集区域（处理逗号分隔）
            if (coach.getCoachServiceArea() != null && !coach.getCoachServiceArea().trim().isEmpty()) {
                String[] areas = coach.getCoachServiceArea().split(",");
                for (String area : areas) {
                    String trimmedArea = area.trim();
                    if (!trimmedArea.isEmpty()) {
                        serviceAreas.add(trimmedArea);
                    }
                }
            }

            // 计算价格区间
            if (coach.getCoachMinPrice() != null) {
                if (minPrice == null || coach.getCoachMinPrice().compareTo(minPrice) < 0) {
                    minPrice = coach.getCoachMinPrice();
                }
            }
            if (coach.getCoachMaxPrice() != null) {
                if (maxPrice == null || coach.getCoachMaxPrice().compareTo(maxPrice) > 0) {
                    maxPrice = coach.getCoachMaxPrice();
                }
            }
        }

        return CoachListResponse.PriceRange.builder()
                .minPrice(minPrice != null ? minPrice : BigDecimal.ZERO)
                .maxPrice(maxPrice != null ? maxPrice : BigDecimal.valueOf(1000))
                .build();
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

        RpcResult<BatchUserInfoResponse> rpcResult = userDubboService.batchGetUserInfo(request);
        Assert.rpcResultOk(rpcResult);
        BatchUserInfoResponse response = rpcResult.getData();

        if (response == null || response.getUsers() == null || response.getUsers().isEmpty()) {
            log.error("无法获取教练基本信息 - coachUserId: {}", coachUserId);
            throw new GloboxApplicationException("无法获取教练基本信息");
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
                            .courseCover(service.getCourseCover())
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

        // 获取场地类型描述
        String venueTypeDesc = "";
        try {
            if (profile.getCoachAcceptVenueType() != null) {
                venueTypeDesc = CoachAcceptVenueTypeEnum.fromValue(profile.getCoachAcceptVenueType()).getDescription();
            }
        } catch (Exception e) {
            log.warn("未知的场地类型: {}", profile.getCoachAcceptVenueType());
        }

        UserInfoVo userInfoVo = new UserInfoVo();
        BeanUtils.copyProperties(userInfo, userInfoVo);

        // 构建简单信息VO
        CoachItemVo simpleInfo = CoachItemVo.builder()
                .coachUserInfo(userInfoVo)
                .coachServiceArea(profile.getCoachServiceArea())
                .coachTeachingYears(profile.getCoachTeachingYears())
                .coachRatingScore(profile.getCoachRatingScore())
                .coachRatingCount(profile.getCoachRatingCount())
                .coachMinPrice(profile.getCoachMinPrice())
                .coachCertificationLevels(profile.getCoachCertificationLevel() != null ?
                        profile.getCoachCertificationLevel() : Collections.emptyList())
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
                .coachAwards(profile.getCoachAward() != null ?
                        profile.getCoachAward() : Collections.emptyList())
                .coachWorkPhotos(profile.getCoachWorkPhotos() != null ?
                        profile.getCoachWorkPhotos() : Collections.emptyList())
                .coachWorkVideos(profile.getCoachWorkVideos() != null ?
                        profile.getCoachWorkVideos() : Collections.emptyList())
                .coachSpecialtyTags(profile.getCoachSpecialtyTags() != null ?
                        profile.getCoachSpecialtyTags() : Collections.emptyList())
                .coachRemoteServiceArea(profile.getCoachRemoteServiceArea())
                .coachRemoteMinHours(profile.getCoachRemoteMinHours())
                .coachAcceptVenueType(profile.getCoachAcceptVenueType())
                .coachAcceptVenueTypeDesc(venueTypeDesc)
                .services(serviceVos)
                .coachCertificationFiles(profile.getCoachCertificationFiles() != null ?
                        profile.getCoachCertificationFiles() : Collections.emptyList())
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

        log.info("批量查询用户信息 - userIds: {}", coachUserIds);

        RpcResult<BatchUserInfoResponse> rpcResult = userDubboService.batchGetUserInfo(request);
        Assert.rpcResultOk(rpcResult);
        BatchUserInfoResponse response = rpcResult.getData();

        Map<Long, UserInfoVo> userInfoMap = new HashMap<>();
        if (response != null && response.getUsers() != null) {
            userInfoMap = response.getUsers().stream()
                    .collect(Collectors.toMap(UserInfoVo::getUserId, userInfo -> userInfo));
        }

        log.info("成功获取用户信息 - 数量: {}", userInfoMap.size());

        return buildCoachItemVos(searchResults, userInfoMap);
    }

    /**
     * 构建教练列表项VO
     */
    private List<CoachItemVo> buildCoachItemVos(List<Map<String, Object>> searchResults,
                                                Map<Long, UserInfoVo> userInfoMap) {
        return searchResults.stream().map(result -> {
            Long coachUserId = ((Number) result.get("coachUserId")).longValue();
            UserInfoVo userInfo = userInfoMap.get(coachUserId);

            if (userInfo == null) {
                log.warn("教练用户信息缺失 - coachUserId: {}", coachUserId);
            } else {
                BeanUtils.copyProperties(userInfo, userInfo);
            }

            // 解析证书列表（从数据库JSON字段）
            String certificationLevelStr = (String) result.get("certificationLevel");
            List<String> certificationTags = parseJsonArrayFromDb(certificationLevelStr);

            BigDecimal distance = result.get("distance") != null
                    ? new BigDecimal(result.get("distance").toString())
                    : null;

            return CoachItemVo.builder()
                    .coachUserInfo(userInfo)
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
     * 解析数据库JSON数组字符串为List
     * 支持格式: ["PTR初级", "PTR中级"] 或 ["PTR初级","PTR中级"]
     */
    private List<String> parseJsonArrayFromDb(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
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