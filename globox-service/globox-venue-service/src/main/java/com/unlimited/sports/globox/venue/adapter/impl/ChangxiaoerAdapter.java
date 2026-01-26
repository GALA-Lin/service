package com.unlimited.sports.globox.venue.adapter.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.unlimited.sports.globox.model.venue.entity.venues.ThirdPartyPlatform;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueThirdPartyConfig;
import com.unlimited.sports.globox.service.RedisService;
import com.unlimited.sports.globox.venue.adapter.ThirdPartyPlatformAdapter;
import com.unlimited.sports.globox.venue.adapter.dto.ThirdPartyAuthInfo;
import com.unlimited.sports.globox.venue.adapter.dto.ThirdPartyCourtSlotDto;
import com.unlimited.sports.globox.venue.adapter.dto.ThirdPartySlotDto;
import com.unlimited.sports.globox.venue.adapter.dto.changxiaoer.ChangxiaoerLockSlotRequest;
import com.unlimited.sports.globox.venue.adapter.dto.changxiaoer.ChangxiaoerLoginRequest;
import com.unlimited.sports.globox.venue.adapter.dto.changxiaoer.ChangxiaoerPlace;
import com.unlimited.sports.globox.venue.adapter.dto.changxiaoer.ChangxiaoerResponse;
import com.unlimited.sports.globox.venue.adapter.dto.changxiaoer.ChangxiaoerUnlockSlotRequest;
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
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    private static final String PLATFORM_CODE = "changxiaoer";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String SLOTS_CACHE_KEY_PREFIX = "third_party:slots:";
    private static final long SLOTS_CACHE_TTL_MINUTES = 5;

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

        // 1. 先从Redis缓存获取
        String cacheKey = buildSlotsCacheKey(config.getVenueId(), date);
        List<ThirdPartyCourtSlotDto> cachedSlots = redisService.getCacheObject(cacheKey,
                new TypeReference<>() {
                });

        if (cachedSlots != null && !cachedSlots.isEmpty()) {
            log.info("[changxiaoer] 从缓存获取槽位成功: venueId={}, date={}, 场地数: {}",
                    config.getVenueId(), date, cachedSlots.size());
            return cachedSlots;
        }

        try {
            // 2. 缓存未命中，调用API获取槽位
            List<ThirdPartyCourtSlotDto> slots = querySlotsFromAPI(config, date);

            // 3. 将结果缓存
            if (slots != null && !slots.isEmpty()) {
                redisService.setCacheObject(cacheKey, slots, SLOTS_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                log.info("[changxiaoer] 槽位已缓存: venueId={}, date={}, ttl={}min",
                        config.getVenueId(), date, SLOTS_CACHE_TTL_MINUTES);
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
            // 1. 获取认证信息
            ThirdPartyAuthInfo authInfo = tokenService.getAuthInfo(config, this);
            String token = authInfo.getToken();
            String adminId = authInfo.getAdminId();

            if (adminId == null) {
                log.error("[changxiaoer] adminId为空: venueId={}", config.getVenueId());
                return null;
            }

            // 2. 格式化日期
            String dateStr = date.format(DATE_FORMATTER);

            // 3. 构建URL和查询参数
            String spaceId = config.getThirdPartyVenueId();
            String apiBaseUrl = getApiBaseUrl(config);
            String url = apiBaseUrl + "/merchants-management/spaces/" + spaceId + "/timetables";

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("basicsId", spaceId)
                    .queryParam("lockSitePatternType", "1")
                    .queryParam("adminId", adminId)
                    .queryParam("usage_start_date", dateStr)
                    .queryParam("usage_end_date", dateStr)
                    .queryParam("lockBeginDay", dateStr);

            // 4. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            // 5. 发送GET请求
            ResponseEntity<ChangxiaoerResponse<List<ChangxiaoerPlace>>> responseEntity = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // 6. 解析响应
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


    // todo 待测试
    @Override
    public String lockSlot(VenueThirdPartyConfig config, String thirdPartyCourtId,
                          String startTime, String endTime, LocalDate date, String remark) {
        log.info("[changxiaoer] 锁定槽位: venueId={}, courtId={}, date={}, time={}-{}",
                config.getVenueId(), thirdPartyCourtId, date, startTime, endTime);

        try {
            // 1. 获取认证信息
            ThirdPartyAuthInfo authInfo = tokenService.getAuthInfo(config, this);
            String token = authInfo.getToken();

            // 2. 格式化日期
            String dateStr = date.format(DATE_FORMATTER);

            // 3. 获取星期几（场小二需要dayOfWeek字段）
            String dayOfWeek = date.getDayOfWeek().toString();

            // 4. 构建锁场请求
            ChangxiaoerLockSlotRequest.DateRange dateRange = ChangxiaoerLockSlotRequest.DateRange.builder()
                    .startDate(dateStr)
                    .endDate(dateStr)
                    .build();

            ChangxiaoerLockSlotRequest.PhoneNumber phoneNumber = ChangxiaoerLockSlotRequest.PhoneNumber.builder()
                    .phoneNumber(config.getUsername())
                    .countryCode("+86")
                    .build();

            ChangxiaoerLockSlotRequest.Booker booker = ChangxiaoerLockSlotRequest.Booker.builder()
                    .phoneNumber(phoneNumber)
                    .bookerName(config.getPassword())
                    .build();

            ChangxiaoerLockSlotRequest.TimeRange timeRange = ChangxiaoerLockSlotRequest.TimeRange.builder()
                    .startTime(startTime)
                    .endTime(endTime)
                    .build();

            ChangxiaoerLockSlotRequest.PlaceOrder placeOrder = ChangxiaoerLockSlotRequest.PlaceOrder.builder()
                    .placeId(Long.parseLong(thirdPartyCourtId))
                    .timeRange(timeRange)
                    .price(0)  // 锁场价格为0
                    .surchargeItems(new ArrayList<>())
                    .build();

            ChangxiaoerLockSlotRequest.PaymentInfo paymentInfo = ChangxiaoerLockSlotRequest.PaymentInfo.builder()
                    .paymentMethod("CASH")
                    .paid(true)
                    .build();

            ChangxiaoerLockSlotRequest lockRequest = ChangxiaoerLockSlotRequest.builder()
                    .dateRange(dateRange)
                    .dayOfWeek(dayOfWeek)
                    .booker(booker)
                    .priceMode("CUSTOM")
                    .placeOrders(Collections.singletonList(placeOrder))
                    .paymentInfo(paymentInfo)
                    .remark(remark != null ? remark : "")
                    .adminId(authInfo.getAdminId())
                    .build();

            // 5. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ChangxiaoerLockSlotRequest> requestEntity = new HttpEntity<>(lockRequest, headers);

            // 6. 构建URL并发送POST请求
            String apiBaseUrl = getApiBaseUrl(config);
            String url = apiBaseUrl + "/merchants-management/spaces/" + config.getThirdPartyVenueId() + "/booking-orders";

            ResponseEntity<ChangxiaoerResponse<Object>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // 7. 解析响应
            ChangxiaoerResponse<Object> response = responseEntity.getBody();
            if (response == null || response.getFlag() == null || !response.getFlag()) {
                log.error("[changxiaoer] 锁场失败: errorMessage={}, venueId={}",
                        response != null ? response.getErrorMessage() : "响应为空", config.getVenueId());
                return null;
            }

            // 8. 锁场成功，但响应中models为空，需要查询获取orderNo
            log.info("[changxiaoer] 锁场请求成功: venueId={}, 正在查询订单ID", config.getVenueId());

            String orderNo = getOrderNoAfterLock(config, token, thirdPartyCourtId, startTime, endTime, date);
            if (orderNo == null) {
                log.error("[changxiaoer] 锁场成功但无法获取订单ID: courtId={}, date={}, time={}-{}",
                        thirdPartyCourtId, date, startTime, endTime);
                return null;
            }

            // 8. 锁场成功，清除槽位缓存
            String cacheKey = buildSlotsCacheKey(config.getVenueId(), date);
            redisService.deleteObject(cacheKey);
            log.info("[changxiaoer] 槽位缓存已清除: venueId={}, date={}", config.getVenueId(), date);

            log.info("[changxiaoer] 锁场成功: venueId={}, orderNo={}", config.getVenueId(), orderNo);
            return orderNo;

        } catch (Exception e) {
            log.error("[changxiaoer] 锁场异常: venueId={}", config.getVenueId(), e);
            return null;
        }
    }


    // todo 待测试
    @Override
    public boolean unlockSlot(VenueThirdPartyConfig config, String thirdPartyBookingId, LocalDate bookingDate) {
        log.info("[changxiaoer] 解锁槽位: venueId={}, bookingId={}, bookingDate={}",
                config.getVenueId(), thirdPartyBookingId, bookingDate);

        try {
            // 1. 获取认证信息
            ThirdPartyAuthInfo authInfo = tokenService.getAuthInfo(config, this);
            String token = authInfo.getToken();

            // 2. 构建解锁请求
            ChangxiaoerUnlockSlotRequest unlockRequest = ChangxiaoerUnlockSlotRequest.builder()
                    .refundOrNot(true)
                    .detailIds(new ArrayList<>())
                    .adminId(authInfo.getAdminId())
                    .build();

            // 3. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ChangxiaoerUnlockSlotRequest> requestEntity = new HttpEntity<>(unlockRequest, headers);

            // 4. 构建URL并发送DELETE请求
            String apiBaseUrl = getApiBaseUrl(config);
            String url = apiBaseUrl + "/merchants-management/spaces/" + config.getThirdPartyVenueId()
                    + "/booking-orders/" + thirdPartyBookingId;

            ResponseEntity<ChangxiaoerResponse<Object>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // 5. 解析响应
            ChangxiaoerResponse<Object> response = responseEntity.getBody();
            if (response == null || response.getFlag() == null || !response.getFlag()) {
                log.error("[changxiaoer] 解锁失败: errorMessage={}, venueId={}",
                        response != null ? response.getErrorMessage() : "响应为空", config.getVenueId());
                return false;
            }

            // 解锁成功，清除槽位缓存
            String cacheKey = buildSlotsCacheKey(config.getVenueId(), bookingDate);
            redisService.deleteObject(cacheKey);
            log.info("[changxiaoer] 槽位缓存已清除: venueId={}, bookingId={}, bookingDate={}", config.getVenueId(), thirdPartyBookingId, bookingDate);

            log.info("[changxiaoer] 解锁成功: venueId={}, bookingId={}, bookingDate={}", config.getVenueId(), thirdPartyBookingId, bookingDate);
            return true;

        } catch (Exception e) {
            log.error("[changxiaoer] 解锁异常: venueId={}", config.getVenueId(), e);
            return false;
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
            String loginUrl = apiBaseUrl + "/admin/merchantAdminLogin";
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
     * 锁场成功后查询获取订单号（orderNo）
     * 流程：1.查询锁场记录列表获取batchNo 2.通过batchNo查询订单详情获取orderNo
     */
    private String getOrderNoAfterLock(VenueThirdPartyConfig config, String token, String courtId,
                                       String startTime, String endTime, LocalDate date) {
        try {
            // 1. 查询进行中的锁场记录列表（finished=false表示进行中的订单）
            String apiBaseUrl = getApiBaseUrl(config);
            ThirdPartyAuthInfo authInfo = tokenService.getAuthInfo(config, this);
            String adminId = authInfo.getAdminId();

            String listUrl = apiBaseUrl + "/merchants-management/spaces/" + config.getThirdPartyVenueId()
                    + "/timetables/periods/usages?finished=false&pageNum=1&pageSize=20&adminId=" + adminId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

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

            // 2. 从列表中查找匹配的记录（根据场地ID、时间段、日期）
            JSONObject data = jsonResponse.getJSONObject("data");
            if (data == null || data.getJSONArray("list") == null) {
                log.error("[changxiaoer] 查询锁场记录失败: data或list为空");
                return null;
            }

            String dateStr = date.format(DATE_FORMATTER);
            String batchNo = null;

            for (Object item : data.getJSONArray("list")) {
                JSONObject record = (JSONObject) item;
                JSONArray placePeriods = record.getJSONArray("placePeriods");
                if (placePeriods == null) continue;

                // 遍历该批次的所有场地时段
                for (Object periodObj : placePeriods) {
                    JSONObject period = (JSONObject) periodObj;
                    JSONObject place = period.getJSONObject("place");
                    JSONObject timeRange = period.getJSONObject("timeRange");
                    String usageDate = period.getString("usageDate");

                    // 匹配场地ID、时间段和日期
                    if (place != null && timeRange != null
                            && courtId.equals(place.getString("placeId"))
                            && startTime.equals(timeRange.getString("startTime"))
                            && endTime.equals(timeRange.getString("endTime"))
                            && dateStr.equals(usageDate)) {
                        batchNo = record.getString("batchNo");
                        break;
                    }
                }

                if (batchNo != null) break;
            }

            if (batchNo == null) {
                log.error("[changxiaoer] 未找到匹配的锁场记录: courtId={}, date={}, time={}-{}",
                        courtId, date, startTime, endTime);
                return null;
            }

            log.info("[changxiaoer] 找到batchNo: {}", batchNo);

            // 3. 通过batchNo查询订单详情
            String batchUrl = apiBaseUrl + "/merchants-management/spaces/" + config.getThirdPartyVenueId()
                    + "/booking-orders/batches/" + batchNo + "?adminId=" + adminId;

            ResponseEntity<String> batchResponseEntity = restTemplate.exchange(
                    batchUrl,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            String batchResponseBody = batchResponseEntity.getBody();
            if (batchResponseBody == null) {
                log.error("[changxiaoer] 查询订单详情失败: 响应为空, batchNo={}", batchNo);
                return null;
            }

            JSONObject batchJsonResponse = JSON.parseObject(batchResponseBody);
            if (batchJsonResponse.getBoolean("flag") == null || !batchJsonResponse.getBoolean("flag")) {
                log.error("[changxiaoer] 查询订单详情失败: {}", batchJsonResponse.getString("errorMessage"));
                return null;
            }

            // 4. 从订单详情中提取orderNo
            JSONObject batchData = batchJsonResponse.getJSONObject("data");
            if (batchData == null || batchData.getJSONArray("orderInfos") == null
                    || batchData.getJSONArray("orderInfos").isEmpty()) {
                log.error("[changxiaoer] 订单详情中没有orderInfos, batchNo={}", batchNo);
                return null;
            }

            JSONObject orderInfo = batchData.getJSONArray("orderInfos").getJSONObject(0);
            String orderNo = orderInfo.getString("orderNo");

            if (orderNo == null || orderNo.isEmpty()) {
                log.error("[changxiaoer] 订单详情中没有orderNo, batchNo={}", batchNo);
                return null;
            }

            log.info("[changxiaoer] 成功获取orderNo: {}, batchNo={}", orderNo, batchNo);
            return orderNo;

        } catch (Exception e) {
            log.error("[changxiaoer] 查询订单号异常", e);
            return null;
        }
    }

    @Override
    public String getPlatformCode() {
        return PLATFORM_CODE;
    }

    /**
     * 构建槽位缓存的Redis Key
     */
    private String buildSlotsCacheKey(Long venueId, LocalDate date) {
        return SLOTS_CACHE_KEY_PREFIX + venueId + ":" + date.toString();
    }
}
