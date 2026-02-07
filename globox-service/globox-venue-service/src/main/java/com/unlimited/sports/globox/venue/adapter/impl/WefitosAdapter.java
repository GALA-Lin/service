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
import com.unlimited.sports.globox.venue.adapter.dto.wefitos.*;
import com.unlimited.sports.globox.venue.mapper.ThirdPartyPlatformMapper;
import com.unlimited.sports.globox.venue.service.IThirdPartyTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
import java.util.stream.IntStream;
import com.unlimited.sports.globox.venue.util.TimeSlotSplitUtil;

/**
 * Wefitos平台适配器
 * 处理微键平台的场馆管理集成
 */
@Slf4j
@Component
public class WefitosAdapter implements ThirdPartyPlatformAdapter {

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

    private static final String PLATFORM_CODE = "wefitos";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // API路径常量
    private static final String API_PATH_LOGIN = "/panel/welcome/login-action";
    private static final String API_PATH_RESOURCE_LIST = "/panel/club/{clubId}/court/Welcome/getOneProjectTable";
    private static final String API_PATH_LOCK = "/panel/club/{clubId}/court/lock/add";
    private static final String API_PATH_UNLOCK = "/panel/club/{clubId}/court/lock/recover";
    private static final String API_PATH_COURT_PROJECT_LIST = "/panel/club/{clubId}/court/project/get-court-project-list";

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
            log.info("[wefitos] 平台API地址已加载: {}", platformBaseApiUrl);
        } else {
            log.warn("[wefitos] 未找到平台配置，platformCode={}", PLATFORM_CODE);
        }
    }

    private List<WefitosCourtProjectListData.CourtProject> fetchCourtProjects(VenueThirdPartyConfig config, ThirdPartyAuthInfo authInfo) {
        try {
            String clubId = getClubIdFromConfig(config);
            String apiBaseUrl = getApiBaseUrl(config);
            String url = apiBaseUrl + API_PATH_COURT_PROJECT_LIST.replace("{clubId}", clubId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("X-Requested-With", "XMLHttpRequest");
            headers.set("Cookie", buildCookieString(authInfo));

            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<WefitosResponse<WefitosCourtProjectListData>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            WefitosResponse<WefitosCourtProjectListData> response = responseEntity.getBody();
            if (response == null || response.getData() == null) {
                return new ArrayList<>();
            }

            if (response.getCn() == null || response.getCn() != 0) {
                return new ArrayList<>();
            }

            List<WefitosCourtProjectListData.CourtProject> projects = response.getData().getList();
            return projects != null ? projects : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private WefitosCourtProjectListData.CourtProject resolveCourtProjectByStartTime(
            List<WefitosCourtProjectListData.CourtProject> projects,
            String startTime) {
        if (projects == null || projects.isEmpty() || startTime == null || startTime.isEmpty()) {
            return null;
        }
        LocalTime t;
        try {
            t = LocalTime.parse(startTime, TIME_FORMATTER);
        } catch (Exception e) {
            return null;
        }
        LocalTime finalT = t;
        return projects.stream()
                .filter(p -> p != null && p.getStartTime() != null && p.getEndTime() != null)
                .filter(p -> {
                    try {
                        LocalTime s = LocalTime.parse(p.getStartTime(), TIME_FORMATTER);
                        // 处理24:00特殊情况
                        String endTimeStr = p.getEndTime();
                        LocalTime e = "24:00".equals(endTimeStr) ? LocalTime.of(23, 59) : LocalTime.parse(endTimeStr, TIME_FORMATTER);
                        return (finalT.equals(s) || finalT.isAfter(s)) && finalT.isBefore(e);
                    } catch (Exception ignore) {
                        return false;
                    }
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * wefixos需要校验是否跨时段预定
     */
    public void validateNoCrossPeriodBooking(VenueThirdPartyConfig config, List<String> startTimes) {
        if (startTimes == null || startTimes.isEmpty()) {
            return;
        }

        ThirdPartyAuthInfo authInfo = tokenService.getAuthInfo(config, this);
        if (authInfo == null) {
            return;
        }

        List<WefitosCourtProjectListData.CourtProject> projects = fetchCourtProjects(config, authInfo);
        if (projects.isEmpty()) {
            return;
        }

        Set<String> selectedProjectIds = startTimes.stream()
                .map(startTime -> resolveCourtProjectByStartTime(projects, startTime))
                .filter(p -> p != null && p.getId() != null && p.getId().getId() != null)
                .map(p -> p.getId().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (selectedProjectIds.size() <= 1) {
            return;
        }

        String periodText = projects.stream()
                .filter(p -> p != null
                        && p.getId() != null
                        && p.getId().getId() != null
                        && selectedProjectIds.contains(p.getId().getId())
                        && p.getName() != null
                        && !p.getName().isEmpty())
                .map(WefitosCourtProjectListData.CourtProject::getName)
                .distinct()
                .collect(Collectors.joining(", "));

        if (periodText.isEmpty()) {
            periodText = projects.stream()
                    .filter(p -> p != null && p.getName() != null && !p.getName().isEmpty())
                    .map(WefitosCourtProjectListData.CourtProject::getName)
                    .distinct()
                    .collect(Collectors.joining(", "));
        }

        String msg = "由于场馆限制,请分别在" + periodText + "时段进行预定,不允许跨时段预定";
        throw new GloboxApplicationException(VenueCode.VENUE_TIME_SLOT_BOOKING_NOT_ALLOWED.getCode(), msg);
    }

    /**
     * 获取API基础地址
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

    /**
     * 从extraConfig中提取clubId和courtProjectId
     */
    private JSONObject parseExtraConfig(VenueThirdPartyConfig config) {
        if (config.getExtraConfig() == null || config.getExtraConfig().isEmpty()) {
            throw new IllegalStateException("Wefitos平台配置缺少extraConfig信息");
        }
        try {
            return JSON.parseObject(config.getExtraConfig());
        } catch (Exception e) {
            throw new IllegalStateException("Wefitos平台extraConfig格式错误: " + e.getMessage());
        }
    }

    /**
     * 从extraConfig中提取clubId
     */
    private String getClubIdFromConfig(VenueThirdPartyConfig config) {
        JSONObject extraConfig = parseExtraConfig(config);
        String clubId = extraConfig.getString("clubId");
        if (clubId == null || clubId.isEmpty()) {
            throw new IllegalStateException("Wefitos平台extraConfig中缺少clubId");
        }
        return clubId;
    }

    /**
     * 获取所有球场项目ID（时段ID）
     * 调用API: /panel/club/{clubId}/court/project/get-court-project-list
     */
    private List<String> fetchCourtProjectIds(VenueThirdPartyConfig config, ThirdPartyAuthInfo tempAuthInfo) {
        try {
            String clubId = getClubIdFromConfig(config);
            String apiBaseUrl = getApiBaseUrl(config);
            String url = apiBaseUrl + API_PATH_COURT_PROJECT_LIST.replace("{clubId}", clubId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Accept", "application/json, text/javascript, */*; q=0.01");
            headers.set("Origin", "https://www.wefitos.com");
            headers.set("Referer", "https://www.wefitos.com/panel/club/" + clubId + "/court/");
            headers.set("X-Requested-With", "XMLHttpRequest");
            String cookieString = buildCookieString(tempAuthInfo);
            headers.set("Cookie", cookieString);
            log.info("[wefitos] fetchCourtProjectIds - 构建Cookie: {}", cookieString);
            log.info("[wefitos] fetchCourtProjectIds - 请求URL: {}", url);

            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<WefitosResponse<WefitosCourtProjectListData>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            WefitosResponse<WefitosCourtProjectListData> response = responseEntity.getBody();
            if (response == null || response.getData() == null) {
                log.warn("[wefitos] 获取courtProjectIds失败: 响应为空");
                return new ArrayList<>();
            }

            if (response.getCn() == null || response.getCn() != 0) {
                log.warn("[wefitos] 获取courtProjectIds失败: code={}, msg={}", response.getCode(), response.getMessage());
                return new ArrayList<>();
            }

            List<String> courtProjectIds = new ArrayList<>();
            List<WefitosCourtProjectListData.CourtProject> projects = response.getData().getList();
            if (projects != null) {
                courtProjectIds = projects.stream()
                        .filter(project -> project.getId() != null && project.getId().getId() != null)
                        .map(project -> project.getId().getId())
                        .collect(Collectors.toList());
            }

            log.info("[wefitos] 获取courtProjectIds成功: 共{}个", courtProjectIds.size());
            return courtProjectIds;

        } catch (Exception e) {
            log.warn("[wefitos] 获取courtProjectIds异常: {}", e.getMessage());
            return new ArrayList<>();
        }
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
            log.error("[wefitos] 查询槽位异常: venueId={}, date={}", config.getVenueId(), date, e);
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
            if (authInfo == null) {
                log.error("[wefitos] 查询槽位失败: 获取认证信息失败");
                return null;
            }

            // 2. 提取clubId和courtProjectIds
            String clubId = getClubIdFromConfig(config);
            List<String> courtProjectIds = authInfo.getCourtProjectIds();

            if (clubId.isEmpty()) {
                log.error("[wefitos] 查询槽位失败: clubId为空");
                return null;
            }

            if (courtProjectIds == null || courtProjectIds.isEmpty()) {
                log.error("[wefitos] 查询槽位失败: courtProjectIds为空");
                return null;
            }

            // 3. 遍历所有courtProjectId查询槽位并合并结果
            String apiBaseUrl = getApiBaseUrl(config);
            String url = apiBaseUrl + API_PATH_RESOURCE_LIST.replace("{clubId}", clubId);
            
            // 用于合并不同时段的槽位，key=courtName（因为wefitos对同一场地在不同时段使用不同的courtId）
            Map<String, ThirdPartyCourtSlotDto> mergedCourts = new LinkedHashMap<>();
            
            for (String courtProjectId : courtProjectIds) {
                log.info("[wefitos] 查询时段槽位: courtProjectId={}, date={}", courtProjectId, date);
                
                // 构建POST请求体
                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                body.add("courtProjectId", courtProjectId);
                body.add("date", date.format(DATE_FORMATTER));

                // 设置请求头
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                headers.set("X-Requested-With", "XMLHttpRequest");
                headers.set("Cookie", buildCookieString(authInfo));

                HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

                try {
                    // 发送POST请求
                    ResponseEntity<WefitosResponse<WefitosResourceListData>> responseEntity = restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            requestEntity,
                            new ParameterizedTypeReference<>() {
                            }
                    );

                    // 解析响应
                    WefitosResponse<WefitosResourceListData> response = responseEntity.getBody();
                    if (response == null || response.getData() == null) {
                        log.warn("[wefitos] 查询时段槽位响应为空: courtProjectId={}", courtProjectId);
                        continue;
                    }

                    if (response.getCn() == null || response.getCn() != 0) {
                        log.warn("[wefitos] 查询时段槽位失败: courtProjectId={}, code={}, msg={}", 
                                courtProjectId, response.getCode(), response.getMessage());
                        continue;
                    }

                    WefitosResourceListData data = response.getData();
                    log.info("[wefitos] 查询时段槽位成功: courtProjectId={}, 场地数={}", 
                            courtProjectId, data.getList() != null ? data.getList().size() : 0);

                    // 转换并合并槽位（按场地名称合并，因为同一场地在不同时段有不同的courtId）
                    List<ThirdPartyCourtSlotDto> periodSlots = convertToCourtSlots(data);
                    for (ThirdPartyCourtSlotDto courtSlot : periodSlots) {
                        String courtName = courtSlot.getCourtName();
                        if (mergedCourts.containsKey(courtName)) {
                            // 合并槽位到已有场地
                            ThirdPartyCourtSlotDto existing = mergedCourts.get(courtName);
                            existing.getSlots().addAll(courtSlot.getSlots());
                        } else {
                            // 新场地，使用第一个时段的courtId
                            mergedCourts.put(courtName, courtSlot);
                        }
                    }
                } catch (Exception e) {
                    log.warn("[wefitos] 查询时段槽位异常: courtProjectId={}, error={}", courtProjectId, e.getMessage());
                }
            }
            
            if (mergedCourts.isEmpty()) {
                log.error("[wefitos] 查询槽位失败: 所有时段都没有返回数据");
                return null;
            }

            List<ThirdPartyCourtSlotDto> result = new ArrayList<>(mergedCourts.values());
            // 对每个场地的槽位按开始时间排序
            result.forEach(court -> {
                court.getSlots().sort(Comparator.comparing(ThirdPartySlotDto::getStartTime));
            });
            
            log.info("[wefitos] 查询槽位完成: venueId={}, date={}, 场地数={}, 总槽位数={}",
                    config.getVenueId(), date, result.size(), 
                    result.stream().mapToInt(c -> c.getSlots().size()).sum());

            return result;

        } catch (Exception e) {
            log.error("[wefitos] 查询槽位异常: venueId={}", config.getVenueId(), e);
            return null;
        }
    }

    /**
     * 查询单个时段的槽位信息（不合并，保留原始courtId）
     * 用于lockSlots中获取正确的period-specific courtId
     */
    private List<ThirdPartyCourtSlotDto> querySinglePeriodSlots(VenueThirdPartyConfig config, LocalDate date,
                                                                  String courtProjectId, ThirdPartyAuthInfo authInfo) {
        try {
            String clubId = getClubIdFromConfig(config);
            String apiBaseUrl = getApiBaseUrl(config);
            String url = apiBaseUrl + API_PATH_RESOURCE_LIST.replace("{clubId}", clubId);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("courtProjectId", courtProjectId);
            body.add("date", date.format(DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("X-Requested-With", "XMLHttpRequest");
            headers.set("Cookie", buildCookieString(authInfo));

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<WefitosResponse<WefitosResourceListData>> responseEntity = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<>() {});

            WefitosResponse<WefitosResourceListData> response = responseEntity.getBody();
            if (response == null || response.getData() == null || response.getCn() == null || response.getCn() != 0) {
                return new ArrayList<>();
            }
            return convertToCourtSlots(response.getData());
        } catch (Exception e) {
            log.error("[wefitos] 查询单时段槽位异常: courtProjectId={}", courtProjectId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 将Wefitos资源转换为统一DTO格式
     */
    private List<ThirdPartyCourtSlotDto> convertToCourtSlots(WefitosResourceListData data) {
        if (data.getList() == null || data.getList().isEmpty()) {
            return new ArrayList<>();
        }

        List<ThirdPartyCourtSlotDto> result = new ArrayList<>();

        for (WefitosResourceListData.WefitosCourtProject project : data.getList()) {
            if (project.getTimeList() == null || project.getTimeList().isEmpty()) {
                continue;
            }

            // 转换时间槽位
            List<ThirdPartySlotDto> slots = project.getTimeList().stream()
                    .map(timeSlot -> {
                        // 状态判断：1=可用, 3=已锁定, 4=已预订, 7=不可用
                        boolean available = timeSlot.getStatus() != null && timeSlot.getStatus() == 1;

                        String lockId = null;
                        String lockRemark = null;
                        if (timeSlot.getStatus() != null && timeSlot.getStatus() == 3) {
                            if (timeSlot.getLock() != null) {
                                if (timeSlot.getLock().getId() != null
                                        && timeSlot.getLock().getId().getId() != null
                                        && !timeSlot.getLock().getId().getId().isEmpty()) {
                                    lockId = timeSlot.getLock().getId().getId();
                                }
                                lockRemark = timeSlot.getLock().getRemark();
                            }
                        }

                        // 获取价格
                        BigDecimal price = BigDecimal.ZERO;
                        if (timeSlot.getPrice() != null && timeSlot.getPrice().getPrice() != null) {
                            try {
                                price = new BigDecimal(timeSlot.getPrice().getPrice());
                            } catch (NumberFormatException e) {
                                log.warn("[wefitos] 价格格式错误: {}", timeSlot.getPrice().getPrice());
                            }
                        }

                        return ThirdPartySlotDto.builder()
                                .startTime(timeSlot.getStart())
                                .endTime(timeSlot.getEnd())
                                .available(available)
                                .price(price)
                                .lockId(lockId)
                                .lockRemark(lockRemark)
                                .build();
                    })
                    .collect(Collectors.toList());

            result.add(ThirdPartyCourtSlotDto.builder()
                    .thirdPartyCourtId(project.getCourtId())
                    .courtName(project.getName())
                    .slots(slots)
                    .build());
        }

        return result;
    }

    @Override
    public LockSlotsResult lockSlots(VenueThirdPartyConfig config, List<SlotLockRequest> slotRequests, String remark) {
        log.info("[wefitos] 锁定槽位开始: venueId={}, slotCount={}", config.getVenueId(), slotRequests.size());

        try {
            // 1. 获取认证信息
            ThirdPartyAuthInfo authInfo = tokenService.getAuthInfo(config, this);
            if (authInfo == null) {
                log.error("[wefitos] 锁定失败: 获取认证信息失败");
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }

            validateNoCrossPeriodBooking(
                    config,
                    slotRequests.stream().map(SlotLockRequest::getStartTime).collect(Collectors.toList())
            );

            String clubId = getClubIdFromConfig(config);
            if (clubId.isEmpty()) {
                log.error("[wefitos] 锁定失败: clubId为空");
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }

            // 2. 获取第一个槽位的日期，解析正确的courtProjectId
            LocalDate firstDate = slotRequests.get(0).getDate();

            // 通过startTime确定正确的courtProjectId（不同时段有不同的courtProjectId）
            List<WefitosCourtProjectListData.CourtProject> projects = fetchCourtProjects(config, authInfo);
            WefitosCourtProjectListData.CourtProject targetProject = resolveCourtProjectByStartTime(
                    projects, slotRequests.get(0).getStartTime());
            if (targetProject == null || targetProject.getId() == null || targetProject.getId().getId() == null) {
                log.error("[wefitos] 锁定失败: 无法解析courtProjectId - startTime={}", slotRequests.get(0).getStartTime());
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }
            String courtProjectId = targetProject.getId().getId();
            log.info("[wefitos] 解析到正确的courtProjectId={}, 时段={}", courtProjectId, targetProject.getName());

            // 3. 查询合并后的槽位信息（用于验证和courtName映射）
            List<ThirdPartyCourtSlotDto> allSlots = querySlotsFromAPI(config, firstDate);
            if (allSlots == null || allSlots.isEmpty()) {
                log.error("[wefitos] 锁定失败: 无法查询槽位信息");
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }

            // 3.5 验证最小预订时长（1小时槽位必须全部选中）
            validateMinBookingDuration(allSlots, slotRequests);

            // 3.6 查询目标时段的未合并槽位（获取该时段的正确courtId）
            List<ThirdPartyCourtSlotDto> periodSlots = querySinglePeriodSlots(config, firstDate, courtProjectId, authInfo);
            if (periodSlots.isEmpty()) {
                log.error("[wefitos] 锁定失败: 目标时段无槽位数据 - courtProjectId={}", courtProjectId);
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }

            // 构建映射表：mergedCourtId -> courtName（从合并数据获取）
            Map<String, String> mergedCourtIdToName = allSlots.stream()
                    .collect(Collectors.toMap(ThirdPartyCourtSlotDto::getThirdPartyCourtId,
                            ThirdPartyCourtSlotDto::getCourtName, (a, b) -> a));

            // 构建映射表：courtName -> 目标时段的courtId（从未合并数据获取）
            Map<String, String> courtNameToPeriodCourtId = periodSlots.stream()
                    .collect(Collectors.toMap(ThirdPartyCourtSlotDto::getCourtName,
                            ThirdPartyCourtSlotDto::getThirdPartyCourtId, (a, b) -> a));

            // 构建映射表：periodCourtId -> 该场地的API槽位列表（原始粒度）
            Map<String, List<ThirdPartySlotDto>> periodCourtSlotMap = periodSlots.stream()
                    .collect(Collectors.toMap(ThirdPartyCourtSlotDto::getThirdPartyCourtId,
                            ThirdPartyCourtSlotDto::getSlots, (a, b) -> a));

            log.info("[wefitos] courtId映射 - mergedCourtIdToName={}, courtNameToPeriodCourtId={}",
                    mergedCourtIdToName, courtNameToPeriodCourtId);

            // 4. 构建锁定请求（合并30分钟槽位回原始API粒度，使用正确的courtId）
            // key = periodCourtId + "_" + apiSlotStartTime，用于去重合并
            Map<String, WefitosLockRequest.CourtWeek> mergedCourtWeeks = new LinkedHashMap<>();
            Map<SlotLockRequest, String> bookingIds = new HashMap<>();
            List<AwaySlotPrice> slotPrices = new ArrayList<>();

            for (SlotLockRequest slot : slotRequests) {
                log.info("[wefitos] 处理锁定请求: dbCourtId={}, startTime={}",
                        slot.getThirdPartyCourtId(), slot.getStartTime());

                // 通过mergedCourtId找到courtName，再找到目标时段的正确courtId
                String courtName = mergedCourtIdToName.get(slot.getThirdPartyCourtId());
                if (courtName == null) {
                    log.warn("[wefitos] 未找到courtName: dbCourtId={}", slot.getThirdPartyCourtId());
                    continue;
                }

                String periodCourtId = courtNameToPeriodCourtId.get(courtName);
                if (periodCourtId == null) {
                    log.warn("[wefitos] 目标时段无此场地: courtName={}, courtProjectId={}", courtName, courtProjectId);
                    continue;
                }

                log.info("[wefitos] courtId映射: dbCourtId={} -> courtName={} -> periodCourtId={}",
                        slot.getThirdPartyCourtId(), courtName, periodCourtId);

                // 查找请求的startTime落在哪个API原始槽位内
                List<ThirdPartySlotDto> apiSlots = periodCourtSlotMap.get(periodCourtId);
                if (apiSlots == null) {
                    log.warn("[wefitos] 未找到场地的槽位数据: periodCourtId={}", periodCourtId);
                    continue;
                }

                LocalTime reqStart = LocalTime.parse(slot.getStartTime());
                ThirdPartySlotDto matchedApiSlot = null;
                for (ThirdPartySlotDto apiSlot : apiSlots) {
                    LocalTime apiStart = LocalTime.parse(apiSlot.getStartTime());
                    String apiEndStr = "24:00".equals(apiSlot.getEndTime()) ? "23:59" : apiSlot.getEndTime();
                    LocalTime apiEnd = LocalTime.parse(apiEndStr);
                    if ((reqStart.equals(apiStart) || reqStart.isAfter(apiStart)) && reqStart.isBefore(apiEnd)) {
                        matchedApiSlot = apiSlot;
                        break;
                    }
                }

                if (matchedApiSlot == null) {
                    log.warn("[wefitos] 未匹配到API槽位: periodCourtId={}, reqStartTime={}", periodCourtId, slot.getStartTime());
                    continue;
                }

                // 合并courtWeek：同一个API槽位只生成一个courtWeek条目
                String mergeKey = periodCourtId + "_" + matchedApiSlot.getStartTime();
                if (!mergedCourtWeeks.containsKey(mergeKey)) {
                    mergedCourtWeeks.put(mergeKey, WefitosLockRequest.CourtWeek.builder()
                            .name(courtName)
                            .date(slot.getDate().format(DATE_FORMATTER))
                            .day(String.valueOf(slot.getDate().getDayOfWeek().getValue() % 7))
                            .courtId(periodCourtId)
                            .startTime(matchedApiSlot.getStartTime())
                            .endTime(matchedApiSlot.getEndTime())
                            .half("0")
                            .remark(remark != null ? remark : "")
                            .build());
                }

                // 计算每个30分钟槽位的价格
                LocalTime apiStart = LocalTime.parse(matchedApiSlot.getStartTime());
                String apiEndStr = "24:00".equals(matchedApiSlot.getEndTime()) ? "23:59" : matchedApiSlot.getEndTime();
                LocalTime apiEnd = LocalTime.parse(apiEndStr);
                int splitCount = TimeSlotSplitUtil.splitTimeSlots(apiStart, apiEnd).size();
                BigDecimal pricePerSlot = splitCount > 1
                        ? matchedApiSlot.getPrice().divide(BigDecimal.valueOf(splitCount), 2, RoundingMode.HALF_UP)
                        : matchedApiSlot.getPrice();

                slotPrices.add(AwaySlotPrice.builder()
                        .startTime(reqStart)
                        .price(pricePerSlot)
                        .thirdPartyCourtId(slot.getThirdPartyCourtId())  // 保持DB courtId用于回映
                        .build());
            }

            List<WefitosLockRequest.CourtWeek> courtWeeks = new ArrayList<>(mergedCourtWeeks.values());

            // 5. 构建锁定请求对象
            WefitosLockRequest lockRequest = WefitosLockRequest.builder()
                    .courtProjectId(courtProjectId)
                    .courtWeek(courtWeeks)
                    .build();

            // 打印锁定请求详情
            log.info("[wefitos] 锁定请求: courtProjectId={}, courtWeeks数量={}", courtProjectId, courtWeeks.size());
            IntStream.range(0, courtWeeks.size()).forEach(i -> {
                WefitosLockRequest.CourtWeek cw = courtWeeks.get(i);
                log.info("[wefitos] 锁定请求[{}]: name={}, courtId={}, startTime={}, endTime={}",
                        i, cw.getName(), cw.getCourtId(), cw.getStartTime(), cw.getEndTime());
            });

            // 6. 发送锁定请求
            String apiBaseUrl = getApiBaseUrl(config);
            String url = apiBaseUrl + API_PATH_LOCK.replace("{clubId}", clubId);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("courtProjectId", courtProjectId);
            // remark作为全局参数，不是courtWeek内的参数（Wefitos平台要求）
            if (remark != null && !remark.isEmpty()) {
                body.add("remark", remark);
            }
            IntStream.range(0, courtWeeks.size()).forEach(i -> {
                WefitosLockRequest.CourtWeek cw = courtWeeks.get(i);
                body.add("courtWeek[" + i + "][name]", cw.getName());
                body.add("courtWeek[" + i + "][date]", cw.getDate());
                body.add("courtWeek[" + i + "][day]", cw.getDay());
                body.add("courtWeek[" + i + "][courtId]", cw.getCourtId());
                body.add("courtWeek[" + i + "][startTime]", cw.getStartTime());
                body.add("courtWeek[" + i + "][endTime]", cw.getEndTime());
                body.add("courtWeek[" + i + "][half]", cw.getHalf());
            });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("X-Requested-With", "XMLHttpRequest");
            headers.set("Cookie", buildCookieString(authInfo));

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            log.info("[wefitos] 锁场API - URL: {}", url);
            log.info("[wefitos] 锁场API - Request: {}", body);

            ResponseEntity<WefitosResponse<Object>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // 7. 解析响应
            WefitosResponse<Object> response = responseEntity.getBody();
            log.info("[wefitos] 锁场API - Response: {}", JSON.toJSONString(response));
            if (response == null || response.getCn() == null || response.getCn() != 0) {
                log.error("[wefitos] 锁定失败: code={}, msg={}",
                        response != null ? response.getCode() : null,
                        response != null ? response.getMessage() : "响应为空");
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }

            // 8. 锁定成功后，重新查询槽位状态来获取锁定ID
            log.info("[wefitos] 锁定请求成功，开始查询锁定后的槽位状态");
            
            // 重新查询当天槽位，获取已锁定槽位的ID
            List<ThirdPartyCourtSlotDto> lockedSlots = querySlotsFromAPI(config, firstDate);
            if (lockedSlots == null || lockedSlots.isEmpty()) {
                log.error("[wefitos] 锁定后查询槽位失败");
                throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
            }
            
            // 匹配锁定的槽位并提取锁定ID（使用范围匹配，因为API返回原始粒度如1小时）
            for (SlotLockRequest slot : slotRequests) {
                boolean found = false;
                for (ThirdPartyCourtSlotDto courtSlot : lockedSlots) {
                    if (courtSlot.getThirdPartyCourtId().equals(slot.getThirdPartyCourtId())) {
                        LocalTime reqStart = LocalTime.parse(slot.getStartTime());
                        for (ThirdPartySlotDto slotDto : courtSlot.getSlots()) {
                            LocalTime apiStart = LocalTime.parse(slotDto.getStartTime());
                            String apiEndStr = "24:00".equals(slotDto.getEndTime()) ? "23:59" : slotDto.getEndTime();
                            LocalTime apiEnd = LocalTime.parse(apiEndStr);
                            // 范围匹配：请求的startTime落在API槽位的[apiStart, apiEnd)内
                            if ((reqStart.equals(apiStart) || reqStart.isAfter(apiStart)) && reqStart.isBefore(apiEnd)
                                    && slotDto.getLockId() != null && !slotDto.getLockId().isEmpty()) {
                                bookingIds.put(slot, slotDto.getLockId());
                                log.info("[wefitos] 找到锁定槽位: courtId={}, reqStartTime={}, apiSlot={}-{}, lockId={}",
                                        courtSlot.getThirdPartyCourtId(), slot.getStartTime(),
                                        slotDto.getStartTime(), slotDto.getEndTime(), slotDto.getLockId());
                                found = true;
                                break;
                            }
                        }
                        if (found) break;
                    }
                }

                if (!found) {
                    log.warn("[wefitos] 未找到锁定槽位: courtId={}, startTime={}",
                            slot.getThirdPartyCourtId(), slot.getStartTime());
                }
            }

            // 清除缓存
            String cacheKey = AwayVenueCacheConstants.buildSlotsCacheKey(config.getVenueId(), firstDate);
            redisService.deleteObject(cacheKey);

            log.info("[wefitos] 锁定成功: venueId={}, count={}, realLockIds={}", 
                    config.getVenueId(), bookingIds.size(), bookingIds.values());

            return LockSlotsResult.builder()
                    .bookingIds(bookingIds)
                    .slotPrices(slotPrices)
                    .build();

        } catch (GloboxApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[wefitos] 锁定异常: venueId={}", config.getVenueId(), e);
            throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
        }
    }

    /**
     * 验证最小预订时长
     * 对于wefitos平台，某些时段的槽位是1小时粒度（如16:00-22:00），系统拆成2个30分钟展示给用户。
     * 用户必须同时选择拆分出来的所有半小时段，否则报错。
     *
     * @param allSlots API返回的原始槽位（未拆分，可能包含1小时粒度）
     * @param slotRequests 用户请求锁定的槽位列表（30分钟粒度）
     */
    private void validateMinBookingDuration(List<ThirdPartyCourtSlotDto> allSlots, List<SlotLockRequest> slotRequests) {
        // 构建用户请求的 (courtId, startTime) 集合，用于快速查找
        Set<String> requestedSlotKeys = slotRequests.stream()
                .map(req -> req.getThirdPartyCourtId() + "_" + req.getStartTime())
                .collect(Collectors.toSet());

        for (ThirdPartyCourtSlotDto courtSlot : allSlots) {
            for (ThirdPartySlotDto apiSlot : courtSlot.getSlots()) {
                LocalTime apiStart = LocalTime.parse(apiSlot.getStartTime());
                String endTimeStr = apiSlot.getEndTime();
                if ("24:00".equals(endTimeStr)) {
                    endTimeStr = "23:59";
                }
                LocalTime apiEnd = LocalTime.parse(endTimeStr);

                // 拆分成30分钟段
                List<TimeSlotSplitUtil.TimeSlot> splitSlots = TimeSlotSplitUtil.splitTimeSlots(apiStart, apiEnd);
                if (splitSlots.size() <= 1) {
                    // 本身就是30分钟槽位，不需要检查
                    continue;
                }

                // 检查用户是否选了这个API槽位的任意一个半小时
                String courtId = courtSlot.getThirdPartyCourtId();
                List<String> splitKeys = splitSlots.stream()
                        .map(s -> courtId + "_" + s.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                        .toList();

                boolean anySelected = splitKeys.stream().anyMatch(requestedSlotKeys::contains);
                boolean allSelected = requestedSlotKeys.containsAll(splitKeys);

                if (anySelected && !allSelected) {
                    log.warn("[wefitos] 最小预订时长校验失败: courtId={}, apiSlot={}-{}, 用户只选了部分半小时",
                            courtId, apiSlot.getStartTime(), apiSlot.getEndTime());
                    throw new GloboxApplicationException(VenueCode.VENUE_MIN_BOOKING_DURATION);
                }
            }
        }
        log.info("[wefitos] 最小预订时长校验通过");
    }

    @Override
    public boolean unlockSlots(VenueThirdPartyConfig config, List<SlotLockRequest> slotRequests) {
        log.info("[wefitos] 解锁槽位开始: venueId={}, slotCount={}", config.getVenueId(), slotRequests.size());

        try {
            // 1. 获取认证信息
            ThirdPartyAuthInfo authInfo = tokenService.getAuthInfo(config, this);
            if (authInfo == null) {
                log.error("[wefitos] 解锁失败: 获取认证信息失败");
                return false;
            }

            String clubId = getClubIdFromConfig(config);
            if (clubId.isEmpty()) {
                log.error("[wefitos] 解锁失败: clubId为空");
                return false;
            }

            // 2. 从请求中提取真实的锁定ID（由锁定时保存）
            LocalDate firstDate = slotRequests.get(0).getDate();
            
            List<String> lockIds = slotRequests.stream()
                    .map(SlotLockRequest::getThirdPartyBookingId)
                    .filter(id -> id != null && !id.isEmpty())
                    .collect(Collectors.toList());

            if (lockIds.isEmpty()) {
                log.error("[wefitos] 解锁失败: 未找到有效的锁定ID");
                return false;
            }
            
            // 获取期望的remark（用于完整匹配，确保只解锁自己锁的槽位）
            String expectedRemark = slotRequests.stream()
                    .map(SlotLockRequest::getThirdPartyRemark)
                    .filter(r -> r != null && !r.isEmpty())
                    .findFirst()
                    .orElse(null);

            if (expectedRemark == null) {
                log.error("[wefitos] 解锁失败: thirdPartyRemark为空, venueId={}", config.getVenueId());
                return false;
            }

            log.info("[wefitos] 解锁槽位: lockIds={}, expectedRemark={}", lockIds, expectedRemark);

            // 2.1 查询当前槽位状态，校验remark是否完整匹配（只解锁我们系统锁定的场地）
            List<ThirdPartyCourtSlotDto> currentSlots = querySlotsFromAPI(config, firstDate);
            if (currentSlots == null || currentSlots.isEmpty()) {
                log.error("[wefitos] 解锁失败: 无法查询当前槽位状态");
                return false;
            }
            
            // 构建lockId到remark的映射
            Map<String, String> lockIdToRemarkMap = new HashMap<>();
            for (ThirdPartyCourtSlotDto courtSlot : currentSlots) {
                for (ThirdPartySlotDto slot : courtSlot.getSlots()) {
                    if (slot.getLockId() != null && !slot.getLockId().isEmpty()) {
                        lockIdToRemarkMap.put(slot.getLockId(), slot.getLockRemark());
                    }
                }
            }
            
            // 过滤出remark完整匹配的lockId（lockId + remark 双重校验）
            List<String> validLockIds = lockIds.stream()
                    .filter(lockId -> {
                        String actualRemark = lockIdToRemarkMap.get(lockId);
                        if (!expectedRemark.equals(actualRemark)) {
                            log.warn("[wefitos] lockId+remark双重校验失败，跳过: lockId={}, 期望remark={}, 实际remark={}", 
                                    lockId, expectedRemark, actualRemark);
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
            
            if (validLockIds.isEmpty()) {
                log.error("[wefitos] 解锁失败: 没有找到remark完整匹配的锁定槽位, expectedRemark={}", expectedRemark);
                return false;
            }
            
            log.info("[wefitos] lockId+remark双重校验通过: validLockIds={}, expectedRemark={}", validLockIds, expectedRemark);

            // 3. 构建解锁请求（使用校验通过的validLockIds）
            String apiBaseUrl = getApiBaseUrl(config);
            String url = apiBaseUrl + API_PATH_UNLOCK.replace("{clubId}", clubId);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            IntStream.range(0, validLockIds.size()).forEach(i -> body.add("ids[" + i + "]", validLockIds.get(i)));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("X-Requested-With", "XMLHttpRequest");
            headers.set("Cookie", buildCookieString(authInfo));

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            log.info("[wefitos] 解锁API - URL: {}", url);
            log.info("[wefitos] 解锁API - Request: {}", body);

            ResponseEntity<WefitosResponse<Object>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // 4. 解析响应
            WefitosResponse<Object> response = responseEntity.getBody();
            log.info("[wefitos] 解锁API - Response: {}", JSON.toJSONString(response));
            if (response == null || response.getCn() == null || response.getCn() != 0) {
                log.error("[wefitos] 解锁失败: code={}, msg={}",
                        response != null ? response.getCode() : null,
                        response != null ? response.getMessage() : "响应为空");
                return false;
            }

            // 清除缓存
            String cacheKey = AwayVenueCacheConstants.buildSlotsCacheKey(config.getVenueId(), firstDate);
            redisService.deleteObject(cacheKey);

            log.info("[wefitos] 解锁成功: venueId={}, count={}", config.getVenueId(), validLockIds.size());
            return true;

        } catch (Exception e) {
            log.error("[wefitos] 解锁异常: venueId={}", config.getVenueId(), e);
            return false;
        }
    }

    @Override
    public ThirdPartyAuthInfo login(VenueThirdPartyConfig config) {
        try {
            // 1. 生成随机数
            String rnd = String.valueOf(System.currentTimeMillis());

            // 2. 构建登录请求体
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("username", config.getUsername());
            body.add("password", config.getPassword());
            body.add("returnEncData", "");
            body.add("rnd", rnd);
            body.add("phone", "");
            body.add("smsCode", "");
            body.add("areaNum", "%2B86");

            // 3. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("X-Requested-With", "XMLHttpRequest");

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            // 4. 发送登录请求
            String apiBaseUrl = getApiBaseUrl(config);
            String loginUrl = apiBaseUrl + API_PATH_LOGIN;

            ResponseEntity<WefitosResponse<WefitosLoginData>> responseEntity = restTemplate.exchange(
                    loginUrl,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            // 5. 解析响应
            WefitosResponse<WefitosLoginData> response = responseEntity.getBody();
            if (response == null || response.getData() == null) {
                log.error("[wefitos] 登录失败: 响应为空");
                return null;
            }

            if (response.getCn() == null || response.getCn() != 0) {
                log.error("[wefitos] 登录失败: code={}, msg={}", response.getCode(), response.getMessage());
                return null;
            }

            // 6. 从响应头获取Set-Cookie
            List<String> setCookies = responseEntity.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (setCookies == null || setCookies.isEmpty()) {
                log.error("[wefitos] 登录失败: 未获取到Set-Cookie");
                return null;
            }

            log.info("[wefitos] 获取到Set-Cookie列表，共{}个", setCookies.size());

            // 7. 提取rnd和panel_sess的值
            String rndValue = "";
            String panelSessValue = "";

            for (String setCookie : setCookies) {
                if (setCookie.startsWith("rnd=")) {
                    rndValue = extractCookieValueFromSetCookie(setCookie);
                    log.info("[wefitos] 提取rnd={}", rndValue);
                } else if (setCookie.startsWith("panel_sess=")) {
                    panelSessValue = extractCookieValueFromSetCookie(setCookie);
                    log.info("[wefitos] 提取panel_sess={}", panelSessValue);
                }
            }

            if (rndValue.isEmpty() || panelSessValue.isEmpty()) {
                log.error("[wefitos] 登录失败: Cookie提取失败, rnd={}, panel_sess={}", rndValue, panelSessValue);
                return null;
            }

            log.info("[wefitos] 登录成功: venueId={}, 已提取Cookie - rnd={}, panel_sess={}", config.getVenueId(), rndValue, panelSessValue);

            // 8. 构建临时认证信息（用于获取courtProjectIds）
            ThirdPartyAuthInfo tempAuthInfo = ThirdPartyAuthInfo.builder()
                    .token(panelSessValue)
                    .adminId(rndValue)
                    .stadiumId("")
                    .build();

            // 9. 获取所有球场项目ID（时段ID）
            List<String> courtProjectIds = fetchCourtProjectIds(config, tempAuthInfo);

            // 10. 构建最终的认证信息并返回

            return ThirdPartyAuthInfo.builder()
                    .token(panelSessValue)
                    .adminId(rndValue)
                    .stadiumId("")
                    .courtProjectIds(courtProjectIds)
                    .build();

        } catch (Exception e) {
            log.error("[wefitos] 登录异常: venueId={}", config.getVenueId(), e);
            return null;
        }
    }

    /**
     * 从Set-Cookie头中提取cookie值（去除额外参数）
     * 例如：rnd=abc123; path=/; Max-Age=5184000 → abc123
     * Cookie值保持URL编码格式，不进行解码
     */
    private String extractCookieValueFromSetCookie(String setCookie) {
        if (setCookie == null || setCookie.isEmpty()) {
            return "";
        }
        String[] parts = setCookie.split(";");
        if (parts.length > 0) {
            String cookiePart = parts[0];
            int equalIndex = cookiePart.indexOf('=');
            if (equalIndex > 0 && equalIndex < cookiePart.length() - 1) {
                return cookiePart.substring(equalIndex + 1);
            }
        }
        return "";
    }

    /**
     * 构建Cookie字符串
     */
    private String buildCookieString(ThirdPartyAuthInfo authInfo) {
        if (authInfo == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (authInfo.getAdminId() != null && !authInfo.getAdminId().isEmpty()) {
            sb.append("rnd=").append(authInfo.getAdminId()).append("; ");
        }
        if (authInfo.getToken() != null && !authInfo.getToken().isEmpty()) {
            sb.append("panel_sess=").append(authInfo.getToken());
        }
        return sb.toString();
    }

    @Override
    public List<AwaySlotPrice> calculatePricing(VenueThirdPartyConfig config, LocalDate date) {
        try {
            List<ThirdPartyCourtSlotDto> courtSlots = querySlots(config, date);
            if (courtSlots == null || courtSlots.isEmpty()) {
                return new ArrayList<>();
            }

            return courtSlots.stream()
                    .filter(courtSlot -> courtSlot.getSlots() != null)
                    .flatMap(courtSlot -> courtSlot.getSlots().stream()
                            .filter(slot -> slot.getAvailable() != null && slot.getAvailable())
                            .flatMap(slot -> {
                                // 将第三方槽位拆分成30分钟的槽位，按比例分配价格
                                // 解决第三方平台返回1小时槽位但本地使用30分钟模板的问题
                                LocalTime slotStart = LocalTime.parse(slot.getStartTime());
                                String endTimeStr = slot.getEndTime();
                                if ("24:00".equals(endTimeStr)) {
                                    endTimeStr = "23:59";
                                }
                                LocalTime slotEnd = LocalTime.parse(endTimeStr);

                                List<TimeSlotSplitUtil.TimeSlot> splitSlots = TimeSlotSplitUtil.splitTimeSlots(slotStart, slotEnd);
                                int halfHourCount = splitSlots.size();
                                if (halfHourCount <= 0) {
                                    halfHourCount = 1;
                                }
                                BigDecimal pricePerHalfHour = slot.getPrice()
                                        .divide(BigDecimal.valueOf(halfHourCount), 2, RoundingMode.HALF_UP);

                                return splitSlots.stream()
                                        .map(splitSlot -> AwaySlotPrice.builder()
                                                .startTime(splitSlot.getStartTime())
                                                .price(pricePerHalfHour)
                                                .thirdPartyCourtId(courtSlot.getThirdPartyCourtId())
                                                .build());
                            })
                    )
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[wefitos] 计算价格异常: venueId={}", config.getVenueId(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public String getPlatformCode() {
        return PLATFORM_CODE;
    }
}
