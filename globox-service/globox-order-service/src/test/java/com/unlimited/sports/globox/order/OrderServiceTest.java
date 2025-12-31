package com.unlimited.sports.globox.order;

import com.unlimited.sports.globox.order.mapper.*;
import com.unlimited.sports.globox.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * OrderServiceTest 类用于测试订单服务(OrderService)的各种功能。
 */
@SpringBootTest
public class OrderServiceTest {

    @Autowired
    private OrderServiceImpl orderService;

    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private OrderItemsMapper orderItemsMapper;
    @Autowired
    private OrderExtraChargesMapper orderExtraChargesMapper;
    @Autowired
    private OrderExtraChargeLinksMapper orderExtraChargeLinksMapper;
    @Autowired
    private OrderStatusLogsMapper orderStatusLogsMapper;

    /**
     * createVenueOrderAction 测试
     */
    @Test
    //@Transactional
    //@Rollback
    public void createVenueOrderAction() {
    }

    @Test
    void getOrderPage() {

    }

    @Test
    void getDetails() {
    }
}
