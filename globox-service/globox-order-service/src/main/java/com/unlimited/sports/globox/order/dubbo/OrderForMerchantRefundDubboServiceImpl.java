package com.unlimited.sports.globox.order.dubbo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.common.enums.order.*;
import com.unlimited.sports.globox.common.lock.RedisLock;
import com.unlimited.sports.globox.common.result.OrderCode;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.dubbo.order.OrderForMerchantRefundDubboService;
import com.unlimited.sports.globox.dubbo.order.dto.*;
import com.unlimited.sports.globox.model.order.entity.*;
import com.unlimited.sports.globox.model.order.vo.RefundTimelineVo;
import com.unlimited.sports.globox.order.constants.RedisConsts;
import com.unlimited.sports.globox.order.mapper.*;
import com.unlimited.sports.globox.order.service.OrderDubboService;
import com.unlimited.sports.globox.order.service.OrderRefundActionService;
import com.unlimited.sports.globox.order.service.OrderRefundService;
import io.seata.spring.annotation.GlobalTransactional;
import io.seata.tm.api.transaction.Propagation;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单服务对商家服务提供的退款方面 rpc 接口
 */
@Slf4j
@Component
@DubboService(group = "rpc")
public class OrderForMerchantRefundDubboServiceImpl implements OrderForMerchantRefundDubboService {

    @Autowired
    private OrderRefundService orderRefundService;

    @Autowired
    private OrderRefundActionService orderRefundActionService;

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderItemsMapper orderItemsMapper;

    @Autowired
    private OrderExtraChargesMapper orderExtraChargesMapper;

    @Autowired
    private OrderExtraChargeLinksMapper orderExtraChargeLinksMapper;

    @Autowired
    private OrderRefundApplyMapper orderRefundApplyMapper;

    @Autowired
    private OrderRefundExtraChargesMapper orderRefundExtraChargesMapper;

    @Autowired
    private OrderItemRefundsMapper orderItemRefundsMapper;

    @Autowired
    private OrderRefundApplyItemsMapper orderRefundApplyItemsMapper;

    @Autowired
    private OrderStatusLogsMapper orderStatusLogsMapper;

    @Autowired
    private MQService mqService;
    @Autowired
    private OrderDubboService orderDubboService;


