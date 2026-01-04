package com.unlimited.sports.globox.coach.controller;

import com.unlimited.sports.globox.coach.service.ICoachSlotService;
import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.utils.AuthContextHolder;
import com.unlimited.sports.globox.model.coach.dto.CoachSlotTemplateInitDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * @since 2026/1/3 16:43
 * 教练时段管理接口
 */
@Slf4j
@RestController
@RequestMapping("/coach/slots")
public class CoachSlotController {

    @Autowired
    private ICoachSlotService slotService;

    @PostMapping("/templates/init")
    public R<Void> initSlotTemplates(@Valid @RequestBody CoachSlotTemplateInitDto dto) {
        Long coachUserId = AuthContextHolder.getLongHeader(RequestHeaderConstants.HEADER_USER_ID);
        dto.setCoachUserId(coachUserId);

        log.info("初始化时段模板 - coachUserId: {}", coachUserId);
        slotService.initSlotTemplates(dto);
        return R.ok();
    }

}
