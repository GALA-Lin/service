package com.unlimited.sports.globox.order;

import com.unlimited.sports.globox.common.enums.order.*;
import com.unlimited.sports.globox.common.utils.IdGenerator;
import com.unlimited.sports.globox.model.order.entity.*;
import com.unlimited.sports.globox.order.mapper.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class OrderDataInitTest {

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

    @Autowired
    private IdGenerator idGenerator;

    @Test
    void initOrderTestData() {
        int orderCount = 10;

        for (int i = 0; i < orderCount; i++) {
            createOneOrder(i);
        }
    }


    private void createOneOrder(int idx) {

        long orderNo = idGenerator.nextId();
        long buyerId = 1L + idx;
        long sellerId = 2000L;
        String sellerName = "测试场馆-" + sellerId;

        /* ================== 1. 插入 orders ================== */
        Orders order = Orders.builder()
                .orderNo(orderNo)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .sellerName(sellerName)
                .sellerType(SellerTypeEnum.VENUE)
                .orderStatus(OrderStatusEnum.PENDING) // PENDING
                .paymentStatus(OrdersPaymentStatusEnum.UNPAID)
                .baseAmount(BigDecimal.ZERO)
                .extraAmount(BigDecimal.ZERO)
                .subtotal(BigDecimal.ZERO)
                .payAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .build();

        ordersMapper.insert(order);

        /* ================== 2. 插入 order_items ================== */
        int itemCount = 2 + idx % 2; // 2 or 3
        BigDecimal baseAmount = BigDecimal.ZERO;

        List<OrderItems> items = new ArrayList<>();

        for (int i = 1; i <= itemCount; i++) {
            BigDecimal unitPrice = new BigDecimal("100.00").add(BigDecimal.valueOf(i * 20));

            OrderItems item = OrderItems.builder()
                    .orderNo(orderNo)
                    .itemType(SellerTypeEnum.VENUE)
                    .resourceId(3000L + i)
                    .resourceName("场地-" + i)
                    .recordId(5000L + i)
                    .bookingDate(LocalDate.now().plusDays(i))
                    .startTime(LocalTime.of(9 + i, 0))
                    .endTime(LocalTime.of(10 + i, 0))
                    .unitPrice(unitPrice)
                    .extraAmount(BigDecimal.ZERO)
                    .subtotal(unitPrice)
                    .refundStatus(RefundStatusEnum.NONE)
                    .build();

            orderItemsMapper.insert(item);
            items.add(item);
            baseAmount = baseAmount.add(unitPrice);
        }

        /* ================== 3. 订单项级额外费用 ================== */
        BigDecimal itemExtraTotal = BigDecimal.ZERO;

        for (OrderItems item : items) {

            // FIX
            BigDecimal fixFee = new BigDecimal("15.00");
            OrderExtraCharges fix = OrderExtraCharges.builder()
                    .orderNo(orderNo)
                    .orderItemId(item.getId())
                    .chargeTypeId(1L)
                    .chargeName("器材费")
                    .chargeMode(ChargeModeEnum.FIXED) // FIXED
                    .fixedValue(fixFee)
                    .chargeAmount(fixFee)
                    .build();
            orderExtraChargesMapper.insert(fix);

            // PERCENTAGE
            BigDecimal percent = new BigDecimal("0.1");
            BigDecimal percentAmount = item.getUnitPrice()
                    .multiply(percent);

            OrderExtraCharges pct = OrderExtraCharges.builder()
                    .orderNo(orderNo)
                    .orderItemId(item.getId())
                    .chargeTypeId(2L)
                    .chargeName("服务费")
                    .chargeMode(ChargeModeEnum.PERCENTAGE)
                    .fixedValue(percent)
                    .chargeAmount(percentAmount)
                    .build();
            orderExtraChargesMapper.insert(pct);

            // links（用于退款）
            orderExtraChargeLinksMapper.insert(
                    OrderExtraChargeLinks.builder()
                            .orderNo(orderNo)
                            .orderItemId(item.getId())
                            .extraChargeId(fix.getId())
                            .chargeMode(fix.getChargeMode())
                            .allocatedAmount(fixFee)
                            .build());

            orderExtraChargeLinksMapper.insert(
                    OrderExtraChargeLinks.builder()
                            .orderNo(orderNo)
                            .orderItemId(item.getId())
                            .extraChargeId(pct.getId())
                            .chargeMode(pct.getChargeMode())
                            .allocatedAmount(percentAmount)
                            .build());

            itemExtraTotal = itemExtraTotal.add(fixFee).add(percentAmount);
        }

        /* ================== 4. 订单级额外费用 ================== */
        BigDecimal orderFix = new BigDecimal("30.00");
        OrderExtraCharges orderFixCh = OrderExtraCharges.builder()
                .orderNo(orderNo)
                .orderItemId(null)
                .chargeTypeId(10L)
                .chargeName("平台服务费")
                .chargeMode(ChargeModeEnum.FIXED)
                .fixedValue(orderFix)
                .chargeAmount(orderFix)
                .build();
        orderExtraChargesMapper.insert(orderFixCh);

        BigDecimal orderPct = new BigDecimal("0.05");
        BigDecimal orderPctAmount = baseAmount
                .multiply(orderPct);

        OrderExtraCharges orderPctCh = OrderExtraCharges.builder()
                .orderNo(orderNo)
                .orderItemId(null)
                .chargeTypeId(11L)
                .chargeName("平台抽成")
                .chargeMode(ChargeModeEnum.PERCENTAGE)
                .fixedValue(orderPct)
                .chargeAmount(orderPctAmount)
                .build();
        orderExtraChargesMapper.insert(orderPctCh);

        /* ================== 5. 更新订单金额 ================== */
        BigDecimal extraTotal = itemExtraTotal.add(orderFix).add(orderPctAmount);
        BigDecimal subtotal = baseAmount.add(extraTotal);

        order.setBaseAmount(baseAmount);
        order.setExtraAmount(extraTotal);
        order.setSubtotal(subtotal);
        order.setPayAmount(subtotal);

        ordersMapper.updateById(order);

        /* ================== 6. 插入状态日志 ================== */
        orderStatusLogsMapper.insert(
                OrderStatusLogs.builder()
                        .orderNo(orderNo)
                        .orderId(order.getId())
                        .action(OrderActionEnum.CREATE) // CREATE
                        .newOrderStatus(OrderStatusEnum.PENDING)
                        .operatorType(OperatorTypeEnum.SYSTEM) // SYSTEM
                        .operatorName(OperatorTypeEnum.MERCHANT.getOperatorTypeName())
                        .remark("初始化测试订单")
                        .build()
        );
    }
}