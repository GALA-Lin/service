package com.unlimited.sports.globox.venue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.merchant.mapper.CourtMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueFacilityRelationMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.venue.dto.GetVenueListDto;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueFacilityRelation;
import com.unlimited.sports.globox.model.venue.enums.CourtCountFilter;
import com.unlimited.sports.globox.model.venue.enums.CourtStatus;
import com.unlimited.sports.globox.model.venue.enums.CourtType;
import com.unlimited.sports.globox.model.venue.enums.GroundType;
import com.unlimited.sports.globox.model.venue.vo.VenueItemVo;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotRecordMapper;
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

    @Override
    public PaginationResult<VenueItemVo> searchVenues(GetVenueListDto dto) {

        // 预处理：查询设施过滤的场馆ID列表
        Set<Long> facilityVenueIds = null;
        if (CollectionUtils.isNotEmpty(dto.getFacilities())) {
            List<Long> venueIdsList = venueFacilityRelationMapper.selectList(new LambdaQueryWrapper<VenueFacilityRelation>()
                    .in(VenueFacilityRelation::getFacilityName,dto.getFacilities()))
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

            // 3. 基于已预订槽位过滤不可预订的场馆
            List<Long> slotUnavailable = venueBookingSlotRecordMapper.selectUnavailableVenueIds(
                    null,
                    dto.getBookingDate(),
                    dto.getStartTime(),
                    dto.getEndTime()
            );
            if (slotUnavailable != null && !slotUnavailable.isEmpty()) {
                unavailableVenueIds.addAll(slotUnavailable);
                log.info("已预订槽位过滤的不可预订场馆数量：{}", slotUnavailable.size());
            }

            log.info("总计不可预订的场馆数量：{}", unavailableVenueIds.size());
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

        // 使用XML方法在数据库层面进行所有过滤、排序和计算距离
        List<Map<String, Object>> searchResults = venueMapper.searchVenues(
                dto.getKeyword(),
                dto.getMinPrice(),
                dto.getMaxPrice(),
                minCourtCount,
                maxCourtCount,
                dto.getLatitude(),
                dto.getLongitude(),
                dto.getMaxDistance(),
                dto.getSortBy(),
                facilityVenueIdsList,
                courtTypeVenueIdsList,
                unavailableVenueIdsList,
                offset,
                dto.getPageSize()
        );

        // 查询总数
        long total = venueMapper.countSearchVenues(
                dto.getKeyword(),
                dto.getMinPrice(),
                dto.getMaxPrice(),
                minCourtCount,
                maxCourtCount,
                dto.getLatitude(),
                dto.getLongitude(),
                dto.getMaxDistance(),
                facilityVenueIdsList,
                courtTypeVenueIdsList,
                unavailableVenueIdsList
        );

        log.info("搜索结果总数：{}", total);

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

            // 距离（数据库已计算）
            BigDecimal distance = result.get("distance") != null
                    ? new BigDecimal(result.get("distance").toString())
                    : null;

            // 场地数量（数据库已统计）
            Integer courtCount = result.get("courtCount") != null
                    ? ((Number) result.get("courtCount")).intValue()
                    : 0;

            // 价格和评分
            BigDecimal minPrice = result.get("minPrice") != null
                    ? new BigDecimal(result.get("minPrice").toString())
                    : BigDecimal.ZERO;
            BigDecimal avgRating = result.get("avgRating") != null
                    ? new BigDecimal(result.get("avgRating").toString())
                    : BigDecimal.ZERO;
            Integer ratingCount = result.get("ratingCount") != null
                    ? ((Number) result.get("ratingCount")).intValue()
                    : 0;

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
}
