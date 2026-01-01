package com.unlimited.sports.globox.coach.service;

import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.model.coach.dto.GetCoachListDto;
import com.unlimited.sports.globox.model.coach.vo.CoachDetailVo;
import com.unlimited.sports.globox.model.coach.vo.CoachItemVo;

import java.math.BigDecimal;

/**
 * @since 2025/12/31 13:58
 * 教练服务接口
 */

public interface ICoachService {


    PaginationResult<CoachItemVo> searchCoaches(GetCoachListDto dto);

    CoachDetailVo getCoachDetail(Long coachUserId, Double latitude, Double longitude);

}