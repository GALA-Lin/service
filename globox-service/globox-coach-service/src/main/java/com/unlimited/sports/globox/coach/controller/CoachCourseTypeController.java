package com.unlimited.sports.globox.coach.controller;

import com.unlimited.sports.globox.coach.service.ICoachCourseTypeService;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.coach.dto.CoachCourseTypeBatchDto;
import com.unlimited.sports.globox.model.coach.dto.CoachCourseTypeDto;
import com.unlimited.sports.globox.model.coach.entity.CoachCourseType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * @since 2026/1/8 10:12
 * 教练课程类型管理接口
 */
@Slf4j
@RestController
@RequestMapping("/coach/course-types")
public class CoachCourseTypeController {

    @Autowired
    private ICoachCourseTypeService courseTypeService;

    /**
     * 创建或更新课程类型
     * 如果该服务类型已存在，则更新；否则创建新的
     * 每种服务类型（1-4）仅允许有一种
     */

    @PostMapping("/batch")
    public R<Map<Integer, Long>> batchSaveOrUpdateCourseTypes(
            @Valid @RequestBody CoachCourseTypeBatchDto batchDto,
            @RequestHeader("X-User-Id") Long coachUserId) {

        log.info("批量创建/更新课程类型 - coachUserId: {}, 课程数量: {}",
                coachUserId, batchDto.getCourseTypes().size());

        // 设置教练ID
        batchDto.setCoachUserId(coachUserId);
        batchDto.getCourseTypes().forEach(dto -> dto.setCoachUserId(coachUserId));

        // 批量保存
        Map<Integer, Long> resultMap = courseTypeService.batchSaveOrUpdateCourseTypes(batchDto);

        return R.ok(resultMap);
    }

    @PostMapping
    public R<Long> saveOrUpdateCourseType(
            @Valid @RequestBody CoachCourseTypeDto dto,
            @RequestHeader("X-User-Id") Long coachUserId) {

        log.info("创建/更新课程类型 - coachUserId: {}, serviceType: {}",
                coachUserId, dto.getCoachServiceTypeEnum());

        dto.setCoachUserId(coachUserId);
        Long courseTypeId = courseTypeService.saveOrUpdateCourseType(dto);

        return R.ok(courseTypeId);
    }

    /**
     * 获取教练的所有课程类型
     */
    @GetMapping
    public R<List<CoachCourseType>> getCourseTypes(
            @RequestHeader("X-User-Id") Long coachUserId) {

        log.info("获取课程类型列表 - coachUserId: {}", coachUserId);

        List<CoachCourseType> courseTypes = courseTypeService.getCoachCourseTypes(coachUserId);
        return R.ok(courseTypes);
    }

    /**
     * 删除课程类型
     */
    @DeleteMapping("/{courseTypeId}")
    public R<Void> deleteCourseType(
            @PathVariable Long courseTypeId,
            @RequestHeader("X-User-Id") Long coachUserId) {

        log.info("删除课程类型 - coachUserId: {}, courseTypeId: {}", coachUserId, courseTypeId);

        courseTypeService.deleteCourseType(coachUserId, courseTypeId);
        return R.ok();
    }

    /**
     * 启用/禁用课程类型
     */
    @PatchMapping("/{courseTypeId}/status")
    public R<Void> toggleStatus(
            @PathVariable Long courseTypeId,
            @RequestParam Integer isActive,
            @RequestHeader("X-User-Id") Long coachUserId) {

        log.info("切换课程类型状态 - coachUserId: {}, courseTypeId: {}, isActive: {}",
                coachUserId, courseTypeId, isActive);

        courseTypeService.toggleCourseTypeStatus(coachUserId, courseTypeId, isActive);
        return R.ok();
    }
}