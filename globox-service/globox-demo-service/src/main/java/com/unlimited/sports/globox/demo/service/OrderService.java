package com.unlimited.sports.globox.demo.service;

import com.unlimited.sports.globox.model.demo.vo.CreateVenueOrderRequestVO;

/**
 * 订单接口 - 测试 seata
 * 测试 seata
 *
 * @author dk
 * @since 2025/12/20 08:48
 */
public interface OrderService {

    String create(CreateVenueOrderRequestVO createVenueOrderRequestVO);
}
