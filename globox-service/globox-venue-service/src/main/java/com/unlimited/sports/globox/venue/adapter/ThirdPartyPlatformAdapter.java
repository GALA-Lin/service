package com.unlimited.sports.globox.venue.adapter;

import com.unlimited.sports.globox.model.venue.entity.venues.VenueThirdPartyConfig;
import com.unlimited.sports.globox.venue.adapter.dto.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 第三方平台适配器接口
 */
public interface ThirdPartyPlatformAdapter {

    /**
     * 查询指定日期的场地槽位信息
     *
     * @param config 第三方平台配置
     * @param date 查询日期
     * @return 统一格式的场地槽位信息列表
     */
    List<ThirdPartyCourtSlotDto> querySlots(VenueThirdPartyConfig config, LocalDate date);

    /**
     * 批量锁定槽位（一次API调用锁定多个槽位）
     * 支持跨场地锁定：slotRequests 中的 slots 可以来自不同的场地
     * 锁场成功后返回每个槽位对应的第三方预订ID（用于后续解锁）和价格信息
     *
     * @param config 第三方平台配置
     * @param slotRequests 槽位列表，每个槽位包含 {thirdPartyCourtId, startTime, endTime, date}
     * @param remark 备注
     * @return LockSlotsResult - 包含每个槽位对应的第三方预订ID（eventId或batchId）和价格信息
     */
    LockSlotsResult lockSlots(VenueThirdPartyConfig config, List<SlotLockRequest> slotRequests, String remark);

    /**
     * 批量解锁槽位（根据时间段信息查询batchId和orderId进行解锁）
     * 支持跨场地解锁：slotRequests 中的 slots 可以来自不同的场地
     *
     * @param config 第三方平台配置
     * @param slotRequests 要解锁的时间段列表，每个槽位包含 {thirdPartyCourtId, startTime, endTime, date}
     * @return 是否全部解锁成功
     */
    boolean unlockSlots(VenueThirdPartyConfig config, List<SlotLockRequest> slotRequests);

    /**
     * 登录获取认证信息
     *
     * @param config 第三方平台配置（包含username和password）
     * @return 认证信息（包含Token、adminId等）
     */
    ThirdPartyAuthInfo login(VenueThirdPartyConfig config);

    /**
     * 计算Away球场的价格
     * 返回slotDtos中所有可用槽位的详细价格信息
     * 调用方可根据具体场景进行价格聚合和处理
     *
     * @param config 第三方平台配置
     * @return 槽位价格列表（包含开始时间、价格、第三方场地ID）
     */
    List<AwaySlotPrice> calculatePricing(VenueThirdPartyConfig config, LocalDate date);

    /**
     * 获取适配器支持的平台代码
     *
     * @return 平台代码
     */
    String getPlatformCode();
}