    /**
     * 商家同意退款的处理方法。
     *
     * @param dto 包含订单号、退款申请ID、场馆ID和商家ID等信息的请求参数
     * @return 返回商家同意退款的结果，包括订单状态、退款申请状态等信息
     */
    @Override
    @RedisLock(value = "#dto.orderNo", prefix = RedisConsts.ORDER_LOCK_KEY_PREFIX)
    @Transactional(rollbackFor = Exception.class)
    public RpcResult<SellerApproveRefundResultDto> approveRefund(MerchantApproveRefundRequestDto dto) {

        Long orderNo = dto.getOrderNo();
        Long refundApplyId = dto.getRefundApplyId();
        Long venueId = dto.getVenueId();
        Long merchantId = dto.getMerchantId();
        LocalDateTime now = LocalDateTime.now();

        // 1) 锁订单（防并发取消/支付/重复审批）
        Orders order = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo)
                        .eq(Orders::getSellerId, venueId)
                        .last("FOR UPDATE"));
        if (ObjectUtils.isEmpty(order)) {
            return RpcResult.error(OrderCode.ORDER_NOT_EXIST);
        }
        if (!order.getOrderStatus().equals(OrderStatusEnum.REFUND_APPLYING)) {
            return RpcResult.error(OrderCode.ORDER_REFUND_APPLY_NOT_EXIST);
        }
        // 商家只能取消场地订单和场地活动订单
        if (order.getSellerType() != SellerTypeEnum.VENUE) {
            return RpcResult.error(OrderCode.ORDER_NOT_EXIST);
        }

        // 2) 锁退款申请（必须属于该订单）
        OrderRefundApply apply = orderRefundApplyMapper.selectOne(
                Wrappers.<OrderRefundApply>lambdaQuery()
                        .eq(OrderRefundApply::getId, refundApplyId)
                        .eq(OrderRefundApply::getOrderNo, orderNo)
                        .last("FOR UPDATE"));
        if (ObjectUtils.isEmpty(apply)) {
            return RpcResult.error(OrderCode.ORDER_REFUND_APPLY_NOT_EXIST);
        }

        // 3) 幂等：已同意直接返回
        if (apply.getApplyStatus() == ApplyRefundStatusEnum.APPROVED) {
            SellerApproveRefundResultDto resultDto = SellerApproveRefundResultDto.builder()
                    .orderNo(orderNo)
                    .refundApplyId(refundApplyId)
                    .applyStatus(apply.getApplyStatus())
                    .reviewedAt(apply.getReviewedAt())
                    .orderStatus(order.getOrderStatus())
                    .orderStatusName(order.getOrderStatus().getDescription())
                    .approvedItemCount(0)
                    .build();
            return RpcResult.ok(resultDto);
        }

        // 只允许 PENDING -> APPROVED
        if (apply.getApplyStatus() != ApplyRefundStatusEnum.PENDING) {
            return RpcResult.error(OrderCode.ORDER_REFUND_APPLY_STATUS_NOT_ALLOW);
        }

        // 4) 取出本次申请的 itemId（用 apply_items 精确绑定）
        List<OrderRefundApplyItems> applyItems = orderRefundApplyItemsMapper.selectList(
                Wrappers.<OrderRefundApplyItems>lambdaQuery()
                        .eq(OrderRefundApplyItems::getRefundApplyId, refundApplyId)
                        .eq(OrderRefundApplyItems::getOrderNo, orderNo)
                        .orderByAsc(OrderRefundApplyItems::getId));
        if (ObjectUtils.isEmpty(applyItems)) {
            return RpcResult.error(OrderCode.ORDER_ITEM_NOT_EXIST);
        }

        List<Long> applyItemIds = applyItems.stream()
                .map(OrderRefundApplyItems::getOrderItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ObjectUtils.isEmpty(applyItemIds)) {
            return RpcResult.error(OrderCode.ORDER_ITEM_NOT_EXIST);
        }

        // 5) 查订单项并校验：必须属于该订单，且状态只能是 WAIT_APPROVING 或 APPROVED（幂等/重试安全）
        List<OrderItems> items = orderItemsMapper.selectList(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, orderNo)
                        .in(OrderItems::getId, applyItemIds)
                        .orderByAsc(OrderItems::getId));
        if (items.size() != applyItemIds.size()) {
            return RpcResult.error(OrderCode.ORDER_ITEM_NOT_EXIST);
        }

        for (OrderItems it : items) {
            RefundStatusEnum st = it.getRefundStatus();
            if (ObjectUtils.isEmpty(st)) {
                return RpcResult.error(OrderCode.ORDER_ITEM_REFUND_STATUS_INVALID);
            }
            if (st != RefundStatusEnum.WAIT_APPROVING && st != RefundStatusEnum.APPROVED) {
                return RpcResult.error(OrderCode.ORDER_ITEM_REFUND_STATUS_INVALID);
            }
        }

        int itemCount = orderRefundActionService.refundAction(orderNo,
                refundApplyId,
                false,
                merchantId,
                OperatorTypeEnum.USER,
                SellerTypeEnum.VENUE);

        SellerApproveRefundResultDto resultDto = SellerApproveRefundResultDto.builder()
                .orderNo(orderNo)
                .refundApplyId(refundApplyId)
                .applyStatus(ApplyRefundStatusEnum.APPROVED)
                .reviewedAt(now)
                .orderStatus(OrderStatusEnum.REFUNDING)
                .orderStatusName(OrderStatusEnum.REFUNDING.getDescription())
                .approvedItemCount(itemCount)
                .build();
        return RpcResult.ok(resultDto);
    }


    @Override
    public RpcResult<SellerRejectRefundResultDto> rejectRefund(MerchantRejectRefundRequestDto dto) {
        return orderDubboService.rejectRefund(dto.getOrderNo(),
                dto.getRefundApplyId(),
                dto.getVenueId(),
                dto.getMerchantId(),
                SellerTypeEnum.VENUE,
                dto.getRemark());
    }

    @Override
    public RpcResult<IPage<MerchantRefundApplyPageResultDto>> getRefundApplyPage(
            MerchantRefundApplyPageRequestDto dto) {

        Page<MerchantRefundApplyPageResultDto> page = new Page<>(dto.getPageNum(), dto.getPageSize());

        // 1) 先分页查“申请单主信息”（必须 join orders 才能按 venueIds 过滤，避免丢页）
        IPage<MerchantRefundApplyPageResultDto> resPage =
                orderRefundApplyMapper.selectMerchantRefundApplyPage(page, dto);

        List<MerchantRefundApplyPageResultDto> records = resPage.getRecords();
        if (records == null || records.isEmpty()) {
            return RpcResult.ok(resPage);
        }

        // 2) 批量准备 key 集合
        List<Long> refundApplyIds = records.stream()
                .map(MerchantRefundApplyPageResultDto::getRefundApplyId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Long> orderNos = records.stream()
                .map(MerchantRefundApplyPageResultDto::getOrderNo)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 3) 申请项明细：applyId -> itemIds + count
        List<OrderRefundApplyItems> applyItems = orderRefundApplyItemsMapper.selectList(
                Wrappers.<OrderRefundApplyItems>lambdaQuery()
                        .in(OrderRefundApplyItems::getRefundApplyId, refundApplyIds));

        Map<Long, List<Long>> applyIdToItemIds = applyItems.stream()
                .collect(Collectors.groupingBy(
                        OrderRefundApplyItems::getRefundApplyId,
                        Collectors.mapping(OrderRefundApplyItems::getOrderItemId, Collectors.toList())));

        Map<Long, Integer> applyIdToItemCount = new HashMap<>();
        applyIdToItemIds.forEach((k, v) -> applyIdToItemCount.put(k,
                v == null ? 0 : (int) v.stream().distinct().count()));

        // 4) 拉取本页涉及的所有 item（用于计算 item 金额）
        List<Long> allItemIds = applyItems.stream()
                .map(OrderRefundApplyItems::getOrderItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, OrderItems> itemMap = allItemIds.isEmpty()
                ? Map.of()
                : orderItemsMapper.selectBatchIds(allItemIds).stream()
                .collect(Collectors.toMap(OrderItems::getId, it -> it, (a, b) -> a));

        // 5) 计算 item 级额外费用（非 SLOT extra）：按 links.allocated_amount，且只认 “订单项级费用”（charge.order_item_id != null）
        Map<Long, BigDecimal> itemIdToExtraSum = new HashMap<>();
        if (!allItemIds.isEmpty()) {
            List<OrderExtraChargeLinks> links = orderExtraChargeLinksMapper.selectList(
                    Wrappers.<OrderExtraChargeLinks>lambdaQuery()
                            .in(OrderExtraChargeLinks::getOrderItemId, allItemIds));

            if (links != null && !links.isEmpty()) {
                List<Long> chargeIds = links.stream()
                        .map(OrderExtraChargeLinks::getExtraChargeId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

                Map<Long, OrderExtraCharges> chargeMap = chargeIds.isEmpty()
                        ? Map.of()
                        : orderExtraChargesMapper.selectBatchIds(chargeIds).stream()
                        .collect(Collectors.toMap(OrderExtraCharges::getId, ch -> ch, (a, b) -> a));

                for (OrderExtraChargeLinks lk : links) {
                    OrderExtraCharges ch = chargeMap.get(lk.getExtraChargeId());
                    if (ch == null) continue;

                    // 只统计 “订单项级额外费用”（order_extra_charges.order_item_id != null）
                    if (ch.getOrderItemId() == null) continue;

                    BigDecimal amt = lk.getAllocatedAmount() == null ? BigDecimal.ZERO : lk.getAllocatedAmount();
                    itemIdToExtraSum.merge(lk.getOrderItemId(), amt, BigDecimal::add);
                }
            }
        }

        // 6) 判断“是否整单申请”：applyItemCount == 订单 item 总数
        //    （仅用于展示 totalRefundAmount 时把订单级 extra 加进去；实际是否退订单级 extra 你在审批环节控制）
        Map<Long, Long> orderNoToTotalItemCount = new HashMap<>();
        if (!orderNos.isEmpty()) {
            // select order_no, count(1) as cnt from order_items where order_no in (...) and deleted=0 group by order_no
            List<Map<String, Object>> maps = orderItemsMapper.selectMaps(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<OrderItems>()
                            .select("order_no", "COUNT(1) AS cnt")
                            .in("order_no", orderNos)
                            .eq("deleted", 0)
                            .groupBy("order_no"));
            for (Map<String, Object> m : maps) {
                Long ono = ((Number) m.get("order_no")).longValue();
                Long cnt = ((Number) m.get("cnt")).longValue();
                orderNoToTotalItemCount.put(ono, cnt);
            }
        }

        // 7) 订单级额外费用：sum(order_extra_charges.charge_amount where order_item_id is null)
        Map<Long, BigDecimal> orderNoToOrderLevelExtraSum = new HashMap<>();
        if (!orderNos.isEmpty()) {
            List<Map<String, Object>> maps = orderExtraChargesMapper.selectMaps(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<OrderExtraCharges>()
                            .select("order_no", "COALESCE(SUM(charge_amount),0) AS amt")
                            .in("order_no", orderNos)
                            .isNull("order_item_id")
                            .eq("deleted", 0)
                            .groupBy("order_no"));
            for (Map<String, Object> m : maps) {
                Long ono = ((Number) m.get("order_no")).longValue();
                BigDecimal amt = (m.get("amt") == null) ? BigDecimal.ZERO : new BigDecimal(String.valueOf(m.get("amt")));
                orderNoToOrderLevelExtraSum.put(ono, amt);
            }
        }

        // 8) 回填每条记录：applyItemCount / totalRefundAmount
        for (MerchantRefundApplyPageResultDto r : records) {
            Long applyId = r.getRefundApplyId();
            Long ono = r.getOrderNo();

            List<Long> itemIds = applyIdToItemIds.getOrDefault(applyId, List.of()).stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            int cnt = applyIdToItemCount.getOrDefault(applyId, 0);
            r.setApplyItemCount(cnt);

            // item 金额口径：unit_price + extra_amount（record extra），不要依赖 subtotal
            BigDecimal itemSum = BigDecimal.ZERO;
            BigDecimal itemExtraSum = BigDecimal.ZERO;

            for (Long itemId : itemIds) {
                OrderItems it = itemMap.get(itemId);
                if (it == null) continue;

                BigDecimal unit = it.getUnitPrice() == null ? BigDecimal.ZERO : it.getUnitPrice();
                BigDecimal recordExtra = it.getExtraAmount() == null ? BigDecimal.ZERO : it.getExtraAmount();

                itemSum = itemSum.add(unit.add(recordExtra));
                itemExtraSum = itemExtraSum.add(itemIdToExtraSum.getOrDefault(itemId, BigDecimal.ZERO));
            }

            // 展示口径：如果本次申请覆盖了整单所有 items，则额外展示“订单级 extra”
            BigDecimal orderLevelExtra = BigDecimal.ZERO;
            Long totalItemCnt = orderNoToTotalItemCount.getOrDefault(ono, 0L);
            if (totalItemCnt != null && totalItemCnt > 0 && (long) cnt == totalItemCnt) {
                orderLevelExtra = orderNoToOrderLevelExtraSum.getOrDefault(ono, BigDecimal.ZERO);
            }

            // fee
            BigDecimal fee = BigDecimal.ZERO;

            BigDecimal totalRefund = itemSum.add(itemExtraSum).add(orderLevelExtra).subtract(fee);
            r.setTotalRefundAmount(totalRefund);
        }

        return RpcResult.ok(resPage);
    }


    /**
     * 商家退款处理方法。
     *
     * @param dto 包含订单号、退款申请ID、场馆ID和商家ID等信息的请求参数
     * @return 返回商家退款的结果，包括订单状态、退款申请状态等信息
     */
    @Override
    @GlobalTransactional(
            // 当前全局事务的名称
            name = "merchant-refund",
            // 回滚异常
            rollbackFor = Exception.class,
            // 全局锁重试间隔
            lockRetryInterval = 5000,
            // 全局锁重试次数
            lockRetryTimes = 5,
            // 超时时间
            timeoutMills = 30000,
            //事务传播
            propagation = Propagation.REQUIRES_NEW
    )
    @RedisLock(value = "#dto.orderNo", prefix = RedisConsts.ORDER_LOCK_KEY_PREFIX)
    public RpcResult<SellerRefundResultDto> refund(MerchantRefundRequestDto dto) {
        List<Long> itemList = orderItemsMapper.selectList(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, dto.getOrderNo())
                        .eq(OrderItems::getRefundStatus, RefundStatusEnum.NONE)
        ).stream().map(OrderItems::getId).toList();
        return orderDubboService.refund(dto.getOrderNo(),
                dto.getVenueId(),
                dto.getMerchantId(),
                itemList,
                SellerTypeEnum.VENUE,
                dto.getRemark());
    }


    @Override
    @Transactional(readOnly = true)
    public RpcResult<MerchantRefundApplyDetailsResultDto> getRefundApplyDetails(MerchantRefundApplyDetailsRequestDto dto) {

        Long refundApplyId = dto.getRefundApplyId();
        Long orderNo = dto.getOrderNo();
        Long venueId = dto.getVenueId();

        // 1) 查订单（校验该场馆是否匹配）
        Orders order = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo)
                        .eq(Orders::getSellerId, venueId)
                        .eq(Orders::getSellerType, SellerTypeEnum.VENUE));
        if (ObjectUtils.isEmpty(order)) {
            return RpcResult.error(OrderCode.ORDER_NOT_EXIST);
        }

        // 2) 查退款申请
        OrderRefundApply apply = orderRefundApplyMapper.selectOne(
                Wrappers.<OrderRefundApply>lambdaQuery()
                        .eq(OrderRefundApply::getId, refundApplyId)
                        .eq(OrderRefundApply::getOrderNo, orderNo));
        if (ObjectUtils.isEmpty(apply)) {
            return RpcResult.error(OrderCode.ORDER_REFUND_APPLY_NOT_EXIST);
        }

        // 3) 查本次申请包含哪些 items（apply_items）
        List<OrderRefundApplyItems> applyItems = orderRefundApplyItemsMapper.selectList(
                Wrappers.<OrderRefundApplyItems>lambdaQuery()
                        .eq(OrderRefundApplyItems::getRefundApplyId, refundApplyId)
                        .eq(OrderRefundApplyItems::getOrderNo, orderNo)
                        .orderByAsc(OrderRefundApplyItems::getId));
        if (ObjectUtils.isEmpty(applyItems)) {
            return RpcResult.error(OrderCode.ORDER_ITEM_NOT_EXIST);
        }

        List<Long> itemIds = applyItems.stream()
                .map(OrderRefundApplyItems::getOrderItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 4) 查订单项
        List<OrderItems> items = orderItemsMapper.selectList(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, orderNo)
                        .in(OrderItems::getId, itemIds)
                        .orderByAsc(OrderItems::getId));
        if (items.size() != itemIds.size()) {
            return RpcResult.error(OrderCode.ORDER_ITEM_NOT_EXIST);
        }

        // 5) 查 item 退款事实（建议按 refund_apply_id + item_id 查当前申请的事实）
        List<OrderItemRefunds> facts = orderItemRefundsMapper.selectList(
                Wrappers.<OrderItemRefunds>lambdaQuery()
                        .eq(OrderItemRefunds::getOrderNo, orderNo)
                        .eq(OrderItemRefunds::getRefundApplyId, refundApplyId)
                        .in(OrderItemRefunds::getOrderItemId, itemIds));
        Map<Long, OrderItemRefunds> factMap = facts.stream()
                .collect(Collectors.toMap(OrderItemRefunds::getOrderItemId, f -> f, (a, b) -> a));

        // 6) 查 extraCharges（本次申请产生的额外费用退款明细）
        List<OrderRefundExtraCharges> refundExtras = orderRefundExtraChargesMapper.selectList(
                Wrappers.<OrderRefundExtraCharges>lambdaQuery()
                        .eq(OrderRefundExtraCharges::getOrderNo, orderNo)
                        .eq(OrderRefundExtraCharges::getRefundApplyId, refundApplyId)
                        .orderByAsc(OrderRefundExtraCharges::getId));

        // extra_charge_id -> charge（拿 name/typeId/order_item_id 等展示字段）
        Map<Long, OrderExtraCharges> extraChargeMap = Map.of();
        if (refundExtras != null && !refundExtras.isEmpty()) {
            List<Long> extraIds = refundExtras.stream()
                    .map(OrderRefundExtraCharges::getExtraChargeId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (!extraIds.isEmpty()) {
                extraChargeMap = orderExtraChargesMapper.selectBatchIds(extraIds).stream()
                        .collect(Collectors.toMap(OrderExtraCharges::getId, x -> x, (a, b) -> a));
            }
        }

        // itemId -> item级extra合计（用来聚合展示与金额计算）
        Map<Long, BigDecimal> itemExtraSum = new HashMap<>();
        BigDecimal orderLevelExtraSum = BigDecimal.ZERO;

        List<MerchantRefundExtraChargeDto> extraChargeDtos = new ArrayList<>();
        if (refundExtras != null) {
            for (OrderRefundExtraCharges re : refundExtras) {
                BigDecimal amt = re.getRefundAmount() == null ? BigDecimal.ZERO : re.getRefundAmount();

                if (re.getOrderItemId() != null) {
                    itemExtraSum.merge(re.getOrderItemId(), amt, BigDecimal::add);
                } else {
                    orderLevelExtraSum = orderLevelExtraSum.add(amt);
                }

                OrderExtraCharges ch = extraChargeMap.get(re.getExtraChargeId());
                extraChargeDtos.add(MerchantRefundExtraChargeDto.builder()
                        .extraChargeId(re.getExtraChargeId())
                        .chargeTypeId(ch == null ? null : ch.getChargeTypeId())
                        .orderItemId(re.getOrderItemId())
                        .chargeName(ch == null ? null : ch.getChargeName())
                        .refundAmount(amt)
                        .build());
            }
        }

        // 7) 组装 item 进度
        List<MerchantRefundItemDto> itemDtos = new ArrayList<>();
        BigDecimal totalRefundAmount = BigDecimal.ZERO;
        BigDecimal refundedAmount = BigDecimal.ZERO;
        BigDecimal refundingAmount = BigDecimal.ZERO;

        for (OrderItems it : items) {

            OrderItemRefunds fact = factMap.get(it.getId());
            BigDecimal itemAmount = (fact != null && fact.getItemAmount() != null)
                    ? fact.getItemAmount()
                    : (it.getUnitPrice() == null ? BigDecimal.ZERO : it.getUnitPrice());

            BigDecimal extraAmt = itemExtraSum.getOrDefault(it.getId(), BigDecimal.ZERO);

            BigDecimal fee = (fact != null && fact.getRefundFee() != null) ? fact.getRefundFee() : BigDecimal.ZERO;
            BigDecimal refundAmt = (fact != null && fact.getRefundAmount() != null)
                    ? fact.getRefundAmount()
                    : itemAmount.add(extraAmt).subtract(fee);

            totalRefundAmount = totalRefundAmount.add(refundAmt);

            // 根据 item refundStatus 粗分：已完成/处理中
            RefundStatusEnum rs = it.getRefundStatus();
            if (rs == RefundStatusEnum.COMPLETED) {
                refundedAmount = refundedAmount.add(refundAmt);
            } else if (rs == RefundStatusEnum.APPROVED || rs == RefundStatusEnum.PENDING) {
                refundingAmount = refundingAmount.add(refundAmt);
            }

            itemDtos.add(MerchantRefundItemDto.builder()
                    .orderItemId(it.getId())
                    .venueId(venueId)
                    .recordId(it.getRecordId())
                    .refundStatus(rs)
                    .itemAmount(itemAmount)
                    .extraChargeAmount(extraAmt)
                    .refundFee(fee)
                    .refundAmount(refundAmt)
                    .build());
        }

        // 订单级 extra（如果你希望展示在 total 里）：
        // 这里我把它加到 totalRefundAmount，但 refunded/refunding 是否加，看订单状态/退款时间判断
        totalRefundAmount = totalRefundAmount.add(orderLevelExtraSum);

        if (orderLevelExtraSum.compareTo(BigDecimal.ZERO) > 0) {
            if (apply.getRefundCompletedAt() != null || order.getOrderStatus() == OrderStatusEnum.REFUNDED) {
                refundedAmount = refundedAmount.add(orderLevelExtraSum);
            } else if (apply.getRefundInitiatedAt() != null) {
                refundingAmount = refundingAmount.add(orderLevelExtraSum);
            }
        }

        // 8) 时间线（可选）
        List<RefundTimelineVo> timeline = List.of();
        if (dto.isIncludeTimeline()) {
            List<OrderStatusLogs> logs = orderStatusLogsMapper.selectList(
                    Wrappers.<OrderStatusLogs>lambdaQuery()
                            .eq(OrderStatusLogs::getOrderNo, orderNo)
                            .eq(OrderStatusLogs::getRefundApplyId, refundApplyId)
                            .in(OrderStatusLogs::getAction,
                                    OrderActionEnum.REFUND_APPLY,
                                    OrderActionEnum.REFUND_APPROVE,
                                    OrderActionEnum.REFUND_REJECT,
                                    OrderActionEnum.REFUND_COMPLETE
                            )
                            .orderByAsc(OrderStatusLogs::getCreatedAt));

            timeline = logs.stream().map(statusLogs -> RefundTimelineVo.builder()
                    .action(statusLogs.getAction())
                    .actionName(statusLogs.getAction().getDescription())
                    .at(statusLogs.getCreatedAt())
                    .remark(statusLogs.getRemark())
                    .operatorType(statusLogs.getOperatorType())
                    .operatorId(statusLogs.getOperatorId())
                    .operatorName(statusLogs.getOperatorName())
                    .build()).toList();
        }

        // 9) 组装返回
        MerchantRefundApplyDetailsResultDto resultDto = MerchantRefundApplyDetailsResultDto.builder()
                .orderNo(orderNo)
                .refundApplyId(refundApplyId)
                .userId(order.getBuyerId())
                .venueId(venueId)
                .venueName(order.getSellerName())
                .orderStatus(order.getOrderStatus())
                .applyStatus(apply.getApplyStatus())
                .reasonCode(apply.getReasonCode())
                .reasonDetail(apply.getReasonDetail())
                .appliedAt(apply.getCreatedAt())
                .reviewedAt(apply.getReviewedAt())
                .sellerRemark(apply.getSellerRemark())
                .refundInitiatedAt(apply.getRefundInitiatedAt())
                .refundCompletedAt(apply.getRefundCompletedAt())
                .refundTransactionId(apply.getRefundTransactionId())
                .totalRefundAmount(totalRefundAmount)
                .refundedAmount(refundedAmount)
                .refundingAmount(refundingAmount)
                .items(itemDtos)
                .extraCharges(extraChargeDtos)
                .timeline(timeline)
                .build();
        return RpcResult.ok(resultDto);
    }


}
