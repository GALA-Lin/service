package com.unlimited.sports.globox.venue.adapter.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.unlimited.sports.globox.model.venue.entity.venues.ThirdPartyPlatform;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueThirdPartyConfig;
import com.unlimited.sports.globox.service.RedisService;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.VenueCode;
import com.unlimited.sports.globox.venue.adapter.ThirdPartyPlatformAdapter;
import com.unlimited.sports.globox.venue.adapter.constant.AwayVenueCacheConstants;
import com.unlimited.sports.globox.venue.adapter.dto.*;
import com.unlimited.sports.globox.venue.adapter.dto.changxiaoer.ChangxiaoerLockSlotRequest;
import com.unlimited.sports.globox.venue.adapter.dto.changxiaoer.ChangxiaoerLoginRequest;
import com.unlimited.sports.globox.venue.adapter.dto.changxiaoer.ChangxiaoerPlace;
import com.unlimited.sports.globox.venue.adapter.dto.changxiaoer.ChangxiaoerResponse;
import com.unlimited.sports.globox.venue.adapter.dto.changxiaoer.ChangxiaoerUnlockSlotRequest;
import com.unlimited.sports.globox.venue.config.AwayConfig;
import com.unlimited.sports.globox.venue.mapper.ThirdPartyPlatformMapper;
import com.unlimited.sports.globox.venue.service.IThirdPartyTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 场小二平台适配器
 */
@Slf4j
@Component
public class ChangxiaoerAdapter implements ThirdPartyPlatformAdapter {

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


    @Autowired
    private AwayConfig awayConfig;

    private static final String PLATFORM_CODE = "changxiaoer";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // API路径常量
    private static final String API_PATH_LOGIN = "/admin/merchantAdminLogin";
    private static final String API_PATH_TIMETABLES = "/merchants-management/spaces/%s/timetables";
    private static final String API_PATH_BOOKING_ORDERS = "/merchants-management/spaces/%s/booking-orders";
    private static final String API_PATH_DELETE_ORDER = "/merchants-management/spaces/%s/booking-orders/%s";
    private static final String API_PATH_PERIODS_USAGES = "/merchants-management/spaces/%s/timetables/periods/usages";
    private static final String API_PATH_BATCHES = "/merchants-management/spaces/%s/booking-orders/batches/%s";

    // 查询参数常量
    private static final String LOCK_SITE_PATTERN_TYPE = "1";
    private static final boolean FINISHED = false;  // 查询进行中的锁场记录
    private static final int PAGE_NUM = 1;
    private static final int PAGE_SIZE = 20;

    // 锁场相关常量
    private static final String PRICE_MODE_CUSTOM = "CUSTOM";
    private static final String PRICE_MODE_NONE = "NONE";
    private static final String PAYMENT_METHOD_CASH = "CASH";
    private static final boolean PAID_STATUS = true;  // 锁场时的支付状态
    private static final String COUNTRY_CODE = "+86";


