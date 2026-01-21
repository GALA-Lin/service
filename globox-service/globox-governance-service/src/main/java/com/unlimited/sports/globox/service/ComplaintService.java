package com.unlimited.sports.globox.service;

import com.unlimited.sports.globox.common.enums.ClientType;
import com.unlimited.sports.globox.model.governance.dto.CreateComplaintRequestDto;

import javax.validation.Valid;

/**
 * 举报工单 服务层
 */
public interface ComplaintService {
    void createComplaint(@Valid CreateComplaintRequestDto dto, Long userId, ClientType clientType);
}
