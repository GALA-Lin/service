package com.unlimited.sports.globox.order.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.common.constants.OrderMQConstants;
import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.enums.order.*;
import com.unlimited.sports.globox.common.message.order.OrderAutoCancelMessage;
import com.unlimited.sports.globox.common.message.order.UnlockSlotMessage;
import com.unlimited.sports.globox.common.result.OrderCode;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.UserAuthCode;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.common.utils.AuthContextHolder;
import com.unlimited.sports.globox.common.utils.IdGenerator;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.dubbo.merchant.MerchantDubboService;
import com.unlimited.sports.globox.dubbo.merchant.dto.*;
import com.unlimited.sports.globox.model.order.dto.CreateVenueActivityOrderDto;
import com.unlimited.sports.globox.model.order.dto.CreateVenueOrderDto;
import com.unlimited.sports.globox.model.order.dto.GetOrderDetailsDto;
import com.unlimited.sports.globox.model.order.dto.GetOrderPageDto;
import com.unlimited.sports.globox.model.order.entity.*;
import com.unlimited.sports.globox.model.order.vo.*;
import com.unlimited.sports.globox.model.order.vo.GetOrderDetailsVo.ExtraChargeVo;
import com.unlimited.sports.globox.order.constants.RedisConsts;
import com.unlimited.sports.globox.order.lock.RedisLock;
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
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * order 服务层
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    /**
     * 订单自动关闭时间 单位 m
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

    @Autowired
    private MQService mqService;

    @Lazy
    @Autowired
    private OrderServiceImpl thisService;

    @Autowired
    private JsonUtils jsonUtils;


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
        PricingResultDto result = merchantDubboService.quoteVenue(pricingRequestDto);
//        PricingResultDto result = getPricingResultDto();

        Assert.isNotEmpty(result.getRecordQuote(), OrderCode.SLOT_HAD_BOOKING);

        log.info("PricingResultDto: {}", jsonUtils.objectToJson(result));

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
        PricingActivityResultDto result = merchantDubboService.quoteVenueActivity(requestDto);

        Assert.isNotEmpty(result.getRecordQuote(), OrderCode.SLOT_HAD_BOOKING);

        return thisService.createVenueOrderAction(result, userId, true);
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

        // 生成对外订单号
        String outTradeNo = "globox" + UUID.randomUUID().toString().replaceAll("-","");

        Orders order = Orders.builder()
                .orderNo(orderNo)
                .outTradeNo(outTradeNo)
                .sourcePlatform(result.getSourcePlatform())
                .buyerId(userId)
                .sellerName(result.getSellerName())
                .sellerType(SellerTypeEnum.VENUE)
                .sellerId(result.getSellerId())
                .orderStatus(OrderStatusEnum.PENDING)
                .paymentStatus(OrdersPaymentStatusEnum.UNPAID)
                .paymentType(PaymentTypeEnum.NONE)
                .refundApplyId(null)
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
                .slotIds(result.getRecordQuote().stream().map(RecordQuote::getRecordId).collect(Collectors.toList()))
                .userId(userId)
                .build();

        // 注册事务回调
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        // 事务成功，发成功事件
                        mqService.send(
                                OrderMQConstants.EXCHANGE_TOPIC_ORDER_CREATED,
                                OrderMQConstants.ROUTING_ORDER_CREATED,
                                order);

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
                                    .bookingDate(result.getBookingDate())
                                    .recordIds(itemCtxList.stream().map(item -> item.itemId).collect(Collectors.toList()))
                                    .build();
                            message.setUserId(userId);
                            log.error("发送取消锁场消息:{}", jsonUtils.objectToJson(message));
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
    public PaginationResult<GetOrderVo> getOrderPage(GetOrderPageDto pageDto) {

        Long userId = AuthContextHolder.getLongHeader(RequestHeaderConstants.HEADER_USER_ID);
        Assert.isNotEmpty(userId, UserAuthCode.TOKEN_EXPIRED);

        // 1. 构建分页对象
        Page<Orders> page = new Page<>(
                pageDto.getPageNum(),
                pageDto.getPageSize());

        // 2. 分页查询订单主表
        IPage<Orders> orderPage = ordersMapper.selectPage(
                page,
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getBuyerId, userId)
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
                .map(order -> {

                    List<OrderItems> orderItems =
                            itemMap.getOrDefault(order.getOrderNo(), List.of());

                    if (orderItems.isEmpty()) {
                        return null;
                    }

                    OrderItems firstItem = orderItems.get(0);

                    List<SlotBookingTime> slotTimes =
                            orderItems.stream()
                                    .map(item -> {
                                        SlotBookingTime t =
                                                new SlotBookingTime();
                                        t.setStartTime(item.getStartTime());
                                        t.setEndTime(item.getEndTime());
                                        return t;
                                    })
                                    .toList();

                    OrderActivities orderActivities = orderActivitiesMapper.selectOne(
                            Wrappers.<OrderActivities>lambdaQuery()
                                    .eq(OrderActivities::getOrderNo, order.getOrderNo())
                    );

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

            VenueSnapshotResultDto snap = merchantDubboService.getVenueSnapshot(req);
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
        }

        // 6) bookingDate：一般同一单同一天；这里取第一个
        LocalDate bookingDate = items.get(0).getBookingDate();

        // 7) 组装 item VO
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

            return GetOrderDetailsVo.OrderItemDetailVo.builder()
                    .itemId(item.getId())
                    .courtSnapshot(courtVo)
                    .itemBaseAmount(item.getUnitPrice())
                    .itemAmount(item.getSubtotal())
                    .extraCharges(itemExtraMap.getOrDefault(item.getId(), List.of()))
                    .refundStatus(item.getRefundStatus())
                    .slotBookingTimes(List.of(time))
                    .refund(refundVo)
                    .build();
        }).toList();

        // 8) 返回
        GetOrderDetailsVo detailsVo = GetOrderDetailsVo.builder()
                .orderNo(orders.getOrderNo())
                .orderType(orders.getSellerType())
                .venueSnapshot(venueSnapshotVo)
                .sellerName(orders.getSellerName())
                .amount(orders.getPayAmount())
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
                .recordIds(recordIds)
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


    //TODO ETA 2026/01/03 TEST
    private PricingResultDto getPricingResultDto() {
        return PricingResultDto.builder()
                .recordQuote(List.of(
                        RecordQuote.builder()
                                .recordId(2000003L)
                                .courtId(2000001L)
                                .courtName("1号红土场 14:00-15:00")
                                .bookingDate(LocalDate.of(2026, 1, 2))
                                .startTime(LocalTime.of(14, 0))
                                .endTime(LocalTime.of(15, 0))
                                .unitPrice(new BigDecimal("150.00"))
                                .recordExtras(List.of(
                                        ExtraQuote.builder()
                                                .chargeTypeId(19L)
                                                .chargeName("灯光费")
                                                .chargeMode(ChargeModeEnum.FIXED)
                                                .fixedValue(new BigDecimal("20.00"))
                                                .amount(new BigDecimal("20.00"))
                                                .build()
                                ))
                                .build(),

                        RecordQuote.builder()
                                .recordId(2000004L)
                                .courtId(2000001L)
                                .courtName("1号红土场 18:00-19:00")
                                .bookingDate(LocalDate.of(2026, 1, 2))
                                .startTime(LocalTime.of(18, 0))
                                .endTime(LocalTime.of(19, 0))
                                .unitPrice(new BigDecimal("180.00"))
                                .recordExtras(List.of(
                                        ExtraQuote.builder()
                                                .chargeTypeId(19L)
                                                .chargeName("灯光费")
                                                .chargeMode(ChargeModeEnum.FIXED)
                                                .fixedValue(new BigDecimal("20.00"))
                                                .amount(new BigDecimal("20.00"))
                                                .build()
                                ))
                                .build()
                ))
                .orderLevelExtras(List.of(
                        OrderLevelExtraQuote.builder()
                                .chargeTypeId(20L)
                                .chargeName("清洁费")
                                .chargeMode(ChargeModeEnum.FIXED)
                                .fixedValue(new BigDecimal("10.00"))
                                .amount(new BigDecimal("10.00"))
                                .build()
                ))
                .sourcePlatform(1)
                .sellerName("成都锦江国际网球中心")
                .sellerId(2000001L)
                .bookingDate(LocalDate.of(2026, 1, 2))
                .build();
    }
}




