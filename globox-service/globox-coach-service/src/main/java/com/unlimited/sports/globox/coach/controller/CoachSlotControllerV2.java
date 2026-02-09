package com.unlimited.sports.globox.coach.controller;

import com.unlimited.sports.globox.coach.service.impl.CoachSlotServiceOptimizedImpl;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.coach.dto.*;
import com.unlimited.sports.globox.model.coach.vo.CoachAvailableSlotVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

import static com.unlimited.sports.globox.common.constants.RequestHeaderConstants.HEADER_USER_ID;

/**
 * 教练时段管理接口（优化版）
 */
@Slf4j
@RestController
@RequestMapping("/coach/slots/v2")
public class CoachSlotControllerV2 {

    @Autowired
    private CoachSlotServiceOptimizedImpl coachSlotServiceOptimized;

    /**
     * 灵活开放时段（新接口）
     * 支持三种模式：
     * 1. SINGLE: 单次开放（如明天9:00-10:00）
     * 2. BATCH: 批量开放（如1月1日、1月3日、1月5日都开放9:00-10:00）
     * 3. TEMPLATE: 长期模板（如每周一9:00-10:00，自动生成未来30天的记录）
     *
     * @param dto 开放参数
     * @param coachUserId 教练ID（从请求头获取）
     * @return 创建的记录数量
     */
    @PostMapping("/open")
    public R<Integer> openSlotsFlexibly(
            @Valid @RequestBody CoachFlexibleSlotOpenDto dto,
            @RequestHeader(HEADER_USER_ID) Long coachUserId) {

        log.info("灵活开放时段 - coachUserId: {}, mode: {}", coachUserId, dto.getOpenMode());
        dto.setCoachUserId(coachUserId);

        int createdCount = coachSlotServiceOptimized.openSlotsFlexibly(dto);
        return R.ok(createdCount);
    }

    /**
     * 查询可预约时段（优化版）
     * 查询逻辑改进：
     * - 只查询数据库中已存在的AVAILABLE记录
     * - 不再依赖模板自动生成
     * - 教练需要主动开放时段才能被预约
     *
     * @param dto 查询参数
     * @return 按日期分组的可预约时段
     */
    @GetMapping("/available/v2")
    public R<Map<String, List<CoachAvailableSlotVo>>> getAvailableSlotsV2(
            @Valid CoachAvailableSlotQueryDto dto) {

        log.info("查询可预约时段（优化版） - coachUserId: {}, {} - {}",
                dto.getCoachUserId(), dto.getStartDate(), dto.getEndDate());

        Map<String, List<CoachAvailableSlotVo>> slots =
                coachSlotServiceOptimized.getAvailableSlotsOptimized(dto);
        return R.ok(slots);
    }

    /**
     * 批量删除时段记录
     * 用于教练取消已开放的时段
     *
     * @param recordIds 时段记录ID列表
     * @param coachUserId 教练ID
     * @return 删除数量
     */
    @DeleteMapping("/records/batch")
    public R<Integer> batchDeleteSlotRecords(
            @RequestBody List<Long> recordIds,
            @RequestHeader(HEADER_USER_ID) Long coachUserId) {

        log.info("批量删除时段记录 - coachUserId: {}, recordIds数量: {}",
                coachUserId, recordIds.size());

        int deletedCount = coachSlotServiceOptimized.batchDeleteSlotRecords(recordIds, coachUserId);
        return R.ok(deletedCount);
    }
}