package com.unlimited.sports.globox.coach.controller;

import com.unlimited.sports.globox.coach.service.ICoachService;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.coach.dto.GetCoachListDto;
import com.unlimited.sports.globox.model.coach.vo.CoachDetailVo;
import com.unlimited.sports.globox.model.coach.vo.CoachItemVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;

/**
 * @since 2025/12/31 16:59
 * 教练相关接口
 */
@Slf4j
@RestController
@RequestMapping("/coach/coaches")
public class CoachController {

    @Autowired
    private ICoachService coachService;

    /**
     * 获取教练列表（支持搜索、过滤、排序）
     *
     * @param dto 查询条件
     * @return 分页的教练列表
     */
    @GetMapping
    public R<PaginationResult<CoachItemVo>> getCoachList(@Valid GetCoachListDto dto) {
        log.info("获取教练列表 - sortBy: {}, page: {}/{}", dto.getSortBy(), dto.getPage(), dto.getPageSize());
        PaginationResult<CoachItemVo> result = coachService.searchCoaches(dto);
        return R.ok(result);
    }

    /**
     * 获取教练详情
     *
     * @param coachUserId 教练用户ID
     * @param latitude 用户当前纬度（可选）
     * @param longitude 用户当前经度（可选）
     * @return 教练详情信息
     */
    @GetMapping("/{coachUserId}")
    public R<CoachDetailVo> getCoachDetail(
            @PathVariable Long coachUserId,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        log.info("获取教练详情 - coachUserId: {}", coachUserId);
        CoachDetailVo result = coachService.getCoachDetail(coachUserId, latitude, longitude);
        return R.ok(result);
    }
}