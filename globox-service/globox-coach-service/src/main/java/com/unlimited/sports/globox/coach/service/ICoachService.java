package com.unlimited.sports.globox.coach.service;

import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.model.coach.dto.QueryCoachListDto;
import com.unlimited.sports.globox.model.coach.vo.CoachListItemVo;

/**
 * @since 2025/12/31 13:58
 * 教练服务接口
 */

public interface ICoachService {

    /**
     * 查询教练列表（支持筛选和排序）
     *
     * @param dto 查询条件
     * @return 分页后的教练列表
     */
    PaginationResult<CoachListItemVo> queryCoachList(QueryCoachListDto dto);

    /**
     * 获取教练详情
     *
     * @param coachUserId 教练ID（用户ID）
     * @param latitude 用户纬度（可选，用于计算距离）
     * @param longitude 用户经度（可选，用于计算距离）
     * @return 教练详情
     */
    CoachDetailVo getCoachDetail(Long coachUserId, Double latitude, Double longitude);
}