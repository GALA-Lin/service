package com.unlimited.sports.globox.merchant.controller;

import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.merchant.service.VenueActivityManagementService;
import com.unlimited.sports.globox.merchant.util.MerchantAuthContext;
import com.unlimited.sports.globox.merchant.util.MerchantAuthUtil;
import com.unlimited.sports.globox.model.merchant.vo.ActivityCreationResultVo;
import com.unlimited.sports.globox.model.merchant.vo.MerchantActivityDetailVo;
import com.unlimited.sports.globox.model.venue.dto.CreateActivityDto;
import com.unlimited.sports.globox.model.venue.dto.UpdateActivityDto;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

import static com.unlimited.sports.globox.common.constants.RequestHeaderConstants.HEADER_MERCHANT_ACCOUNT_ID;
import static com.unlimited.sports.globox.merchant.util.MerchantConstants.HEADER_MERCHANT_ID;
import static com.unlimited.sports.globox.merchant.util.MerchantConstants.HEADER_MERCHANT_ROLE;

/**
 * 活动管理Controller
 * 用于商家端管理活动（畅打、比赛、培训等）
 */
@Slf4j
@RestController
@RequestMapping("/merchant/activities")
@RequiredArgsConstructor
public class VenueActivityManagementController {

    private final VenueActivityManagementService activityManagementService;
    private final MerchantAuthUtil merchantAuthUtil;

    /**
     * 创建活动
     *
     * @param employeeId 员工ID
     * @param roleStr    员工角色
     * @param dto        创建活动请求
     * @return 活动ID
     */
    @PostMapping
    public R<ActivityCreationResultVo> createActivity(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @Valid @RequestBody CreateActivityDto dto) {

        log.info("商家创建活动 - employeeId: {}, role: {}, activityName: {}",
                employeeId, roleStr, dto.getActivityName());

        // 验证权限并获取上下文
        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, merchantId, roleStr);
        ActivityCreationResultVo result = activityManagementService.createActivity(dto, context);

        log.info("活动创建成功 - activityId: {}, batchId: {}, 占用槽位数: {}",
                result.getActivityId(), result.getMerchantBatchId(),
                result.getOccupiedSlots().size());

        return R.ok(result);
    }


    /**
     * 更新活动
     *
     * @param employeeId 员工ID
     * @param roleStr    员工角色
     * @param activityId 活动ID
     * @param dto        更新活动请求
     * @return 更新后的活动详情
     */
    @PutMapping("/{activityId}")
    public R<ActivityCreationResultVo> updateActivity(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @PathVariable Long activityId,
            @Valid @RequestBody UpdateActivityDto dto) {

        log.info("商家更新活动 - employeeId: {}, role: {}, activityId: {}",
                employeeId, roleStr, activityId);

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, merchantId, roleStr);
        ActivityCreationResultVo result = activityManagementService.updateActivity(activityId, dto, context);

        log.info("活动更新成功 - activityId: {}", activityId);

        return R.ok(result);
    }


    @PostMapping("/cancel")
    public R<Void> cancelActivity(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @Valid @RequestBody CancelActivityRequest request) {

        log.info("商家发起取消活动 - employeeId: {}, activityId: {}", employeeId, request.getActivityId());

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, merchantId, roleStr);

        activityManagementService.cancelActivity(request.getActivityId(), context, request.getCancelReason());

        return R.ok();
    }

    @Data
    public static class CancelActivityRequest {
        @NotNull(message = "活动ID不能为空")
        private Long activityId;

        private String cancelReason;
    }

    /**
     * 查询商家所有活动列表
     */
    @GetMapping("/list")
    public R<List<ActivityCreationResultVo>> listMerchantActivities(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr){

        log.info("商家查询活动列表 - employeeId: {}", employeeId);
        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, merchantId, roleStr);

        List<ActivityCreationResultVo> result = activityManagementService.getMerchantActivities(context);
        return R.ok(result);
    }

    /**
     * 根据场馆查询活动
     */
    @GetMapping("/{venueId}/list")
    public R<List<ActivityCreationResultVo>> getActivitiesByVenue(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @PathVariable Long venueId,
            @RequestParam(required = false)LocalDate activityDate) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, merchantId, roleStr);
        // 验证场馆访问权限
        merchantAuthUtil.validateVenueAccess(context, venueId);

        List<ActivityCreationResultVo> result = activityManagementService.getActivitiesByVenueId(venueId,activityDate);
        return R.ok(result);
    }

    /**
     * 获取活动详情（商家端）
     * 包含活动基本信息和参与者列表
     */
    @GetMapping("/{activityId}/detail")
    public R<MerchantActivityDetailVo> getActivityDetail(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @PathVariable Long activityId) {

        log.info("商家查询活动详情 - employeeId: {}, activityId: {}", employeeId, activityId);
        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, merchantId, roleStr);

        MerchantActivityDetailVo result = activityManagementService.getActivityDetail(activityId, context);
        return R.ok(result);
    }
}