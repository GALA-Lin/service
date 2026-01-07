package com.unlimited.sports.globox.venue.adapter;

import com.unlimited.sports.globox.model.venue.entity.venues.VenueThirdPartyConfig;

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
     * @return 场地槽位信息
     */
    Map<String, Object> querySlots(VenueThirdPartyConfig config, LocalDate date);

    /**
     * 锁定槽位
     *
     * @param config 第三方平台配置
     * @param thirdPartyCourtId 第三方平台的场地ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param date 预订日期
     * @param remark 备注
     * @return 锁场成功后返回的第三方订单ID
     */
    String lockSlot(VenueThirdPartyConfig config, String thirdPartyCourtId,
                   String startTime, String endTime, LocalDate date, String remark);

    /**
     * 解锁槽位
     *
     * @param config 第三方平台配置
     * @param thirdPartyBookingId 第三方平台的订单ID
     * @return 是否解锁成功
     */
    boolean unlockSlot(VenueThirdPartyConfig config, String thirdPartyBookingId);

    /**
     * 登录获取Token
     *
     * @param config 第三方平台配置（包含username和password）
     * @return Token
     */
    String login(VenueThirdPartyConfig config);

    /**
     * 获取适配器支持的平台代码
     *
     * @return 平台代码
     */
    String getPlatformCode();
}
