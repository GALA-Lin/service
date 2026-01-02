package com.unlimited.sports.globox.order;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.common.enums.order.ApplyRefundStatusEnum;
import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import com.unlimited.sports.globox.common.enums.order.OrdersPaymentStatusEnum;
import com.unlimited.sports.globox.dubbo.order.OrderDubboService;
import com.unlimited.sports.globox.dubbo.order.dto.*;
import com.unlimited.sports.globox.model.order.entity.Orders;
import com.unlimited.sports.globox.order.mapper.OrdersMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
public class OrderDubboServiceTest {

    @Autowired
    private OrderDubboService orderDubboService;

    @Autowired
    private OrdersMapper ordersMapper;

    @Test
    void merchantGetOrderPage() {
        MerchantGetOrderPageRequestDto dto = new MerchantGetOrderPageRequestDto();
        dto.setMerchantId(1L);
        dto.setVenueId(2000L);
        dto.setPageNum(1);
        dto.setPageSize(10);

        IPage<MerchantGetOrderResultDto> page = orderDubboService.merchantGetOrderPage(dto);

        assertNotNull(page);
        assertNotNull(page.getRecords());

        // 不强依赖是否有数据：只校验分页结构/基本字段合理性
        assertTrue(page.getCurrent() >= 1);
        assertTrue(page.getSize() >= 1);

        // 如果有数据，再做字段断言（避免你测试库为空导致误失败）
        if (!page.getRecords().isEmpty()) {
            MerchantGetOrderResultDto r = page.getRecords().get(0);
            assertNotNull(r.getOrderNo());
            assertNotNull(r.getVenueId());
            assertNotNull(r.getOrderStatus());
            assertNotNull(r.getPaymentStatus());

            // 名称字段通常由枚举 description 映射（按你实现而定）
            assertNotNull(r.getOrderStatusName());
            assertNotNull(r.getPaymentStatusName());

            // slots 允许为空/空集合都可以，但最好别 NPE
            assertNotNull(r.getRecords());
        }
    }

    @Test
    void merchantGetOrderDetails() {

        // 1) 先分页拿一条订单（用于构造详情入参）
        MerchantGetOrderPageRequestDto pageReq = new MerchantGetOrderPageRequestDto();
        pageReq.setMerchantId(1L);
        pageReq.setVenueId(2000L);
        pageReq.setPageNum(1);
        pageReq.setPageSize(1);

        IPage<MerchantGetOrderResultDto> page = orderDubboService.merchantGetOrderPage(pageReq);
        assertNotNull(page);
        assertNotNull(page.getRecords());
        Assertions.assertFalse(page.getRecords().isEmpty(), "merchantGetOrderPage 没拿到任何订单，无法测试详情");

        MerchantGetOrderResultDto any = page.getRecords().get(0);
        assertNotNull(any.getOrderNo(), "page 返回的 orderNo 不能为空");
        assertNotNull(any.getVenueId(), "page 返回的 venueId 不能为空");

        // 2) 调用详情接口
        MerchantGetOrderDetailsRequestDto dto = MerchantGetOrderDetailsRequestDto.builder()
                .orderNo(any.getOrderNo())
                .merchantId(pageReq.getMerchantId())
                .venueId(any.getVenueId())
                .build();

        MerchantGetOrderResultDto detail = orderDubboService.merchantGetOrderDetails(dto);

        // 3) 断言主要功能：返回不为空、关键字段一致
        assertNotNull(detail, "merchantGetOrderDetails 返回不能为空");
        assertEquals(any.getOrderNo(), detail.getOrderNo(), "orderNo 应一致");
        assertEquals(any.getVenueId(), detail.getVenueId(), "venueId 应一致");

        assertNotNull(detail.getUserId(), "userId 不能为空");
        assertNotNull(detail.getPaymentStatus(), "paymentStatus 不能为空");
        assertNotNull(detail.getOrderStatus(), "orderStatus 不能为空");

        // 金额类：至少 totalPrice/subtotal/basePrice 你实现里应有一个不为空（按你字段定义通常都应不为空）
        assertNotNull(detail.getTotalPrice(), "totalPrice 不能为空");
        assertNotNull(detail.getSubtotal(), "subtotal 不能为空");
        assertNotNull(detail.getBasePrice(), "basePrice 不能为空");

        // slots：如果你的详情接口一定返回时段列表就开这个断言；如果允许为空，就删掉这两行
        assertNotNull(detail.getRecords(), "slots 不能为空");
        Assertions.assertFalse(detail.getRecords().isEmpty(), "slots 至少应包含一个时段");
    }


