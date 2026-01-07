package com.unlimited.sports.globox.dubbo.coach;

import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.dubbo.coach.dto.*;

/**
 * @since 2026/1/6 14:21
 * 教练模块 - dubbo 接口
 */

public interface CoachDubboService {

    /**
     * 教练预约价格查询（下单前）
     * 锁定时段并返回价格信息
     *
     * @param dto 包含用户ID、教练ID、预约日期和时段记录ID的请求对象
     * @return 返回包含时段报价信息、教练信息等的定价结果对象
     */
    RpcResult<CoachPricingResultDto> quoteCoach(CoachPricingRequestDto dto);

    /**
     * 获取教练快照信息（用于订单详情展示）Y
     *
     * @param dto 包含用户ID、教练ID、时段记录ID等信息的请求对象
     * @return 返回包含教练姓名、服务区域、证书等级、联系方式等快照信息
     */
    RpcResult<CoachSnapshotResultDto> getCoachSnapshot(CoachSnapshotRequestDto dto);

    void unlockCoachSlot(CoachUnlockSlotRequestDto dto);

    //TODO MQ
//    /**
//     * 解锁教练时段（取消订单或订单失败时调用）
//     *
//     * @param dto 包含用户ID、时段记录ID的请求对象
//     */
//    void unlockCoachSlot(CoachUnlockSlotRequestDto dto);
}
