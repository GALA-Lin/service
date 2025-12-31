package com.unlimited.sports.globox.order.mapper;

import com.unlimited.sports.globox.model.order.entity.OrderRefundApplyItems;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 针对表【order_refund_apply_items(退款申请-订单项明细表（记录本次申请包含哪些订单项）)】的数据库操作Mapper
*/
@Mapper
public interface OrderRefundApplyItemsMapper extends BaseMapper<OrderRefundApplyItems> {

}




