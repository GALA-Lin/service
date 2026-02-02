package com.unlimited.sports.globox.service;

import com.unlimited.sports.globox.common.enums.ClientType;
import com.unlimited.sports.globox.model.governance.dto.FeedbackSubmitRequest;

/**
 * 用户反馈 Service
 */
public interface FeedbackService {


    void submit(FeedbackSubmitRequest req, ClientType clientType, Long userId);
}
