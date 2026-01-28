package com.unlimited.sports.globox.venue.dubbo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.dubbo.venue.IVenueSearchDataService;
import com.unlimited.sports.globox.merchant.mapper.CourtMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueFacilityRelationMapper;
import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.venue.entity.venues.VenuePriceTemplatePeriod;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueFacilityRelation;
import com.unlimited.sports.globox.model.venue.vo.VenueSyncVO;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotRecordMapper;
import com.unlimited.sports.globox.venue.mapper.venues.VenuePriceTemplatePeriodMapper;
import com.unlimited.sports.globox.venue.service.impl.AwayVenueSearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 场馆搜索数据 RPC 服务实现
 * 包含场馆预订可用性查询和数据增量同步功能
 */
@Slf4j
@DubboService(group = "rpc")
@Component
public class VenueSearchDataServiceImpl implements IVenueSearchDataService {

    @Autowired
    private VenueBookingSlotRecordMapper venueBookingSlotRecordMapper;

    @Autowired
    private AwayVenueSearchService awayVenueSearchService;

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private CourtMapper courtMapper;

    @Autowired
    private VenuePriceTemplatePeriodMapper venuePriceTemplatePeriodMapper;

    @Autowired
    private VenueFacilityRelationMapper venueFacilityRelationMapper;

    /**
     * 查询指定日期时间段内不可用的场馆ID列表
     *
     * 业务逻辑：
     * 1. 查询VenueBookingSlotRecord中该日期的时间段内不可用的HOME场馆
     * 2. 查询AwayVenueSearchService中的AWAY场馆的预订情况
     * 3. 合并两种情况的不可用场馆ID列表
     *
     * @param bookingDate 预订日期
     * @param startTime 开始时间（LocalTime）
     * @param endTime 结束时间（LocalTime）
     * @return 不可用的场馆ID列表
     */
    @Override
    public RpcResult<List<Long>> getUnavailableVenueIds(LocalDate bookingDate, LocalTime startTime, LocalTime endTime) {
        log.info("查询不可用场馆: 日期={}, 时间={}-{}", bookingDate, startTime, endTime);

        try {
            // 参数校验
            if (bookingDate == null || startTime == null || endTime == null ||  startTime.isAfter(endTime) || startTime.equals(endTime)) {
                return RpcResult.ok();
            }

            // 查询HOME场馆（自有场馆）中不可用的ID
            Set<Long> unavailableHomeVenueIds = queryUnavailableHomeVenueIds(bookingDate, startTime, endTime);
            log.info("HOME场馆不可用数: {}", unavailableHomeVenueIds.size());

            // 查询AWAY场馆（第三方场馆）中不可用的ID
            Set<Long> unavailableAwayVenueIds = queryUnavailableAwayVenueIds(bookingDate, startTime, endTime);
            log.info("AWAY场馆不可用数: {}", unavailableAwayVenueIds.size());

            // 合并两种情况的结果
            Set<Long> allUnavailableVenueIds = new HashSet<>();
            allUnavailableVenueIds.addAll(unavailableHomeVenueIds);
            allUnavailableVenueIds.addAll(unavailableAwayVenueIds);

            log.info("总共不可用场馆数: {}", allUnavailableVenueIds.size());

            return RpcResult.ok(allUnavailableVenueIds.stream().toList());

        } catch (Exception e) {
            log.error("查询不可用场馆异常: 日期={}, 时间={}-{}", bookingDate, startTime, endTime, e);
            // 直接返回空数据,防止影响数据展示
            return RpcResult.ok();
        }
    }

    /**
     * 查询HOME场馆（自有场馆）中在指定时间段内不可用的ID
     *
     * @param bookingDate 预订日期
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 不可用的HOME场馆ID集合
     */
    private Set<Long> queryUnavailableHomeVenueIds(LocalDate bookingDate, LocalTime startTime, LocalTime endTime) {
        try {
            // 调用Mapper查询不可用的场馆ID
            // venueIds为null表示查询所有场馆
            List<Long> unavailableIds = venueBookingSlotRecordMapper.selectUnavailableVenueIds(
                    null,
                    bookingDate,
                    startTime,
                    endTime
            );

            if (unavailableIds == null) {
                return new HashSet<>();
            }

            log.info("HOME场馆查询完成: 日期={}, 时间={}-{}, 不可用数={}", bookingDate, startTime, endTime, unavailableIds.size());
            return new HashSet<>(unavailableIds);

        } catch (Exception e) {
            log.error("查询HOME场馆不可用ID异常: 日期={}, 时间={}-{}", bookingDate, startTime, endTime, e);
            return new HashSet<>();
        }
    }

