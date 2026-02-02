package com.unlimited.sports.globox.venue.adapter.impl;

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
import com.unlimited.sports.globox.venue.adapter.dto.d2yun.*;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * D2yun平台适配器
 */
@Slf4j
@Component
public class D2yunAdapter implements ThirdPartyPlatformAdapter {

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

    private static final String PLATFORM_CODE = "d2yun";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // API路径常量
    private static final String API_PATH_LOGIN = "/v1/auth/user/login";
    private static final String API_PATH_RESOURCE_LIST = "/v1/space/resource/list";
    private static final String API_PATH_RESOURCE_LOCK = "/v1/space/resource/lock";
    private static final String API_PATH_RESOURCE_UNLOCK = "/v1/space/resource/unlock";

    // 查询参数常量
    private static final String ROLE_TYPE = "1";
    private static final String LOGIN_METHOD = "v2-password";
    private static final String PLATFORM = "PC";

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
            log.info("[d2yun] 平台API地址已加载: {}", platformBaseApiUrl);
        } else {
            log.warn("[d2yun] 未找到平台配置，platformCode={}", PLATFORM_CODE);
        }
    }

    /**
     * 获取API基础地址
     * 优先使用场馆配置的专用API地址，否则使用平台默认地址
     */
    private String getApiBaseUrl(VenueThirdPartyConfig config) {
        String apiUrl = config.getApiUrl();
        if (apiUrl != null && !apiUrl.trim().isEmpty()) {
            return apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
        }

        if (platformBaseApiUrl == null || platformBaseApiUrl.trim().isEmpty()) {
            throw new IllegalStateException("平台基础API地址未配置: " + PLATFORM_CODE);
        }
        return platformBaseApiUrl.endsWith("/")
                ? platformBaseApiUrl.substring(0, platformBaseApiUrl.length() - 1)
                : platformBaseApiUrl;
    }

    @Override
    public List<ThirdPartyCourtSlotDto> querySlots(VenueThirdPartyConfig config, LocalDate date) {
        // 先从Redis缓存获取
        String cacheKey = AwayVenueCacheConstants.buildSlotsCacheKey(config.getVenueId(), date);
        List<ThirdPartyCourtSlotDto> cachedSlots = redisService.getCacheObject(cacheKey,
                new TypeReference<>() {
                });

        if (cachedSlots != null && !cachedSlots.isEmpty()) {
            return cachedSlots;
        }

        try {
            // 缓存未命中，调用API获取槽位
            List<ThirdPartyCourtSlotDto> slots = querySlotsFromAPI(config, date);

            // 将结果缓存
            if (slots != null && !slots.isEmpty()) {
                redisService.setCacheObject(cacheKey, slots,
                        AwayVenueCacheConstants.SLOTS_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            }

            return slots;

        } catch (Exception e) {
            log.error("[d2yun] 查询槽位异常: venueId={}, date={}", config.getVenueId(), date, e);
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
            String stadiumId = authInfo.getStadiumId();
            String businessId = authInfo.getBusinessId();

            if (stadiumId == null || stadiumId.isEmpty()) {
                log.error("[d2yun] 查询槽位失败: stadiumId为空, venueId={}", config.getVenueId());
                return null;
            }

            if (businessId == null || businessId.isEmpty()) {
                log.error("[d2yun] 查询槽位失败: businessId为空, venueId={}", config.getVenueId());
                return null;
            }

            // 2. 格式化日期
            String dateStr = date.format(DATE_FORMATTER);

            // 3. 构建URL
            String apiBaseUrl = getApiBaseUrl(config);
            String url = apiBaseUrl + API_PATH_RESOURCE_LIST
                    + "?date=" + dateStr
                    + "&stadium_id=" + stadiumId
                    + "&business_id=" + businessId
                    + "&role_type=" + ROLE_TYPE;

            // 4. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            // 5. 发送GET请求
            ResponseEntity<D2yunResponse<D2yunResourceListData>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // 6. 解析响应
            D2yunResponse<D2yunResourceListData> response = responseEntity.getBody();
            if (response == null || response.getData() == null) {
                log.error("[d2yun] 查询槽位失败: 响应为空");
                return null;
            }

            if (response.getStatus() == null || response.getStatus() != 0) {
                log.error("[d2yun] 查询槽位失败: status={}, msg={}", response.getStatus(), response.getMsg());
                return null;
            }

            D2yunResourceListData data = response.getData();
            log.info("[d2yun] 查询槽位成功: venueId={}, date={}, 场地数={}",
                    config.getVenueId(), date, data.getSpaces() != null ? data.getSpaces().size() : 0);

            // 7. 转换为统一格式并返回
            return convertResourcesToDto(data);

        } catch (Exception e) {
            log.error("[d2yun] 查询槽位异常: venueId={}", config.getVenueId(), e);
            return null;
        }
    }

    /**
     * 将D2yun的资源数据转换为统一的DTO格式
     */
    private List<ThirdPartyCourtSlotDto> convertResourcesToDto(D2yunResourceListData data) {
        if (data.getSpaces() == null || data.getResources() == null) {
            return new ArrayList<>();
        }

        List<ThirdPartyCourtSlotDto> result = new ArrayList<>();

        // resources是二维数组，每个场地对应一个资源列表
        for (int i = 0; i < data.getSpaces().size() && i < data.getResources().size(); i++) {
            D2yunSpace space = data.getSpaces().get(i);
            List<D2yunResource> resources = data.getResources().get(i);

            // 转换时间槽
            List<ThirdPartySlotDto> slots = resources.stream()
                    .map(resource -> {
                        // status=1表示可用
                        boolean available = resource.getStatus() != null && resource.getStatus() == 1;

                        // 价格从分转换为元
                        BigDecimal price = BigDecimal.ZERO;
                        if (resource.getWorth() != null && resource.getWorth() > 0) {
                            price = new BigDecimal(resource.getWorth()).divide(new BigDecimal(100));
                        }

                        // 时间从分钟数转换为HH:mm格式
                        String startTime = minutesToTime(resource.getTime());
                        // 根据setting的duration计算结束时间
                        Integer duration = data.getSetting() != null ? data.getSetting().getDuration() : 30;
                        String endTime = minutesToTime(resource.getTime() + duration);

                        return ThirdPartySlotDto.builder()
                                .startTime(startTime)
                                .endTime(endTime)
                                .available(available)
                                .price(price)
                                .build();
                    })
                    .collect(Collectors.toList());

            result.add(ThirdPartyCourtSlotDto.builder()
                    .thirdPartyCourtId(space.getSpaceId().toString())
                    .courtName(space.getSpaceName())
                    .slots(slots)
                    .build());
        }

        return result;
    }

    /**
     * 将分钟数转换为HH:mm格式
     * 例如：480 -> "08:00", 510 -> "08:30"
     */
    private String minutesToTime(Integer minutes) {
        if (minutes == null) {
            return "00:00";
        }
        int hours = minutes / 60;
        int mins = minutes % 60;
        return String.format("%02d:%02d", hours, mins);
    }

    /**
     * 将HH:mm格式转换为分钟数
     * 例如："08:00" -> 480, "08:30" -> 510
     */
    private Integer timeToMinutes(String time) {
        if (time == null || time.isEmpty()) {
            return 0;
        }
        String[] parts = time.split(":");
        if (parts.length != 2) {
            return 0;
        }
        try {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            return hours * 60 + minutes;
        } catch (NumberFormatException e) {
            log.warn("[d2yun] 时间格式错误: {}", time);
            return 0;
        }
    }

    @Override
    public LockSlotsResult lockSlots(VenueThirdPartyConfig config, List<SlotLockRequest> slotRequests, String remark) {
        log.info("[d2yun] 锁定槽位开始: venueId={}, slotCount={}", config.getVenueId(), slotRequests.size());

        try {
            // 1. 获取认证信息
            ThirdPartyAuthInfo authInfo = tokenService.getAuthInfo(config, this);
            String token = authInfo.getToken();
            String stadiumId = authInfo.getStadiumId();
            String businessId = authInfo.getBusinessId();

            if (stadiumId == null || stadiumId.isEmpty()) {
                log.error("[d2yun] stadiumId为空: venueId={}", config.getVenueId());
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }

            if (businessId == null || businessId.isEmpty()) {
                log.error("[d2yun] businessId为空: venueId={}", config.getVenueId());
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }

            // 2. 获取第一个槽位的日期（假设所有槽位同一天）
            LocalDate firstDate = slotRequests.get(0).getDate();

            // 3. 查询当天的槽位，获取需要锁定的资源信息
            List<D2yunResource> allResourcesToLock = new ArrayList<>();
            for (SlotLockRequest slot : slotRequests) {
                List<D2yunResource> resources = getResourcesToLock(config, authInfo,
                        slot.getThirdPartyCourtId(), slot.getStartTime(), slot.getEndTime(), slot.getDate());
                if (resources != null && !resources.isEmpty()) {
                    allResourcesToLock.addAll(resources);
                }
            }

            if (allResourcesToLock.isEmpty()) {
                log.error("[d2yun] 未找到需要锁定的资源: venueId={}", config.getVenueId());
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }

            // 4. 构建锁定请求
            List<D2yunLockRequest.D2yunLockResourceItem> lockItems = allResourcesToLock.stream()
                    .map(resource -> D2yunLockRequest.D2yunLockResourceItem.builder()
                            .id(resource.getId())
                            .name(resource.getName())
                            .spaceId(resource.getSpaceId())
                            .time(resource.getTime())
                            .worth(resource.getWorth())
                            .unit(resource.getUnit())
                            .status(resource.getStatus())
                            .remark(remark != null ? remark : "")
                            .resourceId(resource.getResourceId())
                            .scheduleId(resource.getScheduleId())
                            .orderId(resource.getOrderId())
                            .build())
                    .collect(Collectors.toList());

            D2yunLockRequest lockRequest = D2yunLockRequest.builder()
                    .date(firstDate.format(DATE_FORMATTER))
                    .stadiumId(Long.parseLong(stadiumId))
                    .list(lockItems)
                    .remark(remark != null ? remark : "")
                    .build();

            // 5. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<D2yunLockRequest> requestEntity = new HttpEntity<>(lockRequest, headers);

            // 6. 构建URL并发送POST请求
            String apiBaseUrl = getApiBaseUrl(config);
            String url = apiBaseUrl + API_PATH_RESOURCE_LOCK
                    + "?business_id=" + businessId
                    + "&role_type=" + ROLE_TYPE;

            ResponseEntity<D2yunResponse<Object>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // 7. 解析响应
            D2yunResponse<Object> response = responseEntity.getBody();
            if (response == null || response.getStatus() == null || response.getStatus() != 0) {
                log.error("[d2yun] 锁定失败: status={}, msg={}",
                        response != null ? response.getStatus() : null,
                        response != null ? response.getMsg() : "响应为空");
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }

            // 8. 锁场成功，清除槽位缓存
            String cacheKey = AwayVenueCacheConstants.buildSlotsCacheKey(config.getVenueId(), firstDate);
            redisService.deleteObject(cacheKey);

            // 9. 构建锁场结果（使用resource的id作为booking_id）
            Map<SlotLockRequest, String> bookingIds = new HashMap<>();
            List<AwaySlotPrice> slotPrices = new ArrayList<>();

            for (SlotLockRequest slot : slotRequests) {
                // 查找对应的resource
                D2yunResource matchedResource = allResourcesToLock.stream()
                        .filter(r -> r.getSpaceId().toString().equals(slot.getThirdPartyCourtId())
                                && minutesToTime(r.getTime()).equals(slot.getStartTime()))
                        .findFirst()
                        .orElse(null);

                if (matchedResource != null) {
                    bookingIds.put(slot, matchedResource.getId());

                    // 添加价格信息
                    BigDecimal price = matchedResource.getWorth() != null
                            ? new BigDecimal(matchedResource.getWorth()).divide(new BigDecimal(100))
                            : BigDecimal.ZERO;
                    slotPrices.add(AwaySlotPrice.builder()
                            .startTime(LocalTime.parse(slot.getStartTime()))
                            .price(price)
                            .thirdPartyCourtId(slot.getThirdPartyCourtId())
                            .build());
                }
            }

            LockSlotsResult result = LockSlotsResult.builder()
                    .bookingIds(bookingIds)
                    .slotPrices(slotPrices)
                    .build();

            log.info("[d2yun] 锁定成功: venueId={}, count={}", config.getVenueId(), bookingIds.size());
            return result;

        } catch (GloboxApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[d2yun] 锁场异常: venueId={}", config.getVenueId(), e);
            throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
        }
    }

    /**
     * 查询需要锁定的资源列表
     */
    private List<D2yunResource> getResourcesToLock(VenueThirdPartyConfig config, ThirdPartyAuthInfo authInfo,
                                                   String courtId, String startTime, String endTime, LocalDate date) {
        try {
            String token = authInfo.getToken();
            String stadiumId = authInfo.getStadiumId();
            String businessId = authInfo.getBusinessId();

            // 格式化日期
            String dateStr = date.format(DATE_FORMATTER);

            // 构建URL
            String apiBaseUrl = getApiBaseUrl(config);
            String url = apiBaseUrl + API_PATH_RESOURCE_LIST
                    + "?date=" + dateStr
                    + "&stadium_id=" + stadiumId
                    + "&business_id=" + businessId
                    + "&role_type=" + ROLE_TYPE;

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            // 发送GET请求
            ResponseEntity<D2yunResponse<D2yunResourceListData>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            D2yunResponse<D2yunResourceListData> response = responseEntity.getBody();
            if (response == null || response.getData() == null || response.getStatus() != 0) {
                return null;
            }

            D2yunResourceListData data = response.getData();

            // 转换时间为分钟数
            Integer startMinutes = timeToMinutes(startTime);
            Integer endMinutes = timeToMinutes(endTime);

            // 查找匹配的资源
            List<D2yunResource> matchedResources = new ArrayList<>();

            if (data.getSpaces() != null && data.getResources() != null) {
                for (int i = 0; i < data.getSpaces().size() && i < data.getResources().size(); i++) {
                    D2yunSpace space = data.getSpaces().get(i);
                    if (!courtId.equals(space.getSpaceId().toString())) {
                        continue;
                    }

                    List<D2yunResource> resources = data.getResources().get(i);
                    for (D2yunResource resource : resources) {
                        // 匹配时间范围内的所有资源
                        if (resource.getTime() != null
                                && resource.getTime() >= startMinutes
                                && resource.getTime() < endMinutes) {
                            matchedResources.add(resource);
                        }
                    }
                    break;
                }
            }

            return matchedResources;

        } catch (Exception e) {
            log.error("[d2yun] 查询待锁定资源异常", e);
            return null;
        }
    }

    @Override
    public boolean unlockSlots(VenueThirdPartyConfig config, List<SlotLockRequest> slotRequests) {
        log.info("[d2yun] 解锁槽位开始: venueId={}, slotCount={}", config.getVenueId(), slotRequests.size());


        try {
            // 1. 获取认证信息
            ThirdPartyAuthInfo authInfo = tokenService.getAuthInfo(config, this);
            String token = authInfo.getToken();
            String stadiumId = authInfo.getStadiumId();
            String businessId = authInfo.getBusinessId();

            if (stadiumId == null || stadiumId.isEmpty()) {
                log.error("[d2yun] 解锁失败: stadiumId为空, venueId={}", config.getVenueId());
                return false;
            }

            if (businessId == null || businessId.isEmpty()) {
                log.error("[d2yun] 解锁失败: businessId为空, venueId={}", config.getVenueId());
                return false;
            }

            // 2. 提取锁定时保存的thirdPartyBookingId列表
            Set<String> bookingIds = slotRequests.stream()
                    .map(SlotLockRequest::getThirdPartyBookingId)
                    .filter(id -> id != null && !id.isEmpty())
                    .collect(Collectors.toSet());
            
            if (bookingIds.isEmpty()) {
                log.error("[d2yun] 解锁失败: 未找到有效的thirdPartyBookingId, venueId={}", config.getVenueId());
                return false;
            }
            
            log.info("[d2yun] 使用锁定ID解锁: bookingIds={}", bookingIds);

            // 3. 获取第一个槽位的日期（用于清除缓存）
            LocalDate firstDate = slotRequests.get(0).getDate();

            // 4. 查询锁定的资源，并通过thirdPartyBookingId过滤（安全校验）
            List<D2yunResource> allResourcesToUnlock = new ArrayList<>();
            for (SlotLockRequest slot : slotRequests) {
                List<D2yunResource> resources = getResourcesToUnlock(config, authInfo,
                        slot.getThirdPartyCourtId(), slot.getStartTime(), slot.getEndTime(), slot.getDate());
                if (resources != null && !resources.isEmpty()) {
                    // 只解锁ID匹配的资源（安全校验）
                    for (D2yunResource resource : resources) {
                        if (bookingIds.contains(resource.getId())) {
                            allResourcesToUnlock.add(resource);
                        }
                    }
                }
            }

            if (allResourcesToUnlock.isEmpty()) {
                log.error("[d2yun] 未找到匹配的要解锁的资源: venueId={}, bookingIds={}", config.getVenueId(), bookingIds);
                return false;
            }

            // 5. 构建解锁请求
            List<D2yunUnlockRequest.D2yunUnlockResourceItem> unlockItems = allResourcesToUnlock.stream()
                    .map(resource -> D2yunUnlockRequest.D2yunUnlockResourceItem.builder()
                            .id(resource.getId())
                            .name(resource.getName())
                            .spaceId(resource.getSpaceId())
                            .time(resource.getTime())
                            .worth(resource.getWorth())
                            .status(resource.getStatus())
                            .remark(resource.getRemark())
                            .resourceId(resource.getResourceId())
                            .scheduleId(resource.getScheduleId())
                            .orderId(resource.getOrderId())
                            .build())
                    .collect(Collectors.toList());

            D2yunUnlockRequest unlockRequest = D2yunUnlockRequest.builder()
                    .list(unlockItems)
                    .stadiumId(Long.parseLong(stadiumId))
                    .build();

            // 5. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<D2yunUnlockRequest> requestEntity = new HttpEntity<>(unlockRequest, headers);

            // 6. 构建URL并发送POST请求
            String apiBaseUrl = getApiBaseUrl(config);
            String url = apiBaseUrl + API_PATH_RESOURCE_UNLOCK
                    + "?business_id=" + businessId
                    + "&role_type=" + ROLE_TYPE;

            ResponseEntity<D2yunResponse<Object>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // 7. 解析响应
            D2yunResponse<Object> response = responseEntity.getBody();
            if (response == null || response.getStatus() == null || response.getStatus() != 0) {
                log.error("[d2yun] 解锁失败: status={}, msg={}",
                        response != null ? response.getStatus() : null,
                        response != null ? response.getMsg() : "响应为空");
                return false;
            }

            // 解锁成功，清除槽位缓存
            String cacheKey = AwayVenueCacheConstants.buildSlotsCacheKey(config.getVenueId(), firstDate);
            redisService.deleteObject(cacheKey);

            log.info("[d2yun] 解锁成功: venueId={}, count={}", config.getVenueId(), unlockItems.size());
            return true;

        } catch (Exception e) {
            log.error("[d2yun] 解锁异常: venueId={}", config.getVenueId(), e);
            return false;
        }
    }

    /**
     * 查询需要解锁的资源列表
     */
    private List<D2yunResource> getResourcesToUnlock(VenueThirdPartyConfig config, ThirdPartyAuthInfo authInfo,
                                                     String courtId, String startTime, String endTime, LocalDate date) {
        try {
            String token = authInfo.getToken();
            String stadiumId = authInfo.getStadiumId();
            String businessId = authInfo.getBusinessId();

            // 格式化日期
            String dateStr = date.format(DATE_FORMATTER);

            // 构建URL
            String apiBaseUrl = getApiBaseUrl(config);
            String url = apiBaseUrl + API_PATH_RESOURCE_LIST
                    + "?date=" + dateStr
                    + "&stadium_id=" + stadiumId
                    + "&business_id=" + businessId
                    + "&role_type=" + ROLE_TYPE;

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            // 发送GET请求
            ResponseEntity<D2yunResponse<D2yunResourceListData>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            D2yunResponse<D2yunResourceListData> response = responseEntity.getBody();
            if (response == null || response.getData() == null || response.getStatus() != 0) {
                log.warn("[d2yun] 查询资源失败: status={}, msg={}",
                        response != null ? response.getStatus() : null,
                        response != null ? response.getMsg() : "响应为空");
                return null;
            }

            D2yunResourceListData data = response.getData();

            // 转换时间为分钟数
            Integer startMinutes = timeToMinutes(startTime);
            Integer endMinutes = timeToMinutes(endTime);

            log.info("[d2yun] 查询待解锁资源: courtId={}, 时间={}~{}分钟({}-{}), 日期={}",
                    courtId, startMinutes, endMinutes, startTime, endTime, dateStr);

            // 查找匹配的资源（状态为4表示已锁定）
            List<D2yunResource> matchedResources = new ArrayList<>();

            if (data.getSpaces() != null && data.getResources() != null) {
                log.info("[d2yun] 资源查询返回: 场地数={}个", data.getSpaces().size());

                for (int i = 0; i < data.getSpaces().size() && i < data.getResources().size(); i++) {
                    D2yunSpace space = data.getSpaces().get(i);
                    String spaceIdStr = space.getSpaceId().toString();

                    log.info("[d2yun] 比较courtId: 查询={}, 返回={}", courtId, spaceIdStr);

                    if (!courtId.equals(spaceIdStr)) {
                        continue;
                    }

                    log.info("[d2yun] 找到匹配的场地: spaceId={}, spaceName={}", spaceIdStr, space.getSpaceName());

                    List<D2yunResource> resources = data.getResources().get(i);
                    log.info("[d2yun] 该场地包含{}个时段", resources.size());

                    for (D2yunResource resource : resources) {
                        // 匹配时间范围内且状态为4（已锁定）的资源
                        if (resource.getTime() != null) {
                            log.info("[d2yun] 检查资源: time={}, status={}, id={}",
                                    resource.getTime(), resource.getStatus(), resource.getId());
                        }

                        if (resource.getTime() != null
                                && resource.getTime() >= startMinutes
                                && resource.getTime() < endMinutes
                                && resource.getStatus() != null
                                && resource.getStatus() == 4) {
                            log.info("[d2yun] 找到待解锁资源: id={}, time={}, status={}",
                                    resource.getId(), resource.getTime(), resource.getStatus());
                            matchedResources.add(resource);
                        }
                    }
                    break;
                }

                if (matchedResources.isEmpty()) {
                    log.warn("[d2yun] 未找到匹配的待解锁资源: courtId={}, 时间={}~{}, 日期={}",
                            courtId, startTime, endTime, dateStr);
                }
            } else {
                log.warn("[d2yun] 资源数据为空: spaces={}, resources={}",
                        data.getSpaces() != null ? "有" : "空",
                        data.getResources() != null ? "有" : "空");
            }

            return matchedResources;

        } catch (Exception e) {
            log.error("[d2yun] 查询待解锁资源异常", e);
            return null;
        }
    }

    @Override
    public ThirdPartyAuthInfo login(VenueThirdPartyConfig config) {
        try {
            // 1. 构建登录请求
            D2yunLoginRequest loginRequest = D2yunLoginRequest.builder()
                    .phone(config.getUsername())
                    .password(config.getPassword())
                    .platform(PLATFORM)
                    .build();

            // 2. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<D2yunLoginRequest> requestEntity = new HttpEntity<>(loginRequest, headers);

            // 3. 构建URL并发送POST请求
            String apiBaseUrl = getApiBaseUrl(config);
            String loginUrl = apiBaseUrl + API_PATH_LOGIN + "?method=" + LOGIN_METHOD;

            ResponseEntity<D2yunResponse<D2yunLoginData>> responseEntity = restTemplate.exchange(
                    loginUrl,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // 4. 解析响应
            D2yunResponse<D2yunLoginData> response = responseEntity.getBody();
            if (response == null || response.getData() == null) {
                log.error("[d2yun] 登录失败: 响应为空");
                return null;
            }

            if (response.getStatus() == null || response.getStatus() != 0) {
                log.error("[d2yun] 登录失败: status={}, msg={}", response.getStatus(), response.getMsg());
                return null;
            }

            D2yunLoginData loginData = response.getData();
            String token = loginData.getToken();

            if (token == null || token.trim().isEmpty()) {
                log.error("[d2yun] 登录失败: Token为空");
                return null;
            }

            // 5. 构建认证信息（包含token、businessId、stadiumId）
            String businessId = loginData.getBusinessId() != null ? loginData.getBusinessId().toString() : null;
            // stadiumId从数据库配置中读取，不使用登录响应的值（因为账户可能关联多个stadium）
            String stadiumId = config.getThirdPartyVenueId();

            log.info("[d2yun] 登录成功: venueId={}, businessId={}, stadiumId={}, apiResponseStadiumId={}",
                    config.getVenueId(), businessId, stadiumId, loginData.getStadiumId());

            if (businessId == null || businessId.isEmpty()) {
                log.warn("[d2yun] businessId为空，尝试从响应日志中重新打印: {}", response);
            }

            if (stadiumId == null || stadiumId.isEmpty()) {
                log.error("[d2yun] stadiumId为空: 数据库配置中thirdPartyVenueId未设置");
                return null;
            }

            ThirdPartyAuthInfo authInfo = ThirdPartyAuthInfo.builder()
                    .token(token)
                    .businessId(businessId)
                    .stadiumId(stadiumId)
                    .build();

            log.info("[d2yun] 构建的authInfo: token={}, businessId={}, stadiumId={}",
                    token, authInfo.getBusinessId(), authInfo.getStadiumId());

            return authInfo;

        } catch (Exception e) {
            log.error("[d2yun] 登录异常: venueId={}", config.getVenueId(), e);
            return null;
        }
    }

    @Override
    public List<AwaySlotPrice> calculatePricing(VenueThirdPartyConfig config, LocalDate date) {
        log.info("[d2yun] 计算价格: venueId={}, date={}", config.getVenueId(), date);

        try {
            // 查询槽位信息
            List<ThirdPartyCourtSlotDto> courtSlots = querySlots(config, date);
            if (courtSlots == null || courtSlots.isEmpty()) {
                log.warn("[d2yun] 未找到槽位信息: venueId={}, date={}", config.getVenueId(), date);
                return new ArrayList<>();
            }

            // 转换为价格列表
            List<AwaySlotPrice> slotPrices = courtSlots.stream()
                    .filter(courtSlot -> courtSlot.getSlots() != null)
                    .flatMap(courtSlot -> courtSlot.getSlots().stream()
                            .filter(slot -> slot.getAvailable() != null && slot.getAvailable())
                            .map(slot -> AwaySlotPrice.builder()
                                    .startTime(LocalTime.parse(slot.getStartTime()))
                                    .price(slot.getPrice())
                                    .thirdPartyCourtId(courtSlot.getThirdPartyCourtId())
                                    .build())
                    )
                    .collect(Collectors.toList());

            log.info("[d2yun] 价格计算完成: venueId={}, date={}, 可用槽位数: {}",
                    config.getVenueId(), date, slotPrices.size());
            return slotPrices;

        } catch (Exception e) {
            log.error("[d2yun] 计算价格异常: venueId={}, date={}", config.getVenueId(), date, e);
            return new ArrayList<>();
        }
    }

    @Override
    public String getPlatformCode() {
        return PLATFORM_CODE;
    }
}
