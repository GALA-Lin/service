package com.unlimited.sports.globox.dubbo.merchant;

import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.dubbo.merchant.dto.*;

import java.util.List;

/**
 * 商家模块 - dubbo 接口
 *
 * @author dk
 * @since 2025/12/22 17:47
 */
public interface MerchantDubboService {

    /**
     * 订场订单数据核对与价格计算
     *
     * @param dto 包含用户ID、预定日期以及预定槽位列表的请求对象
     * @return 返回包含槽位报价信息、订单级额外收费信息、来源平台标识、提供方名称及ID的定价结果对象
     */
    RpcResult<PricingResultDto> quoteVenue(PricingRequestDto dto);


    RpcResult<PricingActivityResultDto> quoteVenueActivity(PricingActivityRequestDto dto);

    /**
     * 获取场地快照信息。
     *
     * @param dto 包含用户ID、场地ID、预定槽位列表以及经纬度的请求对象
     * @return 返回包含场地名称、电话、所在区域、地址、距离（单位：米）、设施列表和场地快照列表的结果对象
     */
    RpcResult<VenueSnapshotResultDto> getVenueSnapshot(VenueSnapshotRequestDto dto);


    /**
     * 获取揽月的 场馆 id 列表
     * @return 揽月场馆 id 列表
     */
    RpcResult<List<Long>> getMoonCourtIdList();
}
