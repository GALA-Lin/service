package com.unlimited.sports.globox.merchant.controller;

import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.merchant.service.SlotTemplateService;
import com.unlimited.sports.globox.merchant.service.VenueSlotRecordService;
import com.unlimited.sports.globox.merchant.util.MerchantAuthContext;
import com.unlimited.sports.globox.merchant.util.MerchantAuthUtil;
import com.unlimited.sports.globox.model.merchant.dto.BatchTemplateInitDto;
import com.unlimited.sports.globox.model.merchant.vo.BatchTemplateInitResultVo;
import com.unlimited.sports.globox.model.merchant.vo.SlotAvailabilityVo;
import com.unlimited.sports.globox.model.merchant.vo.SlotGenerationResultVo;
import com.unlimited.sports.globox.model.merchant.vo.VenueSlotAvailabilityVo;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static com.unlimited.sports.globox.common.constants.RequestHeaderConstants.HEADER_MERCHANT_ACCOUNT_ID;
import static com.unlimited.sports.globox.merchant.util.MerchantConstants.HEADER_MERCHANT_ID;
import static com.unlimited.sports.globox.merchant.util.MerchantConstants.HEADER_MERCHANT_ROLE;

/**
 * @since 2025/12/28 11:17
 * 时段管理controller
 */
@RestController
@RequestMapping("/merchant/slots")
@RequiredArgsConstructor
public class SlotManagementController {

    private final SlotTemplateService templateService;
    private final VenueSlotRecordService recordService;
    private final MerchantAuthUtil merchantAuthUtil;

    /**
     * 初始化场地时段模板
     *
     * @param courtId   场地ID
     * @param openTime  开放时间
     * @param closeTime 关闭时间
     * @return 模板初始化结果
     */
    @PostMapping("/templates/init")
    public R<Integer> initTemplates(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @RequestParam @NotNull Long courtId,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime openTime,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime closeTime) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId,merchantId, roleStr);

        // 验证场地访问权限
        merchantAuthUtil.validateCourtAccess(context, courtId);

        int count = templateService.initializeTemplatesForCourt(courtId, openTime, closeTime);
        return R.ok(count);
    }

    /**
     * 【新增】批量初始化场地时段模板
     *
     * @param dto 批量初始化请求参数
     * @return 批量初始化结果
     */
    @PostMapping("/templates/batch-init")
    public R<BatchTemplateInitResultVo> batchInitTemplates(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @RequestBody @Valid BatchTemplateInitDto dto) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId,merchantId, roleStr);

        // 验证所有场地的访问权限
        dto.getCourtIds().forEach(courtId ->
                merchantAuthUtil.validateCourtAccess(context, courtId));

        BatchTemplateInitResultVo result = templateService.batchInitializeTemplates(dto);
        return R.ok(result);
    }

    /**
     * 生成指定日期的时段记录
     *
     * @param courtId   场地ID
     * @param date      日期
     * @param overwrite 是否覆盖
     * @return 生成结果
     */
    @PostMapping("/records/generate-daily")
    public R<SlotGenerationResultVo> generateDaily(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @RequestParam @NotNull Long courtId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(defaultValue = "false") boolean overwrite) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId,merchantId, roleStr);

        merchantAuthUtil.validateCourtAccess(context, courtId);

        SlotGenerationResultVo result = recordService.generateRecordsForDate(courtId, date, overwrite);
        return R.ok(result);
    }

    /**
     * 生成指定日期范围的时段记录
     *
     * @param courtId   场地ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param overwrite 是否覆盖
     * @return 生成结果
     */
    @PostMapping("/records/generate-batch")
    public R<SlotGenerationResultVo> generateBatch(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @RequestParam @NotNull Long courtId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "false") boolean overwrite) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId,merchantId, roleStr);

        merchantAuthUtil.validateCourtAccess(context, courtId);

        SlotGenerationResultVo result = recordService.generateRecordsForDateRange(
                courtId, startDate, endDate, overwrite);
        return R.ok(result);
    }

    /**
     * 查询指定日期的时段可用性
     *
     * @param courtId 场地ID
     * @param date    日期
     * @return 可用性列表
     */
    @GetMapping("/records/availability")
    public R<List<SlotAvailabilityVo>> queryAvailability(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @RequestParam @NotNull Long courtId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId,merchantId, roleStr);

        merchantAuthUtil.validateCourtAccess(context, courtId);

        List<SlotAvailabilityVo> result = recordService.queryAvailability(courtId, date);
        return R.ok(result);
    }

    /**
     * 【新增】查询场馆下所有场地某日的时段可用性
     *
     * @param venueId 场馆ID
     * @param date    日期
     * @param startTime 开始时间（可选，筛选时间范围）
     * @param endTime   结束时间（可选，筛选时间范围）
     * @return 场馆级时段可用性列表（按场地分组）
     */
    @GetMapping("/records/venue-availability")
    public R<List<VenueSlotAvailabilityVo>> queryVenueAvailability(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @RequestParam @NotNull Long venueId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime endTime) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId,merchantId, roleStr);
        merchantAuthUtil.validateVenueAccess(context, merchantId);

        List<VenueSlotAvailabilityVo> result = recordService.queryVenueAvailability(
                venueId, date, startTime, endTime);
        return R.ok(result);
    }
}