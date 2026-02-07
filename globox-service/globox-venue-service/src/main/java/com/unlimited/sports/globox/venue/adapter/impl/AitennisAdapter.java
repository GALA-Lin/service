package com.unlimited.sports.globox.venue.adapter.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.VenueCode;
import com.unlimited.sports.globox.model.venue.entity.venues.ThirdPartyPlatform;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueThirdPartyConfig;
import com.unlimited.sports.globox.service.RedisService;
import com.unlimited.sports.globox.venue.adapter.ThirdPartyPlatformAdapter;
import com.unlimited.sports.globox.venue.adapter.constant.AwayVenueCacheConstants;
import com.unlimited.sports.globox.venue.adapter.dto.*;
import com.unlimited.sports.globox.venue.adapter.dto.aitennis.*;
import com.unlimited.sports.globox.venue.mapper.ThirdPartyPlatformMapper;
import com.unlimited.sports.globox.venue.service.IThirdPartyTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.unlimited.sports.globox.venue.util.TimeSlotSplitUtil;

/**
 * aitennis平台适配器
 */
@Slf4j
@Component
public class AitennisAdapter implements ThirdPartyPlatformAdapter {

    @Autowired
    @Qualifier("thirdPartyRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    @Lazy
    private IThirdPartyTokenService tokenService;

    @Autowired
    private ThirdPartyPlatformMapper platformMapper;

    @Autowired
    private RedisService redisService;

    private static final String PLATFORM_CODE = "aitennis";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    // API路径常量
    private static final String API_PATH_LOGIN = "/b/v1/staffs:loginByPhone";
    private static final String API_PATH_GET_DAILY_EVENTS = "/b/v1/calendar:getDailyCourtEvents";
    private static final String API_PATH_GET_EVENTS_DETAILS = "/b/v1/calendar:getEventsDetails";
    private static final String API_PATH_LOCK_COURTS = "/b/v1/lockCourts";

    // 事件类型常量
    private static final String EVENT_TYPE_RENT_PRICE = "rent_price";
    private static final String EVENT_TYPE_LOCK_COURT = "lock_court";

    // 锁场相关常量
    private static final int LOCK_PURPOSE = 20;  // 锁场目的：订场
    private static final boolean IS_DUPLICATE_ALLOWED = false;  // 不允许重复锁场


    /**
     * 平台的基础API地址（从数据库加载并缓存）
     */
    private String platformBaseApiUrl;

    /**
     * 初始化：从数据库加载平台的基础API地址
     */
    @PostConstruct
    public void init() {
        ThirdPartyPlatform platform = platformMapper.selectOne(
                new LambdaQueryWrapper<ThirdPartyPlatform>()
                        .eq(ThirdPartyPlatform::getPlatformCode, PLATFORM_CODE)
        );
        if (platform != null && platform.getBaseApiUrl() != null) {
            this.platformBaseApiUrl = platform.getBaseApiUrl();
            log.info("[aitennis] 平台API地址已加载: {}", platformBaseApiUrl);
        } else {
            log.warn("[aitennis] 未找到平台配置，platformCode={}", PLATFORM_CODE);
        }
    }

    /**
     * 获取API基础地址
     * 优先使用场馆配置的专用API地址，否则使用平台默认地址
     */
    private String getApiBaseUrl(VenueThirdPartyConfig config) {
        String apiUrl = config.getApiUrl();
        if (apiUrl != null && !apiUrl.trim().isEmpty()) {
            // 去掉末尾的斜杠
            return apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
        }

        // 使用平台的基础API地址
        if (platformBaseApiUrl == null || platformBaseApiUrl.trim().isEmpty()) {
            throw new IllegalStateException("平台基础API地址未配置: " + PLATFORM_CODE);
        }
        return platformBaseApiUrl.endsWith("/")
                ? platformBaseApiUrl.substring(0, platformBaseApiUrl.length() - 1)
                : platformBaseApiUrl;
    }

    @Override
    public List<ThirdPartyCourtSlotDto> querySlots(VenueThirdPartyConfig config, LocalDate date) {
        log.info("[aitennis] 查询槽位: venueId={}, thirdPartyVenueId={}, date={}",
                config.getVenueId(), config.getThirdPartyVenueId(), date);
        String cacheKey = AwayVenueCacheConstants.buildSlotsCacheKey(config.getVenueId(), date);
        List<ThirdPartyCourtSlotDto> cachedSlots = redisService.getCacheObject(cacheKey,
                new TypeReference<>() {
                });

        if (cachedSlots != null && !cachedSlots.isEmpty()) {
            log.info("[aitennis] 从缓存获取槽位成功: venueId={}, date={}, 场地数: {}",
                    config.getVenueId(), date, cachedSlots.size());
            return cachedSlots;
        }
        try {
            List<ThirdPartyCourtSlotDto> slots = querySlotsFromAPI(config, date);

            if (slots != null && !slots.isEmpty()) {
                redisService.setCacheObject(cacheKey, slots, AwayVenueCacheConstants.SLOTS_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                log.info("[aitennis] 槽位已缓存: venueId={}, date={}, ttl={}min",
                        config.getVenueId(), date, AwayVenueCacheConstants.SLOTS_CACHE_TTL_MINUTES);
            }

            return slots;

        } catch (Exception e) {
            log.error("[aitennis] 查询槽位异常: venueId={}", config.getVenueId(), e);
            return null;
        }
    }

    /**
     * 从API查询槽位
     */
    private List<ThirdPartyCourtSlotDto> querySlotsFromAPI(VenueThirdPartyConfig config, LocalDate date) {
        try {
            List<AitennisCourtEvent> aitennisCourtEvents = queryDailyCourtEvents(config, date);
            if(aitennisCourtEvents == null) {
                return null;
            }
            log.info("[aitennis] 查询槽位成功: venueId={}, 事件数量={}", config.getVenueId(), aitennisCourtEvents.size());
            return convertEventsToDto(aitennisCourtEvents);
        } catch (Exception e) {
            log.error("[aitennis] 查询槽位异常: venueId={}", config.getVenueId(), e);
            return null;
        }
    }

    /**
     * 将aitennis的CourtEvent数据转换为统一的DTO格式
     * aitennis返回的是1小时级别的slot，需要拆分成30分钟，价格平分 如果是半个小时的,就不需要拆分
     */
    private List<ThirdPartyCourtSlotDto> convertEventsToDto(List<AitennisCourtEvent> events) {
        // 按照场地ID分组
        Map<String, List<AitennisCourtEvent>> groupedEvents = events.stream()
                .collect(Collectors.groupingBy(AitennisCourtEvent::getCourtId));

        // 对分组后的场地排序，并转换为 DTO
        return groupedEvents.entrySet().stream()
                .sorted((e1, e2) -> {
                    // 先按 court_sort 排序，如果相同则按 court_id 排序
                    List<AitennisCourtEvent> events1 = e1.getValue();
                    List<AitennisCourtEvent> events2 = e2.getValue();

                    Integer sort1 = events1.isEmpty() ? 0 : events1.get(0).getCourtSort();
                    Integer sort2 = events2.isEmpty() ? 0 : events2.get(0).getCourtSort();

                    // 优先按 court_sort 排序
                    int sortCompare = Integer.compare(sort1 != null ? sort1 : 0, sort2 != null ? sort2 : 0);
                    if (sortCompare != 0) {
                        return sortCompare;
                    }

                    // court_sort 相同时按 court_id 排序
                    return e1.getKey().compareTo(e2.getKey());
                })
                .map(entry -> {
                    String courtId = entry.getKey();
                    List<AitennisCourtEvent> courtEvents = entry.getValue();
                    // 获取场地名称（同一场地的所有事件名称相同）
                    String courtName = courtEvents.isEmpty() ? "" : courtEvents.get(0).getCourtName();
                    // 转换时间槽并按开始时间排序
                    // aitennis返回的槽位可能是1小时或30分钟，需要根据时长判断是否拆分和价格处理
                    List<ThirdPartySlotDto> slots = courtEvents.stream()
                            .flatMap(event -> {
                                // 判断是否可用：type为rent_price表示可租赁
                                boolean available = "rent_price".equals(event.getType());

                                // 获取价格（从items中提取）
                                BigDecimal eventPrice = BigDecimal.ZERO;
                                if (event.getItems() != null && !event.getItems().isEmpty()) {
                                    AitennisEventItem firstItem = event.getItems().get(0);
                                    if (firstItem.getData() != null && firstItem.getData().getPrice() != null) {
                                        try {
                                            eventPrice = new BigDecimal(firstItem.getData().getPrice());
                                        } catch (NumberFormatException e) {
                                            log.warn("[aitennis] 价格格式错误: {}", firstItem.getData().getPrice());
                                            return Stream.empty();
                                        }
                                    }
                                }

                                // 解析时间，处理24:00特殊情况
                                // 先检查原始值是否为2400/2359，再做格式转换
                                boolean isEndTime2400 = "2400".equals(event.getEndTime()) || "2359".equals(event.getEndTime());
                                String startTimeStr = formatTime(event.getStartTime());
                                String endTimeStr = formatTime(event.getEndTime());
                                LocalTime startTime = LocalTime.parse(startTimeStr.length() > 5 ? startTimeStr.substring(0, 5) : startTimeStr);
                                // 24:00/23:59:59需要特殊处理，转换为23:59用于LocalTime解析
                                LocalTime endTime = isEndTime2400 ? LocalTime.of(23, 59) : LocalTime.parse(endTimeStr.length() > 5 ? endTimeStr.substring(0, 5) : endTimeStr);
                                
                                // 计算槽位时长（分钟），24:00按24*60计算
                                long endMinutes = isEndTime2400 ? 24 * 60 : endTime.getHour() * 60 + endTime.getMinute();
                                long startMinutes = startTime.getHour() * 60 + startTime.getMinute();
                                long durationMinutes = endMinutes - startMinutes;

                                List<ThirdPartySlotDto> resultSlots = new ArrayList<>();
                                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

                                if (durationMinutes == 60) {
                                    // 1小时槽位：拆分成两个30分钟，价格平分
                                    BigDecimal halfHourPrice = eventPrice.divide(new BigDecimal(2), 2, RoundingMode.HALF_UP);
                                    LocalTime midTime = startTime.plusMinutes(30);
                                    // 第一个30分钟槽位
                                    resultSlots.add(ThirdPartySlotDto.builder()
                                            .startTime(startTime.format(timeFormatter))
                                            .endTime(midTime.format(timeFormatter))
                                            .available(available)
                                            .price(halfHourPrice)
                                            .build());
                                    // 第二个30分钟槽位（24:00/2400统一输出为23:59:59）
                                    String secondSlotEndTime = isEndTime2400 ? "23:59:59" : endTime.format(timeFormatter);
                                    resultSlots.add(ThirdPartySlotDto.builder()
                                            .startTime(midTime.format(timeFormatter))
                                            .endTime(secondSlotEndTime)
                                            .available(available)
                                            .price(halfHourPrice)
                                            .build());
                                } else if (durationMinutes == 30) {
                                    // 30分钟槽位：直接使用，价格不变（24:00/2400统一输出为23:59:59）
                                    String slotEndTime = isEndTime2400 ? "23:59:59" : endTime.format(timeFormatter);
                                    resultSlots.add(ThirdPartySlotDto.builder()
                                            .startTime(startTime.format(timeFormatter))
                                            .endTime(slotEndTime)
                                            .available(available)
                                            .price(eventPrice)
                                            .build());
                                } else {
                                    // 其他时长（如share_play、rent_court等占用多个小时的情况）：跳过，不生成槽位
                                    // 这些通常是活动或已预订的场地，available=false，不需要显示
                                    return Stream.empty();
                                }
                                return resultSlots.stream();
                            })
                            .sorted(Comparator.comparing(slot -> LocalTime.parse(slot.getStartTime()))) // 按开始时间排序
                            .collect(Collectors.toList());

                    return ThirdPartyCourtSlotDto.builder()
                            .thirdPartyCourtId(courtId)
                            .courtName(courtName)
                            .slots(slots)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 将时间格式从 "0700" 转换为 "07:00"
     * 特殊处理：2400 转换为 23:59:59（表示午夜），2359 也转换为 23:59:59（与我们系统一致）
     */
    private String formatTime(String time) {
        if (time == null || time.length() != 4) {
            return time;
        }
        // 2400 或 2359 统一转换为 23:59:59（我们系统用 23:59:59 表示午夜）
        if ("2400".equals(time) || "2359".equals(time)) {
            return "23:59:59";
        }
        return time.substring(0, 2) + ":" + time.substring(2, 4);
    }

    @Override
    public LockSlotsResult lockSlots(VenueThirdPartyConfig config, List<SlotLockRequest> slotRequests, String remark) {
        log.info("[aitennis] 批量锁定槽位: venueId={}, slotCount={}, remark={}",
                config.getVenueId(), slotRequests.size(), remark);
        if (slotRequests.isEmpty()) {
            log.warn("[aitennis] slotRequests为空");
            throw new GloboxApplicationException(VenueCode.BOOKING_SLOT_INFO_EMPTY);
        }
        try {
            LocalDate bookingDate = slotRequests.get(0).getDate();
            // 调用 getDailyCourtEvents 获取数据
            List<AitennisCourtEvent> allEvents = queryDailyCourtEvents(config, bookingDate);
            if (allEvents == null) {
                log.error("[aitennis] 查询events失败");
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }
            List<AwaySlotPrice> slotPrices = calculatePricingInterior(bookingDate,allEvents);
            log.info("[aitennis] 锁场 - 查询到总事件数: {}", allEvents.size());
            // 从响应中找到时间段对应的 rent_price 类型的项目，获取其 rent_price_id
            // 支持跨场地锁定：遍历所有 slotRequests，根据每个槽位的 thirdPartyCourtId 查找对应的 rent_price_id
            // 重要：当多个30分钟槽位对应同一个aitennis的rent_price时，需要合并为一次锁场请求
            // 但锁场时间应该使用我们请求的时间段，而不是aitennis的完整1小时时间段
            // 使用Map按rent_price_id分组，记录每个rent_price_id对应的所有槽位请求时间
            Map<String, List<SlotLockRequest>> rentPriceIdToSlotsMap = new LinkedHashMap<>();
            for (SlotLockRequest slotRequest : slotRequests) {
                String courtId = slotRequest.getThirdPartyCourtId();
                String formattedStartTime = unformatTime(slotRequest.getStartTime());
                String formattedEndTime = unformatTime(slotRequest.getEndTime());
                // Away API返回的是小时粒度的events（如2100-2200）
                // 但我们需要锁定30分钟粒度的槽位（如2100-2130）
                // 所以应该找时间段有重叠的events，而不是精确匹配
                AitennisCourtEvent rentPriceEvent = allEvents.stream()
                        .filter(event -> event.getCourtId().equals(courtId)
                                && EVENT_TYPE_RENT_PRICE.equals(event.getType())
                                && event.getItems() != null
                                && !event.getItems().isEmpty()
                                && isTimeOverlap(formattedStartTime, formattedEndTime, event.getStartTime(), event.getEndTime()))
                        .findFirst()
                        .orElse(null);
                if (rentPriceEvent == null) {
                    log.error("[aitennis] 槽位不可用或已被预订: courtId={}, 请求时段: {}-{}, 原始时间: {}-{}",
                            courtId, formattedStartTime, formattedEndTime, slotRequest.getStartTime(), slotRequest.getEndTime());
                    throw new GloboxApplicationException(VenueCode.SLOT_NOT_AVAILABLE);
                }
                String rentPriceId = rentPriceEvent.getItems().get(0).getId();
                // 按rent_price_id分组，收集同一rent_price_id下的所有槽位请求
                rentPriceIdToSlotsMap.computeIfAbsent(rentPriceId, k -> new ArrayList<>()).add(slotRequest);
            }
            
            // 构建锁场请求，合并同一rent_price_id下的多个30分钟槽位为连续时间段
            List<String> rentPriceIds = new ArrayList<>();
            List<AitennisLockRequest.RentPriceInfo> rentPriceInfos = new ArrayList<>();
            for (Map.Entry<String, List<SlotLockRequest>> entry : rentPriceIdToSlotsMap.entrySet()) {
                String rentPriceId = entry.getKey();
                List<SlotLockRequest> slots = entry.getValue();
                rentPriceIds.add(rentPriceId);
                // 找出这些槽位的最早开始时间和最晚结束时间，合并为一个连续时间段
                String minStartTime = slots.stream()
                        .map(s -> unformatTime(s.getStartTime()))
                        .min(String::compareTo)
                        .orElse("0000");
                String maxEndTime = slots.stream()
                        .map(s -> unformatTime(s.getEndTime()))
                        .max(String::compareTo)
                        .orElse("2400");
                // 转换为锁场请求格式，23:59:59转为24:00
                String lockStartTime = formatTime(minStartTime);
                String lockEndTime = "2400".equals(maxEndTime) || "2359".equals(maxEndTime) 
                        ? "24:00" : formatTime(maxEndTime);
                rentPriceInfos.add(AitennisLockRequest.RentPriceInfo.builder()
                        .id(rentPriceId)
                        .startTime(lockStartTime)
                        .endTime(lockEndTime)
                        .build());
                log.info("[aitennis] 锁场请求项: rentPriceId={}, 时间={}-{}, 槽位数={}", rentPriceId, lockStartTime, lockEndTime, slots.size());
            }

            log.info("[aitennis] 所有槽位可用性校验通过，准备锁场: slotCount={}, 去重后锁场项数={}", slotRequests.size(), rentPriceInfos.size());
            AitennisLockRequest lockRequest = AitennisLockRequest.builder()
                    .rentPriceId(rentPriceIds)
                    .purpose(LOCK_PURPOSE)
                    .remark(remark != null ? remark : "")
                    .isDuplicate(IS_DUPLICATE_ALLOWED)
                    .rentPrice(rentPriceInfos)
                    .build();

            // 调用 lockCourts API 进行锁定
            callLockCourtsApi(config, lockRequest);

            // 锁场成功后，重新查询events获取lock_court事件ID 如果查询失败不影响预定成功,因为away调用不可回滚
            // 后续解锁需要人工干预解锁
            List<AitennisCourtEvent> eventsAfterLock = queryDailyCourtEvents(config, bookingDate);
            log.info("[锁场后]获取到的每日事件数据:{}",JSON.toJSON(eventsAfterLock));
            if (eventsAfterLock == null) {
                log.error("[aitennis] 锁场成功但重新查询events失败");
                // 返回失败结果，不抛异常
                String cacheKey = AwayVenueCacheConstants.buildSlotsCacheKey(config.getVenueId(), bookingDate);
                redisService.deleteObject(cacheKey);
                return LockSlotsResult.builder()
                        .bookingIds(new LinkedHashMap<>())
                        .slotPrices(new ArrayList<>())
                        .build();
            }
            // 收集涉及到的场地
            Set<String> courtIds = slotRequests.stream()
                    .map(SlotLockRequest::getThirdPartyCourtId)
                    .collect(Collectors.toSet());

            // 找出这些场地的lock_court事件，获取eventId（source_id）
            // 精确匹配传入的remark，确保拿到正确的lock_id
            final String expectedRemark = remark;
            List<AitennisCourtEvent> lockCourtEvents = eventsAfterLock.stream()
                    .filter(event -> courtIds.contains(event.getCourtId())
                            && EVENT_TYPE_LOCK_COURT.equals(event.getType())
                            && event.getItems() != null
                            && !event.getItems().isEmpty()
                            && event.getItems().stream().anyMatch(item -> 
                                    item.getData() != null && item.getData().getRemark() != null 
                                    && item.getData().getRemark().equals(expectedRemark)))
                    .toList();
            log.info("[锁场后]查询到的lockCourtEvents, expectedRemark={}, events={}", expectedRemark, lockCourtEvents);
            if (lockCourtEvents.isEmpty()) {
                log.error("[aitennis] 锁场成功但未找到lock_court事件");
                // 返回失败结果，不抛异常
                String cacheKey = AwayVenueCacheConstants.buildSlotsCacheKey(config.getVenueId(), bookingDate);
                redisService.deleteObject(cacheKey);
                return LockSlotsResult.builder()
                        .bookingIds(new LinkedHashMap<>())
                        .slotPrices(new ArrayList<>())
                        .build();
            }

            // 根据每个slotRequest的时间匹配对应的lock_court事件，获取eventId
            Map<SlotLockRequest, String> resultMap = new LinkedHashMap<>();
            for (SlotLockRequest slotRequest : slotRequests) {
                String courtId = slotRequest.getThirdPartyCourtId();
                String requestStartTime = unformatTime(slotRequest.getStartTime());
                String requestEndTime = unformatTime(slotRequest.getEndTime());
                
                // 找到时间匹配的lock_court事件
                AitennisCourtEvent matchedEvent = lockCourtEvents.stream()
                        .filter(event -> event.getCourtId().equals(courtId)
                                && isTimeOverlap(requestStartTime, requestEndTime, event.getStartTime(), event.getEndTime()))
                        .findFirst()
                        .orElse(null);
                
                if (matchedEvent != null && !matchedEvent.getItems().isEmpty()) {
                    String eventId = matchedEvent.getItems().get(0).getId();
                    resultMap.put(slotRequest, eventId);
                    log.info("[aitennis] 槽位 {}-{} 匹配到eventId: {}", slotRequest.getStartTime(), slotRequest.getEndTime(), eventId);
                } else {
                    log.warn("[aitennis] 槽位 {}-{} 未找到匹配的lock_court事件", slotRequest.getStartTime(), slotRequest.getEndTime());
                }
            }
            
            if (resultMap.isEmpty()) {
                log.error("[aitennis] 锁场成功但未能匹配任何eventId");
                String cacheKey = AwayVenueCacheConstants.buildSlotsCacheKey(config.getVenueId(), bookingDate);
                redisService.deleteObject(cacheKey);
                return LockSlotsResult.builder()
                        .bookingIds(new LinkedHashMap<>())
                        .slotPrices(new ArrayList<>())
                        .build();
            }
            log.info("[aitennis] 获取到eventId映射: {}", resultMap.size());
            // 清除缓存
            String cacheKey = AwayVenueCacheConstants.buildSlotsCacheKey(config.getVenueId(), bookingDate);
            redisService.deleteObject(cacheKey);
            log.info("[aitennis] 槽位缓存已清除，批量锁场成功，返回eventId映射和价格: {}", resultMap.size());

            // 构建LockSlotsResult返回
            return LockSlotsResult.builder()
                    .bookingIds(resultMap)
                    .slotPrices(slotPrices)
                    .build();
        } catch (GloboxApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[aitennis] 批量锁场异常: venueId={}", config.getVenueId(), e);
            throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL, e);
        }
    }

    /**
     * 查询当前的场地事件
     */
    private List<AitennisCourtEvent> queryDailyCourtEvents(VenueThirdPartyConfig config, LocalDate date) {
        try {
            String dateStr = date.format(DATE_FORMATTER);
            String url = getApiBaseUrl(config) + API_PATH_GET_DAILY_EVENTS + "?date=" + dateStr;
            log.info("[aitennis] 查询每日事件API - URL: {}", url);
            HttpHeaders headers = buildHeaders(config);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<AitennisResponse<List<AitennisCourtEvent>>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );
            AitennisResponse<List<AitennisCourtEvent>> response = responseEntity.getBody();
            if (response != null) {
                log.info("[aitennis] 查询每日事件API - Response code: {}, 事件数: {}",
                        response.getCode(), response.getData() != null ? response.getData().size() : 0);
            }
            if(!validateResponse(response,config.getVenueId(),"[aitennis]查询当天事件失败")) {
                return null;
            }
            return response.getData();
        } catch (Exception e) {
            log.error("[aitennis] 查询每日事件异常", e);
            return null;
        }
    }

    /**
     * 将时间格式从 "07:00" 转换为 "0700"
     * 特殊处理：23:59:59 或 23:59 转换为 2400（表示24:00，与第三方平台一致）
     */
    private String unformatTime(String time) {
        if (time == null) {
            return null;
        }
        // 23:59:59 或 23:59 统一转换为 2400（第三方平台用2400表示24:00/午夜）
        if ("23:59:59".equals(time) || "23:59".equals(time)) {
            return "2400";
        }
        return time.replace(":", "");
    }


    @Override
    public boolean unlockSlots(VenueThirdPartyConfig config, List<SlotLockRequest> slotRequests) {
        log.info("[aitennis] 批量解锁槽位: venueId={}, slotCount={}",
                config.getVenueId(), slotRequests.size());
        if (slotRequests.isEmpty()) {
            log.warn("[aitennis] slotRequests为空");
            throw new GloboxApplicationException(VenueCode.BOOKING_SLOT_INFO_EMPTY);
        }

        try {
            // 从 slotRequests 获取日期
            LocalDate bookingDate = slotRequests.get(0).getDate();

            // 调用 getDailyCourtEvents 获取数据
            List<AitennisCourtEvent> allEvents = queryDailyCourtEvents(config, bookingDate);
            if (allEvents == null) {
                log.error("[aitennis] 查询events失败");
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }

            // 收集所有涉及到的场地的 lock_court 事件
            Set<String> courtIds = slotRequests.stream()
                    .map(SlotLockRequest::getThirdPartyCourtId)
                    .collect(Collectors.toSet());

            // 获取期望的remark（用于完整匹配，确保只解锁自己锁的槽位）
            String expectedRemark = slotRequests.stream()
                    .map(SlotLockRequest::getThirdPartyRemark)
                    .filter(r -> r != null && !r.isEmpty())
                    .findFirst()
                    .orElse(null);

            if (expectedRemark == null) {
                log.error("[aitennis] 解锁失败: thirdPartyRemark为空, venueId={}", config.getVenueId());
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }

            // 找出这些场地的所有 lock_court 事件，必须完整匹配remark（确保只解锁自己锁的槽位）
            List<AitennisCourtEvent> lockCourtEvents = allEvents.stream()
                    .filter(event -> courtIds.contains(event.getCourtId())
                            && EVENT_TYPE_LOCK_COURT.equals(event.getType())
                            && event.getItems() != null
                            && !event.getItems().isEmpty()
                            && event.getItems().stream().anyMatch(item -> 
                                    item.getData() != null && item.getData().getRemark() != null
                                    && item.getData().getRemark().equals(expectedRemark)))
                    .toList();

            if (lockCourtEvents.isEmpty()) {
                log.error("[aitennis] 找不到remark完整匹配的lock_court事件, expectedRemark={}", expectedRemark);
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }
            
            log.info("[aitennis] remark完整匹配校验通过: expectedRemark={}, 匹配事件数={}", expectedRemark, lockCourtEvents.size());

            // 从remark匹配的lock_court事件中获取sourceId（安全：只从我们自己锁的事件中取）
            String sourceId = lockCourtEvents.get(0).getItems().get(0).getId();
            if (sourceId == null || sourceId.isEmpty()) {
                log.error("[aitennis] 未能从remark匹配的lock_court事件中获取sourceId");
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }
            log.info("[aitennis] 获取sourceId成功: sourceId={}, expectedRemark={}", sourceId, expectedRemark);

            // Away球场只支持全部解锁（部分退款已在UnlockSlotConsumer中拦截）
            callUnlockCourtsApi(config, sourceId, Collections.emptyList(), true);

            // 清除缓存
            String cacheKey = AwayVenueCacheConstants.buildSlotsCacheKey(config.getVenueId(), bookingDate);
            redisService.deleteObject(cacheKey);
            log.info("[aitennis] 槽位缓存已清除，批量解锁成功");

            // 返回 true
            return true;

        } catch (Exception e) {
            log.error("[aitennis] 批量解锁异常: venueId={}", config.getVenueId(), e);
            throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL, e);
        }
    }

    @Override
    public ThirdPartyAuthInfo login(VenueThirdPartyConfig config) {
        log.info("[aitennis] 开始登录: venueId={}, username={}",
                config.getVenueId(), config.getUsername());
        try {
            // 构建登录URL（phone和password作为query params）
            String apiBaseUrl = getApiBaseUrl(config);
            String loginUrl = apiBaseUrl + API_PATH_LOGIN
                    + "?phone=" + config.getUsername()
                    + "&password=" + config.getPassword();
            log.info("[aitennis] 调用登录API - URL: {}", loginUrl);
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            // 发送POST请求
            ResponseEntity<AitennisResponse<AitennisLoginResponse>> responseEntity = restTemplate.exchange(
                    loginUrl,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );
            // 解析响应
            AitennisResponse<AitennisLoginResponse> response = responseEntity.getBody();
            if (response != null) {
                log.info("[aitennis] 调用登录API - Response code: {}, 登录成功: {}",
                        response.getCode(), response.getCode() == 0);
            }
            if (response == null || response.getData() == null) {
                log.error("[aitennis] 登录失败: 响应为空, venueId={}", config.getVenueId());
                return null;
            }
            if (response.getCode() == null || response.getCode() != 0) {
                log.error("[aitennis] 登录失败: code={}, msg={}, venueId={}",
                        response.getCode(), response.getMsg(), config.getVenueId());
                return null;
            }
            AitennisLoginResponse loginData = response.getData();
            String token = loginData.getToken();
            if (token == null || token.trim().isEmpty()) {
                log.error("[aitennis] 登录失败: Token为空, venueId={}", config.getVenueId());
                return null;
            }
            // 去掉 "Bearer " 前缀（如果存在），因为使用时会重新添加
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            // 构建认证信息
            ThirdPartyAuthInfo authInfo = ThirdPartyAuthInfo.builder()
                    .token(token)
                    .adminId(null)
                    .build();
            log.info("[aitennis] 登录成功: venueId={}",
                    config.getVenueId());
            return authInfo;
        } catch (Exception e) {
            log.error("[aitennis] 登录异常: venueId={}", config.getVenueId(), e);
            return null;
        }
    }

    @Override
    public String getPlatformCode() {
        return PLATFORM_CODE;
    }

    /**
     * 构建HTTP请求头
     * 统一处理authorization、stadiumId、client和contentType
     */
    private HttpHeaders buildHeaders(VenueThirdPartyConfig config) {
        ThirdPartyAuthInfo authInfo = tokenService.getAuthInfo(config, this);
        String token = authInfo.getToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", "Bearer " + token);
        headers.set("stadiumId", config.getThirdPartyVenueId());
        headers.set("client", "Web");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * 调用 lockCourts API 进行锁定
     */
    private void callLockCourtsApi(VenueThirdPartyConfig config, AitennisLockRequest lockRequest) {
        try {
            HttpHeaders headers = buildHeaders(config);
            HttpEntity<AitennisLockRequest> requestEntity = new HttpEntity<>(lockRequest, headers);
            String lockUrl = getApiBaseUrl(config) + API_PATH_LOCK_COURTS;
            log.info("[aitennis] 调用锁场API - URL: {}", lockUrl);
            log.info("[aitennis] 调用锁场API - Request: {}", JSON.toJSONString(lockRequest));
            ResponseEntity<String> lockResponseEntity = restTemplate.postForEntity(
                    lockUrl,
                    requestEntity,
                    String.class
            );
            log.info("[aitennis] 调用锁场API - Response: {}", lockResponseEntity.getBody());
            String lockResponseBody = lockResponseEntity.getBody();
            if (lockResponseBody == null) {
                log.error("[aitennis] 批量锁场失败: 响应为空");
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }
            AitennisResponse<?> lockResponse = JSON.parseObject(lockResponseBody, AitennisResponse.class);
            if (lockResponse == null || lockResponse.getCode() == null || lockResponse.getCode() != 0) {
                log.error("[aitennis] 批量锁场失败: code={}, msg={}",
                        lockResponse != null ? lockResponse.getCode() : null,
                        lockResponse != null ? lockResponse.getMsg() : null);
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }
            log.info("[aitennis] 锁场API调用成功");
        } catch (GloboxApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[aitennis] 调用lockCourts API异常", e);
            throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL, e);
        }
    }

    /**
     * 调用 unlockCourts API 进行解锁
     *
     * @param config 配置
     * @param sourceId 源ID
     * @param unlockCourtIds 要解锁的item ID列表
     * @param isUnlockAll 是否全部解锁
     */
    private void callUnlockCourtsApi(VenueThirdPartyConfig config, String sourceId, List<String> unlockCourtIds,
                                     boolean isUnlockAll) {
        try {
            // 根据是否全部解锁来构建请求体
            JSONObject unlockRequestBody = new JSONObject();
            if (isUnlockAll) {
                // 全部解锁：包含 is_all:true
                unlockRequestBody.put("is_all", true);
            }
            // item 在全部解锁和部分解锁时都需要
            unlockRequestBody.put("item", unlockCourtIds);

            HttpHeaders headers = buildHeaders(config);
            HttpEntity<String> requestEntity = new HttpEntity<>(unlockRequestBody.toJSONString(), headers);
            String unlockUrl = getApiBaseUrl(config) + API_PATH_LOCK_COURTS + "/" + sourceId;
            log.info("[aitennis] 调用解锁API - URL: {}", unlockUrl);
            log.info("[aitennis] 调用解锁API - Request: {}", unlockRequestBody.toJSONString());
            ResponseEntity<String> unlockResponseEntity = restTemplate.exchange(
                    unlockUrl,
                    HttpMethod.DELETE,
                    requestEntity,
                    String.class
            );
            log.info("[aitennis] 调用解锁API - Response: {}", unlockResponseEntity.getBody());
            String unlockResponseBody = unlockResponseEntity.getBody();
            if (unlockResponseBody == null) {
                log.error("[aitennis] 批量解锁失败: 响应为空");
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }
            AitennisResponse<?> unlockResponse = JSON.parseObject(unlockResponseBody, AitennisResponse.class);
            if (unlockResponse == null || unlockResponse.getCode() == null || unlockResponse.getCode() != 0) {
                log.error("[aitennis] 批量解锁失败: code={}, msg={}",
                        unlockResponse != null ? unlockResponse.getCode() : null,
                        unlockResponse != null ? unlockResponse.getMsg() : null);
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }
            log.info("[aitennis] 解锁API调用成功");
        } catch (GloboxApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[aitennis] 调用unlockCourts API异常", e);
            throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL, e);
        }
    }

    /**
     * 验证API响应是否成功
     * 检查response和code，返回true表示成功
     */
    private boolean validateResponse(AitennisResponse<?> response, Long venueId, String operationName) {
        if (response == null) {
            log.error("[aitennis] {}失败: 响应为空, venueId={}", operationName, venueId);
            return false;
        }
        if (response.getCode() == null || response.getCode() != 0) {
            log.error("[aitennis] {}失败: code={}, msg={}, venueId={}",
                    operationName, response.getCode(), response.getMsg(), venueId);
            return false;
        }
        return true;
    }

    /**
     * 计算Away球场的价格
     * 通过调用queryDailyCourtEvents获取当天的事件数据，从中提取价格信息
     *
     * @param config 第三方平台配置
     * @param date 预订日期
     * @return 槽位价格列表
     */
    @Override
    public List<AwaySlotPrice> calculatePricing(VenueThirdPartyConfig config, LocalDate date) {
        if (date == null) {
            log.error("[aitennis] 预订日期为空，无法计算价格");
            throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
        }
        // 调用queryDailyCourtEvents获取当天的所有事件数据
        List<AitennisCourtEvent> events = queryDailyCourtEvents(config, date);
        if (events == null || events.isEmpty()) {
            log.error("[aitennis] 未获取到事件数据，无法计算价格");
            throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
        }
        return calculatePricingInterior(date,events);
    }


    /**
     * 计算价格的重载方法,用于内部调用计算价格逻辑
     * @param date 预定日期
     * @param events 预先获取的当天事件
     */
    private List<AwaySlotPrice> calculatePricingInterior(LocalDate date,List<AitennisCourtEvent> events) {
        if (date == null) {
            log.error("[aitennis] 预订日期为空，无法计算价格");
            throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
        }
        try {
            if (events == null || events.isEmpty()) {
                log.error("[aitennis] 未获取到事件数据，无法计算价格");
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }
            // 从事件数据中提取价格信息，构建AwaySlotPrice列表（需要拆分成30分钟槽位）
            List<AwaySlotPrice> slotPrices = events.stream()
                    // 只处理可租赁的事件（type为rent_price）
                    .filter(event -> EVENT_TYPE_RENT_PRICE.equals(event.getType()))
                    .filter(event -> event.getItems() != null && !event.getItems().isEmpty())
                    .flatMap(event -> {
                        try {
                            // 从事件的第一个item中提取价格
                            AitennisEventItem firstItem = event.getItems().get(0);
                            BigDecimal price = BigDecimal.ZERO;
                            if (firstItem.getData() != null && firstItem.getData().getPrice() != null) {
                                try {
                                    price = new BigDecimal(firstItem.getData().getPrice());
                                } catch (NumberFormatException e) {
                                    log.warn("[aitennis] 价格格式错误: {}", firstItem.getData().getPrice());
                                    return Stream.empty();
                                }
                            }

                            // 解析开始和结束时间，处理24:00特殊情况
                            String startTimeStr = formatTime(event.getStartTime());
                            String endTimeStr = formatTime(event.getEndTime());
                            LocalTime startTime = LocalTime.parse(startTimeStr);
                            // 24:00需要特殊处理，转换为23:59
                            boolean isEndTime2400 = "24:00".equals(endTimeStr);
                            LocalTime endTime = isEndTime2400 ? LocalTime.of(23, 59) : LocalTime.parse(endTimeStr);

                            // 将槽位拆分成30分钟槽位，按实际拆分数量平分价格
                            List<TimeSlotSplitUtil.TimeSlot> splitSlots = TimeSlotSplitUtil.splitTimeSlots(startTime, endTime);
                            int halfHourCount = splitSlots.size();
                            if (halfHourCount <= 0) {
                                halfHourCount = 1;
                            }
                            BigDecimal halfHourPrice = price.divide(BigDecimal.valueOf(halfHourCount), 2, RoundingMode.HALF_UP);

                            // 每个拆分后的槽位使用平分后的价格
                            List<AwaySlotPrice> splitPrices = new ArrayList<>();
                            for (TimeSlotSplitUtil.TimeSlot splitSlot : splitSlots) {
                                splitPrices.add(AwaySlotPrice.builder()
                                        .startTime(splitSlot.getStartTime())
                                        .price(halfHourPrice)
                                        .thirdPartyCourtId(event.getCourtId())
                                        .build());
                            }
                            return splitPrices.stream();
                        } catch (Exception e) {
                            log.warn("[aitennis] 时间解析失败: startTime={}, endTime={}",
                                    event.getStartTime(), event.getEndTime(), e);
                            return Stream.empty();
                        }
                    })
                    .filter(Objects::nonNull)
                    .filter(slotPrice -> slotPrice.getPrice() != null && slotPrice.getPrice().compareTo(BigDecimal.ZERO) > 0)
                    .collect(Collectors.toList());

            if (slotPrices.isEmpty()) {
                log.error("[aitennis] 未获取到有效的槽位价格");
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }
            log.info("[aitennis] 价格计算完成 - 日期: {}, 槽位数: {}", date, slotPrices.size());
            return slotPrices;
        } catch (GloboxApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[aitennis] 计算价格异常", e);
            throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL, e);
        }
    }

    /**
     * 判断两个时间段是否有重叠
     * 时间格式：HHmm（如"2100"表示21:00）
     *
     * @param start1 时间段1开始时间
     * @param end1   时间段1结束时间
     * @param start2 时间段2开始时间
     * @param end2   时间段2结束时间
     * @return true=有重叠，false=无重叠
     */
    private boolean isTimeOverlap(String start1, String end1, String start2, String end2) {
        // 两个时间段有重叠的条件：start1 < end2 && start2 < end1
        // 字符串比较可以直接用于HHmm格式（如"2100" < "2200"）
        return start1.compareTo(end2) < 0 && start2.compareTo(end1) < 0;
    }
}