    // 解锁相关常量
    private static final boolean REFUND_OR_NOT = true;

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
            log.info("[changxiaoer] 平台API地址已加载: {}", platformBaseApiUrl);
        } else {
            log.warn("[changxiaoer] 未找到平台配置，platformCode={}", PLATFORM_CODE);
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
        log.info("[changxiaoer] 查询槽位: venueId={}, thirdPartyVenueId={}, date={}",
                config.getVenueId(), config.getThirdPartyVenueId(), date);

        // 先从Redis缓存获取
        String cacheKey = AwayVenueCacheConstants.buildSlotsCacheKey(config.getVenueId(), date);
        List<ThirdPartyCourtSlotDto> cachedSlots = redisService.getCacheObject(cacheKey,
                new TypeReference<>() {
                });

        if (cachedSlots != null && !cachedSlots.isEmpty()) {
            log.info("[changxiaoer] 从缓存获取槽位成功: venueId={}, date={}, 场地数: {}",
                    config.getVenueId(), date, cachedSlots.size());
            return cachedSlots;
        }
        try {
            //  缓存未命中，调用API获取槽位
            List<ThirdPartyCourtSlotDto> slots = querySlotsFromAPI(config, date);
            // 将结果缓存
            if (slots != null && !slots.isEmpty()) {
                redisService.setCacheObject(cacheKey, slots, AwayVenueCacheConstants.SLOTS_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                log.info("[changxiaoer] 槽位已缓存: venueId={}, date={}, ttl={}min",
                        config.getVenueId(), date, AwayVenueCacheConstants.SLOTS_CACHE_TTL_MINUTES);
            }
            return slots;
        } catch (Exception e) {
            log.error("[changxiaoer] 查询槽位异常: venueId={}", config.getVenueId(), e);
            return null;
        }
    }

    /**
     * 从API查询槽位
     */
    private List<ThirdPartyCourtSlotDto> querySlotsFromAPI(VenueThirdPartyConfig config, LocalDate date) {
        try {
            // 获取认证信息
            ThirdPartyAuthInfo authInfo = tokenService.getAuthInfo(config, this);
            String token = authInfo.getToken();
            String adminId = authInfo.getAdminId();
            if (adminId == null) {
                log.error("[changxiaoer] adminId为空: venueId={}", config.getVenueId());
                return null;
            }
            // 格式化日期
            String dateStr = date.format(DATE_FORMATTER);
            // 构建URL和查询参数
            String spaceId = config.getThirdPartyVenueId();
            String apiBaseUrl = getApiBaseUrl(config);
            String url = apiBaseUrl + String.format(API_PATH_TIMETABLES, spaceId);
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("basicsId", spaceId)
                    .queryParam("lockSitePatternType", LOCK_SITE_PATTERN_TYPE)
                    .queryParam("adminId", adminId)
                    .queryParam("usage_start_date", dateStr)
                    .queryParam("usage_end_date", dateStr)
                    .queryParam("lockBeginDay", dateStr);
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            // 发送GET请求
            ResponseEntity<ChangxiaoerResponse<List<ChangxiaoerPlace>>> responseEntity = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // 解析响应
            ChangxiaoerResponse<List<ChangxiaoerPlace>> response = responseEntity.getBody();
            if (response == null || response.getData() == null) {
                log.error("[changxiaoer] 查询槽位失败: 响应为空, venueId={}", config.getVenueId());
                return null;
            }
            if (response.getFlag() == null || !response.getFlag()) {
                log.error("[changxiaoer] 查询槽位失败: flag=false, errorMessage={}, venueId={}",
                        response.getErrorMessage(), config.getVenueId());
                return null;
            }

            List<ChangxiaoerPlace> places = response.getData();
            log.info("[changxiaoer] 查询槽位成功: venueId={}, 场地数量={}", config.getVenueId(), places.size());

            // 7. 转换为统一格式并返回
            return convertPlacesToDto(places);

        } catch (Exception e) {
            log.error("[changxiaoer] 查询槽位异常: venueId={}", config.getVenueId(), e);
            return null;
        }
    }

    /**
     * 将场小二的Place数据转换为统一的DTO格式
     */
    private List<ThirdPartyCourtSlotDto> convertPlacesToDto(List<ChangxiaoerPlace> places) {
        return places.stream()
                .map(place -> ThirdPartyCourtSlotDto.builder()
                        .thirdPartyCourtId(place.getPlaceId().toString())
                        .courtName(place.getPlaceName())
                        .slots(place.getTimetables().stream()
                                .map(timetable -> ThirdPartySlotDto.builder()
                                        .startTime(timetable.getStartTime())
                                        .endTime(timetable.getEndTime())
                                        .available(timetable.getBookable())
                                        .price(timetable.getPrice() != null ? BigDecimal.valueOf(timetable.getPrice()) : BigDecimal.ZERO)
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }


    // 批量锁定槽位（一次API调用）
    @Override
    public LockSlotsResult lockSlots(VenueThirdPartyConfig config, List<SlotLockRequest> slotRequests, String remark) {
        log.info("[changxiaoer] 批量锁定槽位: venueId={}, slotCount={}, remark={}",
                config.getVenueId(), slotRequests.size(), remark);
        try {
            // 获取认证信息
            ThirdPartyAuthInfo authInfo = tokenService.getAuthInfo(config, this);
            String token = authInfo.getToken();
            //获取第一个槽位的日期（假设所有槽位同一天）
            LocalDate firstDate = slotRequests.get(0).getDate();
            String dateStr = firstDate.format(DATE_FORMATTER);
            String dayOfWeek = firstDate.getDayOfWeek().toString();
            //  构建通用字段
            ChangxiaoerLockSlotRequest.DateRange dateRange = ChangxiaoerLockSlotRequest.DateRange.builder()
                    .startDate(dateStr)
                    .endDate(dateStr)
                    .build();

            ChangxiaoerLockSlotRequest.PhoneNumber phoneNumber = ChangxiaoerLockSlotRequest.PhoneNumber.builder()
                    .phoneNumber(awayConfig.getBooking().getBookingUserPhone()) // 使用配置中的手机号
                    .countryCode(COUNTRY_CODE)
                    .build();

            ChangxiaoerLockSlotRequest.Booker booker = ChangxiaoerLockSlotRequest.Booker.builder()
                    .phoneNumber(phoneNumber)
                    .bookerName(awayConfig.getBooking().getBookingUserName()) // 使用配置中的名字
                    .build();

            ChangxiaoerLockSlotRequest.PaymentInfo paymentInfo = ChangxiaoerLockSlotRequest.PaymentInfo.builder()
                    .paymentMethod(PAYMENT_METHOD_CASH) // 使用现金支付
                    .paid(PAID_STATUS)
                    .build();

            // 计算价格
            List<AwaySlotPrice> slotPrices = calculatePricing(config, firstDate);
            if (slotPrices == null || slotPrices.isEmpty()) {
                log.error("[changxiaoer] 无法计算价格: venueId={}, date={}", config.getVenueId(), firstDate);
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }
            // 构建场地->时间->价格的映射表
            Map<String, Integer> priceMap = new HashMap<>();
            slotPrices.stream()
                    .map(slotPrice -> new AbstractMap.SimpleEntry<>(
                            slotPrice.getThirdPartyCourtId() + "-" + slotPrice.getStartTime().toString() + "-" + slotPrice.getStartTime().plusMinutes(30).toString(),
                            slotPrice.getPrice().intValue()
                    ))
                    .forEach(entry -> priceMap.put(entry.getKey(), entry.getValue()));

            // 构建placeOrders数组
            List<ChangxiaoerLockSlotRequest.PlaceOrder> placeOrders = new ArrayList<>();
            for (SlotLockRequest slot : slotRequests) {
                String courtId = slot.getThirdPartyCourtId();
                ChangxiaoerLockSlotRequest.TimeRange timeRange = ChangxiaoerLockSlotRequest.TimeRange.builder()
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .build();

                // 从已计算的价格中查询该时段的价格
                String slotKey = courtId + "-" + slot.getStartTime() + "-" + slot.getEndTime();
                Integer price = priceMap.getOrDefault(slotKey, 0);

                ChangxiaoerLockSlotRequest.PlaceOrder placeOrder = ChangxiaoerLockSlotRequest.PlaceOrder.builder()
                        .placeId(Long.parseLong(courtId))
                        .timeRange(timeRange)
                        .price(price)
                        .surchargeItems(new ArrayList<>())
                        .build();

                placeOrders.add(placeOrder);
                log.info("[changxiaoer] 添加placeOrder: courtId={}, time={}-{}, price={}",
                        courtId, slot.getStartTime(), slot.getEndTime(), price);
            }

            // 构建锁场请求
            ChangxiaoerLockSlotRequest lockRequest = ChangxiaoerLockSlotRequest.builder()
                    .dateRange(dateRange)
                    .dayOfWeek(dayOfWeek)
                    .booker(booker)
                    .priceMode(PRICE_MODE_CUSTOM)
                    .placeOrders(placeOrders)
                    .paymentInfo(paymentInfo)
                    .remark(remark != null ? remark : "")
                    .adminId(authInfo.getAdminId())
                    .build();

            //  设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ChangxiaoerLockSlotRequest> requestEntity = new HttpEntity<>(lockRequest, headers);

            // 构建URL并发送POST请求
            String apiBaseUrl = getApiBaseUrl(config);
            String url = apiBaseUrl + String.format(API_PATH_BOOKING_ORDERS, config.getThirdPartyVenueId());

            ResponseEntity<ChangxiaoerResponse<Object>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    });

            //  解析响应
            ChangxiaoerResponse<Object> response = responseEntity.getBody();
            if (response == null || response.getFlag() == null || !response.getFlag()) {
                log.error("[changxiaoer] 批量锁场失败: errorMessage={}, venueId={}",
                        response != null ? response.getErrorMessage() : "响应为空", config.getVenueId());
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }

            // 锁场成功，清除槽位缓存
            String cacheKey = AwayVenueCacheConstants.buildSlotsCacheKey(config.getVenueId(), firstDate);
            redisService.deleteObject(cacheKey);
            log.info("[changxiaoer] 槽位缓存已清除: venueId={}, date={}", config.getVenueId(), firstDate);

            // 锁场成功后，查询batchNo（通过第一个槽位的时间段）
            String batchNo = queryBatchNo(config, slotRequests.get(0).getThirdPartyCourtId(),
                    slotRequests.get(0), dateStr, token, authInfo.getAdminId());

            if (batchNo == null || batchNo.isEmpty()) {
                log.error("[changxiaoer] 锁场成功但查询batchNo失败");
                // 返回失败结果，不抛异常
                return LockSlotsResult.builder()
                        .bookingIds(new LinkedHashMap<>())
                        .slotPrices(new ArrayList<>())
                        .build();
            }

            log.info("[changxiaoer] 获取到batchNo: {}", batchNo);

            // 构建返回的Map，每个slotRequest都对应同一个batchNo
            Map<SlotLockRequest, String> resultMap = slotRequests.stream()
                            .collect(Collectors.toMap(
                                    slotRequest -> slotRequest,
                                    slotRequest -> batchNo
                            ));

            log.info("[changxiaoer] 批量锁场成功: venueId={}, 返回batchNo映射和价格: {}", config.getVenueId(), resultMap.size());

            // 构建LockSlotsResult返回
            return LockSlotsResult.builder()
                    .bookingIds(resultMap)
                    .slotPrices(slotPrices)
                    .build();

        } catch (GloboxApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[changxiaoer] 批量锁场异常: venueId={}", config.getVenueId(), e);
            throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL, e);
        }
    }


    // 批量解锁槽位（根据时间段信息查询batchId和orderId）
    @Override
    public boolean unlockSlots(VenueThirdPartyConfig config, List<SlotLockRequest> slotRequests) {
        log.info("[changxiaoer] 批量解锁槽位: venueId={}, slotCount={}",
                config.getVenueId(), slotRequests.size());
        try {
            // 参数校验
            if (slotRequests.isEmpty()) {
                log.warn("[changxiaoer] 解锁参数为空，无法进行解锁");
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }

            // 获取认证信息
            ThirdPartyAuthInfo authInfo = tokenService.getAuthInfo(config, this);
            String token = authInfo.getToken();
            String adminId = authInfo.getAdminId();

            // 获取预订日期（假设所有槽位同一天）
            LocalDate bookingDate = slotRequests.get(0).getDate();
            String dateStr = bookingDate.format(DATE_FORMATTER);

            // 1. 查询batchNo
            SlotLockRequest firstSlot = slotRequests.get(0);
            String batchNo = queryBatchNo(config, firstSlot.getThirdPartyCourtId(), firstSlot, dateStr, token, adminId);
            if (batchNo == null || batchNo.isEmpty()) {
                log.error("[changxiaoer] 无法找到对应的batchNo");
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }
            log.info("[changxiaoer] 找到batchNo: {}", batchNo);

            // 2. 查询orderNo
            String orderNo = queryOrderNo(config, batchNo, token, adminId);
            if (orderNo == null || orderNo.isEmpty()) {
                log.error("[changxiaoer] 无法从batchNo查询到orderNo: batchNo={}", batchNo);
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }
            log.info("[changxiaoer] 找到orderNo: {}", orderNo);

            // 3. 查询订单详情，获取所有periodOrders（包含detailId和槽位信息）
            Map<String, String> slotToDetailIdMap = queryOrderDetails(config, batchNo, token, adminId);
            if (slotToDetailIdMap.isEmpty()) {
                log.error("[changxiaoer] 无法查询到订单详情");
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }

            // 4. 根据slotRequests筛选要删除的detailIds
            List<String> detailIdsToDelete = new ArrayList<>();
            for (SlotLockRequest slot : slotRequests) {
                String slotKey = slot.getThirdPartyCourtId() + "|" + slot.getStartTime() + "|" + slot.getEndTime();
                String detailId = slotToDetailIdMap.get(slotKey);
                if (detailId != null) {
                    detailIdsToDelete.add(detailId);
                } else {
                    log.warn("[changxiaoer] 未找到槽位对应的detailId: placeId={}, time={}-{}",
                            slot.getThirdPartyCourtId(), slot.getStartTime(), slot.getEndTime());
                }
            }

            if (detailIdsToDelete.isEmpty()) {
                log.error("[changxiaoer] 未找到任何要删除的detailId");
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }

            // 5. 判断是全部解锁还是部分解锁
            boolean isUnlockAll = (detailIdsToDelete.size() == slotToDetailIdMap.size());
            if (isUnlockAll) {
                log.info("[changxiaoer] 执行全部解锁: 解锁全部{}个槽位", detailIdsToDelete.size());
                // 全部解锁：detailIds传空数组
                deleteOrder(config, orderNo, new ArrayList<>(), adminId, token);
            } else {
                log.info("[changxiaoer] 执行部分解锁: 解锁{}个槽位（共{}个）",
                        detailIdsToDelete.size(), slotToDetailIdMap.size());
                // 部分解锁：detailIds传具体的ID列表
                deleteOrder(config, orderNo, detailIdsToDelete, adminId, token);
            }

            // 6. 清除槽位缓存
            String cacheKey = AwayVenueCacheConstants.buildSlotsCacheKey(config.getVenueId(), bookingDate);
            redisService.deleteObject(cacheKey);
            log.info("[changxiaoer] 槽位缓存已清除: venueId={}, bookingDate={}", config.getVenueId(), bookingDate);

            log.info("[changxiaoer] 批量解锁成功: orderNo={}, 解锁{}个槽位", orderNo, detailIdsToDelete.size());
            return true;

        } catch (GloboxApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[changxiaoer] 批量解锁异常: venueId={}", config.getVenueId(), e);
            throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL, e);
        }
    }

    /**
     * 查询订单详情，获取所有periodOrders及其detailId
     *
     * @param config 配置
     * @param batchNo 批次号
     * @param token Token
     * @param adminId 管理员ID
     * @return Map: slotKey(placeId|startTime|endTime) -> detailId
     */
    private Map<String, String> queryOrderDetails(VenueThirdPartyConfig config, String batchNo,
                                                   String token, String adminId) {
        try {
            String apiBaseUrl = getApiBaseUrl(config);
            String spaceId = config.getThirdPartyVenueId();

            // 调用查询批次详情接口
            String batchUrl = apiBaseUrl + String.format(API_PATH_BATCHES, spaceId, batchNo)
                    + "?adminId=" + adminId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            log.info("[changxiaoer] 查询订单详情API - URL: {}", batchUrl);
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    batchUrl,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            String responseBody = responseEntity.getBody();
            log.info("[changxiaoer] 查询订单详情API - Response code: {}", responseEntity.getStatusCode());
            if (responseBody == null) {
                log.error("[changxiaoer] 查询订单详情失败: 响应为空");
                return Collections.emptyMap();
            }

            JSONObject jsonResponse = JSON.parseObject(responseBody);
            if (!jsonResponse.getBoolean("flag")) {
                log.error("[changxiaoer] 查询订单详情失败: {}", jsonResponse.getString("errorMessage"));
                return Collections.emptyMap();
            }

            JSONObject data = jsonResponse.getJSONObject("data");
            if (data == null || data.getJSONArray("orderInfos") == null) {
                log.error("[changxiaoer] 订单详情为空");
                return Collections.emptyMap();
            }

            // 从orderInfos中提取所有periodOrders
            Map<String, String> slotToDetailIdMap = new HashMap<>();
            JSONArray orderInfos = data.getJSONArray("orderInfos");
            for (Object orderInfoObj : orderInfos) {
                JSONObject orderInfo = (JSONObject) orderInfoObj;
                JSONArray periodOrders = orderInfo.getJSONArray("periodOrders");
                if (periodOrders == null) continue;

                for (Object periodOrderObj : periodOrders) {
                    JSONObject periodOrder = (JSONObject) periodOrderObj;
                    String detailId = periodOrder.getString("detailId");
                    JSONObject place = periodOrder.getJSONObject("place");
                    JSONObject timeRange = periodOrder.getJSONObject("timeRange");

                    if (detailId != null && place != null && timeRange != null) {
                        Long placeId = place.getLong("placeId");
                        String startTime = timeRange.getString("startTime");
                        String endTime = timeRange.getString("endTime");

                        String slotKey = placeId + "|" + startTime + "|" + endTime;
                        slotToDetailIdMap.put(slotKey, detailId);
                    }
                }
            }

            log.info("[changxiaoer] 查询到{}个periodOrders", slotToDetailIdMap.size());
            return slotToDetailIdMap;

        } catch (Exception e) {
            log.error("[changxiaoer] 查询订单详情异常", e);
            return Collections.emptyMap();
        }
    }


    /**
     * 查询锁场记录，根据时间段匹配batchNo
     */
    private String queryBatchNo(VenueThirdPartyConfig config, String thirdPartyCourtId,
                                SlotLockRequest slotRequest, String dateStr, String token, String adminId) {
        try {
            String apiBaseUrl = getApiBaseUrl(config);
            String baseUrl = apiBaseUrl + String.format(API_PATH_PERIODS_USAGES, config.getThirdPartyVenueId());
            String listUrl = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("finished", FINISHED)
                    .queryParam("pageNum", PAGE_NUM)
                    .queryParam("pageSize", PAGE_SIZE)
                    .queryParam("adminId", adminId)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            log.info("[changxiaoer] 查询锁场记录API - URL: {}", listUrl);
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    listUrl,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );
            String responseBody = responseEntity.getBody();
            if (responseBody == null) {
                log.error("[changxiaoer] 查询锁场记录失败: 响应为空");
                return null;
            }
            JSONObject jsonResponse = JSON.parseObject(responseBody);
            if (jsonResponse.getBoolean("flag") == null || !jsonResponse.getBoolean("flag")) {
                log.error("[changxiaoer] 查询锁场记录失败: {}", jsonResponse.getString("errorMessage"));
                return null;
            }
            JSONObject data = jsonResponse.getJSONObject("data");
            if (data == null || data.getJSONArray("list") == null) {
                log.error("[changxiaoer] 查询锁场记录失败: data或list为空");
                return null;
            }
            // 匹配时间段找出batchNo
            String timeRange = slotRequest.getStartTime() + "-" + slotRequest.getEndTime();

            return data.getJSONArray("list").stream()
                    .map(item -> (JSONObject) item)
                    .filter(record -> record.getJSONArray("placePeriods") != null)
                    .filter(record -> {
                        JSONArray placePeriods = record.getJSONArray("placePeriods");
                        return placePeriods.stream()
                                .map(periodObj -> (JSONObject) periodObj)
                                .anyMatch(period -> {
                                    JSONObject timeRangeObj = period.getJSONObject("timeRange");
                                    JSONObject place = period.getJSONObject("place");
                                    String usageDate = period.getString("usageDate");

                                    if (timeRangeObj == null || place == null) return false;
                                    if (!timeRangeObj.containsKey("startTime") || !timeRangeObj.containsKey("endTime"))
                                        return false;

                                    String startTime = timeRangeObj.getString("startTime");
                                    String endTime = timeRangeObj.getString("endTime");
                                    String rangeStr = startTime + "-" + endTime;

                                    return thirdPartyCourtId.equals(place.getString("placeId"))
                                            && rangeStr.equals(timeRange)
                                            && dateStr.equals(usageDate);
                                });
                    })
                    .map(record -> record.getString("batchNo"))
                    .filter(batchNo -> batchNo != null && !batchNo.isEmpty())
                    .findFirst()
                    .orElseGet(() -> {
                        log.warn("[changxiaoer] 未找到时间段对应的batchNo: courtId={}, timeRange={}, date={}",
                                thirdPartyCourtId, timeRange, dateStr);
                        return null;
                    });

        } catch (Exception e) {
            log.error("[changxiaoer] 查询batchNo异常", e);
            return null;
        }
    }

    /**
     * 通过batchNo查询订单详情，获取orderId
     */
    private String queryOrderNo(VenueThirdPartyConfig config, String batchNo, String token, String adminId) {
        try {
            String apiBaseUrl = getApiBaseUrl(config);
            String batchUrl = apiBaseUrl + String.format(API_PATH_BATCHES, config.getThirdPartyVenueId(), batchNo)
                    + "?adminId=" + adminId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            log.info("[changxiaoer] 查询订单详情API - URL: {}", batchUrl);
            ResponseEntity<String> batchResponseEntity = restTemplate.exchange(
                    batchUrl,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            String batchResponseBody = batchResponseEntity.getBody();
            log.info("[changxiaoer] 查询订单详情API - Response: {}", batchResponseBody);
            if (batchResponseBody == null) {
                log.error("[changxiaoer] 查询订单详情失败: 响应为空, batchNo={}", batchNo);
                return null;
            }

            JSONObject batchJsonResponse = JSON.parseObject(batchResponseBody);
            if (batchJsonResponse.getBoolean("flag") == null || !batchJsonResponse.getBoolean("flag")) {
                log.error("[changxiaoer] 查询订单详情失败: {}", batchJsonResponse.getString("errorMessage"));
                return null;
            }

            JSONObject batchData = batchJsonResponse.getJSONObject("data");
            if (batchData == null || batchData.getJSONArray("orderInfos") == null
                    || batchData.getJSONArray("orderInfos").isEmpty()) {
                log.error("[changxiaoer] 订单详情中没有orderInfos, batchNo={}", batchNo);
                return null;
            }

            JSONObject orderInfo = batchData.getJSONArray("orderInfos").getJSONObject(0);
            String orderId = orderInfo.getString("orderNo");

            if (orderId == null || orderId.isEmpty()) {
                log.error("[changxiaoer] 订单详情中没有orderNo, batchNo={}", batchNo);
                return null;
            }

            return orderId;

        } catch (Exception e) {
            log.error("[changxiaoer] 查询orderId异常: batchNo={}", batchNo, e);
            return null;
        }
    }

    /**
     * 删除订单（解锁）
     */
    private void deleteOrder(VenueThirdPartyConfig config, String orderId, List<String> detailIds,
                             String adminId, String token) {
        try {
            String apiBaseUrl = getApiBaseUrl(config);
            String unlockUrl = apiBaseUrl + String.format(API_PATH_DELETE_ORDER, config.getThirdPartyVenueId(), orderId);

            ChangxiaoerUnlockSlotRequest unlockRequest = ChangxiaoerUnlockSlotRequest.builder()
                    .refundOrNot(REFUND_OR_NOT)
                    .detailIds(detailIds)  // 空数组表示删除整个订单，非空表示部分删除
                    .adminId(adminId)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ChangxiaoerUnlockSlotRequest> unlockEntity = new HttpEntity<>(unlockRequest, headers);

            log.info("[changxiaoer] 调用解锁API - URL: {}", unlockUrl);
            log.info("[changxiaoer] 调用解锁API - Request: {}", JSON.toJSONString(unlockRequest));
            ResponseEntity<ChangxiaoerResponse<Object>> unlockResponseEntity = restTemplate.exchange(
                    unlockUrl,
                    HttpMethod.DELETE,
                    unlockEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );
            log.info("[changxiaoer] 调用解锁API - Response: {}", JSON.toJSONString(unlockResponseEntity.getBody()));

            ChangxiaoerResponse<Object> unlockResponse = unlockResponseEntity.getBody();
            if (unlockResponse == null || unlockResponse.getFlag() == null || !unlockResponse.getFlag()) {
                log.error("[changxiaoer] 解锁失败: orderId={}, errorMessage={}",
                        orderId, unlockResponse != null ? unlockResponse.getErrorMessage() : "响应为空");
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }

            log.info("[changxiaoer] 解锁成功: orderId={}", orderId);

        } catch (GloboxApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[changxiaoer] 删除订单异常: orderId={}", orderId, e);
            throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL, e);
        }
    }

    @Override
    public ThirdPartyAuthInfo login(VenueThirdPartyConfig config) {
        log.info("[changxiaoer] 开始登录: venueId={}, username={}",
                config.getVenueId(), config.getUsername());

        try {
            // 1. 构建登录请求
            ChangxiaoerLoginRequest loginRequest = ChangxiaoerLoginRequest.builder()
                    .phone(config.getUsername())
                    .pwd(config.getPassword())
                    .build();

            // 2. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 3. 构建请求实体
            HttpEntity<ChangxiaoerLoginRequest> requestEntity = new HttpEntity<>(loginRequest, headers);

            // 4. 发送POST请求
            String apiBaseUrl = getApiBaseUrl(config);
            String loginUrl = apiBaseUrl + API_PATH_LOGIN;
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                    loginUrl,
                    requestEntity,
                    String.class
            );

            // 5. 解析响应
            String responseBody = responseEntity.getBody();
            if (responseBody == null) {
                log.error("[changxiaoer] 登录失败: 响应为空, venueId={}", config.getVenueId());
                return null;
            }

            JSONObject jsonResponse = JSON.parseObject(responseBody);
            Boolean flag = jsonResponse.getBoolean("flag");

            if (flag == null || !flag) {
                log.error("[changxiaoer] 登录失败: flag=false, errorMessage={}, venueId={}",
                        jsonResponse.getString("errorMessage"), config.getVenueId());
                return null;
            }

            // 6. 提取data中的token和adminId
            JSONObject data = jsonResponse.getJSONObject("data");
            if (data == null) {
                log.error("[changxiaoer] 登录失败: data为空, venueId={}", config.getVenueId());
                return null;
            }

            String token = data.getString("token");
            Integer adminId = data.getInteger("adminId");

            if (token == null || token.isEmpty()) {
                log.error("[changxiaoer] 登录失败: Token为空, venueId={}", config.getVenueId());
                return null;
            }

            // 7. 构建认证信息
            ThirdPartyAuthInfo authInfo = ThirdPartyAuthInfo.builder()
                    .token(token)
                    .adminId(adminId != null ? adminId.toString() : null)
                    .build();

            log.info("[changxiaoer] 登录成功: venueId={}, adminId={}", config.getVenueId(), adminId);
            return authInfo;

        } catch (Exception e) {
            log.error("[changxiaoer] 登录异常: venueId={}", config.getVenueId(), e);
            return null;
        }
    }

    /**
     * 计算Away球场的价格
     * 返回slotDtos中所有可用槽位的详细价格信息
     *
     * @param config 第三方平台配置
     * @return 槽位价格列表
     */
    @Override
    public List<AwaySlotPrice> calculatePricing(VenueThirdPartyConfig config, LocalDate date) {
        List<ThirdPartyCourtSlotDto> slotDtos = querySlots(config,date);
        if (slotDtos == null || slotDtos.isEmpty()) {
            log.error("[changxiaoer] 槽位数据为空，无法计算价格");
            throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
        }

        try {
            // 遍历所有场地的槽位，构建详细的价格列表
            List<AwaySlotPrice> slotPrices = slotDtos.stream()
                    .filter(courtSlot -> courtSlot.getSlots() != null && !courtSlot.getSlots().isEmpty())
                    .flatMap(courtSlot -> courtSlot.getSlots().stream()
                            .map(slot -> {
                                try {
                                    LocalTime startTime = LocalTime.parse(slot.getStartTime());
                                    return AwaySlotPrice.builder()
                                            .startTime(startTime)
                                            .price(slot.getPrice())
                                            .thirdPartyCourtId(courtSlot.getThirdPartyCourtId())
                                            .build();
                                } catch (Exception e) {
                                    log.warn("[changxiaoer] 时间解析失败: {}", slot.getStartTime(), e);
                                    return null;
                                }
                            }))
                    .filter(Objects::nonNull)
                    .filter(slotPrice -> slotPrice.getPrice() != null)
                    .collect(Collectors.toList());

            if (slotPrices.isEmpty()) {
                log.error("[changxiaoer] 未获取到有效的槽位价格");
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }

            log.info("[changxiaoer] 价格计算完成 - 槽位数: {}", slotPrices.size());
            return slotPrices;

        } catch (GloboxApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[changxiaoer] 计算价格异常", e);
            throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL, e);
        }
    }

    @Override
    public String getPlatformCode() {
        return PLATFORM_CODE;
    }
}
