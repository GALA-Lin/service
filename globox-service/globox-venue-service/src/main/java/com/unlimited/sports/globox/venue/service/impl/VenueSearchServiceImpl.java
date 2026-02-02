package com.unlimited.sports.globox.venue.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.merchant.mapper.CourtMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueFacilityRelationMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.venue.dto.GetVenueListDto;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueFacilityRelation;
import com.unlimited.sports.globox.model.venue.entity.venues.VenuePriceTemplatePeriod;
import com.unlimited.sports.globox.model.venue.enums.CourtCountFilter;
import com.unlimited.sports.globox.model.venue.enums.CourtStatus;
import com.unlimited.sports.globox.model.venue.enums.CourtType;
import com.unlimited.sports.globox.model.venue.enums.GroundType;
import com.unlimited.sports.globox.model.venue.vo.VenueItemVo;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotRecordMapper;
import com.unlimited.sports.globox.venue.mapper.venues.VenuePriceTemplatePeriodMapper;
import com.unlimited.sports.globox.venue.service.IVenueBusinessHoursService;
import com.unlimited.sports.globox.venue.service.IVenueSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 场馆搜索服务实现 V2
 * 所有过滤和排序都在数据库层面通过XML SQL完成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VenueSearchServiceImpl implements IVenueSearchService {

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private CourtMapper courtMapper;

    @Autowired
    private VenueFacilityRelationMapper venueFacilityRelationMapper;

    @Autowired
    private VenueBookingSlotRecordMapper venueBookingSlotRecordMapper;

    @Value("${default_image.venue_list_cover}")
    private String defaultVenueListCoverImage;


    @Autowired
    private IVenueBusinessHoursService venueBusinessHoursService;

    @Autowired
    private VenuePriceTemplatePeriodMapper venuePriceTemplatePeriodMapper;

    @Autowired
    private AwayVenueSearchService awayVenueSearchService;

    @Override
    public PaginationResult<VenueItemVo> searchVenues(GetVenueListDto dto) {
        log.info("查询条件{}",dto);
        // 预处理：查询设施过滤的场馆ID列表
        Set<Long> facilityVenueIds = null;
        if (CollectionUtils.isNotEmpty(dto.getFacilities())) {
            List<Long> venueIdsList = venueFacilityRelationMapper.selectList(new LambdaQueryWrapper<VenueFacilityRelation>()
                            .in(VenueFacilityRelation::getFacilityId, dto.getFacilities()))
                    .stream().map(VenueFacilityRelation::getVenueId).toList();
            facilityVenueIds = new HashSet<>(venueIdsList);
            if (facilityVenueIds.isEmpty()) {
                log.info("设施{}过滤后无匹配场馆", JsonUtils.toJson(dto.getFacilities()));
                return PaginationResult.build(Collections.emptyList(), 0L, dto.getPage(), dto.getPageSize());
            }
        }
        // 预处理：查询场地类型过滤的场馆ID列表
        Set<Long> courtTypeVenueIds = null;
        if (CollectionUtils.isNotEmpty(dto.getCourtTypes()) ||
                CollectionUtils.isNotEmpty(dto.getGroundTypes())) {
            courtTypeVenueIds = courtMapper.selectList(new LambdaQueryWrapper<Court>()
                            .select(Court::getVenueId)
                            .in(CollectionUtils.isNotEmpty(dto.getCourtTypes()), Court::getCourtType, dto.getCourtTypes())
                            .in(CollectionUtils.isNotEmpty(dto.getGroundTypes()), Court::getGroundType, dto.getGroundTypes()))
                    .stream()
                    .map(Court::getVenueId)
                    .collect(Collectors.toSet());
            if (courtTypeVenueIds.isEmpty()) {
                log.info("场地类型{},{}过滤后无匹配场馆", JsonUtils.toJson(dto.getCourtTypes()),JsonUtils.toJson(dto.getGroundTypes()));
                return PaginationResult.build(Collections.emptyList(), 0L, dto.getPage(), dto.getPageSize());
            }
        }
        // 预处理：查询时段内不可预订的场馆ID列表（基于时间可见性、营业时间和已预订槽位）
        Set<Long> unavailableVenueIds = new HashSet<>();
        if (dto.getBookingDate() != null && dto.getStartTime() != null && dto.getEndTime() != null) {
            // 1. 基于时间可见性规则过滤（maxAdvanceDays + slotVisibilityTime）
            List<Long> visibilityViolations = venueMapper.selectVenuesViolatingVisibilityRules(dto.getBookingDate());
            if (visibilityViolations != null && !visibilityViolations.isEmpty()) {
                unavailableVenueIds.addAll(visibilityViolations);
                log.info("时间可见性规则过滤的场馆数量：{}", visibilityViolations.size());
            }

            // 2. 基于营业时间规则过滤不可预订的场馆（优先级：关闭 > 特殊 > 常规）
            List<Long> businessHoursUnavailable = venueBusinessHoursService.getUnavailableVenueIds(
                    dto.getBookingDate(),
                    dto.getStartTime(),
                    dto.getEndTime()
            );
            if (businessHoursUnavailable != null && !businessHoursUnavailable.isEmpty()) {
                unavailableVenueIds.addAll(businessHoursUnavailable);
                log.info("营业时间规则过滤的不可预订场馆数量：{}", businessHoursUnavailable.size());
            }

            // 3. 基于已预订槽位过滤不可预订的HOME场馆
            List<Long> slotUnavailable = venueBookingSlotRecordMapper.selectUnavailableVenueIds(
                    null,
                    dto.getBookingDate(),
                    dto.getStartTime(),
                    dto.getEndTime()
            );
            log.info("HOME场馆时间筛选后不可预定的{}", JSON.toJSONString(slotUnavailable));
            if (slotUnavailable != null && !slotUnavailable.isEmpty()) {
                unavailableVenueIds.addAll(slotUnavailable);
                log.info("HOME场馆已预订槽位过滤的不可预订场馆数量：{}", slotUnavailable.size());
            }

            // 4. 基于AWAY球场实时槽位过滤不可预订的场馆
            try {
                Set<Long> awayUnavailable = awayVenueSearchService.getUnavailableAwayVenueIds(
                        dto.getBookingDate(),
                        dto.getStartTime(),
                        dto.getEndTime()
                );
                if (awayUnavailable != null && !awayUnavailable.isEmpty()) {
                    unavailableVenueIds.addAll(awayUnavailable);
                    log.info("AWAY场馆槽位过滤的不可预订场馆数量：{}", awayUnavailable.size());
                }
            } catch (Exception e) {
                log.error("查询AWAY场馆槽位异常，跳过AWAY场馆过滤", e);
            }

            log.info("总计不可预订的场馆数量（HOME+AWAY）：{}", unavailableVenueIds.size());
        }

        // 预处理：查询符合价格范围的场馆ID列表
        Set<Long> priceQualifiedVenueIds = null;
        if (dto.getMinPrice() != null || dto.getMaxPrice() != null) {
            List<Long> priceQualifiedList = getPriceQualifiedVenueIds(dto.getMinPrice(), dto.getMaxPrice());
            if (priceQualifiedList != null && !priceQualifiedList.isEmpty()) {
                priceQualifiedVenueIds = new HashSet<>(priceQualifiedList);
                log.info("价格范围过滤的符合条件场馆数量：{}", priceQualifiedVenueIds.size());
            } else {
                log.info("价格范围{}~{}无匹配场馆", dto.getMinPrice(), dto.getMaxPrice());
                return PaginationResult.build(Collections.emptyList(), 0L, dto.getPage(), dto.getPageSize());
            }
        }

        // 解析场地片数筛选
        Integer minCourtCount = null;
        Integer maxCourtCount = null;
        if (dto.getCourtCountFilter() != null) {
            try {
                CourtCountFilter filter = CourtCountFilter.fromValue(dto.getCourtCountFilter());
                minCourtCount = filter.getMinCount();
                maxCourtCount = filter.getMaxCount();
                log.info("场地片数筛选: {} - {}片", minCourtCount, maxCourtCount);
            } catch (IllegalArgumentException e) {
                log.warn("无效的场地片数筛选值: {}", dto.getCourtCountFilter());
            }
        }

        // 计算分页偏移量
        int offset = (dto.getPage() - 1) * dto.getPageSize();


        List<Long> facilityVenueIdsList = facilityVenueIds != null ? new ArrayList<>(facilityVenueIds) : null;
        List<Long> courtTypeVenueIdsList = courtTypeVenueIds != null ? new ArrayList<>(courtTypeVenueIds) : null;
        List<Long> unavailableVenueIdsList = unavailableVenueIds.isEmpty() ? null : new ArrayList<>(unavailableVenueIds);
        List<Long> priceQualifiedVenueIdsList = priceQualifiedVenueIds != null ? new ArrayList<>(priceQualifiedVenueIds) : null;

        // 使用XML方法在数据库层面进行所有过滤、排序和计算距离
        List<Map<String, Object>> searchResults = venueMapper.searchVenues(
                dto.getKeyword(),
                minCourtCount,
                maxCourtCount,
                dto.getLatitude(),
                dto.getLongitude(),
                dto.getMaxDistance(),
                dto.getSortBy(),
                dto.getSortOrder(),
                facilityVenueIdsList,
                courtTypeVenueIdsList,
                unavailableVenueIdsList,
                priceQualifiedVenueIdsList,
                offset,
                dto.getPageSize()
        );

        // 查询总数
        long total = venueMapper.countSearchVenues(
                dto.getKeyword(),
                minCourtCount,
                maxCourtCount,
                dto.getLatitude(),
                dto.getLongitude(),
                dto.getMaxDistance(),
                facilityVenueIdsList,
                courtTypeVenueIdsList,
                unavailableVenueIdsList,
                priceQualifiedVenueIdsList
        );

        log.info("搜索结果总数：{}", total);

        if ("price".equals(dto.getSortBy())) {
            sortSearchResultsByPrice(searchResults, dto.getSortOrder());
        }

        // 转换为VO
        List<VenueItemVo> venueItemVos = convertToVo(searchResults);

        return PaginationResult.build(venueItemVos, total, dto.getPage(), dto.getPageSize());
    }

    /**
     * 转换为VO（从数据库查询结果）
     */
    private List<VenueItemVo> convertToVo(List<Map<String, Object>> searchResults) {
        if (searchResults.isEmpty()) {
            return Collections.emptyList();
        }

        // 提取场馆ID列表
        List<Long> venueIds = searchResults.stream()
                .map(result -> ((Number) result.get("venueId")).longValue())
                .collect(Collectors.toList());

        // 批量查询设施
        Map<Long, List<String>> facilityMap = getFacilityMap(venueIds);

        // 批量查询场地类型和地面类型
        Map<Long, List<Court>> courtMap = getCourtMap(venueIds);

        // 转换每一条结果
        return searchResults.stream().map(result -> {
            Long venueId = ((Number) result.get("venueId")).longValue();
            String name = (String) result.get("name");
            String region = (String) result.get("region");
            String imageUrls = (String) result.get("imageUrls");

            // 经纬度
            BigDecimal latitude = result.get("latitude") != null
                    ? new BigDecimal(result.get("latitude").toString())
                    : null;
            BigDecimal longitude = result.get("longitude") != null
                    ? new BigDecimal(result.get("longitude").toString())
                    : null;

            // 距离（数据库已计算）
            BigDecimal distance = result.get("distance") != null
                    ? new BigDecimal(result.get("distance").toString())
                    : null;

            // 场地数量（数据库已统计）
            Integer courtCount = result.get("courtCount") != null
                    ? ((Number) result.get("courtCount")).intValue()
                    : 0;

            // 评分
            BigDecimal avgRating = result.get("avgRating") != null
                    ? new BigDecimal(result.get("avgRating").toString())
                    : BigDecimal.ZERO;
            Integer ratingCount = result.get("ratingCount") != null
                    ? ((Number) result.get("ratingCount")).intValue()
                    : 0;

            // 最低价格（从templateId动态计算）
            Long templateId = result.get("templateId") != null
                    ? ((Number) result.get("templateId")).longValue()
                    : null;
            BigDecimal minPrice = calculateMinPrice(templateId);

            // 获取设施列表
            List<String> facilities = facilityMap.getOrDefault(venueId, Collections.emptyList());

            // 获取场地类型和地面类型
            List<Court> courts = courtMap.getOrDefault(venueId, Collections.emptyList());
            List<Integer> courtTypes = courts.stream()
                    .map(Court::getCourtType)
                    .distinct()
                    .collect(Collectors.toList());
            List<String> courtTypesDesc = courtTypes.stream()
                    .map(type -> {
                        try {
                            return CourtType.fromValue(type).getDescription();
                        } catch (IllegalArgumentException e) {
                            log.warn("未知的场地类型: {}", type);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            List<Integer> groundTypes = courts.stream()
                    .map(Court::getGroundType)
                    .distinct()
                    .collect(Collectors.toList());
            List<String> groundTypesDesc = groundTypes.stream()
                    .map(type -> {
                        try {
                            return GroundType.fromValue(type).getDescription();
                        } catch (IllegalArgumentException e) {
                            log.warn("未知的地面类型: {}", type);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 获取封面图片
            String coverImage = getCoverImage(imageUrls);

            return VenueItemVo.builder()
                    .venueId(venueId)
                    .name(name)
                    .region(region)
                    .latitude(latitude)
                    .longitude(longitude)
                    .distance(distance)
                    .coverImage(coverImage)
                    .avgRating(avgRating)
                    .ratingCount(ratingCount)
                    .minPrice(minPrice)
                    .courtTypes(courtTypes)
                    .courtTypesDesc(courtTypesDesc)
                    .groundTypes(groundTypes)
                    .groundTypesDesc(groundTypesDesc)
                    .facilities(facilities)
                    .courtCount(courtCount)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 获取设施映射
     */
    private Map<Long, List<String>> getFacilityMap(List<Long> venueIds) {
        List<VenueFacilityRelation> venueFacilityRelations = venueFacilityRelationMapper.selectList(new LambdaQueryWrapper<VenueFacilityRelation>()
                .in(VenueFacilityRelation::getVenueId, venueIds));
        return venueFacilityRelations.stream()
                .collect(Collectors.groupingBy(
                        VenueFacilityRelation::getVenueId,
                        Collectors.mapping(VenueFacilityRelation::getFacilityName, Collectors.toList())
                ));
    }

    /**
     * 获取场地映射
     */
    private Map<Long, List<Court>> getCourtMap(List<Long> venueIds) {
        List<Court> courts = courtMapper.selectList(new LambdaQueryWrapper<Court>()
                .in(Court::getVenueId, venueIds)
                .eq(Court::getStatus,CourtStatus.OPEN.getValue()));
        return courts.stream().collect(Collectors.groupingBy(Court::getVenueId));
    }

    /**
     * 获取封面图片
     */
    private String getCoverImage(String imageUrls) {
        if (StringUtils.isBlank(imageUrls)) {
            return defaultVenueListCoverImage;
        }
        String[] urls = imageUrls.split(";");
        return urls.length > 0 ? urls[0] : defaultVenueListCoverImage;
    }

    /**
     * 预处理：查询符合价格范围的场馆ID列表
     * 从价格模板中检查是否存在符合用户价格范围的价格
     * 只要存在任何一种价格类型（工作日/周末/节假日）符合条件，就认为场馆符合
     *
     * @param minPrice 最低价格（null则不限制）
     * @param maxPrice 最高价格（null则不限制）
     * @return 符合条件的场馆ID列表
     */
    private List<Long> getPriceQualifiedVenueIds(BigDecimal minPrice, BigDecimal maxPrice) {
        // 查询所有启用场馆及其价格模板
        List<Venue> allVenues = venueMapper.selectList(new LambdaQueryWrapper<Venue>()
                .eq(Venue::getStatus, 1));  // 只查询启用的场馆

        List<Long> qualifiedVenueIds = new ArrayList<>();

        for (Venue venue : allVenues) {
            if (venue.getTemplateId() == null) {
                // 没有配置价格模板的场馆不符合条件
                continue;
            }

            // 查询该场馆的所有价格时段
            List<VenuePriceTemplatePeriod> periods = venuePriceTemplatePeriodMapper.selectByTemplateId(venue.getTemplateId());

            if (periods == null || periods.isEmpty()) {
                continue;
            }

            // 检查是否存在任何价格符合用户的价格范围
            boolean hasPriceInRange = periods.stream().anyMatch(period -> {
                List<BigDecimal> prices = new ArrayList<>();
                if (period.getWeekdayPrice() != null) {
                    prices.add(period.getWeekdayPrice());
                }
                if (period.getWeekendPrice() != null) {
                    prices.add(period.getWeekendPrice());
                }
                if (period.getHolidayPrice() != null) {
                    prices.add(period.getHolidayPrice());
                }

                // 检查是否存在价格符合范围
                return prices.stream().anyMatch(price -> {
                    boolean meetsMin = minPrice == null || price.compareTo(minPrice) >= 0;
                    boolean meetsMax = maxPrice == null || price.compareTo(maxPrice) <= 0;
                    return meetsMin && meetsMax;
                });
            });

            if (hasPriceInRange) {
                qualifiedVenueIds.add(venue.getVenueId());
            }
        }

        return qualifiedVenueIds;
    }

    /**
     * 对搜索结果按价格排序
     *
     * @param searchResults 数据库查询的原始结果
     * @param sortOrder 排序顺序：1=升序, 2=降序
     */
    private void sortSearchResultsByPrice(List<Map<String, Object>> searchResults, Integer sortOrder) {
        searchResults.sort((r1, r2) -> {
            Long templateId1 = r1.get("templateId") != null ? ((Number) r1.get("templateId")).longValue() : null;
            Long templateId2 = r2.get("templateId") != null ? ((Number) r2.get("templateId")).longValue() : null;

            BigDecimal price1 = calculateMinPrice(templateId1);
            BigDecimal price2 = calculateMinPrice(templateId2);

            // sortOrder: 1=升序(ASC), 2=降序(DESC)
            if (sortOrder != null && sortOrder == 2) {
                return price2.compareTo(price1);  // 降序
            } else {
                return price1.compareTo(price2);  // 升序（默认）
            }
        });
    }

    /**
     * 计算场馆最低价格
     * 从价格模板中获取所有价格类型（工作日、周末、节假日），取最小值
     * 如果查不到价格则默认999
     *
     * @param templateId 价格模板ID
     * @return 最低价格
     */
    private BigDecimal calculateMinPrice(Long templateId) {
        if (templateId == null) {
            return new BigDecimal("999");
        }

        List<VenuePriceTemplatePeriod> periods = venuePriceTemplatePeriodMapper.selectByTemplateId(templateId);

        if (periods == null || periods.isEmpty()) {
            return new BigDecimal("999");
        }

        return periods.stream()
                .flatMap(period -> java.util.Arrays.stream(
                        new BigDecimal[]{period.getWeekdayPrice(), period.getWeekendPrice(), period.getHolidayPrice()}
                ))
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(new BigDecimal("999"));
    }
}
