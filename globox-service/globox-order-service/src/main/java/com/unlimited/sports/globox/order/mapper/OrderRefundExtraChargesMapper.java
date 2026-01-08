package com.unlimited.sports.globox.order.mapper;

import com.unlimited.sports.globox.model.order.entity.OrderRefundExtraCharges;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 针对表【order_refund_extra_charges(退款申请关联的额外费用退款明细表)】的数据库操作Mapper
*/
@Mapper
public interface OrderRefundExtraChargesMapper extends BaseMapper<OrderRefundExtraCharges> {

}




