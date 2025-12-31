package com.unlimited.sports.globox.order.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.dubbo.order.dto.MerchantRefundApplyPageRequestDto;
import com.unlimited.sports.globox.dubbo.order.dto.MerchantRefundApplyPageResultDto;
import com.unlimited.sports.globox.model.order.entity.OrderRefundApply;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 针对表【order_refund_request(订单退款申请表)】的数据库操作Mapper
*/
@Mapper
public interface OrderRefundApplyMapper extends BaseMapper<OrderRefundApply> {

    IPage<MerchantRefundApplyPageResultDto> selectMerchantRefundApplyPage(
            IPage<MerchantRefundApplyPageResultDto> page,
            @Param("dto") MerchantRefundApplyPageRequestDto dto);
}




