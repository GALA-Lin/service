package com.unlimited.sports.globox.venue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.unlimited.sports.globox.common.utils.LocalDateUtils;
import com.unlimited.sports.globox.merchant.mapper.CourtMapper;
import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.venue.entity.venues.ThirdPartyPlatform;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueThirdPartyConfig;
import com.unlimited.sports.globox.model.venue.enums.VenueThirdPartyConfigStatusEnum;
import com.unlimited.sports.globox.service.RedisService;
import com.unlimited.sports.globox.venue.adapter.ThirdPartyPlatformAdapter;
import com.unlimited.sports.globox.venue.adapter.ThirdPartyPlatformAdapterFactory;
import com.unlimited.sports.globox.venue.adapter.constant.AwayVenueCacheConstants;
import com.unlimited.sports.globox.venue.adapter.dto.ThirdPartyCourtSlotDto;
import com.unlimited.sports.globox.venue.mapper.ThirdPartyPlatformMapper;
import com.unlimited.sports.globox.venue.mapper.VenueThirdPartyConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Away球场搜索服务
 * 负责并行查询away球场的时段数据，并处理缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AwayVenueSearchService {

    /**
     * 最大并发API请求线程数
     */
    private static final int MAX_CONCURRENT_API_THREADS = 5;

    /**
     * API请求超时时间(秒)
     */
    private static final int API_TIMEOUT_SECONDS = 30;

    @Autowired
    private  VenueThirdPartyConfigMapper venueThirdPartyConfigMapper;

    @Autowired
    private  ThirdPartyPlatformMapper thirdPartyPlatformMapper;

    @Autowired
    private  RedisService redisService;

    @Autowired
    private  ThirdPartyPlatformAdapterFactory adapterFactory;

    @Autowired
    private CourtMapper courtMapper;

    /**
     * 查询所有away球场在指定时间段的不可预订场馆ID
     * 优化策略：先批量从缓存获取，缓存miss的再并发请求API
     *
     * @param date      查询日期
     * @param startTime 查询开始时间
     * @param endTime   查询结束时间
     * @return 不可预订的场馆ID集合
     */
    public Set<Long> getUnavailableAwayVenueIds(LocalDate date, LocalTime startTime, LocalTime endTime) {
        //查询所有away球场配置
        List<VenueThirdPartyConfig> allAwayConfigs = venueThirdPartyConfigMapper.selectList(
                new LambdaQueryWrapper<VenueThirdPartyConfig>()
                        .eq(VenueThirdPartyConfig::getStatus, VenueThirdPartyConfigStatusEnum.NORMAL.getValue()));
        if (allAwayConfigs.isEmpty()) {
            log.info("[AwayVenueSearch] 没有away球场配置");
            return new HashSet<>();
        }

        log.info("[AwayVenueSearch] 开始查询away球场可用性: 总数={}, 日期={}, 时间={}-{}",
                allAwayConfigs.size(), date, startTime, endTime);

        // 先批量从缓存获取槽位数据
        Map<Long, List<ThirdPartyCourtSlotDto>> cachedSlotsMap = batchGetFromCache(allAwayConfigs, date);
        log.info("[AwayVenueSearch] 缓存命中: {}/{}", cachedSlotsMap.size(), allAwayConfigs.size());

        // 统计缓存miss的场馆配置
        List<VenueThirdPartyConfig> cacheMissConfigs = allAwayConfigs.stream()
                .filter(config -> !cachedSlotsMap.containsKey(config.getVenueId()))
                .toList();

        //  并发请求缓存miss的场馆API
        Map<Long, List<ThirdPartyCourtSlotDto>> apiSlotsMap = new HashMap<>();
        if (!cacheMissConfigs.isEmpty()) {
            log.info("[AwayVenueSearch] 缓存miss场馆数: {}, 开始并发请求API", cacheMissConfigs.size());
            apiSlotsMap = batchFetchFromApi(cacheMissConfigs, date);
            log.info("[AwayVenueSearch] API请求完成: 成功={}/{}", apiSlotsMap.size(), cacheMissConfigs.size());
        }

        //合并缓存和API查询结果
        Map<Long, List<ThirdPartyCourtSlotDto>> allSlotsMap = new HashMap<>();
        allSlotsMap.putAll(cachedSlotsMap);
        allSlotsMap.putAll(apiSlotsMap);

        // 批量查询所有away场馆的本地场地，过滤掉本地不存在的第三方场地
        List<Long> awayVenueIds = allAwayConfigs.stream()
                .map(VenueThirdPartyConfig::getVenueId)
                .toList();
        List<Court> allLocalCourts = courtMapper.selectList(
                new LambdaQueryWrapper<Court>()
                        .in(Court::getVenueId, awayVenueIds)
                        .isNotNull(Court::getThirdPartyCourtId)
        );
        // 按venueId分组，构建每个场馆的本地thirdPartyCourtId集合
        Map<Long, Set<String>> localThirdPartyCourtIdsByVenueId = allLocalCourts.stream()
                .collect(Collectors.groupingBy(
                        Court::getVenueId,
                        Collectors.mapping(Court::getThirdPartyCourtId, Collectors.toSet())
                ));

        // 过滤allSlotsMap：只保留本地存在的场地数据
        allSlotsMap.replaceAll((venueId, slots) -> {
            Set<String> localCourtIds = localThirdPartyCourtIdsByVenueId.getOrDefault(venueId, Collections.emptySet());
            List<ThirdPartyCourtSlotDto> filtered = slots.stream()
                    .filter(slot -> localCourtIds.contains(slot.getThirdPartyCourtId()))
                    .toList();
            if (filtered.size() < slots.size()) {
                log.info("[AwayVenueSearch] 过滤本地不存在的场地: venueId={}, 原始={}, 过滤后={}",
                        venueId, slots.size(), filtered.size());
            }
            return filtered;
        });

        // 检查每个场馆的时间段可用性
        Set<Long> unavailableVenueIds = allAwayConfigs.stream()
                .filter(config -> {
                    List<ThirdPartyCourtSlotDto> slots = allSlotsMap.get(config.getVenueId());

                    // 没有槽位数据，标记为不可用（包括查询失败的场馆）
                    if (slots == null || slots.isEmpty()) {
                        log.debug("[AwayVenueSearch] 场馆无槽位数据，标记为不可用: venueId={}", config.getVenueId());
                        return true;
                    }

                    // 检查时间段内是否有可用槽位
                    if (!hasAvailableSlotInTimeRange(slots, startTime, endTime)) {
                        log.debug("[AwayVenueSearch] 时间段内无可用槽位: venueId={}, time={}-{}",
                                config.getVenueId(), startTime, endTime);
                        return true;
                    }

                    return false;
                })
                .map(VenueThirdPartyConfig::getVenueId)
                .collect(Collectors.toSet());

        log.info("[AwayVenueSearch] 查询完成: 不可预订场馆数={}/{}", unavailableVenueIds.size(), allAwayConfigs.size());
        return unavailableVenueIds;
    }

    /**
     * 批量从缓存获取槽位数据
     * 使用批量查询减少网络往返次数
     */
    private Map<Long, List<ThirdPartyCourtSlotDto>> batchGetFromCache(List<VenueThirdPartyConfig> configs, LocalDate date) {
        Map<Long, List<ThirdPartyCourtSlotDto>> resultMap = new HashMap<>();

        if (configs.isEmpty()) {
            return resultMap;
        }

        // 1. 批量构建cacheKey和venueId的映射
        Map<String, Long> keyToVenueIdMap = new HashMap<>();
        List<String> cacheKeys = configs.stream()
                .map(config -> {
                    String cacheKey = AwayVenueCacheConstants.buildSlotsCacheKey(config.getVenueId(), date);
                    keyToVenueIdMap.put(cacheKey, config.getVenueId());
                    return cacheKey;
                })
                .collect(Collectors.toList());

        // 2. 批量从Redis获取
        Map<String, List<ThirdPartyCourtSlotDto>> cachedObjectsMap = redisService.getCacheObjects(
                cacheKeys,
                new TypeReference<>() {
                }
        );

        if (cachedObjectsMap == null || cachedObjectsMap.isEmpty()) {
            return resultMap;
        }

        // 3. 将缓存结果转换为venueId映射
        cachedObjectsMap.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .forEach(entry -> {
                    String cacheKey = entry.getKey();
                    Long venueId = keyToVenueIdMap.get(cacheKey);
                    if (venueId != null) {
                        resultMap.put(venueId, entry.getValue());
                    }
                });

        return resultMap;
    }

    /**
     * 并发请求API获取槽位数据
     * 请求失败的场馆不会出现在返回的Map中
     */
    private Map<Long, List<ThirdPartyCourtSlotDto>> batchFetchFromApi(List<VenueThirdPartyConfig> configs, LocalDate date) {
        Map<Long, List<ThirdPartyCourtSlotDto>> resultMap = new ConcurrentHashMap<>();

        // 使用线程池并发请求
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(configs.size(), MAX_CONCURRENT_API_THREADS));
        List<CompletableFuture<Void>> futures = configs.stream()
                .map(config -> CompletableFuture.runAsync(() -> {
                    try {
                        // 查询平台配置
                        ThirdPartyPlatform platform = thirdPartyPlatformMapper.selectById(config.getThirdPartyPlatformId());
                        if (platform == null) {
                            log.warn("[AwayVenueSearch] 未找到第三方平台配置: thirdPartyPlatformId={}, venueId={}",
                                    config.getThirdPartyPlatformId(), config.getVenueId());
                            return;
                        }

                        // 获取adapter并查询槽位（adapter内部会处理缓存）
                        ThirdPartyPlatformAdapter adapter = adapterFactory.getAdapter(platform.getPlatformCode());
                        List<ThirdPartyCourtSlotDto> slots = adapter.querySlots(config, date);

                        if (slots != null && !slots.isEmpty()) {
                            resultMap.put(config.getVenueId(), slots);
                        }

                    } catch (Exception e) {
                        log.error("[AwayVenueSearch] API请求异常: venueId={}, date={}", config.getVenueId(), date, e);
                        // 请求失败不放入resultMap，调用方会将其视为不可用
                    }
                }, executor))
                .toList();

        // 等待所有请求完成
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(API_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("[AwayVenueSearch] API请求超时", e);
            futures.forEach(f -> f.cancel(true));
        } catch (Exception e) {
            log.error("[AwayVenueSearch] API请求异常", e);
        } finally {
            executor.shutdown();
        }

        return resultMap;
    }

    /**
     * 检查是否存在符合时间范围的可用槽位
     *
     * @param courtSlots   场地槽位列表
     * @param startTime    查询开始时间
     * @param endTime      查询结束时间
     * @return true表示有可用槽位，false表示无可用槽位
     */
    private boolean hasAvailableSlotInTimeRange(List<ThirdPartyCourtSlotDto> courtSlots,
                                                LocalTime startTime,
                                                LocalTime endTime) {
        return courtSlots.stream().anyMatch(courtSlot -> {
            if (courtSlot.getSlots() == null || courtSlot.getSlots().isEmpty()) {
                return false;
            }

            return courtSlot.getSlots().stream().anyMatch(slot -> {
                // 检查槽位是否可用
                if (slot.getAvailable() == null || !slot.getAvailable()) {
                    return false;
                }

                // 解析槽位时间（字符串格式：HH:mm 或 HH:mm:ss）
                LocalTime slotStart = LocalDateUtils.parseLocalTime(slot.getStartTime());
                LocalTime slotEnd = LocalDateUtils.parseLocalTime(slot.getEndTime());

                if (slotStart == null || slotEnd == null) {
                    return false;
                }

                // 判断槽位与查询时间是否有重叠
                // 槽位的结束时间 > 查询开始时间 && 槽位的开始时间 < 查询结束时间
                return slotEnd.isAfter(startTime) && slotStart.isBefore(endTime);
            });
        });
    }


}