    /**
     * 查询AWAY场馆（第三方场馆）中在指定时间段内不可用的ID
     *
     * @param bookingDate 预订日期
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 不可用的AWAY场馆ID集合
     */
    private Set<Long> queryUnavailableAwayVenueIds(LocalDate bookingDate, LocalTime startTime, LocalTime endTime) {
        try {
            // 调用AwayVenueSearchService查询AWAY场馆的不可用ID
            Set<Long> unavailableIds = awayVenueSearchService.getUnavailableAwayVenueIds(
                    bookingDate,
                    startTime,
                    endTime
            );

            if (unavailableIds == null) {
                return new HashSet<>();
            }

            log.info("AWAY场馆查询完成: 日期={}, 时间={}-{}, 不可用数={}", bookingDate, startTime, endTime, unavailableIds.size());
            return unavailableIds;

        } catch (Exception e) {
            log.error("查询AWAY场馆不可用ID异常: 日期={}, 时间={}-{}", bookingDate, startTime, endTime, e);
            return new HashSet<>();
        }
    }

    /**
     * 增量同步场馆数据
     *
     * @param updatedTime 上一次同步的时间戳，为空表示同步全部数据，不为空表示同步该时间之后的数据
     * @return 同步的场馆数据列表（VenueSyncVO格式）
     */
    @Override
    public RpcResult<List<VenueSyncVO>> syncVenueData(LocalDateTime updatedTime) {
        try {
            log.info("开始同步场馆数据: updatedTime={}", updatedTime);
            //批量查询场馆数据
            List<Venue> venues = venueMapper.selectList(new LambdaQueryWrapper<Venue>()
                    .gt(updatedTime != null, Venue::getUpdatedAt, updatedTime));
            if (venues == null || venues.isEmpty()) {
                log.info("没有需要同步的场馆数据");
                return RpcResult.ok(new ArrayList<>());
            }
            // 批量获取所有场馆的Court数据
            List<Long> venueIds = venues.stream().map(Venue::getVenueId).toList();
            List<Court> allCourts = courtMapper.selectList(
                    new LambdaQueryWrapper<Court>()
                            .in(Court::getVenueId, venueIds)
            );
            // 按venueId分组
            Map<Long, List<Court>> courtsByVenueId = allCourts.stream()
                    .collect(Collectors.groupingBy(Court::getVenueId));
            // 批量获取所有价格模板ID对应的价格时段数据
            Set<Long> templateIds = venues.stream()
                    .map(Venue::getTemplateId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, List<VenuePriceTemplatePeriod>> periodsByTemplateId;
            if (!templateIds.isEmpty()) {
                List<VenuePriceTemplatePeriod> allPeriods = venuePriceTemplatePeriodMapper.selectList(
                        new LambdaQueryWrapper<VenuePriceTemplatePeriod>()
                                .in(VenuePriceTemplatePeriod::getTemplateId, templateIds)
                );
                periodsByTemplateId = allPeriods.stream()
                        .collect(Collectors.groupingBy(VenuePriceTemplatePeriod::getTemplateId));
            } else {
                periodsByTemplateId = new HashMap<>();
            }

            // 批量获取所有场馆的设施关联数据
            List<VenueFacilityRelation> allFacilityRelations = venueFacilityRelationMapper.selectList(
                    new LambdaQueryWrapper<VenueFacilityRelation>()
                            .in(VenueFacilityRelation::getVenueId, venueIds)
            );
            // 按venueId分组
            Map<Long, List<VenueFacilityRelation>> facilitiesByVenueId = allFacilityRelations.stream()
                    .collect(Collectors.groupingBy(VenueFacilityRelation::getVenueId));

            // 转换为VenueSyncVO格式
            List<VenueSyncVO> vos = new ArrayList<>();
            venues.forEach(venue -> {
                VenueSyncVO vo = convertVenueToVO(
                        venue,
                        courtsByVenueId.getOrDefault(venue.getVenueId(), new ArrayList<>()),
                        periodsByTemplateId.getOrDefault(venue.getTemplateId(), new ArrayList<>()),
                        facilitiesByVenueId.getOrDefault(venue.getVenueId(), new ArrayList<>())
                );
                if (vo != null) {
                    vos.add(vo);
                }
            });

            log.info("场馆数据同步完成: 转换数={}", vos.size());
            return RpcResult.ok(vos);

        } catch (Exception e) {
            log.error("同步场馆数据异常: updatedTime={}", updatedTime, e);
            return RpcResult.ok(new ArrayList<>());
        }
    }

    /**
     * 将Venue实体转换为VenueSyncVO
     *
     * @param venue 场馆实体
     * @param courts 该场馆的Court列表（从外部批量获取）
     * @param periods 该场馆对应的价格时段列表（从外部批量获取）
     * @param facilityRelations 该场馆的设施关联列表（从外部批量获取）
     * @return VenueSyncVO
     */
    private VenueSyncVO convertVenueToVO(Venue venue, List<Court> courts, List<VenuePriceTemplatePeriod> periods,
                                         List<VenueFacilityRelation> facilityRelations) {
        if (venue == null) {
            return null;
        }
        //  处理球场数量和类型
        Integer courtCount = courts != null ? courts.size() : 0;
        List<Integer> courtTypes = new ArrayList<>();
        List<Integer> groundTypes = new ArrayList<>();
        if (courts != null && !courts.isEmpty()) {
            courtTypes = courts.stream()
                    .map(Court::getCourtType)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            groundTypes = courts.stream()
                    .map(Court::getGroundType)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        }
        // 处理价格（从批量获取的periods中提取）
        BigDecimal priceMin = null;
        BigDecimal priceMax = null;
        if (periods != null && !periods.isEmpty()) {
            // 收集所有价格
            List<BigDecimal> allPrices = new ArrayList<>();
            for (VenuePriceTemplatePeriod period : periods) {
                if (period.getWeekdayPrice() != null) {
                    allPrices.add(period.getWeekdayPrice());
                }
                if (period.getWeekendPrice() != null) {
                    allPrices.add(period.getWeekendPrice());
                }
                if (period.getHolidayPrice() != null) {
                    allPrices.add(period.getHolidayPrice());
                }
            }
            // 找出最小和最大价格
            if (!allPrices.isEmpty()) {
                priceMin = allPrices.stream().min(BigDecimal::compareTo).orElse(null);
                priceMax = allPrices.stream().max(BigDecimal::compareTo).orElse(null);
            }
        }

        // 3. 处理设施（从批量获取的facilityRelations中提取）
        List<Integer> facilities = null;
        if (facilityRelations != null && !facilityRelations.isEmpty()) {
            facilities = facilityRelations.stream()
                    .map(VenueFacilityRelation::getFacilityId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        }

        // 获取封面图（从image_urls中取第一个，或者留空）
        String coverUrl = null;
        if (venue.getImageUrls() != null && !venue.getImageUrls().isEmpty()) {
            String[] urls = venue.getImageUrls().split(";");
            if (urls.length > 0 && !urls[0].trim().isEmpty()) {
                coverUrl = urls[0].trim();
            }
        }
        return VenueSyncVO.builder()
                .venueId(venue.getVenueId())
                .venueName(venue.getName())
                .venueDescription(venue.getDescription())
                .region(venue.getRegion())
                .priceMin(priceMin)
                .priceMax(priceMax)
                .rating(venue.getAvgRating())
                .ratingCount(venue.getRatingCount())
                .coverUrl(coverUrl)
                .latitude(venue.getLatitude())
                .longitude(venue.getLongitude())
                .venueType(venue.getVenueType())
                .courtCount(courtCount)
                .courtTypes(courtTypes.isEmpty() ? null : new ArrayList<>(courtTypes))
                .groundTypes(groundTypes.isEmpty() ? null : new ArrayList<>(groundTypes))
                .facilities(facilities)
                .status(venue.getStatus())
                .createdAt(venue.getCreatedAt())
                .updatedAt(venue.getUpdatedAt())
                .build();
    }
}
