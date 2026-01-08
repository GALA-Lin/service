package com.unlimited.sports.globox.coach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.coach.mapper.CoachCourseTypeMapper;
import com.unlimited.sports.globox.coach.service.ICoachCourseTypeService;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.model.coach.dto.CoachCourseTypeDto;
import com.unlimited.sports.globox.model.coach.entity.CoachCourseType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @since 2026/1/8 10:09
 *
 */
@Slf4j
@Service
public class CoachCourseTypeServiceImpl implements ICoachCourseTypeService {

    @Autowired
    private CoachCourseTypeMapper coachCourseTypeMapper;

    /**
     * 创建或更新课程类型
     * 如果该服务类型已存在，则更新；否则创建新的
     * 创建或更新课程类型
     * 每种服务类型目前仅允许有一种
     *
     * @param dto 课程类型DTO
     * @return 课程类型ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrUpdateCourseType(CoachCourseTypeDto dto) {
        log.info("创建/更新课程类型 - coachUserId: {}, serviceType: {}",
                dto.getCoachUserId(), dto.getCoachServiceTypeEnum());

        // 检查该教练该服务类型是否已存在
        CoachCourseType existing = coachCourseTypeMapper.selectOne(
                new LambdaQueryWrapper<CoachCourseType>()
                        .eq(CoachCourseType::getCoachUserId, dto.getCoachUserId())
                        .eq(CoachCourseType::getCoachServiceTypeEnum, dto.getCoachServiceTypeEnum())
        );

        if (existing != null) {
            // 更新现有课程类型
            existing.setCourseCover(dto.getCourseCover());
            existing.setCoachCourseTypeName(dto.getCoachCourseTypeName());
            existing.setCoachDuration(dto.getCoachDuration());
            existing.setCoachPrice(dto.getCoachPrice());
            existing.setCoachDescription(dto.getCoachDescription());
            existing.setCoachIsActive(dto.getCoachIsActive());

            coachCourseTypeMapper.updateById(existing);
            log.info("更新课程类型成功 - courseTypeId: {}", existing.getCoachCourseTypeId());
            return existing.getCoachCourseTypeId();
        } else {
            // 创建新的课程类型
            CoachCourseType courseType = new CoachCourseType();
            courseType.setCoachUserId(dto.getCoachUserId());
            courseType.setCourseCover(dto.getCourseCover());
            courseType.setCoachCourseTypeName(dto.getCoachCourseTypeName());
            courseType.setCoachServiceTypeEnum(dto.getCoachServiceTypeEnum());
            courseType.setCoachDuration(dto.getCoachDuration());
            courseType.setCoachPrice(dto.getCoachPrice());
            courseType.setCoachDescription(dto.getCoachDescription());
            courseType.setCoachIsActive(dto.getCoachIsActive());

            coachCourseTypeMapper.insert(courseType);
            log.info("创建课程类型成功 - courseTypeId: {}", courseType.getCoachCourseTypeId());
            return courseType.getCoachCourseTypeId();
        }
    }


    /**
     * 删除课程类型（软删除）
     *
     * @param coachUserId  教练ID
     * @param courseTypeId 课程类型ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCourseType(Long coachUserId, Long courseTypeId) {
        log.info("删除课程类型 - coachUserId: {}, courseTypeId: {}", coachUserId, courseTypeId);

        CoachCourseType courseType = coachCourseTypeMapper.selectById(courseTypeId);
        if (courseType == null) {
            throw new GloboxApplicationException("课程类型不存在");
        }

        if (!courseType.getCoachUserId().equals(coachUserId)) {
            throw new GloboxApplicationException("无权限删除该课程类型");
        }

        courseType.setCoachIsActive(0);

        log.info("删除课程类型成功");
    }

    /**
     * 获取教练的所有课程类型
     *
     * @param coachUserId 教练ID
     * @return 课程类型列表
     */
    @Override
    public List<CoachCourseType> getCoachCourseTypes(Long coachUserId) {
        log.info("获取教练课程类型列表 - coachUserId: {}", coachUserId);

        return coachCourseTypeMapper.selectList(
                new LambdaQueryWrapper<CoachCourseType>()
                        .eq(CoachCourseType::getCoachUserId, coachUserId)
                        .orderByDesc(CoachCourseType::getCoachIsActive)
                        .orderByAsc(CoachCourseType::getCoachServiceTypeEnum)
        );
    }

    /**
     * 启用/禁用课程类型
     *
     * @param coachUserId  教练ID
     * @param courseTypeId 课程类型ID
     * @param isActive     是否启用
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleCourseTypeStatus(Long coachUserId, Long courseTypeId, Integer isActive) {
        log.info("切换课程类型状态 - coachUserId: {}, courseTypeId: {}, isActive: {}",
                coachUserId, courseTypeId, isActive);

        CoachCourseType courseType = coachCourseTypeMapper.selectById(courseTypeId);
        if (courseType == null) {
            throw new GloboxApplicationException("课程类型不存在");
        }

        if (!courseType.getCoachUserId().equals(coachUserId)) {
            throw new GloboxApplicationException("无权限修改该课程类型");
        }

        courseType.setCoachIsActive(isActive);
        coachCourseTypeMapper.updateById(courseType);
        log.info("切换课程类型状态成功");
    }
}
