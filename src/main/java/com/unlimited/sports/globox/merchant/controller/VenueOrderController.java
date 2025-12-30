package com.unlimited.sports.globox.merchant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.merchant.mapper.VenueStaffMapper;
import com.unlimited.sports.globox.merchant.service.VenueOrderService;
import com.unlimited.sports.globox.model.merchant.dto.OrderCancelDto;
import com.unlimited.sports.globox.model.merchant.dto.OrderQueryDto;
import com.unlimited.sports.globox.model.merchant.vo.OrderCancelResultVo;
import com.unlimited.sports.globox.model.merchant.vo.VenueOrderStatisticsVo;
import com.unlimited.sports.globox.model.merchant.vo.VenueOrderVo;
import com.unlimited.sports.globox.model.merchant.entity.VenueStaff;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import static com.unlimited.sports.globox.common.constants.RequestHeaderConstants.HEADER_USER_ID;
import static com.unlimited.sports.globox.common.result.UserAuthCode.TOKEN_EXPIRED;
/**
 * @author Linsen Hu
 * @since 2025/12/22 12:13
 * 商家订单管理controller
 */
@Tag(name = "订单管理")
@Slf4j
@RestController
@RequestMapping("/merchant/orders")
@RequiredArgsConstructor
@Validated
public class VenueOrderController {
    private final VenueOrderService venueOrderService;
    private final VenueStaffMapper venueStaffMapper;

    /**
     * 分页查询商家订单列表
     * @param userId   当前登录用户ID（从token或session中获取）
     * @param queryDTO 查询条件
     * @return 分页订单列表
     */
    @Operation(summary = "分页查询商家订单列表")
    @GetMapping("/list")
    public R<IPage<VenueOrderVo>> queryOrders(
            @RequestHeader(value = HEADER_USER_ID , required = false) Long userId,
            @Valid OrderQueryDto queryDTO) {
        if (userId == null) {
            log.error("请求头中缺少{}",HEADER_USER_ID);
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }
        // 从商家职工关联表查询merchant_id
        Long merchantId = getMerchantIdByUserId(userId);

        IPage<VenueOrderVo> orderPage = venueOrderService.queryMerchantOrders(merchantId, queryDTO);
        return R.ok(orderPage);
    }

    /**
     * 查询订单详情
     * @param userId  当前登录用户ID
     * @param orderId 订单ID
     * @return 订单详情
     */
    @Operation(summary = "查询订单详情")
    @GetMapping("/{orderId}")
    public R<VenueOrderVo> getOrderDetail(
            @RequestHeader(value = HEADER_USER_ID , required = false) Long userId,
            @PathVariable @NotNull(message = "订单ID不能为空") Long orderId) {
        if (userId == null) {
            log.error("请求头中缺少{}",HEADER_USER_ID);
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }
        // 从商家职工关联表查询merchant_id
        Long merchantId = getMerchantIdByUserId(userId);

        VenueOrderVo orderDetail = venueOrderService.getOrderDetail(merchantId, orderId);
        return R.ok(orderDetail);
    }

    /**
     * 取消订单（全部/部分）
     * @param userId    当前登录用户ID
     * @param cancelDTO 取消订单DTO
     * @return 操作结果
     */
    @Operation(summary = "取消订单（全部/部分）")
    @PostMapping("/cancel")
    public R<OrderCancelResultVo> cancelOrder(
            @RequestHeader(value = HEADER_USER_ID , required = false) Long userId,
            @Valid @RequestBody OrderCancelDto cancelDTO) {
        if (userId == null) {
            log.error("请求头中缺少{}",HEADER_USER_ID);
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }
        Long merchantId = getMerchantIdByUserId(userId);

        // 返回详细的取消结果
        OrderCancelResultVo result = venueOrderService.cancelOrder(merchantId, cancelDTO);

        return R.ok(result);
    }

    /**
     * 确认订单
     * @param userId  当前登录用户ID
     * @param orderId 订单ID
     * @return 操作结果
     */
    @Operation(summary = "确认订单")
    @PostMapping("/{orderId}/confirm")
    public R<VenueOrderVo> confirmOrder(
            @RequestHeader(value = HEADER_USER_ID , required = false) Long userId,
            @PathVariable @NotNull(message = "订单ID不能为空") Long orderId) {
        if (userId == null) {
            log.error("请求头中缺少{}",HEADER_USER_ID);
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }
        // 从商家职工关联表查询merchant_id
        Long merchantId = getMerchantIdByUserId(userId);

        VenueOrderVo orderDetail = venueOrderService.confirmOrder(merchantId, orderId);
        return R.ok(orderDetail);
    }

    /**
     * 获取订单统计数据
     * @param userId  当前登录用户ID
     * @param venueId 场馆ID（可选）
     * @return 订单统计数据
     */
    @Operation(summary = "获取订单统计数据")
    @GetMapping("/statistics")
    public R<VenueOrderStatisticsVo> getStatistics(
            @RequestHeader(value = HEADER_USER_ID , required = false) Long userId,
            @RequestParam(required = false) Long venueId) {
        if (userId == null) {
            log.error("请求头中缺少{}",HEADER_USER_ID);
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }
        // 从商家职工关联表查询merchant_id
        Long merchantId = getMerchantIdByUserId(userId);

        VenueOrderStatisticsVo statistics = venueOrderService.getOrderStatistics(merchantId, venueId);
        return R.ok(statistics);
    }

    /**
     * 根据用户ID从商家职工关联表查询merchant_id
     * @param userId 用户ID
     * @return 商家ID
     * @throws GloboxApplicationException 如果用户不是商家员工
     */
    private Long getMerchantIdByUserId(Long userId) {
        // 查询该用户在商家职工关联表中的记录（只查询在职状态）
        VenueStaff venueStaff = venueStaffMapper.selectActiveStaffByUserId(userId);

        if (venueStaff == null) {
            log.error("用户ID: {} 不是任何商家的员工", userId);
            throw new GloboxApplicationException("您不是商家员工，无权访问此资源");
        }

        log.debug("用户ID: {} 对应的商家ID: {}", userId, venueStaff.getMerchantId());
        return venueStaff.getMerchantId();
    }
}
