package com.unlimited.sports.globox.venue.adapter.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.model.venue.entity.venues.ThirdPartyPlatform;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueThirdPartyConfig;
import com.unlimited.sports.globox.venue.adapter.ThirdPartyPlatformAdapter;
import com.unlimited.sports.globox.venue.adapter.dto.ThirdPartyAuthInfo;
import com.unlimited.sports.globox.venue.adapter.dto.ThirdPartyCourtSlotDto;
import com.unlimited.sports.globox.venue.adapter.dto.ThirdPartySlotDto;
import com.unlimited.sports.globox.venue.adapter.dto.changxiaoer.ChangxiaoerLoginRequest;
import com.unlimited.sports.globox.venue.adapter.dto.changxiaoer.ChangxiaoerPlace;
import com.unlimited.sports.globox.venue.adapter.dto.changxiaoer.ChangxiaoerResponse;
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
import java.util.List;
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

    private static final String PLATFORM_CODE = "changxiaoer";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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
            log.info("[场小二] 平台API地址已加载: {}", platformBaseApiUrl);
        } else {
            log.warn("[场小二] 未找到平台配置，platformCode={}", PLATFORM_CODE);
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
        log.info("[场小二] 查询槽位: venueId={}, thirdPartyVenueId={}, date={}",
                config.getVenueId(), config.getThirdPartyVenueId(), date);

        try {
            // 1. 获取认证信息
            ThirdPartyAuthInfo authInfo = tokenService.getAuthInfo(config, this);
            String token = authInfo.getToken();
            String adminId = authInfo.getAdminId();

            if (adminId == null) {
                log.error("[场小二] adminId为空: venueId={}", config.getVenueId());
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
                log.error("[场小二] 查询槽位失败: 响应为空, venueId={}", config.getVenueId());
                return null;
            }

            if (response.getFlag() == null || !response.getFlag()) {
                log.error("[场小二] 查询槽位失败: flag=false, errorMessage={}, venueId={}",
                        response.getErrorMessage(), config.getVenueId());
                return null;
            }

            List<ChangxiaoerPlace> places = response.getData();
            log.info("[场小二] 查询槽位成功: venueId={}, 场地数量={}", config.getVenueId(), places.size());

            // 7. 转换为统一格式并返回
            return convertPlacesToDto(places);

        } catch (Exception e) {
            log.error("[场小二] 查询槽位异常: venueId={}", config.getVenueId(), e);
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

    @Override
    public String lockSlot(VenueThirdPartyConfig config, String thirdPartyCourtId,
                          String startTime, String endTime, LocalDate date, String remark) {
        // TODO: 实现具体的API调用逻辑
        return "";
    }

    @Override
    public boolean unlockSlot(VenueThirdPartyConfig config, String thirdPartyBookingId) {
        // TODO: 实现具体的API调用逻辑
        return false;
    }

    @Override
    public ThirdPartyAuthInfo login(VenueThirdPartyConfig config) {
        log.info("[场小二] 开始登录: venueId={}, username={}",
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
                log.error("[场小二] 登录失败: 响应为空, venueId={}", config.getVenueId());
                return null;
            }

            JSONObject jsonResponse = JSON.parseObject(responseBody);
            Boolean flag = jsonResponse.getBoolean("flag");

            if (flag == null || !flag) {
                log.error("[场小二] 登录失败: flag=false, errorMessage={}, venueId={}",
                        jsonResponse.getString("errorMessage"), config.getVenueId());
                return null;
            }

            // 6. 提取data中的token和adminId
            JSONObject data = jsonResponse.getJSONObject("data");
            if (data == null) {
                log.error("[场小二] 登录失败: data为空, venueId={}", config.getVenueId());
                return null;
            }

            String token = data.getString("token");
            Integer adminId = data.getInteger("adminId");

            if (token == null || token.isEmpty()) {
                log.error("[场小二] 登录失败: Token为空, venueId={}", config.getVenueId());
                return null;
            }

            // 7. 构建认证信息
            ThirdPartyAuthInfo authInfo = ThirdPartyAuthInfo.builder()
                    .token(token)
                    .adminId(adminId != null ? adminId.toString() : null)
                    .build();

            log.info("[场小二] 登录成功: venueId={}, adminId={}", config.getVenueId(), adminId);
            return authInfo;

        } catch (Exception e) {
            log.error("[场小二] 登录异常: venueId={}", config.getVenueId(), e);
            return null;
        }
    }

    @Override
    public String getPlatformCode() {
        return PLATFORM_CODE;
    }
}
