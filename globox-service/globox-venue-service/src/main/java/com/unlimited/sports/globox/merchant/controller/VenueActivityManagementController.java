package com.unlimited.sports.globox.merchant.controller;

import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.merchant.service.VenueActivityManagementService;
import com.unlimited.sports.globox.merchant.util.MerchantAuthContext;
import com.unlimited.sports.globox.merchant.util.MerchantAuthUtil;
import com.unlimited.sports.globox.model.venue.dto.CreateActivityDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static com.unlimited.sports.globox.merchant.util.MerchantConstants.HEADER_EMPLOYEE_ID;
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
    public R<Long> createActivity(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @Valid @RequestBody CreateActivityDto dto) {

        log.info("商家创建活动 - employeeId: {}, role: {}, activityName: {}", employeeId, roleStr, dto.getActivityName());

        // 验证权限并获取上下文
        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);
        Long activityId = activityManagementService.createActivity(dto, context);

        log.info("活动创建成功 - activityId: {}", activityId);

        return R.ok(activityId);
    }
}
