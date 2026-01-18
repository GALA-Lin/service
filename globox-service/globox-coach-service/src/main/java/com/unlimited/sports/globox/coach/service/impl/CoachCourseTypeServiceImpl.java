package com.unlimited.sports.globox.coach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.coach.mapper.CoachCourseTypeMapper;
import com.unlimited.sports.globox.coach.service.ICoachCourseTypeService;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.model.coach.dto.CoachCourseTypeBatchDto;
import com.unlimited.sports.globox.model.coach.dto.CoachCourseTypeDto;
import com.unlimited.sports.globox.model.coach.entity.CoachCourseType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
     * 批量创建或更新课程类型
     *
     * @param batchDto 批量请求DTO
     * @return 服务类型 -> 课程类型ID 的映射
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<Integer, Long> batchSaveOrUpdateCourseTypes(CoachCourseTypeBatchDto batchDto) {
        Long coachUserId = batchDto.getCoachUserId();
        List<CoachCourseTypeDto> courseTypes = batchDto.getCourseTypes();

        log.info("批量创建/更新课程类型 - coachUserId: {}, 课程数量: {}", coachUserId, courseTypes.size());

        // 检查是否有重复的服务类型
        Set<Integer> serviceTypes = new HashSet<>();
        for (CoachCourseTypeDto dto : courseTypes) {
            if (!serviceTypes.add(dto.getCoachServiceTypeEnum())) {
                throw new GloboxApplicationException("请求中存在重复的服务类型: " + dto.getCoachServiceTypeEnum());
            }
        }

        // 查询该教练现有的所有课程类型
        List<CoachCourseType> existingCourseTypes = coachCourseTypeMapper.selectList(
                new LambdaQueryWrapper<CoachCourseType>()
                        .eq(CoachCourseType::getCoachUserId, coachUserId)
        );

        // 构建现有课程类型的映射 (服务类型 -> 课程类型实体)
        Map<Integer, CoachCourseType> existingMap = existingCourseTypes.stream()
                .collect(Collectors.toMap(
                        CoachCourseType::getCoachServiceTypeEnum,
                        ct -> ct
                ));

        // 结果映射 (服务类型 -> 课程类型ID)
        Map<Integer, Long> resultMap = new LinkedHashMap<>();

        // 遍历请求的课程类型,逐个处理
        for (CoachCourseTypeDto dto : courseTypes) {
            Integer serviceType = dto.getCoachServiceTypeEnum();
            CoachCourseType existing = existingMap.get(serviceType);

            if (existing != null) {
                // 更新现有课程类型
                existing.setCourseCover(dto.getCourseCover());
                existing.setCoachCourseTypeName(dto.getCoachCourseTypeName());
                existing.setCoachDuration(dto.getCoachDuration());
                existing.setCoachPrice(dto.getCoachPrice());
                existing.setCoachDescription(dto.getCoachDescription());
                existing.setCoachIsActive(dto.getCoachIsActive());

                coachCourseTypeMapper.updateById(existing);
                resultMap.put(serviceType, existing.getCoachCourseTypeId());

                log.info("更新课程类型 - serviceType: {}, courseTypeId: {}", serviceType, existing.getCoachCourseTypeId());
            } else {
                // 创建新的课程类型
                CoachCourseType newCourseType = new CoachCourseType();
                newCourseType.setCoachUserId(coachUserId);
                newCourseType.setCourseCover(dto.getCourseCover());
                newCourseType.setCoachCourseTypeName(dto.getCoachCourseTypeName());
                newCourseType.setCoachServiceTypeEnum(dto.getCoachServiceTypeEnum());
                newCourseType.setCoachDuration(dto.getCoachDuration());
                newCourseType.setCoachPrice(dto.getCoachPrice());
                newCourseType.setCoachDescription(dto.getCoachDescription());
                newCourseType.setCoachIsActive(dto.getCoachIsActive());

                coachCourseTypeMapper.insert(newCourseType);
                resultMap.put(serviceType, newCourseType.getCoachCourseTypeId());

                log.info("创建课程类型 - serviceType: {}, courseTypeId: {}", serviceType, newCourseType.getCoachCourseTypeId());
            }
        }

        log.info("批量创建/更新课程类型完成 - coachUserId: {}, 处理数量: {}", coachUserId, resultMap.size());
        return resultMap;
    }

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
