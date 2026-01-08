package com.unlimited.sports.globox.coach.service;

import com.unlimited.sports.globox.model.coach.dto.CoachCourseTypeDto;
import com.unlimited.sports.globox.model.coach.entity.CoachCourseType;

import java.util.List;

/**
 * @since 2026/1/8 10:08
 * 教练课程类型服务接口
 */
public interface ICoachCourseTypeService {

    /**
     * 创建或更新课程类型
     * 如果该服务类型已存在，则更新；否则创建新的
     *
     * @param dto 课程类型DTO
     * @return 课程类型ID
     */
    Long saveOrUpdateCourseType(CoachCourseTypeDto dto);

    /**
     * 删除课程类型（软删除）
     *
     * @param coachUserId 教练ID
     * @param courseTypeId 课程类型ID
     */
    void deleteCourseType(Long coachUserId, Long courseTypeId);

    /**
     * 获取教练的所有课程类型
     *
     * @param coachUserId 教练ID
     * @return 课程类型列表
     */
    List<CoachCourseType> getCoachCourseTypes(Long coachUserId);

    /**
     * 启用/禁用课程类型
     *
     * @param coachUserId 教练ID
     * @param courseTypeId 课程类型ID
     * @param isActive 是否启用
     */
    void toggleCourseTypeStatus(Long coachUserId, Long courseTypeId, Integer isActive);
}