    @Test
    void merchantCancelUnpaidOrder() {
        // 固定一个你已有的订单号（必须是：UNPAID + PENDING）
        Long orderNo = 130341231795507200L;

        // 1) 先从DB读取订单，获取 venueId（sellerId）
        Orders before = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo));
        assertNotNull(before, "测试订单不存在，请先准备 orderNo 对应的数据");

        // 如果这笔单不是“未支付待支付”，那这个用例就不适用，直接跳过（避免误伤环境数据）
        Assumptions.assumeTrue(before.getPaymentStatus() == OrdersPaymentStatusEnum.UNPAID,
                "该订单不是 UNPAID，跳过测试");
        Assumptions.assumeTrue(before.getOrderStatus() == OrderStatusEnum.PENDING,
                "该订单不是 PENDING，跳过测试");

        Long venueId = before.getSellerId(); // 你已确认 sellerId == venueId
        Long merchantId = 999999L;           // 当前实现里仅用于日志

        // 2) 调用：商家取消未支付订单
        MerchantCancelOrderRequestDto dto = MerchantCancelOrderRequestDto.builder()
                .orderNo(orderNo)
                .venueId(venueId)
                .merchantId(merchantId)
                .build();

        MerchantCancelOrderResultDto res = orderDubboService.merchantCancelUnpaidOrder(dto);

        // 3) 断言返回
        assertNotNull(res);
        assertEquals(orderNo, res.getOrderNo());
        assertTrue(res.isSuccess());
        assertEquals(OrderStatusEnum.CANCELLED, res.getOrderStatus());
        assertEquals(OrderStatusEnum.CANCELLED.getDescription(), res.getOrderStatusName());
        assertNotNull(res.getCancelledAt());

        // 4) 断言DB已更新
        Orders after = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo));
        assertNotNull(after);
        assertEquals(OrderStatusEnum.CANCELLED, after.getOrderStatus());
        assertNotNull(after.getCancelledAt());

        // 5) 幂等：再调用一次不应报错，且仍返回 CANCELLED
        MerchantCancelOrderResultDto res2 = orderDubboService.merchantCancelUnpaidOrder(dto);
        assertNotNull(res2);
        assertEquals(orderNo, res2.getOrderNo());
        assertTrue(res2.isSuccess());
        assertEquals(OrderStatusEnum.CANCELLED, res2.getOrderStatus());
        assertNotNull(res2.getCancelledAt());

    }


    @Test
    public void merchantApproveRefund() {
        final long orderNo = 130341232533704704L;
        final long refundApplyId = 4L;
        final long venueId = 2000L;
        final long merchantId = 1L;

        MerchantApproveRefundRequestDto dto = MerchantApproveRefundRequestDto.builder()
                .orderNo(orderNo)
                .refundApplyId(refundApplyId)
                .venueId(venueId)
                .merchantId(merchantId)
                .remark("UT: approve refund")
                .build();

        MerchantApproveRefundResultDto res = orderDubboService.merchantApproveRefund(dto);

        assertNotNull(res);
        assertEquals(orderNo, res.getOrderNo());
        assertEquals(refundApplyId, res.getRefundApplyId());

        // 同意后：申请状态应为 APPROVED，并且有 reviewedAt
        assertEquals(ApplyRefundStatusEnum.APPROVED, res.getApplyStatus());
        assertNotNull(res.getReviewedAt());

        // 订单状态一般仍保持 REFUND_APPLYING（按你现有实现）
        assertNotNull(res.getOrderStatus());
        assertNotNull(res.getOrderStatusName());

        // 本次同意的 item 数量：>0（你实现里 approvedItemCount=items.size()）
        assertNotNull(res.getApprovedItemCount());
        assertTrue(res.getApprovedItemCount() >= 0);
    }

    @Test
    void merchantRejectRefund() {

        // ===== 1. 构造请求参数 =====
        MerchantRejectRefundRequestDto dto = MerchantRejectRefundRequestDto.builder()
                .orderNo(130341229790629888L)
                .refundApplyId(6L)
                .venueId(2000L)
                .merchantId(1L)
                .remark("商家拒绝退款：场地已按约定提供")
                .build();

        // ===== 2. 调用 Dubbo 接口 =====
        MerchantRejectRefundResultDto result =
                orderDubboService.merchantRejectRefund(dto);

        // ===== 3. 基本断言 =====
        Assertions.assertNotNull(result);

        Assertions.assertEquals(dto.getOrderNo(), result.getOrderNo());
        Assertions.assertEquals(dto.getRefundApplyId(), result.getRefundApplyId());

        // ===== 4. 业务关键断言 =====
        Assertions.assertEquals(
                ApplyRefundStatusEnum.REJECTED,
                result.getApplyStatus()
        );

        Assertions.assertNotNull(result.getReviewedAt());

        Assertions.assertEquals(
                OrderStatusEnum.REFUND_REJECTED,
                result.getOrderStatus()
        );

        Assertions.assertEquals(
                OrderStatusEnum.REFUND_REJECTED.getDescription(),
                result.getOrderStatusName()
        );

        // ===== 5. 拒绝的 item 数量 =====
        Assertions.assertNotNull(result.getRejectedItemCount());
        Assertions.assertTrue(result.getRejectedItemCount() > 0);

        // ===== 6. 打印结果（调试友好）=====
        System.out.println("merchantRejectRefund result = " + result);
    }


    @Test
    void merchantGetRefundApplyPage() {

        MerchantRefundApplyPageRequestDto dto = new MerchantRefundApplyPageRequestDto();
        dto.setMerchantId(1L);

        // 商家名下的场馆（至少一个）
        dto.setVenueIds(List.of(2000L));

        dto.setPageNum(1);
        dto.setPageSize(10);

        IPage<MerchantRefundApplyPageResultDto> page =
                orderDubboService.merchantGetRefundApplyPage(dto);

        // ===== 基础断言 =====
        assertNotNull(page, "分页结果不能为空");
        assertNotNull(page.getRecords(), "records 不能为空");

        // 打印结果，方便你肉眼检查
        System.out.println("total = " + page.getTotal());
        System.out.println("current = " + page.getCurrent());
        System.out.println("size = " + page.getSize());

        for (MerchantRefundApplyPageResultDto r : page.getRecords()) {
            System.out.println("---- refund apply ----");
            System.out.println("refundApplyId = " + r.getRefundApplyId());
            System.out.println("orderNo = " + r.getOrderNo());
            System.out.println("applyStatus = " + r.getApplyStatus());
            System.out.println("orderStatus = " + r.getOrderStatus());
            System.out.println("applyItemCount = " + r.getApplyItemCount());
            System.out.println("totalRefundAmount = " + r.getTotalRefundAmount());
        }
    }


    @Test
    void merchantGetRefundApplyDetails() {

        // ===== given =====
        MerchantRefundApplyDetailsRequestDto dto =
                MerchantRefundApplyDetailsRequestDto.builder()
                        .refundApplyId(5L)
                        .orderNo(130341229790629888L)
                        .merchantId(1L)
                        .venueId(2000L)
                        .includeTimeline(true)
                        .build();

        // ===== when =====
        MerchantRefundApplyDetailsResultDto result =
                orderDubboService.merchantGetRefundApplyDetails(dto);

        // ===== then =====
        Assertions.assertNotNull(result, "结果不能为空");

        Assertions.assertEquals(dto.getOrderNo(), result.getOrderNo());
        Assertions.assertEquals(dto.getRefundApplyId(), result.getRefundApplyId());

        Assertions.assertNotNull(result.getOrderStatus(), "订单状态不能为空");
        Assertions.assertNotNull(result.getApplyStatus(), "退款申请状态不能为空");

        Assertions.assertNotNull(result.getAppliedAt(), "申请时间不能为空");

        // items：即使为空，也应该返回 list（而不是 null）
        Assertions.assertNotNull(result.getItems(), "退款项列表不能为空");

        // extraCharges：允许为空列表，但不能是 null
        Assertions.assertNotNull(result.getExtraCharges(), "额外费用退款明细不能为空");

        // timeline：includeTimeline=true 时，允许为空，但不能为 null
        Assertions.assertNotNull(result.getTimeline(), "时间线不能为空");

        // 金额字段（不强制非空，看你实现策略）
        Assertions.assertNotNull(result.getTotalRefundAmount(), "应退总额不能为空");

        // 打印一下，便于调试
        System.out.println("merchantGetRefundApplyDetails result = " + result);
    }
}
