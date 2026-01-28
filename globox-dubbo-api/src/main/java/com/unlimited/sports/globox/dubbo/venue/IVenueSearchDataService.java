package com.unlimited.sports.globox.dubbo.venue;

import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.model.venue.vo.VenueSyncVO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 场馆搜索数据RPC服务接口
 * 包含场馆预订可用性查询和数据增量同步功能
 */
public interface IVenueSearchDataService {

    /**
     * 查询指定日期时间段内不可用的场馆ID列表
     *
     * 业务逻辑：
     * 1. 查询VenueBookingSlotRecord中该日期的startTime-endTime时段是否都被占满
     * 2. 查询AwayVenueSearchService中的第三方场馆的预订情况
     * 3. 返回这两种情况中不可用的场馆ID列表
     *
     * @param bookingDate 预订日期
     * @param startTime 开始时间（格式：HH:mm）
     * @param endTime 结束时间（格式：HH:mm）
     * @return 不可用的场馆ID列表
     */
    RpcResult<List<Long>> getUnavailableVenueIds(LocalDate bookingDate, LocalTime startTime, LocalTime endTime);

    /**
     * 增量同步场馆数据
     *
     * @param updatedTime 上一次同步的时间戳，为空表示同步全部数据，不为空表示同步该时间之后的数据
     * @return 同步的场馆数据列表（VenueSyncVO格式）
     */
    RpcResult<List<VenueSyncVO>> syncVenueData(LocalDateTime updatedTime);
}
