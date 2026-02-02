package com.unlimited.sports.globox.controller;

import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.enums.ClientType;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.governance.dto.FeedbackSubmitRequest;
import com.unlimited.sports.globox.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/governance/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * 用户提交反馈
     */
    @PostMapping("/submit")
    public R<Void> submit(
            @Validated @RequestBody FeedbackSubmitRequest req,
            @RequestHeader(RequestHeaderConstants.HEADER_CLIENT_TYPE) String clientTypeStr,
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId
    ) {
        ClientType clientType = ClientType.fromValue(clientTypeStr);
        feedbackService.submit(req, clientType, userId);
        return R.ok();
    }
}