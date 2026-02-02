package com.unlimited.sports.globox.service.impl;

import com.unlimited.sports.globox.common.enums.ClientType;
import com.unlimited.sports.globox.common.utils.RequestContextHolder;
import com.unlimited.sports.globox.mapper.FeedbackMapper;
import com.unlimited.sports.globox.model.governance.dto.FeedbackSubmitRequest;
import com.unlimited.sports.globox.model.governance.entity.Feedback;
import com.unlimited.sports.globox.service.FeedbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户反馈 Service
 */
@Slf4j
@Service
public class FeedbackServiceImpl implements FeedbackService {

    @Autowired
    private FeedbackMapper feedbackMapper;

    @Override
    public void submit(FeedbackSubmitRequest req, ClientType clientType, Long userId) {
        String ip = RequestContextHolder.getCurrentRequestIp();

        Feedback feedback = Feedback.builder()
                .userId(userId)
                .contact(req.getContact())
                .content(req.getContent())
                .clientType(clientType)
                .appVersion(req.getAppVersion())
                .osVersion(req.getOsVersion())
                .deviceModel(req.getDeviceModel())
                .ip(ip)
                .build();

        feedbackMapper.insert(feedback);
    }
}
