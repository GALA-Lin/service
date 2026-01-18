package com.unlimited.sports.globox.order.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.common.constants.OrderMQConstants;
import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.enums.order.*;
import com.unlimited.sports.globox.common.lock.RedisLock;
import com.unlimited.sports.globox.common.message.order.OrderAutoCancelMessage;
import com.unlimited.sports.globox.common.message.order.UnlockSlotMessage;
import com.unlimited.sports.globox.common.result.OrderCode;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.result.UserAuthCode;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.common.utils.AuthContextHolder;
import com.unlimited.sports.globox.common.utils.IdGenerator;
import com.unlimited.sports.globox.dubbo.coach.CoachDubboService;
import com.unlimited.sports.globox.dubbo.coach.dto.*;
import com.unlimited.sports.globox.dubbo.merchant.MerchantDubboService;
import com.unlimited.sports.globox.dubbo.merchant.MerchantRefundRuleDubboService;
import com.unlimited.sports.globox.dubbo.merchant.dto.*;
import com.unlimited.sports.globox.model.order.dto.*;
import com.unlimited.sports.globox.model.order.entity.*;
import com.unlimited.sports.globox.model.order.vo.*;
import com.unlimited.sports.globox.model.order.vo.GetOrderDetailsVo.ExtraChargeVo;
import com.unlimited.sports.globox.order.constants.RedisConsts;
import com.unlimited.sports.globox.order.mapper.*;
import com.unlimited.sports.globox.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * order 服务层
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    /**
     * 订单自动关闭时间 单位 s
     */
    @Value("${order.unpaid-auto-cancel.scheduled}")
    private Integer delay;

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderItemsMapper orderItemsMapper;

    @Autowired
    private OrderItemRefundsMapper orderItemRefundsMapper;

    @Autowired
    private OrderExtraChargesMapper orderExtraChargesMapper;

    @Autowired
    private OrderExtraChargeLinksMapper orderExtraChargeLinksMapper;

    @Autowired
    private OrderStatusLogsMapper orderStatusLogsMapper;

    @Autowired
    private OrderActivitiesMapper orderActivitiesMapper;

    @Autowired
    private IdGenerator idGenerator;

    @DubboReference(group = "rpc")
    private MerchantDubboService merchantDubboService;

    @DubboReference(group = "rpc")
    private CoachDubboService coachDubboService;

    @Autowired
    private MQService mqService;

    @Lazy
    @Autowired
    private OrderServiceImpl thisService;

    @DubboReference(group = "rpc")
    private MerchantRefundRuleDubboService merchantRefundRuleDubboService;


    /**
     * 创建订场订单
     *
     * @param dto 创建订单参数
     * @return 创建结果
     */
    @Override
    public CreateOrderResultVo createVenueOrder(CreateVenueOrderDto dto) {
        // 0) 基础校验
        Long userId = AuthContextHolder.getLongHeader(RequestHeaderConstants.HEADER_USER_ID);
        Assert.isNotEmpty(userId, UserAuthCode.TOKEN_EXPIRED);

        // 1) RPC：校验 slot / 定价 / HOME-AWAY
        PricingRequestDto pricingRequestDto = new PricingRequestDto();
        BeanUtils.copyProperties(dto, pricingRequestDto);
        pricingRequestDto.setUserId(userId);
        RpcResult<PricingResultDto> rpcResult = merchantDubboService.quoteVenue(pricingRequestDto);
        Assert.rpcResultOk(rpcResult);
        PricingResultDto result = rpcResult.getData();
        // TODO ETA 等微信支付测试通过后删除
//        PricingResultDto result = getPricingResultDto();

        Assert.isNotEmpty(result.getRecordQuote(), OrderCode.SLOT_HAD_BOOKING);

        return thisService.createVenueOrderAction(result, userId, false);
    }


    /**
     * 用户创建活动订单
     *
     * @param dto 创建活动订单参数
     * @return 创建结果
     */
    @Override
    public CreateOrderResultVo createVenueActivityOrder(CreateVenueActivityOrderDto dto) {
        Long userId = AuthContextHolder.getLongHeader(RequestHeaderConstants.HEADER_USER_ID);
        Assert.isNotEmpty(userId, UserAuthCode.TOKEN_EXPIRED);

        PricingActivityRequestDto requestDto = new PricingActivityRequestDto();
        BeanUtils.copyProperties(dto, requestDto);
        requestDto.setUserId(userId);
        RpcResult<PricingActivityResultDto> rpcResult = merchantDubboService.quoteVenueActivity(requestDto);
        Assert.rpcResultOk(rpcResult);
        PricingActivityResultDto result = rpcResult.getData();

        Assert.isNotEmpty(result.getRecordQuote(), OrderCode.SLOT_HAD_BOOKING);

        return thisService.createVenueOrderAction(result, userId, true);
    }


    /**
     * 创建教练订单
     *
     * @param dto 包含创建教练订单所需参数的数据传输对象，包括预订日期和预订的场地时段ID列表
     * @return 包含创建订单结果的信息对象，主要包含生成的订单号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateOrderResultVo createCoachOrder(CreateCoachOrderDto dto) {


        // 0) 基础校验
        Long userId = AuthContextHolder.getLongHeader(RequestHeaderConstants.HEADER_USER_ID);
        Assert.isNotEmpty(userId, UserAuthCode.TOKEN_EXPIRED);

        // 校验并获取价格
        CoachPricingRequestDto requestDto = new CoachPricingRequestDto();
        BeanUtils.copyProperties(dto, requestDto);
        requestDto.setUserId(userId);
        requestDto.setCoachUserId(dto.getCoachId());

        RpcResult<CoachPricingResultDto> rpcResult = coachDubboService.quoteCoach(requestDto);
        Assert.rpcResultOk(rpcResult);
        CoachPricingResultDto resultDto = rpcResult.getData();


        Long orderNo = idGenerator.nextId();

        // 2) 先创建 order_items（每个 item 的 subtotal 只含 unitPrice + extra）
        List<ItemCtx> itemCtxList = new ArrayList<>();
        BigDecimal baseAmount = BigDecimal.ZERO;
        LocalDateTime createdAt = LocalDateTime.now();

        for (CoachSlotQuote slot : resultDto.getSlotQuotes()) {
            // 2.1 计算该槽位的额外费用合计
            BigDecimal slotExtraSum = slot.getRecordExtras().stream()
                    // 每条额外费用的金额快照
                    .map(ExtraQuote::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            OrderItems item = OrderItems.builder()
                    .orderNo(orderNo)
                    // VENUE/COURT
                    .itemType(SellerTypeEnum.COACH)
                    .resourceId(slot.getCoachId())
                    .resourceName(slot.getCoachName())
                    .recordId(slot.getRecordId())
                    .bookingDate(slot.getBookingDate())
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .unitPrice(slot.getUnitPrice())
                    .extraAmount(slotExtraSum)
                    .subtotal(slot.getUnitPrice().add(slotExtraSum))
                    // NONE
                    .refundStatus(RefundStatusEnum.NONE)
                    .build();

            item.setCreatedAt(createdAt);

            int insItem = orderItemsMapper.insert(item);
            Assert.isTrue(insItem > 0, OrderCode.ORDER_CREATE_FAILED);

            // 汇总 baseAmount：基础价 = Σ unit_price
            baseAmount = baseAmount.add(slot.getUnitPrice());

            itemCtxList.add(new ItemCtx(item.getId(), slot.getUnitPrice(), item.getSubtotal(), slot.getRecordExtras()));
        }

        /* 3）写入 extraTotal = 所有额外费用总和（含：
           - 槽级 extra（来自 links）
           - 订单级 extra（来自 order_extra_charges））
         */
        BigDecimal extraTotal = BigDecimal.ZERO;

        for (ItemCtx ctx : itemCtxList) {
            for (ExtraQuote extra : ctx.recordExtras()) {
                OrderExtraCharges ch = OrderExtraCharges.builder()
                        .orderNo(orderNo)
                        .orderItemId(ctx.itemId)
                        .chargeTypeId(extra.getChargeTypeId())
                        .chargeName(extra.getChargeName())
                        // 1=FIXED，2=PERCENTAGE
                        .chargeMode(extra.getChargeMode())
                        // 若是固定金额，填金额；若是百分比，填百分比值
                        .fixedValue(extra.getFixedValue())
                        // 最终金额快照
                        .chargeAmount(extra.getAmount())
                        .build();
                ch.setCreatedAt(createdAt);

                int insCh = orderExtraChargesMapper.insert(ch);
                Assert.isTrue(insCh > 0, OrderCode.ORDER_CREATE_FAILED);

                OrderExtraChargeLinks link = OrderExtraChargeLinks.builder()
                        .orderNo(orderNo)
                        .orderItemId(ctx.itemId())
                        .extraChargeId(ch.getId())
                        .chargeMode(extra.getChargeMode())
                        // 直接等于自身金额
                        .allocatedAmount(ch.getChargeAmount())
                        .build();

                link.setCreatedAt(createdAt);

                int insLink = orderExtraChargeLinksMapper.insert(link);
                Assert.isTrue(insLink > 0, OrderCode.ORDER_CREATE_FAILED);

                extraTotal = extraTotal.add(link.getAllocatedAmount());
            }
        }

        // 4) 写入 订单级别的 extra（暂不涉及分摊，不写入 links）
        for (OrderLevelExtraQuote extra : resultDto.getOrderLevelExtras()) {

            // 1=FIXED, 2=PERCENTAGE
            ChargeModeEnum chargeMode = extra.getChargeMode();

            OrderExtraCharges ch = OrderExtraCharges.builder()
                    .orderNo(orderNo)
                    .chargeTypeId(extra.getChargeTypeId())
                    .chargeName(extra.getChargeName())
                    .chargeMode(chargeMode)
                    .fixedValue(extra.getFixedValue())
                    .chargeAmount(extra.getAmount())
                    .build();

            ch.setCreatedAt(createdAt);

            int insCh = orderExtraChargesMapper.insert(ch);
            Assert.isTrue(insCh > 0, OrderCode.ORDER_CREATE_FAILED);

            extraTotal = extraTotal.add(ch.getChargeAmount());
        }

        // 5) 创建 orders（金额只信：baseAmount + links 汇总 extraTotal）
        BigDecimal discountAmount = BigDecimal.ZERO;

        Orders order = Orders.builder()
                .orderNo(orderNo)
                .sourcePlatform(resultDto.getSourcePlatform())
                .buyerId(userId)
                .sellerName(resultDto.getCoachName())
                .sellerType(SellerTypeEnum.COACH)
                .sellerId(resultDto.getSellerId())
                .orderStatus(OrderStatusEnum.PENDING)
                .paymentStatus(OrdersPaymentStatusEnum.UNPAID)
                .paymentType(PaymentTypeEnum.NONE)
                .refundApplyId(null)
                .activity(false)
                .baseAmount(baseAmount)
                .extraAmount(extraTotal)
                .subtotal(baseAmount.add(extraTotal))
                .discountAmount(discountAmount)
                .payAmount(baseAmount.add(extraTotal).subtract(discountAmount))
                .paidAt(null)
                .cancelledAt(null)
                .completedAt(null)
                .build();

        order.setCreatedAt(createdAt);

        int insOrder = ordersMapper.insert(order);
        Assert.isTrue(insOrder > 0, OrderCode.ORDER_CREATE_FAILED);

        // 6) 创建
        OrderStatusLogs statusLogs = OrderStatusLogs.builder()
                .orderNo(orderNo)
                .orderId(order.getId())
                // 订单级
                .orderItemId(null)
                .action(OrderActionEnum.CREATE)
                .oldOrderStatus(null)
                .newOrderStatus(OrderStatusEnum.PENDING)
                .oldRefundStatus(null)
                .newRefundStatus(null)
                .oldItemRefundStatus(null)
                .newItemRefundStatus(null)
                .refundApplyId(null)
                .itemRefundId(null)
                .operatorId(null)
                .operatorType(OperatorTypeEnum.USER)
                .operatorName("USER_" + userId)
                .remark("创建教练订单")
                .build();
        statusLogs.setCreatedAt(createdAt);

        orderStatusLogsMapper.insert(statusLogs);

        OrderAutoCancelMessage orderAutoCancelMessage = OrderAutoCancelMessage.builder()
                .bookingDate(resultDto.getBookingDate())
                .orderNo(order.getOrderNo())
                .recordIds(resultDto.getSlotQuotes().stream().map(CoachSlotQuote::getCoachId).collect(Collectors.toList()))
                .sellerType(SellerTypeEnum.COACH)
                .userId(userId)
                .build();

        // 注册事务回调
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        // 发送消息，定时关闭订单
                        mqService.sendDelay(
                                OrderMQConstants.EXCHANGE_TOPIC_ORDER_AUTO_CANCEL,
                                OrderMQConstants.ROUTING_ORDER_AUTO_CANCEL,
                                orderAutoCancelMessage,
                                delay);
                    }

                    @Override
                    public void afterCompletion(int status) {
                        // 事务回滚，发取消锁场事件
                        if (status == STATUS_ROLLED_BACK) {
                            UnlockSlotMessage message = UnlockSlotMessage.builder()
                                    .userId(userId)
                                    .operatorType(OperatorTypeEnum.SYSTEM)
                                    .isActivity(order.getActivity())
                                    .bookingDate(resultDto.getBookingDate())
                                    .recordIds(itemCtxList.stream().map(item -> item.itemId).collect(Collectors.toList()))
                                    .build();
                            message.setUserId(userId);
                            mqService.send(
                                    OrderMQConstants.EXCHANGE_TOPIC_ORDER_UNLOCK_COACH_SLOT,
                                    OrderMQConstants.ROUTING_ORDER_UNLOCK_COACH_SLOT,
                                    message);
                        }
                    }
                });

        CreateOrderResultVo vo = new CreateOrderResultVo();
        vo.setOrderNo(orderNo);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public CreateOrderResultVo createVenueOrderAction(PricingResultDto result, Long userId, boolean isActivity) {
        Long orderNo = idGenerator.nextId();

        // 2) 先创建 order_items（每个 item 的 subtotal 只含 unitPrice + extra）
        List<ItemCtx> itemCtxList = new ArrayList<>();
        BigDecimal baseAmount = BigDecimal.ZERO;
        LocalDateTime createdAt = LocalDateTime.now();

        for (RecordQuote slot : result.getRecordQuote()) {
            // 2.1 计算该槽位的额外费用合计
            BigDecimal slotExtraSum = slot.getRecordExtras().stream()
                    // 每条额外费用的金额快照
                    .map(ExtraQuote::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            OrderItems item = OrderItems.builder()
                    .orderNo(orderNo)
                    // VENUE/COURT
                    .itemType(SellerTypeEnum.VENUE)
                    .resourceId(slot.getCourtId())
                    .resourceName(slot.getCourtName())
                    .recordId(slot.getRecordId())
                    .bookingDate(slot.getBookingDate())
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .unitPrice(slot.getUnitPrice())
                    .extraAmount(slotExtraSum)
                    .subtotal(slot.getUnitPrice().add(slotExtraSum))
                    // NONE
                    .refundStatus(RefundStatusEnum.NONE)
                    .build();

            item.setCreatedAt(createdAt);

            int insItem = orderItemsMapper.insert(item);
            Assert.isTrue(insItem > 0, OrderCode.ORDER_CREATE_FAILED);

            // 汇总 baseAmount：基础价 = Σ unit_price
            baseAmount = baseAmount.add(slot.getUnitPrice());

            itemCtxList.add(new ItemCtx(item.getId(), slot.getUnitPrice(), item.getSubtotal(), slot.getRecordExtras()));
        }

        /* 3）写入 extraTotal = 所有额外费用总和（含：
           - 槽级 extra（来自 links）
           - 订单级 extra（来自 order_extra_charges））
         */
        BigDecimal extraTotal = BigDecimal.ZERO;

        for (ItemCtx ctx : itemCtxList) {
            for (ExtraQuote extra : ctx.recordExtras()) {
                OrderExtraCharges ch = OrderExtraCharges.builder()
                        .orderNo(orderNo)
                        .orderItemId(ctx.itemId)
                        .chargeTypeId(extra.getChargeTypeId())
                        .chargeName(extra.getChargeName())
                        // 1=FIXED，2=PERCENTAGE
                        .chargeMode(extra.getChargeMode())
                        // 若是固定金额，填金额；若是百分比，填百分比值
                        .fixedValue(extra.getFixedValue())
                        // 最终金额快照
                        .chargeAmount(extra.getAmount())
                        .build();
                ch.setCreatedAt(createdAt);

                int insCh = orderExtraChargesMapper.insert(ch);
                Assert.isTrue(insCh > 0, OrderCode.ORDER_CREATE_FAILED);

                OrderExtraChargeLinks link = OrderExtraChargeLinks.builder()
                        .orderNo(orderNo)
                        .orderItemId(ctx.itemId())
                        .extraChargeId(ch.getId())
                        .chargeMode(extra.getChargeMode())
                        // 直接等于自身金额
                        .allocatedAmount(ch.getChargeAmount())
                        .build();

                link.setCreatedAt(createdAt);

                int insLink = orderExtraChargeLinksMapper.insert(link);
                Assert.isTrue(insLink > 0, OrderCode.ORDER_CREATE_FAILED);

                extraTotal = extraTotal.add(link.getAllocatedAmount());
            }
        }

        // 4) 写入 订单级别的 extra（暂不涉及分摊，不写入 links）
        for (OrderLevelExtraQuote extra : result.getOrderLevelExtras()) {

            // 1=FIXED, 2=PERCENTAGE
            ChargeModeEnum chargeMode = extra.getChargeMode();

            OrderExtraCharges ch = OrderExtraCharges.builder()
                    .orderNo(orderNo)
                    .chargeTypeId(extra.getChargeTypeId())
                    .chargeName(extra.getChargeName())
                    .chargeMode(chargeMode)
                    .fixedValue(extra.getFixedValue())
                    .chargeAmount(extra.getAmount())
                    .build();

            ch.setCreatedAt(createdAt);

            int insCh = orderExtraChargesMapper.insert(ch);
            Assert.isTrue(insCh > 0, OrderCode.ORDER_CREATE_FAILED);

            extraTotal = extraTotal.add(ch.getChargeAmount());
        }

        // 5) 创建 orders（金额只信：baseAmount + links 汇总 extraTotal）
        BigDecimal discountAmount = BigDecimal.ZERO;

        Orders order = Orders.builder()
                .orderNo(orderNo)
                .sourcePlatform(result.getSourcePlatform())
                .buyerId(userId)
                .sellerName(result.getSellerName())
                .sellerType(SellerTypeEnum.VENUE)
                .sellerId(result.getSellerId())
                .orderStatus(OrderStatusEnum.PENDING)
                .paymentStatus(OrdersPaymentStatusEnum.UNPAID)
                .paymentType(PaymentTypeEnum.NONE)
                .refundApplyId(null)
                .activity(isActivity)
                .baseAmount(baseAmount)
                .extraAmount(extraTotal)
                .subtotal(baseAmount.add(extraTotal))
                .discountAmount(discountAmount)
                .payAmount(baseAmount.add(extraTotal).subtract(discountAmount))
                .paidAt(null)
                .cancelledAt(null)
                .completedAt(null)
                .build();



        order.setCreatedAt(createdAt);

        int insOrder = ordersMapper.insert(order);
        Assert.isTrue(insOrder > 0, OrderCode.ORDER_CREATE_FAILED);

        // 6) 创建
        OrderStatusLogs statusLogs = OrderStatusLogs.builder()
                .orderNo(orderNo)
                .orderId(order.getId())
                // 订单级
                .orderItemId(null)
                .action(OrderActionEnum.CREATE)
                .oldOrderStatus(null)
                .newOrderStatus(OrderStatusEnum.PENDING)
                .oldRefundStatus(null)
                .newRefundStatus(null)
                .oldItemRefundStatus(null)
                .newItemRefundStatus(null)
                .refundApplyId(null)
                .itemRefundId(null)
                .operatorId(null)
                .operatorType(OperatorTypeEnum.USER)
                .operatorName("USER_" + userId)
                .remark("创建订场订单")
                .build();
        statusLogs.setCreatedAt(createdAt);

        orderStatusLogsMapper.insert(statusLogs);

        // 7）如果是活动订单，插入订单-活动绑定表
        if (isActivity) {
            PricingActivityResultDto activityResultDto = (PricingActivityResultDto) result;
            OrderActivities orderActivities = new OrderActivities();
            BeanUtils.copyProperties(activityResultDto, orderActivities);
            orderActivities.setOrderNo(orderNo);
            orderActivitiesMapper.insert(orderActivities);
        }

        OrderAutoCancelMessage orderAutoCancelMessage = OrderAutoCancelMessage.builder()
                .bookingDate(result.getBookingDate())
                .orderNo(order.getOrderNo())
                .sellerType(SellerTypeEnum.VENUE)
                .recordIds(result.getRecordQuote().stream().map(RecordQuote::getRecordId).collect(Collectors.toList()))
                .userId(userId)
                .build();

        // 注册事务回调
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        // 发送消息，定时关闭订单
                        mqService.sendDelay(
                                OrderMQConstants.EXCHANGE_TOPIC_ORDER_AUTO_CANCEL,
                                OrderMQConstants.ROUTING_ORDER_AUTO_CANCEL,
                                orderAutoCancelMessage,
                                delay);
                    }

                    @Override
                    public void afterCompletion(int status) {
                        // 事务回滚，发取消锁场事件
                        if (status == STATUS_ROLLED_BACK) {
                            UnlockSlotMessage message = new UnlockSlotMessage();
                            UnlockSlotMessage.builder()
                                    .userId(userId)
                                    .isActivity(isActivity)
                                    .operatorType(OperatorTypeEnum.SYSTEM)
                                    .bookingDate(result.getBookingDate())
                                    .recordIds(itemCtxList.stream().map(item -> item.itemId).collect(Collectors.toList()))
                                    .build();
                            message.setUserId(userId);
                            mqService.send(
                                    OrderMQConstants.EXCHANGE_TOPIC_ORDER_UNLOCK_SLOT,
                                    OrderMQConstants.ROUTING_ORDER_UNLOCK_SLOT,
                                    message);
                        }
                    }
                });

        CreateOrderResultVo vo = new CreateOrderResultVo();
        vo.setOrderNo(orderNo);
        return vo;
    }

    /**
     * 获取用户的订单列表
     *
     * @return 订单列表
     */
    @Override
    public PaginationResult<GetOrderVo> getOrderPage(GetOrderPageDto pageDto, String openId) {

        Long userId = AuthContextHolder.getLongHeader(RequestHeaderConstants.HEADER_USER_ID);
        Assert.isNotEmpty(userId, UserAuthCode.TOKEN_EXPIRED);

        // 1. 构建分页对象
        Page<Orders> page = new Page<>(
                pageDto.getPageNum(),
                pageDto.getPageSize());

        // 2. 分页查询订单主表
        List<Long> mooncourtIdList = List.of();
        if (!ObjectUtils.isEmpty(openId)) {
            RpcResult<List<Long>> rpcResult = merchantDubboService.getMoonCourtIdList();
            Assert.rpcResultOk(rpcResult);
            mooncourtIdList = rpcResult.getData();
        }

        IPage<Orders> orderPage = ordersMapper.selectPage(
                page,
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getBuyerId, userId)
                        .eq(!ObjectUtils.isEmpty(openId), Orders::getSellerType, SellerTypeEnum.VENUE)
                        .in(!ObjectUtils.isEmpty(openId), Orders::getSellerId, mooncourtIdList)
                        .in(!ObjectUtils.isEmpty(pageDto.getOrderStatus()), Orders::getOrderStatus, pageDto.getOrderStatus())
                        .orderByDesc(Orders::getId));

        // 没有数据
        if (orderPage.getRecords().isEmpty()) {
            return PaginationResult.build(List.of(), orderPage.getTotal(), pageDto.getPageNum(), pageDto.getPageSize());
        }

        // 3. 收集当前页的 orderNo
        List<Long> orderNos = orderPage.getRecords()
                .stream()
                .map(Orders::getOrderNo)
                .toList();

        // 4. 批量查询订单详情
        List<OrderItems> items = orderItemsMapper.selectList(
                Wrappers.<OrderItems>lambdaQuery()
                        .in(OrderItems::getOrderNo, orderNos)
                        .orderByAsc(OrderItems::getStartTime));

        // 5. 按 orderNo 分组
        Map<Long, List<OrderItems>> itemMap = items.stream()
                .collect(Collectors.groupingBy(OrderItems::getOrderNo));

        // 6. 组装 VO
        List<GetOrderVo> voList = orderPage.getRecords().stream()
                .filter(item -> {
                    if (openId != null && openId.isBlank()) {
                        // 过滤教练订单
                        log.info("条件判断：{}",item.getSellerType().equals(SellerTypeEnum.VENUE));
                        return item.getSellerType().equals(SellerTypeEnum.VENUE);
                    }
                    return true;
                })
                .map(order -> {

                    List<OrderItems> orderItems =
                            itemMap.getOrDefault(order.getOrderNo(), List.of());

                    if (orderItems.isEmpty()) {
                        return null;
                    }

                    OrderItems firstItem = orderItems.get(0);

                    // TODO 2026/01/10 是否修改
                    String resourceName = orderItems.stream().map(OrderItems::getResourceName)
                            .reduce(String::concat)
                            .orElseGet(order::getSellerName);

                    List<SlotBookingTime> slotTimes =
                            orderItems.stream()
                                    .map(item -> {
                                        SlotBookingTime t = new SlotBookingTime();
                                        t.setStartTime(item.getStartTime());
                                        t.setEndTime(item.getEndTime());
                                        return t;
                                    })
                                    .toList();

                    OrderActivities orderActivities = orderActivitiesMapper.selectOne(
                            Wrappers.<OrderActivities>lambdaQuery()
                                    .eq(OrderActivities::getOrderNo, order.getOrderNo()));

                    GetOrderVo orderVo = GetOrderVo.builder()
                            .orderNo(order.getOrderNo())
                            .sellerType(order.getSellerType().getCode())
                            .sellerId(order.getSellerId())
                            .sellerName(order.getSellerName())
                            .resourceId(firstItem.getResourceId())
                            .resourceName(firstItem.getResourceName())
                            .bookingDate(firstItem.getBookingDate())
                            .amount(order.getPayAmount())
                            .currentOrderStatus(order.getOrderStatus())
                            .createdAt(order.getCreatedAt())
                            .itemCount(orderItems.size())
                            .slotBookingTimes(slotTimes)
                            .build();

                    // 如果是活动订单，添加字段
                    if (!ObjectUtils.isEmpty(orderActivities)) {
                        orderVo.setActivity(true);
                        orderVo.setActivityTypeName(orderActivities.getActivityTypeName());
                    } else {
                        orderVo.setActivity(false);
                    }

                    return orderVo;
                })
                .filter(Objects::nonNull)
                .toList();

        // 7. 返回分頁 VO
        return PaginationResult.build(voList, orderPage.getTotal(), (int) orderPage.getCurrent(),
                                      (int) orderPage.getSize());
    }


    /**
     * 获取用户的订单详情
     *
     * @param dto 查询订单的参数
     * @return 订单详情
     */
    @Override
    public GetOrderDetailsVo getDetails(GetOrderDetailsDto dto) {

        LocalDateTime now = LocalDateTime.now();
        Long userId = AuthContextHolder.getLongHeader(RequestHeaderConstants.HEADER_USER_ID);
        Assert.isNotEmpty(userId, UserAuthCode.TOKEN_EXPIRED);

        Long orderNo = dto.getOrderNo();
        Assert.isNotEmpty(orderNo, OrderCode.ORDER_NOT_EXIST);

        // 1) 查询订单主表
        Orders orders = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo)
                        .eq(Orders::getBuyerId, userId));
        Assert.isNotEmpty(orders, OrderCode.ORDER_NOT_EXIST);

        // 2) 查询订单项
        List<OrderItems> items = orderItemsMapper.selectList(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, orderNo)
                        .orderByAsc(OrderItems::getId));
        Assert.isTrue(items != null && !items.isEmpty(), OrderCode.ORDER_ITEM_NOT_EXIST);

        List<Long> itemIds = items.stream().map(OrderItems::getId).toList();

        // 3) 查询 item 退款事实（可能没有）
        Map<Long, OrderItemRefunds> refundMap = orderItemRefundsMapper.selectList(
                        Wrappers.<OrderItemRefunds>lambdaQuery()
                                .in(OrderItemRefunds::getOrderItemId, itemIds))
                .stream()
                // 如果历史存在多条，取最新一条
                .collect(Collectors.toMap(
                        OrderItemRefunds::getOrderItemId,
                        r -> r,
                        (a, b) -> a.getId() > b.getId() ? a : b));

        // 4) 查询所有额外费用（订单级 + item级）
        List<OrderExtraCharges> extraCharges = orderExtraChargesMapper.selectList(
                Wrappers.<OrderExtraCharges>lambdaQuery()
                        .eq(OrderExtraCharges::getOrderNo, orderNo));
        if (extraCharges == null) extraCharges = new ArrayList<>();

        // 4.1 订单级额外费用
        List<GetOrderDetailsVo.ExtraChargeVo> orderLevelExtraCharges = extraCharges.stream()
                .filter(ch -> ch.getOrderItemId() == null)
                .map(ch -> {
                    ExtraChargeVo extraChargeVo = new ExtraChargeVo();
                    BeanUtils.copyProperties(ch, extraChargeVo);
                    return  extraChargeVo;
                })
                .toList();

        // 4.2 item 级额外费用（按 itemId 分组）
        Map<Long, List<GetOrderDetailsVo.ExtraChargeVo>> itemExtraMap = extraCharges.stream()
                .filter(ch -> ch.getOrderItemId() != null)
                .collect(Collectors.groupingBy(
                        OrderExtraCharges::getOrderItemId,
                        Collectors.mapping(ch ->{
                                    ExtraChargeVo extraChargeVo = new ExtraChargeVo();
                                    BeanUtils.copyProperties(ch, extraChargeVo);
                                    return extraChargeVo;
                                },
                                Collectors.toList())));

        // 5) 组装 venue/court 快照
        GetOrderDetailsVo.VenueSnapshotVo venueSnapshotVo = null;
        GetOrderDetailsVo.CoachSnapshotVo coachSnapshotVo = null;
        Map<Long, GetOrderDetailsVo.CourtSnapshotVo> courtSnapshotMap = new HashMap<>();

        if (orders.getSellerType() == SellerTypeEnum.VENUE) {

            // courtId 来自订单项 resource_id（COURT item）
            List<Long> courtIds = items.stream()
                    .filter(it -> it.getItemType() == null || it.getItemType() == SellerTypeEnum.VENUE)
                    .map(OrderItems::getResourceId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            VenueSnapshotRequestDto req = VenueSnapshotRequestDto.builder()
                    .userId(userId)
                    .venueId(orders.getSellerId())
                    .courtId(courtIds)
                    .latitude(dto.getLatitude())
                    .longitude(dto.getLongitude())
                    .build();


            RpcResult<VenueSnapshotResultDto> rpcResult = merchantDubboService.getVenueSnapshot(req);
            Assert.rpcResultOk(rpcResult);
            VenueSnapshotResultDto snap = rpcResult.getData();
                    Assert.isNotEmpty(snap, OrderCode.ORDER_NOT_EXIST);

            venueSnapshotVo = new GetOrderDetailsVo.VenueSnapshotVo();
            BeanUtils.copyProperties(snap, venueSnapshotVo);


            if (snap.getCourtSnapshotDtos() != null) {
                for (VenueSnapshotResultDto.CourtSnapshotDto c : snap.getCourtSnapshotDtos()) {
                    if (c == null || c.getId() == null) continue;
                    GetOrderDetailsVo.CourtSnapshotVo snapshotVo = new GetOrderDetailsVo.CourtSnapshotVo();
                    BeanUtils.copyProperties(c, snapshotVo);
                    courtSnapshotMap.put(c.getId(), snapshotVo);
                }
            }
        }else if (orders.getSellerType() == SellerTypeEnum.COACH) {
            Long coachId = orders.getSellerId();
            CoachSnapshotRequestDto req = CoachSnapshotRequestDto.builder()
                    .coachUserId(coachId)
                            .build();
            RpcResult<CoachSnapshotResultDto> coachSnapshotRpcResult = coachDubboService.getCoachSnapshot(req);
            Assert.rpcResultOk(coachSnapshotRpcResult);
            CoachSnapshotResultDto resultDto = coachSnapshotRpcResult.getData();

            coachSnapshotVo = new GetOrderDetailsVo.CoachSnapshotVo();
            BeanUtils.copyProperties(resultDto, coachSnapshotVo);
        }

        // 6) bookingDate：同一单同一天；这里取第一个
        LocalDate bookingDate = items.get(0).getBookingDate();

        // 7) 组装 item VO
        AtomicBoolean orderRefundable = new AtomicBoolean(false);
        List<GetOrderDetailsVo.OrderItemDetailVo> itemVos = items.stream().map(item -> {

            // courtSnapshot：仅 COURT item & venue单
            GetOrderDetailsVo.CourtSnapshotVo courtVo = null;
            if (orders.getSellerType() == SellerTypeEnum.VENUE) {
                Long courtId = item.getResourceId();
                if (courtId != null) {
                    courtVo = courtSnapshotMap.get(courtId);
                }
            }

            // refund：来自 order_item_refunds
            OrderItemRefunds refund = refundMap.get(item.getId());

            GetOrderDetailsVo.ItemRefundVo refundVo = new GetOrderDetailsVo.ItemRefundVo();
            if (refund != null) {
                BeanUtils.copyProperties(refund, refundVo);
            }

            SlotBookingTime time = new SlotBookingTime();
            time.setStartTime(item.getStartTime());
            time.setEndTime(item.getEndTime());

            GetOrderDetailsVo.OrderItemDetailVo itemDetailVo = GetOrderDetailsVo.OrderItemDetailVo.builder()
                    .itemId(item.getId())
                    .courtSnapshot(courtVo)
                    .itemBaseAmount(item.getUnitPrice())
                    .itemAmount(item.getSubtotal())
                    .extraCharges(itemExtraMap.getOrDefault(item.getId(), List.of()))
                    .refundStatus(item.getRefundStatus())
                    .slotBookingTimes(List.of(time))
                    .refund(refundVo)
                    .build();

            // 只有这四种状态的订单可以申请退款，需要查询退款规则
            if (OrderStatusEnum.PAID.equals(orders.getOrderStatus())
                    || OrderStatusEnum.CONFIRMED.equals(orders.getOrderStatus())
                    || OrderStatusEnum.REFUND_CANCELLED.equals(orders.getOrderStatus())
                    || OrderStatusEnum.PARTIALLY_REFUNDED.equals(orders.getOrderStatus())) {
                switch (orders.getSellerType()) {
                    case VENUE -> {
                        MerchantRefundRuleJudgeRequestDto requestDto = MerchantRefundRuleJudgeRequestDto.builder()
                                .eventStartTime(LocalDateTime.of(item.getBookingDate(), item.getStartTime()))
                                .refundApplyTime(now)
                                .venueId(orders.getSellerId())
                                .userId(userId)
                                .orderTime(orders.getCreatedAt())
                                .build();

                        RpcResult<MerchantRefundRuleJudgeResultVo> rpcResult =
                                merchantRefundRuleDubboService.judgeApplicableRefundRule(requestDto);

                        MerchantRefundRuleJudgeResultVo resultVo = rpcResult.getData();
                        if (rpcResult.isSuccess() && resultVo.isCanRefund()) {
                            itemDetailVo.setIsItemRefundable(true);
                            itemDetailVo.setRefundPercentage(resultVo.getRefundPercentage());
                            orderRefundable.set(true);
                        } else {
                            itemDetailVo.setIsItemRefundable(false);
                        }
                    }
                    case COACH -> {
                        itemDetailVo.setIsItemRefundable(true);
                        itemDetailVo.setRefundPercentage(new BigDecimal("100"));
                    }
                }
            }

            return itemDetailVo;
        }).toList();

        boolean isCancelable = orders.getPaymentStatus() == OrdersPaymentStatusEnum.UNPAID
                && orders.getOrderStatus() == OrderStatusEnum.PENDING;
        boolean isRefundable = !isCancelable && orderRefundable.get();


        // 8) 返回
        GetOrderDetailsVo detailsVo = GetOrderDetailsVo.builder()
                .orderNo(orders.getOrderNo())
                .orderType(orders.getSellerType())
                .venueSnapshot(venueSnapshotVo)
                .coachSnapshotVo(coachSnapshotVo)
                .sellerName(orders.getSellerName())
                .amount(orders.getPayAmount())
                .isCancelable(isCancelable)
                .isRefundable(isRefundable)
                .bookingDate(bookingDate)
                .currentOrderStatus(orders.getOrderStatus())
                .orderLevelExtraCharges(orderLevelExtraCharges)
                .createdAt(orders.getCreatedAt())
                .items(itemVos)
                .build();

        // 9）如果是活动订单，添加额外字段
        OrderActivities orderActivities = orderActivitiesMapper.selectOne(
                Wrappers.<OrderActivities>lambdaQuery()
                        .eq(OrderActivities::getOrderNo, orders.getOrderNo()));

        if (!ObjectUtils.isEmpty(orderActivities)) {
            detailsVo.setActivity(true);
            detailsVo.setActivityTypeName(orderActivities.getActivityTypeName());
        } else {
            detailsVo.setActivity(false);
        }

        return detailsVo;
    }


    /**
     * 取消未支付的订单。
     *
     * @param orderNo 包含取消订单所需信息的数据传输对象，包括订单号和可选的取消原因
     * @return 返回包含订单号、当前订单状态、状态描述以及取消时间的结果对象
     */
    @Override
    @RedisLock(value = "#orderNo", prefix = RedisConsts.ORDER_LOCK_KEY_PREFIX)
    @Transactional(rollbackFor = Exception.class)
    public CancelOrderResultVo cancelUnpaidOrder(Long orderNo) {

        Long userId = AuthContextHolder.getLongHeader(RequestHeaderConstants.HEADER_USER_ID);
        Assert.isNotEmpty(userId, UserAuthCode.TOKEN_EXPIRED);

        // 1. 查询订单
        Orders order = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo)
                        .eq(Orders::getBuyerId, userId));
        Assert.isNotEmpty(order, OrderCode.ORDER_NOT_EXIST);
        // 必须是申请取消人的订单
        Assert.isTrue(order.getBuyerId().equals(userId), OrderCode.ORDER_NOT_EXIST);

        // 2. 状态校验
        Assert.isTrue(order.getOrderStatus() == OrderStatusEnum.PENDING,
                OrderCode.ORDER_STATUS_NOT_ALLOW_CANCEL);

        Assert.isTrue(order.getPaymentStatus() == OrdersPaymentStatusEnum.UNPAID,
                OrderCode.ORDER_STATUS_NOT_ALLOW_CANCEL);

        // 3. 查询订单项（用于解锁槽位）
        List<OrderItems> items = orderItemsMapper.selectList(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, orderNo));
        Assert.isNotEmpty(items, OrderCode.ORDER_ITEM_NOT_EXIST);

        List<Long> recordIds = items.stream()
                .map(OrderItems::getRecordId)
                .filter(Objects::nonNull)
                .toList();

        // 同一订单的 bookingDate 一致，取第一个即可
        LocalDate bookingDate = items.get(0)
                .getBookingDate();

        // 4. 更新订单
        Orders update = new Orders();
        update.setId(order.getId());
        update.setOrderStatus(OrderStatusEnum.CANCELLED);
        update.setCancelledAt(LocalDateTime.now());

        int rows = ordersMapper.updateById(update);
        Assert.isTrue(rows == 1, OrderCode.ORDER_CANCEL_FAILED);

        // 5. 写订单状态流转日志
        OrderStatusLogs log = OrderStatusLogs.builder()
                .orderNo(orderNo)
                .orderId(order.getId())
                .orderItemId(null)
                .action(OrderActionEnum.CANCEL)
                .oldOrderStatus(order.getOrderStatus())
                .newOrderStatus(OrderStatusEnum.CANCELLED)
                .operatorType(OperatorTypeEnum.USER)
                .operatorId(userId)
                .operatorName("USER_" + userId)
                .remark("用户手动取消未支付订单")
                .build();

        orderStatusLogsMapper.insert(log);

        // 6. 事务提交后发送解锁消息
        UnlockSlotMessage unlockMessage = UnlockSlotMessage.builder()
                .userId(userId)
                .operatorType(OperatorTypeEnum.USER)
                .recordIds(recordIds)
                .isActivity(order.getActivity())
                .bookingDate(bookingDate)
                .build();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        mqService.send(
                                OrderMQConstants.EXCHANGE_TOPIC_ORDER_UNLOCK_SLOT,
                                OrderMQConstants.ROUTING_ORDER_UNLOCK_SLOT,
                                unlockMessage);
                    }
                });

        // 7. 返回结果
        return CancelOrderResultVo.builder()
                .orderNo(orderNo)
                .orderStatus(OrderStatusEnum.CANCELLED.getCode())
                .orderStatusName(OrderStatusEnum.CANCELLED.getDescription())
                .cancelledAt(LocalDateTime.now())
                .build();
    }


    /**
     * 下单过程内部用的上下文
     */
    private record ItemCtx(
            Long itemId,
            BigDecimal unitPrice,
            BigDecimal itemSubtotal,
            List<ExtraQuote> recordExtras) { }
}