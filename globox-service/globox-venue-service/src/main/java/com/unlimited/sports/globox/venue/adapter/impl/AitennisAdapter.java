package com.unlimited.sports.globox.venue.adapter.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.unlimited.sports.globox.model.venue.entity.venues.ThirdPartyPlatform;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueThirdPartyConfig;
import com.unlimited.sports.globox.service.RedisService;
import com.unlimited.sports.globox.venue.adapter.ThirdPartyPlatformAdapter;
import com.unlimited.sports.globox.venue.adapter.dto.ThirdPartyAuthInfo;
import com.unlimited.sports.globox.venue.adapter.dto.ThirdPartyCourtSlotDto;
import com.unlimited.sports.globox.venue.adapter.dto.ThirdPartySlotDto;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

        // 1. 先从Redis缓存获取
        String cacheKey = buildSlotsCacheKey(config.getVenueId(), date);
        List<ThirdPartyCourtSlotDto> cachedSlots = redisService.getCacheObject(cacheKey,
                new TypeReference<>() {
                });

        if (cachedSlots != null && !cachedSlots.isEmpty()) {
            log.info("[aitennis] 从缓存获取槽位成功: venueId={}, date={}, 场地数: {}",
                    config.getVenueId(), date, cachedSlots.size());
            return cachedSlots;
        }

        try {
            // 2. 缓存未命中，调用API获取槽位
            List<ThirdPartyCourtSlotDto> slots = querySlotsFromAPI(config, date);

            // 3. 将结果缓存
            if (slots != null && !slots.isEmpty()) {
                redisService.setCacheObject(cacheKey, slots, SLOTS_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                log.info("[aitennis] 槽位已缓存: venueId={}, date={}, ttl={}min",
                        config.getVenueId(), date, SLOTS_CACHE_TTL_MINUTES);
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
            // 1. 获取认证信息
            ThirdPartyAuthInfo authInfo = tokenService.getAuthInfo(config, this);
            String token = authInfo.getToken();

            // 2. 格式化日期为 yyyyMMdd 格式
            String dateStr = date.format(DATE_FORMATTER);

            // 3. 构建URL
            String apiBaseUrl = getApiBaseUrl(config);
            String url = apiBaseUrl + "/b/v1/calendar:getDailyCourtEvents?date=" + dateStr;

            // 4. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", "Bearer " + token);
            headers.set("stadiumId", config.getThirdPartyVenueId()); // 场馆ID
            headers.set("client", "Web");
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            // 5. 发送GET请求
            ResponseEntity<AitennisResponse<List<AitennisCourtEvent>>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // 6. 解析响应
            AitennisResponse<List<AitennisCourtEvent>> response = responseEntity.getBody();
            if (response == null || response.getData() == null) {
                log.error("[aitennis] 查询槽位失败: 响应为空, venueId={}", config.getVenueId());
                return null;
            }

            if (response.getCode() == null || response.getCode() != 0) {
                log.error("[aitennis] 查询槽位失败: code={}, msg={}, venueId={}",
                        response.getCode(), response.getMsg(), config.getVenueId());
                return null;
            }

            List<AitennisCourtEvent> events = response.getData();
            log.info("[aitennis] 查询槽位成功: venueId={}, 事件数量={}", config.getVenueId(), events.size());

            // 7. 转换为统一格式并返回
            return convertEventsToDto(events);

        } catch (Exception e) {
            log.error("[aitennis] 查询槽位异常: venueId={}", config.getVenueId(), e);
            return null;
        }
    }

    /**
     * 将爱网球的CourtEvent数据转换为统一的DTO格式
     */
    private List<ThirdPartyCourtSlotDto> convertEventsToDto(List<AitennisCourtEvent> events) {
        // 按照场地ID分组
        return events.stream()
                .collect(Collectors.groupingBy(AitennisCourtEvent::getCourtId))
                .entrySet().stream()
                .map(entry -> {
                    String courtId = entry.getKey();
                    List<AitennisCourtEvent> courtEvents = entry.getValue();

                    // 获取场地名称（同一场地的所有事件名称相同）
                    String courtName = courtEvents.isEmpty() ? "" : courtEvents.get(0).getCourtName();

                    // 转换时间槽
                    List<ThirdPartySlotDto> slots = courtEvents.stream()
                            .map(event -> {
                                // 判断是否可用：type为rent_price表示可租赁
                                boolean available = "rent_price".equals(event.getType());

                                // 获取价格（从items中提取）
                                BigDecimal price = BigDecimal.ZERO;
                                if (event.getItems() != null && !event.getItems().isEmpty()) {
                                    AitennisEventItem firstItem = event.getItems().get(0);
                                    if (firstItem.getData() != null && firstItem.getData().getPrice() != null) {
                                        try {
                                            price = new BigDecimal(firstItem.getData().getPrice());
                                        } catch (NumberFormatException e) {
                                            log.warn("[aitennis] 价格格式错误: {}", firstItem.getData().getPrice());
                                        }
                                    }
                                }

                                return ThirdPartySlotDto.builder()
                                        .startTime(formatTime(event.getStartTime()))
                                        .endTime(formatTime(event.getEndTime()))
                                        .available(available)
                                        .price(price)
                                        .build();
                            })
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
     */
    private String formatTime(String time) {
        if (time == null || time.length() != 4) {
            return time;
        }
        return time.substring(0, 2) + ":" + time.substring(2, 4);
    }


    // todo 待测试
    @Override
    public String lockSlot(VenueThirdPartyConfig config, String thirdPartyCourtId,
                          String startTime, String endTime, LocalDate date, String remark) {
        log.info("[aitennis] 锁定槽位: venueId={}, courtId={}, date={}, time={}-{}",
                config.getVenueId(), thirdPartyCourtId, date, startTime, endTime);

        try {
            // 1. 获取认证信息
            ThirdPartyAuthInfo authInfo = tokenService.getAuthInfo(config, this);
            String token = authInfo.getToken();

            // 2. 先查询当天的槽位，获取rent_price_id
            String rentPriceId = getRentPriceId(config, token, thirdPartyCourtId, startTime, endTime, date);
            if (rentPriceId == null) {
                log.error("[aitennis] 未找到对应的rent_price_id: courtId={}, time={}-{}",
                        thirdPartyCourtId, startTime, endTime);
                return null;
            }

            // 3. 构建锁场请求（注意：锁场请求中的时间格式是"07:00"，与查询响应的"0700"不同）
            AitennisLockRequest.RentPriceInfo rentPriceInfo = AitennisLockRequest.RentPriceInfo.builder()
                    .id(rentPriceId)
                    .startTime(startTime)  // 使用原始的"07:00"格式
                    .endTime(endTime)      // 使用原始的"08:00"格式
                    .build();

            AitennisLockRequest lockRequest = AitennisLockRequest.builder()
                    .rentPriceId(Collections.singletonList(rentPriceId))
                    .purpose(20) // 锁场用途固定为20
                    .remark(remark != null ? remark : "")
                    .isDuplicate(false)
                    .rentPrice(Collections.singletonList(rentPriceInfo))
                    .build();

            // 4. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", "Bearer " + token);
            headers.set("stadiumId", config.getThirdPartyVenueId());
            headers.set("client", "Web");
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<AitennisLockRequest> requestEntity = new HttpEntity<>(lockRequest, headers);

            // 5. 发送POST请求
            String apiBaseUrl = getApiBaseUrl(config);
            String url = apiBaseUrl + "/b/v1/lockCourts";

            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    String.class
            );

            // 6. 解析响应
            String responseBody = responseEntity.getBody();
            if (responseBody == null) {
                log.error("[aitennis] 锁场失败: 响应为空, venueId={}", config.getVenueId());
                return null;
            }

            AitennisResponse<?> response = JSON.parseObject(responseBody, AitennisResponse.class);
            if (response.getCode() == null || response.getCode() != 0) {
                log.error("[aitennis] 锁场失败: code={}, msg={}, venueId={}",
                        response.getCode(), response.getMsg(), config.getVenueId());
                return null;
            }

            // 7. 锁场成功，但响应中data为空，需要再次查询获取lock_court记录ID
            log.info("[aitennis] 锁场请求成功: venueId={}, 正在查询lock_court记录ID", config.getVenueId());

            String lockCourtId = getLockCourtId(config, token, thirdPartyCourtId, startTime, endTime, date);
            if (lockCourtId == null) {
                log.error("[aitennis] 锁场成功但未找到lock_court记录ID: courtId={}, time={}-{}",
                        thirdPartyCourtId, startTime, endTime);
                return null;
            }

            // 8. 锁场成功，清除槽位缓存
            String cacheKey = buildSlotsCacheKey(config.getVenueId(), date);
            redisService.deleteObject(cacheKey);
            log.info("[aitennis] 槽位缓存已清除: venueId={}, date={}", config.getVenueId(), date);

            log.info("[aitennis] 锁场成功: venueId={}, lockCourtId={}", config.getVenueId(), lockCourtId);
            return lockCourtId;

        } catch (Exception e) {
            log.error("[aitennis] 锁场异常: venueId={}", config.getVenueId(), e);
            return null;
        }
    }

    /**
     * 查询指定时间槽的rent_price_id
     */
    private String getRentPriceId(VenueThirdPartyConfig config, String token, String courtId,
                                  String startTime, String endTime, LocalDate date) {
        try {
            // 格式化日期
            String dateStr = date.format(DATE_FORMATTER);

            // 构建URL
            String apiBaseUrl = getApiBaseUrl(config);
            String url = apiBaseUrl + "/b/v1/calendar:getDailyCourtEvents?date=" + dateStr;

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", "Bearer " + token);
            headers.set("stadiumId", config.getThirdPartyVenueId());
            headers.set("client", "Web");
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            // 发送GET请求
            ResponseEntity<AitennisResponse<List<AitennisCourtEvent>>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            AitennisResponse<List<AitennisCourtEvent>> response = responseEntity.getBody();
            if (response == null || response.getData() == null || response.getCode() != 0) {
                return null;
            }

            // 查找匹配的事件
            String formattedStartTime = unformatTime(startTime);
            String formattedEndTime = unformatTime(endTime);

            for (AitennisCourtEvent event : response.getData()) {
                if (event.getCourtId().equals(courtId)
                        && event.getStartTime().equals(formattedStartTime)
                        && event.getEndTime().equals(formattedEndTime)
                        && "rent_price".equals(event.getType())
                        && event.getItems() != null
                        && !event.getItems().isEmpty()) {
                    return event.getItems().get(0).getId();
                }
            }

            return null;

        } catch (Exception e) {
            log.error("[aitennis] 查询rent_price_id异常", e);
            return null;
        }
    }

    /**
     * 将时间格式从 "07:00" 转换为 "0700"
     */
    private String unformatTime(String time) {
        if (time == null) {
            return null;
        }
        return time.replace(":", "");
    }

    /**
     * 查询锁场成功后的lock_court记录ID（用于解锁）
     */
    private String getLockCourtId(VenueThirdPartyConfig config, String token, String courtId,
                                  String startTime, String endTime, LocalDate date) {
        try {
            // 格式化日期
            String dateStr = date.format(DATE_FORMATTER);

            // 构建URL
            String apiBaseUrl = getApiBaseUrl(config);
            String url = apiBaseUrl + "/b/v1/calendar:getDailyCourtEvents?date=" + dateStr;

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", "Bearer " + token);
            headers.set("stadiumId", config.getThirdPartyVenueId());
            headers.set("client", "Web");
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            // 发送GET请求
            ResponseEntity<AitennisResponse<List<AitennisCourtEvent>>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            AitennisResponse<List<AitennisCourtEvent>> response = responseEntity.getBody();
            if (response == null || response.getData() == null || response.getCode() != 0) {
                return null;
            }

            // 查找匹配的lock_court事件
            String formattedStartTime = unformatTime(startTime);
            String formattedEndTime = unformatTime(endTime);

            for (AitennisCourtEvent event : response.getData()) {
                if (event.getCourtId().equals(courtId)
                        && event.getStartTime().equals(formattedStartTime)
                        && event.getEndTime().equals(formattedEndTime)
                        && "lock_court".equals(event.getType())
                        && event.getItems() != null
                        && !event.getItems().isEmpty()) {
                    return event.getItems().get(0).getId();
                }
            }

            return null;

        } catch (Exception e) {
            log.error("[aitennis] 查询lock_court_id异常", e);
            return null;
        }
    }


    // todo 待测试
    @Override
    public boolean unlockSlot(VenueThirdPartyConfig config, String thirdPartyBookingId) {
        log.info("[aitennis] 解锁槽位: venueId={}, bookingId={}",
                config.getVenueId(), thirdPartyBookingId);

        try {
            // 1. 获取认证信息
            ThirdPartyAuthInfo authInfo = tokenService.getAuthInfo(config, this);
            String token = authInfo.getToken();

            // 2. 构建解锁请求
            AitennisUnlockRequest unlockRequest = AitennisUnlockRequest.builder()
                    .isAll(true)
                    .item(Collections.singletonList(thirdPartyBookingId))
                    .build();

            // 3. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", "Bearer " + token);
            headers.set("stadiumId", config.getThirdPartyVenueId());
            headers.set("client", "Web");
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<AitennisUnlockRequest> requestEntity = new HttpEntity<>(unlockRequest, headers);

            // 4. 构建URL并发送DELETE请求
            String apiBaseUrl = getApiBaseUrl(config);
            String url = apiBaseUrl + "/b/v1/lockCourts/" + thirdPartyBookingId;

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    requestEntity,
                    String.class
            );

            // 5. 解析响应
            String responseBody = responseEntity.getBody();
            if (responseBody == null) {
                log.error("[aitennis] 解锁失败: 响应为空, venueId={}", config.getVenueId());
                return false;
            }

            AitennisResponse<?> response = JSON.parseObject(responseBody, AitennisResponse.class);
            if (response.getCode() == null || response.getCode() != 0) {
                log.error("[aitennis] 解锁失败: code={}, msg={}, venueId={}",
                        response.getCode(), response.getMsg(), config.getVenueId());
                return false;
            }

            // 解锁成功，清除槽位缓存
            String cacheKey = buildSlotsCacheKey(config.getVenueId(), LocalDate.now());
            redisService.deleteObject(cacheKey);
            log.info("[aitennis] 槽位缓存已清除: venueId={}, bookingId={}", config.getVenueId(), thirdPartyBookingId);

            log.info("[aitennis] 解锁成功: venueId={}, bookingId={}", config.getVenueId(), thirdPartyBookingId);
            return true;

        } catch (Exception e) {
            log.error("[aitennis] 解锁异常: venueId={}", config.getVenueId(), e);
            return false;
        }
    }

    @Override
    public ThirdPartyAuthInfo login(VenueThirdPartyConfig config) {
        log.info("[aitennis] 开始登录: venueId={}, username={}",
                config.getVenueId(), config.getUsername());

        try {
            // 1. 构建登录URL（phone和password作为query params）
            String apiBaseUrl = getApiBaseUrl(config);
            String loginUrl = apiBaseUrl + "/b/v1/staffs:loginByPhone"
                    + "?phone=" + config.getUsername()
                    + "&password=" + config.getPassword();

            // 2. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            // 3. 发送POST请求
            ResponseEntity<AitennisResponse<AitennisLoginResponse>> responseEntity = restTemplate.exchange(
                    loginUrl,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // 4. 解析响应
            AitennisResponse<AitennisLoginResponse> response = responseEntity.getBody();
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

            // 5. 去掉 "Bearer " 前缀（如果存在），因为使用时会重新添加
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // 6. 构建认证信息
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
     * 构建槽位缓存的Redis Key
     */
    private String buildSlotsCacheKey(Long venueId, LocalDate date) {
        return SLOTS_CACHE_KEY_PREFIX + venueId + ":" + date.toString();
    }
}
