package com.unlimited.sports.globox.merchant.controller;

import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.merchant.service.SlotTemplateService;
import com.unlimited.sports.globox.merchant.service.VenueSlotRecordService;
import com.unlimited.sports.globox.model.merchant.vo.SlotAvailabilityVo;
import com.unlimited.sports.globox.model.merchant.vo.SlotGenerationResultVo;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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
            @RequestParam @NotNull Long courtId,
                @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime openTime,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime closeTime) {

        int count = templateService.initializeTemplatesForCourt(courtId, openTime, closeTime);
        return R.ok(count);
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
            @RequestParam @NotNull Long courtId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        List<SlotAvailabilityVo> result = recordService.queryAvailability(courtId, date);
        return R.ok(result);
    }
}
